package com.bitfire.uracer.game;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.carsimulation.CarDescriptor;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarFactory.CarType;
import com.bitfire.uracer.utils.UUid;

/** Represents replay data to be feed to a GhostCar, the replay player.
 *
 * @author manuel */

public class Replay {
	public static final int MaxEvents = 5000;
	private int eventsCount;

	// car data
	public CarType carType;
	public Vector2 carPosition;
	public float carOrientation;
	public CarDescriptor carDescriptor;

	// replay data
	public String trackName = "no-track";
	public GameDifficulty difficultyLevel = GameDifficulty.Easy;
	public float trackTimeSeconds = 0;
	// public long trackStartTimeNs = 0;
	public CarForces[] forces = null;
	public boolean isValid = false;
	public final long id;

	// time track
	private Time time = new Time();

	public Replay() {
		eventsCount = 0;
		forces = new CarForces[ MaxEvents ];
		for( int i = 0; i < MaxEvents; i++ ) {
			forces[i] = new CarForces();
		}

		id = UUid.get();
	}

	public void dispose() {
		reset();
	}

	public void begin( String trackName, GameDifficulty difficulty, Car car ) {
		reset();
		carType = car.getCarType();
		carPosition = new Vector2( car.pos() );
		carOrientation = car.orient();
		carDescriptor = car.getCarDescriptor().newCopy();
		this.trackName = trackName;
		difficultyLevel = difficulty;
		time.start();
	}

	public void end() {
		time.stop();
		trackTimeSeconds = time.elapsed( Time.Reference.Ticks );
		isValid = true;
	}

	public void reset() {
		eventsCount = 0;
		isValid = false;
	}

	// recording
	public int getEventsCount() {
		return eventsCount;
	}

	public boolean add( CarForces f ) {
		forces[eventsCount++].set( f );
		if( eventsCount == MaxEvents ) {
			eventsCount = 0;
			return false;
		}

		return true;
	}
}
