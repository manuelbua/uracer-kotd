
package com.bitfire.uracer.game.world.models;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.graphics.g3d.model.still.StillSubMesh;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.ScaleUtils;

public class CarStillModel extends OrthographicAlignedStillModel {
	public Mesh body, leftTire, rightTire;
	public StillSubMesh smBody, smLeftTire, smRightTire;

	public Matrix4 mtxbodytransformed = new Matrix4();
	public Matrix4 mtxbody = new Matrix4();

	public Matrix4 mtxltiretransformed = new Matrix4();
	public Matrix4 mtxltire = new Matrix4();

	public Matrix4 mtxrtiretransformed = new Matrix4();
	public Matrix4 mtxrtire = new Matrix4();

	public BoundingBox boundingBox = new BoundingBox();
	public BoundingBox localBoundingBox = new BoundingBox();
	private Vector3 tmpvec = new Vector3();
	private Car car = null;
	private float prevTireAngle = 0;

	public CarStillModel (StillModel aModel, Material material) {
		super(aModel, material);
		setScale(1);

		smBody = model.getSubMesh("car_01");
		smLeftTire = model.getSubMesh("left_tire");
		smRightTire = model.getSubMesh("right_tire");

		if (smBody != null) {
			body = smBody.mesh;
		}

		if (smLeftTire != null) {
			leftTire = smLeftTire.mesh;
		}

		if (smRightTire != null) {
			rightTire = smRightTire.mesh;
		}

		model.getBoundingBox(localBoundingBox);
		boundingBox.set(localBoundingBox);
	}

	public void setCar (Car car) {
		this.car = car;
	}

	public void transform (PerspectiveCamera camPersp, OrthographicCamera camOrtho) {
		// set position to car position
		EntityRenderState state = car.state();
		float s = 1;

		// body
		{
			Vector3 pos = world2Dto3D(camPersp, camOrtho, state.position.x, state.position.y);

			mtxbody.idt();
			mtxbody.translate(pos);
			mtxbody.rotate(0, 0, 1, state.orientation);
			mtxbody.scale(this.scaleAxis.x * s, this.scaleAxis.y * s, this.scaleAxis.z * s);

			// comb = (proj * view) * model (fast mul)
			mtxbodytransformed.set(camPersp.combined).mul(mtxbody);
		}

		float tirerot = MathUtils.radDeg * car.getSteerAngleRads();
		float itirerot = AMath.lerp(prevTireAngle, tirerot, 0.1f);
		prevTireAngle = itirerot;

		// left tire
		{
			float offx = Convert.mt2px(0.97f) * s;
			float offy = Convert.mt2px(1.23f) * s;
			float cos = MathUtils.cosDeg(90 + state.orientation);
			float sin = MathUtils.sinDeg(90 + state.orientation);
			float dx = offy * cos - offx * sin;
			float dy = offy * sin + offx * cos;
			Vector3 pos = world2Dto3D(camPersp, camOrtho, state.position.x + dx, state.position.y + dy);
			mtxltire.set(camPersp.combined);
			mtxltire.translate(pos);
			mtxltire.rotate(0, 0, 1, state.orientation - itirerot);
			mtxltire.scale(this.scaleAxis.x * s, this.scaleAxis.y * s, this.scaleAxis.z * s);
			mtxltiretransformed.set(mtxltire);
		}

		// right tire
		{
			float offx = -Convert.mt2px(0.97f) * s;
			float offy = Convert.mt2px(1.23f) * s;
			float cos = MathUtils.cosDeg(90 + state.orientation);
			float sin = MathUtils.sinDeg(90 + state.orientation);
			float dx = offy * cos - offx * sin;
			float dy = offy * sin + offx * cos;
			Vector3 pos = world2Dto3D(camPersp, camOrtho, state.position.x + dx, state.position.y + dy);
			mtxrtire.set(camPersp.combined);
			mtxrtire.translate(pos);
			mtxrtire.rotate(0, 0, 1, state.orientation - itirerot);
			mtxrtire.scale(this.scaleAxis.x * s, this.scaleAxis.y * s, this.scaleAxis.z * s);
			mtxrtiretransformed.set(mtxrtire);
		}

		// transform the bounding box (uses body transform)
		boundingBox.inf().set(localBoundingBox);
		boundingBox.mul(mtxbody);
	}

	private Vector3 world2Dto3D (PerspectiveCamera camPersp, OrthographicCamera camOrtho, float posPxX, float posPxY) {
		float meshZ = -(camPersp.far - camPersp.position.z) + (camPersp.far * (1 - (camOrtho.zoom)));

		// compute position
		tmpvec.x = (this.positionOffsetPx.x - camPersp.position.x) + (camPersp.viewportWidth / 2) + posPxX;
		tmpvec.y = (this.positionOffsetPx.y + camPersp.position.y) + (camPersp.viewportHeight / 2) - posPxY;
		tmpvec.z = 1;

		tmpvec.x *= ScaleUtils.Scale;
		tmpvec.y *= ScaleUtils.Scale;

		tmpvec.x += ScaleUtils.CropX;
		tmpvec.y += ScaleUtils.CropY;

		// transform to world space
		camPersp.unproject(tmpvec, ScaleUtils.CropX, ScaleUtils.CropY, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);

		// build model matrix
		tmpvec.z = meshZ;

		return tmpvec;
	}
}
