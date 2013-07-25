
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import net.sourceforge.jFuzzyLogic.FIS;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;

public final class PlayerEngineSoundEffect extends SoundEffect {
	private int NumTracks = 7;
	private Sound[] engine = new Sound[NumTracks];
	private long[] mid = new long[NumTracks];
	private boolean[] started = new boolean[NumTracks];

	// car engine data
	private double[] gears = {3, 1, 0.7, 0.5, 0.3, 0.2, 0.1}; // set gear multiplikators
	private int rpm, gear;

	// inference engine
	private FIS feLoad;
	private FIS feIdle, feOnLow, feOnMid, feOnHigh, feOffLow, feOffMid, feOffHigh;

	public PlayerEngineSoundEffect () {
		engine = Sounds.carEngine;
		rpm = 1000;
		gear = 0;

		//@off
		feLoad = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineLoad.fcl", FileType.Internal).read(), true);
		feIdle = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolIdle.fcl", FileType.Internal).read(), true);
		feOnLow = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOnLow.fcl", FileType.Internal).read(), true);
		feOnMid= FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOnMid.fcl", FileType.Internal).read(), true);
		feOnHigh = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOnHigh.fcl", FileType.Internal).read(), true);
		feOffLow = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOffLow.fcl", FileType.Internal).read(), true);
		feOffMid= FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOffMid.fcl", FileType.Internal).read(), true);
		feOffHigh = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOffHigh.fcl", FileType.Internal).read(), true);
		//@on
	}

	@Override
	public void dispose () {
		stop();
	}

	private double fuzzyLoadCompute () {
		feLoad.evaluate();
		return feLoad.getVariable("load").getValue();
	}

	private void fuzzyLoadUpdateInput (double throttle, double rpm) {
		feLoad.setVariable("throttle", throttle);
		feLoad.setVariable("rpm", rpm);
	}

	private int updateRpm (double load, int prev_rpm) {
		int result = 10000;
		if (rpm < 10000) {
			result = (int)((float)prev_rpm + (load * gears[gear]));
		} else {
			if (load < 0) {
				result = (int)((float)prev_rpm + load);
			}
		}

		return result;
	}

	private void updateVolume (int track, FIS fuzzyEngine, double load, double rpm) {
		fuzzyEngine.setVariable("load", load);
		fuzzyEngine.setVariable("rpm", rpm);
		fuzzyEngine.evaluate();
		double volume = fuzzyEngine.getVariable("volume").getValue() / 100;
		if (volume >= 0 && volume <= 1) {
			setVolume(track, (float)volume);
		}
	}

	private void updateVolumes (double load, double rpm) {
		updateVolume(0, feIdle, load, rpm);
		updateVolume(1, feOnLow, load, rpm);
		updateVolume(2, feOnMid, load, rpm);
		updateVolume(3, feOnHigh, load, rpm);
		updateVolume(4, feOffLow, load, rpm);
		updateVolume(5, feOffMid, load, rpm);
		updateVolume(6, feOffHigh, load, rpm);
	}

	private float xnaToAl (float pitch) {
		return (float)Math.pow(2, 0.69314718 * pitch);
	}

	private void setXnaPitch (int track, float pitch) {
		setPitch(track, xnaToAl(pitch));
	}

	private void updatePitches (float rpm) {
		// sample specific
		if (rpm < 6000) {
			setXnaPitch(0, rpm / 6000);
			setXnaPitch(1, rpm / 10000 + 0.2f);
		}

		setXnaPitch(2, rpm / 10000 - 0.4f);
		setXnaPitch(3, rpm / 10000 - 0.8f);
		setXnaPitch(4, rpm / 10000);
		setXnaPitch(5, rpm / 10000 - 0.8f);
		setXnaPitch(6, rpm / 10000 - 0.8f);
	}

	private void updateGear () {
		if (player.isThrottling) {
			switch (gear) {
			case 0:
				if (rpm > 2000) gear++;
				break;
			case 1:
				if (rpm > 3500) gear++;
				break;
			case 2:
				if (rpm > 4500) gear++;
				break;
			case 3:
				if (rpm > 6000) gear++;
				break;
			case 4:
				if (rpm > 7500) gear++;
				break;
			case 5:
				if (rpm > 9000) gear++;
				break;
			}

		} else {
			switch (gear) {
			case 6:
				if (rpm < 9000) gear--;
				break;
			case 5:
				if (rpm < 7500) gear--;
				break;
			case 4:
				if (rpm < 6000) gear--;
				break;
			case 3:
				if (rpm < 4500) gear--;
				break;
			case 2:
				if (rpm < 3500) gear--;
				break;
			case 1:
				if (rpm < 2000) gear--;
				break;
			}

		}

		gear = MathUtils.clamp(gear, 1, 6);
	}

	@Override
	public void tick () {
		updateGear();
		double load = fuzzyLoadCompute();

		// update throttle and rpms, set them as input for the next-frame load computation
		double throttle = player.carState.currSpeedFactor * 100;
		Gdx.app.log("", "engine load=" + load + ", rpm=" + rpm + ", th=" + throttle + ", g=" + gear);

		rpm = updateRpm(load, rpm);
		fuzzyLoadUpdateInput(throttle, rpm);

		// compute volumes
		updateVolumes(load, rpm);
		updatePitches(rpm);
	}

	@Override
	public void stop () {
		for (int i = 0; i < NumTracks; i++) {
			stop(i);
		}
	}

	@Override
	public void gameReset () {
		stop();
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);

		if (hasPlayer) {
			start();
		} else {
			stop();
		}
	}

	private void start (int track) {
		if (started[track]) {
			return;
		}

		started[track] = true;
		mid[track] = loop(engine[track], 0);
		setVolume(track, 0);
	}

	private void start () {
		for (int i = 0; i < NumTracks; i++) {
			start(i);
		}
	}

	public void stop (int track) {
		if (!started[track]) {
			return;
		}

		started[track] = false;
		if (mid[track] > -1) {
			engine[track].stop(mid[track]);
		}
	}

	//

	private void setVolume (int track, float vol) {
		engine[track].setVolume(mid[track], vol);
	}

	private void setPitch (int track, float pitch) {
		engine[track].setPitch(mid[track], pitch);
	}

	// // idle
	// setVolume(0, 0.3f);
	// setPitch(0, 1f); // 1 -> 1.85f (switch to #1)
	//
	// // on, low
	// setVolume(1, 0.3f);
	// setPitch(1, 1f); // 1 -> 1.5f (switch to #2)
	//
	// setVolume(4, 0f);
	// setPitch(4, 1.5f);
	//
	// // on, mid
	// setVolume(2, 0f);
	// setPitch(2, 1f); // 1 -> 1.33f (switch to #3)
	// setVolume(5, 0f);
	// setPitch(5, 1f);
	//
	// // on, high
	// setVolume(3, 0f);
	// setPitch(3, 1f);
	// setVolume(6, 0f);
	// setPitch(6, 1f);
}
