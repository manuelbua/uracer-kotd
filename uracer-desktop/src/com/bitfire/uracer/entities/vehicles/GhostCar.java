package com.bitfire.uracer.entities.vehicles;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.carsimulation.Replay;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.factories.CarFactory.CarType;

/** Implements an automated Car, playing previously recorded events. It will
 * ignore car-to-car collisions, but will respect in-track collisions and
 * responses.
 * 
 * @author manuel */

public class GhostCar extends Car {
	private Replay replay;
	private int indexPlay;
	private boolean hasReplay;

	private GhostCar( World world, CarGraphics graphics, CarType type, CarModel model ) {
		super( world, graphics, model, type, CarInputMode.InputFromReplay, new Vector2( 0, 0 ), 0 );
		indexPlay = 0;
		hasReplay = false;
		replay = null;
		setActive( false, true );
	}

	// factory methods

	public static GhostCar createForFactory( World world, CarGraphics graphics, CarType type, CarModel model ) {
		GhostCar ghost = new GhostCar( world, graphics, type, model );
		EntityManager.add( ghost );
		return ghost;
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
		getCarDescriptor().set( replay.carDescriptor );
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
	public void onDebug() {
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
