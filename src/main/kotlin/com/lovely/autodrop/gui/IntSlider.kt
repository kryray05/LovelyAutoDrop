package com.lovely.autodrop.gui

import net.minecraft.client.gui.widget.SliderWidget
import net.minecraft.text.Text
import kotlin.math.roundToInt

/** Small integer slider used by [ConfigScreen]. */
class IntSlider(
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    private val label: String,
    initial: Int,
    private val min: Int,
    private val max: Int,
    private val onChange: (Int) -> Unit,
) : SliderWidget(
    x, y, w, h,
    Text.literal("$label: $initial"),
    toRatio(initial, min, max),
) {

    private var current: Int = initial

    init {
        updateMessage()
    }

    override fun updateMessage() {
        message = Text.literal("$label: $current")
    }

    override fun applyValue() {
        val v = (min + value * (max - min)).roundToInt().coerceIn(min, max)
        if (v != current) {
            current = v
            onChange(v)
        }
    }

    private companion object {
        fun toRatio(value: Int, min: Int, max: Int): Double =
            if (max == min) 0.0 else ((value - min).toDouble() / (max - min)).coerceIn(0.0, 1.0)
    }
}
