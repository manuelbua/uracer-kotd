package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.logic.hud.Hud;
import com.bitfire.uracer.game.logic.messager.Messager;
import com.bitfire.uracer.game.logic.sounds.SoundManager;
import com.bitfire.uracer.game.logic.trackeffects.TrackEffects;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.task.TaskManagerEvent;

/** Manages the creation and destruction of the main game tasks. */
public final class GameTasksManager {
	private GameWorld gameWorld = null;
	private ScalingStrategy scalingStrategy = null;
	private Array<GameTask> tasks = new Array<GameTask>( 10 );

	/** keeps track of the concrete game tasks
	 * (note that they are all publicly accessible for performance reasons) */

	// input system
	public Input input = null;

	// physics step
	public PhysicsStep physicsStep = null;

	// sound
	public SoundManager sound = null;

	// alerts and infos
	public Messager messager = null;

	// hud
	public Hud hud = null;

	// special effects
	public TrackEffects effects = null;

	public GameTasksManager( GameWorld world, ScalingStrategy strategy ) {
		gameWorld = world;
		scalingStrategy = strategy;
	}

	public void createTasks() {
		// input system
		input = new Input( TaskManagerEvent.Order.MINUS_4 );

		// physics step
		physicsStep = new PhysicsStep( gameWorld.getBox2DWorld(), TaskManagerEvent.Order.MINUS_3 );

		// sound manager
		sound = new SoundManager();
		add( sound );

		// message manager
		messager = new Messager( scalingStrategy.invTileMapZoomFactor );
		add( messager );

		// hud manager
		hud = new Hud();
		add( hud );

		// effects manager
		effects = new TrackEffects();
		add( effects );
	}

	private void add( GameTask task ) {
		tasks.add( task );
	}

	public void dispose() {
		for( GameTask task : tasks ) {
			task.dispose();
		}
	}

	public void reset() {
		for( GameTask task : tasks ) {
			task.onReset();
		}
	}

	public void restart() {
		for( GameTask task : tasks ) {
			task.onRestart();
		}
	}
}
