package org.valkyrienskies.clockwork

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.config.ModConfig

object ClockworkConfigUpdater {

    fun update(config: ModConfig) {
        // VS2 removed: config system previously used VSConfigApi (org.valkyrienskies.mod.api.config)
        // which reflected on @ConfigEntry annotations to populate ForgeConfigSpec.
        // Config values remain at their compiled defaults from ClockworkConfig.
    }

    // VS2 removed: ForgeConfigSpec was previously built via VSConfigApi.buildForgeConfigSpec().
    // Use empty specs so Forge config registration succeeds; defaults from ClockworkConfig apply.
    val SERVER_SPEC: ForgeConfigSpec = ForgeConfigSpec.Builder().build()
    val CLIENT_SPEC: ForgeConfigSpec = ForgeConfigSpec.Builder().build()
}
