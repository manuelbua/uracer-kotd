package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.logic.Level;
import com.bitfire.uracer.game.logic.Player;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.hud.HudLabel;
import com.bitfire.uracer.messager.Message;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.postprocessing.filters.Blur.BlurType;
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
	private Bloom bloom = null;
	private Zoom zblur = null;

	// drawing
	private SpriteBatch batch = null;

	public Game( String levelName, GameDifficulty difficulty )
	{
//		if(!Config.isDesktop)
//			Config.Graphics.EnablePostProcessingFx = false;

		System.out.println("resolution=" + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight() + "px, physics=" + Physics.PhysicsTimestepHz + "Hz");
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
		CarSoundManager.load();

		controller = new DirectorController( Config.Graphics.CameraInterpolationMode );

		// track effects
		TrackEffects.init( logic );

		// audio effects
		CarSoundManager.setPlayer( player );

		if( Config.Graphics.EnablePostProcessingFx )
		{
			int fboWidth = Gdx.graphics.getWidth();
			int fboHeight = Gdx.graphics.getHeight();

			postProcessor = new PostProcessor( fboWidth, fboHeight, false /* depth */, false /* alpha */, Config.isDesktop /* 32 bits */ );

			float rttRatio = 0.25f;

			System.out.println("rttRatio=" + rttRatio);

			bloom = new Bloom( (int)(fboWidth * rttRatio), (int)(fboHeight * rttRatio), postProcessor.getFramebufferFormat() );

//			BloomSettings bs = new BloomSettings( "arrogance-1 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1, 0.25f, 1f, 0.1f, 0.8f, 1.4f );
//			BloomSettings bs = new BloomSettings( "arrogance-2 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1, 0.35f, 1f, 0.1f, 1.4f, 0.75f );
//			BloomSettings bs = new BloomSettings( "subtle / rtt=0.25 / @800x480/1280x800", BlurType.Gaussian5x5, 1, 1.5f, 0.45f, 1f, 0.5f, 1f, 1.5f );
//			BloomSettings bs = new BloomSettings( "subtle / rtt=0.2  / @800x480/1280x800", BlurType.Gaussian3x3b, 1, 1.5f, 0.45f, 1f, 0.5f, 1f, 1.5f );

			Bloom.Settings bs = new Bloom.Settings( "subtle / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1f, 0.45f, 1f, 0.5f, 1f, 1.5f );
			bloom.setSettings( bs );

			// ------
			zblur = new Zoom();

			postProcessor.addEffect( zblur );
			postProcessor.addEffect( bloom );
			// ------
		}

//		Messager.show( "FUCK! BERLU! SCONI!", 600, MessageType.Good, MessagePosition.Bottom, MessageSize.Big );

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
		CarSoundManager.dispose();


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

	public boolean tick()
	{
		if(!logic.tick())
			return false;

		hud.tick();
		TrackEffects.tick();
		CarSoundManager.tick();

		if( Config.Graphics.EnablePostProcessingFx && zblur != null )
		{
			zblur.setOrigin( Director.screenPosFor( player.car.getBody() ) );
			zblur.setStrength( player.currSpeedFactor*0.2f );
//			zblur.setStrength(1);
		}

		Debug.update();
		return true;
	}

//	private float lastFactor = 0f;
	public void render()
	{
		tweener.update((int)(URacer.getLastDeltaSecs()*1000));

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
//			zblur.setStrength( 1 );
//			if(postProcessor.effects.get( 0 ).name.equals( "Bloom" ))
//			{
//				PostProcessorEffect a = postProcessor.effects.get( 0 );
//				PostProcessorEffect b = postProcessor.effects.get( 1 );
//				postProcessor.effects.clear();
//				postProcessor.effects.add( b );
//				postProcessor.effects.add( a );
//			}

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

		hud.render(batch);

		if( Config.Graphics.EnablePostProcessingFx )
		{
			postProcessor.render();
		}

		// lights
		if( level.isNightMode() )
		{
			level.renderLights();
		}

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
