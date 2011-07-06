package com.bitfire.uracer;

import com.badlogic.gdx.InputAdapter;

public class Input extends InputAdapter
{
	// keys
	private static int[] buttons = new int[ 256 ];
	private static int[] old_buttons = new int[ 256 ]; // unused

	// touches
	private static int touchX = 0;
	private static int touchY = 0;
	private static boolean is_touching = false;
	private static boolean is_dragging = false;

	private static final int FLAG_REAL_ON = (1 << 0);
	private static final int FLAG_DELAY_ON = (1 << 1);
	private static final int FLAG_CUR_ON = (1 << 2);
	private static final int FLAG_LAST_ON = (1 << 3);

	//
	// game interface
	//

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

	private static boolean is( int keycode, int flag )
	{
		return ((buttons[keycode] & flag) == flag);
	}

	public Input()
	{
		for( int i = 0; i < buttons.length; i++ )
		{
			buttons[i] = old_buttons[i] = 0;
		}
	}

	public void tick()
	{
		for( int i = 0; i < buttons.length; i++ )
		{
			old_buttons[i] = buttons[i];
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
			buttons[i] = old_buttons[i] = 0;
		}
	}

	//
	// from InputProcessor
	//

	@Override
	public boolean keyDown( int keycode )
	{
		buttons[keycode] |= (FLAG_REAL_ON | FLAG_DELAY_ON);
		// buttons[ keycode ] |= (FLAG_REAL_ON | FLAG_CUR_ON);
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
}
