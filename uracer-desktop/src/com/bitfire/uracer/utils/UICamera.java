
package com.bitfire.uracer.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

/** Implements an orthographic camera that unproject a point from window-space by taking the global viewport scaling and cropping
 * into account.
 * 
 * @author bmanuel */
public class UICamera extends OrthographicCamera {

	private boolean prjForRtt = false;

	@Override
	public Vector3 unproject (Vector3 vec) {
		super.unproject(vec, ScaleUtils.CropX, ScaleUtils.CropY, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
		return vec;
	}

	@Override
	public Vector3 project (Vector3 vec) {
		if (prjForRtt) {
			super.project(vec, 0, 0, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
		} else {
			super.project(vec, ScaleUtils.CropX, ScaleUtils.CropY, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
		}

		return vec;
	}

	public void setProjectForFramebuffer (boolean enabled) {
		prjForRtt = enabled;
	}
}
