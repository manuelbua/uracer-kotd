
package com.bitfire.uracer.game.world.models;

import java.util.List;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.utils.ScaleUtils;

public class TrackTrees {
	public final List<TreeStillModel> models;
	private Vector3 tmpvec = new Vector3();

	public TrackTrees (List<TreeStillModel> models) {
		this.models = models;
	}

	public int count () {
		return (models != null ? models.size() : 0);
	}

	public void transform (PerspectiveCamera camPersp, OrthographicCamera camOrtho) {
		float meshZ = -(camPersp.far - camPersp.position.z) + (camPersp.far * (1 - (camOrtho.zoom)));

		for (int i = 0; i < models.size(); i++) {
			TreeStillModel m = models.get(i);

			Matrix4 transf = m.transformed;

			// compute position
			tmpvec.x = (m.positionOffsetPx.x - camPersp.position.x) + (camPersp.viewportWidth / 2) + m.positionPx.x;
			tmpvec.y = (m.positionOffsetPx.y + camPersp.position.y) + (camPersp.viewportHeight / 2) - m.positionPx.y;
			tmpvec.z = 1;

			tmpvec.x *= ScaleUtils.Scale;
			tmpvec.y *= ScaleUtils.Scale;

			tmpvec.x += ScaleUtils.CropX;
			tmpvec.y += ScaleUtils.CropY;

			// transform to world space
			camPersp.unproject(tmpvec, ScaleUtils.CropX, ScaleUtils.CropY, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);

			// build model matrix
			Matrix4 model = m.mtxmodel;
			tmpvec.z = meshZ;

			model.idt();
			model.translate(tmpvec);
			model.rotate(m.iRotationAxis, m.iRotationAngle);
			model.scale(m.scaleAxis.x, m.scaleAxis.y, m.scaleAxis.z);

			// comb = (proj * view) * model (fast mul)
			transf.set(camPersp.combined).mul(m.mtxmodel);

			// transform the bounding box
			m.boundingBox.inf().set(m.localBoundingBox);
			m.boundingBox.mul(m.mtxmodel);

			// create an AABB out of the corners of the original
			// AABB transformed by the model matrix
			// bb.inf();
			// Vector3[] corners = m.localBoundingBox.getCorners();
			// for(int k = 0; k < corners.length; k++)
			// {
			// vtrans[k].x = corners[k].x;
			// vtrans[k].y = corners[k].y;
			// vtrans[k].z = corners[k].z;
			// vtrans[k].mul( tmpmtx );
			// bb.ext(vtrans[k]);
			// }
			//
			// m.boundingBox.inf();
			// m.boundingBox.set( bb );
		}
	}
}
