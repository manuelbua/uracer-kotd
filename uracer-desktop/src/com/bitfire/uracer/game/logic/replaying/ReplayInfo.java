
package com.bitfire.uracer.game.logic.replaying;

import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.DigestUtils;

/** This represents replay information */
public final class ReplayInfo {
	protected String replayId;
	protected String userId;
	protected String trackId;
	protected float trackTimeSeconds = 0;
	protected int eventsCount;
	protected boolean completed = false;
	protected long created;

	public void copy (ReplayInfo replay) {
		if (replay != null) {
			replayId = replay.replayId;
			userId = replay.userId;
			trackId = replay.trackId;
			trackTimeSeconds = replay.trackTimeSeconds;
			eventsCount = replay.eventsCount;
			completed = replay.completed;
			created = replay.created;
		}
	}

	public void reset () {
		replayId = "";
		userId = "";
		trackId = "";
		trackTimeSeconds = 0;
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

	public float getTrackTime () {
		return trackTimeSeconds;
	}

	public int getTrackTimeInt () {
		return (int)(trackTimeSeconds * AMath.ONE_ON_CMP_EPSILON);
	}

	public long getCreationTimestamp () {
		return created;
	}
}
