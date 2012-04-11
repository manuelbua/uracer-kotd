package com.bitfire.uracer.game;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarFactory;
import com.bitfire.uracer.game.actors.CarAspect;
import com.bitfire.uracer.game.audio.CarSoundManager;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.hud.Hud;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.rendering.Debug;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.task.TaskManager;
import com.bitfire.uracer.utils.BatchUtils;
import com.bitfire.uracer.utils.Convert;

public class Game implements Disposable {

	// config
	public GameplaySettings gameSettings = null;

	// logic
	private GameLogic gameLogic = null;

	// post-processing
	private Bloom bloom = null;
	private Zoom zoom = null;

	// graphics
	private GameRenderer gameRenderer = null;

	// tasks
	private List<Task> gameTasks = null;

	public Game( String levelName, GameDifficulty difficulty, CarAspect carType, CarModel carModel ) {
		GameData.create( difficulty );

		Tweener.init();
		Art.init( GameData.Environment.scalingStrategy.invTileMapZoomFactor );
		BatchUtils.init( Art.base6 );
		Convert.init( GameData.Environment.scalingStrategy.invTileMapZoomFactor, Config.Physics.PixelsPerMeter );
		Director.init();
		Car car = CarFactory.createPlayer( carType, carModel );

		GameData.createStates( car );
		GameData.createSystems( GameData.Environment.b2dWorld, car );
		GameData.createWorld( GameData.Environment.b2dWorld, GameData.Environment.scalingStrategy, levelName, false );

		gameLogic = new GameLogic( GameData.Environment.gameWorld );

		// ----------------------------
		// rendering engine initialization
		// ----------------------------
		gameRenderer = new GameRenderer( GameData.Environment.scalingStrategy, GameData.Environment.gameWorld );

		// in-place customization
		if( Config.Graphics.EnablePostProcessingFx ) {
			setupPostProcessing( gameRenderer.postProcessor );
		}

		// ----------------------------
		// game tasks
		// ----------------------------
		gameTasks = new ArrayList<Task>( 10 );
		gameTasks.add( new Hud( car ) );
		gameTasks.add( new CarSoundManager() );
	}

	@Override
	public void dispose() {
		for( Task task : gameTasks ) {
			task.dispose();
		}

		gameTasks.clear();
		Director.dispose();
		gameRenderer.dispose();
		Art.dispose();

		GameData.dispose();
	}

	private void setupPostProcessing( PostProcessor postProcessor ) {
		bloom = new Bloom( Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight );

		// Bloom.Settings bs = new Bloom.Settings( "arrogance-1 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.25f, 1f, 0.1f, 0.8f, 1.4f );
		// Bloom.Settings bs = new Bloom.Settings( "arrogance-2 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.35f, 1f, 0.1f, 1.4f, 0.75f );

		float threshold = ((GameData.Environment.gameWorld.isNightMode() && !Config.Graphics.DumbNightMode) ? 0.2f : 0.45f);
		Bloom.Settings bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
		bloom.setSettings( bloomSettings );

		zoom = new Zoom( Config.PostProcessing.ZoomQuality );
		postProcessor.addEffect( zoom );

		postProcessor.addEffect( bloom );
	}

	public boolean tick() {
		TaskManager.dispatchTick();

		if( !gameLogic.onTick() ) {
			return false;
		}

		// ---------------------------------------------------
		// post-processor animator's tick impl
		// ---------------------------------------------------
		float factor = 1 - (URacer.timeMultiplier - 0.3f) / (Config.Physics.PhysicsTimeMultiplier - 0.3f);

		if( Config.Graphics.EnablePostProcessingFx && zoom != null ) {
			zoom.setOrigin( Director.screenPosFor( GameData.States.playerState.car.getBody() ) );
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