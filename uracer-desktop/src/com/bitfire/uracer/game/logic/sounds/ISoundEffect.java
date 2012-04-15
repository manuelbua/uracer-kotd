package com.bitfire.uracer.game.logic.sounds;

import com.badlogic.gdx.utils.Disposable;

public interface ISoundEffect extends Disposable {
	void start();

	void tick();

	void stop();

	void reset();
}
