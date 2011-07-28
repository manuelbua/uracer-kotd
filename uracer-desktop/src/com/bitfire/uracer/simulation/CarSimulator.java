package com.bitfire.uracer.simulation;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.VMath;

public class CarSimulator
{
	public static final float CS_2PI = 6.28318530717958647692f;
	public static final float CS_PI = 3.14159265358979323846f;
	public static final float CS_PI_2 = 1.57079632679489661923f;
	public static final float CS_PI_4 = 0.785398163397448309616f;
	public static final float CS_PI_8 = 0.392699081698724154807f;

	public CarDescriptor carDesc;
	public CarInput carInput = new CarInput();
	public Vector2 lastCarScreenPos = new Vector2(), lastTouchPos = new Vector2(), velocity = new Vector2(),
			acceleration_wc = new Vector2(), heading = new Vector2(), side = new Vector2(), flatf = new Vector2(),
			flatr = new Vector2(), ftraction = new Vector2(), resistance = new Vector2(), force = new Vector2(), acceleration = new Vector2();
	public float thisSign, lastSign, lastTouchAngle;


	public CarSimulator(CarDescriptor carDesc)
	{
		this.carDesc = carDesc;
		thisSign = lastSign = 1f;
		lastTouchAngle = 0;
	}

	public void setCarDescriptor( CarDescriptor carDesc )
	{
		carDesc.set( carDesc );
	}

	public void updateHeading()
	{
		VMath.fromAngle( heading, carDesc.wrapped_angle );
		VMath.perp( side, heading );
	}

	public void acquireInput( Body body )
	{
		Vector2 carScreenPos = Director.screenPosFor( body );
		Vector2 touchPos = Input.getXY();

		carInput.updated = Input.isTouching();

		lastCarScreenPos = carScreenPos;
		lastTouchPos = touchPos;

		if( carInput.updated )
		{
			float angle = 0;

			// avoid singularity
			if( (int)-carScreenPos.y + (int)touchPos.y == 0 )
			{
				angle = lastTouchAngle;
			} else
			{
				angle = MathUtils.atan2( -carScreenPos.x + touchPos.x, -carScreenPos.y + touchPos.y );
				lastTouchAngle = angle;
			}

			// TODO: optimization
//			float pa = angle;

			angle -= CS_PI;
			angle += carDesc.wrapped_angle;	// to local
			if( angle < 0 )
				angle += CS_2PI;

			angle = -(angle - CS_2PI);
			if( angle > CS_PI )
				angle = angle - CS_2PI;

//			System.out.println("prev-a="+pa + ", a=" + angle);

			carInput.steerAngle = angle;

			// compute throttle
			carInput.throttle = touchPos.dst( carScreenPos );

			// damp the throttle
			if( !AMath.isZero( carInput.throttle ) )
			{
				carInput.throttle *= 1.5f;
			}
		}
	}

	public void applyInput()
	{
		float maxForce = carDesc.carModel.max_force;
		boolean hasDir = false, hasSteer = false;

		if( carInput.updated )
		{
			// throttle
			if( carInput.throttle > 0 )
			{
				// acceleration
				if( carInput.throttle < maxForce )
					carDesc.throttle = carInput.throttle;
				else
					carDesc.throttle = maxForce;

				carDesc.brake = 0;
				hasDir = true;
			} else if( carInput.throttle < 0 )
			{
				// deceleration
				if( carInput.throttle > -maxForce )
					carDesc.throttle = carInput.throttle;
				else
					carDesc.throttle = -maxForce;

				carDesc.brake = 0;
				hasDir = true;
			}

			// steering
			if( carInput.steerAngle < 0 )
			{
				// left
				carDesc.steerangle = carInput.steerAngle;
				if( carDesc.steerangle < -CS_PI_4 )
					carDesc.steerangle = -CS_PI_4;

				hasSteer = true;
			} else if( carInput.steerAngle > 0 )
			{
				// right
				carDesc.steerangle = carInput.steerAngle;
				if( carDesc.steerangle > CS_PI_4 )
					carDesc.steerangle = CS_PI_4;

				hasSteer = true;
			}
		}

		if( !hasDir )
		{
			if( Math.abs( carDesc.velocity_wc.x ) > 0.5f || Math.abs( carDesc.velocity_wc.y ) > 0.5f )
			{
				if( !AMath.isZero( carDesc.throttle ) )
				{
					carDesc.throttle *= 0.9f;
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
			// gently auto steer to 0 degrees
			if( Math.abs( carDesc.steerangle ) > 0.0001f )
				carDesc.steerangle *= 0.9f;
			else
				carDesc.steerangle = 0.f;
		}

		carDesc.throttle = AMath.clamp( carDesc.throttle, -maxForce, maxForce );
	}

	public void step()
	{
		float sn = MathUtils.sin( carDesc.wrapped_angle );
		float cs = MathUtils.cos( carDesc.wrapped_angle );

		//
		// SAE convention: x is to the front of the car, y is to the right, z is down
		//

		// car's velocity: Vlat and Vlong
		// transform velocity in world reference frame to velocity in car reference frame
		velocity.x =  cs * carDesc.velocity_wc.y + sn * carDesc.velocity_wc.x;
		velocity.y = -sn * carDesc.velocity_wc.y + cs * carDesc.velocity_wc.x;
		VMath.fixup( velocity );


		//
		// Lateral force on wheels
		//

		// Resulting velocity of the wheels as result of the yaw rate of the car body
		// v = yawrate * r where r is distance of wheel to CG (approx. half wheel base)
		// yawrate (ang.velocity) must be in rad/s
		//
		float yawspeed = carDesc.carModel.wheelbase * 0.5f * carDesc.angularvelocity;
		float sideslip = 0, rot_angle = 0;

		// velocity.x = fVLong_, velocity.y = fVLat_
		// fix singularity
		if( AMath.isZero( velocity.x) )
		{
			rot_angle = 0;
			sideslip = 0;
		}
		else
		{
			// compute rotational angle
			rot_angle = MathUtils.atan2( yawspeed, velocity.x );

			// compute the side slip angle of the car (a.k.a. beta)
			sideslip = MathUtils.atan2( velocity.y, velocity.x );
		}

		// Calculate slip angles for front and rear wheels (a.k.a. alpha)
		float slipanglefront = sideslip + rot_angle - carDesc.steerangle;
		float slipanglerear  = sideslip - rot_angle;

		// weight per axle = half car mass times 1G (=9.8m/s^2)
		// (precomputed during initialization)
		//	weight = car->cartype->mass * 9.8f * 0.5f;

		// lateral force on front wheels = (Ca * slip angle) capped to friction circle * load
		flatf.x = 0;
		flatf.y = carDesc.carModel.stiffness_front * slipanglefront;
		flatf.y = Math.min(carDesc.carModel.max_grip, flatf.y);
		flatf.y = Math.max(-carDesc.carModel.max_grip, flatf.y);
		flatf.y *= carDesc.carModel.weight;

		// lateral force on rear wheels
		flatr.x = 0;
		flatr.y = carDesc.carModel.stiffness_rear * slipanglerear;
		flatr.y = Math.min(carDesc.carModel.max_grip, flatr.y);
		flatr.y = Math.max(-carDesc.carModel.max_grip, flatr.y);
		flatr.y *= carDesc.carModel.weight;

//		float s = SGN(velocity.x);
		thisSign = AMath.lowpass( lastSign, AMath.sign(velocity.x), 0.2f );
		lastSign = thisSign;

		ftraction.set( 100f * (carDesc.throttle - carDesc.brake * thisSign /*SGN(velocity.x)*/ ), 0 );

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

		// transform acceleration from car reference frame to world reference frame
		acceleration_wc.x = cs * acceleration.y + sn * acceleration.x;
		acceleration_wc.y = -sn * acceleration.y + cs * acceleration.x;
		VMath.fixup( acceleration_wc );

		// velocity is integrated acceleration
		carDesc.velocity_wc.x += Physics.dt * acceleration_wc.x;
		carDesc.velocity_wc.y += Physics.dt * acceleration_wc.y;
		VMath.fixup( carDesc.velocity_wc );

		// make sure vehicle doesn't exceed maximum velocity
		VMath.truncate( carDesc.velocity_wc, carDesc.carModel.max_speed );

		// position is integrated velocity
		//carDesc.position_wc.x +=  frametime * carDesc.velocity_wc.x;
		//carDesc.position_wc.y += -frametime * carDesc.velocity_wc.y;


		//
		// Angular acceleration, angular velocity and heading
		//

		//angular_acceleration = torque / carDesc.carModel.inertia;
		float angular_acceleration = torque * carDesc.carModel.invinertia;

		// integrate angular acceleration to get angular velocity
		carDesc.angularvelocity += Physics.dt * angular_acceleration;

		// integrate angular velocity to get angular orientation
		carDesc.angularOrientation = Physics.dt * carDesc.angularvelocity;

		// this angle is safe for subframe interpolation
		carDesc.angle += carDesc.angularOrientation;

		// this angle is *NOT* safe for subframe interpolation (wrapped, one border could get lerped)!
		carDesc.wrapped_angle += carDesc.angularOrientation;

		if( carDesc.wrapped_angle > CS_2PI )
			carDesc.wrapped_angle -= CS_2PI;

		if( carDesc.wrapped_angle < 0 )
			carDesc.wrapped_angle += CS_2PI;

		updateHeading();
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
	}
}
