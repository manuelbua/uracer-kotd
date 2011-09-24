package com.bitfire.uracer;

import com.bitfire.uracer.simulations.car.Replay;

public class LapInfo
{
	// replays
	private Replay[] replays;
	private Replay best, worst, last;
	private long startTimeNs;

	public LapInfo()
	{
		startTimeNs = 0;

		// construct replay buffers
		replays = new Replay[ 2 ];
		replays[0] = new Replay();
		replays[1] = new Replay();

		best = worst = null;

		reset();
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
		return startTimeNs;
	}

	public Replay getReplay( int index )
	{
		return replays[index];
	}

	public boolean hasReplayData()
	{
		return (replays[0].isValid && replays[1].isValid);
	}

	public void update()
	{
		if( !hasReplayData() )
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

	public void setAsLast(int index)
	{
		last = replays[index];
	}

	public void setAsLast(Replay replay)
	{
		last = replay;
	}
}
