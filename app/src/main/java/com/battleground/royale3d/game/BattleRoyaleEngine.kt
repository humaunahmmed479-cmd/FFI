package com.battleground.royale3d.game

import com.battleground.royale3d.data.GameCatalog
import com.battleground.royale3d.model.BuildingCollider
import com.battleground.royale3d.model.BulletTracer
import com.battleground.royale3d.model.CameraPerspective
import com.battleground.royale3d.model.CombatantEntity
import com.battleground.royale3d.model.GameHudState
import com.battleground.royale3d.model.GameSettings
import com.battleground.royale3d.model.KillFeedEntry
import com.battleground.royale3d.model.LootEntity
import com.battleground.royale3d.model.LootKind
import com.battleground.royale3d.model.MatchPhase
import com.battleground.royale3d.model.SafeZoneState
import com.battleground.royale3d.model.VehicleEntity
import com.battleground.royale3d.model.VehicleType
import com.battleground.royale3d.model.WeaponSlotState
import com.battleground.royale3d.model.WeaponType
import com.battleground.royale3d.model.WsConnectionState
import com.battleground.royale3d.model.ZoneId
import com.battleground.royale3d.network.MultiplayerClient
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class BattleRoyaleEngine(
    private val colliders: List<BuildingCollider>,
    private val multiplayerClient: MultiplayerClient
) {
    private val rng = Random(System.currentTimeMillis())

    private var phase = MatchPhase.PLANE_CABIN
    private var perspective = CameraPerspective.THIRD_PERSON
    private var playerX = -240f
    private var playerY = 95f
    private var playerZ = -40f
    private var playerVy = 0f
    private var playerYawRad = 0.2f
    private var playerPitchRad = -0.15f
    private var health = 100
    private var shield = 25
    private var medkits = 1
    private var kills = 0
    private var isAiming = false
    private var isReloading = false
    private var reloadElapsed = 0f
    private var fireCooldown = 0f
    private var isFiring = false
    private var isSprinting = false
    private var planeWorldX = -240f
    private var planeWorldZ = -45f
    private var cabinLocalX = 0f
    private var cabinLocalZ = 0f
    private var cabinLocalY = 0f
    private var planeElapsed = 0f
    private val planeTotalSec = 30f
    private var matchSeconds = 0f
    private var hitMarkerTimer = 0f
    private var damageFlashTimer = 0f
    private var zoneDamageAccum = 0f
    private var botSkirmishTimer = 1.6f

    private var activeSlotIndex = 0
    private val weaponSlots = mutableListOf<WeaponSlotState>()
    private val lootItems = mutableListOf<LootEntity>()
    private val vehicles = mutableListOf<VehicleEntity>()
    private val combatants = mutableListOf<CombatantEntity>()
    private val cabinBotLocalOffsets = Array(14) { i ->
        floatArrayOf(
            -9.5f + (i % 7) * 3.0f,
            if (i < 7) -1.55f else 1.55f,
            if (i % 2 == 0) 0f else 3.14f
        )
    }
    private val tracers = mutableListOf<BulletTracer>()
    private val killFeed = ArrayDeque<KillFeedEntry>()

    private var safeZone = SafeZoneState()
    private val zonePhaseRadii = floatArrayOf(260f, 185f, 120f, 65f, 22f, 4f)

    // Continuous input state
    private var moveX = 0f
    private var moveY = 0f

    fun startMatch(settings: GameSettings): GameHudState {
        perspective = settings.defaultPerspective
        phase = MatchPhase.PLANE_CABIN
        planeElapsed = 0f
        matchSeconds = 0f
        planeWorldX = -240f
        planeWorldZ = -45f
        cabinLocalX = -2.5f
        cabinLocalZ = 0f
        cabinLocalY = 0f
        playerX = planeWorldX + cabinLocalX
        playerY = 95f
        playerZ = planeWorldZ + cabinLocalZ
        playerVy = 0f
        playerYawRad = 1.57f
        playerPitchRad = -0.06f
        health = 100
        shield = 0
        medkits = 1
        kills = 0
        isAiming = false
        isReloading = false
        reloadElapsed = 0f
        fireCooldown = 0f
        isFiring = false
        zoneDamageAccum = 0f
        hitMarkerTimer = 0f
        damageFlashTimer = 0f

        weaponSlots.clear()
        val starter = GameCatalog.weapons.getValue(WeaponType.BD_PISTOL)
        weaponSlots.add(WeaponSlotState(starter, starter.magSize, starter.initialReserve))
        activeSlotIndex = 0

        spawnLootTables()
        spawnVehicles()
        spawnBots99()
        tracers.clear()
        killFeed.clear()

        safeZone = SafeZoneState(
            phase = 1,
            currentCenterX = 0f,
            currentCenterZ = 0f,
            currentRadius = zonePhaseRadii[0],
            nextCenterX = rng.nextFloat() * 40f - 20f,
            nextCenterZ = rng.nextFloat() * 40f - 20f,
            nextRadius = zonePhaseRadii[1],
            secondsToShrink = 22f,
            isShrinking = false,
            damagePerSecond = 3
        )

        return buildHudState()
    }

    private fun spawnLootTables() {
        lootItems.clear()
        var idx = 0
        fun addLoot(kind: LootKind, weapon: WeaponType?, amount: Int, x: Float, z: Float, zone: ZoneId) {
            lootItems.add(
                LootEntity(
                    id = "loot_${idx++}",
                    kind = kind,
                    weaponType = weapon,
                    amount = amount,
                    x = x,
                    y = 0.05f,
                    z = z,
                    zone = zone
                )
            )
        }

        // Ride Area: MP40 Cobra Evo & SMG ammo
        for (i in 0 until 10) {
            val ang = i * 0.62f
            val r = 18f + (i % 3) * 18f
            val x = ZoneId.RIDE_AREA.centerX + cos(ang) * r
            val z = ZoneId.RIDE_AREA.centerZ + sin(ang) * r
            when (i % 3) {
                0 -> addLoot(LootKind.WEAPON, WeaponType.MP40_COBRA, 0, x, z, ZoneId.RIDE_AREA)
                1 -> addLoot(LootKind.SHIELD_VEST, null, 50, x, z, ZoneId.RIDE_AREA)
                else -> addLoot(LootKind.AMMO, null, 45, x, z, ZoneId.RIDE_AREA)
            }
        }

        // Central Plaza: High-Tier AWM Sniper + Rifle + Shields
        for (i in 0 until 12) {
            val ang = i * 0.52f
            val r = 16f + (i % 4) * 13f
            val x = ZoneId.CENTRAL_PLAZA.centerX + cos(ang) * r
            val z = ZoneId.CENTRAL_PLAZA.centerZ + sin(ang) * r
            when (i % 4) {
                0 -> addLoot(LootKind.WEAPON, WeaponType.AWM, 0, x, z, ZoneId.CENTRAL_PLAZA)
                1 -> addLoot(LootKind.WEAPON, WeaponType.FF_RIFLE, 0, x, z, ZoneId.CENTRAL_PLAZA)
                2 -> addLoot(LootKind.SHIELD_VEST, null, 75, x, z, ZoneId.CENTRAL_PLAZA)
                else -> addLoot(LootKind.MEDKIT, null, 1, x, z, ZoneId.CENTRAL_PLAZA)
            }
        }

        // Parking Lot: Assault Rifles & Ammo
        for (i in 0 until 9) {
            val x = ZoneId.PARKING.centerX + ((i % 3) - 1) * 24f
            val z = ZoneId.PARKING.centerZ + ((i / 3) - 1) * 24f
            when (i % 3) {
                0 -> addLoot(LootKind.WEAPON, WeaponType.FF_RIFLE, 0, x, z, ZoneId.PARKING)
                1 -> addLoot(LootKind.AMMO, null, 40, x, z, ZoneId.PARKING)
                else -> addLoot(LootKind.MEDKIT, null, 1, x, z, ZoneId.PARKING)
            }
        }

        // BD Sector: Tactical Crate mix (AWM, MP40, Rifle, Shield, Medkit)
        for (i in 0 until 12) {
            val ang = i * 0.55f
            val r = 15f + (i % 3) * 16f
            val x = ZoneId.BD_SECTOR.centerX + cos(ang) * r
            val z = ZoneId.BD_SECTOR.centerZ + sin(ang) * r
            when (i % 5) {
                0 -> addLoot(LootKind.WEAPON, WeaponType.AWM, 0, x, z, ZoneId.BD_SECTOR)
                1 -> addLoot(LootKind.WEAPON, WeaponType.MP40_COBRA, 0, x, z, ZoneId.BD_SECTOR)
                2 -> addLoot(LootKind.WEAPON, WeaponType.FF_RIFLE, 0, x, z, ZoneId.BD_SECTOR)
                3 -> addLoot(LootKind.SHIELD_VEST, null, 100, x, z, ZoneId.BD_SECTOR)
                else -> addLoot(LootKind.MEDKIT, null, 1, x, z, ZoneId.BD_SECTOR)
            }
        }

        // Park Outskirts: Medkits & Shields & Rifles
        for (i in 0 until 8) {
            val x = ZoneId.OUTSKIRTS.centerX + (rng.nextFloat() - 0.5f) * 70f
            val z = ZoneId.OUTSKIRTS.centerZ + (rng.nextFloat() - 0.5f) * 70f
            val kind = if (i % 2 == 0) LootKind.MEDKIT else LootKind.SHIELD_VEST
            addLoot(kind, null, if (kind == LootKind.MEDKIT) 1 else 50, x, z, ZoneId.OUTSKIRTS)
        }
    }

    private fun spawnVehicles() {
        vehicles.clear()
        vehicles.add(
            VehicleEntity(
                id = "veh_audi",
                spec = GameCatalog.vehicles.getValue(VehicleType.AUDI_R15),
                x = 92f,
                y = 0f,
                z = -84f,
                yawRad = 0.35f
            )
        )
        vehicles.add(
            VehicleEntity(
                id = "veh_bmw",
                spec = GameCatalog.vehicles.getValue(VehicleType.BMW_I8),
                x = 128f,
                y = 0f,
                z = -96f,
                yawRad = -0.45f
            )
        )
        vehicles.add(
            VehicleEntity(
                id = "veh_dvergr",
                spec = GameCatalog.vehicles.getValue(VehicleType.AJF12_DVERGR),
                x = 76f,
                y = 0f,
                z = 94f,
                yawRad = 1.1f
            )
        )
    }

    private fun spawnBots99() {
        combatants.clear()
        val prefixes = listOf("Dhaka", "Cobra", "Apex", "Bengal", "Ghost", "Nova", "Tiger", "Viper", "Sniper", "Shadow")
        val allWeapons = WeaponType.entries.toTypedArray()
        for (i in 1..99) {
            val angle = rng.nextFloat() * (2f * PI.toFloat())
            val radius = 32f + rng.nextFloat() * 210f
            val bx = cos(angle) * radius
            val bz = sin(angle) * radius
            val inCabin = i <= cabinBotLocalOffsets.size
            val cx = if (inCabin) planeWorldX + cabinBotLocalOffsets[i - 1][0] else bx
            val cy = if (inCabin) 95f else 0f
            val cz = if (inCabin) planeWorldZ + cabinBotLocalOffsets[i - 1][1] else bz
            val cyaw = if (inCabin) cabinBotLocalOffsets[i - 1][2] else rng.nextFloat() * 6.28f
            combatants.add(
                CombatantEntity(
                    id = "bot_$i",
                    callsign = "${prefixes[i % prefixes.size]}_${10 + i}",
                    x = cx,
                    y = cy,
                    z = cz,
                    yawRad = cyaw,
                    health = 100,
                    shield = if (i % 3 == 0) 50 else 0,
                    alive = true,
                    weaponType = allWeapons[i % allWeapons.size],
                    targetX = bx,
                    targetZ = bz,
                    fireCooldown = 1f + rng.nextFloat() * 2f
                )
            )
        }
    }

    fun setMoveInput(x: Float, y: Float) {
        moveX = x.coerceIn(-1f, 1f)
        moveY = y.coerceIn(-1f, 1f)
    }

    fun addLookDelta(dx: Float, dy: Float, sensitivity: Float) {
        val sens = (if (isAiming) 0.0022f else 0.0046f) * sensitivity
        playerYawRad -= dx * sens
        playerPitchRad = (playerPitchRad - dy * sens).coerceIn(-0.95f, 0.85f)
    }

    fun setFiring(firing: Boolean) {
        isFiring = firing
    }

    fun toggleAim() {
        if (phase == MatchPhase.COMBAT && activeVehicleIndex() == -1) {
            isAiming = !isAiming
        }
    }

    fun togglePerspective() {
        perspective = if (perspective == CameraPerspective.THIRD_PERSON) {
            CameraPerspective.FIRST_PERSON
        } else {
            CameraPerspective.THIRD_PERSON
        }
    }

    fun toggleSprint() {
        isSprinting = !isSprinting
    }

    fun ejectFromPlane() {
        if (phase != MatchPhase.PLANE_CABIN) return
        phase = MatchPhase.FREEFALL
        playerX = planeWorldX
        playerZ = planeWorldZ
        playerY = 95f
        playerVy = -4f
        isAiming = false
        scatterCabinBotsToIsland()
    }

    fun jumpOrEject() {
        when (phase) {
            MatchPhase.PLANE_CABIN -> {
                if (cabinLocalY <= 0.05f) {
                    playerVy = 6.2f
                }
            }
            MatchPhase.FREEFALL -> {
                phase = MatchPhase.PARACHUTE
                playerVy = -5.5f
            }
            MatchPhase.COMBAT -> {
                if (activeVehicleIndex() == -1 && playerY <= 0.05f) {
                    playerVy = 7.4f
                }
            }
            else -> Unit
        }
    }

    private fun scatterCabinBotsToIsland() {
        for (i in 0 until min(cabinBotLocalOffsets.size, combatants.size)) {
            val bot = combatants[i]
            combatants[i] = bot.copy(
                x = bot.targetX,
                y = 0f,
                z = bot.targetZ
            )
        }
    }

    fun triggerReload() {
        if (phase != MatchPhase.COMBAT || isReloading) return
        val slot = weaponSlots.getOrNull(activeSlotIndex) ?: return
        if (slot.magAmmo >= slot.spec.magSize || slot.reserveAmmo <= 0) return
        isReloading = true
        reloadElapsed = 0f
        isAiming = false
    }

    fun selectWeaponSlot(index: Int) {
        if (index in weaponSlots.indices && index != activeSlotIndex) {
            activeSlotIndex = index
            isReloading = false
            reloadElapsed = 0f
            isAiming = false
        }
    }

    fun useMedkit() {
        if (phase != MatchPhase.COMBAT || medkits <= 0 || health >= 100) return
        medkits -= 1
        health = min(100, health + 55)
    }

    fun interactLootOrVehicle() {
        if (phase != MatchPhase.COMBAT) return
        val activeVehIdx = activeVehicleIndex()
        if (activeVehIdx != -1) {
            // Exit vehicle safely to the left
            val v = vehicles[activeVehIdx]
            vehicles[activeVehIdx] = v.copy(occupiedByPlayer = false, speedMs = 0f)
            playerX = v.x - cos(v.yawRad) * 2.8f
            playerZ = v.z + sin(v.yawRad) * 2.8f
            playerY = 0f
            return
        }

        // Check nearby loot first
        val nearLoot = findNearestLoot()
        if (nearLoot != null) {
            collectLoot(nearLoot)
            return
        }

        // Otherwise enter nearest vehicle
        val nearVehIdx = findNearestVehicleIndex()
        if (nearVehIdx != -1) {
            val v = vehicles[nearVehIdx]
            if (v.health > 0) {
                vehicles[nearVehIdx] = v.copy(occupiedByPlayer = true, doorOpen = false)
                isAiming = false
                isReloading = false
                playerX = v.x
                playerZ = v.z
                playerYawRad = v.yawRad
            }
        }
    }

    fun toggleNearestVehicleDoor() {
        val activeIdx = activeVehicleIndex()
        val targetIdx = if (activeIdx != -1) activeIdx else findNearestVehicleIndex()
        if (targetIdx != -1) {
            val v = vehicles[targetIdx]
            vehicles[targetIdx] = v.copy(doorOpen = !v.doorOpen)
        }
    }

    private fun collectLoot(loot: LootEntity) {
        val idx = lootItems.indexOfFirst { it.id == loot.id }
        if (idx == -1) return
        lootItems[idx] = loot.copy(collected = true)
        when (loot.kind) {
            LootKind.WEAPON -> {
                val wType = loot.weaponType ?: return
                val spec = GameCatalog.weapons.getValue(wType)
                val existingIdx = weaponSlots.indexOfFirst { it.spec.type == wType }
                if (existingIdx != -1) {
                    val existing = weaponSlots[existingIdx]
                    weaponSlots[existingIdx] = existing.copy(
                        reserveAmmo = existing.reserveAmmo + spec.magSize * 2
                    )
                    activeSlotIndex = existingIdx
                } else if (weaponSlots.size < 3) {
                    weaponSlots.add(WeaponSlotState(spec, spec.magSize, spec.initialReserve))
                    activeSlotIndex = weaponSlots.lastIndex
                } else {
                    weaponSlots[activeSlotIndex] = WeaponSlotState(spec, spec.magSize, spec.initialReserve)
                }
                isReloading = false
            }
            LootKind.AMMO -> {
                val cur = weaponSlots.getOrNull(activeSlotIndex) ?: return
                weaponSlots[activeSlotIndex] = cur.copy(reserveAmmo = cur.reserveAmmo + loot.amount)
            }
            LootKind.SHIELD_VEST -> {
                shield = (shield + loot.amount).coerceAtMost(100)
            }
            LootKind.MEDKIT -> {
                medkits += loot.amount
            }
        }
    }

    fun step(dtRaw: Float): GameHudState {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        if (phase == MatchPhase.VICTORY || phase == MatchPhase.ELIMINATED) {
            return buildHudState()
        }

        matchSeconds += dt
        if (hitMarkerTimer > 0f) hitMarkerTimer = max(0f, hitMarkerTimer - dt)
        if (damageFlashTimer > 0f) damageFlashTimer = max(0f, damageFlashTimer - dt)

        // Age tracers
        val iter = tracers.iterator()
        while (iter.hasNext()) {
            val t = iter.next()
            if (t.ttlSec - dt <= 0f) iter.remove()
        }
        for (i in tracers.indices) {
            tracers[i] = tracers[i].copy(ttlSec = tracers[i].ttlSec - dt)
        }

        when (phase) {
            MatchPhase.PLANE_CABIN -> {
                planeElapsed += dt
                val progress = (planeElapsed / planeTotalSec).coerceIn(0f, 1f)
                planeWorldX = -240f + progress * 480f
                planeWorldZ = -45f + progress * 95f

                // Player walks & jumps freely inside the 3D Plane Cabin aisle
                val fwdX = sin(playerYawRad)
                val fwdZ = cos(playerYawRad)
                val rightX = cos(playerYawRad)
                val rightZ = -sin(playerYawRad)
                val cabinWalkSpeed = 5.4f
                cabinLocalX = (cabinLocalX + (fwdX * moveY + rightX * moveX) * cabinWalkSpeed * dt).coerceIn(-11.8f, 11.8f)
                cabinLocalZ = (cabinLocalZ + (fwdZ * moveY + rightZ * moveX) * cabinWalkSpeed * dt).coerceIn(-2.3f, 2.3f)
                if (cabinLocalY > 0f || playerVy > 0f) {
                    playerVy -= 20f * dt
                    cabinLocalY += playerVy * dt
                    if (cabinLocalY <= 0f) {
                        cabinLocalY = 0f
                        playerVy = 0f
                    }
                }
                playerX = planeWorldX + cabinLocalX
                playerY = 95f + cabinLocalY
                playerZ = planeWorldZ + cabinLocalZ

                // Animate the 14 cabin bots walking/idling along the cabin aisle
                for (i in 0 until min(cabinBotLocalOffsets.size, combatants.size)) {
                    val off = cabinBotLocalOffsets[i]
                    val phaseAng = matchSeconds * (0.7f + (i % 3) * 0.25f) + i
                    val lx = (off[0] + sin(phaseAng) * 1.4f).coerceIn(-11.2f, 11.2f)
                    val lz = (off[1] + cos(phaseAng * 0.8f) * 0.45f).coerceIn(-2.3f, 2.3f)
                    val lyaw = off[2] + sin(phaseAng * 0.5f) * 0.6f
                    combatants[i] = combatants[i].copy(
                        x = planeWorldX + lx,
                        y = 95f,
                        z = planeWorldZ + lz,
                        yawRad = lyaw
                    )
                }

                if (progress >= 1f) {
                    ejectFromPlane()
                }
            }
            MatchPhase.FREEFALL -> {
                val fwdX = sin(playerYawRad)
                val fwdZ = cos(playerYawRad)
                val speed = 22f
                playerX = (playerX + (fwdX * moveY + cos(playerYawRad) * moveX) * speed * dt).coerceIn(-270f, 270f)
                playerZ = (playerZ + (fwdZ * moveY - sin(playerYawRad) * moveX) * speed * dt).coerceIn(-270f, 270f)
                playerVy = (playerVy - 16f * dt).coerceAtLeast(-28f)
                playerY += playerVy * dt
                if (playerY <= 26f) {
                    phase = MatchPhase.PARACHUTE
                    playerVy = -5.8f
                }
            }
            MatchPhase.PARACHUTE -> {
                val fwdX = sin(playerYawRad)
                val fwdZ = cos(playerYawRad)
                val glideSpeed = 16f
                playerX = (playerX + (fwdX * (0.4f + moveY * 0.6f) + cos(playerYawRad) * moveX * 0.5f) * glideSpeed * dt).coerceIn(-270f, 270f)
                playerZ = (playerZ + (fwdZ * (0.4f + moveY * 0.6f) - sin(playerYawRad) * moveX * 0.5f) * glideSpeed * dt).coerceIn(-270f, 270f)
                playerVy = -5.5f
                playerY += playerVy * dt
                if (playerY <= 0f) {
                    playerY = 0f
                    playerVy = 0f
                    phase = MatchPhase.COMBAT
                }
            }
            MatchPhase.COMBAT -> {
                stepPlayerMovementAndVehicle(dt)
                stepCombatAndReload(dt)
                stepSafeZone(dt)
                stepBotsAndNetwork(dt)
            }
            else -> Unit
        }

        // Send WebSocket position sync when connected
        val activeWep = weaponSlots.getOrNull(activeSlotIndex)?.spec?.type?.id ?: "bd_pistol"
        multiplayerClient.sendPlayerState(playerX, playerY, playerZ, playerYawRad, activeWep, health)

        // Check victory condition
        val aliveTotal = 1 + combatants.count { it.alive }
        if (health <= 0 && phase != MatchPhase.ELIMINATED) {
            health = 0
            phase = MatchPhase.ELIMINATED
        } else if (aliveTotal <= 1 && health > 0 && phase == MatchPhase.COMBAT) {
            phase = MatchPhase.VICTORY
        }

        return buildHudState()
    }

    private fun stepPlayerMovementAndVehicle(dt: Float) {
        val vehIdx = activeVehicleIndex()
        if (vehIdx != -1) {
            val v = vehicles[vehIdx]
            var newSpeed = v.speedMs
            var newFuel = v.fuel
            var newYaw = v.yawRad

            if (v.fuel > 0f && v.health > 0) {
                if (moveY > 0.08f) {
                    newSpeed = min(v.spec.maxSpeedMs, newSpeed + v.spec.acceleration * moveY * dt)
                } else if (moveY < -0.08f) {
                    newSpeed = max(-v.spec.maxSpeedMs * 0.45f, newSpeed + v.spec.acceleration * moveY * 1.3f * dt)
                } else {
                    newSpeed *= (1f - 2.2f * dt).coerceAtLeast(0f)
                }
                if (kotlin.math.abs(newSpeed) > 0.8f && kotlin.math.abs(moveX) > 0.05f) {
                    val dirSign = if (newSpeed >= 0f) 1f else -1f
                    newYaw -= moveX * v.spec.turnRateRad * dirSign * dt
                }
                newFuel = max(0f, newFuel - (kotlin.math.abs(newSpeed) / v.spec.maxSpeedMs) * v.spec.fuelBurnPerSec * dt)
            } else {
                newSpeed *= (1f - 3f * dt).coerceAtLeast(0f)
            }

            val nextX = v.x + sin(newYaw) * newSpeed * dt
            val nextZ = v.z + cos(newYaw) * newSpeed * dt
            val resolved = resolveCollisions(nextX, nextZ, v.spec.collisionRadius)
            vehicles[vehIdx] = v.copy(
                x = resolved.first,
                z = resolved.second,
                yawRad = newYaw,
                speedMs = newSpeed,
                fuel = newFuel
            )
            playerX = resolved.first
            playerZ = resolved.second
            playerY = 0f
            playerYawRad = newYaw
            return
        }

        // On-foot movement + gravity
        val baseSpeed = when {
            isAiming -> 3.6f
            isSprinting && moveY > 0.4f -> 9.6f
            else -> 6.8f
        }
        val fwdX = sin(playerYawRad)
        val fwdZ = cos(playerYawRad)
        val rightX = cos(playerYawRad)
        val rightZ = -sin(playerYawRad)

        val nextX = playerX + (fwdX * moveY + rightX * moveX) * baseSpeed * dt
        val nextZ = playerZ + (fwdZ * moveY + rightZ * moveX) * baseSpeed * dt
        val resolved = resolveCollisions(nextX, nextZ, 0.55f)
        playerX = resolved.first
        playerZ = resolved.second

        if (playerY > 0f || playerVy > 0f) {
            playerVy -= 20f * dt
            playerY += playerVy * dt
            if (playerY <= 0f) {
                playerY = 0f
                playerVy = 0f
            }
        }
    }

    private fun resolveCollisions(x: Float, z: Float, radius: Float): Pair<Float, Float> {
        var rx = x.coerceIn(-272f, 272f)
        var rz = z.coerceIn(-272f, 272f)
        for (b in colliders) {
            val closestX = rx.coerceIn(b.minX, b.maxX)
            val closestZ = rz.coerceIn(b.minZ, b.maxZ)
            val dx = rx - closestX
            val dz = rz - closestZ
            val distSq = dx * dx + dz * dz
            if (distSq < radius * radius) {
                val dist = sqrt(distSq).coerceAtLeast(0.0001f)
                val overlap = radius - dist
                if (distSq < 0.0001f) {
                    rx += radius
                } else {
                    rx += (dx / dist) * overlap
                    rz += (dz / dist) * overlap
                }
            }
        }
        return rx to rz
    }

    private fun stepCombatAndReload(dt: Float) {
        if (fireCooldown > 0f) fireCooldown = max(0f, fireCooldown - dt)
        val slot = weaponSlots.getOrNull(activeSlotIndex) ?: return

        if (isReloading) {
            reloadElapsed += dt
            if (reloadElapsed >= slot.spec.reloadTimeSec) {
                val needed = slot.spec.magSize - slot.magAmmo
                val taken = min(needed, slot.reserveAmmo)
                weaponSlots[activeSlotIndex] = slot.copy(
                    magAmmo = slot.magAmmo + taken,
                    reserveAmmo = slot.reserveAmmo - taken
                )
                isReloading = false
                reloadElapsed = 0f
            }
            return
        }

        if (isFiring && fireCooldown <= 0f && activeVehicleIndex() == -1) {
            if (slot.magAmmo <= 0) {
                triggerReload()
                return
            }
            fireActiveShot(slot)
        }
    }

    private fun fireActiveShot(slot: WeaponSlotState) {
        fireCooldown = slot.spec.fireRateSec
        weaponSlots[activeSlotIndex] = slot.copy(magAmmo = slot.magAmmo - 1)

        // Apply subtle vertical recoil
        val recoilRad = Math.toRadians(slot.spec.recoilDegrees.toDouble()).toFloat() * 0.35f
        playerPitchRad = (playerPitchRad + recoilRad).coerceIn(-0.95f, 0.85f)

        val cosP = cos(playerPitchRad)
        val dirX = sin(playerYawRad) * cosP
        val dirY = sin(playerPitchRad)
        val dirZ = cos(playerYawRad) * cosP

        val originX = playerX
        val originY = playerY + 1.52f
        val originZ = playerZ
        var bestDist = slot.spec.rangeMeters
        var hitBotIndex = -1
        var hitVehIndex = -1

        // Ray-sphere intersection against alive combatants
        for (i in combatants.indices) {
            val c = combatants[i]
            if (!c.alive) continue
            val ocX = c.x - originX
            val ocY = (c.y + 1.05f) - originY
            val ocZ = c.z - originZ
            val proj = ocX * dirX + ocY * dirY + ocZ * dirZ
            if (proj <= 0f || proj >= bestDist) continue
            val closestX = originX + dirX * proj - c.x
            val closestY = originY + dirY * proj - (c.y + 1.05f)
            val closestZ = originZ + dirZ * proj - c.z
            val perpSq = closestX * closestX + closestY * closestY + closestZ * closestZ
            val hitRadius = if (isAiming) 0.95f else 1.25f
            if (perpSq <= hitRadius * hitRadius) {
                bestDist = proj
                hitBotIndex = i
                hitVehIndex = -1
            }
        }

        // Ray-sphere against vehicles
        for (i in vehicles.indices) {
            val v = vehicles[i]
            if (v.health <= 0) continue
            val ocX = v.x - originX
            val ocY = (v.y + 1.0f) - originY
            val ocZ = v.z - originZ
            val proj = ocX * dirX + ocY * dirY + ocZ * dirZ
            if (proj <= 0f || proj >= bestDist) continue
            val cx = originX + dirX * proj - v.x
            val cz = originZ + dirZ * proj - v.z
            if (cx * cx + cz * cz <= v.spec.collisionRadius * v.spec.collisionRadius) {
                bestDist = proj
                hitVehIndex = i
                hitBotIndex = -1
            }
        }

        tracers.add(
            BulletTracer(
                id = "tr_${System.nanoTime()}",
                startX = originX + dirX * 0.7f,
                startY = originY - 0.1f,
                startZ = originZ + dirZ * 0.7f,
                endX = originX + dirX * bestDist,
                endY = originY + dirY * bestDist,
                endZ = originZ + dirZ * bestDist,
                ttlSec = 0.14f,
                isSniper = slot.spec.type == WeaponType.AWM
            )
        )

        if (hitBotIndex != -1) {
            hitMarkerTimer = 0.22f
            val target = combatants[hitBotIndex]
            var dmg = slot.spec.damage
            val newShield = max(0, target.shield - dmg)
            dmg = max(0, dmg - target.shield)
            val newHp = max(0, target.health - dmg)
            val survived = newHp > 0
            combatants[hitBotIndex] = target.copy(
                health = newHp,
                shield = newShield,
                alive = survived
            )
            if (!survived) {
                kills += 1
                pushKillFeed(
                    KillFeedEntry(
                        id = "kf_${System.nanoTime()}",
                        killer = "YOU",
                        victim = target.callsign,
                        weaponName = slot.spec.nameEn,
                        isPlayerInvolved = true
                    )
                )
            }
        } else if (hitVehIndex != -1) {
            hitMarkerTimer = 0.18f
            val v = vehicles[hitVehIndex]
            vehicles[hitVehIndex] = v.copy(health = max(0, v.health - slot.spec.damage))
        }

        multiplayerClient.sendFireEvent(playerX, playerY, playerZ, playerYawRad, slot.spec.type.id)
    }

    private fun stepSafeZone(dt: Float) {
        val sz = safeZone
        if (!sz.isShrinking) {
            val nextSec = sz.secondsToShrink - dt
            if (nextSec <= 0f) {
                safeZone = sz.copy(isShrinking = true, secondsToShrink = 0f)
            } else {
                safeZone = sz.copy(secondsToShrink = nextSec)
            }
        } else {
            val shrinkRate = 7.5f * dt
            val newRadius = max(sz.nextRadius, sz.currentRadius - shrinkRate)
            val dx = sz.nextCenterX - sz.currentCenterX
            val dz = sz.nextCenterZ - sz.currentCenterZ
            val newCx = sz.currentCenterX + dx * 0.25f * dt
            val newCz = sz.currentCenterZ + dz * 0.25f * dt
            if (newRadius <= sz.nextRadius + 0.1f) {
                val nextPhase = min(sz.phase + 1, zonePhaseRadii.lastIndex)
                safeZone = SafeZoneState(
                    phase = nextPhase,
                    currentCenterX = newCx,
                    currentCenterZ = newCz,
                    currentRadius = newRadius,
                    nextCenterX = newCx + (rng.nextFloat() - 0.5f) * 24f,
                    nextCenterZ = newCz + (rng.nextFloat() - 0.5f) * 24f,
                    nextRadius = zonePhaseRadii[nextPhase],
                    secondsToShrink = 18f,
                    isShrinking = false,
                    damagePerSecond = 3 + nextPhase * 2
                )
            } else {
                safeZone = sz.copy(
                    currentCenterX = newCx,
                    currentCenterZ = newCz,
                    currentRadius = newRadius
                )
            }
        }

        // Apply ticking zone damage outside circle
        val distToCenter = distXZ(playerX, playerZ, safeZone.currentCenterX, safeZone.currentCenterZ)
        if (distToCenter > safeZone.currentRadius) {
            zoneDamageAccum += safeZone.damagePerSecond * dt
            if (zoneDamageAccum >= 1f) {
                val dmg = zoneDamageAccum.toInt()
                zoneDamageAccum -= dmg
                applyDamageToPlayer(dmg)
            }
        } else {
            zoneDamageAccum = 0f
        }
    }

    private fun stepBotsAndNetwork(dt: Float) {
        // Periodic AI skirmishes across the 100-player island so alive count progresses naturally
        botSkirmishTimer -= dt
        if (botSkirmishTimer <= 0f) {
            botSkirmishTimer = 1.9f + rng.nextFloat() * 1.4f
            val aliveBots = combatants.indices.filter { combatants[it].alive }
            if (aliveBots.size > 2) {
                val killerIdx = aliveBots[rng.nextInt(aliveBots.size)]
                var victimIdx = aliveBots[rng.nextInt(aliveBots.size)]
                if (victimIdx == killerIdx) victimIdx = aliveBots.first { it != killerIdx }
                val killer = combatants[killerIdx]
                val victim = combatants[victimIdx]
                val distVictimFromZone = distXZ(victim.x, victim.z, safeZone.currentCenterX, safeZone.currentCenterZ)
                val playerDist = distXZ(playerX, playerZ, victim.x, victim.z)
                // Don't arbitrarily vanish a bot right in front of the player
                if (playerDist > 42f || distVictimFromZone > safeZone.currentRadius) {
                    combatants[victimIdx] = victim.copy(alive = false, health = 0)
                    val wName = GameCatalog.weapons.getValue(killer.weaponType).nameEn
                    pushKillFeed(
                        KillFeedEntry(
                            id = "kf_${System.nanoTime()}",
                            killer = killer.callsign,
                            victim = victim.callsign,
                            weaponName = wName,
                            isPlayerInvolved = false
                        )
                    )
                }
            }
        }

        // Move & engage nearby bots
        for (i in combatants.indices) {
            val bot = combatants[i]
            if (!bot.alive) continue

            val distToPlayer = distXZ(bot.x, bot.z, playerX, playerZ)
            val distToZone = distXZ(bot.x, bot.z, safeZone.currentCenterX, safeZone.currentCenterZ)

            // Steer toward safe zone if outside or wandering
            var goalX = bot.targetX
            var goalZ = bot.targetZ
            if (distToZone > safeZone.currentRadius * 0.85f) {
                goalX = safeZone.currentCenterX + (rng.nextFloat() - 0.5f) * 30f
                goalZ = safeZone.currentCenterZ + (rng.nextFloat() - 0.5f) * 30f
            } else if (distToPlayer < 48f) {
                goalX = playerX
                goalZ = playerZ
            }

            val dx = goalX - bot.x
            val dz = goalZ - bot.z
            val d = sqrt(dx * dx + dz * dz).coerceAtLeast(0.01f)
            val yaw = atan2(dx, dz)

            var nx = bot.x
            var nz = bot.z
            if (d > 6f) {
                val speed = 4.6f
                nx = (bot.x + (dx / d) * speed * dt).coerceIn(-270f, 270f)
                nz = (bot.z + (dz / d) * speed * dt).coerceIn(-270f, 270f)
            }

            var cd = bot.fireCooldown - dt
            if (distToPlayer < 46f && cd <= 0f && phase == MatchPhase.COMBAT) {
                cd = 1.4f + rng.nextFloat() * 1.1f
                tracers.add(
                    BulletTracer(
                        id = "btr_${System.nanoTime()}",
                        startX = nx,
                        startY = 1.4f,
                        startZ = nz,
                        endX = playerX,
                        endY = playerY + 1.2f,
                        endZ = playerZ,
                        ttlSec = 0.12f
                    )
                )
                if (rng.nextFloat() < 0.38f) {
                    applyDamageToPlayer(7)
                }
            }

            combatants[i] = bot.copy(
                x = nx,
                z = nz,
                yawRad = yaw,
                targetX = goalX,
                targetZ = goalZ,
                fireCooldown = cd
            )
        }
    }

    private fun applyDamageToPlayer(rawDamage: Int) {
        damageFlashTimer = 0.25f
        val vehIdx = activeVehicleIndex()
        if (vehIdx != -1) {
            val v = vehicles[vehIdx]
            if (v.health > 0) {
                vehicles[vehIdx] = v.copy(health = max(0, v.health - rawDamage))
                return
            }
        }
        val absorbed = min(shield, rawDamage)
        shield -= absorbed
        val rem = rawDamage - absorbed
        health = max(0, health - rem)
    }

    private fun pushKillFeed(entry: KillFeedEntry) {
        killFeed.addFirst(entry)
        while (killFeed.size > 4) killFeed.removeLast()
    }

    private fun activeVehicleIndex(): Int = vehicles.indexOfFirst { it.occupiedByPlayer }

    private fun findNearestLoot(): LootEntity? {
        return lootItems
            .asSequence()
            .filter { !it.collected && distXZ(playerX, playerZ, it.x, it.z) <= 3.6f }
            .minByOrNull { distXZ(playerX, playerZ, it.x, it.z) }
    }

    private fun findNearestVehicleIndex(): Int {
        var bestIdx = -1
        var bestDist = 5.2f
        for (i in vehicles.indices) {
            val v = vehicles[i]
            val d = distXZ(playerX, playerZ, v.x, v.z)
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        return bestIdx
    }

    private fun detectCurrentZone(): ZoneId {
        return ZoneId.entries.minByOrNull { distXZ(playerX, playerZ, it.centerX, it.centerZ) }
            ?: ZoneId.CENTRAL_PLAZA
    }

    private fun buildHudState(): GameHudState {
        val distCenter = distXZ(playerX, playerZ, safeZone.currentCenterX, safeZone.currentCenterZ)
        val outside = distCenter > safeZone.currentRadius
        val distOutside = max(0, (distCenter - safeZone.currentRadius).toInt())
        val angleToZone = atan2(safeZone.currentCenterX - playerX, safeZone.currentCenterZ - playerZ)

        val activeVeh = activeVehicleIndex().let { if (it != -1) vehicles[it] else null }
        val nearVeh = if (activeVeh == null) {
            findNearestVehicleIndex().let { if (it != -1) vehicles[it] else null }
        } else null

        val activeSpec = weaponSlots.getOrNull(activeSlotIndex)?.spec
        val reloadProgress = if (isReloading && activeSpec != null) {
            (reloadElapsed / activeSpec.reloadTimeSec).coerceIn(0f, 1f)
        } else 0f

        // Merge WebSocket peers + local bots
        val wsPeers = multiplayerClient.remotePeers.values.map { p ->
            CombatantEntity(
                id = p.playerId,
                callsign = p.playerId,
                x = p.x,
                y = p.y,
                z = p.z,
                yawRad = p.yaw,
                health = p.health,
                alive = p.health > 0,
                isRemotePlayer = true
            )
        }

        val aliveTotal = 1 + combatants.count { it.alive } + wsPeers.count { it.alive }
        val wsState = if (multiplayerClient.connectionState.value == WsConnectionState.CONNECTED) {
            WsConnectionState.CONNECTED
        } else {
            multiplayerClient.connectionState.value
        }

        return GameHudState(
            phase = phase,
            perspective = perspective,
            playerX = playerX,
            playerY = playerY,
            playerZ = playerZ,
            playerYawRad = playerYawRad,
            playerPitchRad = playerPitchRad,
            health = health,
            shield = shield,
            medkits = medkits,
            aliveCount = aliveTotal,
            kills = kills,
            placement = aliveTotal,
            activeSlotIndex = activeSlotIndex,
            weaponSlots = weaponSlots.toList(),
            isAiming = isAiming,
            isReloading = isReloading,
            reloadProgress = reloadProgress,
            isSprinting = isSprinting,
            currentZone = detectCurrentZone(),
            safeZone = safeZone,
            isOutsideSafeZone = outside,
            distanceToSafeZoneMeters = distOutside,
            angleToSafeZoneRad = angleToZone,
            nearbyLoot = findNearestLoot(),
            nearbyVehicle = nearVeh,
            activeVehicle = activeVeh,
            planeProgress = (planeElapsed / planeTotalSec).coerceIn(0f, 1f),
            planeWorldX = planeWorldX,
            planeWorldZ = planeWorldZ,
            altitudeMeters = playerY.toInt().coerceAtLeast(0),
            killFeed = killFeed.toList(),
            remoteCombatants = wsPeers + combatants,
            vehicles = vehicles.toList(),
            lootItems = lootItems.toList(),
            tracers = tracers.toList(),
            wsState = wsState,
            hitMarkerFlash = hitMarkerTimer > 0f,
            damageFlash = damageFlashTimer > 0f,
            matchDurationSec = matchSeconds.toInt()
        )
    }

    private fun distXZ(ax: Float, az: Float, bx: Float, bz: Float): Float {
        val dx = ax - bx
        val dz = az - bz
        return sqrt(dx * dx + dz * dz)
    }
}
