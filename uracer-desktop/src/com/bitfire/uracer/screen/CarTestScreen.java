package com.bitfire.uracer.screen;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.VersionInfo;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.Car;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.RadialBlur;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.tiled.OrthographicAlignedMesh;
import com.bitfire.uracer.utils.Convert;

public class CarTestScreen extends Screen
{
	private FPSLogger fpslog = new FPSLogger();
	private Car car;
	private Level level;
	// private GhostCar ghost;

	// test
	// private TestTilemap tm;
	private Vector2 carStartPos = new Vector2();
	// private Vector2 replayCarStartPos = new Vector2();
	// private float replayCarStartOrient;
	private RadialBlur rb;

	public CarTestScreen()
	{
		Gdx.graphics.setVSync( true );
		ShaderProgram.pedantic = false;

		Debug.create();
		OrthographicAlignedMesh.initialize();
		EntityManager.create();

		Director.create( this, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		level = Director.loadLevel( "level1" );

		carStartPos.set( Convert.tileToPx( 1, 0 ).add( Convert.scaledPixels( 128, -128 ) ) );

		// carStartPos.set( Convert.scaledPosition( 64, 64 ) );
		car = Car.create( carStartPos, 90 );
		// car.record( true );
		// ghost = GhostCar.create( Convert.scaledPosition( 0, 0 ), 90 );

		rb = new RadialBlur();
		rb.setEnabled( true );
		PostProcessor.init( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
//		PostProcessor.init( 512, 512 );
		PostProcessor.setEffect( rb );
	}

	@Override
	public void removed()
	{
		super.removed();
		Debug.dispose();
	}

	private Vector2 carTileAt = new Vector2();
	private Vector2 lastCarTileAt = new Vector2();

	@Override
	public void tick()
	{
		if( Input.isOn( Keys.R ) )
		{
			// ghost.resetPhysics();
			car.resetPhysics();
			car.setTransform( carStartPos, 90 );
		}

		// Vector3 pos = Director.pos();
		// if( Input.isOn( Keys.UP ) ) pos.y += 10;
		// if( Input.isOn( Keys.DOWN ) ) pos.y -= 10;
		// if( Input.isOn( Keys.LEFT ) ) pos.x -= 10;
		// if( Input.isOn( Keys.RIGHT ) ) pos.x += 10;

		EntityManager.raiseOnTick();

		//
		// ubersimple events dispatcher
		//
		lastCarTileAt.set( carTileAt );
		carTileAt.set( Convert.pxToTile( car.pos().x, car.pos().y ) );
		if( (lastCarTileAt.x != carTileAt.x) || (lastCarTileAt.y != carTileAt.y) )
		{
			onTileChanged( carTileAt );
		}

		// rb.setStrength( car.carDesc.velocity_wc.len() );
		rb.setStrength( (car.carDesc.velocity_wc.len() / car.carDesc.carModel.max_speed) * 0.05f );
		rb.dampStrength( 0.9f );
		rb.setOrigin( Director.screenPosFor( car.getBody() ) );
	}

	// FIXME
	// shit, shouldn't this be subscribed instead, and not self-raised like
	// this?
	//
	// private boolean doRecord = true;
	protected void onTileChanged( Vector2 carAt )
	{
		boolean onStartZone = (carAt.x == 1 && carAt.y == 0);
		if( onStartZone )
		{
			// if(doRecord)
			// {
			// System.out.println("Recording...");
			// replayCarStartPos.set( car.pos() );
			// replayCarStartOrient = car.orient();
			// car.record( true );
			// }
			// else
			// {
			// System.out.println("Playing...");
			// car.record( false );
			// ghost.resetPhysics();
			// ghost.setReplay( car.getReplay(), replayCarStartPos,
			// replayCarStartOrient, car.carDesc );
			// }
			//
			// doRecord = !doRecord;
		}
	}

	@Override
	public void beforeRender( float timeAliasingFactor )
	{
		EntityManager.raiseOnBeforeRender( timeAliasingFactor );
	}

	private void renderScene( GL20 gl, float temporalAliasingFactor )
	{
		gl.glClearDepthf( 1 );
		gl.glClearColor( 0, 0, 0, 1 );
		gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );

		level.syncWithCam( Director.getCamera() );
		level.renderTilemap();
		EntityManager.raiseOnRender( temporalAliasingFactor );
		level.renderMeshes( gl );
	}

	@Override
	public void render( float temporalAliasingFactor )
	{
		GL20 gl = Gdx.graphics.getGL20();

		// follow the car
		Director.setPositionPx( car.state().position, false );

		if( Config.EnablePostProcessingFx )
		{
			PostProcessor.begin();
			renderScene( gl, temporalAliasingFactor );
			PostProcessor.end();
		} else
		{
			gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
			renderScene( gl, temporalAliasingFactor );
		}

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

		Debug.drawString( uRacerInfo, Gdx.graphics.getWidth() - sw, 0, fontW, fontH );
		Debug.drawString( "cam x=" + cam.position.x + ", y=" + cam.position.y, 0, 200 );
		Debug.drawString( "mouse x=" + Input.getMouseX() + ", y=" + Input.getMouseY(), 0, 214 );
		Debug.drawString( "temp_alias=" + temporalAliasingFactor, 0, 221 );
		Debug.drawString( "subframe=" + Config.SubframeInterpolation, 0, 228 );

		Debug.end();
		// fpslog.log();
	}
}
