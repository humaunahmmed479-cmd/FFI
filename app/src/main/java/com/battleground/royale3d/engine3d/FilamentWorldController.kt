package com.battleground.royale3d.engine3d

import android.content.Context
import android.opengl.Matrix
import android.view.Surface
import android.view.TextureView
import com.battleground.royale3d.data.GameCatalog
import com.battleground.royale3d.model.CameraPerspective
import com.battleground.royale3d.model.GameHudState
import com.battleground.royale3d.model.GraphicsQuality
import com.battleground.royale3d.model.MatchPhase
import com.battleground.royale3d.model.VehicleType
import com.battleground.royale3d.model.WeaponType
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.FilamentInstance
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class FilamentWorldController(private val context: Context) {

    companion object {
        init {
            Utils.init()
        }

        private const val ENEMY_VARIANT_COUNT = 5
        private const val INSTANCES_PER_VARIANT = 3
        private const val ENEMY_POOL_SIZE = ENEMY_VARIANT_COUNT * INSTANCES_PER_VARIANT
        private const val LOOT_POOL_SIZE = 18
        private const val TRACER_POOL_SIZE = 8
    }

    private data class NormalizedAsset(
        val asset: FilamentAsset,
        val originalRootMatrix: FloatArray,
        val baseScale: Float,
        val centerX: Float,
        val minY: Float,
        val centerZ: Float,
        var inScene: Boolean = false
    )

    private data class EnemyVariantPool(
        val masterAsset: FilamentAsset,
        val originalRootMatrix: FloatArray,
        val scale: Float,
        val centerX: Float,
        val minY: Float,
        val centerZ: Float,
        val instances: Array<FilamentInstance?>,
        val inScene: BooleanArray
    )

    private val engine: Engine = Engine.create()
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val cameraEntity: Int = EntityManager.get().create()
    private val camera: Camera = engine.createCamera(cameraEntity)

    private val ubershaderProvider = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, ubershaderProvider, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine)

    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
        isOpaque = true
    }
    private var swapChain: SwapChain? = null
    private var viewportWidth = 1920
    private var viewportHeight = 1080

    private val sunEntity: Int = EntityManager.get().create()
    private val fillEntity: Int = EntityManager.get().create()
    private val rimEntity: Int = EntityManager.get().create()
    private val indirectLight: IndirectLight
    private val skybox: Skybox

    private val loadedModels = HashMap<String, NormalizedAsset>()
    private var lobbyStageAsset: FilamentAsset? = null
    private var worldAsset: FilamentAsset? = null
    private var safeZoneAsset: FilamentAsset? = null
    private var parachuteAsset: FilamentAsset? = null
    private var lobbyStageInScene = false
    private var worldInScene = false
    private var safeZoneInScene = false
    private var parachuteInScene = false

    private val enemyPools = ArrayList<EnemyVariantPool>(ENEMY_VARIANT_COUNT)

    private var lootMasterAsset: FilamentAsset? = null
    private val lootInstances = arrayOfNulls<FilamentInstance>(LOOT_POOL_SIZE)
    private val lootInScene = BooleanArray(LOOT_POOL_SIZE)

    private var tracerMasterAsset: FilamentAsset? = null
    private val tracerInstances = arrayOfNulls<FilamentInstance>(TRACER_POOL_SIZE)
    private val tracerInScene = BooleanArray(TRACER_POOL_SIZE)

    private var isDestroyed = false

    private val worldMat = FloatArray(16)
    private val finalMat = FloatArray(16)

    init {
        view.scene = scene
        view.camera = camera
        view.isPostProcessingEnabled = true
        view.blendMode = View.BlendMode.OPAQUE
        renderer.clearOptions = renderer.clearOptions.apply {
            clear = true
        }
        // Physical camera exposure tuned so textured PBR characters, weapons & cabin interior appear bright & vivid
        camera.setExposure(16.0f, 1.0f / 60.0f, 600.0f)

        indirectLight = IndirectLight.Builder()
            .irradiance(
                3,
                floatArrayOf(
                    1.0f, 1.0f, 1.0f,
                    0.0f, 0.35f, 0.0f,
                    0.0f, 0.25f, 0.0f,
                    0.15f, 0.15f, 0.20f,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f
                )
            )
            .intensity(55_000f)
            .build(engine)
        scene.indirectLight = indirectLight

        skybox = Skybox.Builder()
            .color(0.05f, 0.09f, 0.17f, 1.0f)
            .build(engine)
        scene.skybox = skybox

        // Front Key Sun Light (illuminates character face, chest & jersey front cleanly)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.97f, 0.92f)
            .intensity(165_000f)
            .direction(-0.22f, -0.58f, -0.78f)
            .castShadows(false)
            .build(engine, sunEntity)
        scene.addEntity(sunEntity)

        // Cool Cyan-White Front-Left Fill Light
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.78f, 0.90f, 1.0f)
            .intensity(105_000f)
            .direction(0.58f, -0.28f, -0.76f)
            .castShadows(false)
            .build(engine, fillEntity)
        scene.addEntity(fillEntity)

        // Warm Amber-White Rim Backlight for hero separation
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.85f, 0.68f)
            .intensity(110_000f)
            .direction(0.0f, -0.35f, 0.92f)
            .castShadows(false)
            .build(engine, rimEntity)
        scene.addEntity(rimEntity)

        lobbyStageAsset = assetLoader.createAsset(ProceduralGlbBuilder.buildLobbyStage())?.also {
            resourceLoader.loadResources(it)
            it.releaseSourceData()
        }

        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
            }

            override fun onDetachedFromSurface() {
                swapChain?.let {
                    engine.destroySwapChain(it)
                    swapChain = null
                }
                engine.flushAndWait()
            }

            override fun onResized(width: Int, height: Int) {
                viewportWidth = max(1, width)
                viewportHeight = max(1, height)
                view.viewport = Viewport(0, 0, viewportWidth, viewportHeight)
            }
        }
    }

    fun attachTextureView(textureView: TextureView) {
        textureView.isOpaque = true
        uiHelper.attachTo(textureView)
    }

    fun detachSurface() {
        uiHelper.detach()
    }

    fun readAssetBytes(assetPath: String): ByteArray? {
        return runCatching {
            context.assets.open(assetPath).use { it.readBytes() }
        }.getOrNull()
    }

    fun bytesToDirectBuffer(bytes: ByteArray): ByteBuffer {
        return ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            rewind()
        }
    }

    fun installModelFromBuffer(key: String, buffer: ByteBuffer, desiredMaxExtent: Float) {
        if (isDestroyed || loadedModels.containsKey(key)) return
        val asset = assetLoader.createAsset(buffer) ?: return
        resourceLoader.loadResources(asset)
        asset.releaseSourceData()

        val origMat = FloatArray(16)
        val ti = engine.transformManager.getInstance(asset.root)
        if (ti != 0) {
            engine.transformManager.getTransform(ti, origMat)
        } else {
            Matrix.setIdentityM(origMat, 0)
        }

        val box = asset.boundingBox
        val half = box.halfExtent
        val center = box.center
        val maxDim = max(half[0] * 2f, max(half[1] * 2f, half[2] * 2f)).coerceAtLeast(0.001f)
        val scale = desiredMaxExtent / maxDim
        val minY = center[1] - half[1]

        loadedModels[key] = NormalizedAsset(
            asset = asset,
            originalRootMatrix = origMat,
            baseScale = scale,
            centerX = center[0],
            minY = minY,
            centerZ = center[2]
        )
    }

    fun installWorldAndEnemyPools(
        worldGlb: ByteBuffer,
        multiCharacterGlbBytes: ByteArray?
    ) {
        if (isDestroyed) return
        if (worldAsset == null) {
            worldAsset = assetLoader.createAsset(worldGlb)?.also {
                resourceLoader.loadResources(it)
                it.releaseSourceData()
            }
        }
        if (safeZoneAsset == null) {
            safeZoneAsset = assetLoader.createAsset(ProceduralGlbBuilder.buildSafeZoneCylinder())?.also {
                resourceLoader.loadResources(it)
                it.releaseSourceData()
            }
        }
        if (parachuteAsset == null) {
            parachuteAsset = assetLoader.createAsset(ProceduralGlbBuilder.buildParachuteCanopy())?.also {
                resourceLoader.loadResources(it)
                it.releaseSourceData()
            }
        }
        if (lootMasterAsset == null) {
            lootMasterAsset = assetLoader.createInstancedAsset(
                ProceduralGlbBuilder.buildLootBox(),
                lootInstances
            )?.also {
                resourceLoader.loadResources(it)
                it.releaseSourceData()
            }
        }
        if (tracerMasterAsset == null) {
            tracerMasterAsset = assetLoader.createInstancedAsset(
                ProceduralGlbBuilder.buildTracerBeam(),
                tracerInstances
            )?.also {
                resourceLoader.loadResources(it)
                it.releaseSourceData()
            }
        }

        // Create 5 distinct single-character enemy jersey variants (variants 1..5)
        if (enemyPools.isEmpty() && multiCharacterGlbBytes != null) {
            for (variant in 1..ENEMY_VARIANT_COUNT) {
                val singleBuf = GlbCharacterExtractor.extractSingleCharacter(multiCharacterGlbBytes, variant)
                    ?: continue
                val instances = arrayOfNulls<FilamentInstance>(INSTANCES_PER_VARIANT)
                val master = assetLoader.createInstancedAsset(singleBuf, instances) ?: continue
                resourceLoader.loadResources(master)
                master.releaseSourceData()

                val origMat = FloatArray(16)
                val ti = engine.transformManager.getInstance(master.root)
                if (ti != 0) {
                    engine.transformManager.getTransform(ti, origMat)
                } else {
                    Matrix.setIdentityM(origMat, 0)
                }

                val box = master.boundingBox
                val half = box.halfExtent
                val center = box.center
                val maxDim = max(half[0] * 2f, max(half[1] * 2f, half[2] * 2f)).coerceAtLeast(0.001f)
                val scale = 1.85f / maxDim
                val minY = center[1] - half[1]

                enemyPools.add(
                    EnemyVariantPool(
                        masterAsset = master,
                        originalRootMatrix = origMat,
                        scale = scale,
                        centerX = center[0],
                        minY = minY,
                        centerZ = center[2],
                        instances = instances,
                        inScene = BooleanArray(INSTANCES_PER_VARIANT)
                    )
                )
            }
        }
    }

    /**
     * Lobby Showcase: Renders ONLY ONE single player character standing on a 3D cyber stage,
     * holding their equipped gun cleanly in their right hand.
     */
    fun renderLobbyShowcase(
        playerJerseyKey: String,
        equippedWeaponAsset: String,
        orbitYawDeg: Float,
        orbitPitchDeg: Float,
        frameTimeNanos: Long
    ) {
        if (isDestroyed) return
        setShowcaseBackdropMode()
        hideBattlegroundEntities()
        val keep = hashSetOf(playerJerseyKey, equippedWeaponAsset)
        hideUnlistedModels(keep)

        val yawRad = Math.toRadians(orbitYawDeg.toDouble()).toFloat()
        val baseY = -0.92f
        loadedModels[playerJerseyKey]?.let { jersey ->
            setAssetVisible(jersey, true)
            setNormalizedTransform(
                entity = jersey.asset.root,
                model = jersey,
                worldX = 0f,
                worldY = baseY,
                worldZ = 0f,
                yawRad = yawRad,
                scaleMult = 1.0f
            )
        }

        // Attach equipped gun right in the player's right hand, rotating cleanly with character
        loadedModels[equippedWeaponAsset]?.let { gun ->
            setAssetVisible(gun, true)
            val localHandX = -0.52f
            val localHandZ = 0.22f
            val handX = localHandX * cos(yawRad) + localHandZ * sin(yawRad)
            val handZ = -localHandX * sin(yawRad) + localHandZ * cos(yawRad)
            setNormalizedTransformTilted(
                entity = gun.asset.root,
                model = gun,
                worldX = handX,
                worldY = baseY + 0.90f,
                worldZ = handZ,
                yawRad = yawRad - 1.38f,
                rollDeg = -28f,
                scaleMult = 0.78f
            )
        }

        val aspect = viewportWidth.toDouble() / viewportHeight.coerceAtLeast(1).toDouble()
        camera.setProjection(42.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
        val pitchRad = Math.toRadians(orbitPitchDeg.toDouble().coerceIn(-8.0, 26.0))
        val dist = 2.85
        camera.lookAt(
            0.0, 0.22 + sin(pitchRad) * dist, cos(pitchRad) * dist,
            0.0, 0.02, 0.0,
            0.0, 1.0, 0.0
        )
        drawFrame(frameTimeNanos)
    }

    fun renderArmoryShowcase(
        assetKey: String,
        orbitYawDeg: Float,
        orbitPitchDeg: Float,
        zoomDistance: Float,
        frameTimeNanos: Long
    ) {
        if (isDestroyed) return
        setShowcaseBackdropMode()
        hideBattlegroundEntities()
        hideUnlistedModels(setOf(assetKey))

        val model = loadedModels[assetKey]
        if (model != null) {
            setAssetVisible(model, true)
            setNormalizedTransform(
                entity = model.asset.root,
                model = model,
                worldX = 0f,
                worldY = -0.92f,
                worldZ = 0f,
                yawRad = Math.toRadians(orbitYawDeg.toDouble()).toFloat(),
                scaleMult = 1.0f
            )
        }

        val aspect = viewportWidth.toDouble() / viewportHeight.coerceAtLeast(1).toDouble()
        camera.setProjection(44.0, aspect, 0.1, 150.0, Camera.Fov.VERTICAL)
        val pitchRad = Math.toRadians(orbitPitchDeg.toDouble().coerceIn(-12.0, 45.0))
        camera.lookAt(
            0.0, 0.35 + sin(pitchRad) * zoomDistance, cos(pitchRad) * zoomDistance,
            0.0, -0.05, 0.0,
            0.0, 1.0, 0.0
        )
        drawFrame(frameTimeNanos)
    }

    fun renderBattleground(
        state: GameHudState,
        playerJerseyKey: String,
        quality: GraphicsQuality,
        frameTimeNanos: Long
    ) {
        if (isDestroyed) return
        setLobbyStageVisible(false)
        scene.skybox = skybox
        view.blendMode = View.BlendMode.OPAQUE

        val lodMaxDistance = when (quality) {
            GraphicsQuality.LOW -> 95f
            GraphicsQuality.MEDIUM -> 160f
            GraphicsQuality.HIGH -> 250f
        }

        val aspect = viewportWidth.toDouble() / viewportHeight.coerceAtLeast(1).toDouble()

        // Always keep the Theme Park World visible below even while inside the Drop Plane!
        setWorldVisible(true)

        // Safe zone energy barrier
        safeZoneAsset?.let { sz ->
            val ti = engine.transformManager.getInstance(sz.root)
            if (ti != 0) {
                Matrix.setIdentityM(worldMat, 0)
                Matrix.translateM(worldMat, 0, state.safeZone.currentCenterX, 0f, state.safeZone.currentCenterZ)
                Matrix.scaleM(worldMat, 0, state.safeZone.currentRadius, 90f, state.safeZone.currentRadius)
                engine.transformManager.setTransform(ti, worldMat)
            }
        }

        // Vehicles on the Theme Park map (always visible in range, even from the air/plane)
        val vehicleMap = mapOf(
            VehicleType.AUDI_R15 to "models/2009_audi_r15_tdi.glb",
            VehicleType.BMW_I8 to "models/bmw_i8.glb",
            VehicleType.AJF12_DVERGR to "models/ajf-12_dvergr.glb"
        )
        for (v in state.vehicles) {
            val path = vehicleMap[v.spec.type] ?: continue
            val model = loadedModels[path] ?: continue
            val dist = distanceXZ(state.playerX, state.playerZ, v.x, v.z)
            if (dist <= lodMaxDistance || v.occupiedByPlayer || state.phase != MatchPhase.COMBAT) {
                setAssetVisible(model, true)
                val doorLift = if (v.doorOpen) 0.14f else 0f
                setNormalizedTransform(
                    entity = model.asset.root,
                    model = model,
                    worldX = v.x,
                    worldY = v.y + doorLift,
                    worldZ = v.z,
                    yawRad = v.yawRad,
                    scaleMult = 1.0f
                )
            } else {
                setAssetVisible(model, false)
            }
        }

        if (state.phase == MatchPhase.PLANE_CABIN) {
            hideLootAndTracers()
            listOf(
                "models/free_fire_awm_high_texture.glb",
                "models/mp40_cobra_evo_gun.glb",
                "models/free_fire_gun.glb"
            ).forEach { loadedModels[it]?.let { m -> setAssetVisible(m, false) } }

            // Place the Plane Cabin around planeWorldX/Z at altitude 95m so player & bots walk inside
            val cabin = loadedModels[GameCatalog.MODEL_PLANE_CABIN]
            if (cabin != null) {
                setAssetVisible(cabin, true)
                val bob = sin(state.planeProgress * 22f) * 0.08f
                setNormalizedTransform(
                    entity = cabin.asset.root,
                    model = cabin,
                    worldX = state.planeWorldX,
                    worldY = 95.0f - 0.12f + bob,
                    worldZ = state.planeWorldZ,
                    yawRad = 0f,
                    scaleMult = 1.0f
                )
            }

            // Render Player walking inside the Plane Cabin (visible in TPP)
            val jersey = loadedModels[playerJerseyKey]
            val isFpp = state.perspective == CameraPerspective.FIRST_PERSON
            if (jersey != null) {
                if (!isFpp) {
                    setAssetVisible(jersey, true)
                    setNormalizedTransform(
                        entity = jersey.asset.root,
                        model = jersey,
                        worldX = state.playerX,
                        worldY = state.playerY,
                        worldZ = state.playerZ,
                        yawRad = state.playerYawRad,
                        scaleMult = 1.0f
                    )
                } else {
                    setAssetVisible(jersey, false)
                }
            }

            // Render other bots standing/walking inside the Plane Cabin aisle
            renderEnemyPoolInstances(state, lodMaxDistance = 40f)

            camera.setProjection(64.0, aspect, 0.1, 450.0, Camera.Fov.VERTICAL)
            val fwdX = sin(state.playerYawRad)
            val fwdZ = cos(state.playerYawRad)
            val cosP = cos(state.playerPitchRad)
            val sinP = sin(state.playerPitchRad)
            if (isFpp) {
                val eyeY = state.playerY + 1.58f
                camera.lookAt(
                    state.playerX.toDouble(), eyeY.toDouble(), state.playerZ.toDouble(),
                    (state.playerX + fwdX * cosP * 18f).toDouble(),
                    (eyeY + sinP * 18f).toDouble(),
                    (state.playerZ + fwdZ * cosP * 18f).toDouble(),
                    0.0, 1.0, 0.0
                )
            } else {
                val camDist = 2.65f
                val camX = (state.playerX - fwdX * camDist).coerceIn(state.planeWorldX - 13.5f, state.planeWorldX + 13.5f)
                val camZ = (state.playerZ - fwdZ * camDist).coerceIn(state.planeWorldZ - 3.0f, state.planeWorldZ + 3.0f)
                val camY = (state.playerY + 1.85f - sinP * 0.8f).coerceIn(95.6f, 97.4f)
                camera.lookAt(
                    camX.toDouble(), camY.toDouble(), camZ.toDouble(),
                    (state.playerX + fwdX * cosP * 12f).toDouble(),
                    (state.playerY + 1.35f + sinP * 12f).toDouble(),
                    (state.playerZ + fwdZ * cosP * 12f).toDouble(),
                    0.0, 1.0, 0.0
                )
            }
            drawFrame(frameTimeNanos)
            return
        }

        loadedModels[GameCatalog.MODEL_PLANE_CABIN]?.let { setAssetVisible(it, false) }

        // Parachute canopy when gliding from the sky onto the map
        parachuteAsset?.let { chute ->
            if (state.phase == MatchPhase.PARACHUTE) {
                if (!parachuteInScene) {
                    scene.addEntities(chute.entities)
                    parachuteInScene = true
                }
                val ti = engine.transformManager.getInstance(chute.root)
                if (ti != 0) {
                    Matrix.setIdentityM(worldMat, 0)
                    Matrix.translateM(worldMat, 0, state.playerX, state.playerY + 2.8f, state.playerZ)
                    Matrix.rotateM(worldMat, 0, Math.toDegrees(state.playerYawRad.toDouble()).toFloat(), 0f, 1f, 0f)
                    engine.transformManager.setTransform(ti, worldMat)
                }
            } else if (parachuteInScene) {
                scene.removeEntities(chute.entities)
                parachuteInScene = false
            }
        }

        // Single Player Character (only 1 character!)
        val activeWeapon = state.weaponSlots.getOrNull(state.activeSlotIndex)?.spec
        val allGunPaths = listOf(
            "models/free_fire_awm_high_texture.glb",
            "models/mp40_cobra_evo_gun.glb",
            "models/free_fire_gun.glb"
        )
        allGunPaths.forEach { p -> loadedModels[p]?.let { setAssetVisible(it, false) } }

        val jersey = loadedModels[playerJerseyKey]
        val isFpp = state.perspective == CameraPerspective.FIRST_PERSON && state.activeVehicle == null

        if (jersey != null) {
            if (!isFpp && state.activeVehicle == null) {
                setAssetVisible(jersey, true)
                setNormalizedTransform(
                    entity = jersey.asset.root,
                    model = jersey,
                    worldX = state.playerX,
                    worldY = state.playerY,
                    worldZ = state.playerZ,
                    yawRad = state.playerYawRad,
                    scaleMult = 1.0f
                )
            } else {
                setAssetVisible(jersey, false)
            }
        }

        if (activeWeapon != null && state.activeVehicle == null && state.phase == MatchPhase.COMBAT) {
            val gunModel = loadedModels[activeWeapon.modelAsset]
            if (gunModel != null) {
                val hideForScope = isFpp && state.isAiming && activeWeapon.type == WeaponType.AWM
                if (!hideForScope) {
                    setAssetVisible(gunModel, true)
                    val fwdX = sin(state.playerYawRad)
                    val fwdZ = cos(state.playerYawRad)
                    val rightX = cos(state.playerYawRad)
                    val rightZ = -sin(state.playerYawRad)
                    if (isFpp) {
                        val lateral = if (state.isAiming) 0.0f else 0.24f
                        val forward = if (state.isAiming) 0.42f else 0.54f
                        val heightOff = if (state.isAiming) 1.48f else 1.38f
                        setNormalizedTransform(
                            entity = gunModel.asset.root,
                            model = gunModel,
                            worldX = state.playerX + fwdX * forward + rightX * lateral,
                            worldY = state.playerY + heightOff,
                            worldZ = state.playerZ + fwdZ * forward + rightZ * lateral,
                            yawRad = state.playerYawRad + (Math.PI.toFloat() * 0.5f),
                            scaleMult = 0.65f
                        )
                    } else {
                        setNormalizedTransform(
                            entity = gunModel.asset.root,
                            model = gunModel,
                            worldX = state.playerX + fwdX * 0.35f + rightX * 0.35f,
                            worldY = state.playerY + 1.08f,
                            worldZ = state.playerZ + fwdZ * 0.35f + rightZ * 0.35f,
                            yawRad = state.playerYawRad + (Math.PI.toFloat() * 0.5f),
                            scaleMult = 0.72f
                        )
                    }
                }
            }
        }

        // Enemies using 5 distinct single-character jersey variants
        renderEnemyPoolInstances(state, lodMaxDistance)

        // Pooled loot crates
        val nearbyLoot = state.lootItems
            .asSequence()
            .filter { !it.collected }
            .map { it to distanceXZ(state.playerX, state.playerZ, it.x, it.z) }
            .filter { it.second <= minOf(lodMaxDistance, 85f) }
            .sortedBy { it.second }
            .take(LOOT_POOL_SIZE)
            .toList()

        for (i in 0 until LOOT_POOL_SIZE) {
            val inst = lootInstances[i]
            if (i < nearbyLoot.size && inst != null) {
                val l = nearbyLoot[i].first
                if (!lootInScene[i]) {
                    scene.addEntities(inst.entities)
                    lootInScene[i] = true
                }
                val ti = engine.transformManager.getInstance(inst.root)
                if (ti != 0) {
                    Matrix.setIdentityM(worldMat, 0)
                    Matrix.translateM(worldMat, 0, l.x, l.y, l.z)
                    val spin = ((frameTimeNanos / 1_000_000L) % 3600L) * 0.1f
                    Matrix.rotateM(worldMat, 0, spin, 0f, 1f, 0f)
                    engine.transformManager.setTransform(ti, worldMat)
                }
            } else if (lootInScene[i] && inst != null) {
                scene.removeEntities(inst.entities)
                lootInScene[i] = false
            }
        }

        // Pooled tracer beams
        val activeTracers = state.tracers.take(TRACER_POOL_SIZE)
        for (i in 0 until TRACER_POOL_SIZE) {
            val inst = tracerInstances[i]
            if (i < activeTracers.size && inst != null) {
                val tr = activeTracers[i]
                if (!tracerInScene[i]) {
                    scene.addEntities(inst.entities)
                    tracerInScene[i] = true
                }
                val dx = tr.endX - tr.startX
                val dz = tr.endZ - tr.startZ
                val dy = tr.endY - tr.startY
                val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.1f)
                val yawDeg = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
                val ti = engine.transformManager.getInstance(inst.root)
                if (ti != 0) {
                    Matrix.setIdentityM(worldMat, 0)
                    Matrix.translateM(worldMat, 0, tr.startX, tr.startY, tr.startZ)
                    Matrix.rotateM(worldMat, 0, yawDeg, 0f, 1f, 0f)
                    Matrix.scaleM(worldMat, 0, if (tr.isSniper) 2.0f else 1.0f, 1.0f, len)
                    engine.transformManager.setTransform(ti, worldMat)
                }
            } else if (tracerInScene[i] && inst != null) {
                scene.removeEntities(inst.entities)
                tracerInScene[i] = false
            }
        }

        // Camera setup
        val fov = when {
            state.isAiming && activeWeapon != null -> activeWeapon.zoomFov.toDouble()
            state.isSprinting -> 68.0
            else -> 60.0
        }
        camera.setProjection(fov, aspect, 0.1, 450.0, Camera.Fov.VERTICAL)

        val fwdX = sin(state.playerYawRad)
        val fwdZ = cos(state.playerYawRad)
        val cosP = cos(state.playerPitchRad)
        val sinP = sin(state.playerPitchRad)

        when {
            state.activeVehicle != null -> {
                val dist = 7.5f
                camera.lookAt(
                    (state.playerX - fwdX * dist).toDouble(),
                    (state.playerY + 3.2f - sinP * 2.2f).toDouble(),
                    (state.playerZ - fwdZ * dist).toDouble(),
                    (state.playerX + fwdX * 10f).toDouble(),
                    (state.playerY + 1.2f).toDouble(),
                    (state.playerZ + fwdZ * 10f).toDouble(),
                    0.0, 1.0, 0.0
                )
            }
            state.phase == MatchPhase.FREEFALL || state.phase == MatchPhase.PARACHUTE -> {
                val dist = 6.5f
                camera.lookAt(
                    (state.playerX - fwdX * dist).toDouble(),
                    (state.playerY + 2.6f).toDouble(),
                    (state.playerZ - fwdZ * dist).toDouble(),
                    (state.playerX + fwdX * 8f).toDouble(),
                    (state.playerY - 2.5f).toDouble(),
                    (state.playerZ + fwdZ * 8f).toDouble(),
                    0.0, 1.0, 0.0
                )
            }
            isFpp -> {
                val eyeY = state.playerY + 1.62f
                camera.lookAt(
                    state.playerX.toDouble(), eyeY.toDouble(), state.playerZ.toDouble(),
                    (state.playerX + fwdX * cosP * 25f).toDouble(),
                    (eyeY + sinP * 25f).toDouble(),
                    (state.playerZ + fwdZ * cosP * 25f).toDouble(),
                    0.0, 1.0, 0.0
                )
            }
            else -> {
                val rightX = cos(state.playerYawRad)
                val rightZ = -sin(state.playerYawRad)
                val camDist = if (state.isAiming) 2.2f else 4.2f
                val shoulderOffset = if (state.isAiming) 0.55f else 0.65f
                val camX = state.playerX - fwdX * camDist + rightX * shoulderOffset
                val camZ = state.playerZ - fwdZ * camDist + rightZ * shoulderOffset
                val camY = (state.playerY + 2.15f - sinP * 1.8f).coerceAtLeast(0.6f)
                camera.lookAt(
                    camX.toDouble(), camY.toDouble(), camZ.toDouble(),
                    (state.playerX + fwdX * cosP * 22f).toDouble(),
                    (state.playerY + 1.45f + sinP * 22f).toDouble(),
                    (state.playerZ + fwdZ * cosP * 22f).toDouble(),
                    0.0, 1.0, 0.0
                )
            }
        }

        drawFrame(frameTimeNanos)
    }

    private fun renderEnemyPoolInstances(state: GameHudState, lodMaxDistance: Float) {
        val nearbyEnemies = state.remoteCombatants
            .asSequence()
            .filter { it.alive }
            .map { it to distanceXZ(state.playerX, state.playerZ, it.x, it.z) }
            .filter { it.second <= lodMaxDistance }
            .sortedBy { it.second }
            .take(ENEMY_POOL_SIZE)
            .toList()

        val usedPerPool = IntArray(enemyPools.size)
        if (enemyPools.isNotEmpty()) {
            for (pair in nearbyEnemies) {
                val c = pair.first
                val poolIdx = (kotlin.math.abs(c.id.hashCode()) % enemyPools.size)
                val slotIdx = usedPerPool[poolIdx]
                if (slotIdx >= INSTANCES_PER_VARIANT) continue
                usedPerPool[poolIdx]++
                val pool = enemyPools[poolIdx]
                val inst = pool.instances[slotIdx] ?: continue
                if (!pool.inScene[slotIdx]) {
                    scene.addEntities(inst.entities)
                    pool.inScene[slotIdx] = true
                }
                val ti = engine.transformManager.getInstance(inst.root)
                if (ti != 0) {
                    applyNormalizedMatrix(
                        ti = ti,
                        origMat = pool.originalRootMatrix,
                        worldX = c.x,
                        worldY = c.y,
                        worldZ = c.z,
                        yawRad = c.yawRad,
                        scale = pool.scale,
                        cx = pool.centerX,
                        minY = pool.minY,
                        cz = pool.centerZ
                    )
                }
            }
            for (pIdx in enemyPools.indices) {
                val pool = enemyPools[pIdx]
                for (sIdx in usedPerPool[pIdx] until INSTANCES_PER_VARIANT) {
                    val inst = pool.instances[sIdx]
                    if (pool.inScene[sIdx] && inst != null) {
                        scene.removeEntities(inst.entities)
                        pool.inScene[sIdx] = false
                    }
                }
            }
        }
    }

    private fun setShowcaseBackdropMode() {
        scene.skybox = skybox
        view.blendMode = View.BlendMode.OPAQUE
        setLobbyStageVisible(true)
        lobbyStageAsset?.let { stage ->
            val ti = engine.transformManager.getInstance(stage.root)
            if (ti != 0) {
                Matrix.setIdentityM(worldMat, 0)
                Matrix.translateM(worldMat, 0, 0f, -0.92f, 0f)
                engine.transformManager.setTransform(ti, worldMat)
            }
        }
    }

    private fun setLobbyStageVisible(visible: Boolean) {
        val stage = lobbyStageAsset ?: return
        if (lobbyStageInScene == visible) return
        if (visible) scene.addEntities(stage.entities) else scene.removeEntities(stage.entities)
        lobbyStageInScene = visible
    }

    private fun hideUnlistedModels(keepKeys: Set<String>) {
        loadedModels.forEach { (key, model) ->
            if (key !in keepKeys) setAssetVisible(model, false)
        }
    }

    private fun drawFrame(frameTimeNanos: Long) {
        if (!uiHelper.isReadyToRender) return
        val sc = swapChain ?: return
        if (renderer.beginFrame(sc, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    private fun setWorldVisible(visible: Boolean) {
        val wa = worldAsset
        if (wa != null && visible != worldInScene) {
            if (visible) scene.addEntities(wa.entities) else scene.removeEntities(wa.entities)
            worldInScene = visible
        }
        val sz = safeZoneAsset
        if (sz != null && visible != safeZoneInScene) {
            if (visible) scene.addEntities(sz.entities) else scene.removeEntities(sz.entities)
            safeZoneInScene = visible
        }
    }

    private fun hideEnemyPools() {
        for (pool in enemyPools) {
            for (i in pool.instances.indices) {
                val inst = pool.instances[i]
                if (pool.inScene[i] && inst != null) {
                    scene.removeEntities(inst.entities)
                    pool.inScene[i] = false
                }
            }
        }
    }

    private fun hideLootAndTracers() {
        for (i in 0 until LOOT_POOL_SIZE) {
            val inst = lootInstances[i]
            if (lootInScene[i] && inst != null) {
                scene.removeEntities(inst.entities)
                lootInScene[i] = false
            }
        }
        for (i in 0 until TRACER_POOL_SIZE) {
            val inst = tracerInstances[i]
            if (tracerInScene[i] && inst != null) {
                scene.removeEntities(inst.entities)
                tracerInScene[i] = false
            }
        }
    }

    private fun hideBattlegroundEntities() {
        setWorldVisible(false)
        parachuteAsset?.let {
            if (parachuteInScene) {
                scene.removeEntities(it.entities)
                parachuteInScene = false
            }
        }
        hideEnemyPools()
        hideLootAndTracers()
    }

    private fun setAssetVisible(model: NormalizedAsset, visible: Boolean) {
        if (model.inScene == visible) return
        if (visible) scene.addEntities(model.asset.entities) else scene.removeEntities(model.asset.entities)
        model.inScene = visible
    }

    private fun setNormalizedTransform(
        entity: Int,
        model: NormalizedAsset,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        yawRad: Float,
        scaleMult: Float
    ) {
        val ti = engine.transformManager.getInstance(entity)
        if (ti == 0) return
        applyNormalizedMatrix(
            ti = ti,
            origMat = model.originalRootMatrix,
            worldX = worldX,
            worldY = worldY,
            worldZ = worldZ,
            yawRad = yawRad,
            scale = model.baseScale * scaleMult,
            cx = model.centerX,
            minY = model.minY,
            cz = model.centerZ
        )
    }

    private fun setNormalizedTransformTilted(
        entity: Int,
        model: NormalizedAsset,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        yawRad: Float,
        rollDeg: Float,
        scaleMult: Float
    ) {
        val ti = engine.transformManager.getInstance(entity)
        if (ti == 0) return
        val s = model.baseScale * scaleMult
        Matrix.setIdentityM(worldMat, 0)
        Matrix.translateM(worldMat, 0, worldX, worldY, worldZ)
        Matrix.rotateM(worldMat, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(worldMat, 0, rollDeg, 0f, 0f, 1f)
        Matrix.scaleM(worldMat, 0, s, s, s)
        Matrix.translateM(worldMat, 0, -model.centerX, -model.minY, -model.centerZ)
        Matrix.multiplyMM(finalMat, 0, worldMat, 0, model.originalRootMatrix, 0)
        engine.transformManager.setTransform(ti, finalMat)
    }

    private fun applyNormalizedMatrix(
        ti: Int,
        origMat: FloatArray,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        yawRad: Float,
        scale: Float,
        cx: Float,
        minY: Float,
        cz: Float
    ) {
        Matrix.setIdentityM(worldMat, 0)
        Matrix.translateM(worldMat, 0, worldX, worldY, worldZ)
        Matrix.rotateM(worldMat, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(worldMat, 0, scale, scale, scale)
        Matrix.translateM(worldMat, 0, -cx, -minY, -cz)
        Matrix.multiplyMM(finalMat, 0, worldMat, 0, origMat, 0)
        engine.transformManager.setTransform(ti, finalMat)
    }

    private fun distanceXZ(ax: Float, az: Float, bx: Float, bz: Float): Float {
        val dx = ax - bx
        val dz = az - bz
        return sqrt(dx * dx + dz * dz)
    }

    fun destroy() {
        if (isDestroyed) return
        isDestroyed = true
        uiHelper.detach()
        swapChain?.let { engine.destroySwapChain(it); swapChain = null }
        loadedModels.values.forEach { assetLoader.destroyAsset(it.asset) }
        loadedModels.clear()
        lobbyStageAsset?.let { assetLoader.destroyAsset(it) }
        worldAsset?.let { assetLoader.destroyAsset(it) }
        safeZoneAsset?.let { assetLoader.destroyAsset(it) }
        parachuteAsset?.let { assetLoader.destroyAsset(it) }
        enemyPools.forEach { assetLoader.destroyAsset(it.masterAsset) }
        enemyPools.clear()
        lootMasterAsset?.let { assetLoader.destroyAsset(it) }
        tracerMasterAsset?.let { assetLoader.destroyAsset(it) }
        resourceLoader.destroy()
        assetLoader.destroy()
        ubershaderProvider.destroy()
        engine.destroyEntity(sunEntity)
        engine.destroyEntity(fillEntity)
        engine.destroyEntity(rimEntity)
        engine.destroyIndirectLight(indirectLight)
        engine.destroySkybox(skybox)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroy()
    }
}
