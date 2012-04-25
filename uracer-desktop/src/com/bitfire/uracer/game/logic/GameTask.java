package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.task.Task;

public abstract class GameTask extends Task {

	public GameTask() {
	}

	public abstract void onReset();

	public void onRestart() {
		onReset();
	}
}
