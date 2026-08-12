package at.hannibal2.skyhanni.features.slayer.enderman

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.ParticleChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import kotlin.math.abs

@SkyHanniModule
object VoidgloomSeraphLaser {

    private const val CURRENT_PARTICLE_COUNT = 1
    private const val CURRENT_PARTICLE_SPEED = 0.0f
    private const val CURRENT_OFFSET = 0.0f

    private const val LEGACY_PARTICLE_COUNT = 0
    private const val LEGACY_PARTICLE_SPEED = 1.0f

    private const val MAXIMUM_SOURCE_DISTANCE = 22.5
    private const val OFFSET_TOLERANCE = 0.000001f
    private const val RGB_MASK = 0x00FFFFFF

    private const val SIGNATURE_A_X = 0.23137255f
    private const val SIGNATURE_A_Y = 0.05490196f
    private const val SIGNATURE_A_Z = 0.21568628f

    private const val SIGNATURE_B_X = 0.7372549f
    private const val SIGNATURE_B_Y = 0.14509805f
    private const val SIGNATURE_B_Z = 0.14509805f

    private val currentSignature = LaserOffset(
        x = CURRENT_OFFSET,
        y = CURRENT_OFFSET,
        z = CURRENT_OFFSET,
    )

    private val legacySignatureA = LaserOffset(
        x = SIGNATURE_A_X,
        y = SIGNATURE_A_Y,
        z = SIGNATURE_A_Z,
    )

    private val legacySignatureB = LaserOffset(
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
        val packet = event.packet
        if (packet.particle.type != ParticleTypes.DUST) return
        if (!config.staticLaserColor) return
        if (!VoidgloomSeraphApi.isTierFourQuest()) return
        if (!VoidgloomSeraphApi.isRadiationActive) return

        val ownBoss = VoidgloomSeraphApi.currentBoss ?: return

        if (!packet.isOverrideLimiter) return
        if (!hasRadiationSignature(packet)) return
        if (getBossDistance(packet, ownBoss) > MAXIMUM_SOURCE_DISTANCE) return

        val particleOptions = event.particleOptions
        if (particleOptions !is DustParticleOptions) return

        event.particleOptions = DustParticleOptions(
            config.laserColor.argb and RGB_MASK,
            particleOptions.scale,
        )
    }

    private fun hasRadiationSignature(
        packet: ClientboundLevelParticlesPacket,
    ): Boolean {
        return hasCurrentSignature(packet) ||
            hasLegacySignature(packet)
    }

    private fun hasCurrentSignature(
        packet: ClientboundLevelParticlesPacket,
    ): Boolean {
        return packet.count == CURRENT_PARTICLE_COUNT &&
            packet.maxSpeed == CURRENT_PARTICLE_SPEED &&
            matchesOffset(
                x = packet.xDist,
                y = packet.yDist,
                z = packet.zDist,
                expected = currentSignature,
            )
    }

    private fun hasLegacySignature(
        packet: ClientboundLevelParticlesPacket,
    ): Boolean {
        return packet.count == LEGACY_PARTICLE_COUNT &&
            packet.maxSpeed == LEGACY_PARTICLE_SPEED &&
            (
                matchesOffset(
                    x = packet.xDist,
                    y = packet.yDist,
                    z = packet.zDist,
                    expected = legacySignatureA,
                ) ||
                    matchesOffset(
                        x = packet.xDist,
                        y = packet.yDist,
                        z = packet.zDist,
                        expected = legacySignatureB,
                    )
                )
    }

    private fun getBossDistance(
        packet: ClientboundLevelParticlesPacket,
        ownBoss: Mob,
    ): Double {
        val particlePosition = packet.toLorenzVec()
        val bossPosition = ownBoss.baseEntity.getLorenzVec()
        return bossPosition.distance(particlePosition)
    }

    private fun matchesOffset(
        x: Float,
        y: Float,
        z: Float,
        expected: LaserOffset,
    ): Boolean {
        return abs(x - expected.x) <= OFFSET_TOLERANCE &&
            abs(y - expected.y) <= OFFSET_TOLERANCE &&
            abs(z - expected.z) <= OFFSET_TOLERANCE
    }

    private data class LaserOffset(
        val x: Float,
        val y: Float,
        val z: Float,
    )
}
