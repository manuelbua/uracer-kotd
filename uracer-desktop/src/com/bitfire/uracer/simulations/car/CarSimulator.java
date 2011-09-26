package com.bitfire.uracer.simulations.car;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.VMath;

public class CarSimulator
{
	public CarDescriptor carDesc;
	public Vector2 lastCarScreenPos = new Vector2(), lastTouchPos = new Vector2(), velocity = new Vector2(),
			acceleration_wc = new Vector2(), heading = new Vector2(), side = new Vector2(), flatf = new Vector2(),
			flatr = new Vector2(), ftraction = new Vector2(), resistance = new Vector2(), force = new Vector2(),
			acceleration = new Vector2();

	public float thisSign, lastSign, lastTouchAngle;

	private float dampingThrottle = 0.98f;
	private float dampingThrottleFrame;

	// exports
	public Vector2 lateralForceFront, lateralForceRear;

	public CarSimulator( CarDescriptor carDesc )
	{
		this.carDesc = carDesc;
		thisSign = lastSign = 1f;
		lastTouchAngle = 0;

		// precompute constants
		dampingThrottleFrame = (float)Math.pow( 1f - dampingThrottle, Physics.dt );

		// exports
		lateralForceFront = new Vector2();
		lateralForceRear = new Vector2();
	}

	public void setCarDescriptor( CarDescriptor carDesc )
	{
		carDesc.set( carDesc );
	}

	public void updateHeading( Body body )
	{
		VMath.fromAngle( heading, AMath.wrap2PI( body.getAngle() ) );
		VMath.perp( side, heading );
	}

	public void applyInput( CarInput input )
	{
		float maxForce = carDesc.carModel.max_force;
		boolean hasDir = false, hasSteer = false;

		if( input.updated )
		{
			// throttle
			if( AMath.fixup( input.throttle ) > 0 )
			{
				// acceleration
				if( input.throttle < maxForce )
					carDesc.throttle = input.throttle;
				else
					carDesc.throttle = maxForce;

				carDesc.brake = 0;
				hasDir = true;
			} else if( AMath.fixup( input.throttle ) < 0 )
			{
				// deceleration
				if( input.throttle > -maxForce )
					carDesc.throttle = input.throttle;
				else
					carDesc.throttle = -maxForce;

				carDesc.brake = 0;
				hasDir = true;
			}

			// steering
			if( AMath.fixup( input.steerAngle ) < 0 )
			{
				// left
				carDesc.steerangle = input.steerAngle;
				if( carDesc.steerangle < -AMath.PI_4 ) carDesc.steerangle = -AMath.PI_4;

				hasSteer = true;
			} else if( AMath.fixup( input.steerAngle ) > 0 )
			{
				// right
				carDesc.steerangle = input.steerAngle;
				if( carDesc.steerangle > AMath.PI_4 ) carDesc.steerangle = AMath.PI_4;

				hasSteer = true;
			}
		}

		if( !hasDir )
		{
			if( Math.abs( carDesc.velocity_wc.x ) > 0.5f || Math.abs( carDesc.velocity_wc.y ) > 0.5f )
			{
				if( !AMath.isZero( carDesc.throttle ) )
				{
					// carDesc.throttle *= 0.9f;
					carDesc.throttle *= dampingThrottleFrame;
				}

				carDesc.brake = 350f;
			} else
			{
				carDesc.velocity_wc.set( 0, 0 );
				carDesc.angularvelocity = 0;
				carDesc.brake = 0;
				carDesc.throttle = 0;
			}
		}

		if( !hasSteer )
		{
			carDesc.steerangle = 0;
			// float sa = AMath.fixup( carDesc.steerangle );
			// // gently auto steer to 0 degrees
			// if( sa > 0 || sa < 0)
			// carDesc.steerangle *= 0.9f;
			// else
			// carDesc.steerangle = 0.f;
		}

		carDesc.throttle = AMath.clamp( carDesc.throttle, -maxForce, maxForce );
	}

	public void step( Body body )
	{
		float sn = MathUtils.sin( AMath.normalRelativeAngle( -body.getAngle() ) );
		float cs = MathUtils.cos( AMath.normalRelativeAngle( -body.getAngle() ) );

		//
		// SAE convention: x is to the front of the car, y is to the right, z is
		// down
		//

		// car's velocity: Vlat and Vlong
		// transform velocity in world reference frame to velocity in car
		// reference frame
		velocity.x = cs * carDesc.velocity_wc.y + sn * carDesc.velocity_wc.x;
		velocity.y = -sn * carDesc.velocity_wc.y + cs * carDesc.velocity_wc.x;
		VMath.fixup( velocity );

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

		// velocity.x = fVLong_, velocity.y = fVLat_
		// fix singularity
		if( AMath.isZero( velocity.x ) )
		{
			rot_angle = 0;
			sideslip = 0;
		} else
		{
			// compute rotational angle
			rot_angle = MathUtils.atan2( yawspeed, velocity.x );

			// compute the side slip angle of the car (a.k.a. beta)
			sideslip = MathUtils.atan2( velocity.y, velocity.x );
		}

		// Calculate slip angles for front and rear wheels (a.k.a. alpha)
		float slipanglefront = sideslip + rot_angle - carDesc.steerangle;
		float slipanglerear = sideslip - rot_angle;

		// weight per axle = half car mass times 1G (=9.8m/s^2)
		// (precomputed during initialization)
		// weight = car->cartype->mass * 9.8f * 0.5f;

		// lateral force on front wheels = (Ca * slip angle) capped to friction
		// circle * load
		flatf.x = 0;
		flatf.y = carDesc.carModel.stiffness_front * slipanglefront;
		flatf.y = Math.min( carDesc.carModel.max_grip, flatf.y );
		flatf.y = Math.max( -carDesc.carModel.max_grip, flatf.y );
		lateralForceFront.set(flatf);
		flatf.y *= carDesc.carModel.weight;

		// lateral force on rear wheels
		flatr.x = 0;
		flatr.y = carDesc.carModel.stiffness_rear * slipanglerear;
		flatr.y = Math.min( carDesc.carModel.max_grip, flatr.y );
		flatr.y = Math.max( -carDesc.carModel.max_grip, flatr.y );
		lateralForceRear.set(flatr);
		flatr.y *= carDesc.carModel.weight;

		// float s = SGN(velocity.x);
		thisSign = AMath.lowpass( lastSign, AMath.sign( velocity.x ), 0.2f );
		lastSign = thisSign;

		ftraction.set( 100f * (carDesc.throttle - carDesc.brake * thisSign), 0 );

		// torque on body from lateral forces
		float torque = carDesc.carModel.b * flatf.y - carDesc.carModel.c * flatr.y;
		torque = AMath.fixup( torque );

		//
		// Forces and torque on body
		//

		// drag and rolling resistance
		resistance.x = -(carDesc.carModel.resistance * velocity.x + carDesc.carModel.drag * velocity.x * Math.abs( velocity.x ));
		resistance.y = -(carDesc.carModel.resistance * velocity.y + carDesc.carModel.drag * velocity.y * Math.abs( velocity.y ));

		// sum forces
		force.x = ftraction.x + MathUtils.sin( carDesc.steerangle ) * flatf.x + flatr.x + resistance.x;
		force.y = ftraction.y + MathUtils.cos( carDesc.steerangle ) * flatf.y + flatr.y + resistance.y;

		//
		// Acceleration
		//

		// Convert the force into acceleration
		// Newton F = m.a, therefore a = F/m
		acceleration.set( force.x * carDesc.carModel.invmass, force.y * carDesc.carModel.invmass );
		VMath.fixup( acceleration );

		//
		// Velocity and position
		//

		// transform acceleration from car reference frame to world reference
		// frame
		acceleration_wc.x = cs * acceleration.y + sn * acceleration.x;
		acceleration_wc.y = -sn * acceleration.y + cs * acceleration.x;
		VMath.fixup( acceleration_wc );

		// velocity is integrated acceleration
		carDesc.velocity_wc.x += Physics.dt * acceleration_wc.x;
		carDesc.velocity_wc.y += Physics.dt * acceleration_wc.y;
		VMath.fixup( carDesc.velocity_wc );

		// make sure vehicle doesn't exceed maximum velocity
		VMath.truncate( carDesc.velocity_wc, carDesc.carModel.max_speed );

		//
		// Angular acceleration, angular velocity and heading
		//

		// angular_acceleration = torque / carDesc.carModel.inertia;
		float angular_acceleration = torque * carDesc.carModel.invinertia;

		// integrate angular acceleration to get angular velocity
		carDesc.angularvelocity += Physics.dt * angular_acceleration;

		// integrate angular velocity to get angular orientation
		carDesc.angularOrientation = Physics.dt * carDesc.angularvelocity;

		updateHeading( body );
	}

	public void resetPhysics()
	{
		carDesc.velocity_wc.set( 0, 0 );
		carDesc.angularvelocity = 0;
		carDesc.brake = 0;
		carDesc.throttle = 0;
		carDesc.steerangle = 0;
		acceleration_wc.set( 0, 0 );
		velocity.set( 0, 0 );
		thisSign = lastSign = 1f;
	}
}
