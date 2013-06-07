
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
	private int eventsCount;

	// car data
	// public CarPreset.Type carPresetType;
	public Vector2 carWorldPositionMt = new Vector2();
	public float carWorldOrientRads;

	// replay data
	public long userId;
	public String levelId = "";
	public float trackTimeSeconds = 0;
	public CarForces[] forces = null;
	public boolean isValid = false;
	private boolean isLoaded = false;
	private boolean isSaved = false;

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

	public void copyData (Replay replay) {
		userId = replay.userId;
		trackTimeSeconds = replay.trackTimeSeconds;
		levelId = replay.levelId;
		eventsCount = replay.eventsCount;
		// carPresetType = replay.carPresetType;
		carWorldPositionMt.set(replay.carWorldPositionMt);
		carWorldOrientRads = replay.carWorldOrientRads;

		// inherits its data state, eg: if it was saved
		// there is no need to save it again
		isValid = replay.isValid;
		isLoaded = replay.isLoaded;
		isSaved = replay.isSaved;

		for (int i = 0; i < MaxEvents; i++) {
			forces[i].set(replay.forces[i]);
		}
	}

	public void begin (String levelId, Car car) {
		reset();
		carWorldPositionMt.set(car.getWorldPosMt());
		carWorldOrientRads = car.getWorldOrientRads();
		// carPresetType = car.getPresetType();
		this.levelId = levelId;
		time.start();

		// Gdx.app.log( "Replay", "Begin at " + carWorldPositionMt + ", " + carWorldOrientRads );
	}

	public void end () {
		time.stop();
		trackTimeSeconds = time.elapsed(Time.Reference.TickSeconds);
		isValid = true;
		isLoaded = false;
		isSaved = false;
	}

	public void reset () {
		eventsCount = 0;
		for (int i = 0; i < MaxEvents; i++) {
			forces[i].reset();
		}

		// if a previously loaded replay is being used, reset the loaded state
		// since its invalid
		isLoaded = false;

		isSaved = false;
		isValid = false;
	}

	// recording
	public int getEventsCount () {
		return eventsCount;
	}

	public boolean add (CarForces f) {
		forces[eventsCount++].set(f);
		if (eventsCount == MaxEvents) {
			eventsCount = 0;
			return false;
		}

		return true;
	}

	public static Replay loadLocal (String filename) {
		FileHandle fh = Gdx.files.external(Storage.ReplaysRoot + filename);

		if (fh.exists()) {
			try {
				// DataInputStream is = new DataInputStream( fh.read() );
				GZIPInputStream gzis = new GZIPInputStream(fh.read());
				DataInputStream is = new DataInputStream(gzis);

				// read header
				Replay r = new Replay(is.readLong());

				// replay info data
				r.levelId = is.readUTF();
				// r.difficultyLevel = GameDifficulty.valueOf( is.readUTF() );
				r.trackTimeSeconds = is.readFloat();
				if (!Replay.isValidLength(r.trackTimeSeconds)) {
					throw new Exception("invalid duration (" + r.trackTimeSeconds + "sec < " + GameplaySettings.ReplayMinDurationSecs
						+ ")");
				}
				r.eventsCount = is.readInt();

				// car data
				// r.carPresetType = CarPreset.Type.valueOf(is.readUTF());
				r.carWorldPositionMt.x = is.readFloat();
				r.carWorldPositionMt.y = is.readFloat();
				r.carWorldOrientRads = is.readFloat();

				for (int i = 0; i < r.eventsCount; i++) {
					r.forces[i].velocity_x = is.readFloat();
					r.forces[i].velocity_y = is.readFloat();
					r.forces[i].angularVelocity = is.readFloat();
				}

				is.close();

				r.isValid = true;
				r.isSaved = true;
				r.isLoaded = true;

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
		if (isValid && !isLoaded && !isSaved) {

			if (!Replay.isValidLength(trackTimeSeconds)) {
				Gdx.app.log("Replay", "Couldn't save local replay, reason: invalid duration (" + trackTimeSeconds + "sec < "
					+ GameplaySettings.ReplayMinDurationSecs + ")");
				return;
			}

			final String filename = Storage.ReplaysRoot + levelId;
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
						os.writeUTF(levelId);
						// os.writeUTF( difficultyLevel.toString() );
						os.writeFloat(trackTimeSeconds);
						os.writeInt(eventsCount);

						// car data
						// os.writeUTF(carPresetType.toString());
						os.writeFloat(carWorldPositionMt.x);
						os.writeFloat(carWorldPositionMt.y);
						os.writeFloat(carWorldOrientRads);

						// write the effective number of captured CarForces events
						for (int i = 0; i < eventsCount; i++) {
							CarForces f = forces[i];
							os.writeFloat(f.velocity_x);
							os.writeFloat(f.velocity_y);
							os.writeFloat(f.angularVelocity);
						}

						os.close();

						isSaved = true;

						messager.enqueue("Replay saved", 2f, Message.Type.Information, Position.Bottom, Size.Normal);
						// Gdx.app.log( "Replay", "Done saving local replay (" + trackTimeSeconds + ")" );

					} catch (IOException e) {
						Gdx.app.log("Replay", "Couldn't save local replay, reason: " + e.getMessage());
					}
				}
			}).start();
		}
	}

	private static boolean isValidLength (float seconds) {
		return (seconds > GameplaySettings.ReplayMinDurationSecs);
	}
}
