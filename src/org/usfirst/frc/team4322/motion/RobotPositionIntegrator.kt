package org.usfirst.frc.team4322.motion

import org.tenkiv.physikal.core.convertTo
import org.tenkiv.physikal.core.div
import org.tenkiv.physikal.core.plus
import org.usfirst.frc.team4322.math.Rotation
import org.usfirst.frc.team4322.math.TemporalLerpMap
import org.usfirst.frc.team4322.math.Transform
import org.usfirst.frc.team4322.math.Twist
import systems.uom.common.Imperial.INCH
import javax.measure.Quantity
import javax.measure.quantity.Length

object RobotPositionIntegrator
{

    private val posTracker = TemporalLerpMap<Transform>()
    var distanceDriven : Double = 0.0
    var lastVelocity: Twist = Twist.identity
    var lastUpdate : Transform = Transform()

    fun update(timestamp : Double, leftEncoderDeltaDistance : Quantity<Length>, rightEncoderDeltaDistance : Quantity<Length>, currentGyroAngle : Rotation) {
        val delta = Twist(((leftEncoderDeltaDistance + rightEncoderDeltaDistance) / 2.0).convertTo(INCH).value.toDouble(), 0.0, lastUpdate.rotation.inverse().rotateBy(currentGyroAngle).radians())
        distanceDriven += delta.dx
        lastVelocity = delta
        lastUpdate = lastUpdate.transformBy(Transform.fromArc(delta))
        posTracker[timestamp] = lastUpdate
    }

    fun getCurrentPose() : Transform {
        return lastUpdate.transformBy(Transform.fromArc(lastVelocity))
    }

    fun reset() {
        lastUpdate = Transform()
        lastVelocity = Twist.identity
    }

    fun getFuturePose(lookahead : Double) : Transform {
        return lastUpdate.transformBy(Transform.fromArc(lastVelocity.scale(lookahead)))
    }

}