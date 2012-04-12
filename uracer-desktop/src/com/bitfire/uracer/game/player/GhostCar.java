package com.bitfire.uracer.game.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.game.Replay;

/** Implements an automated Car, playing previously recorded events. It will
 * ignore car-to-car collisions, but will respect in-track collisions and
 * responses.
 *
 * @author manuel */

public final class GhostCar extends Car {
	private Replay replay;
	private int indexPlay;
	private boolean hasReplay;

	private GhostCar( CarRenderer graphics, CarAspect type, CarModel model ) {
		super( graphics, model, type, CarInputMode.InputFromReplay );
		indexPlay = 0;
		hasReplay = false;
		replay = null;
		setActive( false, true );
	}

	// factory methods

	public static GhostCar createForFactory( CarRenderer graphics, CarAspect type, CarModel model ) {
		return new GhostCar( graphics, type, model );
	}

	public void setReplay( Replay replay ) {
		this.replay = replay;
		hasReplay = (replay != null && replay.getEventsCount() > 0);
		setActive( hasReplay, true );
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
