package com.battleground.royale3d.ui.screens

import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.battleground.royale3d.data.GameCatalog
import com.battleground.royale3d.data.L10n
import com.battleground.royale3d.model.AppLanguage
import com.battleground.royale3d.model.GameSettings
import com.battleground.royale3d.ui.theme.BlazeAmber
import com.battleground.royale3d.ui.theme.CyberCyan
import com.battleground.royale3d.ui.theme.GunmetalElevated
import com.battleground.royale3d.ui.theme.GunmetalSurface
import com.battleground.royale3d.ui.theme.ObsidianBg
import com.battleground.royale3d.ui.theme.SafeEmerald
import com.battleground.royale3d.ui.theme.TacticalBorder
import com.battleground.royale3d.viewmodel.GameViewModel
import com.battleground.royale3d.viewmodel.RenderTargetMode

@Composable
fun InventoryScreen(
    viewModel: GameViewModel,
    initialItemId: String = "jersey",
    settings: GameSettings,
    onBack: () -> Unit
) {
    val lang = settings.language
    val selectedItem by viewModel.selectedArmoryItem.collectAsStateWithLifecycle()

    LaunchedEffect(initialItemId) {
        viewModel.selectArmoryItemById(initialItemId)
    }

    BackHandler(onBack = onBack)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.detachSurface()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(CyberCyan.copy(alpha = 0.14f), ObsidianBg)
                )
            )
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { tv ->
                    viewModel.attachTextureView(tv, RenderTargetMode.ARMORY_SHOWCASE)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.orbitShowcase(dragAmount.x, dragAmount.y)
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GunmetalSurface.copy(alpha = 0.88f))
                            .testTag("inventory_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = L10n.pick(lang, "VAULT & 3D ARMORY", "ভল্ট ও ৩ডি আর্মরি"),
                            style = MaterialTheme.typography.headlineMedium,
                            color = BlazeAmber
                        )
                        Text(
                            text = L10n.pick(lang, "Drag to rotate • Claim & Save items from Event", "ঘোরাতে ড্র্যাগ করুন • ইভেন্ট থেকে আইটেম ভল্টে সেভ করুন"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Jersey Variant Switcher when viewing Character Skin
                if (selectedItem.id == "jersey") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (v in 0..5) {
                            val active = settings.equippedJerseyVariant == v
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (active) BlazeAmber else TacticalBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.equipFromVault("skin_$v") },
                                color = if (active) BlazeAmber else GunmetalSurface.copy(alpha = 0.88f)
                            ) {
                                Text(
                                    text = "#${v + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) ObsidianBg else Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Landscape Bottom Selector + Stats Panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(GameCatalog.armoryItems, key = { it.id }) { item ->
                        val selected = item.id == selectedItem.id
                        val savedInVault = item.id in settings.vaultUnlockedIds || item.category != "WEAPON"
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) BlazeAmber else TacticalBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectArmoryItem(item) }
                                .testTag("armory_item_${item.id}"),
                            color = if (selected) GunmetalElevated else GunmetalSurface.copy(alpha = 0.88f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (savedInVault) "${item.category} • SAVED" else "${item.category} • EVENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (savedInVault) SafeEmerald else BlazeAmber
                                )
                                Text(
                                    text = L10n.pick(lang, item.titleEn, item.titleBn),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Compact Stats + Save/Equip Panel
                Surface(
                    modifier = Modifier
                        .width(305.dp)
                        .border(1.dp, TacticalBorder, RoundedCornerShape(14.dp)),
                    color = GunmetalSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = L10n.pick(lang, selectedItem.titleEn, selectedItem.titleBn),
                            style = MaterialTheme.typography.titleMedium,
                            color = BlazeAmber
                        )
                        Text(
                            text = L10n.pick(lang, selectedItem.subtitleEn, selectedItem.subtitleBn),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )

                        selectedItem.weaponSpec?.let { w ->
                            StatBarRow(
                                label = L10n.pick(lang, "Damage", "ক্ষতি"),
                                valueText = "${w.damage}",
                                progress = w.damage / 120f,
                                color = BlazeAmber
                            )
                            StatBarRow(
                                label = L10n.pick(lang, "Range", "রেঞ্জ"),
                                valueText = "${w.rangeMeters.toInt()}m",
                                progress = w.rangeMeters / 300f,
                                color = CyberCyan
                            )
                            StatBarRow(
                                label = L10n.pick(lang, "Mag / Reload", "ম্যাগাজিন / রিলোড"),
                                valueText = "${w.magSize} • ${w.reloadTimeSec}s",
                                progress = (w.magSize / 35f).coerceIn(0.1f, 1f),
                                color = SafeEmerald
                            )
                            val unlocked = selectedItem.id in settings.vaultUnlockedIds
                            val equipped = settings.equippedWeaponId == selectedItem.id
                            Button(
                                onClick = {
                                    if (!unlocked) viewModel.claimToVault(selectedItem.id)
                                    else viewModel.equipFromVault(selectedItem.id)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (equipped) GunmetalElevated else BlazeAmber,
                                    contentColor = if (equipped) SafeEmerald else ObsidianBg
                                ),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Text(
                                    when {
                                        !unlocked -> L10n.pick(lang, "SAVE TO VAULT", "ভল্টে সেভ করুন")
                                        equipped -> L10n.pick(lang, "EQUIPPED IN VAULT", "ভল্টে ইকুইপড")
                                        else -> L10n.pick(lang, "EQUIP", "ইকুইপ করুন")
                                    }
                                )
                            }
                        }

                        selectedItem.vehicleSpec?.let { v ->
                            val kmh = (v.maxSpeedMs * 3.6f).toInt()
                            StatBarRow(
                                label = L10n.pick(lang, "Top Speed", "সর্বোচ্চ গতি"),
                                valueText = "$kmh km/h",
                                progress = v.maxSpeedMs / 45f,
                                color = CyberCyan
                            )
                            StatBarRow(
                                label = L10n.pick(lang, "Armor Health", "গাড়ির স্বাস্থ্য"),
                                valueText = "${v.maxHealth} HP",
                                progress = v.maxHealth / 650f,
                                color = SafeEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBarRow(
    label: String,
    valueText: String,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = color)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = GunmetalElevated
        )
    }
}
