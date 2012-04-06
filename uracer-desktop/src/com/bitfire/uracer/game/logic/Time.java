package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.Config;
import com.bitfire.uracer.events.TaskManagerEvent.Order;
import com.bitfire.uracer.task.Task;


/** Tracks elapsed time both in absolute terms (wall clock time) or
 * relative to the tick-based system.
 * This should guarantee some level of fair handicap for any user.
 *
 * @author bmanuel */
public final class Time extends Task {
	public enum Reference {
		Absolute, Ticks, NumberOfTicks
	}

	private static final float oneOnOneBillion = 1.0f / 1000000000.0f;
	private long nsStartTime;
	private long ticks;
	private float ticksInSeconds;
	private boolean stopped;
	private long nsStopTime;

	/** Constructs a new Time object */
	public Time() {
		super( Order.Order_Plus_4 );
		reset();
	}

	/** Starts tracking */
	public void start() {
		reset();
	}

	/** Stops tracking */
	public void stop() {
		stopped = true;
		nsStopTime = System.nanoTime();
	}

	/** Resumes/continues tracking, without resetting the accumulated state
	 * (should be called "continue" but can't */
	public void resume() {
		stopped = false;
	}

	/** Resets the internal state */
	public void reset() {
		stopped = false;

		// abs
		nsStartTime = System.nanoTime();
		nsStopTime = 0;

		// ticks
		ticks = 0;
		ticksInSeconds = 0;
	}

	/** Counts this tick */
	@Override
	public void onTick() {
		if( !stopped ) {
			ticks++;
			ticksInSeconds += Config.Physics.PhysicsDt;
		}
	};

	/** Returns the elapsed time expressed as the specified measuring unit */
	public float elapsed( Reference timeReference ) {
		long now = (stopped ? nsStopTime : System.nanoTime());

		switch( timeReference ) {
		default:
		case Absolute:			// returns seconds
			return (now - nsStartTime) * oneOnOneBillion;
		case Ticks:				// returns seconds
			return ticksInSeconds;
		case NumberOfTicks:		// returns the tick count so far
			return ticks;
		}
	}
}
