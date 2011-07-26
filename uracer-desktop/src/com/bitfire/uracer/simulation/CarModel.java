package com.bitfire.uracer.simulation;

/**
 * Represents the physical car model on which we rely to compute
 * the forces exerted by the simulation.
 *
 * @author manuel
 *
 */
public class CarModel
{
	public float wheelbase; // wheelbase in m
	public float b; // in m, distance from CG to front axle
	public float c; // in m, idem to rear axle
	public float h; // in m, height of CM from ground
	public float mass; // in kg
	public float inertia; // in kg.m
	public float length, width;
	public float wheellength, wheelwidth;

	public float invmass;
	public float invinertia;
	public float density;
	public float friction;
	public float restitution;

	// physical behavior
	public float drag, // factor for air resistance (drag)
			resistance, // factor for rolling resistance
			stiffness_front, // front cornering stiffness
			stiffness_rear, // rear cornering stiffness
			max_grip, // maximum (normalised) friction force, =diameter of friction circle
			max_speed, max_force;

	// physically computed
	public float weight;

	public CarModel()
	{
		toDefault();
	}

	// default car model
	public void toDefault()
	{
		// physical model
		h = 1.f; // m
		mass = 1500.f; // kg
		invmass = 1.f / mass;
		inertia = 1500.f; // kg.m
		invinertia = 1.f / inertia;

		b = 1.2f; // m
		c = 1.2f; // m
		width = 2.5f; // m
		length = 4.0f; // m must be > wheelbase

		wheellength = 0.7f;
		wheelwidth = 0.3f;

		// weight per axle = half car mass times 1G (=9.8m/s^2)
		weight = mass * 9.8f * 0.5f;
		wheelbase = b + c;

		// physical behavior
		drag = 10.f; // factor for air resistance (drag)
		resistance = 5.f; // factor for rolling resistance
		stiffness_rear = -4.2f; // front cornering stiffness
		stiffness_front = -4.4f; // rear cornering stiffness
		max_grip = 8.f; // maximum (normalised) friction force, =diameter of
						// friction circle

		max_speed = 35.f;
		max_force = 300.f;

		density = 1.f;
		friction = .4f;
		restitution = 0.f;
	}

	public void toModel1()
	{
		// physical model
		h = 1.f; // m
		mass = 1500.f; // kg
		invmass = 1.f / mass;
		inertia = 1500.f; // kg.m
		invinertia = 1.f / inertia;

		b = 1.f; // m
		c = 1.f; // m
		width = 2.1f; // m
		length = 3.5f; // m must be > wheelbase

		wheellength = 0.7f;
		wheelwidth = 0.3f;

		// weight per axle = half car mass times 1G (=9.8m/s^2)
		weight = mass * 9.8f * 0.5f;
		wheelbase = b + c;

		// physical behavior
		drag = 10.f; // factor for air resistance (drag)
		resistance = 30.f; // factor for rolling resistance
		stiffness_rear = -5.2f; // front cornering stiffness
		stiffness_front = -5.0f; // rear cornering stiffness
		max_grip = 6.f; // maximum (normalised) friction force, =diameter of
						// friction circle

		max_speed = 35.f;
		max_force = 300.f;

		density = 1.f;
		friction = .4f;
		restitution = 0.f;
	}
}
