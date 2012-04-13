package com.bitfire.uracer.game;

public final class GameplaySettings {
	// settings
	public final GameDifficulty difficulty;
	public final float linearVelocityAfterFeedback;
	public final float throttleDampingAfterFeedback;

	public GameplaySettings( GameDifficulty difficulty ) {
		this.difficulty = difficulty;

		switch( difficulty ) {
		case Hard:
			linearVelocityAfterFeedback = 0.95f;
			throttleDampingAfterFeedback = 0.95f;
			break;
		case Medium:
			linearVelocityAfterFeedback = 0.975f;
			throttleDampingAfterFeedback = 0.975f;
			break;
		default:
		case Easy:
			linearVelocityAfterFeedback = 0.99f;
			throttleDampingAfterFeedback = 0.99f;
			break;
		}
	}
}
