package com.bitfire.uracer.game;

public class GameplaySettings
{
	// settings
	public GameDifficulty difficulty = GameDifficulty.Easy;
	public float linearVelocityAfterFeedback = 0;
	public float throttleDampingAfterFeedback = 1;

	public static GameplaySettings create( GameDifficulty difficulty )
	{
		GameplaySettings s = new GameplaySettings();

		s.difficulty = difficulty;

		switch( difficulty )
		{
		default:
		case Easy:
			s.linearVelocityAfterFeedback = 0.99f;
			s.throttleDampingAfterFeedback = 0.99f;
			break;

		case Medium:
			s.linearVelocityAfterFeedback = 0.975f;
			s.throttleDampingAfterFeedback = 0.975f;
			break;

		case Hard:
			s.linearVelocityAfterFeedback = 0.95f;
			s.throttleDampingAfterFeedback = 0.95f;
			break;
		}

		return s;
	}
}
