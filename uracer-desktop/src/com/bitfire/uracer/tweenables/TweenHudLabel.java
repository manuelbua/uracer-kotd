package com.bitfire.uracer.tweenables;

import aurelienribon.tweenengine.Tweenable;

import com.bitfire.uracer.hud.HudLabel;

public class TweenHudLabel implements Tweenable
{
	public static final int POSITION_XY = 1;
	public static final int POSITION_X = 2;
	public static final int POSITION_Y = 3;
	public static final int SCALE = 4;
	public static final int OPACITY = 5;

	private HudLabel label;

	public TweenHudLabel( HudLabel label )
	{
		this.label = label;
	}

	@Override
	public int getTweenValues( int tweenType, float[] returnValues )
	{
		switch( tweenType )
		{
		case POSITION_XY:
			returnValues[0] = label.getX();
			returnValues[1] = label.getY();
			return 2;

		case POSITION_X:
			returnValues[0] = label.getX();
			return 1;

		case POSITION_Y:
			returnValues[0] = label.getY();
			return 1;

		case SCALE:
			returnValues[0] = label.getScale();
			return 1;

		case OPACITY:
			returnValues[0] = label.getAlpha();
			return 1;

		default:
			assert false;
			return -1;
		}
	}

	@Override
	public void onTweenUpdated( int tweenType, float[] newValues )
	{
		switch( tweenType )
		{
		case POSITION_XY:
			label.setPosition( newValues[0], newValues[1] );
			break;

		case POSITION_X:
			label.setX( newValues[0] );
			break;

		case POSITION_Y:
			label.setY( newValues[0] );
			break;

		case SCALE:
			label.setScale( newValues[0], true );
			break;

		case OPACITY:
			label.setAlpha( newValues[0] );
			break;

		default:
			assert false;
		}
	}

}
