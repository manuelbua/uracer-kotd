
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
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.utils.DigestUtils;
import com.bitfire.uracer.utils.ReplayUtils;
import com.bitfire.uracer.utils.URacerRuntimeException;

/** Represents replay data to be feed to a GhostCar, the replay player.
 * 
 * @author manuel */

public final class Replay implements Disposable, Comparable<Replay> {
	public static final int MaxEvents = 5000;

	// replay data
	private Vector2 carPositionMt = new Vector2();
	private float carOrientationRads;
	private CarForces[] forces = null;

	// replay info data
	protected ReplayInfo info = new ReplayInfo();

	public Replay () {
		forces = new CarForces[MaxEvents];
		for (int i = 0; i < MaxEvents; i++) {
			forces[i] = new CarForces();
		}
		reset();
	}

	@Override
	public int compareTo (Replay o) {
		if (o == null) {
			throw new NullPointerException();
		}

		int thisTicks = info.getTicks();
		int otherTicks = o.info.getTicks();

		// if different time, then compare
		if (thisTicks != otherTicks) {
			return thisTicks - otherTicks;
		} else {
			// equal time, draw
			// the oldest wins
			if (info.created < o.info.created) return -1;
			if (info.created > o.info.created) return 1;
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

	// public void setId (String id) {
	// info.replayId = id;
	// }

	public void reset () {
		info.reset();
		carPositionMt.set(0, 0);
		carOrientationRads = 0;
		for (int i = 0; i < MaxEvents; i++) {
			forces[i].reset();
		}
	}

	public void copy (Replay replay) {
		info.copy(replay.info);
		carPositionMt.set(replay.carPositionMt);
		carOrientationRads = replay.carOrientationRads;
		for (int i = 0; i < MaxEvents; i++) {
			forces[i].set(replay.forces[i]);
		}
	}

	public void begin (String trackId, String userId, Car car) {
		reset();
		info.trackId = trackId;
		info.userId = userId;
		carPositionMt.set(car.getWorldPosMt());
		carOrientationRads = car.getWorldOrientRads();
		info.created = TimeUtils.millis();
	}

	public boolean add (CarForces f) {
		forces[info.eventsCount++].set(f);
		if (info.eventsCount == MaxEvents) {
			reset();
			return false;
		}

		return true;
	}

	public void end (int ticks) {
		info.trackTimeTicks = ticks;
		info.completed = info.eventsCount > 0 && info.eventsCount < MaxEvents;

		info.replayId = DigestUtils.computeDigest(this);
		if (!DigestUtils.isValidDigest(info.replayId)) {
			throw new URacerRuntimeException("The generated Replay ID is invalid (#" + info.replayId + ")");
		}
	}

	/** Saves its data to a filename named as its replay ID in the replays output directory for this data's trackId and userId */
	public boolean save () {
		if (isValid()) { // sanity check
			String dest = ReplayUtils.getDestinationDir(info);

			// ensure destination directory exists
			Gdx.files.external(dest).mkdirs();

			FileHandle hf = Gdx.files.external(ReplayUtils.getFullPath(info));
			if (hf.exists()) {
				throw new URacerRuntimeException("Replay " + info.getShortId() + " exists, this should never happens! ("
					+ info.replayId + ")");
			}

			try {
				GZIPOutputStream gzos = new GZIPOutputStream(hf.write(false));
				DataOutputStream os = new DataOutputStream(gzos);

				// replay info data
				os.writeUTF(info.replayId);
				os.writeUTF(info.userId);
				os.writeUTF(info.trackId);
				os.writeInt(info.trackTimeTicks);
				os.writeInt(info.eventsCount);
				os.writeLong(info.created);

				// car data
				os.writeFloat(carPositionMt.x);
				os.writeFloat(carPositionMt.y);
				os.writeFloat(carOrientationRads);

				// write the effective number of captured CarForces events
				for (int i = 0; i < info.eventsCount; i++) {
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
				r.info.completed = true;

				// replay info data
				r.info.replayId = is.readUTF();
				r.info.userId = is.readUTF();
				r.info.trackId = is.readUTF();
				r.info.trackTimeTicks = is.readInt();
				r.info.eventsCount = is.readInt();
				r.info.created = is.readLong();

				// car data
				r.carPositionMt.x = is.readFloat();
				r.carPositionMt.y = is.readFloat();
				r.carOrientationRads = is.readFloat();

				for (int i = 0; i < r.info.eventsCount; i++) {
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
		return info.isValidData();
	}

	public boolean isValid () {
		return info.isValid();
	}

	public CarForces[] getCarForces () {
		return forces;
	}

	public final Vector2 getStartPosition () {
		return carPositionMt;
	}

	public float getStartOrientation () {
		return carOrientationRads;
	}

	public ReplayInfo getInfo () {
		return info;
	}

	// commodity proxy functions

	public String getId () {
		return info.getId();
	}

	public String getShortId () {
		return info.getShortId();
	}

	public String getUserId () {
		return info.getUserId();
	}

	public String getTrackId () {
		return info.getTrackId();
	}

	public float getSeconds () {
		return info.getSeconds();
	}

	public int getMilliseconds () {
		return info.getMilliseconds();
	}

	public long getCreationTimestamp () {
		return info.getCreationTimestamp();
	}

	public int getEventsCount () {
		return info.getEventsCount();
	}

	public int getTicks () {
		return info.getTicks();
	}

	public String getSecondsStr () {
		return info.getSecondsStr();
	}

}
