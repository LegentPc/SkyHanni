package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbility
import at.hannibal2.skyhanni.features.slayer.enderman.VoidgloomSeraphApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration

@SkyHanniModule
object SoulcryDisplay {

    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.soulcry
    private val soulcry = ItemAbility.ATOMSPLIT_KATANA

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiRenderOverlay() {
        if (!config.displayTimer) return
        if (!VoidgloomSeraphApi.isTierFourQuest()) return

        val timeLeft = (soulcry.lastActivation + soulcry.getCooldown()).timeUntil()
        if (timeLeft <= Duration.ZERO) return

        config.position.renderRenderable(
            Renderable.text("§dSoulcry: §b${timeLeft.format(showMilliSeconds = true)}"),
            posLabel = "Soulcry Timer",
        )
    }
}
