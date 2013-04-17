
package com.bitfire.uracer.game.rendering;

import java.util.List;

import box2dLight.ConeLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.model.still.StillSubMesh;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.logic.helpers.CameraController;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.game.world.models.TrackTrees;
import com.bitfire.uracer.game.world.models.TrackWalls;
import com.bitfire.uracer.game.world.models.TreeStillModel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.utils.ShaderLoader;

public final class GameWorldRenderer {
	// @off
	private static final String treeVertexShader =
		"uniform mat4 u_projTrans;					\n" +
		"attribute vec4 a_position;				\n" +
		"attribute vec2 a_texCoord0;				\n" +
		"varying vec2 v_TexCoord;					\n" +
		"void main()									\n" +
		"{\n" +
		"	gl_Position = u_projTrans * a_position;	\n" +
		"	v_TexCoord = a_texCoord0;						\n" +
		"}\n";
	
	private static final String treeFragmentShader =
		"#ifdef GL_ES											\n" +
			"precision mediump float;							\n" +
			"#endif													\n" +
			"uniform sampler2D u_texture;						\n" +
			"varying vec2 v_TexCoord;							\n" +
			"void main()											\n" +
			"{\n" +
			"	vec4 texel = texture2D( u_texture, v_TexCoord );	\n" +
			"	if(texel.a < 0.5) discard;							\n" +
			"	gl_FragColor = texel;								\n" +
			"}\n";

	private static final String treeFragmentShaderNight =
		"#ifdef GL_ES											\n" +
		"precision mediump float;							\n" +
		"#endif													\n" +
		"uniform sampler2D u_texture;						\n" +
		"uniform vec4 u_ambient;						\n" +
		"varying vec2 v_TexCoord;							\n" +
		"void main()											\n" +
		"{\n" +
		"	vec4 texel = texture2D( u_texture, v_TexCoord );	\n" +
		"	if(texel.a < 0.5) discard;							\n" +
		"	vec4 c = vec4((u_ambient.rgb + texel.rgb*texel.a)*u_ambient.a, texel.a);	\n" +
		"	gl_FragColor = c;								\n" +
		"}\n";
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
	private static final float CamPerspPlaneNear = 1;
	public static final float CamPerspPlaneFar = 240;
	public static final float MaxCameraZoom = 1.4f;
	public static final float CamPerspElevation = 100f;

	// rendering
	private GL20 gl = null;
	private ShaderProgram treeShader = null, treeShaderNight = null;
	private boolean renderPlayerHeadlights = true;
	private final Matrix4 xform = new Matrix4();

	public OrthogonalTiledMapRenderer tileMapRenderer = null;
	private GameTrackDebugRenderer gameTrackDbgRenderer = null;
	private ScalingStrategy scalingStrategy = null;

	// deferred stuff
	private Mesh plane;
	// private final FloatFrameBuffer normalDepthMap;
	private final FrameBuffer normalDepthMap;
	private final ShaderProgram shNormalDepthAlpha, shNormalDepth;

	// render stats
	private ImmediateModeRenderer20 dbg = new ImmediateModeRenderer20(false, true, 0);
	public static int renderedTrees = 0;
	public static int renderedWalls = 0;
	public static int culledMeshes = 0;

	// world refs
	private RayHandler rayHandler = null;
	private List<OrthographicAlignedStillModel> staticMeshes = null;
	private boolean showComplexTrees = false;
	private boolean showWalls = false;
	private TrackTrees trackTrees = null; // complex trees
	private TrackWalls trackWalls = null;
	private ConeLight playerLightsA = null, playerLightsB = null;

	public GameWorldRenderer (ScalingStrategy strategy, GameWorld world) {
		scalingStrategy = strategy;
		this.world = world;
		gl = Gdx.gl20;
		rayHandler = world.getRayHandler();
		playerLightsA = world.getPlayerHeadLights(true);
		playerLightsB = world.getPlayerHeadLights(false);
		staticMeshes = world.getStaticMeshes();

		xform.idt();
		xform.scale(ScaleUtils.Scale, ScaleUtils.Scale, 1);

		createCams();

		tileMapRenderer = new OrthogonalTiledMapRenderer(world.map);
		gameTrackDbgRenderer = new GameTrackDebugRenderer(world.getGameTrack());

		showComplexTrees = UserPreferences.bool(Preference.ComplexTrees);
		showWalls = UserPreferences.bool(Preference.Walls);

		if (showComplexTrees) {
			trackTrees = world.getTrackTrees();
			treeShader = ShaderLoader.fromString(treeVertexShader, treeFragmentShader, "tree-fragment", "tree-vertex");
			if (treeShader == null || !treeShader.isCompiled()) {
				throw new IllegalStateException(treeShader.getLog());
			}

			treeShaderNight = ShaderLoader.fromString(treeVertexShader, treeFragmentShaderNight, "tree-fragment-night",
				"tree-vertex");
			if (treeShaderNight == null || !treeShaderNight.isCompiled()) {
				throw new IllegalStateException(treeShaderNight.getLog());
			}

		} else {
			trackTrees = null;
			treeShader = null;
			treeShaderNight = null;
		}

		if (showWalls) {
			trackWalls = world.getTrackWalls();
		}

		// deferred setup
		float scale = Config.PostProcessing.NormalDepthMapScale;
		normalDepthMap = new FrameBuffer(Format.RGBA8888, (int)((float)ScaleUtils.PlayWidth * scale),
			(int)((float)ScaleUtils.PlayHeight * scale), true);

		shNormalDepthAlpha = ShaderLoader.fromFile("normaldepth", "normaldepth", "#define ENABLE_DIFFUSE");
		shNormalDepthAlpha.begin();
		shNormalDepthAlpha.setUniformf("near", camPersp.near);
		shNormalDepthAlpha.setUniformf("far", camPersp.far);
		shNormalDepthAlpha.end();

		shNormalDepth = ShaderLoader.fromFile("normaldepth", "normaldepth");
		shNormalDepth.begin();
		shNormalDepth.setUniformf("near", camPersp.near);
		shNormalDepth.setUniformf("far", camPersp.far);
		shNormalDepth.end();

		createBackPlane();
	}

	public void dispose () {
		shNormalDepthAlpha.dispose();
		shNormalDepth.dispose();
		normalDepthMap.dispose();

		tileMapRenderer.dispose();
		plane.dispose();
	}

	// permit to the tilemap to appear as a flat surface with the normal pointing upward, towards the camera
	private void createBackPlane () {
		plane = new Mesh(true, 4, 4, new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(
			Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE));

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
		camOrtho = new OrthographicCamera(ScaleUtils.RefScreenWidth, ScaleUtils.RefScreenHeight);
		halfViewport.set(camOrtho.viewportWidth / 2, camOrtho.viewportHeight / 2);

		// creates and setup orthographic camera
		camTilemap = new OrthographicCamera(ScaleUtils.RefScreenWidth, ScaleUtils.RefScreenHeight);
		camTilemap.zoom = 1;

		// creates and setup perspective camera
		// strategically choosen near/far planes, Blender models' 14.2 meters <=> one 256px tile
		// with far plane @48
		camPersp = new PerspectiveCamera(scalingStrategy.verticalFov, ScaleUtils.RefScreenWidth, ScaleUtils.RefScreenHeight);
		camPersp.near = CamPerspPlaneNear;
		camPersp.far = CamPerspPlaneFar;
		camPersp.lookAt(0, 0, -1);
		camPersp.position.set(camTilemap.position.x, camTilemap.position.y, CamPerspElevation);
		camPersp.update();

		camController = new CameraController(Config.Graphics.CameraInterpolationMode, halfViewport, world.worldSizeScaledPx,
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

		// if( Config.isDesktop && (URacer.getFrameCount() & 0x1f) == 0x1f ) {
		// System.out.println( "lights rendered=" + rayHandler.lightRenderedLastFrame );
		// }
	}

	private Color ambientColor = new Color(0.1f, 0.05f, 0.1f, 0.4f);
	private Color treesAmbientColor = new Color(0.1f, 0.05f, 0.1f, 0.4f);

	public Color getAmbientColor () {
		return ambientColor;
	}

	public void setAmbientColor (float r, float g, float b, float a) {
		ambientColor.set(r, g, b, a);
	}

	public void setAmbientColor (Color color) {
		ambientColor.set(color);
	}

	public Color getTreesAmbientColor () {
		return treesAmbientColor;
	}

	public void setTreesAmbientColor (float r, float g, float b, float a) {
		treesAmbientColor.set(r, g, b, a);
	}

	public void setTreesAmbientColor (Color color) {
		treesAmbientColor.set(color);
	}

	private Vector2 cameraPos = new Vector2();
	private float cameraZoom = 1;

	public void setInitialCameraPositionOrient (Car car) {
		cameraPos.set(Convert.mt2px(car.getWorldPosMt()));
		camController.setInitialPositionOrient(cameraPos, car.getWorldOrientRads() * MathUtils.radiansToDegrees, cameraZoom);
	}

	public void setCameraPosition (Vector2 positionPx, float orient, float velocityFactor) {
		cameraPos.set(camController.transform(positionPx, orient, velocityFactor, cameraZoom));
	}

	public void setCameraZoom (float zoom) {
		cameraZoom = MathUtils.clamp(zoom, 0f, MaxCameraZoom);
	}

	// do not use camOrtho.zoom directly since it will be bound later at updateCamera!
	public float getCameraZoom () {
		return cameraZoom;
	}

	public void setGameTrackDebugCar (Car car) {
		gameTrackDbgRenderer.setCar(car);
	}

	public void showDebugGameTrack (boolean show) {
		if (show) {
			gameTrackDbgRenderer.attach();
		} else {
			gameTrackDbgRenderer.detach();
		}
	}

	public void updateCamera () {
		// update orthographic camera

		float zoom = 1f / cameraZoom;

		// remove subpixel accuracy (jagged behavior) by uncommenting the round
		camOrtho.viewportWidth = ScaleUtils.RefScreenWidth;
		camOrtho.viewportHeight = ScaleUtils.RefScreenHeight;

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
		// camOrthoMvpMt.val[Matrix4.M00] *= Config.Physics.PixelsPerMeter;
		// camOrthoMvpMt.val[Matrix4.M01] *= Config.Physics.PixelsPerMeter;
		// camOrthoMvpMt.val[Matrix4.M10] *= Config.Physics.PixelsPerMeter;
		// camOrthoMvpMt.val[Matrix4.M11] *= Config.Physics.PixelsPerMeter;

		// update the tilemap renderer orthographic camera
		// y-down
		camTilemap.up.set(0, -1, 0);
		camTilemap.direction.set(0, 0, 1);
		camTilemap.viewportWidth = ScaleUtils.RefScreenWidth;
		camTilemap.viewportHeight = ScaleUtils.RefScreenHeight;

		camTilemap.position.set(camOrtho.position);
		camTilemap.position.y = world.worldSizeScaledPx.y - camTilemap.position.y;
		// camTilemap.position.scl(ScaleUtils.Scale);
		// camTilemap.zoom = ScaleUtils.Scale * zoom;
		camTilemap.zoom = zoom;
		camTilemap.update();

		// update previous proj view
		camPerspPrevViewProj.set(camPersp.projection).mul(camPersp.view);

		// sync perspective camera to the orthographic camera
		camPersp.viewportWidth = ScaleUtils.RefScreenWidth;
		camPersp.viewportHeight = ScaleUtils.RefScreenHeight;
		camPersp.position.set(cameraPos.x, cameraPos.y, CamPerspElevation);
		camPersp.update(true);

		// update inv proj view
		camPerspInvView.set(camPersp.view);
		Matrix4.inv(camPerspInvView.val);

		camPerspInvProj.set(camPersp.projection);
		Matrix4.inv(camPerspInvProj.val);

		updateRayHandler();
	}

	private void updateRayHandler () {
		if (rayHandler != null) {

			rayHandler.setAmbientLight(ambientColor);

			// @off
			rayHandler.setCombinedMatrix(
				camOrthoMvpMt, 
				Convert.px2mt(camOrtho.position.x), 
				Convert.px2mt(camOrtho.position.y),
				Convert.px2mt(camOrtho.viewportWidth * camOrtho.zoom), 
				Convert.px2mt(camOrtho.viewportHeight* camOrtho.zoom));
			// @on

			rayHandler.update();
			// Gdx.app.log("GameWorldRenderer", "lights rendered=" + rayHandler.lightRenderedLastFrame);

			rayHandler.updateLightMap();
		}
	}

	public void updateNormalDepthMap () {

		Gdx.gl.glEnable(GL20.GL_CULL_FACE);
		Gdx.gl.glCullFace(GL20.GL_BACK);
		Gdx.gl.glDisable(GL20.GL_BLEND);

		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDepthFunc(GL20.GL_LESS);
		Gdx.gl.glDepthMask(true);

		float zscale = 1f / 40f;
		shNormalDepthAlpha.begin();
		shNormalDepthAlpha.setUniformf("inv_depth_scale", zscale);
		shNormalDepthAlpha.end();

		shNormalDepth.begin();
		shNormalDepth.setUniformf("inv_depth_scale", zscale);
		shNormalDepth.end();

		normalDepthMap.begin();
		{
			Gdx.gl.glClearDepthf(1f);
			Gdx.gl.glClearColor(0, 0, 0, 1);
			// Gdx.gl.glClearColor(1, 1, 1, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			// renderAllMeshes(true);
			renderTilemapPlane();
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
		gl.glDisable(GL20.GL_CULL_FACE);

		ShaderProgram shader = shNormalDepth;
		float meshZ = -(camPersp.far - camPersp.position.z) + (camPersp.far * (1 - (camOrtho.zoom)));
		float k = scalingStrategy.meshScaleFactor * OrthographicAlignedStillModel.BlenderToURacer * scalingStrategy.to256;
		float scalex = 6, scalez = 4;

		Matrix4 model = mtx;
		tmpvec.set(camPersp.position.x, camPersp.position.y, meshZ + 0.5f);

		model.idt();
		model.translate(tmpvec);
		model.rotate(1, 0, 0, 90);
		model.scale(scalex * k, 1, scalez * k);
		model.translate(-tmpvec.x, -tmpvec.y, -tmpvec.z);
		model.translate(tmpvec);

		mtx2.set(camPersp.view).mul(model);
		nmat.set(mtx2).inv().transpose();

		shader.begin();
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

			gl.glDisable(GL20.GL_CULL_FACE);
			// gl.glDisable(GL20.GL_DEPTH_TEST);
		}
	}

	public void renderTilemap () {
		gl.glDisable(GL20.GL_DEPTH_TEST);
		gl.glDisable(GL20.GL_CULL_FACE);
		gl.glDisable(GL20.GL_BLEND);
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		tileMapRenderer.setView(camTilemap);
		tileMapRenderer.render();
	}

	public void renderWalls (boolean depthOnly) {
		if (showWalls && trackWalls.count() > 0) {
			renderWalls(trackWalls, depthOnly);
		}
	}

	public void renderTrees (boolean depthOnly) {
		if (showComplexTrees && trackTrees.count() > 0) {
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
		trees.transform(camPersp, camOrtho, halfViewport);

		ShaderProgram shader = null;
		if (depthOnly) {
			shader = shNormalDepthAlpha;
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
				shader.setUniformMatrix("u_projTrans", m.transformed /* combined = cam (proj * view) * model) */);
			} else {
				mtx.set(camPersp.view).mul(m.mtxmodel);
				nmat.set(mtx).inv().transpose();

				shader.setUniformMatrix("nmat", nmat);
				shader.setUniformMatrix("model", m.mtxmodel);
			}

			m.trunk.render(shader, m.smTrunk.primitiveType);
		}

		// all the transparent foliage

		// do NOT cull face so that SSAO appear on back faces as well
		// gl.glDisable(GL20.GL_CULL_FACE);

		if (!depthOnly) {
			gl.glEnable(GL20.GL_BLEND);
			gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		} else {
			// testing!
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

		if (!depthOnly && Config.Debug.Render3DBoundingBoxes) {
			// debug
			for (int i = 0; i < trees.models.size(); i++) {
				TreeStillModel m = trees.models.get(i);
				renderBoundingBox(m.boundingBox);
			}
		}
	}

	private Vector3 tmpvec = new Vector3();
	private Matrix4 mtx = new Matrix4();
	private Matrix3 nmat = new Matrix3();
	private Matrix4 mtx2 = new Matrix4();

	private int renderOrthographicAlignedModels (List<OrthographicAlignedStillModel> models, boolean depthOnly, boolean nightMode) {
		int renderedCount = 0;
		OrthographicAlignedStillModel m;
		StillSubMesh submesh;

		float meshZ = -(camPersp.far - camPersp.position.z) + (camPersp.far * (1 - (camOrtho.zoom)));

		ShaderProgram shader = null;

		if (depthOnly) {
			shader = shNormalDepthAlpha;
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

			// change of basis
			model.idt();
			model.translate(tmpvec);
			model.rotate(m.iRotationAxis, m.iRotationAngle);
			model.scale(m.scaleAxis.x, m.scaleAxis.y, m.scaleAxis.z);
			model.translate(-tmpvec.x, -tmpvec.y, -tmpvec.z);
			model.translate(tmpvec);

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

		if (!depthOnly && Config.Debug.Render3DBoundingBoxes) {
			// debug (tested on a single mesh only!)
			for (int i = 0; i < models.size(); i++) {
				m = models.get(i);
				renderBoundingBox(m.boundingBox);
			}
		}

		return renderedCount;
	}

	/** This is intentionally SLOW. Read it again!
	 * 
	 * @param boundingBox */
	private void renderBoundingBox (BoundingBox boundingBox) {
		float alpha = .15f;
		float r = 0f;
		float g = 0f;
		float b = 1f;
		float offset = 0.5f; // offset for the base, due to pixel-perfect model placement

		Vector3[] corners = boundingBox.getCorners();

		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		dbg.begin(camPersp.combined, GL10.GL_TRIANGLES);
		{
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[0].x, corners[0].y, corners[0].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[4].x, corners[4].y, corners[4].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[4].x, corners[4].y, corners[4].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[2].x, corners[2].y, corners[2].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[2].x, corners[2].y, corners[2].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[6].x, corners[6].y, corners[6].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[2].x, corners[2].y, corners[2].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[6].x, corners[6].y, corners[6].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z + offset);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[6].x, corners[6].y, corners[6].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[0].x, corners[0].y, corners[0].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[0].x, corners[0].y, corners[0].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[4].x, corners[4].y, corners[4].z);

			// top cap
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[4].x, corners[4].y, corners[4].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[6].x, corners[6].y, corners[6].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[0].x, corners[0].y, corners[0].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[2].x, corners[2].y, corners[2].z + offset);
		}
		dbg.end();

		Gdx.gl.glDisable(GL20.GL_BLEND);
	}
}
