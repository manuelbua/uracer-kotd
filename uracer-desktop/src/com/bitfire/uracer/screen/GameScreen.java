package com.bitfire.uracer.screen;

import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.Game;

public class GameScreen extends Screen
{
	private Game game = null;

	public GameScreen()
	{
		game = new Game( Config.Game.difficulty );
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
