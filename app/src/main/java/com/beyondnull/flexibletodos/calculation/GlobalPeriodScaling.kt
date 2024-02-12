package com.beyondnull.flexibletodos.calculation

class GlobalPeriodScaling {
    companion object {
        fun scale(taskFrequency: Int, globalNotificationScale: Int): Int {
            val scaledFrequency = 3.0 + (0.8 * taskFrequency / (1.0 + taskFrequency / 45.0))
            return (scaledFrequency / globalNotificationScale).toInt().coerceAtLeast(1)
        }
    }

}