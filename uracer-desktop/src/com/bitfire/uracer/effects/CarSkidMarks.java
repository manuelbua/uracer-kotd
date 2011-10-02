package com.bitfire.uracer.effects;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.effects.TrackEffects.Effects;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public class CarSkidMarks extends TrackEffect
{
	public static final int MaxSkidMarks = 100;

	private ArrayList<SkidMark> skidMarks;
	private int markIndex;
	private int visibleSkidMarksCount;

	private Car player;
	private CarModel model;
	private Vector2 tmp;
	private Vector2 last;

	public CarSkidMarks( Car player )
	{
		super( Effects.CarSkidMarks );

		markIndex = 0;
		visibleSkidMarksCount = 0;
		tmp = new Vector2();
		last = new Vector2();
		this.player = player;
		this.model = player.getCarModel();

		skidMarks = new ArrayList<SkidMark>( MaxSkidMarks );
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			skidMarks.add( new SkidMark( player ) );
		}
	}

	@Override
	public int getParticleCount()
	{
		return visibleSkidMarksCount;
	}

	@Override
	public void dispose()
	{
		skidMarks.clear();
	}

	@Override
	public void reset()
	{
		markIndex = 0;
	}

	@Override
	public void tick()
	{
		addDriftMark();

		SkidMark d;
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			d = skidMarks.get( i );
			if( d.life > 0 )
			{
				d.life -= Physics.dt;
			} else
			{
				d.life = 0;
			}
		}
	}

	@Override
	public void render( SpriteBatch batch )
	{
		float lifeRatio;
		SkidMark d;
		visibleSkidMarksCount = 0;

		// front drift marks
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			d = skidMarks.get( i );
			if( d.life > 0 && Director.isVisible( d.getBoundingRectangle() ) )
			{
				visibleSkidMarksCount++;

				lifeRatio = d.life / d.maxLife;

				d.front.setColor( 1, 1, 1, d.alphaFront * lifeRatio );
				d.rear.setColor( 1, 1, 1, d.alphaRear * lifeRatio );

				d.front.draw( batch );
				d.rear.draw( batch );
			}
		}
	}

	private void addDriftMark()
	{
		if( player.getCarDescriptor().velocity_wc.len2() < 1 )
		{
			return;
		}

		tmp.set( player.state().position );
		float angle = player.state().orientation;
		// tmp.set( player.pos() );
		// float angle = player.orient();

		if( (int)tmp.x == (int)last.x && (int)tmp.y == (int)last.y )
		{
			return;
		}

		// lateral forces are in the range [-max_grip, max_grip]
		float flatf = Math.abs( player.getSimulator().lateralForceFront.y );
		flatf = AMath.clamp( flatf / model.max_grip, 0, 1f );

		float flatr = Math.abs( player.getSimulator().lateralForceRear.y );
		flatr = AMath.clamp( flatr / model.max_grip, 0, 1f );

		// add front drift marks?
		SkidMark drift = skidMarks.get( markIndex++ );
		if( markIndex == MaxSkidMarks ) markIndex = 0;

		float af = flatf;
		float ar = flatr;
		if( af > 0.2f || ar > 0.2f )
		{
			drift.alphaFront = af;
			drift.alphaRear = ar;
			drift.setPosition( tmp );
			drift.setOrientation( angle );
			drift.front.setScale( AMath.clamp( af + 0.8f, 0.85f, 1.1f ) );
			drift.rear.setScale( AMath.clamp( ar + 0.8f, 0.85f, 1.1f ) );
			drift.maxLife = 1.5f;
			drift.life = drift.maxLife;

			last.set( tmp );
		} else
		{
			drift.life = 0;
		}
	}

	private class SkidMark
	{
		public Sprite front, rear;
		public float life;
		public float maxLife;
		public float alphaFront, alphaRear;
		public Vector2 position;

		public SkidMark( Car player )
		{
			front = new Sprite();
			rear = new Sprite();
			position = new Vector2();

			// setup sprites
			CarModel model = player.getCarModel();
			float carWidth = Convert.mt2px( model.width );
			float carLength = Convert.mt2px( model.length );

			front.setRegion( Art.skidMarksFront );
			front.setSize( carWidth, carLength );
			front.setOrigin( front.getWidth() / 2, front.getHeight() / 2 );
			front.setColor( 1, 1, 1, 1 );

			rear.setRegion( Art.skidMarksRear );
			rear.setSize( carWidth, carLength );
			rear.setOrigin( rear.getWidth() / 2, rear.getHeight() / 2 );
			rear.setColor( 1, 1, 1, 1 );

			life = maxLife = 0;
		}

		public void setPosition( Vector2 pos )
		{
			position.set( pos );
			front.setPosition( pos.x - front.getOriginX(), pos.y - front.getOriginY() );
			rear.setPosition( pos.x - rear.getOriginX(), pos.y - rear.getOriginY() );
		}

		public void setOrientation( float degrees )
		{
			front.setRotation( degrees );
			rear.setRotation( degrees );
		}

		private Rectangle tmp = new Rectangle();

		public Rectangle getBoundingRectangle()
		{
			tmp.set( front.getBoundingRectangle() );
			tmp.merge( rear.getBoundingRectangle() );
			return tmp;
		}
	}

}
