package com.bitfire.uracer.screen;

import com.bitfire.uracer.Game;
import com.bitfire.uracer.GameDifficulty;

public class GameScreen extends Screen
{
	private Game game = null;

	public GameScreen()
	{
		game = new Game( GameDifficulty.Easy );
	}

	@Override
	public void removed()
	{
		super.removed();
		game.dispose();
		game = null;
	}

	@Override
	public void tick()
	{
		game.tick();
	}

	@Override
	public void render()
	{
		game.render();
	}
}
