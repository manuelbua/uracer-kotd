package com.bitfire.uracer.game;


public final class GameplaySettings {
	// settings
	public final GameDifficulty difficulty;
	public final float dampingLinearVelocityAfterFeedback;
	public final float dampingAngularVelocityAfterFeedback;
	public final float dampingThrottleAfterFeedback;
	public final float dampingFriction;

	public GameplaySettings( GameDifficulty difficulty ) {
		this.difficulty = difficulty;

		switch( difficulty ) {
		case Hard:
			dampingLinearVelocityAfterFeedback = 0.95f;
			dampingAngularVelocityAfterFeedback = 0.85f;
			dampingThrottleAfterFeedback = 0.95f;
			dampingFriction = 0.975f;
			break;
		case Medium:
			dampingLinearVelocityAfterFeedback = 0.975f;
			dampingAngularVelocityAfterFeedback = 0.85f;
			dampingThrottleAfterFeedback = 0.975f;
			dampingFriction = 0.975f;
			break;
		case Easy:
		default:
			dampingLinearVelocityAfterFeedback = 0.99f;
			dampingAngularVelocityAfterFeedback = 0.85f;
			dampingThrottleAfterFeedback = 0.99f;
			dampingFriction = 0.975f;
			break;
		}
	}
}
