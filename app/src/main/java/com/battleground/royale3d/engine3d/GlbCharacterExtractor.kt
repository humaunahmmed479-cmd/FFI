package com.battleground.royale3d.engine3d

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extracts a single character (variant 0..5) from the multi-character
 * all_freefire_rare_jersey_for_free_obj_file.glb pack and centers it at origin.
 */
object GlbCharacterExtractor {

    // Exact X/Y centers of each of the 6 characters inside the multi-jersey GLB
    private val characterCenters = arrayOf(
        0.004f to 0.009f,    // Variant 0: Main Player Character (center)
        0.415f to -0.226f,   // Variant 1: Enemy Jersey A
        -0.481f to -0.237f,  // Variant 2: Enemy Jersey B
        0.973f to 0.009f,    // Variant 3: Enemy Jersey C
        -0.969f to 0.009f,   // Variant 4: Enemy Jersey D
        0.004f to -0.595f    // Variant 5: Enemy Jersey E
    )

    fun extractSingleCharacter(glbBytes: ByteArray, variantIndex: Int): ByteBuffer? {
        if (glbBytes.size < 20) return null
        val buf = ByteBuffer.wrap(glbBytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.getInt(0)
        val version = buf.getInt(4)
        if (magic != 0x46546C67 || version != 2) return null

        val jsonChunkLen = buf.getInt(12)
        val jsonChunkType = buf.getInt(16)
        if (jsonChunkType != 0x4E4F534A) return null

        val jsonStart = 20
        val jsonEnd = jsonStart + jsonChunkLen
        if (jsonEnd + 8 > glbBytes.size) return null

        val binChunkLen = buf.getInt(jsonEnd)
        val binChunkType = buf.getInt(jsonEnd + 4)
        if (binChunkType != 0x004E4942) return null

        val jsonStr = String(glbBytes, jsonStart, jsonChunkLen, Charsets.UTF_8).trimEnd('\u0000', ' ')
        val rootJson = JSONObject(jsonStr)
        val nodes = rootJson.optJSONArray("nodes") ?: return null
        if (nodes.length() < 38) return null

        val v = variantIndex.coerceIn(0, 5)
        // Orient Sketchfab Z-up / +Y-front character to glTF Y-up / +Z-front (det = +1)
        val rootNode = nodes.getJSONObject(0)
        rootNode.put("matrix", JSONArray().apply {
            put(-1.0); put(0.0); put(0.0); put(0.0)
            put(0.0); put(0.0); put(1.0); put(0.0)
            put(0.0); put(1.0); put(0.0); put(0.0)
            put(0.0); put(0.0); put(0.0); put(1.0)
        })

        val parentNode = nodes.getJSONObject(1)

        // Each character k has 6 body-part nodes: 2+k, 8+k, 14+k, 20+k, 26+k, 32+k
        val singleCharChildren = JSONArray().apply {
            for (part in 0 until 6) {
                put(2 + v + part * 6)
            }
        }
        parentNode.put("children", singleCharChildren)

        // Center character v at local origin (0, 0, 0)
        val (cx, cy) = characterCenters[v]
        parentNode.put("translation", JSONArray().apply {
            put(-cx.toDouble())
            put(-cy.toDouble())
            put(0.0)
        })

        val modifiedJsonBytes = rootJson.toString().toByteArray(Charsets.UTF_8)
        val jsonPad = (4 - (modifiedJsonBytes.size % 4)) % 4
        val newJsonChunkLen = modifiedJsonBytes.size + jsonPad
        val binPayloadLen = minOf(binChunkLen, glbBytes.size - (jsonEnd + 8))
        val binPad = (4 - (binPayloadLen % 4)) % 4
        val newBinChunkLen = binPayloadLen + binPad

        val totalLen = 12 + 8 + newJsonChunkLen + 8 + newBinChunkLen
        val out = ByteBuffer.allocateDirect(totalLen).order(ByteOrder.LITTLE_ENDIAN)
        // GLB Header
        out.putInt(0x46546C67)
        out.putInt(2)
        out.putInt(totalLen)
        // JSON Chunk
        out.putInt(newJsonChunkLen)
        out.putInt(0x4E4F534A)
        out.put(modifiedJsonBytes)
        repeat(jsonPad) { out.put(0x20.toByte()) }
        // BIN Chunk (unchanged vertex/texture binary payload)
        out.putInt(newBinChunkLen)
        out.putInt(0x004E4942)
        out.put(glbBytes, jsonEnd + 8, binPayloadLen)
        repeat(binPad) { out.put(0.toByte()) }
        out.rewind()
        return out
    }
}
