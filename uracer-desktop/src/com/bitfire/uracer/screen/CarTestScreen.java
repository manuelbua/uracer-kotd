package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.GameDifficulty;
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
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.simulations.car.Recorder;
import com.bitfire.uracer.simulations.car.Replay;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.utils.Convert;

public class CarTestScreen extends Screen
{
	private Car player = null;
	private GhostCar ghost = null;
	private Level level;

	private Vector2 carStartPos = new Vector2();
	private RadialBlur rb;

	private Recorder recorder;
	private Replay[] replays;
	private boolean isFirstLap = true;

	public CarTestScreen()
	{
		recorder = Recorder.create();
		EntityManager.create();
		ModelFactory.init();

		Director.create( this, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		GameplaySettings gs = GameplaySettings.create( GameDifficulty.Easy );
		level = Director.loadLevel( "level1", gs );
		Director.setPositionPx( Director.positionFor( new Vector2(0,0)), false );

		carStartPos.set( Convert.tileToPx( 1, 0 ).add( Convert.scaledPixels( 112, -112 ) ) );

		CarModel m = new CarModel();
		player = CarFactory.createPlayer( CarType.OldSkool, m.toModel2(), carStartPos, 90 );
		ghost = CarFactory.createGhost( player );

		// replay buffers
		replays = new Replay[2];
		replays[0] = new Replay();
		replays[1] = new Replay();

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
			if(player != null )
			{
				player.resetPhysics();
				player.setTransform( carStartPos, 90f );

				ghost.setReplay( null );

				replays[0].clearForces();
				replays[1].clearForces();
				recorder.reset();
				lastCarTileAt.set( -1, -1 );
				carTileAt.set( lastCarTileAt );
				isFirstLap = true;
			}
		}
//		else
//		if( Input.isOn( Keys.Q ))
//		{
//			// start recording
//			recorder.clear();
//			recorder.beginRec( car );
//			System.out.println("------------------- RECORDING");
//		}
//		else
//		if( Input.isOn( Keys.W ))
//		{
//			if(recorder.isRecording())
//			{
//				recorder.endRec();
//				System.out.println("-----------------------------");
//			}
//
//			recorder.beginPlay( ghost );
//		}

		EntityManager.raiseOnTick();

		//
		// ubersimple events dispatcher
		//
		if(player!=null)
		{
			lastCarTileAt.set( carTileAt );
			carTileAt.set( Convert.pxToTile( player.pos().x, player.pos().y ) );
			if( (lastCarTileAt.x != carTileAt.x) || (lastCarTileAt.y != carTileAt.y) )
			{
				onTileChanged( carTileAt );
			}

			if( Config.EnablePostProcessingFx )
			{
				rb.dampStrength( 0.8f, Physics.dt );
				rb.setOrigin( Director.screenPosFor( player.getBody() ) );
			}
		}

		Debug.update();
	}

	// FIXME
	// shit, shouldn't this be subscribed instead, and not self-raised like
	// this?
	//

	protected void onTileChanged( Vector2 carAt )
	{
		boolean onStartZone = (carAt.x == 1 && carAt.y == 0);
		if( onStartZone )
		{
			if( isFirstLap )
			{
				isFirstLap = false;
				System.out.println("Recording began");
				recorder.beginRecording( player, replays[0] );
			} else
			{
				recorder.endRecording();

				// replay best, overwrite worst logic
				if(replays[0].isValid && !replays[1].isValid)
				{
					recorder.beginRecording( player, replays[1] );
					ghost.setReplay( replays[0] );
				}
				else
				if( replays[0].isValid && replays[1].isValid )
				{
					// both valid, replay best, overwrite worst
					Replay best = replays[1], worst = replays[0];
					if( replays[0].trackTimeSeconds < replays[1].trackTimeSeconds )
					{
						best = replays[0];
						worst = replays[1];
					}

					ghost.setReplay( best );
					recorder.beginRecording( player, worst );
				}
			}
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
		if(player!=null)
			Director.setPositionPx( player.state().position, false );

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

		if( Config.isDesktop ) Debug.renderB2dWorld( Director.getMatViewProjMt() );

		Debug.begin();
		EntityManager.raiseOnDebug();
		Debug.renderVersionInfo();
		Debug.renderGraphicalStats( Gdx.graphics.getWidth() - Debug.getStatsWidth(), Gdx.graphics.getHeight() - Debug.getStatsHeight() - Debug.fontHeight );
		if( Config.isDesktop ) Debug.renderMemoryUsage();
//		Debug.drawString( "EMgr::maxSpritesInBatch = " + EntityManager.maxSpritesInBatch(), 0, 6 );
//		Debug.drawString( "EMgr::renderCalls = " + EntityManager.renderCalls(), 0, 12 );
		Debug.end();
	}
}
