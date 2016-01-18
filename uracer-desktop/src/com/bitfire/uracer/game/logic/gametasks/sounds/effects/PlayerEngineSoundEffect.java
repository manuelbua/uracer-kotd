
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent.Order;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines.EngineF40;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines.EngineSoundSet;
import com.bitfire.uracer.game.logic.helpers.TrackProgressData;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.utils.AMath;

import net.sourceforge.jFuzzyLogic.FIS;

public final class PlayerEngineSoundEffect extends SoundEffect {

	// inference engine
	private FIS feLoad;
	private float load;
	private float throttle;

	// throttle autosoftener
	private static final boolean ThrottleAutoSoftener = true;
	private Time driftTimer = new Time();
	private static final int MinSoftnessTicks = 5;
	private static final int MaxSoftnessTicks = 20;
	private int softnessTicks = 0;

	private EngineSoundSet soundset;

	public PlayerEngineSoundEffect (TrackProgressData progressData) {
		feLoad = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineLoad.fcl", FileType.Internal).read(), true);
		load = 0;
		throttle = 0;
		soundset = new EngineF40(progressData);
	}

	public EngineSoundSet getSoundSet () {
		return soundset;
	}

	private PlayerDriftStateEvent.Listener playerListener = new PlayerDriftStateEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			if (!hasPlayer) return;
			switch (type) {
			case onBeginDrift:
				if (player.isThrottling) {
					// while (soundset.hasGears() && soundset.getGear() > 2) {
					// soundset.shiftDown();
					// }

					driftTimer.start();
					float ratio = player.carState.currSpeedFactor;
					softnessTicks = (int)(ratio * (float)MaxSoftnessTicks);
					softnessTicks = MathUtils.clamp(softnessTicks, MinSoftnessTicks, MaxSoftnessTicks);
					// Gdx.app.log("", "st=" + softnessTicks);
					// Gdx.app.log("", "BEGIN DRIFT");
				}
				break;
			case onEndDrift:
				driftTimer.stop();
				break;
			}
		}
	};

	private void attach () {
		GameEvents.driftState.addListener(playerListener, PlayerDriftStateEvent.Type.onBeginDrift);
		GameEvents.driftState.addListener(playerListener, PlayerDriftStateEvent.Type.onEndDrift);
		GameEvents.playerCar.addListener(carListener, CarEvent.Type.onCollision);
		GameEvents.playerCar.addListener(carListener, CarEvent.Type.onOutOfTrack);
		GameEvents.playerCar.addListener(carListener, CarEvent.Type.onBackInTrack);
	}

	private CarEvent.Listener carListener = new CarEvent.Listener() {
		@SuppressWarnings("incomplete-switch")
		@Override
		public void handle (Object source, CarEvent.Type type, CarEvent.Order order) {
			switch (type) {
			case onCollision:
				// soundset.reset();
				throttle = 0;
				break;
			case onOutOfTrack:
				// soundset.shiftDown();
				break;
			case onBackInTrack:
				break;
			}
		}
	};

	private void detach () {
		GameEvents.driftState.removeListener(playerListener, PlayerDriftStateEvent.Type.onBeginDrift);
		GameEvents.driftState.removeListener(playerListener, PlayerDriftStateEvent.Type.onEndDrift);
		GameEvents.playerCar.removeListener(carListener, CarEvent.Type.onCollision);
		GameEvents.playerCar.removeListener(carListener, CarEvent.Type.onOutOfTrack);
		GameEvents.playerCar.removeListener(carListener, CarEvent.Type.onBackInTrack);
	}

	@Override
	public void dispose () {
		detach();
		stop();
	}

	private float fuzzyLoadCompute (float throttle, float rpm) {
		feLoad.setVariable("throttle", throttle);
		feLoad.setVariable("rpm", rpm);
		feLoad.evaluate();
		return (float)feLoad.getVariable("load").getValue();
	}

	@Override
	public void tick () {
		if (!hasPlayer || isPaused) return;

		// if (outOfTrack) {
		// soundset.shiftDown();
		// }

		if (player.isThrottling) {
			if (soundset.hasGears()) {
				throttle += 8f;
			} else {
				throttle += 10;
			}

			if (!soundset.hasGears() && ThrottleAutoSoftener && !driftTimer.isStopped()
				&& driftTimer.elapsed().ticks < softnessTicks) {
				throttle *= AMath.damping(0.8f);
				Gdx.app.log("", "ticks=" + driftTimer.elapsed().ticks);
			}
		} else {
			if (soundset.hasGears()) {
				// avoid sound fading slipping over the off-engine samples
				// throttle = 0;
				// throttle *= AMath.damping(0.94f);
				throttle *= AMath.damping(0.85f);
			} else {
				throttle *= AMath.damping(0.8f);
			}
		}

		throttle = AMath.fixup(throttle);
		throttle = MathUtils.clamp(throttle, 0, 100);

		float rpm = soundset.updateRpm(load);
		load = AMath.fixup(fuzzyLoadCompute(throttle, rpm));

		// Gdx.app.log("", "engine load=" + load + ", rpm=" + rpm + ", th=" + throttle + ", g=" + soundset.getGear() + ", sf="+
		// player.carState.currSpeedFactor);
		// Gdx.app.log("", "engine load=" + load + ", rpm=" + rpm + ", th=" + throttle + ", g=" + soundset.getGear());
		// Gdx.app.log("", "engine load=" + load + ", rpm=" + rpm + ", th=" + throttle + ", sf=" + player.carState.currSpeedFactor);

		// compute volumes
		soundset.updateVolumes(load);
		soundset.updatePitches();
	}

	@Override
	public void stop () {
		soundset.stop();
	}

	@Override
	public void gamePause () {
		super.gamePause();
		soundset.stop();
	}

	@Override
	public void gameResume () {
		super.gameResume();
		soundset.start();
	}

	@Override
	public void gameReset () {
		gameRestart();
	}

	@Override
	public void gameRestart () {
		soundset.reset();
		soundset.stop();
		soundset.start();
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);

		soundset.setPlayer(player);

		if (hasPlayer) {
			attach();
			soundset.start();
		} else {
			soundset.stop();
			detach();
		}
	}
}
