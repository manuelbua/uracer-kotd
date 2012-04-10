package com.bitfire.uracer.game.data;

import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.game.Input;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.effects.TrackEffects;
import com.bitfire.uracer.game.logic.PhysicsStep;
import com.bitfire.uracer.task.TaskManagerEvent;

// @formatter:off
/** Systems
 *
 * This is the order in which tickable and Tasks are being dispatched and consumed by the game super components:
 *
 * 1	Input task
 * 2	PhysicsStep task
 * 3	any other task
 * 4	Time timers
 * 5	GameLogic updates playState and driftState
 *
 **/
// @formatter:on
public final class Systems {
	public PhysicsStep physicsStep;
	public TrackEffects trackEffects;

	public Systems( World b2dWorld, Car car ) {
		input = new Input( TaskManagerEvent.Order.MINUS_4 );
		physicsStep = new PhysicsStep( b2dWorld, TaskManagerEvent.Order.MINUS_3 );

		// FIXME this is just for GameRenderer, Hud and GameData
		trackEffects = new TrackEffects( car );
	}

	public void dispose() {
		trackEffects.dispose();
	}

	public Input input;
}