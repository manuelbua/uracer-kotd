package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Config.Physics;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.audio.CarSoundManager;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.effects.CarSkidMarks;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.events.GameLogicEvent.EventType;
import com.bitfire.uracer.events.GameLogicListener;
import com.bitfire.uracer.game.logic.DirectorController;
import com.bitfire.uracer.game.logic.Level;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.tiled.LevelRenderer;
import com.bitfire.uracer.utils.Convert;

public class Game implements Disposable, GameLogicListener {

	// config
	public GameplaySettings gameSettings = null;

	// logic
	private GameLogic gameLogic = null;
	private DirectorController controller = null;

	// post-processing
	private PostProcessor postProcessor = null;
	private Bloom bloom = null;
	private Bloom.Settings bloomSettings = null;
	private Zoom zoom = null;

	// drawing
	private SpriteBatch batch = null;

	// sounds
	private CarSoundManager carSoundManager = null;

	public Game( String levelName, GameDifficulty difficulty ) {
		GameData.create( difficulty );

		// everything has been setup on a 256px tile, scale back if that's the case
		Config.Physics.PixelsPerMeter /= (GameData.scalingStrategy.targetScreenRatio / GameData.scalingStrategy.to256);

		carSoundManager = new CarSoundManager();	// early load
		Art.scaleFonts( GameData.scalingStrategy.invTileMapZoomFactor );
		Messager.init();
		EntityManager.create();
		Convert.init();

		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();
		Director.init( width, height );


		// TODO, GameData should be consistent *BEFORE* constructing a Level
		GameData.level = new Level( GameData.world, levelName, false, width, height /* night mode */);
		GameData.playerState = GameData.level.getPlayerState();

		controller = new DirectorController( Config.Graphics.CameraInterpolationMode, Director.halfViewport, GameData.level );
		if( Config.Graphics.EnablePostProcessingFx ) {
			setupPostProcessing( width, height, GameData.level );
		}

		Car car = GameData.playerState.car;
		TrackEffects.init( car );
		GameData.hud = new Hud( car );

		// setup listeners
		gameLogic = new GameLogic();
		GameLogic.event.addListener( this );
		GameData.driftState.addListener( GameData.hud );
		GameData.playerState.addListener( gameLogic );
		GameData.playerState.car.addListener( gameLogic );
		GameData.playerState.car.addListener( carSoundManager );

		// Issues may arise on Tegra2 (Asus Transformer) devices if the buffers'
		// count is higher than 10
		batch = new SpriteBatch( 1000, 8 );
		System.out.println( "resolution=" + width + "x" + height + "px, physics=" + Physics.PhysicsTimestepHz + "Hz" );
	}

	@Override
	public void dispose() {
		Director.dispose();
		Messager.dispose();
		TrackEffects.dispose();
		batch.dispose();
		carSoundManager.dispose();

		if( Config.Graphics.EnablePostProcessingFx ) {
			postProcessor.dispose();
		}
	}

	@Override
	public void gameLogicEvent( EventType type ) {
		switch( type ) {
		case OnRestart:
		case OnReset:
			carSoundManager.reset();
			break;
		}
	}

	private void setupPostProcessing( int width, int height, Level level ) {
		postProcessor = new PostProcessor( width, height, false /* depth */, false /* alpha */, Config.isDesktop /* 32
																												 * bits */);
		bloom = new Bloom( postProcessor, Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight );

		// Bloom.Settings bs = new Bloom.Settings( "arrogance-1 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.25f, 1f, 0.1f, 0.8f, 1.4f );
		// Bloom.Settings bs = new Bloom.Settings( "arrogance-2 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.35f, 1f, 0.1f, 1.4f, 0.75f );

		float threshold = ((level.isNightMode() && !Config.Graphics.DumbNightMode) ? 0.2f : 0.45f);
		bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
		bloom.setSettings( bloomSettings );

		zoom = new Zoom( postProcessor, Config.PostProcessing.ZoomQuality );
		postProcessor.addEffect( zoom );

		postProcessor.addEffect( bloom );
	}

	// private float prevFactor = 0;
	public boolean tick() {
		if( !gameLogic.onTick() )
			return false;

		carSoundManager.tick();

		// post-processor debug ------------------------------
		// float factor = player.currSpeedFactor * 1.75f;
		// float factor = GameData.driftState.driftStrength * 2;
		float factor = 1 - (URacer.timeMultiplier - 0.3f) / (Config.Physics.PhysicsTimeMultiplier - 0.3f);

		// factor = AMath.clamp(AMath.lerp( prevFactor, factor, 0.15f ),0,2);
		// prevFactor = factor;
		//
		// if( Config.Graphics.EnablePostProcessingFx && bloom != null ) {
		// bloom.setBaseSaturation( bloomSettings.baseSaturation + 0.5f * (1-factor) );
		// bloom.setBloomIntesity( bloomSettings.bloomIntensity * factor );
		// }

		if( Config.Graphics.EnablePostProcessingFx && zoom != null ) {
			zoom.setOrigin( Director.screenPosFor( GameData.playerState.car.getBody() ) );
			zoom.setStrength( -0.05f * factor );
		}

		if( Config.Graphics.EnablePostProcessingFx && bloom != null && zoom != null ) {
			bloom.setBaseSaturation( 0.5f - 0.5f * factor );
			bloom.setBloomSaturation( 1.5f - factor * 1.15f );
			bloom.setBloomIntesity( 1f + factor * 1.75f );
		}

		// if( Config.Graphics.EnablePostProcessingFx && bloom != null && zoom != null ) {
		// bloom.setBaseSaturation( 0.5f - 0.8f * factor );
		// bloom.setBloomIntesity( 1.0f + 0.25f * factor );
		// bloom.setBloomSaturation( 1.5f + ((level.isNightMode() && !Config.Graphics.DumbNightMode) ? -0.5f : 1.5f) *
		// factor );
		// }
		// ---------------------------------------------------

		Debug.tick();
		return true;
	}

	public void render() {
		Level level = GameData.level;
		GameData.tweener.update( (int)(URacer.getLastDeltaSecs() * 1000) );

		Car playerCar = null;
		GL20 gl = Gdx.graphics.getGL20();
		OrthographicCamera ortho = Director.getCamera();

		// Entity's state() is transformed into pixel space
		EntityManager.raiseOnBeforeRender( URacer.getTemporalAliasing() );

		// follow the car
		if( GameData.playerState != null ) {
			playerCar = GameData.playerState.car;
			controller.setPosition( playerCar.state().position );
		}

		if( Config.Graphics.EnablePostProcessingFx ) {
			postProcessor.capture();
		}

		gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		// resync
		level.syncWithCam( ortho );

		// clear buffers
		// TODO could be more sensible since while post-processing there is already a glClear
		// going on..
		gl.glClearDepthf( 1 );
		gl.glClearColor( 0, 0, 0, 0 );
		gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );

		// render base tilemap
		level.renderTilemap( gl );

		gl.glActiveTexture( GL20.GL_TEXTURE0 );
		batch.setProjectionMatrix( ortho.projection );
		batch.setTransformMatrix( ortho.view );
		batch.begin();
		{
			// batch render effects
			TrackEffects.render( batch );

			// batch render entities
			EntityManager.raiseOnRender( batch, URacer.getTemporalAliasing() );
		}
		batch.end();

		// render 3d meshes
		level.renderMeshes( gl );

		GameData.hud.render( batch );

		if( level.isNightMode() ) {
			if( Config.Graphics.DumbNightMode ) {
				if( Config.Graphics.EnablePostProcessingFx )
					postProcessor.render();
				level.generateLightMap();
				level.renderLigthMap( null );
			} else {
				// render nightmode
				if( level.isNightMode() ) {
					level.generateLightMap();
					// hook into the next PostProcessor source buffer (the last result)
					// and blend the lightmap on it
					if( Config.Graphics.EnablePostProcessingFx )
						level.renderLigthMap( postProcessor.captureEnd() );
					else
						level.renderLigthMap( null );
				}

				if( Config.Graphics.EnablePostProcessingFx )
					postProcessor.render();
			}
		} else {
			if( Config.Graphics.EnablePostProcessingFx )
				postProcessor.render();
		}

		//
		// debug
		//

		if( Config.isDesktop ) {
			if( Config.Graphics.RenderBox2DWorldWireframe )
				Debug.renderB2dWorld( GameData.world, Director.getMatViewProjMt() );

			Debug.begin( batch );
			EntityManager.raiseOnDebug();
			if( Config.Graphics.RenderHudDebugInfo )
				GameData.hud.debug( batch );
			Debug.renderVersionInfo();
			Debug.renderGraphicalStats( Gdx.graphics.getWidth() - Debug.getStatsWidth(), Gdx.graphics.getHeight() - Debug.getStatsHeight() - Debug.fontHeight );
			Debug.renderTextualStats();
			Debug.renderMemoryUsage();
			Debug.drawString( "Visible car skid marks=" + ((CarSkidMarks)TrackEffects.get( TrackEffects.Effects.CarSkidMarks )).getParticleCount(), 0,
					Gdx.graphics.getHeight() - 21 );
			Debug.drawString( "total meshes=" + Level.totalMeshes, 0, Gdx.graphics.getHeight() - 14 );
			Debug.drawString( "rendered meshes=" + (LevelRenderer.renderedTrees + LevelRenderer.renderedWalls) + ", trees=" + LevelRenderer.renderedTrees + ", walls="
					+ LevelRenderer.renderedWalls + ", culled=" + LevelRenderer.culledMeshes, 0, Gdx.graphics.getHeight() - 7 );
			Debug.end();
		} else {
			Debug.begin( batch );
			Debug.renderVersionInfo();
			Debug.renderTextualStats();
			Debug.end();
		}
	}

	public void pause() {
	}

	public void resume() {
		postProcessor.rebind();
	}
}