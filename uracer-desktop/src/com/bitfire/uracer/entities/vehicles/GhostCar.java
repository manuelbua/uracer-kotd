package com.bitfire.uracer.entities.vehicles;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.simulations.car.CarForces;
import com.bitfire.uracer.simulations.car.CarInputMode;

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
	private GhostCar( Car car )
	{
		super( car.getGraphics(), car.getCarModel(), car.getCarType(), CarInputMode.InputFromReplay, new Vector2(0,0), 0 );
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
	protected void onComputeCarForces( CarForces forces )
	{
		forces.reset();

		if( recorder.hasReplay() )
		{
			if(!isActive())
			{
				setActive( true, true );
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
		if(isActive())
		{
			setActive( false, true );
		}
	}
}
