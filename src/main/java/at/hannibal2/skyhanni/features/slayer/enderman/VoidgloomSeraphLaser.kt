package at.hannibal2.skyhanni.features.slayer.enderman

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.ParticleChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.particles.ColorParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.monster.EnderMan

@SkyHanniModule
object VoidgloomSeraphLaser {

    private const val TIER_FOUR = 4
    private const val REQUIRED_PARTICLE_COUNT = 0
    private const val REQUIRED_PARTICLE_SPEED = 1.0f
    private const val MAXIMUM_SOURCE_DISTANCE = 22.5

    private const val SIGNATURE_A_X = 0.23137255f
    private const val SIGNATURE_A_Y = 0.05490196f
    private const val SIGNATURE_A_Z = 0.21568628f

    private const val SIGNATURE_B_X = 0.7372549f
    private const val SIGNATURE_B_Y = 0.14509805f
    private const val SIGNATURE_B_Z = 0.14509805f

    private val signatureA = LaserOffset(
        x = SIGNATURE_A_X,
        y = SIGNATURE_A_Y,
        z = SIGNATURE_A_Z,
    )

    private val signatureB = LaserOffset(
        x = SIGNATURE_B_X,
        y = SIGNATURE_B_Y,
        z = SIGNATURE_B_Z,
    )

    private val config get() = SlayerApi.config.endermen.voidgloom

    @HandleEvent(
        priority = HandleEvent.LOWEST,
        onlyOnSkyblock = true,
    )
    private fun onParticleChange(event: ParticleChangeEvent) {
        val particle = event.particleOptions as? ColorParticleOption

        if (particle != null && shouldRecolor(event.packet)) {
            particle.color = config.laserColor.argb
            event.particleOptions = particle
        }
    }

    private fun shouldRecolor(
        packet: ClientboundLevelParticlesPacket,
    ): Boolean {
        val ownBoss = VoidgloomSeraphApi.currentBoss

        return config.staticLaserColor &&
            VoidgloomSeraphApi.isTierFourQuest() &&
            VoidgloomSeraphApi.isRadiationActive &&
            ownBoss != null &&
            packet.particle.type == ParticleTypes.DUST &&
            packet.count == REQUIRED_PARTICLE_COUNT &&
            packet.maxSpeed == REQUIRED_PARTICLE_SPEED &&
            packet.isOverrideLimiter &&
            isRadiationLaserOffset(
                x = packet.xDist,
                y = packet.yDist,
                z = packet.zDist,
            ) &&
            findNearestRadiationBoss(packet.toLorenzVec()) === ownBoss
    }

    private fun findNearestRadiationBoss(
        particlePosition: LorenzVec,
    ): Mob? = MobData.skyblockMobs
        .asSequence()
        .filter(::isRadiatingTierFourVoidgloom)
        .minByOrNull {
            it.baseEntity.getLorenzVec().distance(particlePosition)
        }
        ?.takeIf {
            it.baseEntity
                .getLorenzVec()
                .distance(particlePosition) <= MAXIMUM_SOURCE_DISTANCE
        }

    private fun isRadiatingTierFourVoidgloom(mob: Mob): Boolean {
        val enderman = mob.baseEntity as? EnderMan

        return mob.isAlive &&
            mob.name == "Voidgloom Seraph" &&
            mob.levelOrTier == TIER_FOUR &&
            enderman?.vehicle != null
    }

    private fun isRadiationLaserOffset(
        x: Float,
        y: Float,
        z: Float,
    ): Boolean {
        return matchesOffset(x, y, z, signatureA) ||
            matchesOffset(x, y, z, signatureB)
    }

    private fun matchesOffset(
        x: Float,
        y: Float,
        z: Float,
        expected: LaserOffset,
    ): Boolean {
        return x == expected.x &&
            y == expected.y &&
            z == expected.z
    }

    private data class LaserOffset(
        val x: Float,
        val y: Float,
        val z: Float,
    )
}
