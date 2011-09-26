package com.bitfire.uracer.effects;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public class TrackEffects
{
	private static GameLogic logic;
	private static SpriteBatch batch;
	private static ArrayList<Sprite> driftMarksFront;
	private static ArrayList<Sprite> driftMarksRear;

	public static void init( GameLogic logic )
	{
		TrackEffects.logic = logic;
		driftMarksFront = new ArrayList<Sprite>();
		driftMarksRear = new ArrayList<Sprite>();
		batch = new SpriteBatch( 1000, 5 );
	}

	public static void tick()
	{
		updateDriftMarks();
//		System.out.println( driftMarksFront.size() );
	}

	public static void render()
	{
		OrthographicCamera cam = Director.getCamera();

		batch.setProjectionMatrix( cam.projection );
		batch.setTransformMatrix( cam.view );
		batch.begin();

		// drift marks
		for( int i = 0; i < driftMarksFront.size(); i++ )
		{
			driftMarksFront.get( i ).draw( batch );
		}

		for( int i = 0; i < driftMarksRear.size(); i++ )
		{
			driftMarksRear.get( i ).draw( batch );
		}

		batch.end();
	}

	public static void reset()
	{
		driftMarksFront.clear();
		driftMarksRear.clear();
	}

	/**
	 * Skid marks
	 *
	 */

	private enum SkidMarkType
	{
		Front, Rear
	}

	private static void updateDriftMarks()
	{
		Car player = logic.getGame().getLevel().getPlayer();
		CarModel model = player.getCarModel();

		Vector2 pos = player.state().position;
		float angle = player.state().orientation;

		// lateral forces are in the range [-max_grip, max_grip]
		float flatf = Math.abs( player.getSimulator().lateralForceFront.y );
		flatf = AMath.clamp( flatf / model.max_grip, 0, 1f );

		float flatr = Math.abs( player.getSimulator().lateralForceRear.y );
		flatr = AMath.clamp( flatr / model.max_grip, 0, 1f );

		// add front drift marks?
		if( flatf > 0.5f ) addSkidMark( SkidMarkType.Front, model, (flatf-0.5f)/0.5f, pos, angle );
		if( flatr > 0.5f ) addSkidMark( SkidMarkType.Rear, model, (flatr-0.5f)/0.5f, pos, angle );
	}

	private static void addSkidMark( SkidMarkType type, CarModel model, float amount, Vector2 pos, float angle )
	{
		ArrayList<Sprite> target = null;
		TextureRegion region = null;

		switch( type )
		{
		default:
		case Front:
			target = driftMarksFront;
			region = Art.skidMarksFront;
			break;
		case Rear:
			target = driftMarksRear;
			region = Art.skidMarksRear;
			break;
		}

		Sprite s = new Sprite();
		s.setRegion( region );
		s.setSize( Convert.mt2px( model.width ), Convert.mt2px( model.length ) );
		s.setOrigin( s.getWidth() / 2, s.getHeight() / 2 );
		s.setColor( 1, 1, 1, amount );
		s.setPosition( pos.x - s.getOriginX(), pos.y - s.getOriginY() );
		s.setRotation( angle );

		target.add( s );
	}
}
