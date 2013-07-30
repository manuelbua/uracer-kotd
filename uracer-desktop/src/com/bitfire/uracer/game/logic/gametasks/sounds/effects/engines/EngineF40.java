
package com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;

public class EngineF40 extends EngineSoundSet {
	private static final boolean UseGears = false;

	// private float[] gears = {3, 1, 0.7f, 0.5f, 0.3f, 0.2f, 0.1f};

	public EngineF40 () {
		super();
		engine = Sounds.carEngine_f40;
		rpm = 1000;
		gear = 1;
	}

	@Override
	public float getGlobalVolume () {
		return 0.2f;
	}

	@Override
	public float getGearRatio () {
		// dbg

		//@off
		switch(gear) {
//		case 0: return 3;
//		case 1: return 1;
//		case 2: return 0.7f;
//		case 3: return 0.5f;
//		case 4: return 0.3f;
//		case 5: return 0.2f;
//		case 6: return 0.1f;
		case 1: return 3f;
		case 2: return 1f;
		case 3: return 0.7f;
		case 4: return 0.5f;
		case 5: return 0.3f;
		case 6: return 0.2f;
		}
		//@on

		return 1;

		// return gears[gear];
	}

	@Override
	public void updatePitches () {
		float lowLimit = 6000f;
		float hiLimit = 10000f;

		float rpmLow = rpm / lowLimit;
		float rpmDef = rpm / hiLimit;

		// sample specific
		if (rpm < lowLimit) {
			setXnaPitch(0, rpmLow);
			setXnaPitch(1, rpmDef + 0.2f);
		}

		setXnaPitch(2, rpmDef - 0.4f);
		setXnaPitch(3, rpmDef - 0.8f);
		setXnaPitch(4, rpmDef);
		setXnaPitch(5, rpmDef - 0.8f);
		setXnaPitch(6, rpmDef - 0.8f);
	}

	@Override
	public float updateRpm (float load) {
		if (UseGears) {
			if (!hasPlayer) {
				rpm = 1000;
				return 1000;
			}

			float sf = player.carState.currSpeedFactor;

			// more accurate representation
			if (load < 0) {
				rpm += load * sf;
			} else {
				rpm += load * getGearRatio() * sf;
			}

			rpm *= AMath.damping(0.995f);
			rpm = MathUtils.clamp(rpm, 1000, 10000);

			Gdx.app.log("EngineSoundSet", "gear=" + gear + ", rpm=" + rpm);// + ", speed=" + sf);

			// count number of full engine loads til now
			if (player.isThrottling && !player.driftState.isDrifting) {
				if (rpm > 9500) {
					shiftUp();
				}
			} else {
				if (!player.driftState.isDrifting && rpm < 5000) {
					// gear = 1;
					shiftDown();
				}
			}

			return rpm;
		}

		return super.updateRpm(load);
	}

	@Override
	public void shiftUp () {
		if (gear > 0 && gear < 6) {
			gear++;

			if (rpm >= 4000) {
				rpm -= 3000;
				// rpm -= rpm * 0.75f;
			} else {
				rpm = 1000;
			}
		}

		if (gear == 0) {
			gear++;
			rpm = 1000;
		}

		rpm = MathUtils.clamp(rpm, 1000, 10000);
	}

	@Override
	public void shiftDown () {
		// gear = 1;

		// if (gear == 1) {
		// gear--;
		// }

		if (gear > 1 && gear <= 6) {
			gear--;
			if (rpm != 1000) {
				rpm += 3000;
			}

			rpm = MathUtils.clamp(rpm, 1000, 10000);
			Gdx.app.log("EngineF40", "shift down");
		}

	}
}
