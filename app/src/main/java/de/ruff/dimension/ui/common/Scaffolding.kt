package de.ruff.dimension.ui.common

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.ruff.dimension.ui.LibraryUiState
import de.ruff.dimension.ui.LoadState
import de.ruff.dimension.ui.theme.Pergament

/** Löst dieselbe Zurück-Aktion aus wie die System-Zurück-Taste. */
@Composable
internal fun rememberSystemBack(): () -> Unit {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    return { dispatcher?.onBackPressed() }
}

/**
 * Kopfzeile eines Bildschirms: optionaler Zurück-Knopf (gleiches Verhalten wie die
 * System-Zurück-Taste) und Titel auf der Titelleiste. Berücksichtigt die Statusleiste.
 */
@Composable
internal fun RuffTopBar(
    title: String,
    showBack: Boolean,
    modifier: Modifier = Modifier,
    applyStatusBarInsets: Boolean = true,
) {
    val back = rememberSystemBack()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (applyStatusBarInsets) {
                        Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = back) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = Pergament)
            }
        } else {
            Spacer(Modifier.width(4.dp))
        }
        TitleBarFrame(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(shadow = TextOnArtShadow),
                color = Pergament,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

/** Zeigt Laden/Fehler wie bisher und den Inhalt erst, wenn die Daten bereit sind. */
@Composable
internal fun LoadGate(
    state: LibraryUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (val load = state.load) {
        is LoadState.Loading -> LoadingState(modifier)
        is LoadState.Error -> ErrorState(load.message, onRetry, modifier)
        is LoadState.Ready -> content()
    }
}

@Composable
internal fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp))
    }
}

@Composable
internal fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(12.dp))
        Text("Archiv wird vorbereitet …", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(message)
        Button(onClick = onRetry) {
            Text("Erneut versuchen")
        }
    }
}

/** Füllfarbe Höllenrot für Hauptaktionen (wie bisher `Ember`). */
@Composable
internal fun emberButtonColors() =
    androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

/** Einzeiliges Suchfeld, wie bisher im Bibliotheksbildschirm. */
@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().testTag("search_field"),
        label = { Text(label) },
        singleLine = true,
    )
}

/** Seitliche Systemleisten/Aussparungen (Querformat) freihalten. */
internal fun Modifier.safeHorizontalPadding(): Modifier =
    composed { windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)) }
