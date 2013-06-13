
package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GhostCarEvent;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.URacerRuntimeException;

/** Implements an automated Car, playing previously recorded events. It will ignore car-to-car collisions, but will respect
 * in-track collisions and responses.
 * 
 * @author manuel */

public final class GhostCar extends Car {
	private static final int FadeEvents = 30;
	private Replay replay;
	private CarForces[] replayForces;
	private int replayForcesCount;
	private int indexPlay;
	private boolean hasReplay;
	public final int id;
	private boolean fadeOutEventTriggered;

	public GhostCar (int id, GameWorld gameWorld, CarPreset.Type presetType) {
		super(gameWorld, CarType.ReplayCar, InputMode.InputFromReplay, presetType, false);
		this.id = id;
		indexPlay = 0;
		hasReplay = false;
		replay = new Replay(-1);
		replayForces = null;
		replayForcesCount = 0;
		stillModel.setAlpha(0.5f);

		setActive(false);
		resetPhysics();
		resetDistanceAndSpeed(true, true);
	}

	// input data for this car cames from a Replay object
	public void setReplay (Replay replay) {
		hasReplay = (replay != null && replay.getEventsCount() > 0 && replay.isValid());
		replayForces = null;
		replayForcesCount = 0;

		if (hasReplay) {
			this.replay.copyData(replay);
		} else {
			this.replay.reset();
		}

		setActive(hasReplay);
		resetPhysics();

		if (hasReplay) {
			replayForces = replay.getCarForces();
			replayForcesCount = replay.getEventsCount();

			stillModel.setAlpha(0);
			restartReplay();

			Gdx.app.log("GhostCar #" + id, "Replaying #" + System.identityHashCode(replay));
		}
	}

	public void restartReplay () {
		if (hasReplay) {
			resetPhysics();
			resetDistanceAndSpeed(true, true);
			setWorldPosMt(replay.getStartPosition(), replay.getStartOrientation());
			indexPlay = 0;
			fadeOutEventTriggered = false;
		}
	}

	public void removeReplay () {
		setReplay(null);
		stillModel.setAlpha(0);
	}

	@Override
	public void resetPhysics () {
		super.resetPhysics();
	}

	public boolean hasReplay () {
		return hasReplay;
	}

	@Override
	public boolean isActive () {
		return super.isActive() && hasReplay;
	}

	@Override
	public boolean isVisible () {
		return isActive() && stillModel.getAlpha() > 0;
	}

	@Override
	protected void onComputeCarForces (CarForces forces) {
		forces.reset();

		if (hasReplay) {
			try {
				forces.set(replayForces[indexPlay]);
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new URacerRuntimeException("!!! MANGLED DATA IN REPLAY !!!");
			}

			// also change opacity, fade in/out based on
			// events played / total events
			if (indexPlay <= FadeEvents) {
				stillModel.setAlpha(((float)indexPlay / (float)FadeEvents) * 0.5f);
			} else if (replay.getEventsCount() - indexPlay <= FadeEvents) {
				float val = (float)(replay.getEventsCount() - indexPlay) / (float)FadeEvents;
				stillModel.setAlpha(val * 0.5f);

				if (!fadeOutEventTriggered) {
					fadeOutEventTriggered = true;
					GameEvents.ghostCars.trigger(this, GhostCarEvent.Type.onGhostFadingOut);
				}
			}
		}
	}

	@Override
	public void onAfterPhysicsSubstep () {
		super.onAfterPhysicsSubstep();

		if (hasReplay) {
			indexPlay++;

			if (indexPlay == replayForcesCount) {
				CarUtils.dumpSpeedInfo("GhostCar #" + id, this, replay.getTrackTime());
				GameEvents.ghostCars.trigger(this, GhostCarEvent.Type.ReplayEnded);
			}
		}
	}
}
