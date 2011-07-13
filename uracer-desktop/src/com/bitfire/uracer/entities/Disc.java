package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.utils.Box2DFactory;

public class Disc extends SubframeInterpolableEntity
{
	public Body body;
	private Sprite sprite;
	private Vector2 pos = new Vector2();
	private EntityScreenState screenState = new EntityScreenState();

	private Disc(Vector2 position, float radius)
	{
		body = Box2DFactory.createCircle( Physics.world, position.x, position.y, radius, false );

		sprite = new Sprite();
		sprite.setRegion( Art.disc );
		sprite.setSize( Physics.w2s( radius*2 ), Physics.w2s( radius*2 ) );
		sprite.setOrigin( sprite.getWidth()/2, sprite.getHeight()/2 );
	}

	// factory method
	public static Disc create( Vector2 position, float radius )
	{
		Disc disc = new Disc( position, radius );
		EntityManager.add( disc );
		return disc;
	}

	@Override
	public boolean isSubframeInterpolated()
	{
		return Config.SubframeInterpolation;
	}


	@Override
	public void saveStateTo( EntityScreenState state )
	{
		state.position.set( body.getPosition() );
		state.orientation = body.getAngle();
	}


	@Override
	public void onRender( SpriteBatch batch, Camera screen, Camera world, float temporalAliasingFactor )
	{
		screenState.set( getState() );

		pos = Physics.w2s( screenState.position );
		float ang = screenState.orientation * MathUtils.radiansToDegrees;

		sprite.setPosition( pos.x - sprite.getOriginX(), pos.y - sprite.getOriginY() );
		sprite.setRotation( ang );
		sprite.draw( batch );
	}
}
