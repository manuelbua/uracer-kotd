
package com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.WindowedMean;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;

public class EngineF40 extends EngineSoundSet {
	private static final boolean UseGears = false;
	private static final int MinGear = 1;
	private static final int MaxGear = 6;
	private WindowedMean currRpm, prevRpm;

	// private float[] gears = {3, 1, 0.7f, 0.5f, 0.3f, 0.2f, 0.1f};

	public EngineF40 () {
		super();
		int meanSize = 4;
		currRpm = new WindowedMean(meanSize);
		prevRpm = new WindowedMean(meanSize);

		engine = Sounds.carEngine_f40;
		rpm = 1000;
		gear = MinGear;
	}

	@Override
	public float getGlobalVolume () {
		return 0.1f;
	}

	@Override
	public boolean hasGears () {
		return UseGears;
	}

	@Override
	public float getGearRatio () {
		if (!UseGears) {
			return 3;
		}

		float res = 1;

		//@off
		switch(gear) {
		case 0: res =  1f;
		//
		case 1: res = 3f; 	break;
		case 2: res = 1f;	break;
		case 3: res = 0.7f;	break;
		case 4: res = 0.5f;	break;
		case 5: res = 0.3f;	break;
		case 6: res = 0.2f;	break;
		}
		//@on

		return res * 1f;
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
			// if (load < 0) {
			// rpm += load;
			// } else {
			// rpm += load * getGearRatio();
			// }

			float newrpm = (1000 + 9000 * sf * getGearRatio());
			rpm = AMath.lerp(rpm, newrpm, 1);

			// rpm *= AMath.damping(0.8f);

			prevRpm.addValue(currRpm.getMean());
			currRpm.addValue(rpm);

			// determine direction
			int dir = 0;
			if (prevRpm.getMean() < currRpm.getMean()) dir = 1;
			if (prevRpm.getMean() > currRpm.getMean()) dir = -1;

			// count number of full engine loads til now
			if (dir > 0 && player.isThrottling) {
				// player.isThrottling /* && !player.driftState.isDrifting */
				if (rpm > 9500) {
					onFullLoad();
				}
			} else if (dir < 0) {
				if (rpm < 9500) {
					onDischarge();
				}
			}

			rpm = MathUtils.clamp(rpm, 1000, 10000);

			Gdx.app.log("EngineSoundSet", "gear=" + gear + ", rpm=" + rpm + ", dir=" + dir);// + ", speed=" + sf);

			return rpm;
		}

		return super.updateRpm(load);
	}

	private int lastGear = 0;

	private void onFullLoad () {

		// if (gear != lastGear) {
		// lastGear = gear;
		//
		// switch (gear) {
		// case 1:
		// rpm -= 3000;
		// break;
		// case 2:
		// rpm -= 3000;
		// break;
		// case 3:
		// rpm -= 3000;
		// break;
		// }
		// }

		shiftUp();

		currRpm.clear();
		prevRpm.clear();
	}

	private int lastDisc = 0;

	private void onDischarge () {
		// if (gear != lastDisc) {
		// lastDisc = gear;
		//
		// switch (gear) {
		// case 3:
		// rpm += 2000;
		// break;
		// case 2:
		// rpm += 1000;
		// break;
		// }
		// }

		shiftDown();

		currRpm.clear();
		prevRpm.clear();

		gear--;
		gear = MathUtils.clamp(gear, MinGear, MaxGear);
	}

	@Override
	public void shiftUp () {
		if (UseGears) {
			if (gear > 0 && gear < MaxGear) {
				gear++;

				float dist = 3000;
				if (rpm >= dist + 1000) {
					rpm -= dist;
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

		gear = MathUtils.clamp(gear, MinGear, MaxGear);
	}

	@Override
	public void shiftDown () {
		if (UseGears) {
			// gear = 1;

			// if (gear == 1) {
			// gear--;
			// }

			if (gear > MinGear && gear <= MaxGear) {
				gear--;
				if (rpm != 1000) {
					// rpm += MathUtils.random(-500, 1100);
					rpm += 3000;
				}

				rpm = MathUtils.clamp(rpm, 1000, 10000);
				// Gdx.app.log("EngineF40", "shift down");
			}
		}

		gear = MathUtils.clamp(gear, MinGear, MaxGear);
	}

	@Override
	public void reset () {
		super.reset();

		rpm = 1000;
		gear = 1;
		currRpm.clear();
		prevRpm.clear();
	}
}
