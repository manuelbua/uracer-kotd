package com.bitfire.uracer.entities;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.simulation.CarInput;

public class GhostCar extends Car
{
	private int playIndex = 0;
	private ArrayList<CarInput> input = new ArrayList<CarInput>();
	private CarInput carInput = new CarInput();

	private GhostCar( TiledMap map, Vector2 position, float orientation)
	{
		super(map, position, orientation, false);
	}

	// factory method
	public static GhostCar create( TiledMap map, Vector2 position, float orientation )
	{
		GhostCar car = new GhostCar( map, position, orientation);
		EntityManager.add( car );
		return car;
	}

	public void restartPlaying()
	{
		if( hasInput() )
		{
			playIndex = 0;
			resetPhysics();
			setTransform( originalPosition, originalOrientation );
		}
	}

	public void replay(ArrayList<CarInput> replay)
	{
		input = replay;
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
