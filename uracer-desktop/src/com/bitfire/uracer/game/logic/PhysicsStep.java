package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.logic.PhysicsStepEvent.Type;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.task.TaskManagerEvent;

public class PhysicsStep extends Task {
	private World world;

	public PhysicsStep( World world, TaskManagerEvent.Order order ) {
		super( order );
		this.world = world;
	}

	@Override
	protected void onTick() {
		GameEvents.physicsStep.trigger( this, true, 0, Type.onBeforeTimestep );
		world.step( Config.Physics.PhysicsDt, 10, 10 );
		GameEvents.physicsStep.trigger( this, true, 0, Type.onAfterTimestep );
	}

	public void triggerOnTemporalAliasing( boolean stepped, float aliasingFactor ) {
		GameEvents.physicsStep.trigger( this, stepped, aliasingFactor, Type.onTemporalAliasing );
	}
}
