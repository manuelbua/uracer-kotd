
package com.bitfire.uracer.game.world.models;

import java.util.List;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;

public class TrackTrees {
	public final List<TreeStillModel> models;
	private final MapUtils mapUtils;

	public TrackTrees (MapUtils mapUtils, List<TreeStillModel> models) {
		this.mapUtils = mapUtils;
		this.models = models;
	}

	public int count () {
		return (models != null ? models.size() : 0);
	}

	private Vector3 tmpvec = new Vector3();
	private Matrix4 tmpmtx = new Matrix4();
	private Matrix4 tmpmtx2 = new Matrix4();
	private Vector2 pospx = new Vector2();

	public void transform (PerspectiveCamera camPersp, OrthographicCamera camOrtho, Vector2 halfViewport) {
		// float meshZ = -(camPersp.far - camPersp.position.z);
		float meshZ = -(camPersp.far - camPersp.position.z) + (GameWorldRenderer.CamPerspPlaneFar * (1 - (camOrtho.zoom)));

		for (int i = 0; i < models.size(); i++) {
			TreeStillModel m = models.get(i);

			Matrix4 transf = m.transformed;

			// compute position
			pospx.set(m.positionPx);
			pospx.set(mapUtils.positionFor(pospx));
			tmpvec.x = (m.positionOffsetPx.x - camOrtho.position.x) + halfViewport.x + pospx.x;
			tmpvec.y = (m.positionOffsetPx.y + camOrtho.position.y) + halfViewport.y - pospx.y;
			tmpvec.z = 1;

			// transform to world space
			camPersp.unproject(tmpvec);

			// build model matrix
			tmpmtx.setToTranslation(tmpvec.x, tmpvec.y, meshZ);
			Matrix4.mul(tmpmtx.val, tmpmtx2.setToScaling(m.scaleAxis).val);
			Matrix4.mul(tmpmtx.val, tmpmtx2.setToRotation(m.iRotationAxis, m.iRotationAngle).val);
			m.mtxmodel.set(tmpmtx);

			// comb = (proj * view) * model (fast mul)
			Matrix4.mul(transf.set(camPersp.combined).val, tmpmtx.val);

			// transform the bounding box
			m.boundingBox.inf().set(m.localBoundingBox);
			m.boundingBox.mul(tmpmtx);

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
