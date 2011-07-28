package com.bitfire.uracer.screen;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.testtilemap.TestTilemap;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.Car;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.simulation.CarContactListener;


public class CarTestScreen extends Screen
{
	private FPSLogger fpslog = new FPSLogger();
	private Debug dbg;
	private Car car;

	// test
	private TestTilemap tm;


	public CarTestScreen()
	{
		dbg = new Debug( this );
		Physics.create( new Vector2( 0, 0 ), false );
		Physics.world.setContactListener( new CarContactListener() );
		EntityManager.create();

		tm = new TestTilemap();
		tm.create();

		// TODO: REFACTOR!
		float org = Config.PixelsPerMeter;
		Config.PixelsPerMeter *= tm.strategy.tileMapZoomFactor;
		System.out.println("px2mt=" + org + " ("+ Config.PixelsPerMeter + ")" + "["+tm.strategy.tileMapZoomFactor+"]");

		float tmw = tm.map.width * tm.map.tileWidth;
		float tmh = tm.map.height * tm.map.tileHeight;

		Director.createFromPixels( Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new Vector2( 0, 0 ), new Vector2(tmw,tmh) );


		Vector2 pos = tileToWorld(0,2);
		car = Car.create( pos, true );
		car.setTransform( pos, 90 * MathUtils.degreesToRadians );
		Director.setPositionPx( pos, false );
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

		if(Input.isOn( Keys.R ))
		{
			car.reset();
			car.setTransform( tileToWorld(0,2), 90 * MathUtils.degreesToRadians );
		}


		// follow the car
		Director.setPositionMt( car.getWorldPos(), false);

		OrthographicCamera cam = Director.getCamPixels();

		// move cam space origin to top-left
		tmpcam.set( cam.position.x, /*Director.worldSizePx.y-*/cam.position.y );

		// enforce bounds at cam space
		float minx = (cam.viewportWidth/2f) * tm.strategy.tileMapZoomFactor;
		float miny = (cam.viewportHeight/2f) * tm.strategy.tileMapZoomFactor;
		float maxx = tm.map.width*tm.map.tileWidth - minx;
		float maxy = tm.map.height*tm.map.tileHeight - miny;
		if(tmpcam.x < minx ) tmpcam.x = minx;
		if(tmpcam.x > maxx ) tmpcam.x = maxx;
		if(tmpcam.y < miny ) tmpcam.y = miny;
		if(tmpcam.y > maxy ) tmpcam.y = maxy;

//		Director.setPositionPx( tmpcam, false );

		Director.update();
	}

	private Vector2 tmptile2w = new Vector2();
	private Vector2 tileToWorld(int tilex, int tiley)
	{
		int mapH = tm.map.height;
		int tilesize = tm.map.tileWidth;
		int offx = 0, offy = 0;
		tmptile2w = new Vector2(
				tilex * tilesize + offx,
				(mapH-tiley) * tilesize + offy);
		return Physics.px2mt(tmptile2w);
	}

	private Vector2 tmpcam = new Vector2();
	@Override
	public void render( float temporalAliasingFactor )
	{
		GL20 gl = Gdx.graphics.getGL20();
		gl.glClearDepthf( 1 );
		gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );


		// sync tilemap orthocamera to director's camera
		OrthographicCamera oc = tm.orthoCam;
		PerspectiveCamera pc = tm.perspCam;
		oc.position.set( Director.getCamPixels().position );

		tm.updateCams( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		tm.tileMapRenderer.render( oc );
		EntityManager.raiseOnRender( temporalAliasingFactor );
		tm.renderMeshes( gl, oc, pc );


		//
		// debug
		//

		OrthographicCamera cam = Director.getCamPixels();

		if( Gdx.app.getType() == ApplicationType.Desktop )
		{
			dbg.renderB2dWorld( Director.getCamMeters().combined );
		}

		batch.begin();
		dbg.renderFrameStats( temporalAliasingFactor );
		car.debug( this, batch );

		drawString( "cam x=" + cam.position.x + ", y=" + cam.position.y, 0, 200 );
		drawString( "tmpcam x=" + tmpcam.x + ", y=" + tmpcam.y, 0, 207 );
		drawString( "tmpcam x=" + Input.getMouseX() + ", y=" + Input.getMouseY(), 0, 214 );
		batch.end();

//		fpslog.log();
	}
}
