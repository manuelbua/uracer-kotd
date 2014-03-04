
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.uracer.utils.UIUtils;

public class GameScreenUI {
	private Input input = null;
	private Stage ui;
	private Table root;
	private Window win;
	private Dialog quit;
	public boolean quitShown;

	public GameScreenUI (final Game game) {
		input = URacer.Game.getInputSystem();
		ui = UIUtils.newScaledStage();
		root = new Table();
		root.debug();
		root.setBounds(0, 0, ui.getWidth(), ui.getHeight());
		root.invalidate();
		ui.addActor(root);

		// setup main window
		win = new Window("Options - Press ESC to resume game", Art.scrSkin);
		ui.addActor(win);
		TextButton closeButton = new TextButton("X", Art.scrSkin);
		closeButton.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				showQuit();
			}
		});
		win.getButtonTable().add(closeButton).height(win.getPadTop());

		// setup quit dialog
		quit = new Dialog("Quit", Art.scrSkin, "dialog") {
			@Override
			protected void result (Object quit) {
				quitShown = false;
				if ((Boolean)quit == true) {
					game.quit();
				}
			};
		}.text("Your current lap will not be saved, are you sure you want to quit?").button("Yes", true).button("No", false)
			.key(Keys.ENTER, true);
	}

	public void setup () {
		Dialog.fadeDuration = 0.4f;
		quitShown = false;

		win.setWidth(400);
		win.setHeight(200);
		win.setPosition((Gdx.graphics.getWidth() - win.getWidth()) / 2, (Gdx.graphics.getHeight() - win.getHeight()) / 2);
	}

	public void showQuit () {
		quit.show(ui);
		quitShown = true;
	}

	//
	//
	//

	public void dispose () {
		ui.dispose();
		disable();
	}

	public void enable () {
		Gdx.input.setInputProcessor(ui);
		setup();
	}

	public void disable () {
		Gdx.input.setInputProcessor(null);
		Dialog.fadeDuration = 0f;
		quit.hide();
	}

	public void tick () {
		ui.act(Config.Physics.Dt);

		if (input.isPressed(Keys.Q) && !quitShown) {
			showQuit();
		}

		if (input.isPressed(Keys.R)) {
			setup();
		}
	}

	public void render (FrameBuffer dest) {
		boolean hasDest = (dest != null);
		if (hasDest) {
			dest.begin();
			ui.setViewport(ScaleUtils.PlayWidth, ScaleUtils.PlayHeight, false, 0, 0, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
		} else {
			ui.setViewport(ScaleUtils.PlayWidth, ScaleUtils.PlayHeight, false, ScaleUtils.CropX, ScaleUtils.CropY,
				ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
		}

		ui.draw();
		Table.drawDebug(ui);

		if (hasDest) {
			dest.end();
		}
	}
}
