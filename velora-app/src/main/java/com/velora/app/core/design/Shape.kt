package com.velora.app.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VeloraShapes = Shapes(
    /** Chips, badges, small tags. */
    extraSmall = RoundedCornerShape(6.dp),
    /** Input fields, compact cards. */
    small = RoundedCornerShape(10.dp),
    /** Standard product cards, list items. */
    medium = RoundedCornerShape(16.dp),
    /** Hero cards, bottom sheets, large panels. */
    large = RoundedCornerShape(24.dp),
    /** Full pill — buttons, FABs, search bars. */
    extraLarge = RoundedCornerShape(50.dp),
)
