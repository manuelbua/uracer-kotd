package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
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
import com.bitfire.uracer.game.logic.DirectorController;
import com.bitfire.uracer.game.logic.GameContactListener;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.Level;
import com.bitfire.uracer.game.logic.Player;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.hud.HudLabel;
import com.bitfire.uracer.messager.Message;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.tiled.LevelRenderer;
import com.bitfire.uracer.tweener.Tweener;
import com.bitfire.uracer.tweener.accessors.HudLabelAccessor;
import com.bitfire.uracer.tweener.accessors.MessageAccessor;

/** TODO most of the shared stuff between Game and GameLogic should go in a
 * GameData structure of some sort, GameLogic is really the logical portion of
 * Game, so data should be accessible for both.
 *
 * @author bmanuel */
public class Game implements Disposable {
	protected World world = null;
	private Level level = null;
	private Player player = null;
	private Hud hud = null;
	private LapState lapState = null;

	private static Tweener tweener = null;

	// config
	public GameplaySettings gameSettings;

	// logic
	private GameLogic logic = null;
	private DirectorController controller;

	// post-processing
	private PostProcessor postProcessor;
	private Bloom bloom = null;
	private Bloom.Settings bloomSettings = null;
	private Zoom zoom = null;

	// drawing
	private SpriteBatch batch = null;

	public Game( String levelName, GameDifficulty difficulty ) {
		Game.tweener = createTweener();

		world = new World( new Vector2( 0, 0 ), false );
		world.setContactListener( new GameContactListener() );

		lapState = new LapState();

		Messager.init();
		gameSettings = GameplaySettings.create( difficulty );

		Director.create( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		Art.scaleFonts( Director.scalingStrategy.invTileMapZoomFactor );

		level = Director.loadLevel( world, levelName, gameSettings, false /* night mode */);
		player = level.getPlayer();

		logic = new GameLogic( this, lapState );
		hud = new Hud( this );
		logic.create();
		CarSoundManager.load();

		controller = new DirectorController( Config.Graphics.CameraInterpolationMode );

		// track effects
		TrackEffects.init( player.car );

		// audio effects
		CarSoundManager.setPlayer( player );

		if( Config.Graphics.EnablePostProcessingFx ) {
			setupPostProcessing();
		}

		// Messager.show( "FUCK! BERLU! SCONI!", 600, MessageType.Good, MessagePosition.Bottom, MessageSize.Big );

		// Issues may arise on Tegra2 (Asus Transformer) devices if the buffers'
		// count is higher than 10
		batch = new SpriteBatch( 1000, 8 );

		System.out.println( "resolution=" + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight() + "px, physics=" + Physics.PhysicsTimestepHz + "Hz" );
	}

	@Override
	public void dispose() {
		Director.dispose();
		Messager.dispose();
		logic.dispose();
		hud.dispose();
		TrackEffects.dispose();
		batch.dispose();
		CarSoundManager.dispose();
		world.dispose();

		if( Config.Graphics.EnablePostProcessingFx ) {
			postProcessor.dispose();
		}
	}

	private Tweener createTweener() {
		Tweener t = new Tweener();
		Tweener.registerAccessor( Message.class, new MessageAccessor() );
		Tweener.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		return t;
	}

	private void setupPostProcessing() {
		postProcessor = new PostProcessor( Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false /* depth */, false /* alpha */, Config.isDesktop /* 32
																																					 * bits */);
		bloom = new Bloom( Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight, postProcessor.getFramebufferFormat() );
		// bloom = new Bloom( Config.PostProcessing.SmallFboWidth, Config.PostProcessing.SmallFboHeight,
		// postProcessor.getFramebufferFormat() );

		// Bloom.Settings bs = new Bloom.Settings( "arrogance-1 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.25f, 1f, 0.1f, 0.8f, 1.4f );
		// Bloom.Settings bs = new Bloom.Settings( "arrogance-2 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.35f, 1f, 0.1f, 1.4f, 0.75f );

		float threshold = ((level.isNightMode() && !Config.Graphics.DumbNightMode) ? 0.2f : 0.45f);
		bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
		bloom.setSettings( bloomSettings );

//		 zoom = new Zoom( Config.PostProcessing.ZoomQuality );

//		 postProcessor.addEffect( zoom );
		postProcessor.addEffect( bloom );
	}

//	private float prevFactor = 0;
	public boolean tick() {
		if( !logic.tick() ) return false;

		hud.tick(lapState);
		TrackEffects.tick();
		CarSoundManager.tick();

		// post-processor debug ------------------------------
//		float factor = player.currSpeedFactor * 1.75f;
//		float factor = DriftInfo.get().driftStrength * 2;
//		factor = 1.8f;

//		factor = AMath.clamp(AMath.lerp( prevFactor, factor, 0.15f ),0,2);
//		prevFactor = factor;
//
//		if( Config.Graphics.EnablePostProcessingFx && bloom != null ) {
//			bloom.setBaseSaturation( bloomSettings.baseSaturation + 0.5f * (1-factor) );
//			bloom.setBloomIntesity( bloomSettings.bloomIntensity * factor );
//		}
//
//		if( Config.Graphics.EnablePostProcessingFx && zoom != null ) {
//			zoom.setOrigin( Director.screenPosFor( player.car.getBody() ) );
//			zoom.setStrength( -0.01f * factor );
//		}

//		if( Config.Graphics.EnablePostProcessingFx && bloom != null && zoom != null ) {
//			bloom.setBaseSaturation( 0.5f - 0.8f * factor );
//			bloom.setBloomIntesity( 1.0f + 0.25f * factor );
//			bloom.setBloomSaturation( 1.5f + ((level.isNightMode() && !Config.Graphics.DumbNightMode) ? -0.5f : 1.5f) * factor );
		// }
		// ---------------------------------------------------

		Debug.update();
		return true;
	}

	public void render() {
		tweener.update( (int)(URacer.getLastDeltaSecs() * 1000) );

		Car playerCar = null;
		GL20 gl = Gdx.graphics.getGL20();
		OrthographicCamera ortho = Director.getCamera();

		// Entity's state() is transformed into pixel space
		EntityManager.raiseOnBeforeRender( URacer.getTemporalAliasing() );

		// follow the car
		if( player != null ) {
			playerCar = player.car;
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

		hud.render( batch );

		if( level.isNightMode() ) {
			if( Config.Graphics.DumbNightMode ) {
				if( Config.Graphics.EnablePostProcessingFx ) postProcessor.render();
				level.generateLightMap();
				level.renderLigthMap( null );
			}
			else {
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

				if( Config.Graphics.EnablePostProcessingFx ) postProcessor.render();
			}
		}
		else {
			if( Config.Graphics.EnablePostProcessingFx ) postProcessor.render();
		}

		//
		// debug
		//

		if( Config.isDesktop ) {
			if( Config.Graphics.RenderBox2DWorldWireframe ) Debug.renderB2dWorld( world, Director.getMatViewProjMt() );

			Debug.begin( batch );
			EntityManager.raiseOnDebug();
			if( Config.Graphics.RenderHudDebugInfo ) hud.debug( batch );
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
		}
		else {
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

	public Level getLevel() {
		return level;
	}

	public Hud getHud() {
		return hud;
	}

	public static Tweener getTweener() {
		return tweener;
	}

	public void restart() {
		Messager.reset();
		level.reset();
		logic.restart();

		TrackEffects.reset();
	}

	public void reset() {
		Messager.reset();
		level.reset();
		logic.reset();

		TrackEffects.reset();
	}
}