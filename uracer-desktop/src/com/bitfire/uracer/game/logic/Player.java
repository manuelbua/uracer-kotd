package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.utils.Convert;

public class Player
{
	public final Car car;
	public final GhostCar ghost;

	/* position/orientation */

	// initial
	public final Vector2 startPos = new Vector2();
	public int startTileX = 1, startTileY = 1;
	public float startOrient = 0f;

	// current
	public int currTileX = 1, currTileY = 1;

	public Player(Car car, GhostCar ghost)
	{
		this.car = car;
		this.ghost = ghost;
	}

	private int lastTileX = 0, lastTileY = 0;
	public void update(IGameLogicListener listener)
	{
		// onTileChanged
		lastTileX = currTileX; lastTileY = currTileY;
		Vector2 tmp = Convert.pxToTile( car.state().position.x, car.state().position.y );
		currTileX = (int)tmp.x; currTileY = (int)tmp.y;
		if( (lastTileX != currTileX) || (lastTileY != currTileY) )
		{
			listener.onTileChanged(this);
		}

	}

	public void reset()
	{
		car.reset();
		ghost.reset();

		// causes an onTileChanged event to be raised
		lastTileX = lastTileY = currTileX = currTileY = -1;
	}
}
