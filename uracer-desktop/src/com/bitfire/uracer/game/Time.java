
package com.bitfire.uracer.game;

import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.task.Task;
import com.bitfire.uracer.game.task.TaskManagerEvent.Order;

/** Tracks elapsed time both in absolute terms (wall clock time) or relative to the tick-based system.
 * 
 * @author bmanuel */
public final class Time extends Task {
	public enum Reference {
		AbsoluteSeconds, LastAbsoluteSeconds, TickSeconds, NumberOfTicks
	}

	private static final float oneOnOneBillion = 1.0f / 1000000000.0f;
	private long nsStartTime;
	private long ticks;
	private float ticksInSeconds;
	private boolean stopped;
	private long nsStopTime;
	private long lastStartTime;

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
	}

	/** Resets the internal state */
	public void reset () {
		stopped = true;

		// abs
		nsStartTime = TimeUtils.nanoTime();
		nsStopTime = 0;
		lastStartTime = nsStartTime;

		// ticks
		ticks = 0;
		ticksInSeconds = 0;
	}

	/** Counts this tick */
	@Override
	protected void onTick () {
		if (!stopped) {
			ticks++;
			ticksInSeconds += Config.Physics.Dt;
		}
	};

	/** Returns the elapsed time expressed as the specified measuring unit */
	public float elapsed (Reference timeReference) {
		long now = (stopped ? nsStopTime : TimeUtils.nanoTime());

		switch (timeReference) {
		case TickSeconds: // returns seconds
			return ticksInSeconds;
		case NumberOfTicks:
			return ticks;
		case LastAbsoluteSeconds:
			float r = (now - lastStartTime) * oneOnOneBillion;
			if (!stopped) {
				lastStartTime = TimeUtils.nanoTime();
			}
			return r;
		case AbsoluteSeconds: // returns seconds
		default:
			return (now - nsStartTime) * oneOnOneBillion;
		}
	}
}
