
package com.bitfire.uracer.game.logic.gametasks.messager;

import aurelienribon.tweenengine.TweenAccessor;

public class MessageAccessor implements TweenAccessor<Message> {
	public static final int POSITION_XY = 1;
	public static final int POSITION_X = 2;
	public static final int POSITION_Y = 3;
	public static final int SCALE_XY = 4;
	public static final int OPACITY = 5;

	@Override
	public int getValues (Message target, int tweenType, float[] returnValues) {
		switch (tweenType) {
		case POSITION_XY:
			returnValues[0] = target.getX();// + msg.getOriginX();
			returnValues[1] = target.getY();// + msg.getOriginY();
			return 2;

		case POSITION_X:
			returnValues[0] = target.getX();// + msg.getOriginX();
			return 1;

		case POSITION_Y:
			returnValues[0] = target.getY();// + msg.getOriginY();
			return 1;

		case SCALE_XY:
			returnValues[0] = target.getScaleX();
			returnValues[1] = target.getScaleY();
			return 2;

		case OPACITY:
			returnValues[0] = target.getAlpha();
			return 1;

		default:
			assert false;
			return -1;
		}
	}

	@Override
	public void setValues (Message target, int tweenType, float[] newValues) {
		switch (tweenType) {
		case POSITION_XY:
			target.setPosition(newValues[0] /*- msg.getOriginX()*/, newValues[1] /*- msg.getOriginY()*/);
			break;

		case POSITION_X:
			target.setX(newValues[0] /*- msg.getOriginX()*/);
			break;

		case POSITION_Y:
			target.setY(newValues[0] /*- msg.getOriginY()*/);
			break;

		case SCALE_XY:
			target.setScale(newValues[0], newValues[1]);
			break;

		case OPACITY:
			target.setAlpha(newValues[0]);
			break;

		default:
			assert false;
		}
	}
}
