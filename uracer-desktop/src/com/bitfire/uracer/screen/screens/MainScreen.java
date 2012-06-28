package com.bitfire.uracer.screen.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.tablelayout.Table;
import com.badlogic.gdx.scenes.scene2d.ui.tablelayout.TableLayout;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.screen.ScreenFactory.ScreenType;

public class MainScreen extends Screen {

	private Stage ui;
	private Input input;

	private void setupUI() {
		ui = new Stage( Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false );
		Gdx.input.setInputProcessor( ui );

		// background
		Image bg = new Image( Art.scrBackground );
		bg.width = Gdx.graphics.getWidth();
		bg.height = Gdx.graphics.getHeight();
		ui.addActor( bg );

		Table table = new Table( Art.scrSkin );
		table.debug();
		table.width = Gdx.graphics.getWidth();
		table.height = Gdx.graphics.getHeight();
		ui.addActor( table );
		TableLayout layout = table.getTableLayout();

		TextButton startGameButton = new TextButton( "Start game", Art.scrSkin );
		startGameButton.setClickListener( new ClickListener() {
			@Override
			public void click( Actor actor, float x, float y ) {
				URacer.Game.show( ScreenType.GameScreen );
			}
		} );

		TextButton optionsButton = new TextButton( "Options", Art.scrSkin );
		optionsButton.setClickListener( new ClickListener() {
			@Override
			public void click( Actor actor, float x, float y ) {
				Gdx.app.log( "MainScreen", "soon..." );
			}
		} );

		TextButton quitButton = new TextButton( "Quit", Art.scrSkin );
		quitButton.setClickListener( new ClickListener() {
			@Override
			public void click( Actor actor, float x, float y ) {
				URacer.Game.quit();
			}
		} );

		layout.register( "startGameButton", startGameButton );
		layout.register( "optionsButton", optionsButton );
		layout.register( "quitButton", quitButton );
		layout.parse( Art.scrDefMain );
	}

	@Override
	public void init( ScalingStrategy scalingStrategy ) {
		input = URacer.Game.getInputSystem();
		setupUI();
	}

	@Override
	public void dispose() {
		ui.dispose();
		Gdx.input.setInputProcessor( null );
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void tick() {
		if( input.isPressed( Keys.Q )  || input.isPressed( Keys.BACK ) || input.isPressed( Keys.ESCAPE ) ) {
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
