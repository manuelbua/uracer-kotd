package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.effects.postprocessing.PostProcessor;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DirectorController;
import com.bitfire.uracer.game.logic.DirectorController.InterpolationMode;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.tiled.Level;

public class Game
{
	private Level level = null;
	private Car player = null;
	private Hud hud = null;

	// config
	public final GameplaySettings gameSettings;

	// logic
	private GameLogic logic = null;
	private DirectorController controller;

	// drawing
	private SpriteBatch batch = null;

	public Game( GameDifficulty difficulty )
	{
		Messager.init();
		gameSettings = GameplaySettings.create( difficulty );
		Director.create( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		Art.scaleFonts( Director.scalingStrategy.invTileMapZoomFactor );
		level = Director.loadLevel( "tutorial-2", gameSettings );
		player = level.getPlayer();

		logic = new GameLogic( this );
		hud = new Hud( logic );

		TrackEffects.init( logic );
		controller = new DirectorController( InterpolationMode.Sigmoid );

		// setup sprite batch at origin top-left => 0,0
		batch = new SpriteBatch( 1000, 10 /* higher values causes issues on Tegra2 (Asus Transformer)*/);
	}

	public void dispose()
	{
		Director.dispose();
		Messager.dispose();
		logic.dispose();
		hud.dispose();
		TrackEffects.dispose();
		batch.dispose();
	}

	public void tick()
	{
		logic.tick();
		hud.tick();
		TrackEffects.tick();

		Debug.update();
	}

	public void render()
	{
		// TODO this should belong to GameLogic..
		GL20 gl = Gdx.graphics.getGL20();
		OrthographicCamera ortho = Director.getCamera();

		EntityManager.raiseOnBeforeRender( URacer.getTemporalAliasing() );

		// follow the car
		if( player != null )
		{
			controller.setPosition( player.state().position );
		}

		if( Config.EnablePostProcessingFx )
		{
			PostProcessor.begin();
		} else
		{
			gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		}

		{
			// resync
			level.syncWithCam( ortho );

			// prepare sprite batch

			batch.setProjectionMatrix( ortho.projection );
			batch.setTransformMatrix( ortho.view );

			gl.glClearDepthf( 1 );
			gl.glClearColor( 0, 0, 0, 1 );
			gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );

			level.renderTilemap();

			gl.glDepthMask( false );
			batch.begin();
			TrackEffects.renderPlayerSkidMarks( batch );
			EntityManager.raiseOnRender( batch, URacer.getTemporalAliasing() );
			batch.end();

			level.renderMeshes( gl );
		}

		if( Config.EnablePostProcessingFx )
		{
			PostProcessor.end();
		}

		logic.render();
		hud.render(batch);

		//
		// debug
		//

		if( Config.isDesktop )
		{
			Debug.renderB2dWorld( Director.getMatViewProjMt() );

			Debug.begin( batch );
			EntityManager.raiseOnDebug();
			hud.debug();
			Debug.renderVersionInfo();
			Debug.renderGraphicalStats( Gdx.graphics.getWidth() - Debug.getStatsWidth(),
					Gdx.graphics.getHeight() - Debug.getStatsHeight() - Debug.fontHeight );
			Debug.renderMemoryUsage();
			Debug.end();
		} else
		{
			Debug.begin( batch );
			Debug.renderGraphicalStats( Gdx.graphics.getWidth() - Debug.getStatsWidth(),
					Gdx.graphics.getHeight() - Debug.getStatsHeight() - Debug.fontHeight );
			Debug.end();
		}
	}

	public Level getLevel()
	{
		return level;
	}

	public void restart()
	{
		Messager.reset();
		level.restart();
		logic.restart();

		TrackEffects.reset();
	}

	public void reset()
	{
		Messager.reset();
		level.restart();
		logic.reset();

		TrackEffects.reset();
	}
}
