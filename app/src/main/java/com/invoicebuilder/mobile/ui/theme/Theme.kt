package com.invoicebuilder.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.invoicebuilder.core.model.InvoiceStatus

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoPrimaryContainerDark,
    onPrimaryContainer = IndigoOnPrimaryContainerDark,
    secondary = EmeraldSecondaryDark,
    onSecondary = EmeraldOnSecondaryDark,
    secondaryContainer = EmeraldSecondaryContainerDark,
    onSecondaryContainer = EmeraldOnSecondaryContainerDark,
    tertiary = SkyTertiaryDark,
    onTertiary = SkyOnTertiaryDark,
    tertiaryContainer = SkyTertiaryContainerDark,
    onTertiaryContainer = SkyOnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = IndigoPrimaryContainerLight,
    onPrimaryContainer = IndigoOnPrimaryContainerLight,
    secondary = EmeraldSecondaryLight,
    onSecondary = EmeraldOnSecondaryLight,
    secondaryContainer = EmeraldSecondaryContainerLight,
    onSecondaryContainer = EmeraldOnSecondaryContainerLight,
    tertiary = SkyTertiaryLight,
    onTertiary = SkyOnTertiaryLight,
    tertiaryContainer = SkyTertiaryContainerLight,
    onTertiaryContainer = SkyOnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

data class StatusColors(
    val background: Color,
    val text: Color,
    val dot: Color
)

@Composable
fun getInvoiceStatusColors(status: InvoiceStatus, isDark: Boolean = isSystemInDarkTheme()): StatusColors {
    return when (status) {
        InvoiceStatus.paid -> if (isDark) {
            StatusColors(
                background = StatusPaidBgDark,
                text = StatusPaidTextDark,
                dot = Color(0xFF22C55E)
            )
        } else {
            StatusColors(
                background = StatusPaidBgLight,
                text = StatusPaidTextLight,
                dot = Color(0xFF16A34A)
            )
        }
        InvoiceStatus.unpaid -> if (isDark) {
            StatusColors(
                background = StatusUnpaidBgDark,
                text = StatusUnpaidTextDark,
                dot = Color(0xFFF43F5E)
            )
        } else {
            StatusColors(
                background = StatusUnpaidBgLight,
                text = StatusUnpaidTextLight,
                dot = Color(0xFFE11D48)
            )
        }
        InvoiceStatus.partially -> if (isDark) {
            StatusColors(
                background = StatusPartiallyBgDark,
                text = StatusPartiallyTextDark,
                dot = Color(0xFFF59E0B)
            )
        } else {
            StatusColors(
                background = StatusPartiallyBgLight,
                text = StatusPartiallyTextLight,
                dot = Color(0xFFD97706)
            )
        }
        InvoiceStatus.open -> if (isDark) {
            StatusColors(
                background = StatusDraftBgDark,
                text = StatusDraftTextDark,
                dot = Color(0xFF38BDF8)
            )
        } else {
            StatusColors(
                background = StatusDraftBgLight,
                text = StatusDraftTextLight,
                dot = Color(0xFF0284C7)
            )
        }
        InvoiceStatus.closed -> if (isDark) {
            StatusColors(
                background = StatusClosedBgDark,
                text = StatusClosedTextDark,
                dot = Color(0xFF94A3B8)
            )
        } else {
            StatusColors(
                background = StatusClosedBgLight,
                text = StatusClosedTextLight,
                dot = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun InvoiceBuilderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
