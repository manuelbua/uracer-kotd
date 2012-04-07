package com.bitfire.uracer.game;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.audio.CarSoundManager;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.factories.CarFactory;
import com.bitfire.uracer.factories.CarFactory.CarType;
import com.bitfire.uracer.game.GameData.State;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.task.TaskManager;
import com.bitfire.uracer.utils.Convert;

// TODO, extrapolate its GameRenderer out of it
public class Game implements Disposable {

	// config
	public GameplaySettings gameSettings = null;

	// logic
	private GameLogic gameLogic = null;

	// post-processing
	private Bloom bloom = null;
	private Bloom.Settings bloomSettings = null;
	private Zoom zoom = null;

	// graphics
	private GameRenderer gameRenderer = null;
	private Hud hud = null;

	// sounds
	private CarSoundManager carSoundManager = null;

	public Game( String levelName, GameDifficulty difficulty ) {
		GameData.create( difficulty );
		Art.init();
		Convert.init( GameData.scalingStrategy.invTileMapZoomFactor );
		Director.init();

		Car car = CarFactory.createPlayer( CarType.OldSkool, new CarModel().toModel2() );
		GameData.createStates( car );
		GameData.createSystems();

		carSoundManager = new CarSoundManager();	// early load

		GameData.createWorld( levelName, false );

		gameLogic = new GameLogic();

		// ----------------------------
		// rendering engine initialization
		// ----------------------------
		gameRenderer = new GameRenderer( GameData.gameWorld );

		// in-place customization
		if( Config.Graphics.EnablePostProcessingFx ) {
			setupPostProcessing( gameRenderer.postProcessor );
		}

		hud = new Hud( car );

		// dbg
		GameData.messager.show( "This is just a test", 300, MessageType.Good, MessagePosition.Bottom, MessageSize.Big );
	}

	@Override
	public void dispose() {
		hud.dispose();
		Director.dispose();
		gameRenderer.dispose();
		carSoundManager.dispose();
		Art.dispose();
		GameData.dispose();
	}

	private void setupPostProcessing( PostProcessor postProcessor ) {
		bloom = new Bloom( postProcessor, Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight );

		// Bloom.Settings bs = new Bloom.Settings( "arrogance-1 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.25f, 1f, 0.1f, 0.8f, 1.4f );
		// Bloom.Settings bs = new Bloom.Settings( "arrogance-2 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.35f, 1f, 0.1f, 1.4f, 0.75f );

		float threshold = ((GameData.gameWorld.isNightMode() && !Config.Graphics.DumbNightMode) ? 0.2f : 0.45f);
		bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
		bloom.setSettings( bloomSettings );

		zoom = new Zoom( postProcessor, Config.PostProcessing.ZoomQuality );
		postProcessor.addEffect( zoom );

		postProcessor.addEffect( bloom );
	}

	public boolean tick() {
		TaskManager.dispatchTick();

		if( !gameLogic.onTick() )
			return false;

		// ---------------------------------------------------
		// post-processor animator's tick impl
		// ---------------------------------------------------
		float factor = 1 - (URacer.timeMultiplier - 0.3f) / (Config.Physics.PhysicsTimeMultiplier - 0.3f);

		if( Config.Graphics.EnablePostProcessingFx && zoom != null ) {
			zoom.setOrigin( Director.screenPosFor( State.playerState.car.getBody() ) );
			zoom.setStrength( -0.05f * factor );
		}

		if( Config.Graphics.EnablePostProcessingFx && bloom != null && zoom != null ) {
			bloom.setBaseSaturation( 0.5f - 0.5f * factor );
			bloom.setBloomSaturation( 1.5f - factor * 1.15f );
			bloom.setBloomIntesity( 1f + factor * 1.75f );
		}
		// ---------------------------------------------------

		Debug.tick();
		return true;
	}

	public void render() {
		gameLogic.onBeforeRender();
		gameRenderer.render();
	}

	public void pause() {
	}

	public void resume() {
		gameRenderer.rebind();
	}
}