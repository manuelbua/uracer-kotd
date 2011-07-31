package com.bitfire.uracer.screen;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.testtilemap.TestTilemap;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.VersionInfo;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.Car;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.simulation.CarContactListener;
import com.bitfire.uracer.utils.Convert;

public class CarTestScreen extends Screen
{
	private FPSLogger fpslog = new FPSLogger();
	private Car car;

	// test
	private TestTilemap tm;

	public CarTestScreen()
	{
		Gdx.graphics.setVSync( true );

		tm = new TestTilemap();
		tm.create();

		Vector2 scaled_worldsize_px = new Vector2(
				tm.map.width * tm.map.tileWidth / tm.strategy.tileMapZoomFactor,
				tm.map.height * tm.map.tileHeight / tm.strategy.tileMapZoomFactor);

		Config.asDefault( tm.strategy.tileMapZoomFactor );
		Convert.init( tm.strategy, tm.map );
		Director.createFromPixels( this, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new Vector2( 0, 0 ), scaled_worldsize_px );

		Debug.create();
		Physics.create( new Vector2( 0, 0 ), false );
		Physics.world.setContactListener( new CarContactListener() );
		EntityManager.create();

//		Vector2 pos = Convert.tileToPx( 0, 0 ).add( Convert.scaledPixels(64,-64) );
		Vector2 pos = Convert.scaledPosition(64, 64);
		car = Car.create( tm.map, pos, 90, true );
		tm.disposableMeshes.add( car.mesh );
	}

	@Override
	public void removed()
	{
		super.removed();
		Debug.dispose();
	}

	@Override
	public void tick()
	{
		if( Input.isOn( Keys.R ) )
		{
			car.reset();
			car.setTransform( Convert.tileToPx( 3, 1 ), 90);
		}

		Vector3 pos = Director.pos();
		if( Input.isOn( Keys.UP ) )
			pos.y += 10;
		if( Input.isOn( Keys.DOWN ) )
			pos.y -= 10;
		if( Input.isOn( Keys.LEFT ) )
			pos.x -= 10;
		if( Input.isOn( Keys.RIGHT ) )
			pos.x += 10;

		EntityManager.raiseOnTick();
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

		// follow the car
		Director.setPositionPx( car.state().position, false );

		Director.update();

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

		if( Gdx.app.getType() == ApplicationType.Desktop )
		{
			Debug.renderB2dWorld( Director.getMatViewProjMt() );
		}

		Debug.begin();

		EntityManager.raiseOnDebug();

		OrthographicCamera cam = Director.getCamera();
		Debug.renderFrameStats( temporalAliasingFactor );

		int fontW = 6;
		int fontH = 12;
		String uRacerInfo = "uRacer " + VersionInfo.versionName;
		int sw = uRacerInfo.length() * fontW;

		Debug.drawString( uRacerInfo, Gdx.graphics.getWidth()-sw, 0, fontW, fontH);
		Debug.drawString( "cam x=" + cam.position.x + ", y=" + cam.position.y, 0, 200 );
		Debug.drawString( "tmpcam x=" + tmpcam.x + ", y=" + tmpcam.y, 0, 207 );
		Debug.drawString( "mouse x=" + Input.getMouseX() + ", y=" + Input.getMouseY(), 0, 214 );
		Debug.drawString( "temp_alias=" + temporalAliasingFactor, 0, 221 );
		Debug.drawString( "subframe=" + Config.SubframeInterpolation, 0, 228 );

		Debug.end();
		// fpslog.log();
	}
}
