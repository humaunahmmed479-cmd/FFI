package com.battleground.royale3d.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.battleground.royale3d.data.L10n
import com.battleground.royale3d.model.AppLanguage
import com.battleground.royale3d.model.GameHudState
import com.battleground.royale3d.model.WeaponType
import com.battleground.royale3d.model.ZoneId
import com.battleground.royale3d.ui.theme.BlazeAmber
import com.battleground.royale3d.ui.theme.CrimsonPulse
import com.battleground.royale3d.ui.theme.CyberCyan
import com.battleground.royale3d.ui.theme.GunmetalElevated
import com.battleground.royale3d.ui.theme.GunmetalSurface
import com.battleground.royale3d.ui.theme.ObsidianBg
import com.battleground.royale3d.ui.theme.SafeEmerald
import com.battleground.royale3d.ui.theme.ShieldBlue
import com.battleground.royale3d.ui.theme.TacticalBorder
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun TacticalMiniMap(
    hud: GameHudState,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(124.dp)
            .border(1.5.dp, TacticalBorder, RoundedCornerShape(14.dp)),
        color = ObsidianBg.copy(alpha = 0.84f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val mapHalf = 280f
                fun toCanvas(wx: Float, wz: Float): Offset {
                    val nx = ((wx + mapHalf) / (mapHalf * 2f)).coerceIn(0f, 1f)
                    val nz = ((wz + mapHalf) / (mapHalf * 2f)).coerceIn(0f, 1f)
                    return Offset(nx * size.width, nz * size.height)
                }

                val scale = size.width / (mapHalf * 2f)

                // Zone landmark dots
                ZoneId.entries.forEach { z ->
                    val center = toCanvas(z.centerX, z.centerZ)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.14f),
                        radius = z.radius * scale * 0.45f,
                        center = center
                    )
                }

                // Current Safe Zone circle (cyan)
                drawCircle(
                    color = CyberCyan,
                    radius = (hud.safeZone.currentRadius * scale).coerceAtLeast(4f),
                    center = toCanvas(hud.safeZone.currentCenterX, hud.safeZone.currentCenterZ),
                    style = Stroke(width = 2.2f)
                )

                // Next Safe Zone circle (dashed white)
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = (hud.safeZone.nextRadius * scale).coerceAtLeast(3f),
                    center = toCanvas(hud.safeZone.nextCenterX, hud.safeZone.nextCenterZ),
                    style = Stroke(
                        width = 1.4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                )

                // Vehicles
                hud.vehicles.forEach { v ->
                    if (v.health > 0) {
                        val pos = toCanvas(v.x, v.z)
                        drawCircle(
                            color = SafeEmerald,
                            radius = 3.5f,
                            center = pos
                        )
                    }
                }

                // Nearby enemies within 80m
                hud.remoteCombatants.forEach { c ->
                    if (c.alive) {
                        val dx = c.x - hud.playerX
                        val dz = c.z - hud.playerZ
                        if (dx * dx + dz * dz < 65f * 65f) {
                            drawCircle(
                                color = CrimsonPulse,
                                radius = 2.6f,
                                center = toCanvas(c.x, c.z)
                            )
                        }
                    }
                }

                // Player (or Drop Plane) position & heading indicator
                val markerX = if (hud.phase == com.battleground.royale3d.model.MatchPhase.PLANE_CABIN) hud.planeWorldX else hud.playerX
                val markerZ = if (hud.phase == com.battleground.royale3d.model.MatchPhase.PLANE_CABIN) hud.planeWorldZ else hud.playerZ
                val pPos = toCanvas(markerX, markerZ)
                val lookEnd = Offset(
                    x = pPos.x + sin(hud.playerYawRad) * 14f,
                    y = pPos.y + cos(hud.playerYawRad) * 14f
                )
                drawLine(color = BlazeAmber, start = pPos, end = lookEnd, strokeWidth = 2.5f)
                drawCircle(color = BlazeAmber, radius = 4.8f, center = pPos)
            }

            Text(
                text = L10n.pick(lang, hud.currentZone.nameEn, hud.currentZone.nameBn),
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(ObsidianBg.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun HealthAndShieldPanel(
    health: Int,
    shield: Int,
    medkits: Int,
    lang: AppLanguage,
    onUseMedkit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp)),
        color = GunmetalSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.width(148.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                // Shield bar (separate from health)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = L10n.pick(lang, "SHIELD", "শিল্ড"),
                        style = MaterialTheme.typography.labelSmall,
                        color = ShieldBlue
                    )
                    Text(
                        text = "$shield/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShieldBlue
                    )
                }
                LinearProgressIndicator(
                    progress = { (shield / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ShieldBlue,
                    trackColor = GunmetalElevated
                )

                // Health bar (100 max)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = L10n.pick(lang, "HP", "স্বাস্থ্য"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (health <= 30) CrimsonPulse else SafeEmerald
                    )
                    Text(
                        text = "$health/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (health <= 30) CrimsonPulse else SafeEmerald
                    )
                }
                LinearProgressIndicator(
                    progress = { (health / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (health <= 30) CrimsonPulse else SafeEmerald,
                    trackColor = GunmetalElevated
                )
            }

            // Medkit quick action
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        if (medkits > 0 && health < 100) SafeEmerald else TacticalBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = medkits > 0 && health < 100, onClick = onUseMedkit)
                    .testTag("medkit_button"),
                color = GunmetalElevated
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "Medkit",
                        tint = if (medkits > 0) SafeEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "x$medkits",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun WeaponLoadoutBar(
    hud: GameHudState,
    lang: AppLanguage,
    onSelectSlot: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        hud.weaponSlots.forEachIndexed { idx, slot ->
            val active = idx == hud.activeSlotIndex
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = if (active) 2.dp else 1.dp,
                        color = if (active) BlazeAmber else TacticalBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectSlot(idx) }
                    .testTag("weapon_slot_$idx"),
                color = GunmetalSurface.copy(alpha = if (active) 0.94f else 0.72f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = L10n.pick(lang, slot.spec.nameEn, slot.spec.nameBn),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) BlazeAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${slot.magAmmo} / ${slot.reserveAmmo}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun VirtualJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(134.dp)
            .clip(CircleShape)
            .background(GunmetalSurface.copy(alpha = 0.42f))
            .border(1.5.dp, CyberCyan.copy(alpha = 0.55f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                    val maxRadius = size.width * 0.36f

                    fun updateFromPos(pos: Offset) {
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                        val clamped = min(dist, maxRadius)
                        val nx = (dx / dist) * (clamped / maxRadius)
                        val ny = (dy / dist) * (clamped / maxRadius)
                        knobOffset = Offset(nx * maxRadius, ny * maxRadius)
                        // Note: negative screen Y drag = move forward (+moveY)
                        onMove(nx, -ny)
                    }

                    updateFromPos(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            knobOffset = Offset.Zero
                            onMove(0f, 0f)
                            break
                        }
                        change.consume()
                        updateFromPos(change.position)
                    }
                }
            }
            .testTag("virtual_joystick")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            drawCircle(
                color = BlazeAmber.copy(alpha = 0.85f),
                radius = 24.dp.toPx(),
                center = center + knobOffset
            )
        }
    }
}

@Composable
fun CrosshairAndScopeOverlay(hud: GameHudState) {
    val activeWeapon = hud.weaponSlots.getOrNull(hud.activeSlotIndex)?.spec
    val isSniperScope = hud.isAiming && activeWeapon?.type == WeaponType.AWM

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f

        if (isSniperScope) {
            // 8x AWM Sniper Scope Vignette + Mil-Dot Crosshair
            val scopeR = min(size.width, size.height) * 0.34f
            drawCircle(
                color = Color.Black.copy(alpha = 0.92f),
                radius = size.maxDimension,
                center = Offset(cx, cy),
                style = Stroke(width = (size.maxDimension - scopeR) * 2f)
            )
            drawCircle(
                color = CyberCyan,
                radius = scopeR,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )
            drawLine(Color.Black, Offset(cx - scopeR, cy), Offset(cx + scopeR, cy), strokeWidth = 2f)
            drawLine(Color.Black, Offset(cx, cy - scopeR), Offset(cx, cy + scopeR), strokeWidth = 2f)
            drawCircle(color = CrimsonPulse, radius = 4f, center = Offset(cx, cy))
        } else {
            val gap = if (hud.isAiming) 6f else 14f
            val len = 12f
            val color = if (hud.hitMarkerFlash) CrimsonPulse else Color.White.copy(alpha = 0.92f)
            drawLine(color, Offset(cx - gap - len, cy), Offset(cx - gap, cy), strokeWidth = 2.5f)
            drawLine(color, Offset(cx + gap, cy), Offset(cx + gap + len, cy), strokeWidth = 2.5f)
            drawLine(color, Offset(cx, cy - gap - len), Offset(cx, cy - gap), strokeWidth = 2.5f)
            drawLine(color, Offset(cx, cy + gap), Offset(cx, cy + gap + len), strokeWidth = 2.5f)

            if (hud.hitMarkerFlash) {
                val h = 11f
                drawLine(CrimsonPulse, Offset(cx - h, cy - h), Offset(cx - 4f, cy - 4f), strokeWidth = 3f)
                drawLine(CrimsonPulse, Offset(cx + h, cy - h), Offset(cx + 4f, cy - 4f), strokeWidth = 3f)
                drawLine(CrimsonPulse, Offset(cx - h, cy + h), Offset(cx - 4f, cy + 4f), strokeWidth = 3f)
                drawLine(CrimsonPulse, Offset(cx + h, cy + h), Offset(cx + 4f, cy + 4f), strokeWidth = 3f)
            }
        }
    }
}
