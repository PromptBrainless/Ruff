package de.ruff.dimension.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.ruff.dimension.R
import de.ruff.dimension.ui.LoadState
import de.ruff.dimension.ui.common.BladeButton
import de.ruff.dimension.ui.common.ChainDivider
import de.ruff.dimension.ui.common.DetailFrame
import de.ruff.dimension.ui.common.GlutPartikel
import de.ruff.dimension.ui.common.TextOnArtShadow
import de.ruff.dimension.ui.theme.Glut
import de.ruff.dimension.ui.theme.Pergament

/**
 * Start/Hauptmenü: Thronsaal als Hintergrund unter einem Scrim, Titel im Detailrahmen,
 * drei Klingen-Knöpfe. Nur von hier schließt die Zurück-Taste die App.
 */
@Composable
internal fun MenuScreen(
    load: LoadState,
    onCreatures: () -> Unit,
    onRules: () -> Unit,
    onSearch: () -> Unit,
    onLicenses: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize().testTag("menu")) {
        Image(
            painter = painterResource(R.drawable.ruff_ui_bg_thronsaal),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.85f)))),
        )
        // Dezente Funken über dem Thronsaal, unter Titel und Knöpfen (im reduzierten Modus aus).
        GlutPartikel(Modifier.fillMaxSize(), count = 18)
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DetailFrame(maxWidth = 440.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "RUFF",
                            style = MaterialTheme.typography.headlineLarge.copy(shadow = TextOnArtShadow),
                            color = Pergament,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            "Archiv der Schwellenlande",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Glut,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                ChainDivider(maxWidth = 300.dp)
                Spacer(Modifier.height(4.dp))
                BladeButton("Wesen", onCreatures)
                BladeButton("Regelwerk", onRules)
                BladeButton("Suche", onSearch)
                Spacer(Modifier.height(8.dp))
                when (load) {
                    is LoadState.Loading ->
                        Text("Archiv wird vorbereitet …", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    is LoadState.Error ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(load.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            TextButton(onClick = onRetry) { Text("Erneut versuchen") }
                        }
                    is LoadState.Ready -> Unit
                }
                TextButton(onClick = onLicenses) { Text("Lizenzen & Herkunft", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
