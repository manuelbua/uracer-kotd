package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.effects.postprocessing.PostProcessor;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
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

		TrackEffects.init( logic );
	}

	public void dispose()
	{
		Director.dispose();
		Messager.dispose();
		logic.dispose();
		hud.dispose();
		TrackEffects.dispose();
	}

	public void tick()
	{
		EntityManager.raiseOnTick();
		logic.tick();
		hud.tick();
		TrackEffects.tick();

		Debug.update();
	}

	public void render()
	{
		GL20 gl = Gdx.graphics.getGL20();

		EntityManager.raiseOnBeforeRender( URacer.getTemporalAliasing() );

		// follow the car
		if( player != null )
		{
			// we'll do here since we could have the interpolated position
			Director.setPositionPx( player.state().position, false );
		}

		if( Config.EnablePostProcessingFx )
		{
			PostProcessor.begin();
		} else
		{
			gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		}

		gl.glClearDepthf( 1 );
		gl.glClearColor( 0, 0, 0, 1 );
		gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );

		level.syncWithCam( Director.getCamera() );
		level.renderTilemap();
		TrackEffects.render();
		EntityManager.raiseOnRender( URacer.getTemporalAliasing() );
		level.renderMeshes( gl );

		if( Config.EnablePostProcessingFx )
		{
			PostProcessor.end();
		}

		logic.render();
		hud.render();

		//
		// debug
		//

		if( Config.isDesktop )
		{
			Debug.renderB2dWorld( Director.getMatViewProjMt() );

			Debug.begin();
			EntityManager.raiseOnDebug();
			hud.debug();
			Debug.renderVersionInfo();
			Debug.renderGraphicalStats( Gdx.graphics.getWidth() - Debug.getStatsWidth(),
					Gdx.graphics.getHeight() - Debug.getStatsHeight() - Debug.fontHeight );
			if( Config.isDesktop ) Debug.renderMemoryUsage();
			// Debug.drawString( "EMgr::maxSpritesInBatch = " +
			// EntityManager.maxSpritesInBatch(), 0, 6 );
			// Debug.drawString( "EMgr::renderCalls = " +
			// EntityManager.renderCalls(), 0, 12 );
			Debug.end();
		} else
		{
			Debug.begin();
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
