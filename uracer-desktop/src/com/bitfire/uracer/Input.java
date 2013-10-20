
package com.bitfire.uracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.configuration.Config;

/** Encapsulates a buffered input state object that can be queried to know the individual key/button/pointer states.
 * 
 * @author bmanuel */
public final class Input implements Disposable {

	/** Encapsulates mouse buttons */
	public enum MouseButton {
		Left(Buttons.LEFT), Right(Buttons.RIGHT), Middle(Buttons.MIDDLE);

		public int ordinal;

		private MouseButton (int value) {
			this.ordinal = value;
		}
	}

	// keys
	private final int[] buttons = new int[256];
	private int anyKeyButton = 0;

	// mouse
	private Pointer pointer = new Pointer();

	// flags
	private static final int FLAG_REAL_ON = 1;
	private static final int FLAG_DELAY_ON = 2;
	private static final int FLAG_CUR_ON = 4;
	private static final int FLAG_LAST_ON = 8;

	// coordinates transform
	private final Rectangle viewport = new Rectangle();

	public Input (Rectangle viewport) {
		this.viewport.set(viewport);
		releaseAllKeys();
		Gdx.input.setCatchBackKey(true);
	}

	@Override
	public void dispose () {
		pointer = null;
	}

	public void releaseAllKeys () {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = 0;
		}

		pointer.reset();
	}

	// pointers
	public boolean isTouching (MouseButton button) {
		return pointer.isTouching(button);
	}

	public boolean isTouched (MouseButton button) {
		return pointer.isTouched(button);
	}

	public boolean isTouchedInBounds (MouseButton button) {
		return pointer.isTouchedInBounds(button);
	}

	public boolean isUntouched (MouseButton button) {
		return pointer.isUntouched(button);
	}

	public boolean isTouching () {
		return pointer.isTouching(MouseButton.Left);
	}

	public int getX () {
		return (int)pointer.touchCoords.x;
	}

	public int getY () {
		return (int)pointer.touchCoords.y;
	}

	public Vector2 getXY () {
		return pointer.touchCoords;
	}

	// keyboard
	public boolean isOn (int keycode) {
		return ((buttons[keycode] & FLAG_CUR_ON) > 0) ? true : false;
	}

	public boolean isOff (int keycode) {
		return ((buttons[keycode] & FLAG_CUR_ON) > 0) ? false : true;
	}

	public boolean isPressed (int keycode) {
		return ((buttons[keycode] & FLAG_CUR_ON) > 0) && !((buttons[keycode] & FLAG_LAST_ON) > 0);
	}

	public boolean isReleased (int keycode) {
		return !((buttons[keycode] & FLAG_CUR_ON) > 0) && ((buttons[keycode] & FLAG_LAST_ON) > 0);
	}

	private void updateKeyState () {
		long flag;
		boolean is_any_key_on = false;

		boolean isKeyPressed = false;

		for (int i = 0; i < buttons.length; i++) {

			// acquire input
			isKeyPressed = Gdx.input.isKeyPressed(i);

			// update flags
			if (isKeyPressed) {
				buttons[i] |= (FLAG_REAL_ON | FLAG_DELAY_ON);
			} else {
				buttons[i] &= ~FLAG_REAL_ON;
			}

			flag = buttons[i];

			if ((flag & FLAG_CUR_ON) > 0) {
				buttons[i] |= FLAG_LAST_ON;
			} else {
				buttons[i] &= ~FLAG_LAST_ON;
			}

			if ((flag & (FLAG_DELAY_ON | FLAG_REAL_ON)) > 0) {
				buttons[i] |= FLAG_CUR_ON;
				is_any_key_on = true;
			} else {
				buttons[i] &= ~FLAG_CUR_ON;
			}

			buttons[i] &= ~FLAG_DELAY_ON;
		}

		flag = anyKeyButton;

		if ((flag & FLAG_CUR_ON) > 0) {
			anyKeyButton |= FLAG_LAST_ON;
		} else {
			anyKeyButton &= ~FLAG_LAST_ON;
		}

		if (is_any_key_on) {
			anyKeyButton |= FLAG_CUR_ON;
		} else {
			anyKeyButton &= ~FLAG_LAST_ON;
		}
	}

	private void updatePointerState () {
		Pointer ptr = pointer;

		int px = Gdx.input.getX() - (int)viewport.x;
		int py = Gdx.input.getY() - (int)viewport.y;
		boolean pointerInBounds = (px >= 0 && py >= 0 && px < viewport.width && py < viewport.height);

		float npx = (float)px / viewport.width;
		float npy = (float)py / viewport.height;

		for (MouseButton b : MouseButton.values()) {
			ptr.setTouching(b, Gdx.input.isButtonPressed(b.ordinal), pointerInBounds);
		}

		// update coords even if not touched
		int tx = (int)(npx * Config.Graphics.ReferenceScreenWidth);
		int ty = (int)(npy * Config.Graphics.ReferenceScreenHeight);
		ptr.touchCoords.set(tx, ty);

	}

	// update key state and transform unbuffered to buffered
	public void tick () {
		updateKeyState();
		updatePointerState();
	}

	/** Encapsulates the touch state for a pointer. */
	private class Pointer {
		private final Vector2 touchCoords = new Vector2(-1, -1);
		private final boolean[] is_touching = new boolean[MouseButton.values().length];
		private final boolean[] was_touching = new boolean[MouseButton.values().length];
		private final boolean[] touched_in_bounds = new boolean[MouseButton.values().length];

		public void reset () {
			for (MouseButton b : MouseButton.values()) {
				is_touching[b.ordinal] = false;
				was_touching[b.ordinal] = false;
				touched_in_bounds[b.ordinal] = false;
			}
		}

		public void setTouching (MouseButton button, boolean touching, boolean inBounds) {
			int i = button.ordinal;
			was_touching[i] = is_touching[i];
			is_touching[i] = touching;

			if (isTouched(button)) {
				touched_in_bounds[i] = inBounds;
			}
		}

		/** Returns whether or not this pointer wasn't touching AND now it is. */
		public boolean isTouched (MouseButton button) {
			return !was_touching[button.ordinal] && is_touching[button.ordinal];
		}

		public boolean isTouchedInBounds (MouseButton button) {
			return touched_in_bounds[button.ordinal];
		}

		public boolean isTouching (MouseButton button) {
			return is_touching[button.ordinal];
		}

		/** Returns whether or not this pointer was touching AND now it is not. */
		public boolean isUntouched (MouseButton button) {
			return was_touching[button.ordinal] && !is_touching[button.ordinal];
		}
	}
}
