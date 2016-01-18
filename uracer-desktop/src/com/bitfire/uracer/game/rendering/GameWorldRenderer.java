
package com.bitfire.uracer.game.rendering;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.helpers.CameraController;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.models.CarStillModel;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.game.world.models.TrackTrees;
import com.bitfire.uracer.game.world.models.TrackWalls;
import com.bitfire.uracer.game.world.models.TreeStillModel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.u3d.still.StillSubMesh;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.uracer.utils.URacerRuntimeException;
import com.bitfire.utils.ShaderLoader;

import box2dLight.ConeLight;
import box2dLight.RayHandler;

public final class GameWorldRenderer {
	// @off
	private static final String treeVertexShader =
		"uniform mat4 u_projTrans;							\n" +
		"attribute vec4 a_position;						\n" +
		"attribute vec2 a_texCoord0;						\n" +
		"varying vec2 v_TexCoord;							\n" +
		"void main()											\n" +
		"{															\n" +
		"	gl_Position = u_projTrans * a_position;	\n" +
		"	v_TexCoord = a_texCoord0;						\n" +
		"}															\n";

	private static final String treeFragmentShader =
		"#ifdef GL_ES														\n" +
		"precision mediump float;										\n" +
		"#endif																\n" +
		"uniform sampler2D u_texture;									\n" +
		"varying vec2 v_TexCoord;										\n" +
		"void main()														\n" +
		"{																		\n" +
		"	vec4 texel = texture2D( u_texture, v_TexCoord );	\n" +
		"	if(texel.a < 0.25) discard;								\n" +
		"	gl_FragColor = texel;										\n" +
		"}																		\n";

	private static final String treeFragmentShaderNight =
		"#ifdef GL_ES																						\n" +
		"precision mediump float;																		\n" +
		"#endif																								\n" +
		"uniform sampler2D u_texture;																	\n" +
		"uniform vec4 u_ambient;																		\n" +
		"varying vec2 v_TexCoord;																		\n" +
		"void main()																						\n" +
		"{																										\n" +
		"	vec4 texel = texture2D( u_texture, v_TexCoord );									\n" +
		"	if(texel.a < 0.25) discard;																\n" +
		"	vec4 c = vec4((u_ambient.rgb + texel.rgb*texel.a)*u_ambient.a, texel.a);	\n" +
		"	gl_FragColor = c;																				\n" +
		"}																										\n";
	// @on

	// the game world
	private GameWorld world = null;

	// camera view
	protected PerspectiveCamera camPersp = null;
	protected OrthographicCamera camTilemap = null;
	protected OrthographicCamera camOrtho = null;
	protected Vector2 halfViewport = new Vector2();
	protected Rectangle camOrthoRect = new Rectangle();
	private Matrix4 camOrthoMvpMt = new Matrix4();
	private Matrix4 camPerspInvView = new Matrix4();
	private Matrix4 camPerspPrevViewProj = new Matrix4();
	private Matrix4 camPerspInvProj = new Matrix4();
	private CameraController camController;
	private static final float CamPerspPlaneNear = 0.001f;
	public static final float CamPerspPlaneFar = 240f;
	public static final float MinCameraZoom = 1f;
	public static final float MaxCameraZoom = 1.5f;
	public static final float ZoomRange = GameWorldRenderer.MaxCameraZoom - GameWorldRenderer.MinCameraZoom;
	public static final float ZoomWindow = 0.2f * ZoomRange;

	public static final float CamPerspElevation = 100f;
	private final float DefaultSsaoScale = 1f / 48f;

	// rendering
	private GL20 gl = null;
	private ShaderProgram treeShader = null, treeShaderNight = null;
	private boolean renderPlayerHeadlights = true;
	private final Matrix4 xform = new Matrix4();

	public OrthogonalTiledMapRenderer tileMapRenderer = null;

	// deferred stuff
	private Mesh plane = null;
	private FrameBuffer normalDepthMap = null;
	private ShaderProgram shNormalDepth = null, shNormalDepthNoDiffuse = null;
	private boolean useDeferredRendering = false;

	// render stats
	public static int renderedTrees = 0;
	public static int renderedWalls = 0;
	public static int culledMeshes = 0;

	// world refs
	private RayHandler rayHandler = null;
	private List<OrthographicAlignedStillModel> staticMeshes = null;
	private TrackTrees trackTrees = null;
	private TrackWalls trackWalls = null;
	private ConeLight playerLightsA = null, playerLightsB = null;
	private GhostCar topmostGhost = null;

	public GameWorldRenderer (GameWorld world, boolean useNormalDepthMap) {
		this.world = world;
		this.useDeferredRendering = useNormalDepthMap;
		gl = Gdx.gl20;
		rayHandler = world.getRayHandler();
		playerLightsA = world.getPlayerHeadLights(true);
		playerLightsB = world.getPlayerHeadLights(false);
		staticMeshes = world.getStaticMeshes();

		xform.idt();
		xform.scale(ScaleUtils.Scale, ScaleUtils.Scale, 1);

		createCams();

		tileMapRenderer = new OrthogonalTiledMapRenderer(world.map);

		trackTrees = world.getTrackTrees();
		treeShader = ShaderLoader.fromString(treeVertexShader, treeFragmentShader, "tree-fragment", "tree-vertex");
		if (treeShader == null || !treeShader.isCompiled()) {
			throw new URacerRuntimeException("Couldn't load tree shader, log:" + treeShader.getLog());
		}

		treeShaderNight = ShaderLoader.fromString(treeVertexShader, treeFragmentShaderNight, "tree-fragment-night", "tree-vertex");
		if (treeShaderNight == null || !treeShaderNight.isCompiled()) {
			throw new URacerRuntimeException("Couldn't load night tree shader, log:" + treeShaderNight.getLog());
		}

		trackWalls = world.getTrackWalls();

		// deferred setup
		if (useNormalDepthMap) {
			float scale = Config.PostProcessing.NormalDepthMapRatio;
			normalDepthMap = new FrameBuffer(Format.RGBA8888, (int)((float)ScaleUtils.PlayWidth * scale),
				(int)((float)ScaleUtils.PlayHeight * scale), true);

			shNormalDepth = ShaderLoader.fromFile("normaldepth", "normaldepth", "#define ENABLE_DIFFUSE");
			shNormalDepthNoDiffuse = ShaderLoader.fromFile("normaldepth", "normaldepth");
			createBackPlane();
		}
	}

	public void dispose () {
		if (useDeferredRendering) {
			plane.dispose();
			shNormalDepth.dispose();
			shNormalDepthNoDiffuse.dispose();
			normalDepthMap.dispose();
		}

		treeShaderNight.dispose();
		treeShader.dispose();

		tileMapRenderer.dispose();
	}

	// permit to the tilemap to appear as a flat surface with the normal pointing upward, towards the camera
	private void createBackPlane () {
		plane = new Mesh(true, 4, 4, new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
			new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE));

		// @formatter:off
		float size = 10f;
		float verts[] = {-size / 2, 0, size / 2, size / 2, 0, size / 2, size / 2, 0, -size / 2, -size / 2, 0, -size / 2};
		// float verts[] = {size, 0, size, size, 0, 0, 0, 0, 0, 0, 0, size};

		float normals[] = {0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0};
		// @formatter:on

		int vidx = 0, nidx = 0;
		int length = 6 * 4;
		float[] vertices = new float[length];
		for (int i = 0; i < length;) {
			vertices[i++] = verts[vidx++];
			vertices[i++] = verts[vidx++];
			vertices[i++] = verts[vidx++];
			vertices[i++] = normals[nidx++];
			vertices[i++] = normals[nidx++];
			vertices[i++] = normals[nidx++];
		}

		plane.setVertices(vertices);
		plane.setIndices(new short[] {0, 1, 2, 3});
	}

	private void createCams () {
		int refW = Config.Graphics.ReferenceScreenWidth;
		int refH = Config.Graphics.ReferenceScreenHeight;

		camOrtho = new OrthographicCamera(refW, refH);
		halfViewport.set(camOrtho.viewportWidth / 2, camOrtho.viewportHeight / 2);

		// creates and setup orthographic camera
		camTilemap = new OrthographicCamera(refW, refH);
		camTilemap.zoom = 1;

		// creates and setup perspective camera
		// strategically choosen near/far planes, Blender models' 14.2 meters <=> one 256px tile
		// with far plane @48
		camPersp = new PerspectiveCamera(47.27123f, refW, refH);
		camPersp.near = CamPerspPlaneNear;
		camPersp.far = CamPerspPlaneFar;
		camPersp.lookAt(0, 0, -1);
		camPersp.position.set(camTilemap.position.x, camTilemap.position.y, CamPerspElevation);
		camPersp.update();

		camController = new CameraController(Config.Graphics.CameraInterpolationMode, halfViewport, world.worldSizePx,
			world.worldSizeTiles);
	}

	public OrthographicCamera getOrthographicCamera () {
		return camOrtho;
	}

	public PerspectiveCamera getPerspectiveCamera () {
		return camPersp;
	}

	public Matrix4 getOrthographicMvpMt () {
		return camOrthoMvpMt;
	}

	public void setRenderPlayerHeadlights (boolean value) {
		renderPlayerHeadlights = value;
		if (playerLightsA != null) playerLightsA.setActive(value);
		if (playerLightsB != null) playerLightsB.setActive(value);
	}

	public FrameBuffer getNormalDepthMap () {
		return normalDepthMap;
	}

	public Matrix4 getInvView () {
		return camPerspInvView;
	}

	public Matrix4 getPrevViewProj () {
		return camPerspPrevViewProj;
	}

	public Matrix4 getInvProj () {
		return camPerspInvProj;
	}

	public void resetCounters () {
		culledMeshes = 0;
		renderedTrees = 0;
		renderedWalls = 0;
	}

	private Vector2 _o2p = new Vector2();

	private Vector2 orientationToPosition (Car car, float angle, float offsetX, float offsetY) {
		Vector2 carPosition = car.state().position;
		float carLength = car.getCarModel().length;

		// the body's compound shape should be created with some clever thinking
		float offx = (carLength / 2f) + .25f + offsetX;
		float offy = offsetY;

		float cos = MathUtils.cosDeg(angle);
		float sin = MathUtils.sinDeg(angle);
		float dX = offx * cos - offy * sin;
		float dY = offx * sin + offy * cos;

		float mx = Convert.px2mt(carPosition.x) + dX;
		float my = Convert.px2mt(carPosition.y) + dY;
		_o2p.set(mx, my);
		return _o2p;
	}

	public void updatePlayerHeadlights (Car car) {
		boolean hasCar = (car != null);

		// update player light (subframe interpolation ready)
		float ang = 90 + car.state().orientation;

		if (hasCar) {

			if (renderPlayerHeadlights) {
				Vector2 v = orientationToPosition(car, ang, 0, 0.5f);
				playerLightsA.setDirection(ang + 5);
				playerLightsA.setPosition(v.x, v.y);

				v = orientationToPosition(car, ang, 0, -0.5f);
				playerLightsB.setDirection(ang - 5);
				playerLightsB.setPosition(v.x, v.y);
			}
		}
	}

	private Color ambientColor = new Color(0.1f, 0.05f, 0.1f, 0.4f);
	private Color treesAmbientColor = new Color(0.1f, 0.05f, 0.1f, 0.4f);

	public Color getAmbientColor () {
		return ambientColor;
	}

	public Color getTreesAmbientColor () {
		return treesAmbientColor;
	}

	private Vector2 cameraPos = new Vector2();
	private float cameraZoom = 1;

	public Vector2 getCameraPosition () {
		return cameraPos;
	}

	public void setCameraPosition (Vector2 positionPx) {
		cameraPos.set(camController.transform(positionPx, cameraZoom));
	}

	public void setCameraZoom (float zoom) {
		cameraZoom = zoom;
	}

	// NOTE: do not use camOrtho.zoom directly since it will be bound later at updateCamera!
	public float getCameraZoom () {
		return cameraZoom;
	}

	public void updateCamera () {
		// update orthographic camera

		float zoom = 1f / cameraZoom;

		// remove subpixel accuracy (jagged behavior) by uncommenting the round
		camOrtho.position.x = /* MathUtils.round */(cameraPos.x);
		camOrtho.position.y = /* MathUtils.round */(cameraPos.y);
		camOrtho.position.z = 0;
		camOrtho.zoom = zoom;
		camOrtho.update();

		halfViewport.set(camOrtho.viewportWidth / 2, camOrtho.viewportHeight / 2);

		// update the unscaled orthographic camera rectangle, for visibility queries
		camOrthoRect.set(camOrtho.position.x - halfViewport.x, camOrtho.position.y - halfViewport.y, camOrtho.viewportWidth,
			camOrtho.viewportHeight);

		// update the model-view-projection matrix, in meters, from the unscaled orthographic camera
		camOrthoMvpMt.set(camOrtho.combined);
		camOrthoMvpMt.scl(Config.Physics.PixelsPerMeter);

		// update the tilemap renderer orthographic camera
		// y-down
		camTilemap.up.set(0, -1, 0);
		camTilemap.direction.set(0, 0, 1);
		camTilemap.position.set(camOrtho.position);
		camTilemap.position.y = world.worldSizePx.y - camTilemap.position.y;
		camTilemap.zoom = zoom;
		camTilemap.update();

		// update previous proj view
		camPerspPrevViewProj.set(camPersp.projection).mul(camPersp.view);

		// sync perspective camera to the orthographic camera
		camPersp.near = 1f;
		camPersp.position.set(cameraPos.x, cameraPos.y, CamPerspElevation);
		camPersp.update(true);

		// update inv matrices
		camPerspInvView.set(camPersp.view);
		Matrix4.inv(camPerspInvView.val);

		camPerspInvProj.set(camPersp.projection);
		Matrix4.inv(camPerspInvProj.val);
	}

	public void updateRayHandler () {
		if (rayHandler != null) {

			rayHandler.setAmbientLight(ambientColor);

			// @off
			rayHandler.setCombinedMatrix(
				camOrthoMvpMt,
				Convert.px2mt(camOrtho.position.x),
				Convert.px2mt(camOrtho.position.y),
				Convert.px2mt(camOrtho.viewportWidth * camOrtho.zoom),
				Convert.px2mt(camOrtho.viewportHeight * camOrtho.zoom));
			// @on

			rayHandler.update();
			// Gdx.app.log("GameWorldRenderer", "lights rendered=" + rayHandler.lightRenderedLastFrame);

			rayHandler.updateLightMap();
		}
	}

	private void setSsaoScale (float scale) {
		shNormalDepth.begin();
		shNormalDepth.setUniformf("inv_depth_scale", scale);
		shNormalDepth.end();
	}

	private void updateSsaoPlanes () {
		shNormalDepth.begin();
		shNormalDepth.setUniformf("near", camPersp.near);
		shNormalDepth.setUniformf("far", camPersp.far);
		shNormalDepth.end();
	}

	public void updateNormalDepthMap () {

		gl.glCullFace(GL20.GL_BACK);
		gl.glFrontFace(GL20.GL_CCW);
		gl.glEnable(GL20.GL_CULL_FACE);
		gl.glDisable(GL20.GL_BLEND);

		gl.glEnable(GL20.GL_DEPTH_TEST);
		gl.glDepthFunc(GL20.GL_LESS);
		gl.glDepthMask(true);

		setSsaoScale(DefaultSsaoScale);
		updateSsaoPlanes();

		normalDepthMap.begin();
		{
			gl.glClearDepthf(1f);
			gl.glClearColor(0, 0, 0, 1);
			gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			// renderAllMeshes(true);
			renderTilemapPlane();
			renderCars(true);
			renderWalls(true);

			// if (staticMeshes.size() > 0) {
			// gl.glEnable(GL20.GL_DEPTH_TEST);
			// gl.glDepthFunc(GL20.GL_LESS);
			//
			// renderOrthographicAlignedModels(staticMeshes, true);
			// }

			renderTrees(true);
		}
		normalDepthMap.end();
	}

	private void renderTilemapPlane () {
		ShaderProgram shader = shNormalDepthNoDiffuse;
		float meshZ = -(camPersp.far - camPersp.position.z) + (camPersp.far * (1 - (camOrtho.zoom)));
		float k = OrthographicAlignedStillModel.BlenderToURacer;
		float scalex = 6, scalez = 4;

		Matrix4 model = mtx;
		tmpvec.set(camPersp.position.x, camPersp.position.y, meshZ + 0.5f);

		model.idt();
		model.translate(tmpvec);
		model.rotate(1, 0, 0, 90);
		model.scale(scalex * k, 1, scalez * k);

		mtx2.set(camPersp.view).mul(model);
		nmat.set(mtx2).inv().transpose();

		shader.begin();
		shader.setUniformf("inv_depth_scale", DefaultSsaoScale);
		shader.setUniformf("near", camPersp.near);
		shader.setUniformf("far", camPersp.far);
		shader.setUniformMatrix("proj", camPersp.projection);
		shader.setUniformMatrix("view", camPersp.view);
		shader.setUniformMatrix("nmat", nmat);
		shader.setUniformMatrix("model", model);
		plane.render(shader, GL20.GL_TRIANGLE_FAN);
		shader.end();
	}

	public void renderLigthMap (FrameBuffer dest) {
		rayHandler.renderLightMap(ScaleUtils.PlayViewport, dest);
	}

	public void renderStaticMeshes () {
		if (staticMeshes.size() > 0) {
			// render "static-meshes" layer
			gl.glEnable(GL20.GL_CULL_FACE);
			gl.glFrontFace(GL20.GL_CCW);
			gl.glCullFace(GL20.GL_BACK);
			renderOrthographicAlignedModels(staticMeshes, false, world.isNightMode());
		}
	}

	public void renderTilemap () {
		gl.glDisable(GL20.GL_DEPTH_TEST);
		gl.glDisable(GL20.GL_CULL_FACE);
		gl.glDisable(GL20.GL_BLEND);
		gl.glActiveTexture(GL20.GL_TEXTURE0);
		tileMapRenderer.setView(camTilemap);
		tileMapRenderer.getSpriteBatch().disableBlending();
		tileMapRenderer.render();
	}

	public void renderWalls (boolean depthOnly) {
		if (trackWalls.count() > 0) {
			renderWalls(trackWalls, depthOnly);
		}
	}

	public void renderTrees (boolean depthOnly) {
		if (trackTrees.count() > 0) {
			renderTrees(trackTrees, depthOnly);
		}
	}

	private void renderWalls (TrackWalls walls, boolean depthOnly) {
		if (!depthOnly) {
			gl.glEnable(GL20.GL_BLEND);
			gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		}

		gl.glDisable(GL20.GL_CULL_FACE);
		renderedWalls = renderOrthographicAlignedModels(walls.models, depthOnly, false);
	}

	private void renderTrees (TrackTrees trees, boolean depthOnly) {
		trees.transform(camPersp, camOrtho);

		ShaderProgram shader = null;
		if (depthOnly) {
			shader = shNormalDepth;
		} else {
			if (world.isNightMode()) {
				shader = treeShaderNight;
			} else {
				shader = treeShader;
			}
		}

		gl.glDisable(GL20.GL_CULL_FACE);
		gl.glDisable(GL20.GL_BLEND);

		shader.begin();
		Art.meshTreeTrunk.bind();

		if (depthOnly) {
			shader.setUniformMatrix("proj", camPersp.projection);
			shader.setUniformMatrix("view", camPersp.view);
			shader.setUniformi("u_texture", 0);
		} else {
			if (world.isNightMode()) {
				shader.setUniformf("u_ambient", treesAmbientColor);
			}
		}

		// all the trunks
		for (int i = 0; i < trees.models.size(); i++) {
			TreeStillModel m = trees.models.get(i);

			if (!depthOnly) {
				shader.setUniformMatrix("u_projTrans", m.transformed);
			} else {
				mtx.set(camPersp.view).mul(m.mtxmodel);
				nmat.set(mtx).inv().transpose();
				shader.setUniformMatrix("nmat", nmat);
				shader.setUniformMatrix("model", m.mtxmodel);
			}

			m.trunk.render(shader, m.smTrunk.primitiveType);
		}

		// all the transparent foliage

		// do NOT cull faces so that SSAO appear on backfaces as well
		if (!depthOnly) {
			gl.glEnable(GL20.GL_BLEND);
			gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		} else {
			gl.glDisable(GL20.GL_CULL_FACE);
		}

		boolean needRebind = false;
		for (int i = 0; i < trees.models.size(); i++) {
			TreeStillModel m = trees.models.get(i);
			if (m.leaves == null) {
				needRebind = true;
				continue;
			}

			if (Config.Debug.FrustumCulling && !camPersp.frustum.boundsInFrustum(m.boundingBox)) {
				needRebind = true;
				if (!depthOnly) culledMeshes++;
				continue;
			}

			if (!depthOnly) {
				shader.setUniformMatrix("u_projTrans", m.transformed);
			} else {
				mtx.set(camPersp.view).mul(m.mtxmodel);
				nmat.set(mtx).inv().transpose();
				shader.setUniformMatrix("nmat", nmat);
				shader.setUniformMatrix("model", m.mtxmodel);
			}

			if (i == 0 || needRebind) {
				m.material.bind(shader);
			} else if (!trees.models.get(i - 1).material.equals(m.material)) {
				m.material.bind(shader);
			}

			m.leaves.render(shader, m.smLeaves.primitiveType);

			renderedTrees++;
		}

		shader.end();
	}

	private boolean renderCar (Car car, boolean depthOnly, boolean nightMode) {
		CarStillModel model = car.getStillModel();

		if (Config.Debug.FrustumCulling && !camPersp.frustum.boundsInFrustum(model.boundingBox)) {
			return false;
		}

		ShaderProgram shader = null;
		if (depthOnly) {
			shader = shNormalDepth;
		} else {
			if (nightMode) {
				shader = OrthographicAlignedStillModel.shaderNight;
			} else {
				shader = OrthographicAlignedStillModel.shader;
			}
		}

		gl.glEnable(GL20.GL_CULL_FACE);

		shader.begin();
		Art.meshCar.get(car.getCarPreset().type.regionName).bind();

		// common matrices
		if (depthOnly) {
			shader.setUniformMatrix("proj", camPersp.projection);
			shader.setUniformMatrix("view", camPersp.view);
			shader.setUniformi("u_texture", 0);
		} else {
			shader.setUniformf("alpha", model.getAlpha());
			if (nightMode) {
				shader.setUniformf("u_ambient", treesAmbientColor);
			}
		}

		// car body
		{
			if (depthOnly) {
				mtx.set(camPersp.view).mul(model.mtxbody);
				nmat.set(mtx).inv().transpose();
				shader.setUniformMatrix("nmat", nmat);
				shader.setUniformMatrix("model", model.mtxbody);
			} else {
				shader.setUniformMatrix("u_projTrans", model.mtxbodytransformed);
			}

			model.body.render(shader, model.smBody.primitiveType);
		}

		// car left tire
		{
			if (depthOnly) {
				mtx.set(camPersp.view).mul(model.mtxltire);
				nmat.set(mtx).inv().transpose();
				shader.setUniformMatrix("nmat", nmat);
				shader.setUniformMatrix("model", model.mtxltire);
			} else {
				shader.setUniformMatrix("u_projTrans", model.mtxltiretransformed);
			}

			model.leftTire.render(shader, model.smLeftTire.primitiveType);
		}

		// car right tire
		{
			if (depthOnly) {
				mtx.set(camPersp.view).mul(model.mtxrtire);
				nmat.set(mtx).inv().transpose();
				shader.setUniformMatrix("nmat", nmat);
				shader.setUniformMatrix("model", model.mtxrtire);
			} else {
				shader.setUniformMatrix("u_projTrans", model.mtxrtiretransformed);
			}

			model.rightTire.render(shader, model.smRightTire.primitiveType);
		}

		return true;
	}

	public void setTopMostGhostCar (GhostCar ghost) {
		topmostGhost = ghost;
	}

	public void renderCars (boolean depthOnly) {
		CarStillModel model;

		// Art.meshCar.bind();

		if (!depthOnly) {
			gl.glEnable(GL20.GL_BLEND);
			gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		}

		// ghosts
		GhostCar[] ghosts = world.getGhostCars();
		if (ghosts != null && ghosts.length > 0) {
			if (topmostGhost != null) {
				// already transformed
				model = topmostGhost.getStillModel();
				if (model.getAlpha() > 0) {
					model.transform(camPersp, camOrtho);
					if (depthOnly) {
						float ca = model.getAlpha();
						float a = (ca - 0.5f) * 2;
						float s = AMath.clampf(AMath.sigmoid(a * 3f + 4f - (1 - ca)), 0, 1);
						setSsaoScale(DefaultSsaoScale * s);
					}

					renderCar(topmostGhost, depthOnly, false);
				}
			}

			for (int i = 0; i < ghosts.length; i++) {
				GhostCar ghost = ghosts[i];
				if (depthOnly && !ghost.isSsaoReady()) continue;

				model = ghost.getStillModel();
				if (model.getAlpha() <= 0) continue;

				model.transform(camPersp, camOrtho);
				if (depthOnly) {
					float ca = model.getAlpha();
					float a = (ca - 0.5f) * 2;
					float s = AMath.clampf(AMath.sigmoid(a * 3f + 4f - (1 - ca)), 0, 1);
					setSsaoScale(DefaultSsaoScale * s);
				}

				// found a topmost ghost, render last
				if (ghost != topmostGhost) {
					renderCar(ghost, depthOnly, false);
				}
			}
		}

		if (depthOnly) {
			setSsaoScale(DefaultSsaoScale);
		}

		// player
		if (!depthOnly) {
			gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
		}

		PlayerCar player = world.getPlayer();
		if (player != null) {
			model = player.getStillModel();
			model.transform(camPersp, camOrtho);
			renderCar(player, depthOnly, false);
		}

		if (!depthOnly) {
			gl.glDisable(GL20.GL_BLEND);
		}
	}

	private Vector3 tmpvec = new Vector3();
	private Matrix4 mtx = new Matrix4();
	private Matrix3 nmat = new Matrix3();
	private Matrix4 mtx2 = new Matrix4();

	private int renderOrthographicAlignedModels (List<OrthographicAlignedStillModel> models, boolean depthOnly,
		boolean nightMode) {
		int renderedCount = 0;
		OrthographicAlignedStillModel m;
		StillSubMesh submesh;

		float meshZ = -(camPersp.far - camPersp.position.z) + (camPersp.far * (1 - (camOrtho.zoom)));

		ShaderProgram shader = null;

		if (depthOnly) {
			shader = shNormalDepth;
		} else {
			if (nightMode) {
				shader = OrthographicAlignedStillModel.shaderNight;
			} else {
				shader = OrthographicAlignedStillModel.shader;
			}
		}

		shader.begin();

		if (depthOnly) {
			shader.setUniformMatrix("proj", camPersp.projection);
			shader.setUniformMatrix("view", camPersp.view);
			shader.setUniformi("u_texture", 0);
		} else {
			if (nightMode) {
				shader.setUniformf("u_ambient", ambientColor);
			}
		}

		boolean needRebind = false;
		for (int i = 0; i < models.size(); i++) {
			m = models.get(i);
			submesh = m.model.subMeshes[0];

			// transform position
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
			Matrix4 model = mtx;
			tmpvec.z = meshZ;

			model.idt();
			model.translate(tmpvec);
			model.rotate(m.iRotationAxis, m.iRotationAngle);
			model.scale(m.scaleAxis.x, m.scaleAxis.y, m.scaleAxis.z);

			// ensure the bounding box is transformed
			m.boundingBox.inf().set(m.localBoundingBox);
			m.boundingBox.mul(model);

			// perform culling
			if (Config.Debug.FrustumCulling && !camPersp.frustum.boundsInFrustum(m.boundingBox)) {
				needRebind = true;
				if (!depthOnly) culledMeshes++;
				continue;
			}

			if (!depthOnly) {
				// comb = (proj * view) * model (fast mul)
				Matrix4 mvp = mtx2;
				mvp.set(camPersp.combined).mul(model);
				shader.setUniformMatrix("u_projTrans", mvp);
				shader.setUniformf("alpha", m.getAlpha());
			} else {
				mtx2.set(camPersp.view).mul(model);
				nmat.set(mtx2).inv().transpose();
				shader.setUniformMatrix("nmat", nmat);
				shader.setUniformMatrix("model", model);
			}

			if (i == 0 || needRebind) {
				m.material.bind(shader);
			} else if (!models.get(i - 1).material.equals(m.material)) {
				m.material.bind(shader);
			}

			submesh.mesh.render(shader, submesh.primitiveType);
			renderedCount++;
		}

		shader.end();
		return renderedCount;
	}

	// private int renderOrthographicAlignedModel (OrthographicAlignedStillModel aModel, boolean depthOnly, boolean nightMode) {
	// int renderedCount = 0;
	// OrthographicAlignedStillModel m = aModel;
	// StillSubMesh submesh;
	//
	// float meshZ = -(camPersp.far - camPersp.position.z) + (camPersp.far * (1 - (camOrtho.zoom)));
	//
	// ShaderProgram shader = null;
	//
	// if (depthOnly) {
	// shader = shNormalDepth;
	// } else {
	// if (nightMode) {
	// shader = OrthographicAlignedStillModel.shaderNight;
	// } else {
	// shader = OrthographicAlignedStillModel.shader;
	// }
	// }
	//
	// shader.begin();
	//
	// if (depthOnly) {
	// shader.setUniformMatrix("proj", camPersp.projection);
	// shader.setUniformMatrix("view", camPersp.view);
	// shader.setUniformi("u_texture", 0);
	// } else {
	// if (nightMode) {
	// shader.setUniformf("u_ambient", ambientColor);
	// }
	// }
	//
	// {
	// submesh = m.model.subMeshes[0];
	//
	// // transform position
	// tmpvec.x = (m.positionOffsetPx.x - camPersp.position.x) + (camPersp.viewportWidth / 2) + m.positionPx.x;
	// tmpvec.y = (m.positionOffsetPx.y + camPersp.position.y) + (camPersp.viewportHeight / 2) - m.positionPx.y;
	// tmpvec.z = 1;
	//
	// tmpvec.x *= ScaleUtils.Scale;
	// tmpvec.y *= ScaleUtils.Scale;
	//
	// tmpvec.x += ScaleUtils.CropX;
	// tmpvec.y += ScaleUtils.CropY;
	//
	// // transform to world space
	// camPersp.unproject(tmpvec, ScaleUtils.CropX, ScaleUtils.CropY, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
	//
	// // build model matrix
	// Matrix4 model = mtx;
	// tmpvec.z = meshZ;
	//
	// model.idt();
	// model.translate(tmpvec);
	// model.rotate(m.iRotationAxis, m.iRotationAngle);
	// model.scale(m.scaleAxis.x, m.scaleAxis.y, m.scaleAxis.z);
	//
	// // ensure the bounding box is transformed
	// m.boundingBox.inf().set(m.localBoundingBox);
	// m.boundingBox.mul(model);
	//
	// // perform culling
	// if (Config.Debug.FrustumCulling && !camPersp.frustum.boundsInFrustum(m.boundingBox)) {
	// if (!depthOnly) culledMeshes++;
	// } else {
	// if (!depthOnly) {
	// // comb = (proj * view) * model (fast mul)
	// Matrix4 mvp = mtx2;
	// mvp.set(camPersp.combined).mul(model);
	// shader.setUniformMatrix("u_projTrans", mvp);
	// shader.setUniformf("alpha", m.getAlpha());
	// } else {
	// mtx2.set(camPersp.view).mul(model);
	// nmat.set(mtx2).inv().transpose();
	// shader.setUniformMatrix("nmat", nmat);
	// shader.setUniformMatrix("model", model);
	// }
	//
	// m.material.bind(shader);
	//
	// submesh.mesh.render(shader, submesh.primitiveType);
	// renderedCount++;
	// }
	// }
	//
	// shader.end();
	//
	// if (!depthOnly && Config.Debug.Render3DBoundingBoxes) {
	// // debug (tested on a single mesh only!)
	// renderBoundingBox(m.boundingBox);
	// }
	//
	// return renderedCount;
	// }

}
