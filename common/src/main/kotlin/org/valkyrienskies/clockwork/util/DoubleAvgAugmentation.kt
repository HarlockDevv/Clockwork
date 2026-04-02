package org.valkyrienskies.clockwork.util

// VS2 removed: no longer implements DoubleAugmentation (VS2 interface)
class DoubleAvgAugmentation(val key: String) {
    fun combineDouble(a: Double, b: Double): Double = (a + b) / 2.0
}
