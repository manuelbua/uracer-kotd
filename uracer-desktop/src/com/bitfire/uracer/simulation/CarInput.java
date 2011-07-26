package com.bitfire.uracer.simulation;

/**
 * Represents the input forces for the simulator.
 *
 * @author manuel
 *
 */

public class CarInput
{
	// stylus/mouse
	float throttle, steerAngle;

	// data
	boolean updated;

	public CarInput()
	{
		reset();
	}

	public void reset()
	{
		updated = false;
		throttle = steerAngle = 0;
	};
}
