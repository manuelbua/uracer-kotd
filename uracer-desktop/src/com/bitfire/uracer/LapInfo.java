package com.bitfire.uracer;

import com.bitfire.uracer.simulations.car.Replay;

public class LapInfo
{
	// replays
	private Replay[] replays;
	private Replay best, worst, last, prev;
	private long startTimeNs;

	public LapInfo()
	{
		startTimeNs = 0;

		// construct replay buffers
		replays = new Replay[ 2 ];
		replays[0] = new Replay();
		replays[1] = new Replay();

		best = worst = last = prev = null;

		reset();
		update();
	}

	public void reset()
	{
		replays[0].clearForces();
		replays[1].clearForces();
		restart();
	}

	public float getElapsedSeconds()
	{
		return (float)(System.nanoTime() - startTimeNs) / 1000000000f;
	}

	public long getStartNanotime()
	{
		return startTimeNs;
	}

	public long restart()
	{
		startTimeNs = System.nanoTime();
		if(!replays[0].isValid) replays[0].clearForces();
		if(!replays[1].isValid) replays[1].clearForces();
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

		last = worst;
	}

	public Replay getNextBuffer()
	{
		prev = last;
		update();
		if( !replays[0].isValid ) { last = replays[0]; return last; }
		if( !replays[1].isValid ) { last = replays[1]; return last; }

		// if both are valid
		last = getWorstReplay();
		return last;
	}

	public Replay getPrevBuffer()
	{
		return prev;
	}

	public Replay getBestReplay()
	{
		return best;
	}

	public Replay getWorstReplay()
	{
		return worst;
	}

	// not necessarily best or worst, just the last recorded
	public Replay getLastReplay()
	{
		return last;
	}

	public Replay getFirstAvailable()
	{
		if(replays[0].isValid) return replays[0];
		if(replays[1].isValid) return replays[1];
		return null;
	}
}
