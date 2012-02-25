package com.bitfire.uracer.carsimulation;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.factories.CarFactory.CarType;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.utils.UUid;

/**
 * Represents replay data to be feed to a GhostCar, the replay player.
 *
 * @author manuel
 *
 */

public class Replay
{
	public final int MaxEvents = 5000;
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
	public long trackStartTimeNs = 0;
	public CarForces[] forces = null;
	public boolean isValid = false;
	public final long id;

	public Replay()
	{
		eventsCount = 0;
		forces = new CarForces[MaxEvents];
		for( int i = 0; i < MaxEvents; i++ )
		{
			forces[i] = new CarForces();
		}

		id = UUid.get();
	}

	public void dispose()
	{
		clearForces();
	}

	public void clearForces()
	{
		eventsCount = 0;
		isValid = false;
	}

	public void setCarData( Car car )
	{
		carType = car.getCarType();
		carPosition = new Vector2( car.pos() );
		carOrientation = car.orient();
		carDescriptor = car.getCarDescriptor().clone();
	}

	public void setReplayData( String trackName, GameDifficulty difficulty, float timeSeconds )
	{
		this.trackName = trackName;
		this.difficultyLevel = difficulty;
		this.trackTimeSeconds = timeSeconds;
		this.isValid = true;
	}

	// recording
	public int getEventsCount()
	{
		return eventsCount;
	}

	public boolean add( CarForces f )
	{
		forces[eventsCount++].set( f );
		if( eventsCount == MaxEvents )
		{
			eventsCount = 0;
			return false;
		}

		return true;
	}
}
