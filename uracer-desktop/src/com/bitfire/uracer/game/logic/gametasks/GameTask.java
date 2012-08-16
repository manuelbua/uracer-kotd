
package com.bitfire.uracer.game.logic.gametasks;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.task.Task;

public abstract class GameTask extends Task implements Disposable {

	public GameTask () {
	}

	public abstract void onReset ();

	public void onRestart () {
		onReset();
	}
}
