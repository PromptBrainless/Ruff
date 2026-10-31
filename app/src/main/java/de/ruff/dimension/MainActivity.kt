package de.ruff.dimension

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.ruff.dimension.ui.LibraryViewModel
import de.ruff.dimension.ui.akte.AkteBinding
import de.ruff.dimension.ui.akte.CreatureProfileViewModel
import de.ruff.dimension.ui.akte.LocalCreatureProfiles
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
    private val akteViewModel: CreatureProfileViewModel by viewModels()

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
                val akteState by akteViewModel.uiState.collectAsStateWithLifecycle()
                val akte =
                    remember(akteState) {
                        AkteBinding(
                            state = akteState,
                            onEdit = akteViewModel::startEdit,
                            onCancel = akteViewModel::cancelEdit,
                            onSave = akteViewModel::save,
                            onConfirm = akteViewModel::confirmDerived,
                            onDismissHinweis = akteViewModel::dismissHinweis,
                        )
                    }
                CompositionLocalProvider(LocalCreatureProfiles provides akte) {
                    RuffNavHost(state = state, viewModel = viewModel)
                }
            }
        }
    }
}
