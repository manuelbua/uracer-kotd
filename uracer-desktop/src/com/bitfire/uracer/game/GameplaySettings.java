package com.bitfire.uracer.game;


public final class GameplaySettings {
	// settings
	public final GameDifficulty difficulty;
	public final float dampingFriction;

	public GameplaySettings( GameDifficulty difficulty ) {
		this.difficulty = difficulty;

		switch( difficulty ) {
		case Hard:
			dampingFriction = 0.975f;
			break;
		case Medium:
			dampingFriction = 0.975f;
			break;
		case Easy:
		default:
			dampingFriction = 0.975f;
			break;
		}
	}
}
