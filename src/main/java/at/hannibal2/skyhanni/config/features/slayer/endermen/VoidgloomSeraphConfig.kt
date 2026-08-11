package at.hannibal2.skyhanni.config.features.slayer.endermen

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class VoidgloomSeraphConfig {

    @Expose
    @ConfigOption(
        name = "Phase HUD",
        desc = "Show the current Tier IV Voidgloom phase on a movable HUD. " +
            "Shows Kills, Hitshield, Damage, and Radiation phases.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var phaseDisplay: Boolean = false

    @Expose
    @ConfigLink(owner = VoidgloomSeraphConfig::class, field = "phaseDisplay")
    val phasePosition: Position = Position(10, 80)

    @Expose
    @ConfigOption(
        name = "Laser Timer",
        desc = "Show the remaining Tier IV Broken Heart Radiation laser time on a movable HUD.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var laserTimer: Boolean = false

    @Expose
    @ConfigLink(owner = VoidgloomSeraphConfig::class, field = "laserTimer")
    val laserTimerPosition: Position = Position(10, 90)

    @Expose
    @ConfigOption(
        name = "Static Laser Color",
        desc = "Replace the Tier IV rainbow Broken Heart Radiation particles with one bright static color. " +
            "Hypixel's original particle positions and movement are preserved.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var staticLaserColor: Boolean = false

    @Expose
    @ConfigOption(
        name = "Laser Color",
        desc = "Select the static color used for Tier IV Broken Heart Radiation particles.",
    )
    @ConfigEditorDropdown
    var laserColor: LaserColor = LaserColor.AQUA

    enum class LaserColor(
        private val displayName: String,
        val argb: Int,
    ) {
        AQUA("Aqua", 0xFF55FFFF.toInt()),
        YELLOW("Yellow", 0xFFFFFF55.toInt()),
        LIME("Lime", 0xFF55FF55.toInt()),
        WHITE("White", 0xFFFFFFFF.toInt()),
        RED("Red", 0xFFFF5555.toInt()),
        ;

        override fun toString() = displayName
    }
}
