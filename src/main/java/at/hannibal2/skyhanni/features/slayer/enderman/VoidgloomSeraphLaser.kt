package at.hannibal2.skyhanni.features.slayer.enderman

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.ParticleChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.particles.ColorParticleOption
import kotlin.math.abs

@SkyHanniModule
object VoidgloomSeraphLaser {

    private const val MAX_HORIZONTAL_DISTANCE = 12.0
    private const val MAX_VERTICAL_DISTANCE = 4.0

    private val config get() = SlayerApi.config.endermen.voidgloom

    @HandleEvent(
        priority = HandleEvent.LOWEST,
        onlyOnSkyblock = true,
    )
    private fun onParticleChange(event: ParticleChangeEvent) {
        if (!config.staticLaserColor) return
        if (!VoidgloomSeraphApi.isTierFourQuest()) return

        val particle = event.particleOptions as? ColorParticleOption ?: return
        val boss = VoidgloomSeraphApi.currentBoss ?: return
        if (!VoidgloomSeraphApi.isRadiationActive) return

        val bossPosition = boss.baseEntity.getLorenzVec()
        val particlePosition = event.packet.toLorenzVec()

        if (particlePosition.distanceIgnoreY(bossPosition) > MAX_HORIZONTAL_DISTANCE) return
        if (abs(particlePosition.y - bossPosition.y) > MAX_VERTICAL_DISTANCE) return

        particle.color = config.laserColor.argb
        event.particleOptions = particle
    }
}
