
package com.bitfire.uracer.game.logic.replaying;

import com.bitfire.uracer.utils.DigestUtils;
import com.bitfire.uracer.utils.ReplayUtils;

/** This represents replay information */
public final class ReplayInfo {
	protected String replayId;
	protected String userId;
	protected String trackId;
	protected int trackTimeTicks = 0; // express track length 1/dt steps
	protected int eventsCount;
	protected boolean completed = false;
	protected long created;

	public void copy (ReplayInfo replay) {
		if (replay != null) {
			replayId = replay.replayId;
			userId = replay.userId;
			trackId = replay.trackId;
			trackTimeTicks = replay.trackTimeTicks;
			eventsCount = replay.eventsCount;
			completed = replay.completed;
			created = replay.created;
		}
	}

	public void reset () {
		replayId = "";
		userId = "";
		trackId = "";
		trackTimeTicks = 0;
		eventsCount = 0;
		completed = false;
		created = 0;
	}

	public boolean isValidData () {
		return completed && created > 0 && userId.length() > 0 && trackId.length() > 0;
	}

	public boolean isValid () {
		return isValidData() && DigestUtils.isValidDigest(replayId);
	}

	public int getEventsCount () {
		return eventsCount;
	}

	public String getId () {
		return replayId;
	}

	public String getShortId () {
		return replayId.substring(0, 6);
	}

	public String getUserId () {
		return userId;
	}

	public String getTrackId () {
		return trackId;
	}

	public int getTicks () {
		return trackTimeTicks;
	}

	// return milliseconds, i.e. 14035
	public int getMilliseconds () {
		return ReplayUtils.ticksToMilliseconds(trackTimeTicks);
	}

	// return seconds (from computed milliseconds, minimize error), i.e. 14.035
	public float getSeconds () {
		return ReplayUtils.ticksToSeconds(trackTimeTicks);
	}

	public long getCreationTimestamp () {
		return created;
	}

	public String getSecondsStr () {
		return String.format("%.03f", getSeconds());
	}
}
