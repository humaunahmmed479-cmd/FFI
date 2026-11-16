package com.battleground.royale3d.ui.screens

import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.battleground.royale3d.R
import com.battleground.royale3d.data.L10n
import com.battleground.royale3d.model.AppLanguage
import com.battleground.royale3d.model.CameraPerspective
import com.battleground.royale3d.model.GameSettings
import com.battleground.royale3d.model.WsConnectionState
import com.battleground.royale3d.ui.theme.BlazeAmber
import com.battleground.royale3d.ui.theme.CyberCyan
import com.battleground.royale3d.ui.theme.GunmetalSurface
import com.battleground.royale3d.ui.theme.ObsidianBg
import com.battleground.royale3d.ui.theme.SafeEmerald
import com.battleground.royale3d.ui.theme.TacticalBorder
import com.battleground.royale3d.viewmodel.GameViewModel
import com.battleground.royale3d.viewmodel.RenderTargetMode

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    settings: GameSettings,
    isWorldReady: Boolean,
    onPlayClick: () -> Unit,
    onOpenEventClick: () -> Unit,
    onOpenArmoryItem: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val lang = settings.language
    val wsState by viewModel.wsConnectionState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.detachSurface()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Fullscreen 3D Opaque Lobby TextureView: 1 Single Player Character standing on 3D Cyber Stage holding equipped gun
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { tv ->
                    viewModel.attachTextureView(tv, RenderTargetMode.LOBBY_SHOWCASE)
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
            // === TOP LOBBY BAR ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
                        .clickable { onOpenArmoryItem("jersey") },
                    color = GunmetalSurface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BlazeAmber)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MilitaryTech,
                                contentDescription = "Rank",
                                tint = ObsidianBg,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "BD_SURVIVOR",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "LV.35",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ObsidianBg,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberCyan)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "HEROIC • Vault Items: ${settings.vaultUnlockedIds.size} • 100 HP",
                                    "হিরোয়িক • ভল্ট: ${settings.vaultUnlockedIds.size}টি আইটেম • ১০০ স্বাস্থ্য"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = BlazeAmber
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.border(1.dp, TacticalBorder, RoundedCornerShape(10.dp)),
                        color = GunmetalSurface.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Fair Combat",
                                tint = SafeEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (wsState == WsConnectionState.CONNECTED) {
                                    "WS LIVE"
                                } else {
                                    L10n.pick(lang, "100-BOT ARENA", "১০০-প্লেয়ার এরিনা")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = SafeEmerald
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                val next = if (lang == AppLanguage.BENGALI) {
                                    AppLanguage.ENGLISH
                                } else {
                                    AppLanguage.BENGALI
                                }
                                viewModel.setLanguage(next)
                            }
                            .testTag("language_toggle_button"),
                        color = GunmetalSurface.copy(alpha = 0.88f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Language",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (lang == AppLanguage.BENGALI) "বাংলা" else "EN",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GunmetalSurface.copy(alpha = 0.88f))
                            .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = BlazeAmber
                        )
                    }
                }
            }

            // === MIDDLE & BOTTOM LOBBY AREA ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // LEFT SIDEBAR: EVENT (for Guns!), VAULT (saved skins/guns), WEAPONS, GARAGE, PLANE
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        LobbySideTab(
                            icon = Icons.Default.CardGiftcard,
                            label = L10n.pick(lang, "EVENT (GUNS)", "ইভেন্ট (গান ও স্কিন)"),
                            accent = BlazeAmber,
                            testTag = "lobby_tab_event",
                            onClick = onOpenEventClick
                        )
                        LobbySideTab(
                            icon = Icons.Default.Checkroom,
                            label = L10n.pick(lang, "VAULT (SAVED)", "ভল্ট (সেভ করা)"),
                            accent = CyberCyan,
                            testTag = "lobby_tab_vault",
                            onClick = { onOpenArmoryItem("jersey") }
                        )
                        LobbySideTab(
                            icon = Icons.Default.Security,
                            label = L10n.pick(lang, "WEAPONS 3D", "অস্ত্র ৩ডি"),
                            accent = BlazeAmber,
                            testTag = "inventory_button",
                            onClick = { onOpenArmoryItem("awm") }
                        )
                        LobbySideTab(
                            icon = Icons.Default.DirectionsCar,
                            label = L10n.pick(lang, "VEHICLES", "গাড়ি ও যানবাহন"),
                            accent = SafeEmerald,
                            testTag = "lobby_tab_garage",
                            onClick = { onOpenArmoryItem("audi") }
                        )
                        LobbySideTab(
                            icon = Icons.Default.FlightTakeoff,
                            label = L10n.pick(lang, "DROP PLANE", "প্লেন কেবিন"),
                            accent = CyberCyan,
                            testTag = "lobby_tab_plane",
                            onClick = { onOpenArmoryItem("plane") }
                        )
                    }
                }

                // CENTER BOTTOM: Clean character rotation prompt only (NO floating gun!)
                Text(
                    text = L10n.pick(
                        lang,
                        "DRAG TO ROTATE PLAYER",
                        "প্লেয়ার ঘোরাতে ড্র্যাগ করুন"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // RIGHT PANEL: Map Mode Card + TPP/FPP + START Button
                Column(
                    modifier = Modifier.width(248.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp)),
                        color = GunmetalSurface.copy(alpha = 0.90f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_drop_zone_banner),
                                contentDescription = "Theme Park Map",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp, 42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column {
                                Text(
                                    text = L10n.pick(lang, "BATTLE ROYALE", "ব্যাটেল রয়্যাল"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BlazeAmber
                                )
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "Theme Park • BD Zone",
                                        "থিম পার্ক • বাংলাদেশ জোন"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = L10n.pick(lang, "Plane Drop -> Parachute", "প্লেন লবি -> প্যারাসুট"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GunmetalSurface.copy(alpha = 0.90f))
                            .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isTpp = settings.defaultPerspective == CameraPerspective.THIRD_PERSON
                        Button(
                            onClick = { viewModel.setDefaultPerspective(CameraPerspective.THIRD_PERSON) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTpp) CyberCyan else Color.Transparent,
                                contentColor = if (isTpp) ObsidianBg else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("mode_tpp_button")
                        ) {
                            Text("TPP", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = { viewModel.setDefaultPerspective(CameraPerspective.FIRST_PERSON) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isTpp) CyberCyan else Color.Transparent,
                                contentColor = if (!isTpp) ObsidianBg else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("mode_fpp_button")
                        ) {
                            Text("FPP", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Button(
                        onClick = onPlayClick,
                        enabled = isWorldReady,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlazeAmber,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("play_button")
                    ) {
                        if (!isWorldReady) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ObsidianBg,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "LOADING MAP...", "ম্যাপ লোড হচ্ছে..."),
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "START", "ম্যাচ শুরু (START)"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbySideTab(
    icon: ImageVector,
    label: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(164.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = GunmetalSurface.copy(alpha = 0.86f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
