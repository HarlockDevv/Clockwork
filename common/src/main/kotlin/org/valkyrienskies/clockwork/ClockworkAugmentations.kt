package org.valkyrienskies.clockwork

// VS2 removed: ClockworkAugmentations previously managed VS2 DoubleAugmentation/DoubleComponentAugmentation
// registrations via VsiServerShipWorld. All register/get methods are no-ops since VS2 is absent.
object ClockworkAugmentations {
    // Stubs retained so call sites that reference this object compile without error
    fun registerSumAugmentation(key: String) {}
    fun registerAvgAugmentation(key: String) {}
    fun registerComponentSumAugmentation(key: String) {}
    fun registerComponentAvgAugmentation(key: String) {}
}
