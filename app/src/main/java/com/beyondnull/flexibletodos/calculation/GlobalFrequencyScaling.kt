package com.beyondnull.flexibletodos.calculation

class GlobalFrequencyScaling {
    companion object {
        fun scale(taskFrequency: Int, globalFrequency: Int): Int {
            val factor = 5.0
            val scalingFactor = 0.5 + (taskFrequency.coerceAtMost(365) * factor / 365)
            return (taskFrequency / (globalFrequency * scalingFactor)).toInt().coerceAtLeast(1)
        }
    }

}