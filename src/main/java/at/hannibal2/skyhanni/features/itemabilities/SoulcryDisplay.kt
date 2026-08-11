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
import at.hannibal2.skyhanni.utils.InventoryUtils
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
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SoulcryDisplay {

    private const val SOULCRY_SOUND = "entity.ghast.ambient"
    private const val SOULCRY_SOUND_PITCH = 0.4920635f
    private const val SOULCRY_SOUND_VOLUME = 0.15f
    private const val MAXIMUM_LOCAL_SOUND_DISTANCE = 2.0

    private const val SOULCRY_COOLDOWN_SECONDS = 4
    private const val CLICK_CONFIRMATION_SECONDS = 1
    private const val DUPLICATE_CONFIRMATION_SECONDS = 2

    private const val MILLISECONDS_PER_TENTH = 100.0
    private const val TENTHS_PER_SECOND = 10.0

    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.soulcry
    private val atomsplitKatana = "ATOMSPLIT_KATANA".toInternalName()

    private val soulcryCooldown = SOULCRY_COOLDOWN_SECONDS.seconds
    private val clickConfirmationWindow = CLICK_CONFIRMATION_SECONDS.seconds
    private val duplicateConfirmationWindow =
        DUPLICATE_CONFIRMATION_SECONDS.seconds

    private var pendingClick = SimpleTimeMark.farPast()
    private var lastActivation = SimpleTimeMark.farPast()
    private var lastConfirmation = SimpleTimeMark.farPast()
    private var soulcryInLastActionBar = false

    @HandleEvent(onlyOnSkyblock = true)
    private fun onItemClick(event: ItemClickEvent) {
        val isAtomsplitRightClick =
            config.displayTimer &&
                VoidgloomSeraphApi.isTierFourQuest() &&
                event.clickType == InteractClickType.RIGHT_CLICK &&
                event.itemInHand?.getInternalName() == atomsplitKatana

        if (isAtomsplitRightClick) {
            pendingClick = SimpleTimeMark.now()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        val canTrack =
            config.displayTimer &&
                VoidgloomSeraphApi.isTierFourQuest()

        val containsSoulcry =
            canTrack &&
                event.actionBar.removeColor().contains("Soulcry")

        if (containsSoulcry && !soulcryInLastActionBar) {
            confirmActivation()
        }

        soulcryInLastActionBar = containsSoulcry
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onPlaySound(event: PlaySoundEvent) {
        val isSoulcrySound =
            config.displayTimer &&
                VoidgloomSeraphApi.isTierFourQuest() &&
                event.soundName == SOULCRY_SOUND &&
                event.pitch == SOULCRY_SOUND_PITCH &&
                event.volume == SOULCRY_SOUND_VOLUME &&
                event.distanceToPlayer <= MAXIMUM_LOCAL_SOUND_DISTANCE

        if (isSoulcrySound && canConfirmSound()) {
            confirmActivation()
        }
    }

    @HandleEvent(
        GuiRenderEvent.GuiOverlayRenderEvent::class,
        onlyOnSkyblock = true,
    )
    private fun onGuiRenderOverlay() {
        if (
            config.displayTimer &&
            VoidgloomSeraphApi.isTierFourQuest()
        ) {
            config.position.renderRenderable(
                Renderable.text(createDisplayText()),
                posLabel = "Soulcry Cooldown",
            )
        }
    }

    @HandleEvent(SlayerChangeEvent::class)
    private fun onSlayerChange() {
        reset()
    }

    @HandleEvent
    private fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        val isActiveQuestState =
            event.state == SlayerApi.ActiveQuestState.GRINDING ||
                event.state == SlayerApi.ActiveQuestState.BOSS_FIGHT

        if (!isActiveQuestState) {
            reset()
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() {
        reset()
    }

    private fun canConfirmSound(): Boolean {
        val recentlyClicked =
            !pendingClick.isFarPast() &&
                pendingClick.passedSince() <= clickConfirmationWindow

        val holdingAtomsplit =
            InventoryUtils.getItemInHand()?.getInternalName() ==
                atomsplitKatana

        return recentlyClicked || holdingAtomsplit
    }

    private fun confirmActivation() {
        val canConfirm =
            lastConfirmation.isFarPast() ||
                lastConfirmation.passedSince() >=
                duplicateConfirmationWindow

        if (canConfirm) {
            val now = SimpleTimeMark.now()
            lastActivation = now
            lastConfirmation = now
            pendingClick = SimpleTimeMark.farPast()
        }
    }

    private fun createDisplayText(): String {
        val isReady =
            lastActivation.isFarPast() ||
                lastActivation.passedSince() >= soulcryCooldown

        return if (isReady) {
            "§aSoulcry: READY"
        } else {
            val elapsed = lastActivation.passedSince()
            val remaining =
                (soulcryCooldown - elapsed).coerceAtLeast(Duration.ZERO)

            val tenths =
                (remaining.inWholeMilliseconds /
                    MILLISECONDS_PER_TENTH).roundToInt()

            val seconds = String.format(
                Locale.US,
                "%.1f",
                tenths / TENTHS_PER_SECOND,
            )

            "§dSoulcry: §b${seconds}s"
        }
    }

    private fun reset() {
        pendingClick = SimpleTimeMark.farPast()
        lastActivation = SimpleTimeMark.farPast()
        lastConfirmation = SimpleTimeMark.farPast()
        soulcryInLastActionBar = false
    }
}
