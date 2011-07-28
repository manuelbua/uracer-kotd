package com.bitfire.uracer.screen;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.Disc;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.Rope;
import com.bitfire.uracer.utils.Box2DFactory;

public class TestScreen extends Screen
{
	private FPSLogger fpslog = new FPSLogger();
	private Debug dbg;

	private Vector2 gravity = new Vector2();

	public TestScreen()
	{
		dbg = new Debug( this );
		Physics.create( new Vector2( 0, -10 ), false );
		EntityManager.create();
		Director.createFromPixels( Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new Vector2( 0, 0 ), new Vector2(100,100) );
		populateWorld();
	}

	private void populateWorld()
	{
		float w = Physics.px2mt( Gdx.graphics.getWidth() );
		float h = Physics.px2mt( Gdx.graphics.getHeight() );

		Box2DFactory.createThinWall( Physics.world, -w / 2, -h / 2, -w / 2, h / 2, 0.1f );
		Box2DFactory.createThinWall( Physics.world, w / 2, -h / 2, w / 2, h / 2, 0.1f );
		Body ground = Box2DFactory.createThinWall( Physics.world, -w / 2, -h / 2, w / 2, -h / 2, 0.1f );
		Box2DFactory.createThinWall( Physics.world, -w / 2, h / 2, w / 2, h / 2, 0.1f );

		Rope.create( 6, ground );

		for( int i = 0; i < 50; i++ )
		{
			float x = (random.nextFloat() - 0.5f) * 2f;
			float y = (random.nextFloat() - 0.5f) * 2f;
			float radius = (random.nextFloat() + .1f) / 2f;
			Disc.create( new Vector2( x, y ), radius );
		}
	}

	@Override
	public void removed()
	{
		super.removed();
		dbg.dispose();
	}

	@Override
	public void tick()
	{
		EntityManager.raiseOnTick();

		if( Input.isOn( Keys.SPACE ) )
		{
			System.out.println( "JUMP" );
		}

		if( Input.isTouching() )
		{
			Config.PhysicsTimeMultiplier = 0.1f;
		} else
		{
			Config.PhysicsTimeMultiplier = 1f;
		}

		if( Gdx.app.getType() == ApplicationType.Android )
		{
			gravity.x = Input.getAccelY();
			gravity.y = -Input.getAccelX();
			gravity.mul( 2.25f );
			Physics.world.setGravity( gravity );
		}
	}

	@Override
	public void render( float temporalAliasingFactor )
	{
		GL20 gl = Gdx.graphics.getGL20();
		gl.glClearColor( 0, 0, 0, 1 );
		gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

		// render background
		// batch.begin();
		// draw( Art.titleScreen, 0, 0, Gdx.graphics.getWidth(),
		// Gdx.graphics.getHeight() );
		// batch.end();

		Director.update();
		EntityManager.raiseOnRender( temporalAliasingFactor );

		// debug

		if( Gdx.app.getType() == ApplicationType.Desktop )
		{
			dbg.renderB2dWorld( Director.getCamMeters().combined );
		}

		batch.begin();
		dbg.renderFrameStats( temporalAliasingFactor );
		batch.end();

		fpslog.log();
	}
}
