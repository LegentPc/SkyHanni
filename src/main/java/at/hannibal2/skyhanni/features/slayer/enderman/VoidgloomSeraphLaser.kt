package at.hannibal2.skyhanni.features.slayer.enderman

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ParticleChangeEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.particles.ColorParticleOption
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

    private const val SIGNATURE_A_X = 0.23137255f
    private const val SIGNATURE_A_Y = 0.05490196f
    private const val SIGNATURE_A_Z = 0.21568628f

    private const val SIGNATURE_B_X = 0.7372549f
    private const val SIGNATURE_B_Y = 0.14509805f
    private const val SIGNATURE_B_Z = 0.14509805f

    private const val NOT_CAPTURED = "not captured"

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

    private var particleEvents = 0L
    private var dustEvents = 0L
    private var configEnabledEvents = 0L
    private var tierFourEvents = 0L
    private var radiationEvents = 0L
    private var ownBossEvents = 0L
    private var longDistanceEvents = 0L
    private var signatureEvents = 0L
    private var bossDistanceEvents = 0L
    private var nearOwnBossEvents = 0L
    private var colorParticleOptionEvents = 0L
    private var recoloredEvents = 0L

    private var eventOptionClass = NOT_CAPTURED
    private var packetOptionClass = NOT_CAPTURED
    private var optionDescription = NOT_CAPTURED
    private var optionHierarchy = NOT_CAPTURED
    private var optionFields = NOT_CAPTURED
    private var optionConstructors = NOT_CAPTURED
    private var optionMethods = NOT_CAPTURED
    private var observedColor = NOT_CAPTURED
    private var assignedColor = NOT_CAPTURED
    private var observedAlpha = NOT_CAPTURED

    private var minimumBossDistance: Double? = null
    private var maximumBossDistance: Double? = null

    @HandleEvent(
        priority = HandleEvent.LOWEST,
        onlyOnSkyblock = true,
    )
    private fun onParticleChange(event: ParticleChangeEvent) {
        particleEvents++

        val packet = event.packet
        if (packet.particle.type != ParticleTypes.DUST) return
        dustEvents++

        val particleOptions = event.particleOptions
        captureOptionMetadata(
            eventOptions = particleOptions,
            packetOptions = packet.particle,
        )

        if (!config.staticLaserColor) return
        configEnabledEvents++

        if (!VoidgloomSeraphApi.isTierFourQuest()) return
        tierFourEvents++

        if (!VoidgloomSeraphApi.isRadiationActive) return
        radiationEvents++

        val ownBoss = VoidgloomSeraphApi.currentBoss ?: return
        ownBossEvents++

        if (!packet.isOverrideLimiter) return
        longDistanceEvents++

        if (!hasRadiationSignature(packet)) return
        signatureEvents++

        val bossDistance = getBossDistance(
            packet = packet,
            ownBoss = ownBoss,
        )

        bossDistanceEvents++
        recordBossDistance(bossDistance)

        if (bossDistance > MAXIMUM_SOURCE_DISTANCE) return
        nearOwnBossEvents++

        if (particleOptions !is ColorParticleOption) return
        colorParticleOptionEvents++

        observedColor = particleOptions.color.toString()
        observedAlpha = particleOptions.alpha.toString()

        val targetColor = config.laserColor.argb
        particleOptions.color = targetColor
        assignedColor = particleOptions.color.toString()

        event.particleOptions = particleOptions
        recoloredEvents++
    }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Voidgloom Laser")
        event.addData {
            add("staticLaserColorEnabled: ${config.staticLaserColor}")
            add("tierFourQuestActive: ${VoidgloomSeraphApi.isTierFourQuest()}")
            add("radiationCurrentlyActive: ${VoidgloomSeraphApi.isRadiationActive}")
            add("ownBossCurrentlyPresent: ${VoidgloomSeraphApi.currentBoss != null}")
            add("particleEvents: $particleEvents")
            add("dustEvents: $dustEvents")
            add("configEnabledEvents: $configEnabledEvents")
            add("tierFourEvents: $tierFourEvents")
            add("radiationEvents: $radiationEvents")
            add("ownBossEvents: $ownBossEvents")
            add("longDistanceEvents: $longDistanceEvents")
            add("signatureEvents: $signatureEvents")
            add("bossDistanceEvents: $bossDistanceEvents")
            add("nearOwnBossEvents: $nearOwnBossEvents")
            add("colorParticleOptionEvents: $colorParticleOptionEvents")
            add("recoloredEvents: $recoloredEvents")
            add("bossDistanceRange: ${getBossDistanceRange()}")
            add("eventOptionClass: $eventOptionClass")
            add("packetOptionClass: $packetOptionClass")
            add("optionDescription: $optionDescription")
            add("optionHierarchy: $optionHierarchy")
            add("optionFields: $optionFields")
            add("optionConstructors: $optionConstructors")
            add("optionMethods: $optionMethods")
            add("observedColor: $observedColor")
            add("assignedColor: $assignedColor")
            add("observedAlpha: $observedAlpha")
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() {
        resetDiagnostics()
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

    private fun recordBossDistance(distance: Double) {
        minimumBossDistance = minimumBossDistance
            ?.let { minimum -> minOf(minimum, distance) }
            ?: distance

        maximumBossDistance = maximumBossDistance
            ?.let { maximum -> maxOf(maximum, distance) }
            ?: distance
    }

    private fun getBossDistanceRange(): String {
        val minimum = minimumBossDistance ?: return NOT_CAPTURED
        val maximum = maximumBossDistance ?: return NOT_CAPTURED
        return "$minimum..$maximum"
    }

    private fun captureOptionMetadata(
        eventOptions: Any,
        packetOptions: Any,
    ) {
        val eventClass = eventOptions.javaClass
        val packetClass = packetOptions.javaClass
        val eventClassName = eventClass.name
        val packetClassName = packetClass.name

        optionDescription = runCatching {
            eventOptions.toString().replace("\n", "\\n")
        }.getOrElse { exception ->
            "error: ${exception.javaClass.name}: ${exception.message}"
        }

        if (
            eventOptionClass == eventClassName &&
            packetOptionClass == packetClassName
        ) {
            return
        }

        eventOptionClass = eventClassName
        packetOptionClass = packetClassName

        val hierarchy = getClassHierarchy(eventClass)

        optionHierarchy = hierarchy.joinToString(" -> ") { type ->
            type.name
        }

        optionFields = runCatching {
            hierarchy
                .flatMap { owner ->
                    owner.declaredFields.map { field ->
                        "${owner.name}.${field.name}: ${field.type.typeName}"
                    }
                }
                .sorted()
                .ifEmpty { listOf("none") }
                .joinToString(" | ")
        }.getOrElse { exception ->
            "error: ${exception.javaClass.name}: ${exception.message}"
        }

        optionConstructors = runCatching {
            hierarchy
                .flatMap { owner ->
                    owner.declaredConstructors.map { constructor ->
                        val parameters = constructor.parameterTypes
                            .joinToString(", ") { parameter ->
                                parameter.typeName
                            }

                        "${owner.name}($parameters)"
                    }
                }
                .sorted()
                .ifEmpty { listOf("none") }
                .joinToString(" | ")
        }.getOrElse { exception ->
            "error: ${exception.javaClass.name}: ${exception.message}"
        }

        optionMethods = runCatching {
            hierarchy
                .flatMap { owner ->
                    owner.declaredMethods.map { method ->
                        val parameters = method.parameterTypes
                            .joinToString(", ") { parameter ->
                                parameter.typeName
                            }

                        "${owner.name}.${method.name}($parameters): " +
                            method.returnType.typeName
                    }
                }
                .sorted()
                .ifEmpty { listOf("none") }
                .joinToString(" | ")
        }.getOrElse { exception ->
            "error: ${exception.javaClass.name}: ${exception.message}"
        }
    }

    private fun getClassHierarchy(
        initialClass: Class<*>,
    ): List<Class<*>> {
        val hierarchy = mutableListOf<Class<*>>()
        var currentClass: Class<*>? = initialClass

        while (true) {
            val capturedClass = currentClass ?: break
            hierarchy.add(capturedClass)
            currentClass = capturedClass.superclass
        }

        return hierarchy
    }

    private fun resetDiagnostics() {
        particleEvents = 0L
        dustEvents = 0L
        configEnabledEvents = 0L
        tierFourEvents = 0L
        radiationEvents = 0L
        ownBossEvents = 0L
        longDistanceEvents = 0L
        signatureEvents = 0L
        bossDistanceEvents = 0L
        nearOwnBossEvents = 0L
        colorParticleOptionEvents = 0L
        recoloredEvents = 0L

        eventOptionClass = NOT_CAPTURED
        packetOptionClass = NOT_CAPTURED
        optionDescription = NOT_CAPTURED
        optionHierarchy = NOT_CAPTURED
        optionFields = NOT_CAPTURED
        optionConstructors = NOT_CAPTURED
        optionMethods = NOT_CAPTURED
        observedColor = NOT_CAPTURED
        assignedColor = NOT_CAPTURED
        observedAlpha = NOT_CAPTURED

        minimumBossDistance = null
        maximumBossDistance = null
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
