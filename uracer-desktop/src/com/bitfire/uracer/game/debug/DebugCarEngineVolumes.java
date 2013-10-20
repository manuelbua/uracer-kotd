
package com.bitfire.uracer.game.debug;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.debug.DebugHelper.RenderFlags;
import com.bitfire.uracer.game.debug.player.DebugMeter;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines.EngineSoundSet;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.ColorUtils;
import com.bitfire.uracer.utils.SpriteBatchUtils;

public class DebugCarEngineVolumes extends DebugRenderable {
	private Array<DebugMeter> meters = new Array<DebugMeter>();
	private EngineSoundSet soundset;
	private Matrix4 idt = new Matrix4();
	private String[] sampleNames = {"idle    ", "on-low  ", "on-mid  ", "on-high ", "off-low ", "off-mid ", "off-high"};

	public DebugCarEngineVolumes (RenderFlags flag, EngineSoundSet soundset) {
		super(flag);
		this.soundset = soundset;

		for (int i = 0; i < EngineSoundSet.NumSamples; i++) {
			DebugMeter m = new DebugMeter(64, Art.DebugFontHeight);
			m.setLimits(0, 1);
			m.setShowLabel(false);
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
		return hasPlayer && soundset != null;
	}

	@Override
	public void tick () {
		if (isActive()) {

			// String dbg = "";
			float[] volumes = soundset.getVolumes();
			for (int i = 0; i < soundset.getVolumes().length; i++) {
				float v = volumes[i];
				meters.get(i).setValue(v);

				// dbg += "[" + ((i == tensiveMusic.getMusicIndex()) ? "*" : " ") + String.format("%02.1f", v) + "] ";
			}

			// Gdx.app.log("MusicVolumes", dbg);
		}
	}

	@Override
	public void renderBatch (SpriteBatch batch) {
		if (isActive() && meters.size > 0) {
			Matrix4 prev = batch.getTransformMatrix();
			batch.setTransformMatrix(idt);
			batch.enableBlending();

			float prevHeight = 0;
			int index = 0;
			int drawx = 410;
			int drawy = 0;

			SpriteBatchUtils.drawString(batch, "car engine soundset", drawx, drawy);
			SpriteBatchUtils.drawString(batch, "=======================", drawx, drawy + Art.DebugFontHeight);

			String text;
			for (DebugMeter m : meters) {
				int x = drawx, y = drawy + Art.DebugFontHeight * 2;

				// offset by index
				y += index * (prevHeight + 1);

				// compute color
				float alpha = 1;
				Color c = ColorUtils.paletteRYG(1.5f - m.getValue() * 1.5f, alpha);

				{
					// render track number
					text = sampleNames[index];
					batch.setColor(1, 1, 1, alpha);
					SpriteBatchUtils.drawString(batch, text, x, y);
					batch.setColor(1, 1, 1, 1);

					// render meter after text
					int meter_x = x + (text.length() * Art.DebugFontWidth) + 2;
					m.color.set(c);
					m.setPosition(meter_x, y);
					m.render(batch);

					// render volume numerical value
					text = String.format("%.02f", m.getValue());
					batch.setColor(1, 1, 1, alpha);
					SpriteBatchUtils.drawString(batch, text, meter_x + m.getWidth() + 2, y);
					batch.setColor(1, 1, 1, 1);
				}

				index++;
				prevHeight = m.getHeight();
			}

			batch.setTransformMatrix(prev);
			batch.disableBlending();
		}
	}

	@Override
	public void reset () {
	}
}
