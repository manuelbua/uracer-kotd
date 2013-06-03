
package com.bitfire.uracer.game.logic.gametasks;

import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.events.PhysicsStepEvent.Type;
import com.bitfire.uracer.events.TaskManagerEvent;
import com.bitfire.uracer.game.GameEvents;

public class PhysicsStep extends GameTask {
	private World world;

	public PhysicsStep (World world, TaskManagerEvent.Order order) {
		super(order);
		this.world = world;
	}

	@Override
	public void dispose () {
		super.dispose();
		GameEvents.physicsStep.removeAllListeners();
	}

	@Override
	protected void onTick () {
		GameEvents.physicsStep.trigger(this, Type.onBeforeTimestep);
		world.step(Config.Physics.Dt, 10, 10);
		GameEvents.physicsStep.trigger(this, Type.onAfterTimestep);
	}

	@Override
	protected void onTickCompleted () {
		world.clearForces();
		GameEvents.physicsStep.trigger(this, Type.onSubstepCompleted);
	}

	@Override
	public void onReset () {
		world.clearForces();
	}

	@Override
	public void onRestart () {
		onReset();
	}
}
