package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.debug.Box2DDebugRenderer20;

public class TestScreen extends Screen
{
	private BitmapFont font;
	private FPSLogger fpslog = new FPSLogger();

	private Box2DDebugRenderer20 renderer;
	private World world;
	private TestRope rope;
	private OrthographicCamera camScreen, camWorld;
	private long frameStart = System.nanoTime();
	private float physicsTime, renderTime;

	public TestScreen()
	{
		font = new BitmapFont( true );
		renderer = new Box2DDebugRenderer20();

		camWorld = new OrthographicCamera( Physics.s2w( Gdx.graphics.getWidth() ), Physics.s2w( Gdx.graphics.getHeight() ) );
		camWorld.position.set( 0, 0, 0 );

		camScreen = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		camScreen.position.set( 0, 0, 0 );

		createWorld();
	}

	private void createWorld()
	{
		int e_count = 30;

		world = new World( new Vector2( 0, -10 ), true );

		Body ground;
		{
			BodyDef bd = new BodyDef();
			ground = world.createBody( bd );

			PolygonShape shape = new PolygonShape();
			shape.setAsEdge( new Vector2( -40, 0 ), new Vector2( 40.0f, 0 ) );

			ground.createFixture( shape, 0 );
			shape.dispose();
		}

		rope = new TestRope( world, e_count, ground );
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
		world.step( Physics.dt, 10, 10 );
	}

	@Override
	public void render( float timeAliasingFactor )
	{
		long time = System.nanoTime();

		GL20 gl = Gdx.graphics.getGL20();

		gl.glClearColor( 1, 0.5f, 0, 1 );
		gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

		// render background

		spriteBatch.begin();
		draw( Art.titleScreen, 0, 0 );
		spriteBatch.end();

		// render box2d world

		camScreen.update();
		camWorld.update();

		// renderer.render( camWorld.combined, world );
		rope.render( camScreen );

		// debug

		if( time - frameStart > 1000000000 )
		{
			physicsTime = URacer.getPhysicsTime();
			renderTime = URacer.getRenderTime();
			frameStart = time;
		}

		spriteBatch.begin();
		drawString( "fps:" + Gdx.graphics.getFramesPerSecond() + ", physics: " + physicsTime + ", render: " + renderTime, 0, 130 );
		spriteBatch.end();

		// fpslog.log();
	}
}
