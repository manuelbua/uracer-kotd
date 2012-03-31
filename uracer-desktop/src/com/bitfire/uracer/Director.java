package com.bitfire.uracer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.logic.Level;
import com.bitfire.uracer.utils.Convert;

public class Director {
	public static Vector2 worldSizeScaledPx, worldSizeScaledMt, worldSizeTiles;
	public static Level currentLevel;
	public static GameplaySettings gameplaySettings;
	public static Rectangle boundsPx;
	public static Vector2 halfViewport;

	private static OrthographicCamera camera;
	private static Vector2 screenPosFor;
	private static Matrix4 mvpMt, mvpPx;

	private static Vector2 tmp;

	public static void init() {
		ShaderProgram.pedantic = false;

		worldSizeScaledPx = new Vector2();
		worldSizeScaledMt = new Vector2();
		worldSizeTiles = new Vector2();
		screenPosFor = new Vector2();
		mvpMt = new Matrix4();
		mvpPx = new Matrix4();
		halfViewport = new Vector2();
		boundsPx = new Rectangle();
		tmp = new Vector2();
		currentLevel = null;
		gameplaySettings = null;
		cameraRect = new Rectangle();

		// everything has been setup on a 256px tile, scale back if that's the
		// case
		Config.Physics.PixelsPerMeter /= GameData.scalingStrategy.targetScreenRatio / GameData.scalingStrategy.to256;
		// System.out.println("ppm=" + Config.PixelsPerMeter);

	}

	public static void create( int widthPx, int heightPx ) {
		init();
		camera = new OrthographicCamera( widthPx, heightPx );
		halfViewport.set( camera.viewportWidth / 2f, camera.viewportHeight / 2f );
	}

	public static void dispose() {
	}

	public static Level loadLevel( World world, String levelName, GameplaySettings playSettings, boolean nightMode ) {
		// construct tilemap, cameras and renderer
		Level level = new Level( world, levelName, GameData.scalingStrategy, nightMode );

		// setup converter
		Convert.init( GameData.scalingStrategy, level.map );

		// compute world size
		Director.worldSizeTiles.set( level.map.width, level.map.height );
		Director.worldSizeScaledPx.set( level.map.width * level.map.tileWidth, level.map.height * level.map.tileHeight );
		Director.worldSizeScaledPx.mul( GameData.scalingStrategy.invTileMapZoomFactor );
		Director.worldSizeScaledMt.set( Convert.px2mt( worldSizeScaledPx ) );

		// compute camera bounds
		boundsPx.x = halfViewport.x;
		boundsPx.width = Director.worldSizeScaledPx.x - halfViewport.x;
		boundsPx.height = halfViewport.y;
		boundsPx.y = Director.worldSizeScaledPx.y - halfViewport.y;

		// construct level objects from tmx definitions
		level.construct();

		currentLevel = level;
		gameplaySettings = playSettings;

		return level;
	}

	public static OrthographicCamera getCamera() {
		return camera;
	}

	private static void update() {
		camera.update();

		mvpPx.set( camera.combined );
		mvpMt.set( mvpPx );

		// rescale
		mvpMt.val[Matrix4.M00] *= Config.Physics.PixelsPerMeter;
		mvpMt.val[Matrix4.M01] *= Config.Physics.PixelsPerMeter;
		mvpMt.val[Matrix4.M10] *= Config.Physics.PixelsPerMeter;
		mvpMt.val[Matrix4.M11] *= Config.Physics.PixelsPerMeter;
	}

	public static void setPositionPx( Vector2 pos, boolean flipY, boolean round ) {
		tmp.set( pos );

		if( flipY ) tmp.y = worldSizeScaledPx.y - tmp.y;

		// ensure in bounds
		if( Config.Debug.DirectorHasBounds ) {
			if( tmp.x < boundsPx.x ) tmp.x = boundsPx.x;
			if( tmp.x > boundsPx.width ) tmp.x = boundsPx.width;
			if( tmp.y > boundsPx.y ) tmp.y = boundsPx.y;
			if( tmp.y < boundsPx.height ) tmp.y = boundsPx.height;
		}

		// remove subpixel accuracy (jagged behavior)
		if( round ) {
			camera.position.x = MathUtils.round( tmp.x );
			camera.position.y = MathUtils.round( tmp.y );
		}
		else {
			camera.position.x = tmp.x;
			camera.position.y = tmp.y;
		}

		camera.position.z = 0;

		update();
	}

	public static void setPositionMt( Vector2 pos, boolean flipY, boolean round ) {
		setPositionPx( Convert.mt2px( pos ), flipY, round );
	}

	public static Vector3 pos() {
		return camera.position;
	}

	public static Matrix4 getMatViewProjPx() {
		return mvpPx;
	}

	public static Matrix4 getMatViewProjMt() {
		return mvpMt;
	}

	/** PUBLIC HELPERS */
	public static Vector2 screenPosFor( Body body ) {
		return Director.screenPosForMt( body.getPosition() );
	}

	public static Vector2 screenPosForMt( Vector2 worldPosition ) {
		screenPosFor.x = Convert.mt2px( worldPosition.x ) - camera.position.x + halfViewport.x;
		screenPosFor.y = camera.position.y - Convert.mt2px( worldPosition.y ) + halfViewport.y;
		return screenPosFor;
	}

	public static Vector2 screenPosForPx( Vector2 worldPosition ) {
		screenPosFor.x = worldPosition.x - camera.position.x + halfViewport.x;
		screenPosFor.y = camera.position.y - worldPosition.y + halfViewport.y;
		return screenPosFor;
	}

	public static Vector2 positionFor( Vector2 position ) {
		return positionFor( position.x, position.y );
	}

	public static Vector2 positionFor( float x, float y ) {
		tmp = Convert.scaledPixels( tmp.set( x, y ) );
		tmp.y = Director.worldSizeScaledPx.y - tmp.y;
		return tmp;
	}

	/** orthographic camera visibility queries */

	private static Rectangle cameraRect;

	public static boolean isVisible( Rectangle rect ) {
		cameraRect.set( camera.position.x - halfViewport.x, camera.position.y - halfViewport.y, camera.viewportWidth, camera.viewportHeight );
		return cameraRect.overlaps( rect );
	}
}
