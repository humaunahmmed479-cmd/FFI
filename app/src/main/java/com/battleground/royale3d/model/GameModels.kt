package com.battleground.royale3d.model

import kotlinx.serialization.Serializable

enum class WeaponType(val id: String) {
    AWM("awm"),
    MP40_COBRA("mp40_cobra"),
    FF_RIFLE("ff_rifle"),
    BD_PISTOL("bd_pistol")
}

data class WeaponSpec(
    val type: WeaponType,
    val nameEn: String,
    val nameBn: String,
    val categoryEn: String,
    val categoryBn: String,
    val modelAsset: String,
    val damage: Int,
    val rangeMeters: Float,
    val fireRateSec: Float,
    val magSize: Int,
    val recoilDegrees: Float,
    val reloadTimeSec: Float,
    val zoomFov: Float,
    val initialReserve: Int
)

enum class VehicleType(val id: String) {
    AUDI_R15("audi_r15"),
    BMW_I8("bmw_i8"),
    AJF12_DVERGR("ajf12_dvergr")
}

data class VehicleSpec(
    val type: VehicleType,
    val nameEn: String,
    val nameBn: String,
    val roleEn: String,
    val roleBn: String,
    val modelAsset: String,
    val maxSpeedMs: Float,
    val maxHealth: Int,
    val acceleration: Float,
    val turnRateRad: Float,
    val fuelBurnPerSec: Float,
    val collisionRadius: Float
)

enum class ZoneId(
    val nameEn: String,
    val nameBn: String,
    val centerX: Float,
    val centerZ: Float,
    val radius: Float,
    val lootQualityEn: String,
    val lootQualityBn: String
) {
    RIDE_AREA("Ride Area", "রাইড এলাকা", -115f, -95f, 85f, "SMG & Speed", "এসএমজি ও দ্রুত লুট"),
    PARKING("Parking Lot", "পার্কিং", 115f, -90f, 80f, "Vehicles & AR", "যানবাহন ও রাইফেল"),
    CENTRAL_PLAZA("Central Plaza", "মূল প্লাজা", 0f, 0f, 75f, "High-Tier Sniper", "হাই-টিয়ার স্নাইপার"),
    BD_SECTOR("BD Sector", "বাংলাদেশ জোন", 105f, 110f, 75f, "Tactical Crate", "ট্যাকটিক্যাল লুট"),
    OUTSKIRTS("Park Outskirts", "পার্ক সীমান্ত", -95f, 110f, 80f, "Shield & Meds", "শিল্ড ও মেডকিট")
}

enum class MatchPhase {
    PLANE_CABIN,
    FREEFALL,
    PARACHUTE,
    COMBAT,
    VICTORY,
    ELIMINATED
}

enum class CameraPerspective {
    THIRD_PERSON,
    FIRST_PERSON
}

enum class GraphicsQuality {
    LOW,
    MEDIUM,
    HIGH
}

enum class AppLanguage {
    ENGLISH,
    BENGALI
}

data class GameSettings(
    val lookSensitivity: Float = 1.0f,
    val defaultPerspective: CameraPerspective = CameraPerspective.THIRD_PERSON,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    val language: AppLanguage = AppLanguage.BENGALI,
    val hapticsEnabled: Boolean = true,
    val wsServerUrl: String = "wss://echo.websocket.events",
    val vaultUnlockedIds: Set<String> = setOf("jersey", "ff_gun"),
    val equippedWeaponId: String = "ff_gun",
    val equippedJerseyVariant: Int = 0
)

data class WeaponSlotState(
    val spec: WeaponSpec,
    val magAmmo: Int,
    val reserveAmmo: Int
)

enum class LootKind {
    WEAPON,
    AMMO,
    SHIELD_VEST,
    MEDKIT
}

data class LootEntity(
    val id: String,
    val kind: LootKind,
    val weaponType: WeaponType? = null,
    val amount: Int = 0,
    val x: Float,
    val y: Float,
    val z: Float,
    val zone: ZoneId,
    val collected: Boolean = false
)

data class VehicleEntity(
    val id: String,
    val spec: VehicleSpec,
    val x: Float,
    val y: Float,
    val z: Float,
    val yawRad: Float,
    val speedMs: Float = 0f,
    val health: Int = spec.maxHealth,
    val fuel: Float = 100f,
    val doorOpen: Boolean = false,
    val occupiedByPlayer: Boolean = false
)

data class CombatantEntity(
    val id: String,
    val callsign: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val yawRad: Float,
    val health: Int = 100,
    val shield: Int = 50,
    val alive: Boolean = true,
    val isRemotePlayer: Boolean = false,
    val weaponType: WeaponType = WeaponType.FF_RIFLE,
    val targetX: Float = x,
    val targetZ: Float = z,
    val fireCooldown: Float = 0f
)

data class BulletTracer(
    val id: String,
    val startX: Float,
    val startY: Float,
    val startZ: Float,
    val endX: Float,
    val endY: Float,
    val endZ: Float,
    val ttlSec: Float = 0.14f,
    val isSniper: Boolean = false
)

data class KillFeedEntry(
    val id: String,
    val killer: String,
    val victim: String,
    val weaponName: String,
    val isPlayerInvolved: Boolean
)

data class BuildingCollider(
    val id: String,
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float,
    val height: Float
)

data class SafeZoneState(
    val phase: Int = 1,
    val currentCenterX: Float = 0f,
    val currentCenterZ: Float = 0f,
    val currentRadius: Float = 260f,
    val nextCenterX: Float = 12f,
    val nextCenterZ: Float = -10f,
    val nextRadius: Float = 185f,
    val secondsToShrink: Float = 25f,
    val isShrinking: Boolean = false,
    val damagePerSecond: Int = 3
)

enum class WsConnectionState {
    LOCAL_SIMULATION,
    CONNECTING,
    CONNECTED,
    DISCONNECTED_FALLBACK
}

data class GameHudState(
    val phase: MatchPhase = MatchPhase.PLANE_CABIN,
    val perspective: CameraPerspective = CameraPerspective.THIRD_PERSON,
    val playerX: Float = -240f,
    val playerY: Float = 95f,
    val playerZ: Float = 0f,
    val playerYawRad: Float = 0f,
    val playerPitchRad: Float = -0.15f,
    val health: Int = 100,
    val shield: Int = 0,
    val medkits: Int = 1,
    val aliveCount: Int = 100,
    val kills: Int = 0,
    val placement: Int = 100,
    val activeSlotIndex: Int = 0,
    val weaponSlots: List<WeaponSlotState> = emptyList(),
    val isAiming: Boolean = false,
    val isReloading: Boolean = false,
    val reloadProgress: Float = 0f,
    val isSprinting: Boolean = false,
    val currentZone: ZoneId = ZoneId.CENTRAL_PLAZA,
    val safeZone: SafeZoneState = SafeZoneState(),
    val isOutsideSafeZone: Boolean = false,
    val distanceToSafeZoneMeters: Int = 0,
    val angleToSafeZoneRad: Float = 0f,
    val nearbyLoot: LootEntity? = null,
    val nearbyVehicle: VehicleEntity? = null,
    val activeVehicle: VehicleEntity? = null,
    val planeProgress: Float = 0f,
    val planeWorldX: Float = -240f,
    val planeWorldZ: Float = -45f,
    val altitudeMeters: Int = 95,
    val killFeed: List<KillFeedEntry> = emptyList(),
    val remoteCombatants: List<CombatantEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val lootItems: List<LootEntity> = emptyList(),
    val tracers: List<BulletTracer> = emptyList(),
    val wsState: WsConnectionState = WsConnectionState.LOCAL_SIMULATION,
    val hitMarkerFlash: Boolean = false,
    val damageFlash: Boolean = false,
    val matchDurationSec: Int = 0
)

data class ArmoryModelItem(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val subtitleEn: String,
    val subtitleBn: String,
    val modelAsset: String,
    val category: String,
    val weaponSpec: WeaponSpec? = null,
    val vehicleSpec: VehicleSpec? = null
)

@Serializable
data class NetPacket(
    val type: String,
    val playerId: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val yaw: Float = 0f,
    val weapon: String = "ff_rifle",
    val health: Int = 100
)
