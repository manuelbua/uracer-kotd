package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.task.Task;

public abstract class GameTask extends Task implements Disposable {

	public GameTask() {
	}

	public abstract void onReset();

	public void onRestart() {
		onReset();
	}
}
