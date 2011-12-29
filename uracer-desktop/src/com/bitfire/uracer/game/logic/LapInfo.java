package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.Config;
import com.bitfire.uracer.carsimulation.Replay;

public class LapInfo
{
	// replays
	private Replay[] replays;
	private Replay best, worst;
	private long startTimeNs;
	private float lastTrackTimeSecs;
	private boolean hasLastTrackTimeSecs;

	private static LapInfo instance = null;
	private static GameLogic logic = null;

	private LapInfo()
	{
		startTimeNs = 0;
		lastTrackTimeSecs = 0;
		hasLastTrackTimeSecs = false;

		// construct replay buffers
		replays = new Replay[ 2 ];
		replays[0] = new Replay();
		replays[1] = new Replay();

		best = worst = null;

		reset();
		update();
	}

	public static void init( GameLogic logic )
	{
		instance = new LapInfo();
		LapInfo.logic = logic;
	}

	public static LapInfo get()
	{
		return instance;
	}

	public void reset()
	{
		hasLastTrackTimeSecs = false;
		best = worst = null;
		replays[0].clearForces();
		replays[1].clearForces();
		startTimeNs = System.nanoTime();
	}

	public long restart()
	{
		startTimeNs = System.nanoTime();
		if(!replays[0].isValid) replays[0].clearForces();
		if(!replays[1].isValid) replays[1].clearForces();
		return startTimeNs;
	}

	public float getElapsedSeconds()
	{
		return ((float)(System.nanoTime() - startTimeNs) / 1000000000f) * Config.Physics.PhysicsTimeMultiplier;
	}

	public long getStartNanotime()
	{
		return startTimeNs;
	}

	public Replay getReplay( int index )
	{
		return replays[index];
	}

	public boolean hasAllReplayData()
	{
		return (replays[0].isValid && replays[1].isValid);
	}

	public boolean hasAnyReplayData()
	{
		return (replays[0].isValid || replays[1].isValid);
	}

	public void update()
	{
		if( !hasAllReplayData() )
		{
			return;
		}

		best = replays[1];
		worst = replays[0];

		if( replays[0].trackTimeSeconds < replays[1].trackTimeSeconds )
		{
			best = replays[0];
			worst = replays[1];
		}
	}

	public Replay getNextBuffer()
	{
		update();
		if( !replays[0].isValid ) { return replays[0]; }
		if( !replays[1].isValid ) { return replays[1]; }

		// if both are valid
		return getWorstReplay();
	}

	public Replay getBestReplay()
	{
		return best;
	}

	public Replay getWorstReplay()
	{
		return worst;
	}

	public void setLastTrackTimeSeconds( float value )
	{
		lastTrackTimeSecs = value;
		hasLastTrackTimeSecs = true;
	}

	public float getLastTrackTimeSeconds()
	{
		return lastTrackTimeSecs;
	}

	public boolean hasLastTrackTimeSeconds()
	{
		return hasLastTrackTimeSecs;
	}

	public Replay getAnyReplay()
	{
		if(replays[0].isValid) return replays[0];
		if(replays[1].isValid) return replays[1];
		return null;
	}
}
