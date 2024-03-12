package com.beyondnull.flexibletodos.calculation

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.beyondnull.flexibletodos.R

class UrgencyColorMapping {
    public enum class ColorRange {
        STANDARD, EXTENDED, DISCRETE

    }

    companion object {
        @ColorInt
        fun get(
            context: Context,
            taskPeriod: Int,
            daysUntilDue: Int,
            colorRange: ColorRange
        ): Int {
            val startColor = ContextCompat.getColor(context, R.color.red)
            val endColor = if (colorRange == ColorRange.EXTENDED) {
                ContextCompat.getColor(context, R.color.blue)
            } else {
                ContextCompat.getColor(context, R.color.green)
            }

            val ratio = (daysUntilDue.toFloat() / taskPeriod).coerceIn(-1F, 1F)
            val color = ColorUtils.blendARGB(startColor, endColor, (ratio / 2) + 0.5F)

            return if (colorRange == ColorRange.DISCRETE) {
                // TODO(P3): improve this rounding algorithm
                (color / 1000) * 1000
            } else {
                color
            }
        }
    }
}