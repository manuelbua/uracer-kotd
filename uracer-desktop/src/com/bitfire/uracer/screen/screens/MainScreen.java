package com.bitfire.uracer.screen.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.tablelayout.Table;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.screen.ScreenFactory.ScreenType;
import com.bitfire.uracer.utils.UIUtils;

public final class MainScreen extends Screen {

	private Stage ui;
	private Input input;
	private Image bg;

	@Override
	public void init( ScalingStrategy scalingStrategy ) {
		input = URacer.Game.getInputSystem();
		setupUI();
	}

	@Override
	public void enable() {
		Gdx.input.setInputProcessor( ui );
	}

	private void setupUI() {
		ui = new Stage( Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false );

		// background
		bg = new Image( Art.scrBackground );
		bg.setFillParent( true );
		ui.addActor( bg );

		Table buttonsTable = new Table( Art.scrSkin );
		buttonsTable.debug();
		buttonsTable.setFillParent( true );
		ui.addActor( buttonsTable );

		TextButton startGameButton = UIUtils.newTextButton( "Start game", new ClickListener() {
			@Override
			public void click( Actor actor, float x, float y ) {
				URacer.Game.show( ScreenType.GameScreen );
			}
		} );

		TextButton optionsButton = UIUtils.newTextButton( "Options", new ClickListener() {
			@Override
			public void click( Actor actor, float x, float y ) {
				URacer.Game.show( ScreenType.OptionsScreen );
			}
		} );

		TextButton quitButton = UIUtils.newTextButton( "Quit", new ClickListener() {
			@Override
			public void click( Actor actor, float x, float y ) {
				URacer.Game.quit();
			}
		} );

		buttonsTable.add( startGameButton ).width( 300 ).height( 50 ).pad( 5 );
		buttonsTable.row();
		buttonsTable.add( optionsButton ).width( 300 ).height( 50 ).pad( 5 );
		buttonsTable.row();
		buttonsTable.add( quitButton ).width( 300 ).height( 50 ).pad( 5 );

		Table infoTable = new Table( Art.scrSkin );
		// infoTable.debug();
		infoTable.setFillParent( true );
		ui.addActor( infoTable );

		Label versionLabel = new Label( URacer.getVersionInformation(), Art.scrSkin );
		infoTable.row();
		infoTable.add( versionLabel ).expand().bottom().left();
	}

	@Override
	public void dispose() {
		ui.dispose();
		disable();
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void tick() {
		if( input.isPressed( Keys.Q ) || input.isPressed( Keys.BACK ) || input.isPressed( Keys.ESCAPE ) ) {
			URacer.Game.quit();
		}
	}

	@Override
	public void tickCompleted() {
	}

	@Override
	public void render( FrameBuffer dest ) {
		boolean hasDest = (dest != null);
		if( hasDest ) {
			dest.begin();
		}

		Gdx.gl.glClear( GL20.GL_COLOR_BUFFER_BIT );
		Gdx.gl.glClearColor( 0, 0, 0, 0 );
		ui.draw();
		Table.drawDebug( ui );

		if( hasDest ) {
			dest.end();
		}
	}

}
