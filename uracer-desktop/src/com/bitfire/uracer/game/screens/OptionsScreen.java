
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bitfire.postprocessing.filters.RadialBlur;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.game.GameplaySettings.TimeDilateInputMode;
import com.bitfire.uracer.game.logic.post.effects.Ssao;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.screen.UIScreen;
import com.bitfire.uracer.utils.UIUtils;

public class OptionsScreen extends UIScreen {

	private Table root, ltable, rtable;
	private CheckBox ppBloom, ppVignetting, ppZoomBlur, ppCrtScreen, ppCurvature, ppSsao;
	private SelectBox<String> ppZoomBlurQuality, ppSsaoQuality;

	@Override
	protected void setupUI (Stage ui) {
		root = UIUtils.newTable();
		// root.debug();
		root.setBounds(0, 0, ui.getWidth(), ui.getHeight());
		root.invalidate();
		ui.addActor(root);

		// background
		Image bg = new Image(Art.scrBackground);
		bg.setFillParent(true);
		root.addActor(bg);
		// root.debug();

		int w = (int)(ui.getWidth() / 2) - 20;
		int h = (int)(ui.getHeight() / 2);

		// version info
		// Table infoTable = UIUtils.newVersionInfoTable();
		// root.addActor(infoTable);
		// root.bottom().padBottom(50);

		// layout tables
		ltable = UIUtils.newTable();
		// ltable.debug();
		ltable.defaults().padLeft(5);
		ltable.align(Align.left | Align.top);
		root.add(ltable).expandX().left().height(h);

		rtable = UIUtils.newTable();
		// rtable.debug();
		rtable.defaults().padRight(5);
		rtable.align(Align.right | Align.top);
		root.add(rtable).expandX().right().height(h);

		/** left table */

		{
			// time dilation input mode

			SelectBox<String> box = UIUtils.newSelectBox(new String[] {"Touch to toggle", "Touch and release"},
				new ChangeListener() {
					@Override
					public void changed (ChangeEvent event, Actor actor) {
						Sounds.menuRollover.play();
						@SuppressWarnings("unchecked")
						int index = ((SelectBox<String>)actor).getSelectedIndex();
						UserPreferences.string(Preference.TimeDilateInputMode, TimeDilateInputMode.values()[index].toString());
						UserPreferences.save();
					}
				});

			TimeDilateInputMode im = TimeDilateInputMode.valueOf(UserPreferences.string(Preference.TimeDilateInputMode));
			box.setSelectedIndex(im.ordinal());

			Label desc = UIUtils.newLabel("Choose your preferred input mode for activating/deactivating "
				+ "the time dilation feature", true);

			ltable.add(desc).width(w).row();
			ltable.add(box).left().padLeft(40).padTop(5);
		}

		{
			// night mode
			CheckBox box = UIUtils.newCheckBox("Enable night mode", UserPreferences.bool(Preference.NightMode),
				new ChangeListener() {
					@Override
					public void changed (ChangeEvent event, Actor actor) {
						Sounds.menuRollover.play();
						UserPreferences.bool(Preference.NightMode, ((CheckBox)actor).isChecked());
						UserPreferences.save();
					}
				});

			Label desc = UIUtils.newLabel("Play the game at night: please note that this mode requires a bit more power "
				+ "from your machine since lights and shadows are computed in real-time", true);

			ltable.row().padTop(20);
			ltable.add(desc).width(w).row();
			ltable.add(box).left().padLeft(40).padTop(5);
		}

		{
			// post-processing switch
			CheckBox box = UIUtils.newCheckBox("Enable post-processing", UserPreferences.bool(Preference.PostProcessing),
				new ChangeListener() {
					@Override
					public void changed (ChangeEvent event, Actor actor) {
						Sounds.menuRollover.play();
						boolean doPostprocess = ((CheckBox)actor).isChecked();
						UserPreferences.bool(Preference.PostProcessing, doPostprocess);
						UserPreferences.save();

						ppBloom.setDisabled(!doPostprocess);
						ppVignetting.setDisabled(!doPostprocess);
						ppCrtScreen.setDisabled(!doPostprocess);
						ppCurvature.setDisabled(!doPostprocess);
						ppZoomBlur.setDisabled(!doPostprocess);
						ppSsao.setDisabled(!doPostprocess);
					}
				});

			Label desc = UIUtils.newLabel("Post-processing effects will enhance your gaming experience greatly, but this comes at "
				+ "the expense of major performance penalties if your system isn't up to the task", true);

			ltable.row().padTop(20);
			ltable.add(desc).width(w).row();
			ltable.add(box).left().padLeft(40).padTop(5);
		}

		/** right table */

		{
			// post-processing effects
			Label desc = UIUtils.newLabel("Enable or disable post-processing effects to find the best match between performance "
				+ "and visual quality on your machine:", true);
			rtable.add(desc).width(w).row();

			// bloom
			ppBloom = UIUtils.newCheckBox("Full-scene bloom", UserPreferences.bool(Preference.Bloom), new ChangeListener() {
				@Override
				public void changed (ChangeEvent event, Actor actor) {
					Sounds.menuRollover.play();
					UserPreferences.bool(Preference.Bloom, ((CheckBox)actor).isChecked());
					UserPreferences.save();
				}
			});
			ppBloom.setDisabled(!UserPreferences.bool(Preference.PostProcessing));

			// vignetting
			ppVignetting = UIUtils.newCheckBox("Vignette and gradient mapping", UserPreferences.bool(Preference.Vignetting),
				new ChangeListener() {
					@Override
					public void changed (ChangeEvent event, Actor actor) {
						Sounds.menuRollover.play();
						UserPreferences.bool(Preference.Vignetting, ((CheckBox)actor).isChecked());
						UserPreferences.save();
					}
				});
			ppVignetting.setDisabled(!UserPreferences.bool(Preference.PostProcessing));

			// crt emulation
			ppCrtScreen = UIUtils.newCheckBox("CRT screen emulation", UserPreferences.bool(Preference.CrtScreen),
				new ChangeListener() {
					@Override
					public void changed (ChangeEvent event, Actor actor) {
						Sounds.menuRollover.play();
						UserPreferences.bool(Preference.CrtScreen, ((CheckBox)actor).isChecked());
						UserPreferences.save();
					}
				});
			ppCrtScreen.setDisabled(!UserPreferences.bool(Preference.PostProcessing));

			// earth curvature
			ppCurvature = UIUtils.newCheckBox("Earth curvature", UserPreferences.bool(Preference.EarthCurvature),
				new ChangeListener() {
					@Override
					public void changed (ChangeEvent event, Actor actor) {
						Sounds.menuRollover.play();
						UserPreferences.bool(Preference.EarthCurvature, ((CheckBox)actor).isChecked());
						UserPreferences.save();
					}
				});
			ppCurvature.setDisabled(!UserPreferences.bool(Preference.PostProcessing));

			// radial zoom blur
			{
				ppZoomBlur = UIUtils.newCheckBox("Zoom blur", UserPreferences.bool(Preference.ZoomRadialBlur), new ChangeListener() {
					@Override
					public void changed (ChangeEvent event, Actor actor) {
						Sounds.menuRollover.play();
						UserPreferences.bool(Preference.ZoomRadialBlur, ((CheckBox)actor).isChecked());
						UserPreferences.save();
					}
				});
				ppZoomBlur.setDisabled(!UserPreferences.bool(Preference.PostProcessing));

				ppZoomBlurQuality = UIUtils.newSelectBox(new String[] {"Very high", "High", "Normal", "Medium", "Low"},
					new ChangeListener() {
						@Override
						public void changed (ChangeEvent event, Actor actor) {
							Sounds.menuRollover.play();
							@SuppressWarnings("unchecked")
							int index = ((SelectBox<String>)actor).getSelectedIndex();
							UserPreferences.string(Preference.ZoomRadialBlurQuality, RadialBlur.Quality.values()[index].toString());
							UserPreferences.save();
						}
					});
				// ppZoomBlurQuality.setDisabled(!UserPreferences.bool(Preference.PostProcessing));

				RadialBlur.Quality quality = RadialBlur.Quality.valueOf(UserPreferences.string(Preference.ZoomRadialBlurQuality));
				ppZoomBlurQuality.setSelectedIndex(quality.ordinal());
			}

			// SSAO
			{
				ppSsao = UIUtils.newCheckBox("SSAO (ambient occlusion)", UserPreferences.bool(Preference.Ssao), new ChangeListener() {
					@Override
					public void changed (ChangeEvent event, Actor actor) {
						Sounds.menuRollover.play();
						UserPreferences.bool(Preference.Ssao, ((CheckBox)actor).isChecked());
						UserPreferences.save();
					}
				});
				ppSsao.setDisabled(!UserPreferences.bool(Preference.PostProcessing));

				String[] qualityList = new String[Ssao.Quality.values().length];
				int idx = 0;
				for (Ssao.Quality q : Ssao.Quality.values()) {
					qualityList[idx++] = q.toString();
				}

				ppSsaoQuality = UIUtils.newSelectBox(qualityList, new ChangeListener() {
					@Override
					public void changed (ChangeEvent event, Actor actor) {
						Sounds.menuRollover.play();
						@SuppressWarnings("unchecked")
						int index = ((SelectBox<String>)actor).getSelectedIndex();
						UserPreferences.string(Preference.SsaoQuality, Ssao.Quality.values()[index].toString());
						UserPreferences.save();
					}
				});
				// ppSsaoQuality.setDisabled(!UserPreferences.bool(Preference.PostProcessing));

				Ssao.Quality quality = Ssao.Quality.valueOf(UserPreferences.string(Preference.SsaoQuality));
				ppSsaoQuality.setSelectedIndex(quality.ordinal());
			}

			// back button
			Button back = UIUtils.newTextButton("Back to main", new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					Sounds.menuClick.play();
					URacer.Game.show(ScreenType.MainScreen);
				}
			});

			rtable.add(ppBloom).left().padLeft(40).padTop(5).row();
			rtable.add(ppVignetting).left().padLeft(40).row();
			rtable.add(ppCrtScreen).left().padLeft(40).row();
			rtable.add(ppCurvature).left().padLeft(40).row();

			rtable.add(ppZoomBlur).left().padLeft(40).row();
			rtable.add(ppZoomBlurQuality).left().padLeft(80).padTop(5).row();

			rtable.add(ppSsao).left().padLeft(40).padTop(5).row();
			rtable.add(ppSsaoQuality).left().padLeft(80).padTop(5).row();
			rtable.add(back).expandY().bottom().right();
		}
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
			URacer.Game.show(ScreenType.MainScreen);
		}
		if (input.isPressed(Keys.R)) {
			reload();
		} else {
			ui.act(Config.Physics.Dt);
		}
	}

	@Override
	public void tickCompleted () {
	}

	@Override
	protected void draw () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		ui.draw();
		// Table.drawDebug(ui);
	}
}
