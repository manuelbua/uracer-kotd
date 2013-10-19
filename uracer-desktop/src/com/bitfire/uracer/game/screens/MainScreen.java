
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.game.GameLevels;
import com.bitfire.uracer.game.GameLevels.GameLevelDescriptor;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.uracer.utils.UIUtils;

public final class MainScreen extends Screen {

	private Stage ui;
	private Input input;
	private Table root, ltable;
	private List trackList;

	@Override
	public void init () {
		input = URacer.Game.getInputSystem();
		setupUI();
	}

	@Override
	public void enable () {
		Gdx.input.setInputProcessor(ui);
	}

	private void setupUI () {
		ui = UIUtils.newScaledStage();
		root = new Table();
		root.debug();
		root.setBounds(0, 0, ui.getWidth(), ui.getHeight());
		root.invalidate();
		ui.addActor(root);

		// background
		Image bg = new Image(Art.scrBackground);
		bg.setFillParent(true);
		root.addActor(bg);

		// int w = (int)(ui.getWidth() / 3);
		// int h = (int)(ui.getHeight() / 2);

		// version info
		Table infoTable = UIUtils.newVersionInfoTable();
		root.addActor(infoTable);
		root.bottom().padBottom(25);

		// layout tables
		ltable = new Table();
		ltable.debug();
		ltable.defaults();
		ltable.align(Align.left | Align.top);
		root.add(ltable).expandX();// .height(h);

		// rtable = new Table();
		// rtable.debug();
		// rtable.defaults().padRight(5);
		// rtable.align(Align.right | Align.top);
		// root.add(rtable).expandX().height(h);

		// buttonsTable = new Table();
		// root.addActor(buttonsTable);
		// buttonsTable.debug();
		// buttonsTable.setFillParent(true);

		/** left table */

		// track list
		{
			ScrollPane listPane = UIUtils.newScrollPane();
			trackList = UIUtils.newListBox(GameLevels.getLevels(), new ChangeListener() {
				@Override
				public void changed (ChangeEvent event, Actor actor) {
					List source = (List)actor;
					chooseLevel(source.getSelectedIndex());
				}
			});

			listPane.setWidget(trackList);
			// listPane.setFadeScrollBars(false);

			// restore previous user selection, if any
			if (ScreensShared.selectedLevelId.length() > 0) {
				if (GameLevels.levelIdExists(ScreensShared.selectedLevelId)) {
					trackList.setSelection(GameLevels.getLevel(ScreensShared.selectedLevelId).toString());
				} else {
					// level not found?
					chooseFirstLevelAndSave(trackList);
				}
			} else {
				// first run?
				chooseFirstLevelAndSave(trackList);
			}

			TextButton start = UIUtils.newTextButton("START", new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					URacer.Game.show(ScreenType.GameScreen, 1000);
				}
			});

			TextButton options = UIUtils.newTextButton("OPTIONS", new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					URacer.Game.show(ScreenType.OptionsScreen, 1000);
				}
			});

			TextButton quit = UIUtils.newTextButton("EXIT", new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					URacer.Game.quit();
				}
			});

			ltable.add(listPane).colspan(3).width(400).height(200).left().padBottom(10).row();
			ltable.add(start).width(150).height(40).row().expandX();
			ltable.add(options).width(150).height(40).row();
			ltable.add(quit).width(150).height(40).row();
		}

		/** right table */
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

	private void chooseFirstLevelAndSave (List trackList) {
		trackList.setSelectedIndex(0);
		GameLevelDescriptor desc = GameLevels.getLevels()[0];
		ScreensShared.selectedLevelId = desc.getId();
		UserPreferences.string(Preference.LastPlayedTrack, ScreensShared.selectedLevelId);
		UserPreferences.save();
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
			disable();
			ui.dispose();
			setupUI();
			enable();
		} else if (input.isPressed(Keys.S)) {
			URacer.Game.show(ScreenType.GameScreen, 1000);
		} else if (input.isPressed(Keys.O)) {
			URacer.Game.show(ScreenType.OptionsScreen, 1000);
		} else if (input.isPressed(Keys.UP)) {
			int count = trackList.getItems().length;
			int newidx = MathUtils.clamp(trackList.getSelectedIndex() - 1, 0, count - 1);
			trackList.setSelectedIndex(newidx);
			chooseLevel(newidx);
		} else if (input.isPressed(Keys.DOWN)) {
			int count = trackList.getItems().length;
			int newidx = MathUtils.clamp(trackList.getSelectedIndex() + 1, 0, count - 1);
			trackList.setSelectedIndex(newidx);
			chooseLevel(newidx);
		} else {
			ui.act(Config.Physics.Dt);
		}
	}

	@Override
	public void tickCompleted () {
	}

	@Override
	public void render (FrameBuffer dest) {
		boolean hasDest = (dest != null);
		if (hasDest) {
			dest.begin();
			ui.setViewport(ScaleUtils.PlayWidth, ScaleUtils.PlayHeight, false, 0, 0, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
		} else {
			ui.setViewport(ScaleUtils.PlayWidth, ScaleUtils.PlayHeight, false, ScaleUtils.CropX, ScaleUtils.CropY,
				ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
		}

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		ui.draw();

		Table.drawDebug(ui);

		if (hasDest) {
			dest.end();
		}
	}
}
