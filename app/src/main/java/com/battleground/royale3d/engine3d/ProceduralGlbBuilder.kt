package com.battleground.royale3d.engine3d

import com.battleground.royale3d.model.BuildingCollider
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates valid in-memory glTF 2.0 binary (.glb) buffers for the 3D Theme Park map,
 * BD Sector, Safe Zone energy barrier, parachute canopy, loot pedestals, and bullet tracers.
 */
object ProceduralGlbBuilder {

    data class MaterialDef(
        val name: String,
        val r: Float,
        val g: Float,
        val b: Float,
        val a: Float = 1f,
        val metallic: Float = 0.08f,
        val roughness: Float = 0.78f,
        val emissiveR: Float = 0f,
        val emissiveG: Float = 0f,
        val emissiveB: Float = 0f,
        val alphaBlend: Boolean = false,
        val doubleSided: Boolean = false
    )

    private class MeshBatch(val materialIndex: Int) {
        val positions = ArrayList<Float>(1024)
        val normals = ArrayList<Float>(1024)
        val indices = ArrayList<Short>(1024)

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float): Short {
            val idx = (positions.size / 3).toShort()
            positions.add(x); positions.add(y); positions.add(z)
            normals.add(nx); normals.add(ny); normals.add(nz)
            minX = min(minX, x); minY = min(minY, y); minZ = min(minZ, z)
            maxX = max(maxX, x); maxY = max(maxY, y); maxZ = max(maxZ, z)
            return idx
        }

        fun addQuad(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float,
            nx: Float, ny: Float, nz: Float
        ) {
            if ((positions.size / 3) + 4 >= 65000) return
            val i0 = addVertex(x0, y0, z0, nx, ny, nz)
            val i1 = addVertex(x1, y1, z1, nx, ny, nz)
            val i2 = addVertex(x2, y2, z2, nx, ny, nz)
            val i3 = addVertex(x3, y3, z3, nx, ny, nz)
            indices.add(i0); indices.add(i1); indices.add(i2)
            indices.add(i0); indices.add(i2); indices.add(i3)
        }

        fun addBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float) {
            val hx = sx * 0.5f
            val hy = sy * 0.5f
            val hz = sz * 0.5f
            val x0 = cx - hx; val x1 = cx + hx
            val y0 = cy - hy; val y1 = cy + hy
            val z0 = cz - hz; val z1 = cz + hz
            // Front (+Z)
            addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f)
            // Back (-Z)
            addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f)
            // Right (+X)
            addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f)
            // Left (-X)
            addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f)
            // Top (+Y)
            addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f)
            // Bottom (-Y)
            addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f)
        }
    }

    data class WorldMapResult(
        val glbBuffer: ByteBuffer,
        val colliders: List<BuildingCollider>
    )

    fun buildThemeParkWorld(): WorldMapResult {
        val materials = listOf(
            MaterialDef("ground_turf", 0.14f, 0.24f, 0.16f, roughness = 0.92f),      // 0: Grass
            MaterialDef("road_asphalt", 0.14f, 0.15f, 0.18f, roughness = 0.85f),     // 1: Roads / Parking
            MaterialDef("plaza_stone", 0.27f, 0.30f, 0.36f, roughness = 0.72f),      // 2: Central Plaza
            MaterialDef("amber_accent", 1.0f, 0.62f, 0.11f, metallic = 0.4f, roughness = 0.4f, emissiveR = 0.25f, emissiveG = 0.12f), // 3: Amber trim
            MaterialDef("bd_emerald", 0.0f, 0.42f, 0.28f, roughness = 0.65f),        // 4: BD green
            MaterialDef("bd_crimson", 0.85f, 0.16f, 0.22f, roughness = 0.55f, emissiveR = 0.12f), // 5: BD red
            MaterialDef("steel_dark", 0.22f, 0.26f, 0.32f, metallic = 0.7f, roughness = 0.38f), // 6: Steel structures
            MaterialDef("cyan_neon", 0.0f, 0.85f, 1.0f, emissiveR = 0.0f, emissiveG = 0.45f, emissiveB = 0.65f), // 7: Neon trim
            MaterialDef("sea_water", 0.06f, 0.15f, 0.26f, roughness = 0.22f)         // 8: Island ocean
        )

        val batches = materials.indices.map { MeshBatch(it) }
        val colliders = ArrayList<BuildingCollider>()

        fun addBuilding(matIdx: Int, cx: Float, cz: Float, sx: Float, sy: Float, sz: Float, solid: Boolean = true) {
            batches[matIdx].addBox(cx, sy * 0.5f, cz, sx, sy, sz)
            if (solid) {
                colliders.add(
                    BuildingCollider(
                        id = "b_${colliders.size}",
                        minX = cx - sx * 0.5f,
                        maxX = cx + sx * 0.5f,
                        minZ = cz - sz * 0.5f,
                        maxZ = cz + sz * 0.5f,
                        height = sy
                    )
                )
            }
        }

        // Island Ocean & Turf Floor
        batches[8].addBox(0f, -1.2f, 0f, 920f, 1f, 920f)
        batches[0].addBox(0f, -0.25f, 0f, 580f, 0.5f, 580f)

        // Main Cross-Island Roads
        batches[1].addBox(0f, 0.03f, 0f, 520f, 0.06f, 18f)
        batches[1].addBox(0f, 0.03f, 0f, 18f, 0.06f, 520f)
        // Diagonal connector to BD Sector
        batches[1].addBox(115f, 0.03f, 10f, 14f, 0.06f, 220f)
        batches[1].addBox(-115f, 0.03f, 10f, 14f, 0.06f, 220f)

        // === CENTRAL PLAZA (0, 0) ===
        batches[2].addBox(0f, 0.08f, 0f, 110f, 0.16f, 110f)
        // Central Monument & Sniper Tower
        addBuilding(6, 0f, 0f, 10f, 14f, 10f)
        batches[3].addBox(0f, 14.5f, 0f, 11f, 1.0f, 11f)
        batches[7].addBox(0f, 16.5f, 0f, 3.5f, 3.5f, 3.5f)
        // Tactical Cover Walls around Plaza
        val plazaCovers = listOf(
            floatArrayOf(-26f, -18f, 12f, 2.8f, 2.2f),
            floatArrayOf(26f, -18f, 12f, 2.8f, 2.2f),
            floatArrayOf(-26f, 18f, 12f, 2.8f, 2.2f),
            floatArrayOf(26f, 18f, 12f, 2.8f, 2.2f),
            floatArrayOf(0f, -38f, 18f, 4.5f, 6f),
            floatArrayOf(0f, 38f, 18f, 4.5f, 6f)
        )
        plazaCovers.forEach { c -> addBuilding(2, c[0], c[1], c[2], c[3], c[4]) }

        // === RIDE AREA (-115, -95) ===
        batches[2].addBox(-115f, 0.06f, -95f, 120f, 0.12f, 120f)
        // Ferris Wheel Tower + Ring Segments
        addBuilding(6, -130f, -110f, 5f, 28f, 5f)
        addBuilding(6, -116f, -110f, 5f, 28f, 5f)
        val wheelCenterX = -123f
        val wheelCenterY = 26f
        val wheelCenterZ = -110f
        val wheelRadius = 19f
        for (i in 0 until 16) {
            val a = (i / 16.0 * Math.PI * 2.0).toFloat()
            val gx = wheelCenterX + cos(a) * wheelRadius
            val gy = wheelCenterY + sin(a) * wheelRadius
            batches[if (i % 2 == 0) 3 else 7].addBox(gx, gy, wheelCenterZ, 3.4f, 3.4f, 4.2f)
        }
        // Carnival Booths & Coaster Arches
        addBuilding(3, -92f, -76f, 14f, 5f, 8f)
        addBuilding(5, -92f, -112f, 14f, 5f, 8f)
        addBuilding(7, -145f, -72f, 12f, 4.5f, 10f)
        addBuilding(6, -108f, -54f, 20f, 3.0f, 3f)

        // === PARKING LOT (115, -90) ===
        batches[1].addBox(115f, 0.06f, -90f, 125f, 0.12f, 115f)
        // Parking Stripe Bars & Guard Booth
        for (row in -2..2) {
            batches[3].addBox(85f + row * 16f, 0.15f, -90f, 0.5f, 0.1f, 32f)
        }
        addBuilding(6, 152f, -120f, 18f, 6.5f, 12f)
        addBuilding(2, 72f, -65f, 10f, 3.2f, 14f)
        addBuilding(6, 145f, -55f, 14f, 4.5f, 9f)

        // === BD SECTOR (105, 110) ===
        batches[4].addBox(105f, 0.08f, 110f, 118f, 0.16f, 118f)
        // Red & Green Tactical Arches + Watchtowers + Supply Bunkers
        addBuilding(4, 72f, 82f, 16f, 6f, 12f)
        addBuilding(5, 138f, 82f, 16f, 6f, 12f)
        addBuilding(4, 105f, 142f, 24f, 7f, 12f)
        batches[5].addBox(105f, 8.5f, 142f, 8f, 2.2f, 12.5f)
        addBuilding(6, 75f, 136f, 6f, 12f, 6f)
        addBuilding(6, 135f, 136f, 6f, 12f, 6f)
        // Red sun emblem monument in BD plaza center
        batches[5].addBox(105f, 3.2f, 110f, 7f, 6.4f, 7f)
        colliders.add(BuildingCollider("bd_monument", 101.5f, 108.5f, 106.5f, 113.5f, 6.4f))

        // === PARK OUTSKIRTS (-95, 110) ===
        addBuilding(2, -95f, 110f, 18f, 5.5f, 14f)
        addBuilding(6, -135f, 92f, 12f, 8f, 12f)
        addBuilding(3, -62f, 132f, 14f, 4f, 8f)
        addBuilding(4, -120f, 138f, 16f, 5f, 10f)

        // Perimeter Watch Walls along island coast
        for (i in -3..3) {
            if (i == 0) continue
            addBuilding(6, i * 70f, -275f, 26f, 4.5f, 4f)
            addBuilding(6, i * 70f, 275f, 26f, 4.5f, 4f)
            addBuilding(6, -275f, i * 70f, 4f, 4.5f, 26f)
            addBuilding(6, 275f, i * 70f, 4f, 4.5f, 26f)
        }

        return WorldMapResult(
            glbBuffer = encodeGlb(materials, batches.filter { it.indices.isNotEmpty() }),
            colliders = colliders
        )
    }

    fun buildSafeZoneCylinder(): ByteBuffer {
        val mat = MaterialDef(
            name = "safe_zone_wall",
            r = 0.0f,
            g = 0.82f,
            b = 1.0f,
            a = 0.28f,
            metallic = 0.0f,
            roughness = 0.2f,
            emissiveR = 0.0f,
            emissiveG = 0.7f,
            emissiveB = 1.0f,
            alphaBlend = true,
            doubleSided = true
        )
        val batch = MeshBatch(0)
        val segments = 64
        val height = 1.0f
        for (i in 0 until segments) {
            val a0 = (i.toDouble() / segments * Math.PI * 2.0).toFloat()
            val a1 = ((i + 1).toDouble() / segments * Math.PI * 2.0).toFloat()
            val x0 = cos(a0); val z0 = sin(a0)
            val x1 = cos(a1); val z1 = sin(a1)
            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)
            batch.addQuad(
                x0, 0f, z0,
                x1, 0f, z1,
                x1, height, z1,
                x0, height, z0,
                nx, 0f, nz
            )
        }
        return encodeGlb(listOf(mat), listOf(batch))
    }

    fun buildParachuteCanopy(): ByteBuffer {
        val materials = listOf(
            MaterialDef("chute_amber", 1.0f, 0.62f, 0.12f, roughness = 0.55f, doubleSided = true),
            MaterialDef("chute_cyan", 0.0f, 0.85f, 1.0f, roughness = 0.55f, doubleSided = true)
        )
        val b0 = MeshBatch(0)
        val b1 = MeshBatch(1)
        val slices = 10
        val width = 3.6f
        val depth = 2.0f
        for (i in 0 until slices) {
            val t0 = i.toFloat() / slices
            val t1 = (i + 1).toFloat() / slices
            val a0 = (t0 - 0.5f) * Math.PI.toFloat() * 0.85f
            val a1 = (t1 - 0.5f) * Math.PI.toFloat() * 0.85f
            val x0 = sin(a0) * width * 0.5f
            val y0 = cos(a0) * 1.1f
            val x1 = sin(a1) * width * 0.5f
            val y1 = cos(a1) * 1.1f
            val target = if (i % 2 == 0) b0 else b1
            target.addQuad(
                x0, y0, -depth * 0.5f,
                x1, y1, -depth * 0.5f,
                x1, y1, depth * 0.5f,
                x0, y0, depth * 0.5f,
                0f, 1f, 0f
            )
        }
        return encodeGlb(materials, listOf(b0, b1))
    }

    fun buildLootBox(): ByteBuffer {
        val materials = listOf(
            MaterialDef("crate_body", 0.15f, 0.22f, 0.30f, metallic = 0.45f, roughness = 0.45f),
            MaterialDef("crate_glow", 1.0f, 0.72f, 0.12f, emissiveR = 0.9f, emissiveG = 0.55f, emissiveB = 0.05f)
        )
        val b0 = MeshBatch(0).apply { addBox(0f, 0.3f, 0f, 0.85f, 0.55f, 0.65f) }
        val b1 = MeshBatch(1).apply { addBox(0f, 0.62f, 0f, 0.9f, 0.1f, 0.7f) }
        return encodeGlb(materials, listOf(b0, b1))
    }

    fun buildTracerBeam(): ByteBuffer {
        val mat = MaterialDef(
            name = "tracer",
            r = 1.0f,
            g = 0.85f,
            b = 0.2f,
            emissiveR = 1.0f,
            emissiveG = 0.75f,
            emissiveB = 0.15f
        )
        val batch = MeshBatch(0).apply {
            addBox(0f, 0f, 0.5f, 0.05f, 0.05f, 1.0f)
        }
        return encodeGlb(listOf(mat), listOf(batch))
    }

    fun buildLobbyStage(): ByteBuffer {
        val materials = listOf(
            MaterialDef("stage_floor", 0.09f, 0.13f, 0.21f, metallic = 0.35f, roughness = 0.55f),
            MaterialDef("stage_pedestal", 0.16f, 0.21f, 0.30f, metallic = 0.55f, roughness = 0.42f),
            MaterialDef("stage_cyan_glow", 0.0f, 0.85f, 1.0f, emissiveR = 0.0f, emissiveG = 0.55f, emissiveB = 0.85f),
            MaterialDef("stage_amber_trim", 1.0f, 0.62f, 0.12f, emissiveR = 0.55f, emissiveG = 0.30f, emissiveB = 0.04f)
        )
        val b0 = MeshBatch(0)
        val b1 = MeshBatch(1)
        val b2 = MeshBatch(2)
        val b3 = MeshBatch(3)

        // Wide tactical hangar floor & backdrop pillars behind player (-Z)
        b0.addBox(0f, -0.35f, 0f, 24f, 0.4f, 24f)
        b1.addBox(0f, -0.07f, 0f, 2.3f, 0.14f, 2.3f)
        b1.addBox(0f, -0.02f, 0f, 1.75f, 0.05f, 1.75f)
        // Glowing outer ring segments
        val segs = 24
        for (i in 0 until segs) {
            val a = (i.toDouble() / segs * Math.PI * 2.0).toFloat()
            val x = cos(a) * 1.15f
            val z = sin(a) * 1.15f
            val target = if (i % 6 == 0) b3 else b2
            target.addBox(x, 0.01f, z, 0.24f, 0.03f, 0.24f)
        }
        // Subtle cyber backdrop columns far behind player (-Z) for depth
        b1.addBox(-3.6f, 1.8f, -4.5f, 0.55f, 4.0f, 0.55f)
        b1.addBox(3.6f, 1.8f, -4.5f, 0.55f, 4.0f, 0.55f)
        b2.addBox(-3.6f, 1.8f, -4.2f, 0.12f, 3.6f, 0.08f)
        b2.addBox(3.6f, 1.8f, -4.2f, 0.12f, 3.6f, 0.08f)
        b3.addBox(0f, 3.7f, -4.5f, 7.6f, 0.18f, 0.35f)

        return encodeGlb(materials, listOf(b0, b1, b2, b3))
    }

    private fun encodeGlb(materials: List<MaterialDef>, batches: List<MeshBatch>): ByteBuffer {
        val binStream = ByteArrayOutputStream()
        val bufferViews = ArrayList<String>()
        val accessors = ArrayList<String>()
        val primitives = ArrayList<String>()

        var byteOffset = 0

        fun writeFloats(list: List<Float>): Int {
            val viewIdx = bufferViews.size
            val len = list.size * 4
            val buf = ByteBuffer.allocate(len).order(ByteOrder.LITTLE_ENDIAN)
            for (v in list) buf.putFloat(v)
            binStream.write(buf.array())
            bufferViews.add("""{"buffer":0,"byteOffset":$byteOffset,"byteLength":$len,"target":34962}""")
            byteOffset += len
            return viewIdx
        }

        fun writeShorts(list: List<Short>): Int {
            val viewIdx = bufferViews.size
            val rawLen = list.size * 2
            val pad = (4 - (rawLen % 4)) % 4
            val buf = ByteBuffer.allocate(rawLen + pad).order(ByteOrder.LITTLE_ENDIAN)
            for (v in list) buf.putShort(v)
            for (p in 0 until pad) buf.put(0)
            binStream.write(buf.array())
            bufferViews.add("""{"buffer":0,"byteOffset":$byteOffset,"byteLength":$rawLen,"target":34963}""")
            byteOffset += rawLen + pad
            return viewIdx
        }

        for (batch in batches) {
            val posView = writeFloats(batch.positions)
            val normView = writeFloats(batch.normals)
            val idxView = writeShorts(batch.indices)

            val vertCount = batch.positions.size / 3
            val posAcc = accessors.size
            accessors.add(
                """{"bufferView":$posView,"componentType":5126,"count":$vertCount,"type":"VEC3","min":[${batch.minX},${batch.minY},${batch.minZ}],"max":[${batch.maxX},${batch.maxY},${batch.maxZ}]}"""
            )
            val normAcc = accessors.size
            accessors.add(
                """{"bufferView":$normView,"componentType":5126,"count":$vertCount,"type":"VEC3"}"""
            )
            val idxAcc = accessors.size
            accessors.add(
                """{"bufferView":$idxView,"componentType":5123,"count":${batch.indices.size},"type":"SCALAR"}"""
            )
            primitives.add(
                """{"attributes":{"POSITION":$posAcc,"NORMAL":$normAcc},"indices":$idxAcc,"material":${batch.materialIndex}}"""
            )
        }

        val matsJson = materials.joinToString(",") { m ->
            val alpha = if (m.alphaBlend) ""","alphaMode":"BLEND"""" else ""
            val ds = if (m.doubleSided) ""","doubleSided":true""" else ""
            val em = if (m.emissiveR > 0f || m.emissiveG > 0f || m.emissiveB > 0f) {
                ""","emissiveFactor":[${m.emissiveR},${m.emissiveG},${m.emissiveB}]"""
            } else ""
            """{"name":"${m.name}","pbrMetallicRoughness":{"baseColorFactor":[${m.r},${m.g},${m.b},${m.a}],"metallicFactor":${m.metallic},"roughnessFactor":${m.roughness}}$em$alpha$ds}"""
        }

        val jsonStr = """{"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}],"meshes":[{"primitives":[${primitives.joinToString(",")}]}],"materials":[$matsJson],"buffers":[{"byteLength":$byteOffset}],"bufferViews":[${bufferViews.joinToString(",")}],"accessors":[${accessors.joinToString(",")}]}"""

        val jsonBytesRaw = jsonStr.toByteArray(Charsets.UTF_8)
        val jsonPad = (4 - (jsonBytesRaw.size % 4)) % 4
        val jsonLen = jsonBytesRaw.size + jsonPad

        val binRaw = binStream.toByteArray()
        val binPad = (4 - (binRaw.size % 4)) % 4
        val binLen = binRaw.size + binPad

        val totalLen = 12 + 8 + jsonLen + 8 + binLen
        val out = ByteBuffer.allocateDirect(totalLen).order(ByteOrder.LITTLE_ENDIAN)
        // Header
        out.putInt(0x46546C67)
        out.putInt(2)
        out.putInt(totalLen)
        // JSON Chunk
        out.putInt(jsonLen)
        out.putInt(0x4E4F534A)
        out.put(jsonBytesRaw)
        repeat(jsonPad) { out.put(0x20.toByte()) }
        // BIN Chunk
        out.putInt(binLen)
        out.putInt(0x004E4942)
        out.put(binRaw)
        repeat(binPad) { out.put(0.toByte()) }

        out.rewind()
        return out
    }
}
