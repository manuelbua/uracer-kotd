package com.bitfire.uracer.entities.vehicles;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.simulations.car.CarDescriptor;
import com.bitfire.uracer.simulations.car.CarInput;

public class GhostCar extends Car
{
	private int playIndex = 0;
	private CarInput carInput = new CarInput();

	// replay data
	private ArrayList<CarInput> input = new ArrayList<CarInput>();
	private Vector2 replayStartPosition = new Vector2();
	private float replayStartOrientation;
	private CarDescriptor replayCarDesc = new CarDescriptor();

	private GhostCar( Vector2 position, float orientation)
	{
		super(position, orientation, false);
	}

	// factory method
	public static GhostCar create( Vector2 position, float orientation )
	{
		GhostCar car = new GhostCar( position, orientation);
		EntityManager.add( car );
		return car;
	}

	public void restartPlaying()
	{
		if( hasInput() )
		{
			playIndex = 0;
			resetPhysics();
			this.carDesc.set( replayCarDesc );
			this.carSim.carDesc.set( replayCarDesc );
			setTransform( replayStartPosition, replayStartOrientation );
		}
	}

	public void setReplay(ArrayList<CarInput> replay, Vector2 startPosition, float startOrientation, CarDescriptor carDesc )
	{
		input.clear();
		input.addAll( replay );
		System.out.println("Replaying "+input.size() + "(" + replay.size() + ")");
		replayStartPosition = startPosition;
		replayStartOrientation = startOrientation;
		replayCarDesc.set( carDesc );
		restartPlaying();
	}

	@Override
	public void onRender( SpriteBatch batch )
	{
		if(input.size() > 0 )
		{
			sprite.setPosition( stateRender.position.x - sprite.getOriginX(), stateRender.position.y - sprite.getOriginY() );
			sprite.setRotation( stateRender.orientation );
			sprite.draw( batch, 0.5f );
		}
	}

	public boolean hasInput()
	{
		return (input.size() > 0);
	}

	@Override
	public void onDebug()
	{
		Debug.drawString( "[R] input count = " + input.size(), 0, 124 );
		Debug.drawString( "[R] play index = " + playIndex, 0, 132 );
	}

	@Override
	protected CarInput acquireInput()
	{
		if( hasInput() )
		{
			carInput.set( input.get( playIndex++ ) );

			if( playIndex == input.size() )
			{
				restartPlaying();
			}
		} else {
			carInput.reset();
		}

		return carInput;
	}
}
