package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Config.Physics;
import com.bitfire.uracer.utils.Convert;

public final class Director {
	public static Vector2 halfViewport;

	private static OrthographicCamera camera;
	private static Vector2 screenPosFor;
	private static Matrix4 mvpMt, mvpPx;

	private static Vector2 tmp;

	private Director() {
	}

	public static void init() {
		screenPosFor = new Vector2();
		mvpMt = new Matrix4();
		mvpPx = new Matrix4();
		halfViewport = new Vector2();
		tmp = new Vector2();
		cameraRect = new Rectangle();
		camera = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		halfViewport.set( camera.viewportWidth / 2f, camera.viewportHeight / 2f );
	}

	public static void dispose() {
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

	public static void setPositionPx( Vector2 pos, boolean round ) {
		tmp.set( pos );

		// remove subpixel accuracy (jagged behavior)
		if( round ) {
			camera.position.x = MathUtils.round( tmp.x );
			camera.position.y = MathUtils.round( tmp.y );
		} else {
			camera.position.x = tmp.x;
			camera.position.y = tmp.y;
		}

		camera.position.z = 0;

		update();
	}

	public static void setPositionMt( Vector2 pos, boolean round ) {
		setPositionPx( Convert.mt2px( pos ), round );
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

	private static Rectangle cameraRect;

	public static boolean isVisible( Rectangle rect ) {
		cameraRect.set( camera.position.x - halfViewport.x, camera.position.y - halfViewport.y, camera.viewportWidth, camera.viewportHeight );
		return cameraRect.overlaps( rect );
	}
}
