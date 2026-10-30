package de.ruff.dimension.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import de.ruff.dimension.R
import de.ruff.dimension.ui.common.RuffTopBar
import de.ruff.dimension.ui.common.SectionHeader
import de.ruff.dimension.ui.common.safeHorizontalPadding

/** Herkunft der Gestaltungselemente und Lizenztexte der gebündelten Schriften (SIL OFL 1.1). */
@Composable
internal fun LicensesScreen() {
    val resources = LocalContext.current.resources
    val cinzel = remember { resources.openRawResource(R.raw.ofl_cinzel).bufferedReader().use { it.readText() } }
    val alegreya = remember { resources.openRawResource(R.raw.ofl_alegreya).bufferedReader().use { it.readText() } }
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().safeHorizontalPadding()) {
        RuffTopBar(title = "Lizenzen", showBack = true)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            SectionHeader("Gestaltung")
            Text(
                "Hintergrund, Rahmen, Leisten, Trenner, Klingen-Knopf und Avatar-Ring (ruff_ui_*) " +
                    "stammen aus Fynn Levin Hirts Design-Elementen und sind von ihm unter seinem Namen lizenziert. " +
                    "Wesen-Bilder und Inhalte gehören zum Projekt Ruff.",
                color = colors.onSurface,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            SectionHeader("Schrift Cinzel")
            LicenseText(cinzel)
            SectionHeader("Schrift Alegreya")
            LicenseText(alegreya)
        }
    }
}

@Composable
private fun LicenseText(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier.padding(vertical = 10.dp),
    )
}
