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
import com.badlogic.gdx.math.Vector3;
import com.bitfire.testtilemap.TestTilemap;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.Car;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.EntityState;
import com.bitfire.uracer.simulation.CarContactListener;


public class CarTestScreen extends Screen
{
	private FPSLogger fpslog = new FPSLogger();
	private Debug dbg;
	private Car car;
	private EntityState camStateCurr = new EntityState(), camStatePrev = new EntityState(), camState = new EntityState();

	// test
	private TestTilemap tm;


	public CarTestScreen()
	{
		Gdx.graphics.setVSync( true );

		dbg = new Debug( this );
		Physics.create( new Vector2( 0, 0 ), false );
		Physics.world.setContactListener( new CarContactListener() );
		EntityManager.create();

		tm = new TestTilemap();
		tm.create();

		// TODO: REFACTOR!
//		float org = Config.PixelsPerMeter;
		Config.PixelsPerMeter /= tm.strategy.tileMapZoomFactor;
//		System.out.println("px2mt=" + org + " ("+ Config.PixelsPerMeter + ")" + "["+tm.strategy.tileMapZoomFactor+"]");

		float tmw = tm.map.width * tm.map.tileWidth / tm.strategy.tileMapZoomFactor;
		float tmh = tm.map.height * tm.map.tileHeight / tm.strategy.tileMapZoomFactor;

		Director.createFromPixels( tm.strategy, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new Vector2( 0, 0 ), new Vector2(tmw,tmh) );
//		System.out.println("px");
//		System.out.println(Director.getCamPixels().combined.toString());
//		System.out.println("mvpPx");
//		System.out.println(Director.getMatViewProjPx().toString());
//		System.out.println("mt");
//		System.out.println(Director.getCamMeters().combined.toString());
//		System.out.println("mvpMt");
//		System.out.println(Director.getMatViewProjMt().toString());

		Vector2 pos = tileToWorld(3,1);
		Vector2 zero = new Vector2(0,0);
//		pos.mul( tm.strategy.tileMapZoomFactor );
		car = Car.create( pos, true );
		car.setTransform( pos, 90 * MathUtils.degreesToRadians );
//		Director.setPositionMt( zero, true );
		Director.setPositionMt( car.getWorldPos(), false );
		camStateCurr.set( Director.pos2(), 0 );
		camStatePrev.set( camStateCurr );
		camState.set( camStateCurr );
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
		if(Input.isOn( Keys.R ))
		{
			car.reset();
			car.setTransform( tileToWorld(1,1), 90 * MathUtils.degreesToRadians );
		}

		OrthographicCamera cam = Director.getCamera();

		Vector3 pos = Director.pos();
		if(Input.isOn( Keys.UP )) pos.y += 10;
		if(Input.isOn( Keys.DOWN )) pos.y -= 10;
		if(Input.isOn( Keys.LEFT)) pos.x -= 10;
		if(Input.isOn( Keys.RIGHT )) pos.x += 10;

		EntityManager.raiseOnTick();
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
		return Physics.px2mt(tmptile2w).mul(1f/tm.strategy.tileMapZoomFactor);
//		return Physics.px2mt(tmptile2w);
	}

	@Override
	public void beforeRender( float timeAliasingFactor )
	{
		EntityManager.raiseOnBeforeRender( timeAliasingFactor );
	}

	private Vector2 tmpcam = new Vector2();
	@Override
	public void render( float temporalAliasingFactor )
	{
		GL20 gl = Gdx.graphics.getGL20();
		gl.glClearDepthf( 1 );
		gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );


		OrthographicCamera cam = Director.getCamera();
		{
			camStatePrev.set( Director.pos2(), 0 );

			// follow the car
//			Director.setPositionMt( car.getWorldPos(), false );
			Director.setPositionMt( car.getState().position, false );

			// move cam space origin to top-left
			tmpcam.set( cam.position.x, /* Director.worldSizePx.y- */cam.position.y );

			// // enforce bounds at cam space
			float minx = (cam.viewportWidth / 2f);
			float maxy = (cam.viewportHeight / 2f);
			float maxx = Director.worldSizePx.x - (cam.viewportWidth / 2f);
			float miny = Director.worldSizePx.y - (cam.viewportHeight / 2f);
			if( tmpcam.x < minx )
				tmpcam.x = minx;
			if( tmpcam.x > maxx )
				tmpcam.x = maxx;

			if( tmpcam.y > miny )
				tmpcam.y = miny;
			if( tmpcam.y < maxy )
				tmpcam.y = maxy;

			Director.setPositionPx( tmpcam, false );

			camStateCurr.set( Director.pos2(), 0 );
		}

		Director.update();






//		if(Config.SubframeInterpolation)
//		{
//			camState.set( EntityState.interpolate( camStatePrev, camStateCurr, temporalAliasingFactor ));
//			Vector3 pos = Director.pos();
//			pos.x = camState.position.x;
//			pos.y = camState.position.y;
//			Director.update();
//		}

		// sync tilemap orthocamera to director's camera
		OrthographicCamera oc = tm.orthoCam;
		PerspectiveCamera pc = tm.perspCam;

		oc.position.set( Director.getCamera().position );
		oc.position.mul( tm.strategy.tileMapZoomFactor );
		tm.updateCams( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );


		tm.tileMapRenderer.render( oc );
		EntityManager.raiseOnRender( temporalAliasingFactor );
		tm.renderMeshes( gl, oc, pc );


		//
		// debug
		//

//		OrthographicCamera cam = Director.getCamera();

		if( Gdx.app.getType() == ApplicationType.Desktop )
		{
			dbg.renderB2dWorld( Director.getMatViewProjMt() );
		}

		batch.begin();
		dbg.renderFrameStats( temporalAliasingFactor );
		car.debug( this, batch );

		drawString( "cam x=" + cam.position.x + ", y=" + cam.position.y, 0, 200 );
		drawString( "tmpcam x=" + tmpcam.x + ", y=" + tmpcam.y, 0, 207 );
		drawString( "mouse x=" + Input.getMouseX() + ", y=" + Input.getMouseY(), 0, 214 );
		drawString( "temp_alias=" + temporalAliasingFactor, 0, 221 );
		drawString( "subframe=" + Config.SubframeInterpolation, 0, 228 );
		batch.end();

//		fpslog.log();
	}
}
