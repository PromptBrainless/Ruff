package de.ruff.dimension

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.ruff.dimension.ui.LibraryViewModel
import de.ruff.dimension.ui.navigation.RuffNavHost
import de.ruff.dimension.ui.theme.RuffTheme

/**
 * Einstieg der App: nur Theme, Edge-to-Edge und Navigation. Die Bildschirme liegen
 * unter `ui/` (menu, creature, rule, search, editor, about), die Bausteine unter
 * `ui/common`, Farben und Schriften unter `ui/theme`.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Identität: helle Symbole in Status- und Navigationsleiste.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.argb(0xB3, 0x0E, 0x0B, 0x0C)),
        )
        super.onCreate(savedInstanceState)
        setContent {
            RuffTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                RuffNavHost(state = state, viewModel = viewModel)
            }
        }
    }
}
