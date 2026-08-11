package at.hannibal2.skyhanni.features.slayer.enderman

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object VoidgloomSeraphDisplay {

    private val config get() = SlayerApi.config.endermen.voidgloom

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiRenderOverlay() {
        if (config.phaseDisplay) {
            VoidgloomSeraphApi.currentPhase?.let { phase ->
                config.phasePosition.renderRenderable(
                    Renderable.text(phase.toDisplayText()),
                    posLabel = "Voidgloom Phase",
                )
            }
        }

        if (config.laserTimer && VoidgloomSeraphApi.isTierFourQuest()) {
            VoidgloomSeraphApi.radiationTimeLeft?.let { timeLeft ->
                config.laserTimerPosition.renderRenderable(
                    Renderable.text("§dLaser: §b${timeLeft.format(showMilliSeconds = true)}"),
                    posLabel = "Voidgloom Laser Timer",
                )
            }
        }
    }

    private fun VoidgloomSeraphApi.Phase.toDisplayText(): String = when (this) {
        is VoidgloomSeraphApi.Phase.Kills -> {
            val progressText = if (progress.isBlank()) "" else " §7- $progress"
            "§5Voidgloom: §eKills$progressText"
        }

        is VoidgloomSeraphApi.Phase.Hitshield ->
            "§5Voidgloom: §dHitshield §f$index/$total §7- §e$hits§7/$maximumHits hits"

        is VoidgloomSeraphApi.Phase.Damage ->
            "§5Voidgloom: §cDamage §f$index/$total"

        is VoidgloomSeraphApi.Phase.Radiation ->
            "§5Voidgloom: §bRadiation §f$index/$total"
    }
}
