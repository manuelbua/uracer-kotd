package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.Car.Aspect;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.debug.DebugHelper;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Vignette;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.task.TaskManager;

public class Game implements Disposable {

	// config
	public GameplaySettings gameplaySettings = null;

	// debug
	private DebugHelper debug = null;

	// logic
	private GameLogic gameLogic = null;

	// rendering
	private GameRenderer gameRenderer = null;
	private boolean canPostProcess = false;

	// post processing
	private Bloom bloom = null;
	private Zoom zoom = null;
	private Vignette vignette = null;

	public Game( String levelName, ScalingStrategy scalingStrategy, GameDifficulty difficulty, Aspect carAspect, CarModel carModel ) {
		gameplaySettings = new GameplaySettings( difficulty );

		// handle game rules and mechanics, it's all about game data
		gameLogic = new GameLogic( gameplaySettings, scalingStrategy, levelName, carAspect, carModel );

		GameWorld world = gameLogic.getGameWorld();

		// handles rendering
		gameRenderer = new GameRenderer( scalingStrategy, world, Config.PostProcessing.Enabled );
		canPostProcess = gameRenderer.hasPostProcessor();

		// post-processing
		if( canPostProcess ) {
			configurePostProcessing( gameRenderer.getPostProcessor(), world );
		}

		// initialize the debug helper
		debug = new DebugHelper( gameRenderer.getWorldRenderer(), gameLogic.getBox2dWorld() );
		debug.setPlayer( gameLogic.getPlayer() );
		Gdx.app.log( "Game", "Debug helper initialized with player instance" );
	}

	@Override
	public void dispose() {
		debug.dispose();
		gameRenderer.dispose();
		gameLogic.dispose();
	}

	private void configurePostProcessing( PostProcessor processor, GameWorld world ) {

		processor.setEnabled( true );

		bloom = new Bloom( Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight );

		// Bloom.Settings bs = new Bloom.Settings( "arrogance-1 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.25f, 1f, 0.1f, 0.8f, 1.4f );
		// Bloom.Settings bs = new Bloom.Settings( "arrogance-2 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.35f, 1f, 0.1f, 1.4f, 0.75f );

		float threshold = ((world.isNightMode() && !Config.Graphics.DumbNightMode) ? 0.2f : 0.45f);
		Bloom.Settings bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
		bloom.setSettings( bloomSettings );

		zoom = new Zoom( Config.PostProcessing.ZoomQuality );
		processor.addEffect( zoom );
		processor.addEffect( bloom );

		if( Config.PostProcessing.EnableVignetting ) {
			vignette = new Vignette();
			vignette.setCoords( 0.75f, 0.4f );
			processor.addEffect( vignette );
		}
	}

	// FIXME, this is logic and it shouldn't be here
	private void updatePostProcessingEffects() {
		float factor = 1 - (URacer.timeMultiplier - GameLogic.TimeMultiplierMin) / (Config.Physics.PhysicsTimeMultiplier - GameLogic.TimeMultiplierMin);
		Car playerCar = gameLogic.getPlayer();

		if( zoom != null && playerCar != null ) {
			zoom.setOrigin( GameRenderer.screenPosForMt( playerCar.getBody().getPosition() ) );
			zoom.setStrength( -0.1f * factor );
		}

		if( bloom != null && zoom != null ) {
			bloom.setBaseSaturation( 0.5f - 0.5f * factor );
//			bloom.setBloomSaturation( 1.5f - factor * 0.85f );	// TODO when charged
			bloom.setBloomSaturation( 1.5f - factor * 1.5f );	// TODO when completely discharged
			bloom.setBloomIntesity( 1f + factor * 1.75f );

			// vignette.setY( (1 - factor) * 0.74f + factor * 0.4f );
			// vignette.setIntensity( 1f );

			vignette.setIntensity( factor );
		}
	}

	public boolean tick() {
		TaskManager.dispatchTick();

		if( !gameLogic.onTick() ) {
			return false;
		}

		if( canPostProcess ) {
			updatePostProcessingEffects();
		}

		debug.tick();

		return true;
	}

	public void render() {
		PlayerCar player = gameLogic.getPlayer();

		gameLogic.onBeforeRender();
		gameRenderer.onBeforeRender( player );
		gameRenderer.render( player );
	}

	public void pause() {
	}

	public void resume() {
		gameRenderer.rebind();
	}
}