package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.features.slayer.enderman.VoidgloomSeraphApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SoulcryDisplay {

    private const val SOULCRY_SOUND = "entity.ghast.ambient"
    private const val SOULCRY_SOUND_PITCH = 0.4920635f
    private const val SOULCRY_SOUND_VOLUME = 0.15f

    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.soulcry
    private val atomsplitKatana = "ATOMSPLIT_KATANA".toInternalName()
    private val soulcryCooldown = 4.seconds
    private val confirmationWindow = 1.seconds
    private val duplicateConfirmationWindow = 500.milliseconds

    private var pendingClick = SimpleTimeMark.farPast()
    private var lastActivation = SimpleTimeMark.farPast()
    private var lastConfirmation = SimpleTimeMark.farPast()
    private var soulcryInLastActionBar = false

    @HandleEvent(onlyOnSkyblock = true)
    private fun onItemClick(event: ItemClickEvent) {
        if (!config.displayTimer) return
        if (!VoidgloomSeraphApi.isTierFourQuest()) return
        if (event.clickType != InteractClickType.RIGHT_CLICK) return
        if (event.itemInHand?.getInternalName() != atomsplitKatana) return

        pendingClick = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        if (!config.displayTimer || !VoidgloomSeraphApi.isTierFourQuest()) {
            soulcryInLastActionBar = false
            return
        }

        val containsSoulcry = event.actionBar.removeColor().contains("Soulcry")

        if (containsSoulcry && !soulcryInLastActionBar) {
            confirmActivation()
        }

        soulcryInLastActionBar = containsSoulcry
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onPlaySound(event: PlaySoundEvent) {
        if (!config.displayTimer) return
        if (!VoidgloomSeraphApi.isTierFourQuest()) return
        if (event.soundName != SOULCRY_SOUND) return
        if (event.pitch != SOULCRY_SOUND_PITCH) return
        if (event.volume != SOULCRY_SOUND_VOLUME) return

        confirmActivation()
    }

    @HandleEvent(
        GuiRenderEvent.GuiOverlayRenderEvent::class,
        onlyOnSkyblock = true,
    )
    private fun onGuiRenderOverlay() {
        if (!config.displayTimer) return
        if (!VoidgloomSeraphApi.isTierFourQuest()) return

        config.position.renderRenderable(
            Renderable.text(createDisplayText()),
            posLabel = "Soulcry Cooldown",
        )
    }

    @HandleEvent(SlayerChangeEvent::class)
    private fun onSlayerChange() {
        reset()
    }

    @HandleEvent
    private fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        if (
            event.state != SlayerApi.ActiveQuestState.GRINDING &&
            event.state != SlayerApi.ActiveQuestState.BOSS_FIGHT
        ) {
            reset()
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() {
        reset()
    }

    private fun confirmActivation() {
        if (pendingClick.isFarPast()) return

        if (pendingClick.passedSince() > confirmationWindow) {
            pendingClick = SimpleTimeMark.farPast()
            return
        }

        if (lastConfirmation.passedSince() < duplicateConfirmationWindow) {
            pendingClick = SimpleTimeMark.farPast()
            return
        }

        val now = SimpleTimeMark.now()
        lastActivation = now
        lastConfirmation = now
        pendingClick = SimpleTimeMark.farPast()
    }

    private fun createDisplayText(): String {
        if (lastActivation.isFarPast()) {
            return "§aSoulcry: READY"
        }

        val elapsed = lastActivation.passedSince()

        if (elapsed >= soulcryCooldown) {
            return "§aSoulcry: READY"
        }

        val remaining = (soulcryCooldown - elapsed).coerceAtLeast(Duration.ZERO)
        val tenths = (remaining.inWholeMilliseconds / 100.0).roundToInt()
        val seconds = String.format(Locale.US, "%.1f", tenths / 10.0)

        return "§dSoulcry: §b${seconds}s"
    }

    private fun reset() {
        pendingClick = SimpleTimeMark.farPast()
        lastActivation = SimpleTimeMark.farPast()
        lastConfirmation = SimpleTimeMark.farPast()
        soulcryInLastActionBar = false
    }
}
