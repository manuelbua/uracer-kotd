package com.bitfire.uracer.game;


public final class GameplaySettings {
	// settings
	public final GameDifficulty difficulty;
	public final float dampingLinearVelocityAfterFeedback;
	public final float dampingThrottleAfterFeedback;
	public final float dampingFriction;

	public GameplaySettings( GameDifficulty difficulty ) {
		this.difficulty = difficulty;

		switch( difficulty ) {
		case Hard:
			dampingLinearVelocityAfterFeedback = 1.46f;
			dampingThrottleAfterFeedback = 1.46f;
			dampingFriction = 1.5f;
			break;
		case Medium:
			dampingLinearVelocityAfterFeedback = 1.5f;
			dampingThrottleAfterFeedback = 1.5f;
			dampingFriction = 1.5f;
			break;
		case Easy:
		default:
			dampingLinearVelocityAfterFeedback = 1.52f;
			dampingThrottleAfterFeedback = 1.52f;
			dampingFriction = 1.5f;
			break;
		}
	}
}
