package com.example.xinqiao.community

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.compositionLocalOf

/* ===============  Color  =============== */
object CommunityColor {
    val Primary        = Color(0xFF7C4DFF)
    val PrimaryDark    = Color(0xFF5E35B1)
    val Success        = Color(0xFF00C853)
    val Danger         = Color(0xFFFF3D00)
    val Neutral900     = Color(0xFF212121)
    val Neutral700     = Color(0xFF616161)
    val Neutral500     = Color(0xFF9E9E9E)
    val Neutral200     = Color(0xFFEEEEEE)
    val Neutral050     = Color(0xFFFAFAFA)
    val Surface        = Color(0xFFFFFFFF)
    val LavenderSoft   = Color(0x1F7C4DFF)
}

/* ===============  Typography  =============== */
object CommunityType {
    val Title        = 18.sp
    val CardTitle    = 16.sp
    val Body         = 14.sp
    val Caption      = 12.sp
}

/* ===============  Elevation  =============== */
object CommunityElevation {
    val Card   = 8.dp
    val TopBar = 2.dp
    val Fab    = 12.dp
}

/* ===============  Corner  =============== */
object CommunityCorner {
    val Card   = 16.dp
    val Button = 12.dp
    val Chip   = 12.dp
}

/* ===============  Spacing  =============== */
object CommunitySpacing {
    val XS  = 4.dp
    val S   = 8.dp
    val M   = 12.dp
    val L   = 16.dp
    val XL  = 24.dp
}

/* ===============  LocalProvider  =============== */
val LocalCommunityTokens = compositionLocalOf { CommunityTokensInstance }

val CommunityTokensInstance = CommunityTokens(
    color   = CommunityColor,
    type    = CommunityType,
    elevate = CommunityElevation,
    corner  = CommunityCorner,
    spacing = CommunitySpacing
)

data class CommunityTokens(
    val color:   CommunityColor,
    val type:    CommunityType,
    val elevate: CommunityElevation,
    val corner:  CommunityCorner,
    val spacing: CommunitySpacing
)