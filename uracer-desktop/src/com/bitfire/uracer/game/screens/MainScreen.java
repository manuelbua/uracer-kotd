
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameTracks;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.uracer.utils.UICamera;
import com.bitfire.uracer.utils.UIUtils;

public final class MainScreen extends Screen {

	private Stage ui;
	private Input input;
	private Table root, buttonsTable, infoTable;
	private TextButton quitButton, optionsButton, startGameButton;
	private SelectBox sbTracks;
	private Label versionLabel;

	@Override
	public void init (ScalingStrategy scalingStrategy) {
		input = URacer.Game.getInputSystem();
		setupUI();
	}

	@Override
	public void enable () {
		Gdx.input.setInputProcessor(ui);
	}

	private void setupUI () {
		ui = new Stage(0, 0, false);
		Camera uicam = new UICamera();
		ui.setCamera(uicam);
		ui.setViewport(ScaleUtils.RefScreenWidth, ScaleUtils.RefScreenHeight, false);
		ui.getCamera().translate(-ui.getGutterWidth(), -ui.getGutterHeight(), 0);

		root = new Table();
		// root.setSize(ScaleUtils.RefScreenWidth, ScaleUtils.RefScreenHeight);
		root.setSize(ui.getWidth(), ui.getHeight());
		ui.addActor(root);

		// background
		Image bg = new Image(Art.scrBackground);
		bg.setFillParent(true);
		root.addActor(bg);

		buttonsTable = new Table();
		root.addActor(buttonsTable);
		// buttonsTable.debug();
		buttonsTable.setFillParent(true);

		sbTracks = UIUtils.newSelectBox(GameTracks.getAvailableTracks(), new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				SelectBox source = (SelectBox)event.getListenerActor();

				switch (source.getSelectionIndex()) {
				case 0:
					break;
				}
			}
		});

		// restore previous user selection, if any
		if (ScreensShared.selectedTrackId.length() > 0) {
			sbTracks.setSelection(ScreensShared.selectedTrackId);
		}

		startGameButton = UIUtils.newTextButton("Start game", new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				ScreensShared.selectedTrackId = sbTracks.getSelection();
				URacer.Game.show(ScreenType.GameScreen);
			}
		});

		optionsButton = UIUtils.newTextButton("Options", new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				URacer.Game.show(ScreenType.OptionsScreen);
			}
		});

		quitButton = UIUtils.newTextButton("Quit", new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				URacer.Game.quit();
			}
		});

		buttonsTable.row();
		buttonsTable.add(sbTracks).width(300).pad(5);
		buttonsTable.row();
		buttonsTable.add(startGameButton).width(300).height(50).pad(5);
		buttonsTable.row();
		buttonsTable.add(optionsButton).width(300).height(50).pad(5);
		buttonsTable.row();
		buttonsTable.add(quitButton).width(300).height(50).pad(5);

		infoTable = new Table(Art.scrSkin);
		root.addActor(infoTable);

		versionLabel = new Label(URacer.versionInfo, Art.scrSkin);
		infoTable.row();
		infoTable.add(versionLabel).expand().bottom().left();
	}

	@Override
	public void dispose () {
		ui.dispose();
		disable();
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
		} else {
			ui.act(Config.Physics.PhysicsDt);
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
		}

		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		ui.draw();
		// Table.drawDebug(ui);

		if (hasDest) {
			dest.end();
		}
	}

}
