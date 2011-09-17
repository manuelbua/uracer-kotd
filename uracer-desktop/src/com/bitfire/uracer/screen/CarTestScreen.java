package com.bitfire.uracer.screen;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.GameplaySettings;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.factories.CarFactory;
import com.bitfire.uracer.factories.CarFactory.CarType;
import com.bitfire.uracer.factories.ModelFactory;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.RadialBlur;
import com.bitfire.uracer.simulations.car.Recorder;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.utils.Convert;

public class CarTestScreen extends Screen
{
	private Car car = null, other = null;
	private GhostCar ghost = null;
	private Level level;

	// test
	// private TestTilemap tm;
	private Vector2 carStartPos = new Vector2();
	private Vector2 otherStartPos = new Vector2();
	// private Vector2 replayCarStartPos = new Vector2();
	// private float replayCarStartOrient;
	private RadialBlur rb;

	private Recorder recorder;

	public CarTestScreen()
	{
		ShaderProgram.pedantic = false;

		recorder = Recorder.create();
		EntityManager.create();
		ModelFactory.init();

		Director.create( this, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		GameplaySettings gs = GameplaySettings.create( GameplaySettings.Easy );
		level = Director.loadLevel( "level1", gs );
		Director.setPositionPx( Director.positionFor( new Vector2(0,0)), false );

		carStartPos.set( Convert.tileToPx( 1, 0 ).add( Convert.scaledPixels( 112, -112 ) ) );
		otherStartPos.set( Convert.tileToPx( 3, 0 ).add( Convert.scaledPixels( 112, -112 ) ) );

		CarModel m = new CarModel();
		car = CarFactory.createPlayer( CarType.OldSkool, m.toModel2(), carStartPos, 90 );
		ghost = CarFactory.createGhost( car );

		if( Config.EnablePostProcessingFx )
		{
			rb = new RadialBlur();
			rb.setEnabled( true );
			PostProcessor.init( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
//			PostProcessor.init( 512, 512 );
			PostProcessor.setEffect( rb );
		}
	}

	@Override
	public void removed()
	{
		super.removed();
		Director.dispose();
	}

	private Vector2 carTileAt = new Vector2();
	private Vector2 lastCarTileAt = new Vector2();

	@Override
	public void tick()
	{
		if( Input.isOn( Keys.R ) )
		{
			if(car != null )
			{
				car.resetPhysics();
				car.setTransform( carStartPos, 90f );
				recorder.clear();
				recorder.beginRec( car );
			}

			if(other!=null)
			{
				other.resetPhysics();
				other.setTransform( otherStartPos, 90f );
			}
		}
		else
		if( Input.isOn( Keys.Q ))
		{
			// start recording
			recorder.clear();
			recorder.beginRec( car );
			System.out.println("------------------- RECORDING");
		}
		else
		if( Input.isOn( Keys.W ))
		{
			if(recorder.isRecording())
			{
				recorder.endRec();
				System.out.println("-----------------------------");
			}

			recorder.beginPlay( ghost );
		}

		EntityManager.raiseOnTick();

		//
		// ubersimple events dispatcher
		//
		if(car!=null)
		{
			lastCarTileAt.set( carTileAt );
			carTileAt.set( Convert.pxToTile( car.pos().x, car.pos().y ) );
			if( (lastCarTileAt.x != carTileAt.x) || (lastCarTileAt.y != carTileAt.y) )
			{
				onTileChanged( carTileAt );
			}

			if( Config.EnablePostProcessingFx )
			{
				rb.dampStrength( 0.8f, Physics.dt );
				rb.setOrigin( Director.screenPosFor( car.getBody() ) );
			}
		}
	}

	// FIXME
	// shit, shouldn't this be subscribed instead, and not self-raised like
	// this?
	//

	private boolean firstLap = true;
	protected void onTileChanged( Vector2 carAt )
	{
		boolean onStartZone = (carAt.x == 1 && carAt.y == 0);
		if( onStartZone )
		{
//			if(firstLap)
//			{
//				firstLap = false;
//				recorder.beginRec( car );
//			}
//			else
//			{
//				if(!recorder.hasReplay())
//				{
//					int recevents = recorder.endRec();
//					System.out.println( "arrived, playing " + recevents + " events" );
//				}
//
//				recorder.beginPlay( ghost );
//			}

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
		if(car!=null)
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

		boolean isDesktop = Gdx.app.getType() == ApplicationType.Desktop;
		if( isDesktop ) Debug.renderB2dWorld( Director.getMatViewProjMt() );

		Debug.begin();
		EntityManager.raiseOnDebug();
		Debug.renderVersionInfo();
		Debug.renderFrameStats( temporalAliasingFactor );
		if( isDesktop ) Debug.renderMemoryUsage();
//		Debug.drawString( "EMgr::maxSpritesInBatch = " + EntityManager.maxSpritesInBatch(), 0, 6 );
//		Debug.drawString( "EMgr::renderCalls = " + EntityManager.renderCalls(), 0, 12 );
		Debug.end();
	}
}
