package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.task.TaskManagerEvent;

public class Input extends InputAdapter {
	// keys
	private int[] buttons = new int[ 256 ];;

	// touches
	private Vector2 touchCoords = new Vector2( 0, 0 );
	private int touchX = 0;
	private int touchY = 0;
	private boolean is_touching = false;
	private boolean is_dragging = false;

	// mouse (desktop only)
	private int mouseX, mouseY;
	private Vector2 mouseCoords = new Vector2( 0, 0 );

	// accelerometer
	// private float accelX = 0, accelY = 0, accelZ = 0;

	// flags
	private static final int FLAG_REAL_ON = (1 << 0);
	private static final int FLAG_DELAY_ON = (1 << 1);
	private static final int FLAG_CUR_ON = (1 << 2);
	private static final int FLAG_LAST_ON = (1 << 3);

	private Task ticker;

	public Input( TaskManagerEvent.Order order ) {
		ticker = new Task( order ) {
			@Override
			protected void onTick() {
				tick();
			}
		};

		for( int i = 0; i < buttons.length; i++ ) {
			buttons[i] = 0;
		}

		Gdx.input.setInputProcessor( this );
		releaseAllKeys();
	}

	public void dispose() {
		ticker.dispose();
	}

	//
	// game interface
	//

	public boolean isTouching() {
		return is_touching;
	}

	public boolean isDragging() {
		return is_dragging;
	}

	public int getX() {
		return touchX;
	}

	public int getY() {
		return touchY;
	}

	public Vector2 getXY() {
		return touchCoords;
	}

	public int getMouseX() {
		return mouseX;
	}

	public int getMouseY() {
		return mouseY;
	}

	public Vector2 getMouseXY() {
		return mouseCoords;
	}

	// public float getAccelX()
	// {
	// return accelX;
	// }
	//
	// public float getAccelY()
	// {
	// return accelY;
	// }
	//
	// public float getAccelZ()
	// {
	// return accelZ;
	// }

	public boolean isOn( int keycode ) {
		return is( keycode, FLAG_CUR_ON );
	}

	public boolean isOff( int keycode ) {
		return !isOn( keycode );
	}

	public boolean wasOn( int keycode ) {
		return is( keycode, FLAG_LAST_ON );
	}

	public boolean wasOff( int keycode ) {
		return !wasOn( keycode );
	}

	public boolean isPressed( int keycode ) {
		return (isOn( keycode ) && wasOff( keycode ));
	}

	public boolean isReleased( int keycode ) {
		return (isOff( keycode ) && wasOn( keycode ));
	}

	private void tick() {
		// if( Gdx.input.isPeripheralAvailable( Peripheral.Accelerometer ) )
		// {
		// accelX = Gdx.input.getAccelerometerX();
		// accelY = Gdx.input.getAccelerometerY();
		// accelZ = Gdx.input.getAccelerometerZ();
		// }

		mouseX = Gdx.input.getX();
		mouseY = Gdx.input.getY();
		mouseCoords.set( mouseX, mouseY );

		int flag;
		for( int i = 0; i < buttons.length; i++ ) {
			flag = buttons[i];

			if( (flag & FLAG_CUR_ON) == FLAG_CUR_ON ) {
				buttons[i] |= FLAG_LAST_ON;
			} else {
				buttons[i] &= ~FLAG_LAST_ON;
			}

			if( (flag & (FLAG_DELAY_ON | FLAG_REAL_ON)) == (FLAG_DELAY_ON | FLAG_REAL_ON) ) {
				buttons[i] |= FLAG_CUR_ON;
			} else {
				buttons[i] &= ~FLAG_CUR_ON;
			}

			buttons[i] &= ~FLAG_DELAY_ON;
		}
	}

	public void releaseAllKeys() {
		for( int i = 0; i < buttons.length; i++ ) {
			buttons[i] = /* old_buttons[i] = */0;
		}
	}

	//
	// from InputProcessor
	//

	@Override
	public boolean keyDown( int keycode ) {
		buttons[keycode] |= (FLAG_REAL_ON | FLAG_DELAY_ON);
		return false;
	}

	@Override
	public boolean keyUp( int keycode ) {
		buttons[keycode] &= ~FLAG_REAL_ON;
		return false;
	}

	@Override
	public boolean touchDown( int x, int y, int pointer, int button ) {
		touchX = x;
		touchY = y;
		touchCoords.set( x, y );

		is_touching = true;
		is_dragging = false;
		return false;
	}

	@Override
	public boolean touchDragged( int x, int y, int pointer ) {
		touchX = x;
		touchY = y;
		touchCoords.set( x, y );

		is_dragging = true;
		return false;
	}

	@Override
	public boolean touchUp( int x, int y, int pointer, int button ) {
		touchX = x;
		touchY = y;
		touchCoords.set( x, y );

		is_touching = false;
		is_dragging = false;
		return false;
	}

	private boolean is( int keycode, int flag ) {
		return ((buttons[keycode] & flag) == flag);
	}
}
