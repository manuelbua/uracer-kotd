package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.game.input.Replay;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.CarUtils;

/** Implements an automated Car, playing previously recorded events. It will
 * ignore car-to-car collisions, but will respect in-track collisions and
 * responses.
 *
 * @author manuel */

public final class GhostCar extends Car {
	private static final int FadeEvents = 30;
	private Replay replay;
	private int indexPlay;
	private boolean hasReplay;

	// public CarState carState = null;

	public GhostCar( GameWorld gameWorld, CarModel model, Aspect aspect ) {
		super( gameWorld, CarType.ReplayCar, InputMode.InputFromReplay, model, aspect );
		indexPlay = 0;
		hasReplay = false;
		replay = null;
		this.renderer.setAlpha( 0.5f );
		// this.carState = new CarState( gameWorld, this );

		setActive( false );
		reset();
	}

	// input data for this car cames from a Replay object
	public void setReplay( Replay replay ) {
		this.replay = replay;
		hasReplay = (replay != null && replay.getEventsCount() > 0);

		setActive( hasReplay );
		resetPhysics();

		if( hasReplay ) {
			setAspect( replay.carAspect );
			setCarModel( replay.carModelType );
			renderer.setAlpha( 0 );

			// System.out.println( "Replaying " + replay.id );
			restart( replay );
//			Gdx.app.log( "GhostCar", "Replaying " + replay.trackTimeSeconds + "s" );
		}

		// else
		// {
		// if(replay==null)
		// System.out.println("Replay disabled");
		// else
		// System.out.println("Replay has no recorded events, disabling replaying.");
		// }
	}

	public boolean hasReplay() {
		return hasReplay;
	}

	private void restart( Replay replay ) {
		pos( replay.carPosition );
		orient( replay.carOrientation );
		resetTraveledDistance();
		indexPlay = 0;
	}

	@Override
	public void reset() {
		super.reset();
		setReplay( null );
		renderer.setAlpha( 0 );
	}

	@Override
	public void onRender( SpriteBatch batch ) {
		if( hasReplay && isActive() && renderer.getAlpha() > 0 ) {
			renderer.render( batch, stateRender );
		}
	}

	@Override
	protected void onComputeCarForces( CarForces forces ) {
		forces.reset();

		if( hasReplay ) {

			// indexPlay is NOT updated here, we don't want
			// to process a non-existent event when (indexPlay == replay.getEventsCount())
			forces.set( replay.forces[indexPlay] );

			// also change opacity, fade in/out based on
			// events played, events remaining
			if( indexPlay <= FadeEvents ) {
				renderer.setAlpha( ((float)indexPlay / (float)FadeEvents) * 0.5f );
			} else if( replay.getEventsCount() - indexPlay <= FadeEvents ) {
				float val = (float)(replay.getEventsCount() - indexPlay) / (float)FadeEvents;
				renderer.setAlpha( val * 0.5f );
			}
		}
	}

	@Override
	public void onAfterPhysicsSubstep() {
		super.onAfterPhysicsSubstep();

		if( hasReplay ) {
			indexPlay++;

			if( indexPlay == replay.getEventsCount() ) {

				CarUtils.dumpSpeedInfo( " Ghost", this, replay.trackTimeSeconds );

				restart( replay );
			}
		}
	}
}
