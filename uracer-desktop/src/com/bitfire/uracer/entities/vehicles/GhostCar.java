package com.bitfire.uracer.entities.vehicles;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.simulations.car.CarForces;

/**
 * Implements an automated Car, playing previously recorded events.
 * It will ignore car-to-car collisions, but will respect in-track
 * collisions and responses.
 *
 * @author manuel
 *
 */

public class GhostCar extends Car
{
	private CarForces forces = new CarForces();
	private boolean activated = false;

	private GhostCar( Car car )
	{
		super( car.graphics, car.carDesc.carModel, car.carType, new Vector2(0,0), 0, false );
		inputMode = CarInputMode.InputFromReplay;
		activated = false;
	}

	// factory methods
	public static GhostCar createForFactory( Car car )
	{
		GhostCar ghost = new GhostCar( car );
		EntityManager.add( ghost );
		return ghost;
	}

	@Override
	public void onRender( SpriteBatch batch )
	{
		if( recorder.hasReplay() )
		{
			graphics.render( batch, stateRender, 0.5f );
		}
	}

	@Override
	public void onDebug()
	{
	}

	@Override
	protected strictfp void transformInput()
	{
		forces.reset();

		if( recorder.hasReplay() )
		{
			if(!activated)
			{
				this.body.setActive( true );
				activated = true;
			}

			if( !recorder.get( forces ) )
			{
				if( recorder.hasFinishedPlaying() )
				{
					// restart playing
					System.out.println( "restarting replay" );
					recorder.beginPlay( this );
				}
			}
		}
		else
		if(activated)
		{
			this.body.setActive( false );
			activated = false;
		}

		carDesc.velocity_wc.set( forces.velocity_x, forces.velocity_y );
		carDesc.angularvelocity = forces.angularVelocity;
	}
}
