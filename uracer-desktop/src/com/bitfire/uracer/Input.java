package com.bitfire.uracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.InputAdapter;

public class Input extends InputAdapter
{
	// keys
	private static int[] buttons;

	// touches
	private static int touchX;
	private static int touchY;
	private static boolean is_touching;
	private static boolean is_dragging;

	// accelerometer
	private static float accelX, accelY, accelZ;

	// flags
	private static int FLAG_REAL_ON;
	private static int FLAG_DELAY_ON;
	private static int FLAG_CUR_ON;
	private static int FLAG_LAST_ON;

	//
	// game interface
	//

	public static void create()
	{
	}

	public static void dispose()
	{
	}

	public static boolean isTouching()
	{
		return is_touching;
	}

	public static boolean isDragging()
	{
		return is_dragging;
	}

	public static int getX()
	{
		return touchX;
	}

	public static int getY()
	{
		return touchY;
	}

	public static float getAccelX()
	{
		return accelX;
	}

	public static float getAccelY()
	{
		return accelY;
	}

	public static float getAccelZ()
	{
		return accelZ;
	}

	public static boolean isOn( int keycode )
	{
		return is( keycode, FLAG_CUR_ON );
	}

	public static boolean isOff( int keycode )
	{
		return !isOn( keycode );
	}

	public static boolean wasOn( int keycode )
	{
		return is( keycode, FLAG_LAST_ON );
	}

	public static boolean wasOff( int keycode )
	{
		return !wasOn( keycode );
	}

	public static boolean isPressed( int keycode )
	{
		return (isOn( keycode ) && wasOff( keycode ));
	}

	public static boolean isReleased( int keycode )
	{
		return (isOff( keycode ) && wasOn( keycode ));
	}

	public Input()
	{
		buttons = new int[ 256 ];
		is_touching = is_dragging = false;
		touchX = touchY = 0;
		accelX = accelY = accelZ = 0;

		FLAG_REAL_ON = (1 << 0);
		FLAG_DELAY_ON = (1 << 1);
		FLAG_CUR_ON = (1 << 2);
		FLAG_LAST_ON = (1 << 3);

		for( int i = 0; i < buttons.length; i++ )
		{
			buttons[i] = 0;
		}
	}

	public void tick()
	{
		if( Gdx.input.isPeripheralAvailable( Peripheral.Accelerometer ) )
		{
			accelX = Gdx.input.getAccelerometerX();
			accelY = Gdx.input.getAccelerometerY();
			accelZ = Gdx.input.getAccelerometerZ();
		}

		int flag;
		for( int i = 0; i < buttons.length; i++ )
		{
			flag = buttons[i];

			if( (flag & FLAG_CUR_ON) == FLAG_CUR_ON )
			{
				buttons[i] |= FLAG_LAST_ON;
			} else
			{
				buttons[i] &= ~FLAG_LAST_ON;
			}

			if( (flag & (FLAG_DELAY_ON | FLAG_REAL_ON)) == (FLAG_DELAY_ON | FLAG_REAL_ON) )
			{
				buttons[i] |= FLAG_CUR_ON;
			} else
			{
				buttons[i] &= ~FLAG_CUR_ON;
			}

			buttons[i] &= ~FLAG_DELAY_ON;
		}
	}

	public void releaseAllKeys()
	{
		for( int i = 0; i < buttons.length; i++ )
		{
			buttons[i] = /* old_buttons[i] = */0;
		}
	}

	//
	// from InputProcessor
	//

	@Override
	public boolean keyDown( int keycode )
	{
		buttons[keycode] |= (FLAG_REAL_ON | FLAG_DELAY_ON);
		return false;
	}

	@Override
	public boolean keyUp( int keycode )
	{
		buttons[keycode] &= ~FLAG_REAL_ON;
		return false;
	}

	@Override
	public boolean touchDown( int x, int y, int pointer, int button )
	{
		touchX = x;
		touchY = y;
		is_touching = true;
		is_dragging = false;
		return false;
	}

	@Override
	public boolean touchDragged( int x, int y, int pointer )
	{
		touchX = x;
		touchY = y;
		is_dragging = true;
		return false;
	}

	@Override
	public boolean touchUp( int x, int y, int pointer, int button )
	{
		touchX = x;
		touchY = y;
		is_touching = false;
		is_dragging = false;
		return false;
	}

	//
	// helpers
	//

	private static boolean is( int keycode, int flag )
	{
		return ((buttons[keycode] & flag) == flag);
	}
}
