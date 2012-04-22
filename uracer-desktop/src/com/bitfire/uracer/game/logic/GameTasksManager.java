package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.utils.Array;

public class GameTasksManager {
	private Array<GameTask> tasks = new Array<GameTask>( 10 );

	public void add( GameTask task ) {
		tasks.add( task );
	}

	public void dispose() {
		for( GameTask task : tasks ) {
			task.dispose();
		}
	}

	public void reset() {
		for( GameTask task : tasks ) {
			task.onReset();
		}
	}

	public void restart() {
		for( GameTask task : tasks ) {
			task.onRestart();
		}
	}
}
