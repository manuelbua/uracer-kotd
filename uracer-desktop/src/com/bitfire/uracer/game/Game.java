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
import com.bitfire.uracer.effects.TrackEffects.Effects;
import com.bitfire.uracer.effects.postprocessing.PostProcessor;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DirectorController;
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
	public GameplaySettings gameSettings;

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
		level = Director.loadLevel( "level1", gameSettings );
		player = level.getPlayer();

		logic = new GameLogic( this );
		hud = new Hud( logic );
		logic.create();

		controller = new DirectorController( Config.cameraInterpolationMode );

		// track effects
		TrackEffects.init( logic );

		// setup sprite batch at origin top-left => 0,0
		// Issues may arise on Tegra2 (Asus Transformer) devices if the buffers'
		// count is higher than 10
		batch = new SpriteBatch( 1000, 10 );
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
			TrackEffects.renderEffect( Effects.CarSkidMarks, batch );
			TrackEffects.renderEffect( Effects.SmokeTrails, batch );
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
//			Debug.renderB2dWorld( Director.getMatViewProjMt() );

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

	public Hud getHud()
	{
		return hud;
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
