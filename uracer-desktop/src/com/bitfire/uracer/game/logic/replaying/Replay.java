
package com.bitfire.uracer.game.logic.replaying;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.logic.gametasks.Messager;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;

/** Represents replay data to be feed to a GhostCar, the replay player.
 * 
 * @author manuel */

public class Replay implements Disposable {
	public static final int MaxEvents = 5000;

	// car data
	private Vector2 carPositionMt = new Vector2();
	private float carOrientationRads;

	// replay data
	private long userId;
	private String trackId = "";
	private float trackTimeSeconds = 0;
	private CarForces[] forces = null;
	private int eventsCount;
	private boolean completed = false;

	// time track
	private Time time = new Time();

	public Replay (long userId) {
		this.userId = userId;
		eventsCount = 0;
		forces = new CarForces[MaxEvents];
		for (int i = 0; i < MaxEvents; i++) {
			forces[i] = new CarForces();
		}
	}

	@Override
	public void dispose () {
		reset();
		time.dispose();

		for (int i = 0; i < MaxEvents; i++) {
			forces[i] = null;
		}
	}

	public void reset () {
		trackId = "";
		completed = false;
		eventsCount = 0;
		carPositionMt.set(0, 0);
		carOrientationRads = 0;
		time.reset();
		for (int i = 0; i < MaxEvents; i++) {
			forces[i].reset();
		}
	}

	public void copyData (Replay replay) {
		userId = replay.userId;
		trackId = replay.trackId;
		trackTimeSeconds = replay.trackTimeSeconds;
		eventsCount = replay.eventsCount;
		carPositionMt.set(replay.carPositionMt);
		carOrientationRads = replay.carOrientationRads;
		completed = replay.completed;

		for (int i = 0; i < MaxEvents; i++) {
			forces[i].set(replay.forces[i]);
		}
	}

	public void begin (String trackId, Car car) {
		reset();
		carPositionMt.set(car.getWorldPosMt());
		carOrientationRads = car.getWorldOrientRads();
		this.trackId = trackId;
		time.start();

		// Gdx.app.log( "Replay", "Begin at " + carWorldPositionMt + ", " + carWorldOrientRads );
	}

	public boolean add (CarForces f) {
		forces[eventsCount++].set(f);
		if (eventsCount == MaxEvents) {
			reset();
			return false;
		}

		return true;
	}

	public void end () {
		time.stop();
		trackTimeSeconds = time.elapsed(Time.Reference.TickSeconds);
		completed = eventsCount > 0 && eventsCount < MaxEvents;
	}

	public static Replay loadLocal (String filename) {
		FileHandle fh = Gdx.files.external(Storage.ReplaysRoot + filename);

		if (fh.exists()) {
			try {
				// DataInputStream is = new DataInputStream( fh.read() );
				GZIPInputStream gzis = new GZIPInputStream(fh.read());
				DataInputStream is = new DataInputStream(gzis);

				// read header
				long userId = is.readLong();

				Replay r = new Replay(userId);

				// replay info data
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

	public void saveLocal (final Messager messager) {
		if (isValid()) {
			final String filename = Storage.ReplaysRoot + trackId;
			final FileHandle hf = Gdx.files.external(filename);

			// this is an asynchronous operation, but it's safe since saving a replay
			// imply this replay won't get overwritten anytime soon
			new Thread(new Runnable() {

				@Override
				public void run () {
					try {

						// DataOutputStream os = new DataOutputStream(hf.write(false));
						GZIPOutputStream gzos = null;

						try {
							gzos = new GZIPOutputStream(hf.write(false));/*
																						 * { { def.setLevel(Deflater.BEST_COMPRESSION); } };
																						 */
						} catch (GdxRuntimeException e) {
							messager
								.enqueue("Couldn't save local replay, no space?", 5f, Message.Type.Bad, Position.Bottom, Size.Normal);
							return;
						}

						DataOutputStream os = new DataOutputStream(gzos);
						// ObjectOutputStream os = new ObjectOutputStream(gzos);

						// final Long luid = userId;

						// write header
						os.writeLong(userId);

						// replay info data
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

						messager.enqueue("Replay saved", 2f, Message.Type.Information, Position.Bottom, Size.Normal);
						// Gdx.app.log( "Replay", "Done saving local replay (" + trackTimeSeconds + ")" );

					} catch (IOException e) {
						Gdx.app.log("Replay", "Couldn't save local replay, reason: " + e.getMessage());
					}
				}
			}).start();
		} else {
			Gdx.app.log("Replay", "Couldn't save invalid local replay.");
		}
	}

	public boolean isValid () {
		return completed && (trackTimeSeconds > GameplaySettings.ReplayMinDurationSecs);
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

	public long getUserId () {
		return userId;
	}

	public String getTrackId () {
		return trackId;
	}

	public float getTrackTime () {
		return trackTimeSeconds;
	}
}
