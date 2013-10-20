
package com.bitfire.uracer.game.actors;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenEquation;

import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GhostCarEvent;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;

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
	private final int id;
	private boolean fadeOutEventTriggered, startedEventTriggered;
	private boolean started;
	private BoxedFloat bfAlpha;

	public GhostCar (int id, GameWorld gameWorld, CarPreset.Type presetType) {
		super(gameWorld, null, CarType.ReplayCar, InputMode.InputFromReplay, presetType, false);
		this.id = id;
		started = false;
		replay = new Replay();
		resetDistanceAndSpeed(true, true);
		removeReplay();
		bfAlpha = new BoxedFloat(Config.Graphics.DefaultGhostCarOpacity);
		stillModel.setAlpha(0);
		getTrackState().ghostArrived = false;
	}

	public int getId () {
		return id;
	}

	public Replay getReplay () {
		return replay;
	}

	/** starts playing the available Replay, if any */
	public void start () {
		if (!started) {
			indexPlay = 0;
			startedEventTriggered = false;
			fadeOutEventTriggered = false;
			stillModel.setAlpha(0);
			resetWithTrackState();
			setActive(true);
			getTrackState().ghostArrived = false;
			started = true;
		}
	}

	/** stops playing the replay and returns to being idle */
	public void stop () {
		if (started) {
			started = false;
			stillModel.setAlpha(0);
			setActive(false);
			resetPhysics();
		}
	}

	public boolean isPlaying () {
		return started;
	}

	public void setAlpha (float alpha) {
		bfAlpha.value = alpha;
	}

	public float getAlpha () {
		return bfAlpha.value;
	}

	public void tweenAlphaTo (float value) {
		tweenAlphaTo(value, Config.Graphics.DefaultGhostOpacityChangeMs, Config.Graphics.DefaultGhostOpacityChangeEq);
	}

	public void tweenAlphaTo (float value, float ms) {
		tweenAlphaTo(value, ms, Config.Graphics.DefaultGhostOpacityChangeEq);
	}

	public void tweenAlphaTo (float value, float ms, TweenEquation eq) {
		GameTweener.stop(bfAlpha);
		Timeline timeline = Timeline.createSequence();
		timeline.push(Tween.to(bfAlpha, BoxedFloatAccessor.VALUE, ms).target(value).ease(eq));
		GameTweener.start(timeline);
	}

	public boolean isSsaoReady () {
		return bfAlpha.value > 0.5f;
	}

	// input data for this car comes from a Replay object
	public void setReplay (Replay replay) {
		stop();

		hasReplay = (replay != null && replay.getEventsCount() > 0 && replay.isValid());
		replayForces = null;
		replayForcesCount = 0;

		if (hasReplay) {
			this.replay.copy(replay);
		} else {
			this.replay.reset();
		}

		if (hasReplay) {
			replayForces = replay.getCarForces();
			replayForcesCount = replay.getEventsCount();
			resetWithTrackState();
			indexPlay = 0;
			fadeOutEventTriggered = false;
			startedEventTriggered = false;
			stillModel.setAlpha(0);
			bfAlpha.value = 0;
		}
	}

	private void resetWithTrackState () {
		getTrackState().ghostArrived = false;

		resetPhysics();
		resetDistanceAndSpeed(true, true);

		if (hasReplay) {
			setWorldPosMt(replay.getStartPosition(), replay.getStartOrientation());
			gameTrack.resetTrackState(this);
		}
	}

	public void removeReplay () {
		stop();
		setReplay(null);
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
		// returns empty forces in case its not started nor ready
		forces.reset();

		if (started && hasReplay) {

			if (!startedEventTriggered) {
				startedEventTriggered = true;
				GameEvents.ghostCars.trigger(this, GhostCarEvent.Type.ReplayStarted);
			}

			if (indexPlay < replayForcesCount) {
				forces.set(replayForces[indexPlay]);
			}

			stillModel.setAlpha(bfAlpha.value);

			// also change opacity, fade in/out based on events played / total events
			if (indexPlay <= FadeEvents) {
				stillModel.setAlpha(((float)indexPlay / (float)FadeEvents) * bfAlpha.value);
			} else if (replay.getEventsCount() - indexPlay <= FadeEvents) {
				float val = (float)(replay.getEventsCount() - indexPlay) / (float)FadeEvents;
				stillModel.setAlpha(val * bfAlpha.value);

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
		if (!started) return;

		if (hasReplay) {
			indexPlay++;

			if (indexPlay == replayForcesCount) {
				GameEvents.ghostCars.trigger(this, GhostCarEvent.Type.ReplayEnded);
			}
		}
	}
}
