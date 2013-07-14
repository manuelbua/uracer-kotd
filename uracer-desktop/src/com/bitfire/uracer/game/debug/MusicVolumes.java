
package com.bitfire.uracer.game.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.debug.DebugHelper.RenderFlags;
import com.bitfire.uracer.game.debug.player.DebugMeter;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerTensiveMusic;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Art;

public class MusicVolumes extends DebugRenderable {
	private Array<DebugMeter> meters = new Array<DebugMeter>();
	private PlayerTensiveMusic tensiveMusic;

	public MusicVolumes (RenderFlags flag, PlayerTensiveMusic tensiveMusic) {
		super(flag);
		this.tensiveMusic = tensiveMusic;

		for (int i = 0; i < PlayerTensiveMusic.NumTracks; i++) {
			DebugMeter m = new DebugMeter(100, 5);
			m.setLimits(0, 1);
			m.setName("music track " + i);
			meters.add(m);
		}
	}

	@Override
	public void dispose () {
		for (DebugMeter m : meters) {
			m.dispose();
		}
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);
		if (hasPlayer) {
			this.player = player;
		}
	}

	private boolean isActive () {
		return hasPlayer;
	}

	@Override
	public void tick () {
		if (isActive()) {

			String dbg = "";
			for (int i = 0; i < tensiveMusic.getVolumes().length; i++) {
				float v = tensiveMusic.getVolumes()[i];
				meters.get(i).setValue(v);

				dbg += "[" + ((i == tensiveMusic.getMusicIndex()) ? "*" : " ") + String.format("%02.1f", v) + "] ";
			}

			Gdx.app.log("MusicVolumes", dbg);
		}
	}

	@Override
	public void renderBatch (SpriteBatch batch) {
		if (isActive()) {
			float prevHeight = 0;
			int index = 0;
			for (DebugMeter m : meters) {
				int x = 0, y = 100;

				// offset by index
				y += index * (prevHeight + Art.DebugFontHeight);

				m.setPosition(x, y);
				m.render(batch);

				index++;
				prevHeight = m.getHeight();
			}
		}
	}

	@Override
	public void reset () {
	}

}
