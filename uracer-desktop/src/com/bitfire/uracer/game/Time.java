package com.bitfire.uracer.game;

import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.task.TaskManagerEvent.Order;

/** Tracks elapsed time both in absolute terms (wall clock time) or
 * relative to the tick-based system.
 *
 * @author bmanuel */
public final class Time extends Task {
	public enum Reference {
		AbsoluteSeconds, TickSeconds, NumberOfTicks
	}

	private static final float oneOnOneBillion = 1.0f / 1000000000.0f;
	private long nsStartTime;
	private long ticks;
	private float ticksInSeconds;
	private boolean stopped;
	private long nsStopTime;

	/** Constructs a new Time object */
	public Time() {
		super( Order.PLUS_4 );
		reset();
	}

	/** Starts tracking */
	public void start() {
		reset();
	}

	/** Stops tracking */
	public void stop() {
		stopped = true;
		nsStopTime = TimeUtils.nanoTime();
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
		nsStartTime = TimeUtils.nanoTime();
		nsStopTime = 0;

		// ticks
		ticks = 0;
		ticksInSeconds = 0;
	}

	/** Counts this tick */
	@Override
	protected void onTick() {
		if( !stopped ) {
			ticks++;
			ticksInSeconds += Config.Physics.PhysicsDt;
		}
	};

	/** Returns the elapsed time expressed as the specified measuring unit */
	public float elapsed( Reference timeReference ) {
		long now = (stopped ? nsStopTime : TimeUtils.nanoTime());

		switch( timeReference ) {
		case TickSeconds:		// returns seconds
			return ticksInSeconds;
		case NumberOfTicks:		// returns the tick count so far
			return ticks;
		case AbsoluteSeconds:			// returns seconds
		default:
			return (now - nsStartTime) * oneOnOneBillion;
		}
	}
}
