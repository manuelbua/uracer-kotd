package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.utils.Box2DFactory;
import com.bitfire.uracer.utils.Convert;

public class Disc extends b2dEntity
{
	private Sprite sprite;

	private Disc( Vector2 position, float radius )
	{
		body = Box2DFactory.createCircle( Physics.world, position.x, position.y, radius, false );

		sprite = new Sprite();
		sprite.setRegion( Art.disc );
		sprite.setSize( Convert.mt2px( radius * 2 ), Convert.mt2px( radius * 2 ) );
		sprite.setOrigin( sprite.getWidth() / 2, sprite.getHeight() / 2 );
	}

	// factory method
	public static Disc create( Vector2 position, float radius )
	{
		Disc disc = new Disc( position, radius );
		EntityManager.add( disc );
		return disc;
	}

	@Override
	public void onRender( SpriteBatch batch )
	{
		sprite.setPosition( stateRender.position.x - sprite.getOriginX(), stateRender.position.y - sprite.getOriginY() );
		sprite.setRotation( stateRender.orientation );
		sprite.draw( batch );
	}
}
