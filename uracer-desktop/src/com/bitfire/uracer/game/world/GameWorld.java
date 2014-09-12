
package com.bitfire.uracer.game.world;

import java.util.ArrayList;
import java.util.List;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameLevels;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.collisions.CollisionFilters;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.GameTrack.TrackPosition;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.WorldDefs.Layer;
import com.bitfire.uracer.game.world.WorldDefs.ObjectGroup;
import com.bitfire.uracer.game.world.WorldDefs.ObjectProperties;
import com.bitfire.uracer.game.world.models.MapUtils;
import com.bitfire.uracer.game.world.models.ModelFactory;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.game.world.models.TrackTrees;
import com.bitfire.uracer.game.world.models.TrackWalls;
import com.bitfire.uracer.game.world.models.TreeStillModel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.u3d.materials.Material;
import com.bitfire.uracer.u3d.materials.TextureAttribute;
import com.bitfire.uracer.u3d.still.StillModel;
import com.bitfire.uracer.u3d.still.StillSubMesh;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.ScaleUtils;

/** Encapsulates the game's world. Yay!
 *
 * @author bmanuel */
public final class GameWorld {

	// statistics
	public static int TotalMeshes = 0;

	// public level data
	public final TiledMap map;
	public final int mapWidth, mapHeight, tileWidth, tileHeight;
	public final float invTileWidth;
	public final Vector2 worldSizePx, worldSizeTiles, worldSizeMt;

	// private data
	private World box2dWorld;
	private final MapUtils mapUtils;
	private final String levelId;
	private final float pixelsPerMeterFactor;

	// player data
	public PlayerCar player = null;
	public TrackPosition playerStart = null;

	// ghost data
	public GhostCar[] ghosts = null;

	// light/night system
	private boolean nightMode;
	protected RayHandler rayHandler = null;
	protected ConeLight playerHeadlightsA, playerHeadlightsB = null;
	protected PointLight playerImpulse = null;
	protected PointLight[] lights = null;

	// level meshes, package-level access for GameWorldRenderer (ugly but faster than accessors)
	protected TrackWalls trackWalls = null;
	protected TrackTrees trackTrees = null;
	protected List<OrthographicAlignedStillModel> staticMeshes = new ArrayList<OrthographicAlignedStillModel>();

	// routes
	private GameTrack gameTrack = null;
	private List<Vector2> route = new ArrayList<Vector2>();
	private List<Polygon> polys = new ArrayList<Polygon>();

	public GameWorld (String levelId, boolean nightMode) {
		float widthRatio = (float)Config.Graphics.ReferenceScreenWidth / (float)ScaleUtils.PlayWidth;
		pixelsPerMeterFactor = ((widthRatio * 256f) / 224f) * ScaleUtils.Scale;

		box2dWorld = new World(new Vector2(0, 0), false);
		box2dWorld.setContactListener(new GameWorldContactListener());

		boolean autoClearForces = false;
		boolean continuousPhysics = false;
		box2dWorld.setAutoClearForces(autoClearForces);
		box2dWorld.setContinuousPhysics(continuousPhysics); // power off TOI and make it *really* deterministic

		Gdx.app.log("GameWorld", "Box2D world created (CCD=" + continuousPhysics + ", auto clear forces=" + autoClearForces + ")");

		map = GameLevels.load(levelId);

		this.levelId = levelId;
		this.nightMode = nightMode;

		// get map properties
		mapWidth = map.getProperties().get("width", int.class);
		mapHeight = map.getProperties().get("height", int.class);
		tileWidth = map.getProperties().get("tilewidth", int.class);
		tileHeight = map.getProperties().get("tileheight", int.class);
		invTileWidth = 1f / (float)tileWidth;

		// compute world size
		worldSizeTiles = new Vector2(mapWidth, mapHeight);
		worldSizePx = new Vector2(mapWidth * tileWidth, mapHeight * tileHeight);
		worldSizeMt = new Vector2(Convert.px2mt(mapWidth * tileWidth), Convert.px2mt(mapHeight * tileHeight));

		// initialize tilemap utils
		mapUtils = new MapUtils(map, tileWidth, mapHeight, worldSizePx);

		createMeshes();
		route = createRoute();
		polys = createTrackPolygons();
		gameTrack = new GameTrack(route, polys);

		if (route == null) {
			throw new GdxRuntimeException("No route for this track");
		}

		playerStart = gameTrack.generateTrackPosition(-3);

		if (nightMode) {
			createLights();
		}
	}

	public void dispose () {
		map.dispose();
		polys.clear();
		route.clear();
		mapUtils.dispose();

		if (rayHandler != null) {
			rayHandler.dispose();
			rayHandler = null;
		}

		staticMeshes.clear();
		staticMeshes = null;

		if (trackWalls != null && trackWalls.models != null) {
			for (int i = 0; i < trackWalls.models.size(); i++) {
				trackWalls.models.get(i).model.dispose();
			}
			trackWalls = null;
		}

		box2dWorld.dispose();
		box2dWorld = null;
	}

	private void createMeshes () {
		staticMeshes.clear();
		TotalMeshes = 0;

		// static meshes layer
		if (mapUtils.hasObjectGroup(ObjectGroup.StaticMeshes)) {
			MapLayer group = mapUtils.getObjectGroup(ObjectGroup.StaticMeshes);
			for (int i = 0; i < group.getObjects().getCount(); i++) {
				MapObject o = group.getObjects().get(i);

				float scale = 1f;
				if (o.getProperties().get(ObjectProperties.MeshScale.mnemonic) != null) {
					scale = Float.parseFloat(o.getProperties().get(ObjectProperties.MeshScale.mnemonic, String.class));
				}

				// @off
				OrthographicAlignedStillModel mesh = ModelFactory.create(
					o.getProperties().get("type", String.class),
					o.getProperties().get("x", Integer.class),
					o.getProperties().get("y", Integer.class),
					scale * pixelsPerMeterFactor
				);
				// @on

				if (mesh != null) {
					staticMeshes.add(mesh);
				}
			}
		}

		// walls by polylines
		List<OrthographicAlignedStillModel> walls = createWalls();
		trackWalls = new TrackWalls(walls);

		// trees
		List<TreeStillModel> trees = createTrees();
		trackTrees = new TrackTrees(trees);

		TotalMeshes = staticMeshes.size() + trackWalls.count() + trackTrees.count();
	}

	private void createLights () {
		if (!mapUtils.hasObjectGroup(ObjectGroup.Lights)) {
			this.nightMode = false;
			return;
		}

		float rttScale = 0.5f;
		int maxRays = 360;

		if (!URacer.Game.isDesktop()) {
			rttScale = 0.2f;
			maxRays = 360;
		}

		RayHandler.setColorPrecisionHighp();

		rayHandler = new RayHandler(box2dWorld, maxRays, (int)(ScaleUtils.PlayWidth * rttScale),
			(int)(ScaleUtils.PlayHeight * rttScale), true);
		rayHandler.setShadows(true);
		rayHandler.setCulling(true);
		rayHandler.setBlur(true);
		rayHandler.setBlurNum(2);
		rayHandler.setAmbientLight(0.1f, 0.05f, 0.1f, 0.4f);

		final Color c = new Color();

		// setup player headlights data
		c.set(0.1f, 0.2f, 0.9f, 0.85f);

		int headlightsMask = CollisionFilters.CategoryTrackWalls;
		// int headlightsMask = CollisionFilters.CategoryTrackWalls | CollisionFilters.CategoryReplay;
		// int headlightsMask = CollisionFilters.CategoryReplay;
		// int headlightsMask = 0;

		playerHeadlightsA = new ConeLight(rayHandler, maxRays, c, 25, 0, 0, 0, 9);
		playerHeadlightsA.setSoft(true);
		playerHeadlightsA.setMaskBits(headlightsMask);

		playerHeadlightsB = new ConeLight(rayHandler, maxRays, c, 25, 0, 0, 0, 9);
		playerHeadlightsB.setSoft(true);
		playerHeadlightsB.setMaskBits(headlightsMask);

		// setup level lights data, if any
		Vector2 pos = new Vector2();
		MapLayer group = mapUtils.getObjectGroup(ObjectGroup.Lights);

		int lights_count = group.getObjects().getCount();
		lights = new PointLight[lights_count];

		for (int i = 0; i < lights_count; i++) {
			//@off
			c.set(
//			 MathUtils.random(0,1),
//			 MathUtils.random(0,1),
//			 MathUtils.random(0,1),
							1f, .85f, 0.6f, 0.8f
//				MathUtils.random(0.85f,1),
//				MathUtils.random(0.8f,0.85f),
//				MathUtils.random(0.6f,0.8f),
//				0.55f
			);
			//@on

			RectangleMapObject o = (RectangleMapObject)group.getObjects().get(i);
			pos.set(o.getRectangle().x, o.getRectangle().y);// .scl(scalingStrategy.invTileMapZoomFactor);
			pos.y = worldSizePx.y - pos.y;
			pos.set(Convert.px2mt(pos));// .scl(scalingStrategy.tileMapZoomFactor);

			PointLight l = new PointLight(rayHandler, maxRays, c, MathUtils.random(15, 20), pos.x, pos.y);
			l.setSoft(true);
			l.setStaticLight(false);
			l.setMaskBits(CollisionFilters.CategoryPlayer | CollisionFilters.CategoryTrackWalls);

			lights[i] = l;
		}

		// playerImpulse = new PointLight(rayHandler, maxRays);
		// playerImpulse.setMaskBits(CollisionFilters.CategoryPlayer | CollisionFilters.CategoryReplay);
		// playerImpulse.setSoft(true);
		// playerImpulse.setStaticLight(false);
		// playerImpulse.setActive(true);
		// playerImpulse.setColor(1, 1, 1, 1f);
		// playerImpulse.setDistance(5);
	}

	//
	// construct route/sectors
	//

	private List<Vector2> createRoute () {
		List<Vector2> r = null;

		if (mapUtils.hasObjectGroup(ObjectGroup.Route)) {
			Vector2 fromMt = new Vector2();
			Vector2 toMt = new Vector2();
			Vector2 offsetMt = new Vector2();

			MapLayer group = mapUtils.getObjectGroup(ObjectGroup.Route);
			if (group.getObjects().getCount() == 1) {
				PolylineMapObject o = (PolylineMapObject)group.getObjects().get(0);

				//@off
				List<Vector2> points = MapUtils.extractPolyData(
					o.getPolyline().getVertices());
				//@on

				r = new ArrayList<Vector2>(points.size());

				offsetMt.set(o.getPolyline().getX(), o.getPolyline().getY());
				offsetMt.set(Convert.px2mt(offsetMt));

				fromMt.set(Convert.px2mt(points.get(0))).add(offsetMt);
				fromMt.y = worldSizeMt.y - fromMt.y;

				r.add(new Vector2(fromMt));

				for (int j = 1; j <= points.size() - 1; j++) {
					toMt.set(Convert.px2mt(points.get(j))).add(offsetMt);
					toMt.y = worldSizeMt.y - toMt.y;
					r.add(new Vector2(toMt));
				}
			} else {
				if (group.getObjects().getCount() > 1) {
					throw new GdxRuntimeException("Too many routes");
				} else if (group.getObjects().getCount() == 0) {
					throw new GdxRuntimeException("No route defined for this track");
				}
			}

		}

		return r;
	}

	private List<Polygon> createTrackPolygons () {
		List<Polygon> s = null;

		if (mapUtils.hasObjectGroup(ObjectGroup.Sectors)) {
			Vector2 pt = new Vector2();
			Vector2 offsetMt = new Vector2();

			MapLayer group = mapUtils.getObjectGroup(ObjectGroup.Sectors);
			if (group.getObjects().getCount() > 0) {
				s = new ArrayList<Polygon>(group.getObjects().getCount());

				for (int i = 0; i < group.getObjects().getCount(); i++) {
					PolygonMapObject o = (PolygonMapObject)group.getObjects().get(i);

					//@off
					List<Vector2> points = MapUtils.extractPolyData(
						o.getPolygon().getVertices()
					);
					//@on

					if (points.size() != 4) {
						throw new GdxRuntimeException("A quadrilateral is required!");
					}

					offsetMt.set(o.getPolygon().getX(), o.getPolygon().getY());
					offsetMt.set(Convert.px2mt(offsetMt));

					float[] vertices = new float[8];
					for (int j = 0; j < points.size(); j++) {
						// convert to uracer convention
						pt.set(Convert.px2mt(points.get(j))).add(offsetMt);
						pt.y = worldSizeMt.y - pt.y;

						vertices[j * 2] = pt.x;
						vertices[j * 2 + 1] = pt.y;
					}

					Polygon p = new Polygon(vertices);
					Rectangle r = p.getBoundingRectangle();
					float oX = r.x + r.width / 2;
					float oY = r.y + r.height / 2;
					p.setOrigin(oX, oY);

					s.add(p);
				}

			} else {
				throw new GdxRuntimeException("There are no defined sectors for this track");
			}

		}

		return s;

	}

	//
	// construct walls
	//

	private List<OrthographicAlignedStillModel> createWalls () {
		List<OrthographicAlignedStillModel> models = null;

		if (mapUtils.hasObjectGroup(ObjectGroup.Walls)) {
			Vector2 fromMt = new Vector2();
			Vector2 toMt = new Vector2();
			Vector2 offsetMt = new Vector2();

			// create material
			TextureAttribute ta = new TextureAttribute(Art.meshTrackWall, 0, "u_texture");
			ta.uWrap = TextureWrap.Repeat.getGLEnum();
			ta.vWrap = TextureWrap.Repeat.getGLEnum();
			Material mat = new Material("trackWall", ta);

			MapLayer group = mapUtils.getObjectGroup(ObjectGroup.Walls);
			if (group.getObjects().getCount() > 0) {
				models = new ArrayList<OrthographicAlignedStillModel>(group.getObjects().getCount());

				for (int i = 0; i < group.getObjects().getCount(); i++) {
					PolylineMapObject o = (PolylineMapObject)group.getObjects().get(i);

					//@off
					List<Vector2> points = MapUtils.extractPolyData(
						o.getPolyline().getVertices());
					//@on
					if (points.size() >= 2) {
						float wallTicknessMt = 0.75f;
						float[] mags = new float[points.size() - 1];

						offsetMt.set(o.getPolyline().getX(), o.getPolyline().getY());
						offsetMt.set(Convert.px2mt(offsetMt));

						fromMt.set(Convert.px2mt(points.get(0))).add(offsetMt);
						fromMt.y = worldSizeMt.y - fromMt.y;

						for (int j = 1; j <= points.size() - 1; j++) {
							toMt.set(Convert.px2mt(points.get(j))).add(offsetMt);
							toMt.y = worldSizeMt.y - toMt.y;

							// create box2d wall
							Box2DFactory.createWall(box2dWorld, fromMt, toMt, wallTicknessMt, 0f);

							// compute magnitude
							mags[j - 1] = (float)Math.sqrt((toMt.x - fromMt.x) * (toMt.x - fromMt.x) + (toMt.y - fromMt.y)
								* (toMt.y - fromMt.y));

							fromMt.set(toMt);
						}

						Mesh mesh = buildWallMesh(points, mags);

						StillSubMesh[] subMeshes = new StillSubMesh[1];
						subMeshes[0] = new StillSubMesh("wall", mesh, GL20.GL_TRIANGLES);

						OrthographicAlignedStillModel model = new OrthographicAlignedStillModel(new StillModel(subMeshes), mat);

						model.setPosition(o.getPolyline().getX(), worldSizePx.y - o.getPolyline().getY());
						model.setScale(1);

						models.add(model);
					}
				}
			}
		}

		return models;
	}

	private Vector3 vec1 = new Vector3(), vec2 = new Vector3(), vec3 = new Vector3(), vecFirst = new Vector3(),
		vecSecond = new Vector3(), vecNormal = new Vector3();

	private Mesh buildWallMesh (List<Vector2> points, float[] magnitudes) {
		final int X1 = 0;
		final int Y1 = 1;
		final int Z1 = 2;
		final int NX1 = 3;
		final int NY1 = 4;
		final int NZ1 = 5;
		final int U1 = 6;
		final int V1 = 7;

		final int X2 = 8;
		final int Y2 = 9;
		final int Z2 = 10;
		final int NX2 = 11;
		final int NY2 = 12;
		final int NZ2 = 13;
		final int U2 = 14;
		final int V2 = 15;

		Vector2 in = new Vector2();
		MathUtils.random.setSeed(Long.MIN_VALUE);

		// scaling factors
		float factor = pixelsPerMeterFactor;
		float oneOnWorld3DFactor = 1f / OrthographicAlignedStillModel.World3DScalingFactor;
		float wallHeightMt = 5f * factor * oneOnWorld3DFactor;
		float textureScalingU = 0.5f;
		float coordU = 1f;
		float coordV = 1f;

		// jitter
		float jitterPositional = 0.75f * factor * oneOnWorld3DFactor;
		// float jitterAltitudinal = 3f * factor * oneOnWorld3DFactor;
		boolean addJitter = true;

		int vertexCount = points.size() * 2;
		int indexCount = (points.size() - 1) * 6;

		int vertSize = 8; // x, y, z, u, v, nx, ny, nz
		float[] verts = new float[vertSize * vertexCount];
		short[] indices = new short[indexCount];
		float mag, prevmag;
		mag = magnitudes[0];
		prevmag = magnitudes[0];

		// add input (interleaved w/ later filled dupes w/ just a meaningful
		// z-coordinate)
		for (int i = 0, j = 0, vc = 0, vci = 0; i < points.size(); i++, j += 2 * vertSize) {
			int magidx = i - 1;
			if (magidx < 0) {
				magidx = 0;
			}

			mag = AMath.lerp(prevmag, magnitudes[magidx], .5f);
			prevmag = mag;

			coordU = mag * textureScalingU;

			in.set(Convert.px2mt(points.get(i)));
			in.y = -in.y;
			in.scl(factor * oneOnWorld3DFactor);

			// base
			verts[j + X1] = in.x;
			verts[j + Y1] = in.y;
			verts[j + Z1] = 0;// -0.025f; // should be 0, but fixes some nasty flickering border issue

			// elevation
			verts[j + X2] = in.x + (addJitter ? MathUtils.random(-jitterPositional, jitterPositional) : 0);
			verts[j + Y2] = in.y + (addJitter ? MathUtils.random(-jitterPositional, jitterPositional) : 0);
			verts[j + Z2] = wallHeightMt;// + (addJitter? MathUtils.random(
			// -jitterAltitudinal,
			// jitterAltitudinal ) :
			// 0);

			// tex coords
			verts[j + U1] = ((i & 1) == 0 ? coordU : 0f);
			verts[j + V1] = coordV;

			verts[j + U2] = ((i & 1) == 0 ? coordU : 0f);
			verts[j + V2] = 0f;

			// normal
			verts[j + NX1] = 0;
			verts[j + NY1] = 0;
			verts[j + NZ1] = 0;

			verts[j + NX2] = 0;
			verts[j + NY2] = 0;
			verts[j + NZ2] = 0;

			//
			vc += 2;

			if (vc > 2) {
				indices[vci++] = (short)(vc - 3);
				indices[vci++] = (short)(vc - 4);
				indices[vci++] = (short)(vc - 2);
				indices[vci++] = (short)(vc - 3);
				indices[vci++] = (short)(vc - 2);
				indices[vci++] = (short)(vc - 1);
			}
		}

		// alias it
		float[] v = verts;

		// compute normals
		int count = indices.length / 3;
		for (int i = 0; i < count; i++) {
			int first = indices[i * 3 + 1] * vertSize;
			int second = indices[i * 3 + 0] * vertSize;
			int third = indices[i * 3 + 2] * vertSize;

			vec1.set(v[first + X1], v[first + Y1], v[first + Z1]);
			vec2.set(v[second + X1], v[second + Y1], v[second + Z1]);
			vec3.set(v[third + X1], v[third + Y1], v[third + Z1]);

			vecFirst.set(vec1).sub(vec2);
			vecSecond.set(vec2).sub(vec3);

			vecNormal.set(vecSecond).crs(vecFirst);
			vecNormal.nor();

			//@off
			v[first + NX1] += vecNormal.x;	v[first + NY1] += vecNormal.y;	v[first + NZ1] += vecNormal.z;
			v[second + NX1] += vecNormal.x;	v[second + NY1] += vecNormal.y;	v[second + NZ1] += vecNormal.z;
			v[third + NX1] += vecNormal.x;	v[third + NY1] += vecNormal.y;	v[third + NZ1] += vecNormal.z;
			//@on
		}

		// normalize everything
		count = verts.length / vertSize;
		for (int i = 0; i < count; i++) {
			int k = vertSize * i;

			vecNormal.x = v[k + NX1];
			vecNormal.y = v[k + NY1];
			vecNormal.z = v[k + NZ1];

			vecNormal.nor();

			v[k + NX1] = vecNormal.x;
			v[k + NY1] = vecNormal.y;
			v[k + NZ1] = vecNormal.z;
		}

		//@off
		Mesh mesh = new Mesh(VertexDataType.VertexArray, true, vertexCount, indexCount,
			new VertexAttribute(Usage.Position, 3,ShaderProgram.POSITION_ATTRIBUTE),
			new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE),
			new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")
		);
		//@on

		mesh.setVertices(verts);
		mesh.setIndices(indices);

		return mesh;
	}

	//
	// construct trees
	//

	private float[] treeRotations = new float[4];

	private List<TreeStillModel> createTrees () {
		List<TreeStillModel> models = null;

		if (mapUtils.hasObjectGroup(ObjectGroup.Trees)) {

			// We want to differentiate tree meshes as much as we can
			// rotation will helps immensely, but non-orthogonal rotations
			// will cause the bounding box to get recomputed only approximately
			// thus loosing precision: orthogonal rotations instead provides
			// high
			// quality AABB recomputation.
			//
			// We still have 4 variations for any given tree!
			treeRotations[0] = 0;
			treeRotations[1] = 90;
			treeRotations[2] = 180;
			treeRotations[3] = 270;

			MathUtils.random.setSeed(Long.MAX_VALUE);
			MapLayer group = mapUtils.getObjectGroup(ObjectGroup.Trees);

			if (group.getObjects().getCount() > 0) {

				models = new ArrayList<TreeStillModel>(group.getObjects().getCount());

				for (int i = 0; i < group.getObjects().getCount(); i++) {
					RectangleMapObject o = (RectangleMapObject)group.getObjects().get(i);

					float scale = 1f;
					if (o.getProperties().get(ObjectProperties.MeshScale.mnemonic) != null) {
						scale = Float.parseFloat(o.getProperties().get(ObjectProperties.MeshScale.mnemonic, String.class));
					}

					TreeStillModel model = null;
					if (o.getProperties().get("type") != null) {
						//@off
						model = ModelFactory.createTree(
							o.getProperties().get("type", String.class),
							o.getRectangle().x,
							worldSizePx.y - o.getRectangle().y,
							scale);
						//@on
					} else {
						Gdx.app.log("TrackTrees", "Load error, no type was given for the tree #" + (i + 1));
					}

					if (model != null) {
						// model.setRotation( MathUtils.random( -180f, 180f ),
						// 0, 0, 1f );
						model.setRotation(treeRotations[MathUtils.random(0, 3)], 0, 0, 1f);
						models.add(nextIndexForTrees(models, model), model);
					}
				}
			}
		}

		return models;
	}

	private int nextIndexForTrees (List<TreeStillModel> models, TreeStillModel model) {
		for (int i = 0; i < models.size(); i++) {
			if (model.material.equals(models.get(i).material)) {
				return i;
			}
		}

		return 0;
	}

	public boolean isNightMode () {
		return nightMode;
	}

	public PointLight[] getLights () {
		return lights;
	}

	public TrackWalls getTrackWalls () {
		return trackWalls;
	}

	public TrackTrees getTrackTrees () {
		return trackTrees;
	}

	public List<Vector2> getTrackRoute () {
		return route;
	}

	public List<Polygon> getTrackPolygons () {
		return polys;
	}

	public GameTrack getGameTrack () {
		return gameTrack;
	}

	public String getLevelId () {
		return levelId;
	}

	public List<OrthographicAlignedStillModel> getStaticMeshes () {
		return staticMeshes;
	}

	public RayHandler getRayHandler () {
		return rayHandler;
	}

	public ConeLight getPlayerHeadLights (boolean aOrB) {
		return aOrB ? playerHeadlightsA : playerHeadlightsB;
	}

	public PointLight getPlayerImpulseLight () {
		return playerImpulse;
	}

	public World getBox2DWorld () {
		return box2dWorld;
	}

	public PlayerCar getPlayer () {
		return player;
	}

	public void setPlayer (PlayerCar player) {
		this.player = player;
	}

	public void setGhostCars (GhostCar[] ghosts) {
		this.ghosts = ghosts;
	}

	public GhostCar[] getGhostCars () {
		return ghosts;
	}

	// helpers from maputils

	public Vector2 pxToTile (float x, float y) {
		return mapUtils.pxToTile(x, y);
	}

	public float getTileSizePx () {
		return tileWidth;
	}

	public float getTileSizePxInv () {
		return invTileWidth;
	}

	public TiledMapTileLayer getLayer (Layer layer) {
		return mapUtils.getLayer(layer);
	}

	public boolean isValidTilePosition (Vector2 tilePosition) {
		return tilePosition.x >= 0 && tilePosition.x < mapWidth && tilePosition.y >= 0 && tilePosition.y < mapHeight;
	}
}
