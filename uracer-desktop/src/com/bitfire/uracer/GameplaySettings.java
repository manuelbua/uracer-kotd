package com.bitfire.uracer;

public class GameplaySettings
{
	// difficulty
	public static final int Easy = 1;
	public static final int Medium = 2;
	public static final int Hard = 3;

	// settings
	public int difficulty = Easy;
	public float linearVelocityAfterFeedback = 0;
	public float throttleDampingAfterFeedback = 1;

	public static GameplaySettings create( int difficulty )
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
