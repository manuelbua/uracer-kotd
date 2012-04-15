package com.bitfire.uracer.game;

public final class GameplaySettings {
	// settings
	public final GameDifficulty difficulty;
	public final float linearVelocityDampingAfterFeedback;
	public final float throttleDampingAfterFeedback;

	public GameplaySettings( GameDifficulty difficulty ) {
		this.difficulty = difficulty;

		switch( difficulty ) {
		case Hard:
			linearVelocityDampingAfterFeedback = 0.95f;
			throttleDampingAfterFeedback = 0.95f;
			break;
		case Medium:
			linearVelocityDampingAfterFeedback = 0.975f;
			throttleDampingAfterFeedback = 0.975f;
			break;
		case Easy:
		default:
			linearVelocityDampingAfterFeedback = 0.99f;
			throttleDampingAfterFeedback = 0.99f;
			break;
		}
	}
}
