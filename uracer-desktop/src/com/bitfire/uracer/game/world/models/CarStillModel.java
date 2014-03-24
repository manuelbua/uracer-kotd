
package com.bitfire.uracer.game.world.models;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.u3d.materials.Material;
import com.bitfire.uracer.u3d.still.StillModel;
import com.bitfire.uracer.u3d.still.StillSubMesh;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.InterpolatedFloat;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.uracer.utils.URacerRuntimeException;

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

	private InterpolatedFloat tireAngle = new InterpolatedFloat();
	private Vector3 tmpvec = new Vector3();
	private Car car = null;

	private InterpolatedFloat sideAngle = new InterpolatedFloat();
	private InterpolatedFloat bodyAngle = new InterpolatedFloat();

	private GameLogic gameLogic = null;

	public CarStillModel (GameLogic gameLogic, StillModel aModel, Material material, Car car) {
		super(aModel, material);
		this.gameLogic = gameLogic;
		this.car = car;
		if (car == null) {
			throw new URacerRuntimeException("The specified Car doesn't exists!");
		}

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

	// TODO refactor collisionFactor, gameLogic shouldn't stay here!!!!
	public void transform (PerspectiveCamera camPersp, OrthographicCamera camOrtho) {
		// set position to car position
		EntityRenderState state = car.state();
		float s = 1;

		// weight transfer *simulation*
		float wt_body_angle = 0;
		if (car instanceof PlayerCar) {
			float sideangle_amount = 0;

			PlayerCar player = (PlayerCar)car;
			{
				float collisionFactor = 0;
				float sf = player.carState.currSpeedFactor;
				float front = -player.getSimulator().lateralForceFront.y * player.getCarModel().inv_max_grip;
				float rear = -player.getSimulator().lateralForceRear.y * player.getCarModel().inv_max_grip;
				float amount = MathUtils.clamp((front + rear) * 0.5f, -1, 1) / 0.75f;
				sideangle_amount = (80 * Math.abs(amount)) * sf * amount;
				// Gdx.app.log("", "" + amount);

				if (gameLogic != null) collisionFactor = gameLogic.getCollisionFactor();
				if (collisionFactor * 0.5f > 0) {
					float front_ratio = gameLogic.getCollisionFrontRatio();
					sideangle_amount += front_ratio * (200 * front) + (1 - front_ratio) * (200 * rear);
				}

				float max = 20;
				sideangle_amount = MathUtils.clamp(sideangle_amount, -max, max);

				float alpha = 0.05f;
				sideAngle.set(sideangle_amount, alpha);
				bodyAngle.set(sideAngle.get(), 1 - sf);
			}

			// Gdx.app.log("", "" + bodyAngle.get());
			wt_body_angle = bodyAngle.get();
		}

		// body
		{
			Vector3 pos = world2Dto3D(camPersp, camOrtho, state.position.x, state.position.y);

			mtxbody.idt();
			mtxbody.translate(pos);
			mtxbody.rotate(0, 0, 1, state.orientation);
			mtxbody.rotate(0, 1, 0, wt_body_angle);
			mtxbody.scale(this.scaleAxis.x * s, this.scaleAxis.y * s, this.scaleAxis.z * s);

			// comb = (proj * view) * model (fast mul)
			mtxbodytransformed.set(camPersp.combined).mul(mtxbody);
		}

		tireAngle.set(MathUtils.radDeg * car.getSteerAngleRads(), 0.1f);

		float offx = Convert.mt2px(0.97f) * s;
		float offy = Convert.mt2px(1.23f) * s;
		float cos = MathUtils.cosDeg(90 + state.orientation);
		float sin = MathUtils.sinDeg(90 + state.orientation);

		// left tire
		{
			float dx = offy * cos - offx * sin;
			float dy = offy * sin + offx * cos;
			Vector3 pos = world2Dto3D(camPersp, camOrtho, state.position.x + dx, state.position.y + dy);
			mtxltire.set(camPersp.combined);
			mtxltire.translate(pos);
			mtxltire.rotate(0, 0, 1, state.orientation);
			mtxltire.rotate(0, 1, 0, wt_body_angle);
			mtxltire.rotate(0, 0, 1, -tireAngle.get());
			mtxltire.scale(this.scaleAxis.x * s, this.scaleAxis.y * s, this.scaleAxis.z * s);
			mtxltiretransformed.set(mtxltire);
		}

		// right tire
		{
			float dx = offy * cos + offx * sin;
			float dy = offy * sin - offx * cos;
			Vector3 pos = world2Dto3D(camPersp, camOrtho, state.position.x + dx, state.position.y + dy);
			mtxrtire.set(camPersp.combined);
			mtxrtire.translate(pos);
			mtxrtire.rotate(0, 0, 1, state.orientation);
			mtxrtire.rotate(0, 1, 0, wt_body_angle);
			mtxrtire.rotate(0, 0, 1, -tireAngle.get());
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
