package at.hannibal2.skyhanni.features.slayer.enderman

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.getNameTagWith
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.EnderMan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object VoidgloomSeraphApi {

    private const val TIER = 4
    private const val MAXIMUM_HITS = 100
    private const val DAMAGE_PHASES = 6
    private const val HITSHIELD_PHASES = 3
    private const val RADIATION_PHASES = 3

    private val patternGroup = RepoPattern.group("slayer.enderman.voidgloom")

    private val hitshieldPattern by patternGroup.pattern(
        "hitshield",
        ".* §[5fd]§l(?<hits>\\d+) Hits?",
    )

    private val radiationDuration = 8.2.seconds

    private var trackedBoss: Mob? = null
    private var currentHitshield: Phase.Hitshield? = null

    val currentBoss: Mob?
        get() = trackedBoss?.takeIf { it.isAlive }

    val currentPhase: Phase?
        get() {
            if (!isTierFourQuest()) return null

            return when (SlayerApi.state) {
                SlayerApi.ActiveQuestState.GRINDING -> Phase.Kills(SlayerApi.latestProgress)
                SlayerApi.ActiveQuestState.BOSS_FIGHT -> getBossPhase()
                else -> null
            }
        }

    val isRadiationActive: Boolean
        get() = getRadiationVehicle() != null

    val radiationTimeLeft: Duration?
        get() {
            val vehicle = getRadiationVehicle() ?: return null
            return (radiationDuration - vehicle.tickCount.ticks)
                .coerceAtLeast(Duration.ZERO)
        }

    fun isTierFourQuest(): Boolean {
        if (SlayerApi.activeType != SlayerType.VOID) return false
        if (SlayerApi.tier != TIER) return false

        return SlayerApi.state == SlayerApi.ActiveQuestState.GRINDING ||
            SlayerApi.state == SlayerApi.ActiveQuestState.BOSS_FIGHT
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob

        if (mob.name != "Voidgloom Seraph") return
        if (mob.levelOrTier != TIER) return
        if (!mob.belongsToPlayer()) return

        trackedBoss = mob
        currentHitshield = findHitshield(mob)
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob !== trackedBoss) return
        clearBoss()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onTick() {
        currentHitshield = currentBoss?.let(::findHitshield)
    }

    @HandleEvent(SlayerChangeEvent::class)
    private fun onSlayerChange() {
        clearBoss()
    }

    @HandleEvent
    private fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        if (
            event.state != SlayerApi.ActiveQuestState.GRINDING &&
            event.state != SlayerApi.ActiveQuestState.BOSS_FIGHT
        ) {
            clearBoss()
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() {
        clearBoss()
    }

    private fun getBossPhase(): Phase? {
        val boss = currentBoss ?: return null
        val damagePhase = getDamagePhase(boss)

        if (isRadiationActive) {
            return Phase.Radiation(
                index = (damagePhase.index / 2).coerceIn(1, RADIATION_PHASES),
                total = RADIATION_PHASES,
            )
        }

        currentHitshield?.let { return it }
        return damagePhase
    }

    private fun findHitshield(boss: Mob): Phase.Hitshield? {
        val enderman = boss.baseEntity as? EnderMan ?: return null
        val armorStand = enderman.getNameTagWith(3, " Hit") ?: return null

        val hits = hitshieldPattern.matchMatcher(
            armorStand.name.formattedTextCompatLessResets(),
        ) {
            group("hits").toInt()
        } ?: return null

        val damagePhase = getDamagePhase(boss)
        val hitshieldIndex = ((damagePhase.index + 1) / 2)
            .coerceIn(1, HITSHIELD_PHASES)

        return Phase.Hitshield(
            index = hitshieldIndex,
            total = HITSHIELD_PHASES,
            hits = hits,
            maximumHits = MAXIMUM_HITS,
        )
    }

    private fun getDamagePhase(boss: Mob): Phase.Damage {
        val maximumHealth = boss.maxHealth.toDouble()

        if (maximumHealth <= 0.0) {
            return Phase.Damage(
                index = 1,
                total = DAMAGE_PHASES,
            )
        }

        val health = boss.health.toDouble().coerceAtLeast(0.0)
        val index = (1..DAMAGE_PHASES).firstOrNull { phase ->
            val threshold = maximumHealth *
                (DAMAGE_PHASES - phase).toDouble() /
                DAMAGE_PHASES

            health > threshold
        } ?: DAMAGE_PHASES

        return Phase.Damage(
            index = index,
            total = DAMAGE_PHASES,
        )
    }

    private fun getRadiationVehicle(): Entity? {
        val boss = currentBoss ?: return null
        val enderman = boss.baseEntity as? EnderMan ?: return null
        return enderman.vehicle
    }

    private fun clearBoss() {
        trackedBoss = null
        currentHitshield = null
    }

    sealed interface Phase {

        data class Kills(
            val progress: String,
        ) : Phase

        data class Hitshield(
            val index: Int,
            val total: Int,
            val hits: Int,
            val maximumHits: Int,
        ) : Phase

        data class Damage(
            val index: Int,
            val total: Int,
        ) : Phase

        data class Radiation(
            val index: Int,
            val total: Int,
        ) : Phase
    }
}
