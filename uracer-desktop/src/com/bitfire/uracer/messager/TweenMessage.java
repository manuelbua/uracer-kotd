package com.bitfire.uracer.messager;

import aurelienribon.tweenengine.Tweenable;

public class TweenMessage implements Tweenable
{
	public static final int POSITION_XY = 1;
	public static final int SCALE_XY = 2;
	public static final int OPACITY = 3;

	private Message msg;

	public TweenMessage(Message message) {
		this.msg = message;
	}

	@Override
	public int getTweenValues(int tweenType, float[] returnValues) {
		switch (tweenType) {
			case POSITION_XY:
				returnValues[0] = msg.getX() + msg.getOriginX();
				returnValues[1] = msg.getY() + msg.getOriginY();
				return 2;

			case SCALE_XY:
				returnValues[0] = msg.getScaleX();
				returnValues[1] = msg.getScaleY();
				return 2;

			case OPACITY:
				returnValues[0] = msg.getAlpha();
				return 1;

			default: assert false; return -1;
		}
	}

	@Override
	public void onTweenUpdated(int tweenType, float[] newValues) {
		switch (tweenType) {
			case POSITION_XY:
				msg.setPosition(
					newValues[0] - msg.getOriginX(),
					newValues[1] - msg.getOriginY());
				break;

			case SCALE_XY:
				msg.setScale(newValues[0], newValues[1]);
				break;


			case OPACITY:
				msg.setAlpha( newValues[0] );
				break;

			default: assert false;
		}
	}
}
