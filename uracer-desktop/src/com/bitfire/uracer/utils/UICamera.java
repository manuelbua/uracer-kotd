
package com.bitfire.uracer.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

/** Implements an orthographic camera that unproject a point from window-space by taking the global viewport scaling and cropping
 * into account.
 * 
 * @author bmanuel */
public class UICamera extends OrthographicCamera {

	@Override
	public void unproject (Vector3 vec) {
		unproject(vec, ScaleUtils.CropX, ScaleUtils.CropY, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
	}
}
