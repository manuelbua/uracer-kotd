
package com.bitfire.uracer.game.logic.replaying;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.DigestUtils;
import com.bitfire.uracer.utils.URacerRuntimeException;

/** Represents replay data to be feed to a GhostCar, the replay player.
 * 
 * @author manuel */

public final class Replay implements Disposable, Comparable<Replay> {
	public static final int MaxEvents = 5000;

	// car data
	private Vector2 carPositionMt = new Vector2();
	private float carOrientationRads;

	// replay data
	private String replayId;
	private String userId;
	private String trackId;
	private float trackTimeSeconds = 0;
	private CarForces[] forces = null;
	private int eventsCount;
	private boolean completed = false;
	private long created;

	public Replay () {
		forces = new CarForces[MaxEvents];
		for (int i = 0; i < MaxEvents; i++) {
			forces[i] = new CarForces();
		}
		reset();
	}

	@Override
	public int compareTo (Replay o) {
		// compare up to the 3rd decimal
		int thisSecs = (int)(getTrackTime() * AMath.ONE_ON_CMP_EPSILON);
		int otherSecs = (int)(o.getTrackTime() * AMath.ONE_ON_CMP_EPSILON);

		// if different time, then compare
		if (thisSecs != otherSecs) {
			return thisSecs - otherSecs;
		} else {
			// equal time, draw
			// the oldest wins
			if (created < o.created) return -1;
			if (created > o.created) return 1;
			return 0;
		}
	}

	@Override
	public void dispose () {
		reset();

		for (int i = 0; i < MaxEvents; i++) {
			forces[i] = null;
		}
	}

	public void setId (String id) {
		replayId = id;
	}

	public void reset () {
		replayId = "";
		userId = "";
		trackId = "";
		trackTimeSeconds = 0;
		eventsCount = 0;
		carPositionMt.set(0, 0);
		carOrientationRads = 0;
		completed = false;
		created = 0;

		for (int i = 0; i < MaxEvents; i++) {
			forces[i].reset();
		}
	}

	public void copyData (Replay replay) {
		replayId = replay.replayId;
		userId = replay.userId;
		trackId = replay.trackId;
		trackTimeSeconds = replay.trackTimeSeconds;
		eventsCount = replay.eventsCount;
		carPositionMt.set(replay.carPositionMt);
		carOrientationRads = replay.carOrientationRads;
		completed = replay.completed;
		created = replay.created;

		for (int i = 0; i < MaxEvents; i++) {
			forces[i].set(replay.forces[i]);
		}
	}

	public void begin (String trackId, String userId, Car car) {
		reset();
		this.trackId = trackId;
		this.userId = userId;
		carPositionMt.set(car.getWorldPosMt());
		carOrientationRads = car.getWorldOrientRads();
		created = TimeUtils.millis();
	}

	public boolean add (CarForces f) {
		forces[eventsCount++].set(f);
		if (eventsCount == MaxEvents) {
			reset();
			return false;
		}

		return true;
	}

	public void end (float trackTime) {
		trackTimeSeconds = trackTime;
		completed = eventsCount > 0 && eventsCount < MaxEvents;

		replayId = DigestUtils.computeDigest(this);
		if (!DigestUtils.isValidDigest(replayId)) {
			throw new URacerRuntimeException("The generated Replay ID is invalid (#" + replayId + ")");
		}
	}

	private String getDestDir () {
		// FIXME move this out of here!!!!
		return Storage.ReplaysRoot + trackId + "/" + userId + "/";
	}

	public boolean delete () {
		if (isValid()) {
			FileHandle hf = Gdx.files.external(getDestDir() + replayId);
			if (hf.exists()) {
				hf.delete();
				return true;
			}
		} else {
			Gdx.app.log("Replay", "Can't delete an invalid replay");
		}
		return false;
	}

	public String filename () {
		if (isValid()) {
			return getDestDir() + replayId;
		}

		return "";
	}

	/** Saves its data to a filename named as its replay ID in the replays output directory for this data's trackId and userId */
	public boolean save () {
		if (isValid()) { // sanity check
			String dest = getDestDir();

			// ensure destination directory exists
			Gdx.files.external(dest).mkdirs();

			FileHandle hf = Gdx.files.external(filename());
			if (hf.exists()) {
				Gdx.app.log("Replay", "=====> NOT OVERWRITING REPLAY (" + replayId + ") <=====");
				return false;
			}

			try {
				GZIPOutputStream gzos = new GZIPOutputStream(hf.write(false));
				DataOutputStream os = new DataOutputStream(gzos);

				// replay info data
				os.writeUTF(replayId);
				os.writeUTF(userId);
				os.writeUTF(trackId);
				os.writeFloat(trackTimeSeconds);
				os.writeInt(eventsCount);
				os.writeLong(created);

				// car data
				os.writeFloat(carPositionMt.x);
				os.writeFloat(carPositionMt.y);
				os.writeFloat(carOrientationRads);

				// write the effective number of captured CarForces events
				for (int i = 0; i < eventsCount; i++) {
					CarForces f = forces[i];
					os.writeFloat(f.velocity_x);
					os.writeFloat(f.velocity_y);
					os.writeFloat(f.angularVelocity);
				}

				os.close();

				return true;
			} catch (Exception e) {
				Gdx.app.log("Replay", "Couldn't save replay, reason: " + e.getMessage());
				return false;
			}
		} else {
			Gdx.app.log("Replay", "Couldn't save replay because its not valid.");
			return false;
		}
	}

	public static Replay load (String fullpathFilename) {
		FileHandle fh = Gdx.files.external(fullpathFilename);
		if (fh.exists()) {
			try {
				GZIPInputStream gzis = new GZIPInputStream(fh.read());
				DataInputStream is = new DataInputStream(gzis);

				Replay r = new Replay();
				r.completed = true;

				// replay info data
				r.replayId = is.readUTF();
				r.userId = is.readUTF();
				r.trackId = is.readUTF();
				r.trackTimeSeconds = is.readFloat();
				r.eventsCount = is.readInt();
				r.created = is.readLong();

				// car data
				r.carPositionMt.x = is.readFloat();
				r.carPositionMt.y = is.readFloat();
				r.carOrientationRads = is.readFloat();

				for (int i = 0; i < r.eventsCount; i++) {
					r.forces[i].velocity_x = is.readFloat();
					r.forces[i].velocity_y = is.readFloat();
					r.forces[i].angularVelocity = is.readFloat();
				}

				is.close();

				return r;
			} catch (Exception e) {
				Gdx.app.log("Replay",
					"Couldn't load replay (" + fullpathFilename + "), reason: " + e.getMessage() + " (" + e.toString() + ")");
			}
		} else {
			Gdx.app.log("Replay", "The specified replay doesn't exist (" + fullpathFilename + ")");
		}

		return null;
	}

	public boolean isValidData () {
		return completed && created > 0 && userId.length() > 0 && trackId.length() > 0;
	}

	public boolean isValid () {
		return isValidData() && DigestUtils.isValidDigest(replayId);
	}

	public CarForces[] getCarForces () {
		return forces;
	}

	public int getEventsCount () {
		return eventsCount;
	}

	public final Vector2 getStartPosition () {
		return carPositionMt;
	}

	public float getStartOrientation () {
		return carOrientationRads;
	}

	public String getReplayId () {
		return replayId;
	}

	public String getShortReplayId () {
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

	public long getCreationTimestamp () {
		return created;
	}
}
