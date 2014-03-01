
package com.bitfire.uracer.game;

import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.events.TaskManagerEvent.Order;
import com.bitfire.uracer.game.task.Task;

/** Tracks elapsed time both in absolute terms (wall clock time) or relative to the tick-based system.
 * 
 * @author bmanuel */
public final class Time extends Task {
	public static final class TimeValue {
		public long ticks;
		public float tickSeconds;
		public float absSeconds;
		public float lastAbsSeconds;

		public TimeValue () {
			reset();
		}

		public void reset () {
			ticks = 0;
			tickSeconds = 0;
			absSeconds = 0;
			lastAbsSeconds = 0;
		}
	}

	private static final float oneOnOneBillion = 1.0f / 1000000000.0f;
	private long nsStartTime;
	private long ticks;
	private boolean stopped;
	private long nsStopTime;
	private long lastStartTime;
	private TimeValue result = new TimeValue();

	/** Constructs a new Time object */
	public Time () {
		super(Order.PLUS_4);
		reset();
	}

	/** Returns whether or not this timer is stopped */
	public boolean isStopped () {
		return stopped;
	}

	/** Starts tracking */
	public void start () {
		reset();
		nsStartTime = TimeUtils.nanoTime();
		lastStartTime = nsStartTime;
		stopped = false;
	}

	/** Stops tracking */
	public void stop () {
		nsStopTime = TimeUtils.nanoTime();
		stopped = true;
	}

	/** Resumes/continues tracking, without resetting the accumulated state (should be called "continue" but can't */
	public void resume () {
		stopped = false;
		lastStartTime = TimeUtils.nanoTime();
	}

	/** Resets the internal state */
	public void reset () {
		stopped = true;

		nsStartTime = 0;
		nsStopTime = 0;
		lastStartTime = 0;

		result.reset();

		// ticks
		ticks = 0;
	}

	/** Counts this tick */
	@Override
	protected void onTick () {
		if (!stopped) {
			ticks++;

			long now = (stopped ? nsStopTime : TimeUtils.nanoTime());

			// plain number of ticks
			result.ticks = ticks;

			// number of ticks to seconds (in 1/dt increments)
			result.tickSeconds = ticks * Config.Physics.Dt;

			// last frame delta
			result.lastAbsSeconds = (now - lastStartTime) * oneOnOneBillion;
			if (!stopped) lastStartTime = TimeUtils.nanoTime();

			// absolute seconds
			result.absSeconds = (now - nsStartTime) * oneOnOneBillion;
		}
	};

	/** Returns the elapsed time expressed in a number of useful units */
	public TimeValue elapsed () {
		return result;
	}
}
