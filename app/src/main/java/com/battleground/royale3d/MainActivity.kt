package com.battleground.royale3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.battleground.royale3d.ui.screens.EventScreen
import com.battleground.royale3d.ui.screens.GameScreen
import com.battleground.royale3d.ui.screens.InventoryScreen
import com.battleground.royale3d.ui.screens.MainMenuScreen
import com.battleground.royale3d.ui.screens.SettingsScreen
import com.battleground.royale3d.ui.theme.BattleRoyaleTheme
import com.battleground.royale3d.viewmodel.GameViewModel
import kotlinx.serialization.Serializable

@Serializable
object MenuRoute

@Serializable
data class InventoryRoute(val initialItemId: String = "jersey")

@Serializable
object EventRoute

@Serializable
object SettingsRoute

@Serializable
object GameRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enterImmersiveFullscreen()

        setContent {
            BattleRoyaleTheme {
                val navController = rememberNavController()
                val gameViewModel: GameViewModel = viewModel()
                val settings by gameViewModel.settings.collectAsStateWithLifecycle()
                val isWorldReady by gameViewModel.isWorldReady.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = MenuRoute
                ) {
                    composable<MenuRoute> {
                        MainMenuScreen(
                            viewModel = gameViewModel,
                            settings = settings,
                            isWorldReady = isWorldReady,
                            onPlayClick = { navController.navigate(GameRoute) },
                            onOpenEventClick = { navController.navigate(EventRoute) },
                            onOpenArmoryItem = { itemId ->
                                navController.navigate(InventoryRoute(initialItemId = itemId))
                            },
                            onSettingsClick = { navController.navigate(SettingsRoute) }
                        )
                    }
                    composable<EventRoute> {
                        EventScreen(
                            viewModel = gameViewModel,
                            settings = settings,
                            onOpenVaultItem = { itemId ->
                                navController.navigate(InventoryRoute(initialItemId = itemId))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<InventoryRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<InventoryRoute>()
                        InventoryScreen(
                            viewModel = gameViewModel,
                            initialItemId = route.initialItemId,
                            settings = settings,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(
                            viewModel = gameViewModel,
                            settings = settings,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<GameRoute> {
                        GameScreen(
                            viewModel = gameViewModel,
                            settings = settings,
                            onExitToMenu = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveFullscreen()
    }

    private fun enterImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
