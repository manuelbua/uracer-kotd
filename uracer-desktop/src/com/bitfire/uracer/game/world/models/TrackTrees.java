
package com.bitfire.uracer.game.world.models;

import java.util.List;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.ScaleUtils;

public class TrackTrees {
	public final List<TreeStillModel> models;
	private final GameWorld world;

	public TrackTrees (GameWorld world, List<TreeStillModel> models) {
		this.world = world;
		this.models = models;
	}

	public int count () {
		return (models != null ? models.size() : 0);
	}

	private Vector3 tmpvec = new Vector3();
	// private Matrix4 tmpmtx2 = new Matrix4();
	private Vector2 pospx = new Vector2();

	float rotation = 0;

	public void transform (PerspectiveCamera camPersp, OrthographicCamera camOrtho, Vector2 halfViewport) {
		// float meshZ = -(camPersp.far - camPersp.position.z);
		float meshZ = -(camPersp.far - camPersp.position.z) + (camPersp.far * (1 - (camOrtho.zoom)));

		for (int i = 0; i < models.size(); i++) {
			TreeStillModel m = models.get(i);

			// debug
			// m.setRotation(-70, 1, 0, 0);
			// m.setScale(4);
			// rotation += 0.01f;
			// debug

			Matrix4 transf = m.transformed;

			// compute position
			pospx.set(m.positionPx);
			pospx.set(world.positionFor(pospx));
			tmpvec.x = (m.positionOffsetPx.x - camOrtho.position.x) + halfViewport.x + pospx.x;
			tmpvec.y = (m.positionOffsetPx.y + camOrtho.position.y) + halfViewport.y - pospx.y;
			tmpvec.z = 1;

			// transform to world space
			camPersp.unproject(tmpvec, 0, 0, ScaleUtils.RefScreenWidth, ScaleUtils.RefScreenHeight);

			// build model matrix
			Matrix4 model = m.mtxmodel;
			tmpvec.z = meshZ;

			// change of basis
			model.idt();
			model.translate(tmpvec);
			model.rotate(m.iRotationAxis, m.iRotationAngle);
			model.scale(m.scaleAxis.x, m.scaleAxis.y, m.scaleAxis.z);
			model.translate(-tmpvec.x, -tmpvec.y, -tmpvec.z);
			model.translate(tmpvec);

			// comb = (proj * view) * model (fast mul)
			// transf.set(xform).mul(camPersp.combined).mul(m.mtxmodel);
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
