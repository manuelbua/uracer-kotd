
package com.bitfire.uracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.utils.ScaleUtils;

/** Encapsulates a buffered input state object that can be queried to know the individual key/button/pointer states.
 * 
 * @author bmanuel */
public final class Input implements Disposable {
	public static final int MaxPointers = 2;

	// scaling
	private ScalingStrategy scalingStrategy;

	// keys
	private int[] buttons = new int[256];
	private int anyKeyButton = 0;

	// touches
	private Pointer[] pointer = new Pointer[MaxPointers];

	// flags
	private static final int FLAG_REAL_ON = 1;
	private static final int FLAG_DELAY_ON = 2;
	private static final int FLAG_CUR_ON = 4;
	private static final int FLAG_LAST_ON = 8;

	public Input (ScalingStrategy scalingStrategy) {
		this.scalingStrategy = scalingStrategy;

		for (int p = 0; p < MaxPointers; p++) {
			pointer[p] = new Pointer( /* p */);
		}

		releaseAllKeys();
		Gdx.input.setCatchBackKey(true);
	}

	@Override
	public void dispose () {
		for (int p = 0; p < MaxPointers; p++) {
			pointer[p] = null;
		}
	}

	public void releaseAllKeys () {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = 0;
		}

		for (int p = 0; p < MaxPointers; p++) {
			pointer[p].reset();
		}
	}

	// pointers
	public boolean isTouching (int ptr) {
		return pointer[ptr].is_touching;
	}

	public boolean isTouched (int ptr) {
		return pointer[ptr].touched();
	}

	public boolean isUntouched (int ptr) {
		return pointer[ptr].untouched();
	}

	public int getX (int ptr) {
		return pointer[ptr].touchX;
	}

	public int getY (int ptr) {
		return pointer[ptr].touchY;
	}

	public Vector2 getXY (int ptr) {
		return pointer[ptr].touchCoords;
	}

	public boolean isTouching () {
		return pointer[0].is_touching;
	}

	public int getX () {
		return pointer[0].touchX;
	}

	public int getY () {
		return pointer[0].touchY;
	}

	public Vector2 getXY () {
		return pointer[0].touchCoords;
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
		for (int p = 0; p < MaxPointers; p++) {
			Pointer ptr = pointer[p];

			int px = Gdx.input.getX(p) - ScaleUtils.CropX;
			int py = Gdx.input.getY(p) - ScaleUtils.CropY;
			// boolean inBoundaries = (px >= 0 && py >= 0 && px < ScaleUtils.PlayWidth && py < ScaleUtils.PlayHeight);

			float npx = (float)px / (float)ScaleUtils.PlayWidth;
			float npy = (float)py / (float)ScaleUtils.PlayHeight;
			// Gdx.app.log("Input", npx + "," + npy);

			ptr.setTouching(/* inBoundaries && */Gdx.input.isButtonPressed(p));

			// update coords even if not touched
			ptr.touchX = (int)(npx * ScaleUtils.RefScreenWidth);
			ptr.touchY = (int)(npy * ScaleUtils.RefScreenHeight);
			ptr.touchCoords.set(ptr.touchX, ptr.touchY);
		}

	}

	// update key state and transform unbuffered to buffered
	public void tick () {
		updateKeyState();
		updatePointerState();
	}

	/** Encapsulates the touch state for a pointer. */
	private class Pointer {
		public final Vector2 touchCoords = new Vector2(0, 0);
		public int touchX = 0;
		public int touchY = 0;
		public boolean is_touching = false;
		private boolean was_touching = false;

		public void reset () {
			is_touching = false;
			was_touching = false;
			touchX = 0;
			touchY = 0;
		}

		public void setTouching (boolean value) {
			was_touching = is_touching;
			is_touching = value;
		}

		/** Returns whether or not this pointer wasn't touching AND now it is. */
		public boolean touched () {
			return !was_touching && is_touching;
		}

		/** Returns whether or not this pointer was touching AND now it is not. */
		public boolean untouched () {
			return was_touching && !is_touching;
		}
	}
}
