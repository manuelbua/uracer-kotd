package com.bitfire.uracer.game.input;

import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.Car.Aspect;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.utils.UUid;

/** Represents replay data to be feed to a GhostCar, the replay player.
 *
 * @author manuel */

public class Replay {
	public static final int MaxEvents = 5000;
	private int eventsCount;

	// car data
	public Aspect carAspect;
	public CarModel.Type carModelType;
	public Vector2 carPosition = new Vector2();
	public float carOrientation;

	// replay data
	public final long id;
	public String trackName = "no-track";
	public GameDifficulty difficultyLevel = GameDifficulty.Easy;
	public float trackTimeSeconds = 0;
	public CarForces[] forces = null;
	public boolean isValid = false;

	// time track
	private Time time = new Time();

	public Replay() {
		eventsCount = 0;
		forces = new CarForces[ MaxEvents ];
		for( int i = 0; i < MaxEvents; i++ ) {
			forces[i] = new CarForces();
		}

		id = UUid.get();
	}

	public void dispose() {
		reset();
	}

	public void begin( String trackName, GameDifficulty difficulty, Car car ) {
		reset();
		carPosition.set( car.pos() );
		carOrientation = car.orient();
		carAspect = car.getAspect();
		carModelType = car.getCarModel().type;
		this.trackName = trackName;
		difficultyLevel = difficulty;
		time.start();
	}

	public void end() {
		time.stop();
		trackTimeSeconds = time.elapsed( Time.Reference.TickSeconds );
		isValid = true;
	}

	public void reset() {
		eventsCount = 0;
		isValid = false;
	}

	// recording
	public int getEventsCount() {
		return eventsCount;
	}

	public boolean add( CarForces f ) {
		forces[eventsCount++].set( f );
		if( eventsCount == MaxEvents ) {
			eventsCount = 0;
			return false;
		}

		return true;
	}

	public void save() {
		if( isValid ) {
			new Thread( new Runnable() {

				@Override
				public void run() {
					String filename = Config.URacerConfigFolder + Config.LocalReplaysStore + trackName + "-" + difficultyLevel + "-" + carModelType.toString();
					FileHandle hf = Gdx.files.external( filename );

					DataOutputStream os = new DataOutputStream( hf.write( false ) );

					try {
						os.writeLong( id );
						os.writeChars( trackName );
						os.writeChars( difficultyLevel.toString() );
						os.writeFloat( trackTimeSeconds );

						for( int i = 0; i < eventsCount; i++ ) {
							CarForces f = forces[i];
							os.writeFloat( f.velocity_x );
							os.writeFloat( f.velocity_y );
							os.writeFloat( f.angularVelocity );
						}

						os.close();
						Gdx.app.log( "Replay", "Done saving replay" );
					} catch( IOException e ) {
						Gdx.app.log( "Replay", "Couldn't save replay, reason: " + e.getMessage() );
					}
				}
			} ).start();
		}
	}
}
