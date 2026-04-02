package org.valkyrienskies.clockwork.util

// VS2 removed: no longer implements DoubleComponentAugmentation (VS2 interface)
class DoubleAvgComponentAugmentation(val key: String) {
    fun combineDouble(a: Double, b: Double): Double = (a + b) / 2.0
}
