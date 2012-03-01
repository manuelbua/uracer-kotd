package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.audio.CarSoundManager;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.effects.CarSkidMarks;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.effects.postprocessing.PostProcessor;
import com.bitfire.uracer.effects.postprocessing.bloom.Bloom;
import com.bitfire.uracer.effects.postprocessing.bloom.BloomSettings;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DirectorController;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.logic.Level;
import com.bitfire.uracer.game.logic.Player;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.hud.HudLabel;
import com.bitfire.uracer.messager.Message;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.tiled.LevelRenderer;
import com.bitfire.uracer.tweener.Tweener;
import com.bitfire.uracer.tweener.accessors.HudLabelAccessor;
import com.bitfire.uracer.tweener.accessors.MessageAccessor;

public class Game
{
	private Level level = null;
	private Player player = null;
	private Hud hud = null;

	private static Tweener tweener = null;

	// config
	public GameplaySettings gameSettings;

	// logic
	private GameLogic logic = null;
	private DirectorController controller;

	// effects
	private PostProcessor postProcessor;
	private Bloom bloom;

	// drawing
	private SpriteBatch batch = null;

	public Game( String levelName, GameDifficulty difficulty )
	{
//		if(!Config.isDesktop)
//			Config.Graphics.EnablePostProcessingFx = false;

		Game.tweener = createTweener();

		Messager.init();
		gameSettings = GameplaySettings.create( difficulty );
		Director.create( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		Art.scaleFonts( Director.scalingStrategy.invTileMapZoomFactor );

		// bring up level
		level = Director.loadLevel( levelName, gameSettings, false /* night mode */ );
		player = level.getPlayer();

		logic = new GameLogic( this );
		hud = new Hud( this );
		logic.create();

		controller = new DirectorController( Config.Graphics.CameraInterpolationMode );

		// track effects
		TrackEffects.init( logic );

		// audio effects
		CarSoundManager.setPlayer( player );

		if( Config.Graphics.EnablePostProcessingFx )
		{
			postProcessor = new PostProcessor( Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false /* depth */, false /* alpha */, true /* 32Bits */ );

			float rttRatio = 0.5f;
			int blurPasses = 4;

//			if(!Config.isDesktop)
//			{
//				rttRatio = 0.25f;
//				blurPasses = 2;
//			}

			int fboWidth = (int)(Gdx.graphics.getWidth() * rttRatio);
			int fboHeight = (int)(Gdx.graphics.getHeight() * rttRatio);
			bloom = new Bloom( fboWidth, fboHeight, postProcessor.getFramebufferFormat() );

			// this is some nice graphic expression for "arrogance mode"
//			BloomSettings bs = new BloomSettings( "arrogance", blurPasses, 0.35f, 1f, 0.1f, 1.4f, 0.75f );
//			BloomSettings bs = new BloomSettings( "default", blurPasses, 0.35f, 1f, 0.3f, 1.3f, 1.5f );
			BloomSettings bs = new BloomSettings( "soft", 2, 0.25f, 1f, 0.3f, 1f, 1.4f );
//			BloomSettings bs = new BloomSettings( "soft", blurPasses, 0f, 1f, 1f, 1f, 1f );
//			BloomSettings bs = new BloomSettings( "desaturated", 2, 0.5f, 1f, 1f, 2f, 0f );
//			BloomSettings bs = new BloomSettings( "saturated", 3, 0.25f, 1f, 0f, 2f, 2f );
//			BloomSettings bs = new BloomSettings( "blurry", 2, 0f, 0.1f, 1f, 1f, 1f );
//			BloomSettings bs = new BloomSettings( "subtle", 2, 0.5f, 1f, 1f, 1f, 1f );

			bloom.setSettings( bs );
			postProcessor.setEffect( bloom );
		}

		// setup sprite batch at origin top-left => 0,0
		// Issues may arise on Tegra2 (Asus Transformer) devices if the buffers'
		// count is higher than 10
		batch = new SpriteBatch( 1000, 8 );
	}

	public void dispose()
	{
		Director.dispose();
		Messager.dispose();
		logic.dispose();
		hud.dispose();
		TrackEffects.dispose();
		batch.dispose();

		if(Config.Graphics.EnablePostProcessingFx)
		{
			postProcessor.dispose();
		}
	}

	private Tweener createTweener()
	{
		Tweener t = new Tweener();
		Tweener.registerAccessor( Message.class, new MessageAccessor() );
		Tweener.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		return t;
	}

	public void tick()
	{
		logic.tick();
		hud.tick();
		TrackEffects.tick();
		CarSoundManager.tick();

//		if( Config.Graphics.EnablePostProcessingFx )
//		{
//			radialBlur.dampStrength( 0.8f );
//			radialBlur.setOrigin( Director.screenPosFor( level.getPlayer().car.getBody() ) );
//		}

		Debug.update();
	}

	public void render()
	{
		Car playerCar = null;
		GL20 gl = Gdx.graphics.getGL20();
		OrthographicCamera ortho = Director.getCamera();

		// Entity's state() is transformed into pixel space
		EntityManager.raiseOnBeforeRender( URacer.getTemporalAliasing() );

		// follow the car
		if( player != null )
		{
			playerCar = player.car;
			controller.setPosition( playerCar.state().position );
		}

		if( Config.Graphics.EnablePostProcessingFx )
		{
//			bloom.setThresholdType( ThresholdType.Saturate );
//			bloom.setBloomMixing( BloomMixing.WeightedAverage );
//			bloom.setBloomIntesity( 2f );
//			bloom.setOriginalIntesity( 1f );
//			bloom.setTreshold( 0.4f );
//			bloom.setBlending( true );
//			bloom.setClearColor( 1f, .3f, .3f, 0 );
//			bloom.blurPasses = 4;


			// dbg (hotcode)
//			bloom.setBaseIntesity( 1f );	bloom.setBaseSaturation( .3f );
//			bloom.setBloomIntesity( 1f );		bloom.setBloomSaturation( 1.4f );
//			bloom.setThreshold( 0.25f );
//			bloom.setBlurPasses( 2 );

			postProcessor.capture();
		}

		gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		// resync
		level.syncWithCam( ortho );

		// prepare sprite batch
		batch.setProjectionMatrix( ortho.projection );
		batch.setTransformMatrix( ortho.view );

		// clear buffers
		gl.glClearDepthf( 1 );
		gl.glClearColor( 0, 0, 0, 0 );
		gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );

		// render base tilemap
		level.renderTilemap(gl);

		gl.glActiveTexture( GL20.GL_TEXTURE0 );
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

		if( Config.Graphics.EnablePostProcessingFx )
		{
			postProcessor.render();
		}

		// lights
		if( level.isNightMode() )
		{
			level.renderLights();
		}

		tweener.update((int)(URacer.getLastDeltaSecs()*1000));
		hud.render(batch);

		//
		// debug
		//

		if( Config.isDesktop )
		{
			if( Config.Graphics.RenderBox2DWorldWireframe )
				Debug.renderB2dWorld( Director.getMatViewProjMt() );

			Debug.begin( batch );
			EntityManager.raiseOnDebug();
			if( Config.Graphics.RenderHudDebugInfo ) hud.debug( batch );
			Debug.renderVersionInfo();
			Debug.renderGraphicalStats( Gdx.graphics.getWidth() - Debug.getStatsWidth(),Gdx.graphics.getHeight() - Debug.getStatsHeight() - Debug.fontHeight );
			Debug.renderTextualStats();
			Debug.renderMemoryUsage();
			Debug.drawString( "Visible car skid marks=" + ((CarSkidMarks)TrackEffects.get(TrackEffects.Effects.CarSkidMarks)).getParticleCount(), 0, Gdx.graphics.getHeight()-21 );
			Debug.drawString( "total meshes=" + Level.totalMeshes, 0, Gdx.graphics.getHeight()-14 );
			Debug.drawString( "rendered meshes=" + (LevelRenderer.renderedTrees + LevelRenderer.renderedWalls)
					+ ", trees=" + LevelRenderer.renderedTrees
					+ ", walls=" + LevelRenderer.renderedWalls
					+ ", culled=" + LevelRenderer.culledMeshes, 0, Gdx.graphics.getHeight()-7 );
			Debug.end();
		} else
		{
			Debug.begin( batch );
			Debug.renderVersionInfo();
			Debug.renderTextualStats();
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

	public static Tweener getTweener()
	{
		return tweener;
	}

	public void restart()
	{
		Messager.reset();
		level.reset();
		logic.restart();

		TrackEffects.reset();
	}

	public void reset()
	{
		Messager.reset();
		level.reset();
		logic.reset();

		TrackEffects.reset();
	}
}
