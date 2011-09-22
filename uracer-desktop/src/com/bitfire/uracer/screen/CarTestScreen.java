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
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.hud.Messager;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.RadialBlur;
import com.bitfire.uracer.simulations.car.Replay;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.utils.Convert;

public class CarTestScreen extends Screen
{
	private Car player = null;
	private GhostCar ghost = null;
	private Level level;

	private RadialBlur rb;

//	private Recorder recorder;
	private Replay[] replays;
	private boolean isFirstLap = true;

	public CarTestScreen()
	{
//		recorder = Recorder.create();
//		EntityManager.create();
//		ModelFactory.init();

		Director.create( this, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		GameplaySettings gs = GameplaySettings.create( GameDifficulty.Easy );
		level = Director.loadLevel( "level1", gs );
		Director.setPositionPx( Director.positionFor( new Vector2(0,0)), false );

		player = level.getPlayer();
		ghost = level.getGhost();

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
				level.reset();
				Messager.reset();

				replays[0].clearForces();
				replays[1].clearForces();
				lastLapId = 0;

				lastCarTileAt.set( -1, -1 );
				carTileAt.set( lastCarTileAt );
				isFirstLap = true;
			}
		}

		level.tick();

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
		Messager.update();
	}

	// FIXME
	// shit, shouldn't this be subscribed instead, and not self-raised like
	// this?
	//

	private long lastLapId = 0;
	protected void onTileChanged( Vector2 carAt )
	{
		boolean onStartZone = (carAt.x == 1 && carAt.y == 0);
		if( onStartZone )
		{
			if( isFirstLap )
			{
				isFirstLap = false;
				level.beginRecording( replays[0] );
				lastLapId = replays[0].id;

				Messager.add( "WARM  UP  LAP", 5f );
			} else
			{
				level.endRecording();

				// replay best, overwrite worst logic

				if(replays[0].isValid && !replays[1].isValid)
				{
					// only one single replay
					level.beginRecording( replays[1] );
					lastLapId = replays[1].id;

					ghost.setReplay( replays[0] );
					Messager.add( "GO!  GO!  GO!", 5f );
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

					if(lastLapId == best.id)
					{
						Messager.add( "THAT   WAS   FAST!\n -" + String.format("%.2f", worst.trackTimeSeconds-best.trackTimeSeconds) + " seconds!", 5f );
					}
					else
					{
						Messager.add( "YOU   LOST!\nYou were " + String.format("%.2f",worst.trackTimeSeconds-best.trackTimeSeconds) + " seconds slower!", 5f );
					}

					ghost.setReplay( best );
					level.beginRecording( worst );

					lastLapId = worst.id;
				}
			}
		}
	}


	@Override
	public void render()
	{
		GL20 gl = Gdx.graphics.getGL20();
		EntityManager.raiseOnBeforeRender( URacer.getTemporalAliasing() );

		// follow the car
		if(player!=null)
		{
			// we'll do here since we could have the interpolated position
			Director.setPositionPx( player.state().position, false );
		}

		if( Config.EnablePostProcessingFx )
		{
			PostProcessor.begin();
			level.render();
			PostProcessor.end();
		} else
		{
			gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
			level.render();
		}

		Messager.render();

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
