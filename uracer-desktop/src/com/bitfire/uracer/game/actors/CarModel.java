package com.bitfire.uracer.game.actors;

/** Represents the physical car model on which we rely to compute
 * the forces exerted by the simulation.
 *
 * @author manuel */
public final class CarModel {
	public enum Type {
		ModelDefault( "model-default" ), Model1( "model-1" ), Model2( "model-2" ), Model3( "model-3" ), ModelBlack( "model-black" );

		public final String modelName;

		private Type( String modelName ) {
			this.modelName = modelName;
		}
	}

	public Type type;

	public float wheelbase; // wheelbase in m
	public float b; // in m, distance from CG to front axle
	public float c; // in m, idem to rear axle
	// public float h; // in m, height of CM from ground
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

	public CarModel() {
		toDefault();
	}

	public CarModel( CarModel other ) {
		set( other );
	}

	public static CarModel fromType( Type type ) {
		switch( type ) {
		case Model1:
			return (new CarModel()).toModel1();
		case Model2:
			return (new CarModel()).toModel2();
		case Model3:
			return (new CarModel()).toModel3();
		case ModelBlack:
			return (new CarModel()).toBlackCar();
		default:
		case ModelDefault:
			return (new CarModel()).toDefault();
		}
	}

	public CarModel newCopy() {
		return new CarModel( this );
	}

	public void set( CarModel other ) {
		this.type = other.type;
		this.wheelbase = other.wheelbase;
		this.b = other.b;
		this.c = other.c;
		this.mass = other.mass;
		this.inertia = other.inertia;
		this.length = other.length;
		this.width = other.width;
		this.wheellength = other.wheellength;
		this.wheelwidth = other.wheelwidth;
		this.invmass = other.invmass;
		this.invinertia = other.invinertia;
		this.density = other.density;
		this.friction = other.friction;
		this.restitution = other.restitution;
		this.drag = other.drag;
		this.resistance = other.resistance;
		this.stiffness_front = other.stiffness_front;
		this.stiffness_rear = other.stiffness_rear;
		this.max_grip = other.max_grip;
		this.max_speed = other.max_speed;
		this.max_force = other.max_force;
		this.weight = other.weight;
	}

	public void toModelType( CarModel.Type type ) {
		switch( type ) {
		case Model1:
			this.toModel1();
			break;
		case Model2:
			this.toModel2();
			break;
		case Model3:
			this.toModel3();
			break;
		case ModelBlack:
			this.toBlackCar();
			break;
		default:
		case ModelDefault:
			this.toDefault();
			break;
		}
	}

	// default car model
	public CarModel toDefault() {
		type = Type.ModelDefault;

		// physical model
		// h = 1f; // m
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

		density = 1f;
		friction = 4f;
		restitution = 0.25f;

		return this;
	}

	public CarModel toModel1() {
		toDefault();
		type = Type.Model1;

		// physical model
		b = 1.f; // m
		c = 1.f; // m
		width = 2.3f; // m
		length = 3.5f; // m (must be > wheelbase)

		// physical behavior
		drag = 12.f; // factor for air resistance (drag)
		resistance = 30.f; // factor for rolling resistance
		stiffness_rear = -6.7f; // front cornering stiffness
		stiffness_front = -6.7f; // rear cornering stiffness
		max_grip = 6f; // maximum (normalised) friction force, =diameter of friction circle

		max_speed = 35.f;
		max_force = 300.f;

		density = 1f;
		friction = 4f;
		restitution = 0.25f;

		return this;
	}

	public CarModel toModel2() {
		toDefault();
		type = Type.Model2;

		// physical model
		// h = .85f; // m
		b = 1.f; // m
		c = 1.f; // m
		width = 2.5f; // m
		length = 3.5f; // m (must be > wheelbase)

		// physical behavior
		drag = 15.f; // factor for air resistance (drag)
		resistance = 30.f; // factor for rolling resistance
		stiffness_rear = -4.4f; // rear cornering stiffness
		stiffness_front = -4.2f; // front cornering stiffness
		max_grip = 6f; // maximum (normalised) friction force, =diameter of friction circle

		max_speed = 35.f;
		max_force = 300.f;

		// mostly for collision response
		density = 1f;
		friction = 4f;
		restitution = 0.25f;

		return this;
	}

	public CarModel toModel3() {
		toModel2();
		type = Type.Model3;

		// physical model
		// h = .85f; // m
		b = 1.f; // m
		c = 1.f; // m

		width = 2.25f; // m
		length = 3.5f; // m (must be > wheelbase)

		// physical behavior
		drag = 20.f; // factor for air resistance (drag)
		resistance = 30.f; // factor for rolling resistance

		stiffness_rear = -4.6f; // rear cornering stiffness
		stiffness_front = -4.4f; // front cornering stiffness

		// TODO think, cars for drift (max_grip = 3/4/5f), other cars (max_grip = 6/7/8f)
		max_grip = 7f; // maximum (normalised) friction force, =diameter of friction circle

		max_speed = 40.f;
		max_force = 400.f;

		// mostly for collision response
		density = 1f;
		friction = 8.02f;
		restitution = 0f;

		return this;
	}

	public CarModel toBlackCar() {
		toModel1();
		type = Type.ModelBlack;

		width = 3.1f; // m
		length = 4.5f; // m (must be > wheelbase)
		max_grip = 10f;
		stiffness_rear = -4.7f; // front cornering stiffness
		stiffness_front = -4.7f; // rear cornering stiffness

		return this;
	}
}
