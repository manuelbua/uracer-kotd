package com.bitfire.uracer.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.GameLogic;

public class TrackEffects
{
	private static Car player;

	private static CarSkidMarks skidMarks;

	public static void init( GameLogic logic )
	{
		player = logic.getGame().getLevel().getPlayer();
		skidMarks = new CarSkidMarks(player);
	}

	public static void tick()
	{
		skidMarks.tick();
	}

	public static void renderPlayerSkidMarks(SpriteBatch batch)
	{
		skidMarks.render(batch);
	}

	public static void reset()
	{
		skidMarks.reset();
	}

	public static void dispose()
	{
		skidMarks.dispose();
	}


	/**
	 * dbg
	 */

	public static int getVisibleSkidMarksCount()
	{
		return skidMarks.visibleSkidMarksCount;
	}
}

