package com.bitfire.uracer.simulations.car;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.entities.vehicles.Car;

public class Recorder
{
	private final int MaxEvents = 10000;
	private ArrayList<CarForces> currBuffer;
	private ArrayList<CarForces> prevBuffer;

	private ArrayList<CarForces> playBuffer;
	private ArrayList<CarForces> recBuffer;

	// initial state
	private Vector2 startPosition;
	private float startOrientation;
	private CarDescriptor startDescriptor;
	private Car carRec, carPlay;

	// play/rec indexes
	private int indexPlay;
	private int indexRec;
	private int playEvents;
	private boolean canReplay;
	private boolean isRecording;

	private static Recorder instance;

	public static Recorder create()
	{
		Recorder.instance = new Recorder();
		return Recorder.instance;
	}

	public static Recorder instance()
	{
		return instance;
	}

	private Recorder()
	{
		currBuffer = new ArrayList<CarForces>( MaxEvents );
		prevBuffer = new ArrayList<CarForces>( MaxEvents );
		for( int i = 0; i < MaxEvents; i++ )
		{
			currBuffer.add( new CarForces() );
			prevBuffer.add( new CarForces() );
		}

		recBuffer = currBuffer;
		playBuffer = prevBuffer;
		canReplay = false;
		playEvents = 0;
		carPlay = carRec = null;
		isRecording = false;

		startPosition = new Vector2();
		startDescriptor = new CarDescriptor();
	}

	public void clear()
	{
		indexPlay = 0;
		indexRec = 0;
		playEvents = 0;
		canReplay = false;
	}

	public void beginRec( Car car )
	{
		startPosition.set( car.pos() );
		startOrientation = car.orient();
		startDescriptor.set( car.getCarDescriptor() );
		indexRec = 0;
		carRec = car;
		isRecording = true;
	}

	public void add( CarForces f )
	{
		recBuffer.get( indexRec++ ).set( f );
		if( indexRec == MaxEvents )
		{
			indexRec = 0;
			System.out.println( "Recording limit reached (" + MaxEvents + " events), recording restarted." );
		}
	}

	public int endRec()
	{
		ArrayList<CarForces> tmpBuffer = playBuffer;

		// exchange buffers
		playBuffer = recBuffer;
		recBuffer = tmpBuffer;

		canReplay = true;
		playEvents = indexRec;
		isRecording = false;

		System.out.println("Recorded " + playEvents + " events");
		return playEvents;
	}

	public void beginPlay( Car car )
	{
		if( !canReplay ) return;

		car.resetPhysics();
		car.pos( startPosition );
		car.orient( startOrientation );
		car.getCarDescriptor().set( startDescriptor );
		carPlay = car;
		indexPlay = 0;
	}

	public boolean get( CarForces forces )
	{
		if( canReplay && indexPlay < playEvents )
		{
//			System.out.println("Replaying event #" + indexPlay + "/" + (playEvents-1));
			forces.set( playBuffer.get( indexPlay++ ) );
			return true;
		}

		return false;
	}

	/**
	 * Utilities
	 */

	public boolean hasFinishedPlaying()
	{
		return (indexPlay == playEvents);
	}

	public boolean hasReplay()
	{
		return playEvents > 0 && canReplay;
	}

	public boolean isRecording()
	{
		return isRecording;
	}
}