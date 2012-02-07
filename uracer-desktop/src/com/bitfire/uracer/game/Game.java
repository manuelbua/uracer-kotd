package com.bitfire.uracer.game;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.audio.CarSoundManager;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.effects.TrackEffects.Effects;
import com.bitfire.uracer.effects.postprocessing.PostProcessor;
import com.bitfire.uracer.entities.CollisionFilters;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DirectorController;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.hud.HudLabel;
import com.bitfire.uracer.messager.Message;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.tweener.Tweener;
import com.bitfire.uracer.tweener.accessors.HudLabelAccessor;
import com.bitfire.uracer.tweener.accessors.MessageAccessor;
import com.bitfire.uracer.utils.Convert;

public class Game
{
	private Level level = null;
	private Car player = null;
	private Hud hud = null;

	private static Tweener tweener = null;

	// config
	public GameplaySettings gameSettings;

	// logic
	private GameLogic logic = null;
	private DirectorController controller;

	// ray handling
	private RayHandler rayHandler;
	private ConeLight playerLight;
	private PointLight[] levelLights = new PointLight[10];

	// drawing
	private SpriteBatch batch = null;

	public Game( GameDifficulty difficulty )
	{
		createTweener();

		Messager.init();
		gameSettings = GameplaySettings.create( difficulty );
		Director.create( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		Art.scaleFonts( Director.scalingStrategy.invTileMapZoomFactor );

		// bring up level
		level = Director.loadLevel( "level1", gameSettings );
		player = level.getPlayer();

		logic = new GameLogic( this );
		hud = new Hud( this );
		logic.create();

		controller = new DirectorController( Config.Graphics.CameraInterpolationMode );

		// track effects
		TrackEffects.init( logic );

		// audio effects
		CarSoundManager.setPlayer( player );

		// setup sprite batch at origin top-left => 0,0
		// Issues may arise on Tegra2 (Asus Transformer) devices if the buffers'
		// count is higher than 10
		batch = new SpriteBatch( 1000, 8 );

		if( Config.Graphics.NightMode )
		{
			// setup ray handling stuff
			float rttScale = .25f;

			if(!Config.isDesktop)
				rttScale = 0.2f;

			int maxRays = 128;
			RayHandler.setColorPrecisionMediump();
			rayHandler = new RayHandler(Physics.world, maxRays, (int)(Gdx.graphics.getWidth()*rttScale), (int)(Gdx.graphics.getHeight()*rttScale));
			rayHandler.setShadows(true);
			rayHandler.setAmbientLight( 0, 0.05f, 0.25f, 0.2f );
//			RayHandler.setGammaCorrection( true );
//			RayHandler.useDiffuseLight( true );
//			rayHandler.setAmbientLight( 0, 0.01f, 0.025f, 0f );
			rayHandler.setCulling(true);
			rayHandler.setBlur(true);
			rayHandler.setBlurNum(1);

			// attach light to player
			final Color c = new Color();

			c.set( .7f, .7f, 1f, 1f );
			playerLight = new ConeLight( rayHandler, maxRays, c, 30, 0, 0, 0, 15 );
			playerLight.setSoft( false );
			playerLight.setMaskBits( 0 );

			// level lights test
			Vector2 tile;
			float dist = 20f;
			float intensity = 1f;
			float halfTileMt = Convert.px2mt( Convert.scaledPixels( 112 ) );

			tile = Convert.tileToMt( 1, 1 ).add(halfTileMt, -halfTileMt);
			c.set( 1f, .85f, .35f, intensity );
			levelLights[0] = new PointLight( rayHandler, maxRays, c, dist/2, tile.x, tile.y );

			tile = Convert.tileToMt( 9, 1 ).add(halfTileMt, -halfTileMt);
			c.set( 1f, .85f, .35f, intensity );
			levelLights[1] = new PointLight( rayHandler, maxRays, c, dist, tile.x, tile.y );

			tile = Convert.tileToMt( 1, 6 ).add(halfTileMt, -halfTileMt);
			c.set( 1f, .85f, .35f, intensity );
			levelLights[2] = new PointLight( rayHandler, maxRays, c, dist/2, tile.x, tile.y );

			tile = Convert.tileToMt( 5, 6 ).add(halfTileMt, -halfTileMt);
			c.set( 1f, .85f, .35f, intensity );
			levelLights[3] = new PointLight( rayHandler, maxRays, c, dist, tile.x, tile.y );

			tile = Convert.tileToMt( 6, 2 ).add(0, -halfTileMt/2f );
			c.set( 1f, .85f, .35f, intensity );
			levelLights[4] = new PointLight( rayHandler, maxRays, c, dist/2, tile.x, tile.y );

			tile = Convert.tileToMt( 8, 2 ).add(0, -halfTileMt/2f );
			c.set( 1f, .85f, .35f, intensity );
			levelLights[5] = new PointLight( rayHandler, maxRays, c, dist, tile.x, tile.y );

			tile = Convert.tileToMt( 7, 7 ).add(0, halfTileMt/2f );
			c.set( 1f, .85f, .35f, intensity );
			levelLights[6] = new PointLight( rayHandler, maxRays, c, dist/2, tile.x, tile.y );

			tile = Convert.tileToMt( 9, 7 ).add(0, halfTileMt/2f );
			c.set( 1f, .85f, .35f, intensity );
			levelLights[7] = new PointLight( rayHandler, maxRays, c, dist, tile.x, tile.y );

			tile = Convert.tileToMt( 5, 5 ).add(-halfTileMt/2f,0);
			c.set( 1f, .85f, .35f, intensity );
			levelLights[8] = new PointLight( rayHandler, maxRays, c, dist/2, tile.x, tile.y );

			tile = Convert.tileToMt( 1, 4 ).add(halfTileMt,-halfTileMt);
			c.set( 1f, .85f, .35f, intensity );
			levelLights[9] = new PointLight( rayHandler, maxRays, c, dist, tile.x, tile.y );

			for( int i = 0; i < 10; i++)
			{
				levelLights[i].setSoft( false );
				levelLights[i].setMaskBits( CollisionFilters.CategoryPlayer | CollisionFilters.CategoryTrackWalls );
			}
		}
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

	private void createTweener()
	{
		tweener = new Tweener();
		Tweener.registerAccessor( Message.class, new MessageAccessor() );
		Tweener.registerAccessor( HudLabel.class, new HudLabelAccessor() );
	}

	public void tick()
	{
		logic.tick();
		hud.tick();
		TrackEffects.tick();
		CarSoundManager.tick();

		Debug.update();
	}

	private int frameCount = 0;
	public void render()
	{
		GL20 gl = Gdx.graphics.getGL20();
		OrthographicCamera ortho = Director.getCamera();

		// Entity's state() is transformed into pixel space
		EntityManager.raiseOnBeforeRender( URacer.getTemporalAliasing() );

		// follow the car
		if( player != null )
		{
			controller.setPosition( player.state().position );
		}

		if( Config.Graphics.EnablePostProcessingFx )
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

			// clear buffers
			gl.glClearDepthf( 1 );
			gl.glClearColor( 0, 0, 0, 1 );
			gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );

			// render base tilemap
			level.renderTilemap();

			gl.glDepthMask( false );
			batch.begin();
			{
				// batch render effects
				if( Config.Graphics.hasEffect(TrackEffects.Effects.CarSkidMarks.id) )
					TrackEffects.renderEffect( Effects.CarSkidMarks, batch );

				if( Config.Graphics.hasEffect(TrackEffects.Effects.SmokeTrails.id) )
					TrackEffects.renderEffect( Effects.SmokeTrails, batch );

				// batch render entities
				EntityManager.raiseOnRender( batch, URacer.getTemporalAliasing() );
			}
			batch.end();

			// render 3d meshes
			level.renderMeshes( gl );

			//
			// rays stuff
			//
			if( Config.Graphics.NightMode )
			{
				// update player light (subframe interpolation ready)
				float ang = 90 + player.state().orientation;

				// the body's compound shape should be created with some clever thinking in it :)
				float offx = (player.getCarModel().length/2f) + .25f;
				float offy = 0f;

				float cos = MathUtils.cosDeg(ang);
				float sin = MathUtils.sinDeg(ang);
				float dX = offx * cos - offy * sin;
				float dY = offx * sin + offy * cos;

				float px = Convert.px2mt(player.state().position.x) + dX;
				float py = Convert.px2mt(player.state().position.y) + dY;

				playerLight.setDirection( ang );
				playerLight.setPosition( px, py );

//				playerLight.setDirection( 90 - player.orient() );

//				if( Config.Graphics.SubframeInterpolation || URacer.hasStepped() )
					rayHandler.update();

				rayHandler.setCombinedMatrix( Director.getMatViewProjMt(),
						Convert.px2mt( ortho.position.x ),
						Convert.px2mt( ortho.position.y ),
						Convert.px2mt( ortho.viewportWidth * ortho.zoom ),
						Convert.px2mt( ortho.viewportHeight * ortho.zoom )
				);

				rayHandler.render();

//				if( (frameCount&0x3f)==0x3f)
//				{
//					System.out.println("lights rendered="+rayHandler.lightRenderedLastFrame);
//				}
			}

			frameCount++;
		}

		if( Config.Graphics.EnablePostProcessingFx )
		{
			PostProcessor.end();
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
			Debug.renderGraphicalStats( Gdx.graphics.getWidth() - Debug.getStatsWidth(),
					Gdx.graphics.getHeight() - Debug.getStatsHeight() - Debug.fontHeight );
			Debug.renderMemoryUsage();
			Debug.end();
		} else
		{
			Debug.begin( batch );
			Debug.renderVersionInfo();
			Debug.renderGraphicalStats( Gdx.graphics.getWidth() - Debug.getStatsWidth(),
					Gdx.graphics.getHeight() - Debug.getStatsHeight() - Debug.fontHeight );
			Debug.end();
		}
	}

	public Level getLevel()
	{
		return level;
	}

	public Car getPlayer()
	{
		return player;
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
