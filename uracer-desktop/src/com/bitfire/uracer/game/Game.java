package com.bitfire.uracer.game;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.Car.Aspect;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.rendering.Debug;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Vignette;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.task.TaskManager;

public class Game implements Disposable {

	// config
	public GameplaySettings gameplaySettings = null;

	// logic
	private GameLogic gameLogic = null;

	// rendering
	private GameRenderer gameRenderer = null;

	// post processing
	private Bloom bloom = null;
	private Zoom zoom = null;
	private Vignette vignette = null;

	public Game( String levelName, ScalingStrategy scalingStrategy, GameDifficulty difficulty, Aspect carAspect, CarModel carModel ) {
		gameplaySettings = new GameplaySettings( difficulty );

		// handle game rules and mechanics, it's all about game data
		gameLogic = new GameLogic( gameplaySettings, scalingStrategy, levelName, carAspect, carModel );
		GameWorld world = gameLogic.getGameWorld();
		World box2dWorld = gameLogic.getBox2dWorld();

		// handles rendering
		gameRenderer = new GameRenderer( scalingStrategy, world, box2dWorld );
		configurePostProcessing( gameRenderer.getPostProcessor(), world );
	}

	@Override
	public void dispose() {
		gameLogic.dispose();
		gameRenderer.dispose();
	}

	private void configurePostProcessing( PostProcessor processor, GameWorld world ) {
		if( !Config.PostProcessing.Enabled || processor == null ) {
			return;
		}

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
			vignette.setCoords( 0.75f, 0.35f );
			processor.addEffect( vignette );
		}
	}

	// FIXME, this is logic and it shouldn't be here
	private void updatePostProcessing() {
		float factor = 1 - (URacer.timeMultiplier - GameLogic.TimeMultiplierMin) / (Config.Physics.PhysicsTimeMultiplier - GameLogic.TimeMultiplierMin);

		Car playerCar = gameLogic.getPlayer();

		if( Config.PostProcessing.Enabled && zoom != null && playerCar != null ) {
			zoom.setOrigin( Director.screenPosFor( playerCar.getBody() ) );
			zoom.setStrength( -0.08f * factor );
		}

		if( Config.PostProcessing.Enabled && bloom != null && zoom != null ) {
			bloom.setBaseSaturation( 0.5f - 0.5f * factor );
			bloom.setBloomSaturation( 1.5f - factor * 1.425f );
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

		gameRenderer.getWorldRenderer().updateRayHandler();
		updatePostProcessing();

		Debug.tick();
		return true;
	}

	public void render() {
		gameLogic.onBeforeRender();
		gameRenderer.render( gameLogic.getPlayer() );
	}

	public void pause() {
	}

	public void resume() {
		gameRenderer.rebind();
	}
}