
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Linear;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.LapInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.NumberString;

public class HudLapInfo extends HudElement {

	private HudLabel curr;
	private LapInfo lapInfo;
	private BoxedFloat r, g, b;
	private boolean isValid;

	public HudLapInfo (LapInfo lapInfo) {
		this.lapInfo = lapInfo;

		curr = new HudLabel(FontFace.LcdWhite, "99.99", true);
		curr.setScale(1.5f);
		curr.setPosition((Config.Graphics.ReferenceScreenWidth / 2), Config.Graphics.ReferenceScreenHeight - curr.getHeight() / 2
			- 10);

		r = new BoxedFloat(1);
		g = new BoxedFloat(1);
		b = new BoxedFloat(1);
		isValid = true;
	}

	@Override
	public void dispose () {
	}

	public void toDefaultColor () {
		toColor(1, 1, 1);
	}

	public void toColor (float red, float green, float blue) {
		toColor(500, red, green, blue);
	}

	public void toColor (int millisecs, float red, float green, float blue) {
		Timeline seq = Timeline.createParallel();

		//@off
		seq
			.push(Tween.to(r, BoxedFloatAccessor.VALUE, millisecs).target(red).ease(Linear.INOUT))
			.push(Tween.to(g, BoxedFloatAccessor.VALUE, millisecs).target(green).ease(Linear.INOUT))
			.push(Tween.to(b, BoxedFloatAccessor.VALUE, millisecs).target(blue).ease(Linear.INOUT))
		;
		//@on

		GameTweener.start(seq);
	}

	public boolean isValid () {
		return isValid;
	}

	public void setValid (boolean valid) {
		isValid = valid;
	}

	public void setInvalid (String message) {
		isValid = false;
		curr.setString(message, true);
	}

	@Override
	public void onRender (SpriteBatch batch) {
		// current time
		if (isValid) {
			curr.setString(NumberString.format(lapInfo.getElapsedSeconds()), true);
		}

		curr.setColor(r.value, g.value, b.value);
		curr.render(batch);
	}

	@Override
	public void onRestart () {
		toDefaultColor();
		setValid(true);
	}
}
