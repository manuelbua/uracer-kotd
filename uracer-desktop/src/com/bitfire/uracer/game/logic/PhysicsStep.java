package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.events.PhysicsStepEvent.Type;
import com.bitfire.uracer.events.TaskManagerEvent.Order;
import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.task.Task;

public class PhysicsStep extends Task {
	private World world;

	public PhysicsStep( World world ) {
		super( Order.Order_Minus_4 );	// TODO, rename to Minus_4 instead
		this.world = world;
	}

	@Override
	protected void onTick() {
		Events.physicsStep.trigger( Type.onBeforeTimestep );
		world.step( Config.Physics.PhysicsDt, 10, 10 );
		Events.physicsStep.trigger( Type.onAfterTimestep );
	}

	public void triggerOnTemporalAliasing( float aliasingFactor ) {
		Events.physicsStep.temporalAliasingFactor = aliasingFactor;
		Events.physicsStep.trigger( Type.onTemporalAliasing );
	}
}
