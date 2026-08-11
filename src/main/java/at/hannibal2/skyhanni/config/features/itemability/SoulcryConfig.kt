package at.hannibal2.skyhanni.config.features.itemability

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SoulcryConfig {

    @Expose
    @ConfigOption(
        name = "Soulcry Cooldown",
        desc = "Show the Soulcry cooldown and READY state while an active Tier IV Voidgloom quest is running.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var displayTimer: Boolean = false

    @Expose
    @ConfigLink(owner = SoulcryConfig::class, field = "displayTimer")
    val position: Position = Position(10, 100)
}
