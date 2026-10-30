package de.ruff.dimension.ui.common

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.ruff.dimension.R
import de.ruff.dimension.data.Creature

/** Ressourcen-ID eines Bildes aus den Inhaltsdaten; `0`, wenn es kein solches Bild gibt. */
@SuppressLint("DiscouragedApi")
@Composable
internal fun drawableId(resourceName: String): Int {
    val context = LocalContext.current
    return remember(resourceName) {
        if (resourceName.isBlank()) {
            0
        } else {
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }
    }
}

/** Bild für den Porträtrahmen: Porträt, sonst Cover, sonst Icon; `null` = Platzhalter. */
@Composable
internal fun creaturePortraitId(creature: Creature): Int? =
    listOf(creature.portrait, creature.cover, creature.icon)
        .map { drawableId(it) }
        .firstOrNull { it != 0 }

/** Platzhalter für Wesen ohne Bild (unverändert aus der bisherigen Oberfläche). */
@DrawableRes
internal val PlaceholderImage: Int = R.drawable.ruff_platzhalter_kapuze
