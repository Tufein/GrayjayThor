package com.tsutsen.platformplayer.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tsutsen.platformplayer.core.designsystem.theme.DarkColorScheme
import com.tsutsen.platformplayer.core.designsystem.theme.LightColorScheme
import com.tsutsen.platformplayer.core.designsystem.theme.resolveBluejayColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BluejayThemeContrastTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun oldAndroidDynamicPreferenceUsesReadableLightBrandRoles() {
        assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        val scheme = resolveBluejayColorScheme(context, darkTheme = false, dynamicColor = true)
        assertSame(LightColorScheme, scheme)
        assertControlsAreReadable(scheme)
    }

    @Test fun oldAndroidDynamicPreferenceUsesReadableDarkBrandRoles() {
        assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        val scheme = resolveBluejayColorScheme(context, darkTheme = true, dynamicColor = true)
        assertSame(DarkColorScheme, scheme)
        assertControlsAreReadable(scheme)
    }

    @Test fun disabledDynamicColorUsesTheSelectedBrandMode() {
        assertSame(LightColorScheme, resolveBluejayColorScheme(context, false, false))
        assertSame(DarkColorScheme, resolveBluejayColorScheme(context, true, false))
    }

    @Test fun customSchemeTakesPrecedenceOverSystemPaletteAndMode() {
        val custom = LightColorScheme.copy(primary = Color(0xFF804000), onPrimary = Color.White)
        assertSame(custom, resolveBluejayColorScheme(context, false, true, custom))
        assertSame(custom, resolveBluejayColorScheme(context, true, true, custom))
    }

    private fun assertControlsAreReadable(scheme: ColorScheme) {
        // These are the role pairs used by welcome/home CTAs, selected
        // navigation pills and ordinary page content in the runtime smoke test.
        for ((foreground, background) in listOf(
            scheme.onPrimary to scheme.primary,
            scheme.onSecondaryContainer to scheme.secondaryContainer,
            scheme.onSurface to scheme.surface,
        )) {
            assertEquals(1f, foreground.alpha, 0f)
            assertEquals(1f, background.alpha, 0f)
            val contrast = ColorUtils.calculateContrast(foreground.toArgb(), background.toArgb())
            assertTrue("Expected readable theme roles, contrast was $contrast", contrast >= 4.5)
        }
    }
}
