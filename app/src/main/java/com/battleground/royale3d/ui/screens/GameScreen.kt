package com.battleground.royale3d.ui.screens

import android.view.KeyEvent
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Paragliding
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.battleground.royale3d.data.L10n
import com.battleground.royale3d.model.CameraPerspective
import com.battleground.royale3d.model.GameSettings
import com.battleground.royale3d.model.LootKind
import com.battleground.royale3d.model.MatchPhase
import com.battleground.royale3d.model.WsConnectionState
import com.battleground.royale3d.ui.theme.BlazeAmber
import com.battleground.royale3d.ui.theme.CrimsonPulse
import com.battleground.royale3d.ui.theme.CyberCyan
import com.battleground.royale3d.ui.theme.GunmetalElevated
import com.battleground.royale3d.ui.theme.GunmetalSurface
import com.battleground.royale3d.ui.theme.ObsidianBg
import com.battleground.royale3d.ui.theme.SafeEmerald
import com.battleground.royale3d.ui.theme.TacticalBorder
import com.battleground.royale3d.viewmodel.GameViewModel
import com.battleground.royale3d.viewmodel.RenderTargetMode

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    settings: GameSettings,
    onExitToMenu: () -> Unit
) {
    val hud by viewModel.hudState.collectAsStateWithLifecycle()
    val lang = settings.language
    val focusRequester = remember { FocusRequester() }
    val pressedKeys = remember { mutableSetOf<Int>() }
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        showExitConfirm = !showExitConfirm
    }

    LaunchedEffect(Unit) {
        viewModel.startNewMatch()
        runCatching { focusRequester.requestFocus() }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setFiring(false)
            viewModel.setMoveInput(0f, 0f)
            viewModel.detachSurface()
        }
    }

    fun syncKeyboardMovement() {
        var x = 0f
        var y = 0f
        if (KeyEvent.KEYCODE_W in pressedKeys || KeyEvent.KEYCODE_DPAD_UP in pressedKeys) y += 1f
        if (KeyEvent.KEYCODE_S in pressedKeys || KeyEvent.KEYCODE_DPAD_DOWN in pressedKeys) y -= 1f
        if (KeyEvent.KEYCODE_A in pressedKeys || KeyEvent.KEYCODE_DPAD_LEFT in pressedKeys) x -= 1f
        if (KeyEvent.KEYCODE_D in pressedKeys || KeyEvent.KEYCODE_DPAD_RIGHT in pressedKeys) x += 1f
        viewModel.setMoveInput(x, y)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                val code = ev.nativeKeyEvent.keyCode
                if (ev.type == KeyEventType.KeyDown) {
                    pressedKeys.add(code)
                    syncKeyboardMovement()
                    when (code) {
                        KeyEvent.KEYCODE_SPACE -> {
                            if (hud.phase == MatchPhase.PLANE_CABIN) viewModel.ejectFromPlane() else viewModel.jumpOrEject()
                            true
                        }
                        KeyEvent.KEYCODE_R -> { viewModel.triggerReload(); true }
                        KeyEvent.KEYCODE_E -> { viewModel.interactLootOrVehicle(); true }
                        KeyEvent.KEYCODE_F -> { viewModel.toggleVehicleDoor(); true }
                        KeyEvent.KEYCODE_V -> { viewModel.togglePerspective(); true }
                        KeyEvent.KEYCODE_1 -> { viewModel.selectWeaponSlot(0); true }
                        KeyEvent.KEYCODE_2 -> { viewModel.selectWeaponSlot(1); true }
                        KeyEvent.KEYCODE_3 -> { viewModel.selectWeaponSlot(2); true }
                        else -> false
                    }
                } else if (ev.type == KeyEventType.KeyUp) {
                    pressedKeys.remove(code)
                    syncKeyboardMovement()
                    false
                } else false
            }
    ) {
        // 3D Filament Battleground TextureView
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { tv ->
                    viewModel.attachTextureView(tv, RenderTargetMode.BATTLEGROUND)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Touch-drag Look Layer (covers playfield behind HUD buttons)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.addLookDelta(dragAmount.x, dragAmount.y)
                    }
                }
        )

        // Damage flash vignette
        if (hud.damageFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(8.dp, CrimsonPulse.copy(alpha = 0.7f))
            )
        }

        // Crosshair / Sniper Scope
        if (hud.phase == MatchPhase.COMBAT && hud.activeVehicle == null) {
            CrosshairAndScopeOverlay(hud = hud)
        }

        // Top & Bottom Tactical HUD
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD ROW: Mini-map + Safe Zone Timer + Alive/Kills + Camera/Exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalMiniMap(hud = hud, lang = lang)

                    // Safe Zone & Kill Feed Column
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier.border(
                                1.dp,
                                if (hud.isOutsideSafeZone) CrimsonPulse else CyberCyan,
                                RoundedCornerShape(10.dp)
                            ),
                            color = GunmetalSurface.copy(alpha = 0.86f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                val zoneText = if (hud.safeZone.isShrinking) {
                                    L10n.pick(lang, "ZONE SHRINKING", "জোন সংকুচিত হচ্ছে")
                                } else {
                                    L10n.pick(
                                        lang,
                                        "Zone P${hud.safeZone.phase}: ${hud.safeZone.secondsToShrink.toInt()}s",
                                        "জোন P${hud.safeZone.phase}: ${hud.safeZone.secondsToShrink.toInt()}সে."
                                    )
                                }
                                Text(
                                    text = zoneText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (hud.isOutsideSafeZone) CrimsonPulse else CyberCyan
                                )
                                if (hud.isOutsideSafeZone) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Outside Zone",
                                            tint = CrimsonPulse,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = L10n.pick(
                                                lang,
                                                "Safe Zone ${hud.distanceToSafeZoneMeters}m",
                                                "সেফ জোন ${hud.distanceToSafeZoneMeters}মি."
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrimsonPulse
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Kill Feed
                        hud.killFeed.take(2).forEach { entry ->
                            Surface(
                                color = ObsidianBg.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${entry.killer} ⚡ ${entry.victim}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.isPlayerInvolved) BlazeAmber else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Top Right: Alive / Kills / Network Badge / TPP-FPP Switch / Exit
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HudBadge(
                            label = L10n.pick(lang, "ALIVE", "জীবিত"),
                            value = "${hud.aliveCount}",
                            accent = BlazeAmber
                        )
                        HudBadge(
                            label = L10n.pick(lang, "KILLS", "কিল"),
                            value = "${hud.kills}",
                            accent = SafeEmerald
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { viewModel.togglePerspective() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GunmetalSurface.copy(alpha = 0.85f))
                                .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                                .testTag("toggle_perspective_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwitchCamera,
                                contentDescription = "Switch TPP/FPP",
                                tint = CyberCyan
                            )
                        }
                        IconButton(
                            onClick = { showExitConfirm = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GunmetalSurface.copy(alpha = 0.85f))
                                .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                                .testTag("exit_match_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Exit Match",
                                tint = CrimsonPulse
                            )
                        }
                    }

                    Text(
                        text = if (hud.wsState == WsConnectionState.CONNECTED) {
                            "WS LIVE • ${if (hud.perspective == CameraPerspective.FIRST_PERSON) "FPP" else "TPP"}"
                        } else {
                            "100-BOT ARENA • ${if (hud.perspective == CameraPerspective.FIRST_PERSON) "FPP" else "TPP"}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                }
            }

            // CENTER CONTEXT BANNERS (Plane Cabin / Freefall / Parachute / Loot / Vehicle Prompts)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (hud.phase) {
                    MatchPhase.PLANE_CABIN -> {
                        // Slim non-blocking flight bar at top-center so player & bots inside cabin stay visible
                        Surface(
                            modifier = Modifier.border(1.dp, BlazeAmber.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                            color = GunmetalSurface.copy(alpha = 0.80f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = BlazeAmber, modifier = Modifier.size(18.dp))
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "PLANE CABIN • ALT ${hud.altitudeMeters}m • WALK & JUMP READY",
                                        "প্লেন কেবিন • উচ্চতা ${hud.altitudeMeters}মি. • ভেতরে হাঁটুন ও লাফ দিন"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BlazeAmber
                                )
                                LinearProgressIndicator(
                                    progress = { hud.planeProgress },
                                    modifier = Modifier.width(84.dp).height(5.dp),
                                    color = CyberCyan,
                                    trackColor = GunmetalElevated
                                )
                            }
                        }
                    }
                    MatchPhase.FREEFALL -> {
                        Button(
                            onClick = { viewModel.jumpOrEject() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = ObsidianBg),
                            modifier = Modifier.height(48.dp).testTag("open_parachute_button")
                        ) {
                            Icon(Icons.Default.Paragliding, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "DEPLOY PARACHUTE (${hud.altitudeMeters}m)",
                                    "প্যারাশুট খুলুন (${hud.altitudeMeters}মি.)"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    MatchPhase.PARACHUTE -> {
                        Surface(
                            color = GunmetalSurface.copy(alpha = 0.80f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "GLIDING • ALT ${hud.altitudeMeters}m",
                                    "প্যারাশুট গ্লাইড • ${hud.altitudeMeters}মি."
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                    else -> Unit
                }

                // Active Vehicle Telemetry
                hud.activeVehicle?.let { veh ->
                    val kmh = kotlin.math.abs((veh.speedMs * 3.6f).toInt())
                    Surface(
                        modifier = Modifier.border(1.dp, SafeEmerald, RoundedCornerShape(12.dp)),
                        color = GunmetalSurface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$kmh km/h",
                                style = MaterialTheme.typography.titleLarge,
                                color = CyberCyan
                            )
                            Text(
                                text = "HP ${veh.health}/${veh.spec.maxHealth}",
                                style = MaterialTheme.typography.labelLarge,
                                color = SafeEmerald
                            )
                            Text(
                                text = "FUEL ${veh.fuel.toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = BlazeAmber
                            )
                            Text(
                                text = if (veh.doorOpen) "DOOR: OPEN" else "DOOR: SHUT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                // Contextual Loot / Vehicle Interact Pill (E)
                val nearbyLoot = hud.nearbyLoot
                val nearbyVehicle = hud.nearbyVehicle
                if (hud.phase == MatchPhase.COMBAT && (nearbyLoot != null || nearbyVehicle != null || hud.activeVehicle != null)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val actionLabel = when {
                            hud.activeVehicle != null -> L10n.pick(lang, "EXIT VEHICLE (E)", "গাড়ি থেকে নামুন (E)")
                            nearbyLoot != null -> {
                                val what = when (nearbyLoot.kind) {
                                    LootKind.WEAPON -> nearbyLoot.weaponType?.name ?: "WEAPON"
                                    LootKind.AMMO -> "+${nearbyLoot.amount} AMMO"
                                    LootKind.SHIELD_VEST -> "+${nearbyLoot.amount} SHIELD"
                                    LootKind.MEDKIT -> "+1 MEDKIT"
                                }
                                L10n.pick(lang, "LOOT $what (E)", "লুট নিন: $what (E)")
                            }
                            nearbyVehicle != null -> L10n.pick(
                                lang,
                                "DRIVE ${nearbyVehicle.spec.nameEn} (E)",
                                "গাড়িতে উঠুন: ${nearbyVehicle.spec.nameBn}"
                            )
                            else -> ""
                        }

                        Button(
                            onClick = { viewModel.interactLootOrVehicle() },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeEmerald, contentColor = ObsidianBg),
                            modifier = Modifier.height(48.dp).testTag("interact_button")
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(actionLabel, fontWeight = FontWeight.Bold)
                        }

                        if (nearbyVehicle != null || hud.activeVehicle != null) {
                            OutlinedButton(
                                onClick = { viewModel.toggleVehicleDoor() },
                                modifier = Modifier.height(48.dp).testTag("door_toggle_button")
                            ) {
                                Icon(Icons.Default.DoorSliding, contentDescription = "Toggle Door")
                                Spacer(Modifier.width(4.dp))
                                Text(L10n.pick(lang, "DOOR (F)", "দরজা (F)"))
                            }
                        }
                    }
                }
            }

            // BOTTOM CONTROLS: Left Joystick + Health/Shield + Weapon Slots + Right Fire/Aim/Reload/Jump
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                VirtualJoystick(
                    onMove = { mx, my -> viewModel.setMoveInput(mx, my) }
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hud.isReloading) {
                        LinearProgressIndicator(
                            progress = { hud.reloadProgress },
                            modifier = Modifier.width(140.dp).height(6.dp),
                            color = BlazeAmber,
                            trackColor = GunmetalElevated
                        )
                    }
                    WeaponLoadoutBar(
                        hud = hud,
                        lang = lang,
                        onSelectSlot = { idx -> viewModel.selectWeaponSlot(idx) }
                    )
                    HealthAndShieldPanel(
                        health = hud.health,
                        shield = hud.shield,
                        medkits = hud.medkits,
                        lang = lang,
                        onUseMedkit = { viewModel.useMedkit() }
                    )
                }

                // Right Action Cluster: Eject button in Plane, or Aim/Reload/Jump/Fire on Map
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hud.phase == MatchPhase.PLANE_CABIN) {
                        Button(
                            onClick = { viewModel.ejectFromPlane() },
                            colors = ButtonDefaults.buttonColors(containerColor = BlazeAmber, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(54.dp).testTag("eject_plane_button")
                        ) {
                            Icon(Icons.Default.FlightTakeoff, contentDescription = "Jump From Plane")
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "JUMP PLANE", "প্লেন থেকে লাফ"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionCircleButton(
                            label = "ADS",
                            active = hud.isAiming,
                            accent = CyberCyan,
                            onClick = { viewModel.toggleAim() },
                            testTag = "aim_button"
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = "Aim", tint = if (hud.isAiming) ObsidianBg else CyberCyan)
                        }
                        ActionCircleButton(
                            label = "R",
                            active = hud.isReloading,
                            accent = BlazeAmber,
                            onClick = { viewModel.triggerReload() },
                            testTag = "reload_button"
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = if (hud.isReloading) ObsidianBg else BlazeAmber)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionCircleButton(
                            label = "JUMP",
                            active = false,
                            accent = SafeEmerald,
                            onClick = { viewModel.jumpOrEject() },
                            testTag = "jump_button"
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Jump", tint = SafeEmerald)
                        }

                        // Hold-to-fire tactical trigger
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(CrimsonPulse.copy(alpha = 0.88f))
                                .border(2.5.dp, BlazeAmber, CircleShape)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        viewModel.setFiring(true)
                                        while (true) {
                                            val ev = awaitPointerEvent()
                                            val ch = ev.changes.firstOrNull { it.id == down.id }
                                            if (ch == null || !ch.pressed) {
                                                viewModel.setFiring(false)
                                                break
                                            }
                                            ch.consume()
                                        }
                                    }
                                }
                                .testTag("fire_button")
                        ) {
                            Text(
                                text = L10n.pick(lang, "FIRE", "গুলি"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Victory / Eliminated Result Overlay
        if (hud.phase == MatchPhase.VICTORY || hud.phase == MatchPhase.ELIMINATED) {
            val isWin = hud.phase == MatchPhase.VICTORY
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(24.dp)
                        .border(2.dp, if (isWin) BlazeAmber else CrimsonPulse, RoundedCornerShape(20.dp)),
                    color = GunmetalSurface,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isWin) {
                                L10n.pick(lang, "BOOYAH! VICTORY #1", "বুইয়াহ! আপনি বিজয়ী #১")
                            } else {
                                L10n.pick(lang, "ELIMINATED #${hud.placement}/100", "এলিমিনেটেড #${hud.placement}/১০০")
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (isWin) BlazeAmber else CrimsonPulse
                        )
                        Text(
                            text = L10n.pick(
                                lang,
                                "Kills: ${hud.kills} • Survival: ${hud.matchDurationSec}s",
                                "মোট কিল: ${hud.kills} • সময়: ${hud.matchDurationSec} সেকেন্ড"
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onExitToMenu,
                                modifier = Modifier.height(48.dp).testTag("result_menu_button")
                            ) {
                                Text(L10n.pick(lang, "LOBBY", "মেনু"))
                            }
                            Button(
                                onClick = { viewModel.startNewMatch() },
                                colors = ButtonDefaults.buttonColors(containerColor = BlazeAmber, contentColor = ObsidianBg),
                                modifier = Modifier.height(48.dp).testTag("result_replay_button")
                            ) {
                                Text(L10n.pick(lang, "PLAY AGAIN", "আবার খেলুন"))
                            }
                        }
                    }
                }
            }
        }

        // Exit Confirmation Modal
        if (showExitConfirm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(24.dp)
                        .border(1.5.dp, TacticalBorder, RoundedCornerShape(16.dp)),
                    color = GunmetalSurface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = L10n.pick(lang, "Leave Match?", "ম্যাচ থেকে বের হবেন?"),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { showExitConfirm = false },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(L10n.pick(lang, "Resume", "খেলুন"))
                            }
                            Button(
                                onClick = {
                                    showExitConfirm = false
                                    onExitToMenu()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPulse),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(L10n.pick(lang, "Leave", "বের হন"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudBadge(label: String, value: String, accent: Color) {
    Surface(
        modifier = Modifier.border(1.dp, TacticalBorder, RoundedCornerShape(10.dp)),
        color = GunmetalSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = accent)
        }
    }
}

@Composable
private fun ActionCircleButton(
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    icon: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (active) accent else GunmetalSurface.copy(alpha = 0.84f))
            .border(1.5.dp, accent, CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) ObsidianBg else accent
            )
        }
    }
}
