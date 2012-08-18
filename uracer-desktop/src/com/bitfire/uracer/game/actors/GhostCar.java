
package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.CarUtils;

/** Implements an automated Car, playing previously recorded events. It will ignore car-to-car collisions, but will respect
 * in-track collisions and responses.
 * 
 * @author manuel */

public final class GhostCar extends Car {
	private static final int FadeEvents = 30;
	private Replay replay;
	private int indexPlay;
	private boolean hasReplay;

	public GhostCar (GameWorld gameWorld, CarPreset.Type presetType) {
		super(gameWorld, CarType.ReplayCar, InputMode.InputFromReplay, GameRendererEvent.Order.DEFAULT, presetType, false);
		indexPlay = 0;
		hasReplay = false;
		replay = null;
		this.renderer.setAlpha(0.5f);
		// this.carState = new CarState( gameWorld, this );

		setActive(false);
		resetPhysics();
		resetDistanceAndSpeed();
	}

	// input data for this car cames from a Replay object
	public void setReplay (Replay replay) {
		this.replay = replay;
		hasReplay = (replay != null && replay.getEventsCount() > 0);

		setActive(hasReplay);
		resetPhysics();

		if (hasReplay) {
			setPreset(replay.carPresetType);
			renderer.setAlpha(0);

			// System.out.println( "Replaying " + replay.id );
			restart(replay);
			Gdx.app.log("GhostCar", "Replaying #" + System.identityHashCode(replay) + "s");
		}

		// else
		// {
		// if(replay==null)
		// System.out.println("Replay disabled");
		// else
		// System.out.println("Replay has no recorded events, disabling replaying.");
		// }
	}

	public void removeReplay () {
		setReplay(null);
		renderer.setAlpha(0);
	}

	public boolean hasReplay () {
		return hasReplay;
	}

	private void restart (Replay replay) {
		resetPhysics();
		resetDistanceAndSpeed();
		setWorldPosMt(replay.carWorldPositionMt, replay.carWorldOrientRads);
		indexPlay = 0;

		// Gdx.app.log( "GhostCar", "Set to " + body.getPosition() + ", " + body.getAngle() );
	}

	@Override
	public boolean isVisible () {
		return hasReplay && isActive() && renderer.getAlpha() > 0;
	}

	@Override
	protected void onComputeCarForces (CarForces forces) {
		forces.reset();

		if (hasReplay) {

			// indexPlay is NOT updated here, we don't want
			// to process a non-existent event when (indexPlay == replay.getEventsCount())
			forces.set(replay.forces[indexPlay]);
			// Gdx.app.log( "ghost", "index="+indexPlay + ", px=" + NumberString.formatVeryLong(body.getPosition().x) +
			// ", py=" + NumberString.formatVeryLong(body.getPosition().y) );

			// Gdx.app.log( "", "cf=" +
			// NumberString.formatVeryLong(forces.velocity_x) + ", " +
			// NumberString.formatVeryLong(forces.velocity_y) + ", " +
			// NumberString.formatVeryLong(forces.angularVelocity)
			// );

			// also change opacity, fade in/out based on
			// events played, events remaining
			if (indexPlay <= FadeEvents) {
				renderer.setAlpha(((float)indexPlay / (float)FadeEvents) * 0.5f);
			} else if (replay.getEventsCount() - indexPlay <= FadeEvents) {
				float val = (float)(replay.getEventsCount() - indexPlay) / (float)FadeEvents;
				renderer.setAlpha(val * 0.5f);
			}
		}
		// else {
		// Gdx.app.log( "GhostCar", "No replay, injecting null forces" );
		// }
	}

	@Override
	public void onAfterPhysicsSubstep () {
		super.onAfterPhysicsSubstep();

		if (hasReplay) {
			indexPlay++;

			if (indexPlay == replay.getEventsCount()) {
				CarUtils.dumpSpeedInfo(" Ghost", this, replay.trackTimeSeconds);
				restart(replay);
			}
		}
	}
}
