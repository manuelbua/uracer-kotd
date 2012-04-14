package com.bitfire.uracer.game;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.player.Car.Aspect;
import com.bitfire.uracer.game.player.CarModel;
import com.bitfire.uracer.game.rendering.Debug;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.task.TaskManager;

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

	public Game( String levelName, GameDifficulty difficulty, Aspect carAspect, CarModel carModel ) {
		GameData.create( URacer.getScalingStrategy(), levelName, false, difficulty, carAspect, carModel );

		gameLogic = new GameLogic();
		gameRenderer = new GameRenderer();

		if( Config.Graphics.EnablePostProcessingFx ) {
			setupPostProcessing( gameRenderer.postProcessor );
		}
	}

	@Override
	public void dispose() {
		gameRenderer.dispose();
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
			zoom.setOrigin( Director.screenPosFor( GameData.States.player.car.getBody() ) );
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