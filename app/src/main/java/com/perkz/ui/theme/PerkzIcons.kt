package com.perkz.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PerkzIcons {
    val Perks: ImageVector
        get() = ImageVector.Builder(
            name = "Perks",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            // A "cooler" 2x2 grid where the top-right is a sparkle/star
            moveTo(4f, 4f)
            curveTo(4f, 2.9f, 4.9f, 2f, 6f, 2f)
            horizontalLineTo(10f)
            curveTo(11.1f, 2f, 12f, 2.9f, 12f, 4f)
            verticalLineTo(8f)
            curveTo(12f, 9.1f, 11.1f, 10f, 10f, 10f)
            horizontalLineTo(6f)
            curveTo(4.9f, 10f, 4f, 9.1f, 4f, 8f)
            verticalLineTo(4f)
            close()

            moveTo(4f, 16f)
            curveTo(4f, 14.9f, 4.9f, 14f, 6f, 14f)
            horizontalLineTo(10f)
            curveTo(11.1f, 14f, 12f, 14.9f, 12f, 16f)
            verticalLineTo(20f)
            curveTo(12f, 21.1f, 11.1f, 22f, 10f, 22f)
            horizontalLineTo(6f)
            curveTo(4.9f, 22f, 4f, 21.1f, 4f, 20f)
            verticalLineTo(16f)
            close()

            moveTo(14f, 16f)
            curveTo(14f, 14.9f, 14.9f, 14f, 16f, 14f)
            horizontalLineTo(20f)
            curveTo(21.1f, 14f, 22f, 14.9f, 22f, 16f)
            verticalLineTo(20f)
            curveTo(22f, 21.1f, 21.1f, 22f, 20f, 22f)
            horizontalLineTo(16f)
            curveTo(14.9f, 22f, 14f, 21.1f, 14f, 20f)
            verticalLineTo(16f)
            close()

            // The "Sparkle" top-right
            moveTo(18f, 2f)
            lineTo(18.7f, 5.3f)
            lineTo(22f, 6f)
            lineTo(18.7f, 6.7f)
            lineTo(18f, 10f)
            lineTo(17.3f, 6.7f)
            lineTo(14f, 6f)
            lineTo(17.3f, 5.3f)
            close()
        }.build()

    val Notifications: ImageVector
        get() = ImageVector.Builder(
            name = "Notifications",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 22f)
            curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
            horizontalLineTo(10f)
            curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
            close()
            moveTo(18f, 16f)
            verticalLineTo(11f)
            curveTo(18f, 7.9f, 16.4f, 5.4f, 13.5f, 4.7f)
            verticalLineTo(4f)
            curveTo(13.5f, 3.2f, 12.8f, 2.5f, 12f, 2.5f)
            curveTo(11.2f, 2.5f, 10.5f, 3.2f, 10.5f, 4f)
            verticalLineTo(4.7f)
            curveTo(7.6f, 5.4f, 6f, 7.9f, 6f, 11f)
            verticalLineTo(16f)
            lineTo(4f, 18f)
            verticalLineTo(19f)
            horizontalLineTo(20f)
            verticalLineTo(18f)
            lineTo(18f, 16f)
            close()
            // Add a small "active" dot
            moveTo(21f, 6.5f)
            curveTo(21f, 7.3f, 20.3f, 8f, 19.5f, 8f)
            curveTo(18.7f, 8f, 18f, 7.3f, 18f, 6.5f)
            curveTo(18f, 5.7f, 18.7f, 5f, 19.5f, 5f)
            curveTo(20.3f, 5f, 21f, 5.7f, 21f, 6.5f)
            close()
        }.build()

    val Settings: ImageVector
        get() = ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            // Modern "Sliders" icon
            moveTo(3f, 17f)
            verticalLineTo(19f)
            horizontalLineTo(9f)
            verticalLineTo(17f)
            horizontalLineTo(3f)
            close()
            moveTo(3f, 5f)
            verticalLineTo(7f)
            horizontalLineTo(13f)
            verticalLineTo(5f)
            horizontalLineTo(3f)
            close()
            moveTo(13f, 21f)
            verticalLineTo(15f)
            horizontalLineTo(11f)
            verticalLineTo(21f)
            horizontalLineTo(13f)
            close()
            moveTo(7f, 9f)
            verticalLineTo(3f)
            horizontalLineTo(5f)
            verticalLineTo(9f)
            horizontalLineTo(7f)
            close()
            moveTo(21f, 13f)
            verticalLineTo(11f)
            horizontalLineTo(11f)
            verticalLineTo(13f)
            horizontalLineTo(21f)
            close()
            moveTo(17f, 15f)
            horizontalLineTo(15f)
            verticalLineTo(9f)
            horizontalLineTo(17f)
            verticalLineTo(15f)
            close()
        }.build()
}
