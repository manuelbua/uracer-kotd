package com.bitfire.uracer.tweenables;

import aurelienribon.tweenengine.Tweenable;

import com.bitfire.uracer.hud.HudLabel;

public class TweenHudLabel implements Tweenable
{
	public static final int OPACITY = 1;

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
		case OPACITY:
			label.setAlpha( newValues[0] );
			break;

		default:
			assert false;
		}
	}

}
