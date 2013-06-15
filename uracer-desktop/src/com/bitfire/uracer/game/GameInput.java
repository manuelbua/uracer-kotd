
package com.bitfire.uracer.game;

import com.badlogic.gdx.Input.Keys;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Input.MouseButton;
import com.bitfire.uracer.configuration.Gameplay;
import com.bitfire.uracer.configuration.Gameplay.TimeDilateInputMode;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;

public final class GameInput {

	private Input input;
	private GameLogic logic;
	private TimeDilateInputMode timeMode;
	private boolean timeDilation;

	public GameInput (GameLogic logic, Input inputSystem) {
		this.logic = logic;
		this.input = inputSystem;
		this.timeMode = Gameplay.TimeDilateInputMode.valueOf(UserPreferences.string(Preference.TimeDilateInputMode));
		this.timeDilation = false;
	}

	public void setInputMode (TimeDilateInputMode mode) {
		this.timeMode = mode;
	}

	public boolean isTimeDilating () {
		return timeDilation;
	}

	public void resetTimeDilating () {
		timeDilation = false;
	}

	// public void reset () {
	// resetTimeDilating();
	// input.releaseAllKeys();
	// }

	public void update () {

		if (input.isPressed(Keys.R)) {
			logic.restartGame();
		} else if (input.isPressed(Keys.T)) {
			logic.resetGame();
		} else if (input.isPressed(Keys.Q) || input.isPressed(Keys.ESCAPE) || input.isPressed(Keys.BACK)) {
			logic.quitGame();
		}

		boolean rightMouseButton = input.isTouched(MouseButton.Right);// && input.isTouchedInBounds(MouseButton.Right);

		switch (timeMode) {
		case Toggle:
			if (input.isPressed(Keys.SPACE) || rightMouseButton) {
				timeDilation = !timeDilation;

				if (timeDilation) {
					if (logic.isTimeDilationAvailable()) {
						logic.startTimeDilation();
					} else {
						timeDilation = false;
					}
				} else {
					logic.endTimeDilation();
				}
			}
			break;

		case TouchAndRelease:
			if (input.isPressed(Keys.SPACE) || rightMouseButton) {
				if (!timeDilation && logic.isTimeDilationAvailable()) {
					timeDilation = true;
					logic.startTimeDilation();
				}
			} else if (input.isReleased(Keys.SPACE) || input.isUntouched(MouseButton.Right)) {
				if (timeDilation) {
					timeDilation = false;
					logic.endTimeDilation();
				}
			}
			break;
		}
	}

}
