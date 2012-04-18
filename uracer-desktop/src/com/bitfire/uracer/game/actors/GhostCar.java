package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.game.input.Replay;

/** Implements an automated Car, playing previously recorded events. It will
 * ignore car-to-car collisions, but will respect in-track collisions and
 * responses.
 *
 * @author manuel */

public final class GhostCar extends Car {
	private Replay replay;
	private int indexPlay;
	private boolean hasReplay;

	public GhostCar( World box2dWorld, CarRenderer graphics, CarModel model, Aspect aspect ) {
		super( box2dWorld, graphics, model, aspect );
		indexPlay = 0;
		hasReplay = false;
		replay = null;
		this.inputMode = InputMode.InputFromReplay;
		setActive( false );
		resetPhysics();
	}

	@Override
	public Vector2 getVelocity() {
		return null;
	}

	@Override
	public float getThrottle() {
		return 0;
	}

	@Override
	public Vector2 getLateralForceFront() {
		return null;
	}

	@Override
	public Vector2 getLateralForceRear() {
		return null;
	}

	// input data for this car cames from a Replay object
	public void setReplay( Replay replay ) {
		this.replay = replay;
		hasReplay = (replay != null && replay.getEventsCount() > 0);

		setActive( hasReplay );
		resetPhysics();

		if( hasReplay ) {
			// System.out.println( "Replaying " + replay.id );
			restart( replay );
		}

		// else
		// {
		// if(replay==null)
		// System.out.println("Replay disabled");
		// else
		// System.out.println("Replay has no recorded events, disabling replaying.");
		// }
	}

	private void restart( Replay replay ) {
		pos( replay.carPosition );
		orient( replay.carOrientation );
		indexPlay = 0;
	}

	@Override
	public void reset() {
		super.reset();
		setReplay( null );
	}

	@Override
	public void onRender( SpriteBatch batch ) {
		if( isActive() ) {
			graphics.render( batch, stateRender, 0.5f );
		}
	}

	@Override
	public void onDebug( SpriteBatch batch ) {
		// no output
	}

	@Override
	protected void onComputeCarForces( CarForces forces ) {
		forces.reset();

		if( hasReplay ) {
			if( indexPlay == replay.getEventsCount() ) {
				// System.out.println( "Playing finished, restarting." );
				restart( replay );
			}

			forces.set( replay.forces[indexPlay++] );
		}
	}
}
