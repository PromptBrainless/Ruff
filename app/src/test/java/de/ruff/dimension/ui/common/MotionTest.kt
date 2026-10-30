package de.ruff.dimension.ui.common

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Regeln des reduzierten Animationsmodus (Motion.kt). */
@RunWith(RobolectricTestRunner::class)
class MotionTest {
    @Test
    fun reduzierterModusLiefertDauerNull() {
        assertEquals(0, motionDuration(RuffDurations.MITTEL, animatorScale = 0f))
        assertEquals(0, motionDuration(RuffDurations.LANG, animatorScale = -1f))
        assertEquals(0, RuffMotion(0f).duration(RuffDurations.KURZ))
        assertTrue(RuffMotion(0f).reduced)
    }

    @Test
    fun normalerModusBehaeltBasisdauer() {
        assertEquals(RuffDurations.MITTEL, motionDuration(RuffDurations.MITTEL, animatorScale = 1f))
        assertEquals(RuffDurations.KURZ, motionDuration(RuffDurations.KURZ, animatorScale = 0.5f))
        assertFalse(RuffMotion(1f).reduced)
    }

    @Test
    fun reduzierterModusSpringtStattZuAnimieren() {
        assertTrue(RuffMotion(0f).spec<Float>() is SnapSpec)
        val tween = RuffMotion(1f).spec<Float>(RuffDurations.LANG)
        assertTrue(tween is TweenSpec)
        assertEquals(RuffDurations.LANG, (tween as TweenSpec).durationMillis)
    }

    @Test
    fun reduzierterModusOhneBildschirmuebergaenge() {
        val reduced = RuffMotion(0f)
        assertEquals(EnterTransition.None, reduced.screenEnter())
        assertEquals(ExitTransition.None, reduced.screenExit())
        assertEquals(EnterTransition.None, reduced.screenPopEnter())
        assertEquals(ExitTransition.None, reduced.screenPopExit())
        assertNotEquals(EnterTransition.None, RuffMotion(1f).screenEnter())
    }

    @Test
    fun systemskalaWirdGelesen() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        assertEquals(0f, readAnimatorDurationScale(context))
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        assertEquals(1f, readAnimatorDurationScale(context))
    }

    @Test
    fun schluesselJeWesenEindeutig() {
        assertNotEquals(creatureImageKey("V2-001"), creatureImageKey("V2-002"))
    }
}
