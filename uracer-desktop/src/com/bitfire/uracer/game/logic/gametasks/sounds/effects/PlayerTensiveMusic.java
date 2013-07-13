
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent.Order;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.logic.helpers.TrackProgressData;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.InterpolatedFloat;

public final class PlayerTensiveMusic extends SoundEffect {
	private static final int NumTracks = 7;
	private Sound[] music = new Sound[NumTracks]; // prologue [0,3], inciso [4,6]
	private long[] mid = new long[NumTracks];
	private boolean[] started = new boolean[NumTracks];

	private boolean paused;
	private float[] lastVolume = new float[NumTracks];
	private static final float MinVolume = 0.4f;
	private TrackProgressData progressData;
	private InterpolatedFloat[] volTrack = new InterpolatedFloat[NumTracks];

	public PlayerTensiveMusic (TrackProgressData progressData) {
		this.progressData = progressData;
		paused = false;

		for (int i = 0; i < 7; i++) {
			started[i] = false;
			lastVolume[i] = 0;
			mid[i] = -1;
			music[i] = Sounds.musTensive[i];
			volTrack[i] = new InterpolatedFloat();
		}
	}

	@Override
	public void dispose () {
		detach();
		stop();
	}

	private PlayerLapCompletionMonitorEvent.Listener playerCompletionListener = new PlayerLapCompletionMonitorEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			start();
		}
	};

	private void attach () {
		GameEvents.lapCompletion.addListener(playerCompletionListener, PlayerLapCompletionMonitorEvent.Type.onWarmUpStarted);
	}

	private void detach () {
		GameEvents.lapCompletion.removeListener(playerCompletionListener, PlayerLapCompletionMonitorEvent.Type.onWarmUpStarted);
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);

		if (hasPlayer) {
			attach();
		} else {
			detach();
			stop();
		}
	}

	private void start () {
		for (int i = 0; i < NumTracks; i++) {
			start(i);
		}
	}

	private void start (int track) {
		if (started[track]) {
			return;
		}

		started[track] = true;
		mid[track] = loop(music[track], 0);
		music[track].setVolume(mid[track], 0);
	}

	@Override
	public void stop () {
		for (int i = 0; i < NumTracks; i++) {
			stop(i);
		}
	}

	public void stop (int track) {
		if (!started[track]) {
			return;
		}

		started[track] = false;
		if (mid[track] > -1) {
			music[track].stop(mid[track]);
		}
	}

	@Override
	public void gamePause () {
		// paused = true;
	}

	@Override
	public void gameResume () {
		// paused = false;
	}

	@Override
	public void gameReset () {
		stop();
	}

	@Override
	public void gameRestart () {
		stop();
	}

	private void setVolume (int track, float volume) {
		float v = MathUtils.clamp(volume, 0, 1);
		lastVolume[track] = v;
		music[track].setVolume(mid[track], v);
	}

	private float[] volOut = new float[NumTracks];

	@Override
	public void tick () {
		float scalemt = 40;
		float rangeVol = 1 - MinVolume;
		float tgt_vol = 0;

		if (hasPlayer) {

			// assumes index 0 (player in disadvantage)
			int mus_idx = 0;

			if (!progressData.isWarmUp && progressData.hasTarget && !progressData.targetArrived) {
				float v = progressData.playerDistance.get() - progressData.targetDistance.get();
				float to_target = AMath.fixup(MathUtils.clamp(v / scalemt, -1, 1));
				tgt_vol = 1 - MathUtils.clamp(-to_target, 0, 1);

				if (to_target > 0) {
					// player is heading the race
					mus_idx = NumTracks - 1;
				} else {
					float fidx = tgt_vol * (NumTracks - 1);
					mus_idx = progressData.isWarmUp ? 0 : (int)fidx;
				}

				// Gdx.app.log("PlayerTensiveMusic", "to_target=" + to_target + ", mus_idx=" + (int)mus_idx + ", tgt=" + tgt);
			}

			// update all volume accumulators
			float alpha = 0.025f;
			for (int i = 0; i < NumTracks; i++) {
				if (mus_idx == i) {
					float v = MinVolume + rangeVol * tgt_vol;
					volTrack[i].set(v, alpha);
				} else {
					volTrack[i].set(0, alpha);
				}

				volOut[i] = volTrack[i].get();

				setVolume(i, volOut[i]);
			}

			String dbg = "";
			for (int i = 0; i < NumTracks; i++) {
				dbg += "[" + ((i == mus_idx) ? "*" : " ") + String.format("%02.1f", volOut[i]) + "] ";
			}

			Gdx.app.log("PlayerTensiveMusic", dbg);
		}
	}
}
