
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.game.GameLevels;
import com.bitfire.uracer.game.GameLevels.GameLevelDescriptor;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.screen.UIScreen;
import com.bitfire.uracer.utils.UIUtils;
import com.bitfire.uracer.utils.Window;

public final class MainScreen extends UIScreen {
	private List<String> trackList;
	ScrollPane listPane;

	private void ensureScrollIsVisible () {
		float fidx = (float)(trackList.getSelectedIndex()) / (float)(trackList.getItems().size - 1);
		listPane.validate();
		listPane.setScrollPercentY(fidx);
	}

	@Override
	protected void setupUI (Stage ui) {
		Table root = UIUtils.newTable();
		root.setBounds(0, 0, ui.getWidth(), ui.getHeight());
		root.invalidate();
		ui.addActor(root);

		// background
		Image bg = new Image(Art.scrBackground);
		bg.setFillParent(true);
		root.addActor(bg);

		Window win = UIUtils.newWindow("SINGLE PLAYER");
		ui.addActor(win);

		win.row().fill().expand();

		Table content = UIUtils.newTable();
		Table buttons = UIUtils.newTable();
		Table bottom = UIUtils.newTable();

		win.row().fill().expand();
		win.add(content);
		win.row().fill().expand();
		win.add(buttons);
		win.row().fill().expand();
		win.add(bottom);

		// track list
		{
			listPane = UIUtils.newScrollPane();
			String[] levels = new String[GameLevels.getLevels().length];
			int idx = 0;
			for (GameLevelDescriptor ld : GameLevels.getLevels()) {
				levels[idx++] = ld.toString();
			}

			trackList = UIUtils.newListBox(levels);

			trackList.addListener(new ChangeListener() {
				@Override
				public void changed (ChangeEvent event, Actor actor) {
					Sounds.menuRollover.play();
					ensureScrollIsVisible();
				}
			});

			listPane.setWidget(trackList);
			listPane.setFadeScrollBars(false);
			// listPane.setScrollingDisabled(false, false);

			// restore previous user selection, if any
			if (ScreensShared.selectedLevelId.length() > 0) {
				if (GameLevels.levelIdExists(ScreensShared.selectedLevelId)) {
					trackList.getSelection().set(GameLevels.getLevel(ScreensShared.selectedLevelId).toString());
				} else {
					// level not found?
					chooseLevel(0);
				}
			} else {
				// first run?
				chooseLevel(0);
			}

			TextButton start = UIUtils.newTextButton("RACE!", new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					Sounds.menuClick.play();
					chooseLevel(trackList.getSelectedIndex());
					URacer.Game.show(ScreenType.GameScreen);
				}
			});

			TextButton options = UIUtils.newTextButton("OPTIONS", new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					Sounds.menuClick.play();
					URacer.Game.show(ScreenType.OptionsScreen);
				}
			});

			TextButton quit = UIUtils.newTextButton("EXIT", new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					Sounds.menuClick.play();
					URacer.Game.quit();
				}
			});

			content.row().expand();
			content.add(listPane).width(200).height(150);

			buttons.row();
			buttons.add(start).pad(0, 2, 0, 2);
			buttons.add(options).pad(0, 2, 0, 2);
			buttons.add(quit).pad(0, 2, 0, 2);

			bottom.left().bottom();
			bottom.add(UIUtils.newVersionInfoLabel());
		}

		win.setWidth(600);
		win.setHeight(400);
		win.setPosition((ui.getWidth() - win.getWidth()) / 2, (ui.getHeight() - win.getHeight()) / 2);
	}

	@Override
	public void dispose () {
		ui.dispose();
		disable();
	}

	private void chooseLevel (int levelIndex) {
		if (levelIndex >= 0 && levelIndex < GameLevels.getLevels().length) {
			GameLevelDescriptor desc = GameLevels.getLevels()[levelIndex];
			ScreensShared.selectedLevelId = desc.getId();
			UserPreferences.string(Preference.LastPlayedTrack, ScreensShared.selectedLevelId);
			UserPreferences.save();
		}
	}

	@Override
	public void pause () {
	}

	@Override
	public void resume () {
	}

	@Override
	public void tick () {
		if (input.isPressed(Keys.Q) || input.isPressed(Keys.BACK) || input.isPressed(Keys.ESCAPE)) {
			URacer.Game.quit();
		} else if (input.isPressed(Keys.R)) {
			reload();
		} else if (input.isPressed(Keys.S)) {
			Sounds.menuClick.play();
			chooseLevel(trackList.getSelectedIndex());
			URacer.Game.show(ScreenType.GameScreen);
		} else if (input.isPressed(Keys.O)) {
			URacer.Game.show(ScreenType.OptionsScreen);
		} else if (input.isPressed(Keys.UP) || input.isRepeatedOn(Keys.UP)) {
			int count = trackList.getItems().size;
			int newidx = MathUtils.clamp(trackList.getSelectedIndex() - 1, 0, count - 1);
			trackList.setSelectedIndex(newidx);
		} else if (input.isPressed(Keys.DOWN) || input.isRepeatedOn(Keys.DOWN)) {
			int count = trackList.getItems().size;
			int newidx = MathUtils.clamp(trackList.getSelectedIndex() + 1, 0, count - 1);
			trackList.setSelectedIndex(newidx);
		} else {
			ui.act(Config.Physics.Dt);
		}
	}

	@Override
	public void tickCompleted () {
	}

	@Override
	public void draw () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		ui.draw();
	}
}
