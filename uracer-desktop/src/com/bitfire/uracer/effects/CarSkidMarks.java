package com.bitfire.uracer.effects;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.effects.TrackEffects.Effects;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DriftInfo;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public class CarSkidMarks extends TrackEffect
{
	public static final int MaxSkidMarks = 100;

	private ArrayList<SkidMark> skidMarks;
	private int markIndex;
	private int visibleSkidMarksCount;

	private Car player;
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

		skidMarks = new ArrayList<SkidMark>( MaxSkidMarks );
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			skidMarks.add( new SkidMark( player.getCarModel() ) );
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
		tmp.set( player.state().position );

		if( player.getCarDescriptor().velocity_wc.len2() < 1 )
		{
			return;
		}

		// avoid blatant overdrawing
		if( (int)tmp.x == (int)last.x && (int)tmp.y == (int)last.y )
		{
			return;
		}

		DriftInfo di = DriftInfo.get();
		if( di.driftStrength > 0.2f )
//		if( di.isDrifting )
		{
			// add front drift marks?
			SkidMark drift = skidMarks.get( markIndex++ );
			if( markIndex == MaxSkidMarks ) markIndex = 0;

			drift.alphaFront = di.driftStrength;
			drift.alphaRear = di.driftStrength;
			drift.setPosition( tmp );
			drift.setOrientation( player.state().orientation );
			drift.front.setScale( AMath.clamp( di.lateralForcesFront + 0.8f, 0.85f, 1.1f ) );
			drift.rear.setScale( AMath.clamp( di.lateralForcesRear + 0.8f, 0.85f, 1.1f ) );
			drift.maxLife = 1.5f;
			drift.life = drift.maxLife;

			last.set( tmp );
		}
	}

	private class SkidMark
	{
		public Sprite front, rear;
		public float life;
		public float maxLife;
		public float alphaFront, alphaRear;
		public Vector2 position;

		public SkidMark( CarModel model )
		{
			front = new Sprite();
			rear = new Sprite();
			position = new Vector2();

			// setup sprites
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
