package com.bitfire.uracer.effects;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.utils.AMath;

public class TrackEffects
{
	public static final int MaxSkidMarks = 500;
	private static GameLogic logic;

	private static SpriteBatch driftsBatch;
	private static ArrayList<CarDrift> driftMarks;
	private static int driftIndex;
	public static int visibleDriftsCount;

	private static Car player;
	private static CarModel model;

	public static void init( GameLogic logic )
	{
		TrackEffects.logic = logic;

		driftIndex = 0;
		visibleDriftsCount = 0;
		driftMarks = new ArrayList<CarDrift>( MaxSkidMarks );

		player = logic.getGame().getLevel().getPlayer();
		model = player.getCarModel();

		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			driftMarks.add( new CarDrift( player ) );
		}

		driftsBatch = new SpriteBatch( MaxSkidMarks / 2, 5 );

		tmp = new Vector2();
		last = new Vector2();
	}

	public static void tick()
	{
		addDriftMark();

		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			driftMarks.get( i ).tick();
		}
	}

	public static void render()
	{

		OrthographicCamera cam = Director.getCamera();

		// optimize drawing, group by texture

		driftsBatch.setProjectionMatrix( cam.projection );
		driftsBatch.setTransformMatrix( cam.view );

		CarDrift d;
		visibleDriftsCount = 0;

		// front drift marks
		driftsBatch.begin();
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			d = driftMarks.get( i );
			if( d.visible && d.life > 0 )
			{
				visibleDriftsCount++;
				d.updateRatio();
				d.renderFront( driftsBatch );
			}
		}
		driftsBatch.end();

		// rear drift marks
		driftsBatch.begin();
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			d = driftMarks.get( i );
			if( d.visible && d.life > 0 )
			{
				d.renderRear( driftsBatch );
			}
		}
		driftsBatch.end();
	}

	public static void reset()
	{
		driftIndex = 0;
	}

	public static void dispose()
	{
		driftMarks.clear();
	}

	/**
	 * Skid marks
	 *
	 */

	private static Vector2 tmp;
	private static Vector2 last;

	private static void addDriftMark()
	{
		if( player.getCarDescriptor().velocity_wc.len2() < 1 )
		{
			return;
		}

		tmp.set( player.state().position );
		float angle = player.state().orientation;

		if( (int)tmp.x == (int)last.x && (int)tmp.y == (int)last.y )
		{
			return;
		}

		// tmp.set( Convert.mt2px(player.getBody().getPosition()) );
		// float angle = player.getBody().getAngle() *
		// MathUtils.radiansToDegrees;

		// lateral forces are in the range [-max_grip, max_grip]
		float flatf = Math.abs( player.getSimulator().lateralForceFront.y );
		flatf = AMath.clamp( flatf / model.max_grip, 0, 1f );

		float flatr = Math.abs( player.getSimulator().lateralForceRear.y );
		flatr = AMath.clamp( flatr / model.max_grip, 0, 1f );

		// add front drift marks?
		CarDrift drift = driftMarks.get( driftIndex++ );
		if( driftIndex == MaxSkidMarks ) driftIndex = 0;

		float from = 0f;
		float div = 1f - from;
		float af = (flatf - from) / div;
		float ar = (flatr - from) / div;
		if( af > 0 || ar > 0 )
		{
			drift.setAlpha( af, ar );
			drift.setPosition( tmp );
			drift.setOrientation( angle );
			drift.maxLife = 5f;
			drift.life = drift.maxLife;

			last.set( tmp );
		} else
		{
			drift.life = 0;
		}
	}
}
