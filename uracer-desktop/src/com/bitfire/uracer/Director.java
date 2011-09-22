package com.bitfire.uracer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.factories.Box2DFactory;
import com.bitfire.uracer.hud.Messager;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.tiled.ScalingStrategy;
import com.bitfire.uracer.utils.Convert;

public class Director
{
	public static Vector2 worldSizeScaledPx, worldSizeScaledMt;
	public static ScalingStrategy scalingStrategy;
	public static Level currentLevel;
	public static GameplaySettings gameplaySettings;

	private static OrthographicCamera camera;
	private static Vector2 screenPosFor;
	private static Matrix4 mvpMt, mvpPx;
	private static Vector2 halfViewport;
	private static Rectangle boundsPx;

	private static Vector2 tmp;

	public static void init()
	{
		ShaderProgram.pedantic = false;

		worldSizeScaledPx = new Vector2();
		worldSizeScaledMt = new Vector2();
		screenPosFor = new Vector2();
		mvpMt = new Matrix4();
		mvpPx = new Matrix4();
		halfViewport = new Vector2();
		boundsPx = new Rectangle();
		tmp = new Vector2();
		currentLevel = null;
		gameplaySettings = null;

		// computed for a 256px tile size target (need conversion)
		scalingStrategy = new ScalingStrategy( new Vector2( 1280, 800 ), 70f, 224, 1f);

		// setup configuration
//		Config.asDefault();

		// everything has been setup on a 256px tile, scale back if that's the case
		Config.PixelsPerMeter /= scalingStrategy.targetScreenRatio / scalingStrategy.to256;
//		System.out.println("ppm=" + Config.PixelsPerMeter);

		Physics.create( new Vector2( 0, 0 ), false );

		Box2DFactory.init();
		Messager.init();
	}

	public static void create( Screen parent, int widthPx, int heightPx )
	{
		init();

		camera = new OrthographicCamera( widthPx, heightPx );
		halfViewport.set( camera.viewportWidth / 2f, camera.viewportHeight / 2f );
	}

	public static void dispose()
	{
		if( currentLevel != null )
		{
			currentLevel.dispose();
		}

		Physics.dispose();
		Messager.dispose();
	}

	public static Level loadLevel(String levelName, GameplaySettings playSettings)
	{
		// construct tilemap and cameras
		Level level = new Level( "level1", scalingStrategy );

		// setup converter
		Convert.init( scalingStrategy, level.map );

		// compute world size
		Director.worldSizeScaledPx.set( level.map.width * level.map.tileWidth, level.map.height * level.map.tileHeight );
		Director.worldSizeScaledPx.mul( scalingStrategy.invTileMapZoomFactor );
		Director.worldSizeScaledMt = Convert.px2mt( worldSizeScaledPx );

		// compute camera bounds
		boundsPx.x = halfViewport.x;
		boundsPx.width = Director.worldSizeScaledPx.x - halfViewport.x;
		boundsPx.height = halfViewport.y;
		boundsPx.y = Director.worldSizeScaledPx.y - halfViewport.y;

		// construct level objects from tmx definitions
		level.init();

		currentLevel = level;
		gameplaySettings = playSettings;

		return level;
	}

	public static OrthographicCamera getCamera()
	{
		return camera;
	}

	private static void update()
	{
		camera.update();

		mvpPx.set( camera.combined );
		mvpMt.set( mvpPx );

		// rescale
		mvpMt.val[Matrix4.M00] *= Config.PixelsPerMeter;
		mvpMt.val[Matrix4.M01] *= Config.PixelsPerMeter;
		mvpMt.val[Matrix4.M10] *= Config.PixelsPerMeter;
		mvpMt.val[Matrix4.M11] *= Config.PixelsPerMeter;
	}

	public static void setPositionPx( Vector2 pos, boolean flipY )
	{
		tmp.set( pos );

		if( flipY ) tmp.y = worldSizeScaledPx.y - tmp.y;

		// ensure in bounds
		if( tmp.x < boundsPx.x ) tmp.x = boundsPx.x;
		if( tmp.x > boundsPx.width ) tmp.x = boundsPx.width;
		if( tmp.y > boundsPx.y ) tmp.y = boundsPx.y;
		if( tmp.y < boundsPx.height ) tmp.y = boundsPx.height;

		// remove subpixel accuracy (jagged behavior)
		camera.position.x = MathUtils.round( tmp.x );
		camera.position.y = MathUtils.round( tmp.y );
		camera.position.z = 0;

		update();
	}

	public static void setPositionMt( Vector2 pos, boolean flipY )
	{
		setPositionPx( Convert.mt2px( pos ), flipY );
	}

	public static Vector3 pos()
	{
		return camera.position;
	}

	public static Matrix4 getMatViewProjPx()
	{
		return mvpPx;
	}

	public static Matrix4 getMatViewProjMt()
	{
		return mvpMt;
	}

	/**
	 * PUBLIC HELPERS
	 *
	 */
	public static Vector2 screenPosFor( Body body )
	{
		screenPosFor.x = Convert.mt2px( body.getPosition().x ) - camera.position.x + halfViewport.x;
		screenPosFor.y = camera.position.y - Convert.mt2px( body.getPosition().y ) + halfViewport.y;
		return screenPosFor;
	}

	public static Vector2 positionFor( Vector2 position )
	{
		return positionFor( position.x, position.y );
	}

	public static Vector2 positionFor( float x, float y )
	{
		tmp = Convert.scaledPixels( tmp.set( x, y ) );
		tmp.y = Director.worldSizeScaledPx.y - tmp.y;
		return tmp;
	}
}
