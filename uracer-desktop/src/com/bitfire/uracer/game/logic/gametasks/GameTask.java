
package com.bitfire.uracer.game.logic.gametasks;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.events.TaskManagerEvent.Order;
import com.bitfire.uracer.game.task.Task;

public abstract class GameTask extends Task implements Disposable {

	public GameTask () {
		super(Order.DEFAULT);
	}

	public GameTask (Order order) {
		super(order);
	}

	public void onReset () {
	}

	public void onRestart () {
		// onReset();
	}
}
