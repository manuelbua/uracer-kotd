
package com.bitfire.uracer.game.player;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.actors.CarDescriptor;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.VMath;

public final class CarSimulator {
	public CarDescriptor carDesc;
	private Vector2 velocity = new Vector2();
	private Vector2 acceleration_wc = new Vector2();
	private Vector2 flatf = new Vector2();
	private Vector2 flatr = new Vector2();
	private Vector2 ftraction = new Vector2();
	private Vector2 resistance = new Vector2();
	private Vector2 force = new Vector2();
	private Vector2 acceleration = new Vector2();
	private float rpmWheel = 0;
	private float thisSign, lastSign;
	private static final float DampingThrottle = AMath.damping(0.98f);

	// exports
	public Vector2 lateralForceFront, lateralForceRear;

	public CarSimulator (CarDescriptor carDesc) {
		this.carDesc = carDesc;
		thisSign = 1f;
		lastSign = 1f;

		// exports
		lateralForceFront = new Vector2();
		lateralForceRear = new Vector2();
	}

	public void applyInput (CarInput input) {
		float maxForce = carDesc.carModel.max_force;
		boolean hasDir = false, hasSteer = false;

		carDesc.brake = input.brake;
		if (input.updated) {
			// throttle
			if (AMath.fixup(input.throttle) > 0) {
				// acceleration
				if (input.throttle < maxForce) {
					carDesc.throttle = input.throttle;
				} else {
					carDesc.throttle = maxForce;
				}

				// carDesc.brake = 0;
				hasDir = true;
			} else if (AMath.fixup(input.throttle) < 0) {
				// deceleration
				if (input.throttle > -maxForce) {
					carDesc.throttle = input.throttle;
				} else {
					carDesc.throttle = -maxForce;
				}

				// carDesc.brake = 0;
				hasDir = true;
			}

			// steering
			if (AMath.fixup(input.steerAngle) < 0) {
				// left
				carDesc.steerangle = input.steerAngle;
				if (carDesc.steerangle < -AMath.PI_4) {
					carDesc.steerangle = -AMath.PI_4;
				}

				hasSteer = true;
			} else if (AMath.fixup(input.steerAngle) > 0) {
				// right
				carDesc.steerangle = input.steerAngle;
				if (carDesc.steerangle > AMath.PI_4) {
					carDesc.steerangle = AMath.PI_4;
				}

				hasSteer = true;
			}
		}

		if (!hasDir) {
			if (Math.abs(carDesc.velocity_wc.x) > 0.5f || Math.abs(carDesc.velocity_wc.y) > 0.5f) {
				if (!AMath.isZero(carDesc.throttle)) {
					carDesc.throttle *= DampingThrottle;
				}

				if (!AMath.isZero(carDesc.brake)) {
					carDesc.brake *= DampingThrottle;
				}

				// carDesc.brake = 200f;
			} else {
				carDesc.velocity_wc.set(0, 0);
				carDesc.angularvelocity = 0;
				carDesc.brake = 0;
				carDesc.throttle = 0;
			}
		}

		if (!hasSteer) {
			carDesc.steerangle = 0;
		}

		carDesc.throttle = AMath.clamp(carDesc.throttle, -maxForce, maxForce);
		carDesc.brake = AMath.clamp(carDesc.brake, -maxForce * 2, maxForce * 2);
	}

	public void step (float dt, float bodyAngle) {
		float sn = MathUtils.sin(AMath.normalRelativeAngle(-bodyAngle));
		float cs = MathUtils.cos(AMath.normalRelativeAngle(-bodyAngle));

		//
		// SAE convention: x is to the front of the car, y is to the right, z is
		// down
		//

		// car's velocity: Vlat and Vlong
		// transform velocity in world reference frame to velocity in car
		// reference frame
		velocity.x = cs * carDesc.velocity_wc.y + sn * carDesc.velocity_wc.x;
		velocity.y = -sn * carDesc.velocity_wc.y + cs * carDesc.velocity_wc.x;
		VMath.fixup(velocity);

		//
		// Lateral force on wheels
		//

		// Resulting velocity of the wheels as result of the yaw rate of the car
		// body
		// v = yawrate * r where r is distance of wheel to CG (approx. half
		// wheel base)
		// yawrate (ang.velocity) must be in rad/s
		//
		float yawspeed = carDesc.carModel.wheelbase * 0.5f * carDesc.angularvelocity;
		float sideslip = 0, rot_angle = 0;
		float slipanglefront = 0, slipanglerear = 0;

		// velocity.x = fVLong_, velocity.y = fVLat_
		// fix singularity
		if (AMath.isZero(velocity.x)) {
			rot_angle = 0;
			sideslip = 0;

			slipanglefront = sideslip + rot_angle;
			slipanglerear = sideslip - rot_angle;

		} else {
			// compute rotational angle
			rot_angle = MathUtils.atan2(yawspeed, velocity.x);

			// compute the side slip angle of the car (a.k.a. beta)
			sideslip = MathUtils.atan2(velocity.y, velocity.x);

			slipanglefront = sideslip + rot_angle - carDesc.steerangle;
			slipanglerear = sideslip - rot_angle;
		}

		// Calculate slip angles for front and rear wheels (a.k.a. alpha)
		// float slipanglefront = sideslip + rot_angle - carDesc.steerangle;
		// float slipanglerear = sideslip - rot_angle;

		// weight per axle = half car mass times 1G (=9.8m/s^2)
		// (precomputed during initialization)
		// weight = car->cartype->mass * 9.8f * 0.5f;

		// lateral force on front wheels = (Ca * slip angle) capped to friction circle * load
		flatf.x = 0;
		flatf.y = carDesc.carModel.stiffness_front * slipanglefront;
		flatf.y = Math.min(carDesc.carModel.max_grip, flatf.y);
		flatf.y = Math.max(-carDesc.carModel.max_grip, flatf.y);
		lateralForceFront.set(flatf);
		flatf.y *= carDesc.carModel.weight;

		// lateral force on rear wheels
		flatr.x = 0;
		flatr.y = carDesc.carModel.stiffness_rear * slipanglerear;
		flatr.y = Math.min(carDesc.carModel.max_grip, flatr.y);
		flatr.y = Math.max(-carDesc.carModel.max_grip, flatr.y);
		lateralForceRear.set(flatr);
		flatr.y *= carDesc.carModel.weight;

		// float s = SGN(velocity.x);
		// thisSign = AMath.lowpass(lastSign, AMath.sign(velocity.x), 0.2f);
		// lastSign = thisSign;
		thisSign = AMath.sign(velocity.x);

		ftraction.set(100f * (carDesc.throttle - carDesc.brake * thisSign), 0);

		// torque on body from lateral forces
		float torque = carDesc.carModel.b * flatf.y - carDesc.carModel.c * flatr.y;
		torque = AMath.fixup(torque);

		//
		// Forces and torque on body
		//

		// drag and rolling resistance
		resistance.x = -(carDesc.carModel.resistance * velocity.x + carDesc.carModel.drag * velocity.x * Math.abs(velocity.x));
		resistance.y = -(carDesc.carModel.resistance * velocity.y + carDesc.carModel.drag * velocity.y * Math.abs(velocity.y));

		// sum forces
		force.x = ftraction.x + MathUtils.sin(carDesc.steerangle) * flatf.x + flatr.x + resistance.x;
		force.y = ftraction.y + MathUtils.cos(carDesc.steerangle) * flatf.y + flatr.y + resistance.y;

		//
		// Acceleration
		//

		// Convert the force into acceleration
		// Newton F = m.a, therefore a = F/m
		acceleration.set(force.x * carDesc.carModel.invmass, force.y * carDesc.carModel.invmass);
		VMath.fixup(acceleration);

		//
		// Velocity and position
		//

		// transform acceleration from car reference frame to world reference
		// frame
		acceleration_wc.x = cs * acceleration.y + sn * acceleration.x;
		acceleration_wc.y = -sn * acceleration.y + cs * acceleration.x;
		VMath.fixup(acceleration_wc);

		// velocity is integrated acceleration
		carDesc.velocity_wc.x += dt * acceleration_wc.x;
		carDesc.velocity_wc.y += dt * acceleration_wc.y;
		VMath.fixup(carDesc.velocity_wc);

		// make sure vehicle doesn't exceed maximum velocity
		VMath.truncate(carDesc.velocity_wc, carDesc.carModel.max_speed);

		// Angular acceleration, angular velocity and heading

		float angular_acceleration = torque * carDesc.carModel.invinertia;

		// integrate angular acceleration to get angular velocity
		carDesc.angularvelocity += dt * angular_acceleration;
		carDesc.angularvelocity = AMath.fixup(carDesc.angularvelocity);

		//
		float degreeOfRotationPerFrame = ((velocity.len() * dt) / carDesc.carModel.wheellength) * 360f;
		float degreeOfRotationPerSecond = degreeOfRotationPerFrame * 30f;
		float rpsWheel = degreeOfRotationPerSecond / 360f;
		// float kmh = ((rpsWheel * carDesc.carModel.wheellength) * 3600f) / 1000f;
		rpmWheel = rpsWheel * 60;

		// Gdx.app.log("CarSimulator", "rpmWheel=" + rpmWheel );
	}

	public float getRpmWheel () {
		return rpmWheel;
	}

	public void resetPhysics () {
		carDesc.velocity_wc.set(0, 0);
		carDesc.angularvelocity = 0;
		carDesc.brake = 0;
		carDesc.throttle = 0;
		carDesc.steerangle = 0;
		acceleration_wc.set(0, 0);
		velocity.set(0, 0);
		thisSign = 1f;
		lastSign = 1f;
		lateralForceFront.set(0, 0);
		lateralForceRear.set(0, 0);
	}
}
