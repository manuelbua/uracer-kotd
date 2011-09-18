package com.bitfire.uracer.simulations.car;

import com.bitfire.uracer.Director;
import com.bitfire.uracer.entities.vehicles.Car;

public class Recorder
{
	private boolean isRecording;

	// replay data
	private Replay replay;
	private long trackNanoseconds;

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
		isRecording = false;
		replay = null;
	}

	public void reset()
	{
		isRecording = false;
		replay = null;
	}

	public void beginRecording( Car car, Replay replay )
	{
		isRecording = true;
		this.replay = replay;
		replay.clearForces();
		replay.setCarData( car );
		trackNanoseconds = System.nanoTime();
	}

	public void add( CarForces f )
	{
		if( !isRecording )
		{
			System.out.println("Cannot add event, recording not enabled!");
			return;
		}

		if( !replay.add( f ) )
		{
			System.out.println( "Replay memory limit reached (" + replay.MaxEvents + " events), restarting." );
		}
	}

	public void endRecording()
	{
		if( !isRecording )
		{
			System.out.println("Cannot end a recording that wasn't enabled!");
			return;
		}

		trackNanoseconds = System.nanoTime() - trackNanoseconds;
		float secs = (float)trackNanoseconds / 1000000000f;
		replay.setReplayData( Director.currentLevel.name, Director.gameplaySettings.difficulty, secs );

		System.out.println( "Recorded " + replay.getEventsCount() + " events" );

		isRecording = false;
		replay = null;
	}

	public boolean isRecording()
	{
		return isRecording;
	}
}