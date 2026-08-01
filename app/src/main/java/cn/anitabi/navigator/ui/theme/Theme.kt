package cn.anitabi.navigator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Paper = Color(0xFFF7F6F2)
val Ink = Color(0xFF20231F)
val Vermilion = Color(0xFFC93E4F)
val Moss = Color(0xFF5F625D)
val Sand = Color(0xFFC9C6BE)
val MutedInk = Color(0xFF5F625D)

val BrandYellow = Color(0xFFF4C95E)
val BrandSky = Color(0xFF9DDBF1)
val SoftVermilion = Color(0xFFF8DADD)
val SurfaceMuted = Color(0xFFECEAE4)

private val colorScheme = lightColorScheme(
    primary = Vermilion,
    onPrimary = Color.White,
    primaryContainer = SoftVermilion,
    onPrimaryContainer = Color(0xFF5E101B),
    inversePrimary = Color(0xFFFFB2BA),
    secondary = Moss,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E6E1),
    onSecondaryContainer = Color(0xFF292B28),
    tertiary = Color(0xFF666963),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7E8E4),
    onTertiaryContainer = Color(0xFF252824),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = MutedInk,
    surfaceTint = Vermilion,
    inverseSurface = Color(0xFF2E312D),
    inverseOnSurface = Color(0xFFF1F0EB),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Sand,
    outlineVariant = Color(0xFFE2DFD7),
    scrim = Color.Black,
    surfaceBright = Color(0xFFFDFCF8),
    surfaceDim = Color(0xFFDEDDD8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F7F3),
    surfaceContainer = Color(0xFFF2F1EC),
    surfaceContainerHigh = Color(0xFFECEBE6),
    surfaceContainerHighest = Color(0xFFE6E5E0),
)

private val typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

val NumericTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontFeatureSettings = "tnum",
    fontWeight = FontWeight.SemiBold,
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AnitabiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
