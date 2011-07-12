package com.bitfire.uracer.screen;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.Box2DFactory;
import com.bitfire.uracer.entities.Disc;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.Rope;

public class TestScreen extends Screen
{
	private FPSLogger fpslog = new FPSLogger();
	private Debug dbg;

	private OrthographicCamera camScreen, camWorld;
	private SpriteBatch entitiesBatch;

	public TestScreen()
	{
		dbg = new Debug( this );
		Physics.create( new Vector2( 0, -10 ), false );
		EntityManager.clear();
		entitiesBatch = new SpriteBatch();

		Vector2 campos = new Vector2();
		campos.x = 0;
		campos.y = 100;

		camWorld = new OrthographicCamera( Physics.s2w( Gdx.graphics.getWidth() ), Physics.s2w( Gdx.graphics.getHeight() ) );
		camWorld.position.set( Physics.s2w( campos.x ), Physics.s2w( campos.y ), 0 );

		camScreen = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		camScreen.position.set( campos.x, campos.y, 0 );

		populateWorld();
	}

	private void populateWorld()
	{
		Body ground;
		{
			BodyDef bd = new BodyDef();
			ground = Physics.world.createBody( bd );

			PolygonShape shape = new PolygonShape();
			shape.setAsEdge( new Vector2( -40, 0 ), new Vector2( 40.0f, 0 ) );

			ground.createFixture( shape, 0 );
			shape.dispose();
		}


		Box2DFactory.createThinWall( Physics.world, -4.5f,0, -4.5f,5, 0.1f );
		Box2DFactory.createThinWall( Physics.world, 4.5f,0, 4.5f,5, 0.1f );
		Box2DFactory.createThinWall( Physics.world, -4.5f,5f, 4.5f,5f, 0.1f );

		Rope.create( 6, ground );
		Disc.create( new Vector2( 0, 2 ), 0.5f );
		Disc.create( new Vector2( -1, 3 ), 0.5f );
		Disc.create( new Vector2( 1, 2.5f ), 0.5f );
		Disc.create( new Vector2( 0, 4.5f ), 0.2f );
		Disc.create( new Vector2( -1.25f, 4.5f ), 0.32f );
	}

	@Override
	public void removed()
	{
		super.removed();
		dbg.dispose();
	}

	private Vector2 gravity = new Vector2();
	@Override
	public void tick( Input input )
	{
		EntityManager.onBeforePhysicsSubstep();
		Physics.world.step( Physics.dt, 10, 10 );
		EntityManager.onAfterPhysicsSubstep();

		if(Gdx.app.getType() == ApplicationType.Android)
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

		spriteBatch.begin();
		draw( Art.titleScreen, 0, 0 );
		spriteBatch.end();

		// render box2d world

		camScreen.update();
		camWorld.update();

		entitiesBatch.setProjectionMatrix( camScreen.projection );
		entitiesBatch.setTransformMatrix( camScreen.view );
		entitiesBatch.begin();
		EntityManager.onRender( entitiesBatch, camScreen, camWorld, temporalAliasingFactor );
		entitiesBatch.end();

		// debug

		if( Gdx.app.getType() != ApplicationType.Android )
		{
			dbg.renderB2dWorld( camWorld.combined );

			spriteBatch.begin();
			dbg.renderFrameStats( temporalAliasingFactor );
			spriteBatch.end();
		}

		fpslog.log();
	}
}
