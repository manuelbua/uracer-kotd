package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.PhysicsStepEvent.Type;
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
		GameEvents.physicsStep.trigger( Type.onBeforeTimestep );
		world.step( Config.Physics.PhysicsDt, 10, 10 );
		GameEvents.physicsStep.trigger( Type.onAfterTimestep );
	}

	public void triggerOnTemporalAliasing( float aliasingFactor ) {
		GameEvents.physicsStep.temporalAliasingFactor = aliasingFactor;
		GameEvents.physicsStep.trigger( Type.onTemporalAliasing );
	}
}
