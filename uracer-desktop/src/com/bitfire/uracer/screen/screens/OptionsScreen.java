package com.bitfire.uracer.screen.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectionListener;
import com.badlogic.gdx.scenes.scene2d.ui.tablelayout.Table;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Gameplay;
import com.bitfire.uracer.configuration.Gameplay.TimeDilateInputMode;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.postprocessing.filters.RadialBlur;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.screen.ScreenFactory.ScreenType;
import com.bitfire.uracer.utils.UIUtils;

public class OptionsScreen extends Screen {

	private Stage ui;
	private Input input;

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
		Image bg = new Image( Art.scrBackground );
		bg.width = Gdx.graphics.getWidth();
		bg.height = Gdx.graphics.getHeight();
		ui.addActor( bg );

		Table table = new Table( Art.scrSkin );
		table.debug();
		table.setFillParent( true );
		ui.addActor( table );

		// time dilation input mode
		{
			TimeDilateInputMode im = TimeDilateInputMode.valueOf( UserPreferences.string( Preference.TimeDilateInputMode ) );
			Label timeInputModeLabel = new Label( "Time dilation input mode", Art.scrSkin );
			SelectBox timeInputModeSel = new SelectBox( Art.scrSkin );
			timeInputModeSel.setItems( new String[] { "Touch to toggle", "Touch and release" } );
			timeInputModeSel.setSelection( im.ordinal() );
			timeInputModeSel.setSelectionListener( new SelectionListener() {
				@Override
				public void selected( Actor actor, int index, String value ) {
					UserPreferences.string( Preference.TimeDilateInputMode, Gameplay.TimeDilateInputMode.values()[index].toString() );
					UserPreferences.save();
				}
			} );

			table.add( timeInputModeLabel ).width( 200 ).pad( 5 );
			table.add( timeInputModeSel );
		}

		// post-processing
		{
			final CheckBox ppVignetting = UIUtils.newCheckBox( "Vignetting and gradient mapping", 200, UserPreferences.bool( Preference.Vignetting ) );
			final CheckBox ppBloom = UIUtils.newCheckBox( "Bloom", 100, UserPreferences.bool( Preference.Bloom ) );
			final CheckBox ppZoom = UIUtils.newCheckBox( "Zoom", 100, UserPreferences.bool( Preference.Zoom ) );
			final CheckBox ppZoomBlur = UIUtils.newCheckBox( "Zoom blur", 100, UserPreferences.bool( Preference.ZoomRadialBlur ) );

			ppVignetting.setClickListener( new ClickListener() {
				@Override
				public void click( Actor actor, float x, float y ) {
					UserPreferences.bool( Preference.Vignetting, ppVignetting.isChecked() );
					UserPreferences.save();
				}
			} );

			ppBloom.setClickListener( new ClickListener() {
				@Override
				public void click( Actor actor, float x, float y ) {
					UserPreferences.bool( Preference.Bloom, ppBloom.isChecked() );
					UserPreferences.save();
				}
			} );

			ppZoom.setClickListener( new ClickListener() {
				@Override
				public void click( Actor actor, float x, float y ) {
					UserPreferences.bool( Preference.Zoom, ppZoom.isChecked() );
					UserPreferences.save();
				}
			} );

			ppZoomBlur.setClickListener( new ClickListener() {
				@Override
				public void click( Actor actor, float x, float y ) {
					UserPreferences.bool( Preference.ZoomRadialBlur, ppZoomBlur.isChecked() );
					UserPreferences.save();
				}
			} );

			RadialBlur.Quality rbq = RadialBlur.Quality.valueOf( UserPreferences.string( Preference.ZoomRadialBlurQuality ) );
			final SelectBox ppZoomBlurQ = new SelectBox( Art.scrSkin );
			ppZoomBlurQ.setItems( new String[] { "Very high", "High", "Normal", "Medium", "Low" } );
			ppZoomBlurQ.setSelection( rbq.ordinal() );
			ppZoomBlurQ.setSelectionListener( new SelectionListener() {
				@Override
				public void selected( Actor actor, int index, String value ) {
					UserPreferences.string( Preference.ZoomRadialBlurQuality, RadialBlur.Quality.values()[index].toString() );
					UserPreferences.save();
				}
			} );

			final CheckBox postProcessingCb = UIUtils.newCheckBox( "Enable post-processing effects", 200, UserPreferences.bool( Preference.PostProcessing ) );
			postProcessingCb.setClickListener( new ClickListener() {
				@Override
				public void click( Actor actor, float x, float y ) {
					if( !postProcessingCb.isChecked() ) {
						// disable all post-processing
						ppVignetting.setChecked( false );
						ppBloom.setChecked( false );
						ppZoom.setChecked( false );
						ppZoomBlur.setChecked( false );
					}
					UserPreferences.bool( Preference.PostProcessing, postProcessingCb.isChecked() );
					UserPreferences.bool( Preference.Vignetting, ppVignetting.isChecked() );
					UserPreferences.bool( Preference.Bloom, ppBloom.isChecked() );
					UserPreferences.bool( Preference.Zoom, ppZoom.isChecked() );
					UserPreferences.bool( Preference.ZoomRadialBlur, ppZoomBlur.isChecked() );
					UserPreferences.save();
				}
			} );

			// lay out things
			table.row().colspan( 2 );
			table.add( postProcessingCb );

			table.row().colspan( 2 );
			table.add( ppVignetting );

			table.row().colspan( 2 );
			table.add( ppBloom );

			table.row().colspan( 2 );
			table.add( ppZoom );
			{
				table.row().colspan( 2 );
				table.add( ppZoomBlur );

				table.row();
				table.add( new Label( "Zoom blur quality", Art.scrSkin ) );
				table.add( ppZoomBlurQ );
			}
		}
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
			URacer.Game.show( ScreenType.MainScreen );
			// URacer.Game.quit();
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
