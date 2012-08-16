
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.screens.GameScreenFactory.ScreenType;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.utils.UIUtils;

public final class MainScreen extends Screen {

	private Stage ui;
	private Input input;
	private Table buttonsTable, infoTable;
	private TextButton quitButton, optionsButton, startGameButton;
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
		ui = new Stage();

		// background
		Image bg = new Image(Art.scrBackground);
		bg.setFillParent(true);
		ui.addActor(bg);

		buttonsTable = new Table();
		ui.addActor(buttonsTable);
		// buttonsTable.debug();
		buttonsTable.setFillParent(true);

		startGameButton = UIUtils.newTextButton("Start game", new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
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
		buttonsTable.add(startGameButton).width(300).height(50).pad(5);
		buttonsTable.row();
		buttonsTable.add(optionsButton).width(300).height(50).pad(5);
		buttonsTable.row();
		buttonsTable.add(quitButton).width(300).height(50).pad(5);

		infoTable = new Table(Art.scrSkin);
		ui.addActor(infoTable);

		versionLabel = new Label(URacer.getVersionInformation(), Art.scrSkin);
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
		// Table.drawDebug( ui );

		if (hasDest) {
			dest.end();
		}
	}

}
