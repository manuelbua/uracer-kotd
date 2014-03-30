
package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.UIUtils;

/** Implements a Screen that can overlay libgdx's UI elements on it
 * 
 * @author bmanuel */
public abstract class UIScreen extends Screen {
	protected Stage ui;
	protected Input input;

	public UIScreen () {
		input = URacer.Game.getInputSystem();
		ui = createStage();
	}

	protected abstract void setupUI (Stage ui);

	protected abstract void draw ();

	protected Stage createStage () {
		return UIUtils.newFittedStage();
	}

	protected void reload () {
		disable();
		ui.dispose();
		Art.disposeScreensData();
		Art.loadScreensData();
		ui = createStage();
		setupUI(ui);
		enable();
	}

	@Override
	public boolean init () {
		setupUI(ui);
		return true;
	}

	@Override
	public void enable () {
		Gdx.input.setInputProcessor(ui);
	}

	@Override
	public void disable () {
		Gdx.input.setInputProcessor(null);
	}

	@Override
	public void dispose () {
		disable();
		ui.dispose();
	}

	@Override
	public void render (FrameBuffer dest) {
		boolean hasDest = (dest != null);

		if (hasDest) {
			ui.getViewport().update(dest.getWidth(), dest.getHeight(), true);
			dest.begin();
		} else {
			ui.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		}

		draw();

		if (hasDest) {
			dest.end();
		}
	}
}
