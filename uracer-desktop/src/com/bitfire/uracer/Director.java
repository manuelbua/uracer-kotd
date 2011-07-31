package com.bitfire.uracer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.utils.Convert;

public class Director
{
	private static OrthographicCamera camera;
	public static Vector2 worldSizePx, worldSizeMt;
	private static Vector2 screenPosFor;
	private static Matrix4 mvpMt, mvpPx;
	private static Vector2 halfViewport;
	private static Rectangle boundsPx;
	private static Screen parent;

	private static Vector2 tmp;

	public static void init()
	{
		worldSizePx = new Vector2();
		worldSizeMt = new Vector2();
		screenPosFor = new Vector2();
		mvpMt = new Matrix4();
		mvpPx = new Matrix4();
		halfViewport = new Vector2();
		boundsPx = new Rectangle();
		tmp = new Vector2();
	}

	public static void createFromPixels( Screen parent, int widthPx, int heightPx, Vector2 positionPx, Vector2 worldSizePx )
	{
		init();

		camera = new OrthographicCamera( widthPx, heightPx );
		halfViewport.set( camera.viewportWidth/2f, camera.viewportHeight/2f );
		Director.parent = parent;

		// compute world size
		Director.worldSizePx = worldSizePx;
		Director.worldSizeMt = Convert.px2mt( worldSizePx );

		// compute camera bounds
		boundsPx.x = halfViewport.x;
		boundsPx.width = Director.worldSizePx.x - halfViewport.x;
		boundsPx.height = halfViewport.y;
		boundsPx.y = Director.worldSizePx.y - halfViewport.y;

		setPositionPx( positionPx, false );
		update();
	}

	public static OrthographicCamera getCamera()
	{
		return camera;
	}

	public static void update()
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
		tmp.set(pos);

		if(flipY)
			tmp.y = worldSizePx.y - tmp.y;

		// ensure in bounds
		if( tmp.x < boundsPx.x ) tmp.x = boundsPx.x;
		if( tmp.x > boundsPx.width ) tmp.x = boundsPx.width;
		if( tmp.y > boundsPx.y ) tmp.y = boundsPx.y;
		if( tmp.y < boundsPx.height ) tmp.y = boundsPx.height;

		camera.position.set( tmp.x, tmp.y, 0 );
	}

	public static void setPositionMt( Vector2 pos, boolean flipY )
	{
		setPositionPx( Convert.mt2px(pos), flipY );
	}

	public static Vector3 pos()
	{
		return camera.position;
	}

	public static Vector2 screenPosFor( Body body )
	{
		screenPosFor.x = Convert.mt2px(body.getPosition().x) - Director.getCamera().position.x + halfViewport.x;
		screenPosFor.y = Director.getCamera().position.y - Convert.mt2px(body.getPosition().y) + halfViewport.y;
		return screenPosFor;
	}

	public static Matrix4 getMatViewProjPx()
	{
		return mvpPx;
	}

	public static Matrix4 getMatViewProjMt()
	{
		return mvpMt;
	}

	public static Screen screen()
	{
		return parent;
	}
}
