
package com.bitfire.uracer.game.logic.gametasks.hud;

import aurelienribon.tweenengine.TweenAccessor;

public class HudLabelAccessor implements TweenAccessor<HudLabel> {
	public static final int POSITION_XY = 1;
	public static final int POSITION_X = 2;
	public static final int POSITION_Y = 3;
	public static final int SCALE = 4;
	public static final int OPACITY = 5;

	@Override
	public int getValues (HudLabel target, int tweenType, float[] returnValues) {
		switch (tweenType) {
		case POSITION_XY:
			returnValues[0] = target.getX();
			returnValues[1] = target.getY();
			return 2;

		case POSITION_X:
			returnValues[0] = target.getX();
			return 1;

		case POSITION_Y:
			returnValues[0] = target.getY();
			return 1;

		case SCALE:
			returnValues[0] = target.getScale();
			return 1;

		case OPACITY:
			returnValues[0] = target.getAlpha();
			return 1;

		default:
			assert false;
			return -1;
		}
	}

	@Override
	public void setValues (HudLabel target, int tweenType, float[] newValues) {
		switch (tweenType) {
		case POSITION_XY:
			target.setPosition(newValues[0], newValues[1]);
			break;

		case POSITION_X:
			target.setX(newValues[0]);
			break;

		case POSITION_Y:
			target.setY(newValues[0]);
			break;

		case SCALE:
			target.setScale(newValues[0]);
			break;

		case OPACITY:
			target.setAlpha(newValues[0]);
			break;

		default:
			assert false;
		}
	}

}
