
package com.bitfire.uracer.game.debug.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.debug.DebugHelper.RenderFlags;
import com.bitfire.uracer.game.debug.DebugRenderable;
import com.bitfire.uracer.game.logic.gametasks.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffect;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffectType;
import com.bitfire.uracer.game.player.DriftState;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.CarUtils;

public class DebugPlayer extends DebugRenderable {
	private DriftState driftState;
	private final TrackEffect smokeTrails;
	private final TrackEffect skidMarks;

	private DebugMeter meterDriftStrength, meterSkidMarks, meterSpeed, meterSmokeTrails;

	private Array<DebugMeter> meters = new Array<DebugMeter>();
	private Vector2 pos = new Vector2();

	public DebugPlayer (RenderFlags flag, GameTasksManager manager) {
		super(flag);
		skidMarks = manager.effects.getEffect(TrackEffectType.CarSkidMarks);
		smokeTrails = manager.effects.getEffect(TrackEffectType.CarSmokeTrails);

		// meter lateral forces
		meterDriftStrength = new DebugMeter(100, 5);
		meterDriftStrength.setLimits(0, 1);
		meterDriftStrength.setName("drift strength");
		meters.add(meterDriftStrength);

		// meter skid marks count
		if (skidMarks != null) {
			meterSkidMarks = new DebugMeter(100, 5);
			meterSkidMarks.setLimits(0, skidMarks.getMaxParticleCount());
			meterSkidMarks.setName("skid marks");
			meters.add(meterSkidMarks);
		}

		// meter smoke trails count
		if (smokeTrails != null) {
			meterSmokeTrails = new DebugMeter(100, 5);
			meterSmokeTrails.setLimits(0, smokeTrails.getMaxParticleCount());
			meterSmokeTrails.setName("smoke trails");
			meters.add(meterSmokeTrails);
		}

		// player speed, km/h
		meterSpeed = new DebugMeter(100, 5);
		meterSpeed.setName("speed km/h");
		meters.add(meterSpeed);
	}

	@Override
	public void dispose () {
		for (DebugMeter m : meters) {
			m.dispose();
		}
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);
		if (hasPlayer) {
			this.player = player;
			driftState = player.driftState;
			meterSpeed.setLimits(0, CarUtils.mtSecToKmHour(player.getCarModel().max_speed));
		}
	}

	private boolean isActive () {
		return hasPlayer;
	}

	@Override
	public void tick () {
		if (isActive()) {
			// lateral forces
			meterDriftStrength.setValue(driftState.driftStrength);

			if (driftState.isDrifting) {
				meterDriftStrength.color.set(.3f, 1f, .3f, 1f);
			} else {
				meterDriftStrength.color.set(1f, 1f, 1f, 1f);
			}

			// skid marks count
			if (skidMarks != null) {
				meterSkidMarks.setValue(skidMarks.getParticleCount());
			}

			// smoke trails count
			if (smokeTrails != null) {
				meterSmokeTrails.setValue(smokeTrails.getParticleCount());
			}

			// player's speed
			meterSpeed.setValue(CarUtils.mtSecToKmHour(player.getInstantSpeed()));
		}
	}

	@Override
	public void renderBatch (SpriteBatch batch) {
		if (isActive()) {
			float prevHeight = 0;
			int index = 0;
			for (DebugMeter m : meters) {
				pos.set(GameRenderer.ScreenUtils.worldPxToScreen(player.state().position));
				pos.x += 100;
				pos.y += 0;

				// offset by index
				pos.y += index * (prevHeight + Art.DebugFontHeight);

				m.setPosition(pos);
				m.render(batch);

				index++;
				prevHeight = m.getHeight();
			}
		}
	}

	@Override
	public void reset () {
	}
}
