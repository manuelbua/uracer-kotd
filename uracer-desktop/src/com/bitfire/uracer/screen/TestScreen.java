package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;

public class TestScreen extends Screen
{
	private BitmapFont font;

	private Box2DDebugRenderer renderer;
	private World world;
	private OrthographicCamera b2dCamera;

	// defines how many pixels are 1 Box2d meter
	private float PixelsToMeter = 10.0f;

	public TestScreen()
	{
		font = new BitmapFont( true );
		renderer = new Box2DDebugRenderer();
		world = new World( new Vector2( 0, -10 ), true );

		b2dCamera = new OrthographicCamera( s2w(Gdx.graphics.getWidth()), s2w(Gdx.graphics.getHeight()) );
		b2dCamera.position.set( 0, 0, 0 );

		createWorld();
	}

	private void createWorld()
	{
		int e_count = 30;
		Body ground;
		{
			BodyDef bd = new BodyDef();
			ground = world.createBody( bd );

			PolygonShape shape = new PolygonShape();
			shape.setAsEdge( new Vector2( -40, 0 ), new Vector2( 40.0f, 0 ) );

			ground.createFixture( shape, 0 );
			shape.dispose();
		}

		{
			PolygonShape shape = new PolygonShape();
			shape.setAsBox( 0.5f, 0.125f );
			FixtureDef fd = new FixtureDef();
			fd.shape = shape;
			fd.density = 20.0f;
			fd.friction = 0.2f;

			RevoluteJointDef jd = new RevoluteJointDef();

			Body prevBody = ground;

			for( int i = 0; i < e_count; i++ )
			{
				BodyDef bd = new BodyDef();
				bd.type = BodyType.DynamicBody;
				bd.position.set( -14.5f + 1.0f * i, 5.0f );
				Body body = world.createBody( bd );
				body.createFixture( fd );

				Vector2 anchor = new Vector2( -15.0f + 1.0f * i, 5.0f );
				jd.initialize( prevBody, body, anchor );
				world.createJoint( jd );
				prevBody = body;
			}

			Vector2 anchor = new Vector2( -15.0f + 1.0f * e_count, 5.0f );
			jd.initialize( prevBody, ground, anchor );
			world.createJoint( jd );
			shape.dispose();
		}

		for( int i = 0; i < 2; i++ )
		{
			Vector2[] vertices = new Vector2[ 3 ];
			vertices[0] = new Vector2( -0.5f, 0 );
			vertices[1] = new Vector2( 0.5f, 0 );
			vertices[2] = new Vector2( 0, 1.5f );

			PolygonShape shape = new PolygonShape();
			shape.set( vertices );

			FixtureDef fd = new FixtureDef();
			fd.shape = shape;
			fd.density = 1.0f;

			BodyDef bd = new BodyDef();
			bd.type = BodyType.DynamicBody;
			bd.position.set( -8.0f + 8.0f * i, 12.0f );
			Body body = world.createBody( bd );
			body.createFixture( fd );

			shape.dispose();
		}

		for( int i = 0; i < 3; i++ )
		{
			CircleShape shape = new CircleShape();
			shape.setRadius( 0.5f );

			FixtureDef fd = new FixtureDef();
			fd.shape = shape;
			fd.density = 1.0f;

			BodyDef bd = new BodyDef();
			bd.type = BodyType.DynamicBody;
			bd.position.set( -6.0f + 6.0f * i, 10.0f );

			Body body = world.createBody( bd );
			body.createFixture( fd );

			shape.dispose();
		}


		//
		PolygonShape shape = new PolygonShape();
		shape.setAsBox( 2.5f/2, 4.0f/2 );

		FixtureDef fd = new FixtureDef();
		fd.shape = shape;
		fd.density = 1.0f;

		BodyDef bd = new BodyDef();
		bd.type = BodyType.StaticBody;
		bd.position.set( 0, 0 );

		Body body = world.createBody( bd );
		body.createFixture( fd );

		shape.dispose();
	}

	@Override
	public void removed()
	{
		super.removed();
		font.dispose();
		renderer.dispose();
		world.dispose();
	}

	@Override
	public void tick( Input input )
	{
		world.step( URacer.oneOnTimestepHz, 10, 10 );
	}

	@Override
	public void render( float timeAliasingFactor )
	{

		GL10 gl = Gdx.graphics.getGL10();

		gl.glClearColor( 0, 0, 0, 1 );
		gl.glClear( GL10.GL_COLOR_BUFFER_BIT );

		b2dCamera.update();
		b2dCamera.apply( gl );

		spriteBatch.begin();
		draw( Art.titleScreen, 0, 0 );
		spriteBatch.end();

		// render box2d world
		b2dCamera.apply( gl );

		renderer.render( world );

		spriteBatch.begin();
		drawString(
			"fps:" + Gdx.graphics.getFramesPerSecond() +
			", physics: " + String.format( "%.08f", URacer.getPhysicsTime() ) +
			", render: " + String.format( "%.08f", URacer.getRenderTime() )
			, 0, 0
		);
		spriteBatch.end();
	}


	//
	// utils
	//

	private float w2s( float v )
	{
		return v * PixelsToMeter;
	}

	private float s2w( float v )
	{
		return v / PixelsToMeter;
	}

}
