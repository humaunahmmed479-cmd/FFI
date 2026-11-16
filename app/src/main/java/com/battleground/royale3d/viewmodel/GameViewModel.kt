package com.battleground.royale3d.viewmodel

import android.app.Application
import android.view.Choreographer
import android.view.TextureView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battleground.royale3d.data.GameCatalog
import com.battleground.royale3d.data.SettingsRepository
import com.battleground.royale3d.engine3d.FilamentWorldController
import com.battleground.royale3d.engine3d.GlbCharacterExtractor
import com.battleground.royale3d.engine3d.ProceduralGlbBuilder
import com.battleground.royale3d.game.BattleRoyaleEngine
import com.battleground.royale3d.model.AppLanguage
import com.battleground.royale3d.model.ArmoryModelItem
import com.battleground.royale3d.model.CameraPerspective
import com.battleground.royale3d.model.GameHudState
import com.battleground.royale3d.model.GameSettings
import com.battleground.royale3d.model.GraphicsQuality
import com.battleground.royale3d.model.WsConnectionState
import com.battleground.royale3d.network.MultiplayerClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RenderTargetMode {
    NONE,
    LOBBY_SHOWCASE,
    ARMORY_SHOWCASE,
    BATTLEGROUND
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    val multiplayerClient = MultiplayerClient()
    private val worldController = FilamentWorldController(application)

    private var engine: BattleRoyaleEngine? = null
    private var assetsPreloaded = false

    val settings: StateFlow<GameSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GameSettings()
    )

    val wsConnectionState: StateFlow<WsConnectionState> = multiplayerClient.connectionState
    val wsPingMs: StateFlow<Int?> = multiplayerClient.lastPingMs

    private val _hudState = MutableStateFlow(GameHudState())
    val hudState: StateFlow<GameHudState> = _hudState.asStateFlow()

    private val _selectedArmoryItem = MutableStateFlow(GameCatalog.armoryItems.first())
    val selectedArmoryItem: StateFlow<ArmoryModelItem> = _selectedArmoryItem.asStateFlow()

    private val _isWorldReady = MutableStateFlow(false)
    val isWorldReady: StateFlow<Boolean> = _isWorldReady.asStateFlow()

    private var renderMode = RenderTargetMode.NONE
    private var showcaseOrbitYawDeg = 8f
    private var showcaseOrbitPitchDeg = 6f
    private var lastFrameNanos = 0L

    private fun playerJerseyKey(): String = "jersey_variant_${settings.value.equippedJerseyVariant.coerceIn(0, 5)}"
    private fun equippedWeaponModelAsset(): String = when (settings.value.equippedWeaponId) {
        "awm" -> "models/free_fire_awm_high_texture.glb"
        "mp40" -> "models/mp40_cobra_evo_gun.glb"
        else -> "models/free_fire_gun.glb"
    }

    private val choreographer = Choreographer.getInstance()
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (renderMode == RenderTargetMode.NONE) return
            val dt = if (lastFrameNanos == 0L) 0.016f else (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = frameTimeNanos

            when (renderMode) {
                RenderTargetMode.LOBBY_SHOWCASE -> {
                    worldController.renderLobbyShowcase(
                        playerJerseyKey = playerJerseyKey(),
                        equippedWeaponAsset = equippedWeaponModelAsset(),
                        orbitYawDeg = showcaseOrbitYawDeg,
                        orbitPitchDeg = showcaseOrbitPitchDeg,
                        frameTimeNanos = frameTimeNanos
                    )
                }
                RenderTargetMode.ARMORY_SHOWCASE -> {
                    val item = _selectedArmoryItem.value
                    val key = if (item.id == "jersey") playerJerseyKey() else item.modelAsset
                    val zoom = when (item.category) {
                        "VEHICLE" -> 5.4f
                        "AIRCRAFT" -> 7.6f
                        "WEAPON" -> 2.4f
                        else -> 2.9f
                    }
                    worldController.renderArmoryShowcase(
                        assetKey = key,
                        orbitYawDeg = showcaseOrbitYawDeg,
                        orbitPitchDeg = showcaseOrbitPitchDeg,
                        zoomDistance = zoom,
                        frameTimeNanos = frameTimeNanos
                    )
                }
                RenderTargetMode.BATTLEGROUND -> {
                    val activeEngine = engine
                    if (activeEngine != null) {
                        val updated = activeEngine.step(dt)
                        _hudState.value = updated
                        worldController.renderBattleground(
                            state = updated,
                            playerJerseyKey = playerJerseyKey(),
                            quality = settings.value.graphicsQuality,
                            frameTimeNanos = frameTimeNanos
                        )
                    }
                }
                RenderTargetMode.NONE -> Unit
            }
            choreographer.postFrameCallback(this)
        }
    }

    init {
        preload3DAssets()
    }

    private fun preload3DAssets() {
        if (assetsPreloaded) return
        viewModelScope.launch {
            // Stage 1: Extract SINGLE player character (variant 0) from multi-jersey GLB for immediate Lobby view
            val jerseyBytes = withContext(Dispatchers.IO) {
                worldController.readAssetBytes(GameCatalog.MODEL_JERSEY)
            }
            if (jerseyBytes != null) {
                val mainPlayerBuf = withContext(Dispatchers.Default) {
                    GlbCharacterExtractor.extractSingleCharacter(jerseyBytes, 0)
                }
                if (mainPlayerBuf != null) {
                    worldController.installModelFromBuffer("jersey_variant_0", mainPlayerBuf, 1.85f)
                }
            }

            // Stage 2: Load guns, vehicles, plane cabin, enemy variants & Theme Park world
            val stage2Models = listOf(
                "models/free_fire_awm_high_texture.glb" to 1.25f,
                "models/mp40_cobra_evo_gun.glb" to 0.78f,
                "models/free_fire_gun.glb" to 0.85f,
                GameCatalog.MODEL_PLANE_CABIN to 34.0f,
                "models/2009_audi_r15_tdi.glb" to 4.6f,
                "models/bmw_i8.glb" to 4.5f,
                "models/ajf-12_dvergr.glb" to 5.4f
            )
            val loadedBytes = withContext(Dispatchers.IO) {
                stage2Models.map { (path, size) ->
                    Triple(path, worldController.readAssetBytes(path), size)
                }
            }
            val worldResult = withContext(Dispatchers.Default) {
                ProceduralGlbBuilder.buildThemeParkWorld()
            }

            // Install additional unlockable player jersey variants (1..5) so Vault equip switches immediately
            if (jerseyBytes != null) {
                for (v in 1..5) {
                    val vBuf = withContext(Dispatchers.Default) {
                        GlbCharacterExtractor.extractSingleCharacter(jerseyBytes, v)
                    }
                    if (vBuf != null) {
                        worldController.installModelFromBuffer("jersey_variant_$v", vBuf, 1.85f)
                    }
                }
            }

            for ((path, bytes, size) in loadedBytes) {
                if (bytes != null) {
                    worldController.installModelFromBuffer(
                        path,
                        worldController.bytesToDirectBuffer(bytes),
                        size
                    )
                }
            }

            worldController.installWorldAndEnemyPools(worldResult.glbBuffer, jerseyBytes)
            engine = BattleRoyaleEngine(worldResult.colliders, multiplayerClient)
            assetsPreloaded = true
            _isWorldReady.value = true
        }
    }

    fun attachTextureView(textureView: TextureView, mode: RenderTargetMode) {
        worldController.attachTextureView(textureView)
        renderMode = mode
        lastFrameNanos = 0L
        choreographer.removeFrameCallback(frameCallback)
        choreographer.postFrameCallback(frameCallback)
    }

    fun detachSurface() {
        renderMode = RenderTargetMode.NONE
        choreographer.removeFrameCallback(frameCallback)
        worldController.detachSurface()
    }

    fun orbitShowcase(dx: Float, dy: Float) {
        showcaseOrbitYawDeg = (showcaseOrbitYawDeg + dx * 0.35f) % 360f
        showcaseOrbitPitchDeg = (showcaseOrbitPitchDeg + dy * 0.25f).coerceIn(-15f, 38f)
    }

    fun selectArmoryItem(item: ArmoryModelItem) {
        _selectedArmoryItem.value = item
        showcaseOrbitYawDeg = 22f
        showcaseOrbitPitchDeg = 8f
    }

    fun selectArmoryItemById(id: String) {
        val found = GameCatalog.armoryItems.firstOrNull { it.id == id } ?: return
        selectArmoryItem(found)
    }

    // Event & Vault persistence
    fun claimToVault(itemId: String) {
        viewModelScope.launch {
            settingsRepo.updateSettings {
                it.copy(vaultUnlockedIds = it.vaultUnlockedIds + itemId)
            }
        }
    }

    fun equipFromVault(itemId: String) {
        viewModelScope.launch {
            settingsRepo.updateSettings { current ->
                if (itemId.startsWith("skin_")) {
                    val v = itemId.removePrefix("skin_").toIntOrNull()?.coerceIn(0, 5) ?: 0
                    current.copy(
                        vaultUnlockedIds = current.vaultUnlockedIds + itemId,
                        equippedJerseyVariant = v
                    )
                } else {
                    current.copy(
                        vaultUnlockedIds = current.vaultUnlockedIds + itemId,
                        equippedWeaponId = itemId
                    )
                }
            }
        }
    }

    fun startNewMatch() {
        val currentSettings = settings.value
        val activeEngine = engine ?: return
        _hudState.value = activeEngine.startMatch(currentSettings)
        if (currentSettings.wsServerUrl.isNotBlank()) {
            multiplayerClient.connect(currentSettings.wsServerUrl)
        }
    }

    // In-game controls forwarded cleanly to BattleRoyaleEngine
    fun setMoveInput(x: Float, y: Float) = engine?.setMoveInput(x, y)
    fun addLookDelta(dx: Float, dy: Float) =
        engine?.addLookDelta(dx, dy, settings.value.lookSensitivity)

    fun setFiring(firing: Boolean) = engine?.setFiring(firing)
    fun toggleAim() = engine?.toggleAim()
    fun togglePerspective() = engine?.togglePerspective()
    fun toggleSprint() = engine?.toggleSprint()
    fun jumpOrEject() = engine?.jumpOrEject()
    fun ejectFromPlane() = engine?.ejectFromPlane()
    fun triggerReload() = engine?.triggerReload()
    fun selectWeaponSlot(idx: Int) = engine?.selectWeaponSlot(idx)
    fun useMedkit() = engine?.useMedkit()
    fun interactLootOrVehicle() = engine?.interactLootOrVehicle()
    fun toggleVehicleDoor() = engine?.toggleNearestVehicleDoor()

    // Settings updates
    fun setSensitivity(value: Float) {
        viewModelScope.launch { settingsRepo.updateSettings { it.copy(lookSensitivity = value) } }
    }

    fun setDefaultPerspective(p: CameraPerspective) {
        viewModelScope.launch { settingsRepo.updateSettings { it.copy(defaultPerspective = p) } }
    }

    fun setGraphicsQuality(q: GraphicsQuality) {
        viewModelScope.launch { settingsRepo.updateSettings { it.copy(graphicsQuality = q) } }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { settingsRepo.updateSettings { it.copy(language = lang) } }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateSettings { it.copy(hapticsEnabled = enabled) } }
    }

    fun setWsServerUrl(url: String) {
        viewModelScope.launch { settingsRepo.updateSettings { it.copy(wsServerUrl = url) } }
    }

    fun testWebSocketConnection(url: String) {
        multiplayerClient.connect(url)
    }

    override fun onCleared() {
        super.onCleared()
        renderMode = RenderTargetMode.NONE
        choreographer.removeFrameCallback(frameCallback)
        multiplayerClient.disconnect()
        worldController.destroy()
    }
}
