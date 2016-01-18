
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.game.logic.gametasks.SoundManager;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.Dialog;
import com.bitfire.uracer.utils.UIUtils;
import com.bitfire.uracer.utils.Window;

public class GameScreenUI {
	private Input input = null;
	private Stage ui;
	private Window win;
	private Dialog dlg_quit;
	private boolean quitShown;
	private final Game game;
	private boolean enabled;

	// sound
	private Slider sfx_slider, music_slider;
	private Label sfx_label, music_label;

	// quit
	private Button btn_quit, btn_resume;

	public GameScreenUI (final Game game) {
		this.game = game;
		enabled = false;
		input = URacer.Game.getInputSystem();
		constructUI();
	}

	public void dispose () {
		disable();
		ui.dispose();
	}

	// utilities

	private void constructUI () {
		ui = UIUtils.newFittedStage();

		// setup main window
		win = UIUtils.newWindow("OPTIONS");
		ui.addActor(win);

		Table content = UIUtils.newTable();
		Table bottom = UIUtils.newTable();

		win.row().fill().expand();
		win.add(content);
		win.row().fill().expand();
		win.add(bottom);

		content.row().expandX();

		// win.setBackground(brushed);

		// quit button
		btn_quit = UIUtils.newButton("QUIT RACE (Q)", new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				if (enabled && !quitShown) {
					showQuit();
				}
			}
		});

		// resume button
		btn_resume = UIUtils.newButton("RESUME GAME (ESC)", new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				if (enabled) {
					disable();
					enabled = false;
				}
			}
		});
		bottom.add(btn_resume).padRight(5);
		bottom.add(btn_quit);
		bottom.right();

		// quit dialog
		dlg_quit = new Dialog("Confirm", Art.scrSkin, "dialog") {
			@Override
			protected void result (Object quit) {
				quitShown = false;
				if ((Boolean)quit == true) {
					quit();
				}
			};
		}.text("Your current lap will not be saved, are you sure you want to quit?").button("YES (Y)", true)
			.button("NO (N)", false).key(Keys.Y, true).key(Keys.N, false);

		// sound sliders
		music_label = UIUtils.newLabel("Music volume", false);
		sfx_label = UIUtils.newLabel("Sound effects volume", false);
		sfx_slider = UIUtils.newSlider(0, 1, 0.01f, SoundManager.SfxVolumeMul, new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Slider source = (Slider)event.getListenerActor();
				SoundManager.SfxVolumeMul = source.getValue();
				UserPreferences.real(Preference.SfxVolume, SoundManager.SfxVolumeMul);
			}
		});
		content.add(sfx_label).left();
		content.add(sfx_slider).right();

		music_slider = UIUtils.newSlider(0, 1, 0.01f, SoundManager.MusicVolumeMul, new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Slider source = (Slider)event.getListenerActor();
				SoundManager.MusicVolumeMul = source.getValue();
				UserPreferences.real(Preference.MusicVolume, SoundManager.MusicVolumeMul);
			}
		});
		content.row();
		content.add(music_label).left();
		content.add(music_slider).right();
	}

	private void setup () {
		Dialog.fadeDuration = 0.4f;
		quitShown = false;

		win.setWidth(480);
		win.setHeight(280);
		win.setPosition((Gdx.graphics.getWidth() - win.getWidth()) / 2, (Gdx.graphics.getHeight() - win.getHeight()) / 2);
	}

	private void showQuit () {
		dlg_quit.getContentTable().padBottom(10);
		dlg_quit.pack();
		dlg_quit.show(ui);
		quitShown = true;
	}

	private void hideQuit () {
		quitShown = false;
		dlg_quit.hide();
	}

	private void quit () {
		UserPreferences.save();
		game.quit();
		game.tick();
	}

	private void enable () {
		game.pause();
		Gdx.input.setInputProcessor(ui);
		setup();
	}

	private void disable () {
		Gdx.input.setInputProcessor(null);
		Dialog.fadeDuration = 0f;
		hideQuit();
		game.resume();
	}

	private void handleInput () {
		// toggle in-game menu, this shortcut shall be always available
		if (input.isPressed(Keys.ESCAPE)) {
			if (quitShown) {
				hideQuit();
			} else {
				enabled = !enabled;
				if (enabled) {
					enable();
				} else {
					disable();
				}
			}
		}

		if (enabled) {
			if (input.isPressed(Keys.Q) && !quitShown) {
				showQuit();
			}

			if (input.isPressed(Keys.R)) {
				Gdx.input.setInputProcessor(null);
				hideQuit();
				ui.dispose();
				Art.disposeScreensData();
				Art.loadScreensData();
				constructUI();
				Gdx.input.setInputProcessor(ui);
				setup();
			}
		}
	}

	//

	public void tick () {
		handleInput();
	}

	public void render (FrameBuffer dest) {
		if (!enabled) return;
		ui.act(URacer.Game.getLastDeltaSecs() /* Config.Physics.Dt */);

		boolean hasDest = (dest != null);
		if (hasDest) {
			ui.getViewport().update(dest.getWidth(), dest.getHeight(), true);
			dest.begin();
		} else {
			ui.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		}

		ui.draw();

		if (hasDest) {
			dest.end();
		}
	}
}
