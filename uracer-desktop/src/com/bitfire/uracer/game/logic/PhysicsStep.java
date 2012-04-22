package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.events.PhysicsStepEvent;
import com.bitfire.uracer.game.events.PhysicsStepEvent.Type;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.task.TaskManagerEvent;

public class PhysicsStep extends Task {
	// event
	public final PhysicsStepEvent event = new PhysicsStepEvent();

	private World world;

	public PhysicsStep( World world, TaskManagerEvent.Order order ) {
		super( order );
		this.world = world;
	}

	@Override
	protected void onTick() {
		event.trigger( 0, Type.onBeforeTimestep );
		world.step( Config.Physics.PhysicsDt, 10, 10 );
		event.trigger( 0, Type.onAfterTimestep );
	}

	public void triggerOnTemporalAliasing( float aliasingFactor ) {
		event.trigger( aliasingFactor, Type.onTemporalAliasing );
	}
}
