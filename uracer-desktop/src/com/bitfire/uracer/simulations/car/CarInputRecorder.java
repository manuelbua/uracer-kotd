package com.bitfire.uracer.simulations.car;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.entities.vehicles.Car;

public class CarInputRecorder
{
	private final int MaxInput = 10000;
	private ArrayList<CarInput> currBuffer;
	private ArrayList<CarInput> prevBuffer;

	private ArrayList<CarInput> playBuffer;
	private ArrayList<CarInput> recBuffer;

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

	private static CarInputRecorder instance;

	public static CarInputRecorder create()
	{
		CarInputRecorder.instance = new CarInputRecorder();
		return CarInputRecorder.instance;
	}

	public static CarInputRecorder instance()
	{
		return instance;
	}

	private CarInputRecorder()
	{
		currBuffer = new ArrayList<CarInput>( MaxInput );
		prevBuffer = new ArrayList<CarInput>( MaxInput );
		for( int i = 0; i < MaxInput; i++ )
		{
			currBuffer.add( new CarInput() );
			prevBuffer.add( new CarInput() );
		}

		recBuffer = currBuffer;
		playBuffer = prevBuffer;
		canReplay = false;
		playEvents = 0;
		carPlay = carRec = null;

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
		startDescriptor.set( car.carDesc );
		indexRec = 0;
		carRec = car;
	}

	public void add( CarInput i )
	{
		recBuffer.get( indexRec++ ).set( i );
		if( indexRec == MaxInput )
		{
			indexRec = 0;
			System.out.println( "Input recording limit reached (" + MaxInput + "), recording restarted." );
		}
	}

	public int endRec()
	{
		ArrayList<CarInput> tmpBuffer = playBuffer;

		// exchange buffers
		playBuffer = recBuffer;
		recBuffer = tmpBuffer;

		canReplay = true;
		playEvents = indexRec;
		System.out.println("Recorded " + playEvents + " events");
		return playEvents;
	}

	public void beginPlay( Car car )
	{
		if( !canReplay ) return;

		car.resetPhysics();
		car.pos( startPosition );
		car.orient( startOrientation );
		car.carDesc.set( startDescriptor );
		carPlay = car;
		indexPlay = 0;
	}

	public boolean get( CarInput input )
	{
		if( canReplay && indexPlay < playEvents )
		{
//			System.out.println("Replaying event #" + indexPlay + "/" + (playEvents-1));
			input.set( playBuffer.get( indexPlay++ ) );
			return true;
		}

		return false;
	}

	public boolean hasFinishedPlaying()
	{
		return (indexPlay == playEvents);
	}

	public boolean hasReplay()
	{
		return playEvents > 0 && canReplay;
	}
}