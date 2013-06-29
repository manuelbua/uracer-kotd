
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
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.utils.AMath;

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
	private long millis;

	public Replay () {
		forces = new CarForces[MaxEvents];
		for (int i = 0; i < MaxEvents; i++) {
			forces[i] = new CarForces();
		}

		reset();
	}

	// public boolean canCompareTo (Replay o) {
	// // a Replay can be compared to another Replay only if it has been recorded on the same track
	// return trackId.equals(o.getTrackId());
	// }

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
			if (millis < o.millis) return -1;
			if (millis > o.millis) return 1;
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

	public void reset () {
		replayId = "";
		userId = "";
		trackId = "";
		trackTimeSeconds = 0;
		eventsCount = 0;
		carPositionMt.set(0, 0);
		carOrientationRads = 0;
		completed = false;
		millis = 0;

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
		millis = replay.millis;

		for (int i = 0; i < MaxEvents; i++) {
			forces[i].set(replay.forces[i]);
		}
	}

	public void begin (String trackId, String userId, Car car) {
		reset();
		millis = TimeUtils.millis();
		this.userId = userId;
		this.trackId = trackId;
		carPositionMt.set(car.getWorldPosMt());
		carOrientationRads = car.getWorldOrientRads();
	}

	public boolean add (CarForces f) {
		forces[eventsCount++].set(f);
		if (eventsCount == MaxEvents) {
			reset();
			return false;
		}

		return true;
	}

	public void end (String replayId, float trackTime) {
		this.replayId = replayId;
		trackTimeSeconds = trackTime;
		completed = eventsCount > 0 && eventsCount < MaxEvents;
	}

	private String getDestDir () {
		return Storage.ReplaysRoot + trackId + "/" + userId + "/";
	}

	public void delete () {
		FileHandle hf = Gdx.files.external(getDestDir() + replayId);
		if (hf.exists()) hf.delete();
	}

	public boolean save (String filename) {
		if (isValid()) {
			String dest = getDestDir();
			Gdx.files.external(dest).mkdirs();

			FileHandle hf = Gdx.files.external(dest + filename);

			if (hf.exists()) {
				Gdx.app.log("Replay", "=====> NOT OVERWRITING REPLAY (" + replayId + ") <=====");
				return false;
			}

			try {

				// DataOutputStream os = new DataOutputStream(hf.write(false));
				GZIPOutputStream gzos = null;

				gzos = new GZIPOutputStream(hf.write(false));/*
																			 * { { def.setLevel(Deflater.BEST_COMPRESSION); } };
																			 */

				DataOutputStream os = new DataOutputStream(gzos);
				// ObjectOutputStream os = new ObjectOutputStream(gzos);

				// replay info data
				os.writeUTF(replayId);
				os.writeUTF(userId);
				os.writeUTF(trackId);
				os.writeFloat(trackTimeSeconds);
				os.writeInt(eventsCount);

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

				Gdx.app.log("Replay", "Replay \"" + replayId + "\" saved to \"" + filename + "\"");
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

	public static Replay loadLocal (String filename) {
		FileHandle fh = Gdx.files.external(Storage.ReplaysRoot + filename);

		if (fh.exists()) {
			try {
				// DataInputStream is = new DataInputStream( fh.read() );
				GZIPInputStream gzis = new GZIPInputStream(fh.read());
				DataInputStream is = new DataInputStream(gzis);

				Replay r = new Replay();

				// replay info data
				r.userId = is.readUTF();
				r.trackId = is.readUTF();
				r.trackTimeSeconds = is.readFloat();
				r.eventsCount = is.readInt();

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

				// Gdx.app.log( "Replay", "Done loading local replay" );
				return r;

			} catch (Exception e) {
				Gdx.app.log("Replay", "Couldn't load local replay, reason: " + e.getMessage());
			}
		} else {
			Gdx.app.log("Replay", "The specified replay doesn't exist (" + filename + ")");
		}

		return null;
	}

	public boolean isTrackTimeValid () {
		return (trackTimeSeconds > GameplaySettings.ReplayMinDurationSecs);
	}

	public boolean isValid () {
		return completed && isTrackTimeValid() && replayId.length() > 0 && userId.length() > 0 && trackId.length() > 0;
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

	public String getUserId () {
		return userId;
	}

	public String getTrackId () {
		return trackId;
	}

	public float getTrackTime () {
		return trackTimeSeconds;
	}
}
