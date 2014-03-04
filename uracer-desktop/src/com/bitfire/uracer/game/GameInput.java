
package com.bitfire.uracer.game;

import com.badlogic.gdx.Input.Keys;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Input.MouseButton;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.game.GameplaySettings.TimeDilateInputMode;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;

public final class GameInput {

	private Input input;
	private GameLogic logic;
	private TimeDilateInputMode timeMode;
	private boolean timeDilation;

	public GameInput (GameLogic logic, Input inputSystem) {
		this.logic = logic;
		this.input = inputSystem;
		this.timeMode = TimeDilateInputMode.valueOf(UserPreferences.string(Preference.TimeDilateInputMode));
		this.timeDilation = false;
		input.releaseAllKeys();
	}

	public void setInputMode (TimeDilateInputMode mode) {
		this.timeMode = mode;
	}

	public TimeDilateInputMode getInputMode () {
		return this.timeMode;
	}

	public boolean isTimeDilating () {
		return timeDilation;
	}

	public void resetTimeDilating () {
		timeDilation = false;
	}

	public void ensureConsistenceAfterResume () {
		// In case the input mode is set to TouchAndRelease then the keyup/button-released event may have
		// been already triggered during the pause, check for it and disable time dilation if it's the case.
		if (timeMode == TimeDilateInputMode.TouchAndRelease) {
			if (!input.isOn(Keys.SPACE) && !input.isTouching(MouseButton.Right)) {
				logic.endTimeDilation();
			}
		}
	}

	// public void reset () {
	// resetTimeDilating();
	// input.releaseAllKeys();
	// }

	public void update () {
		if (input.isPressed(Keys.R)) {
			logic.restartGame();
			logic.showMessage("Restarted", 1.5f, Message.Type.Information, Position.Bottom, Size.Big);
		} else if (input.isPressed(Keys.T)) {
			logic.resetGame();
			logic.showMessage("Reset", 1.5f, Message.Type.Information, Position.Bottom, Size.Big);
		} else if (input.isPressed(Keys.TAB)) {
			// choose next/prev best target
			boolean backward = input.isOn(Keys.SHIFT_LEFT) || input.isOn(Keys.SHIFT_RIGHT);
			logic.chooseNextTarget(backward);
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
