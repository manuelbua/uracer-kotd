package com.bitfire.uracer.effects;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.utils.Convert;

public class CarDrift
{
	public Sprite front, rear;
	public float life;
	public float maxLife;
	public boolean visible;
	private float lifeRatio;
	private float alphaFront, alphaRear;
	public Vector2 position;

	public CarDrift( Car player )
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
		front.setColor( 1, 1, 1, 0 );

		rear.setRegion( Art.skidMarksRear );
		rear.setSize( carWidth, carLength );
		rear.setOrigin( rear.getWidth() / 2, rear.getHeight() / 2 );
		rear.setColor( 1, 1, 1, 0 );

		life = maxLife = 0;
		visible = false;
	}

	public void tick()
	{
		if( life > 0 )
		{
			life -= Physics.dt;
			visible = Director.isVisible( getBoundingRectangle() );
		} else
		{
			life = 0;
			visible = false;
		}
	}

	public void setLife( float secs )
	{
		this.life = secs;
	}

	public void setMaxLife( float maxlife )
	{
		this.maxLife = maxlife;
	}

	public void renderFront( SpriteBatch batch )
	{
		front.setColor( 1, 1, 1, alphaFront * lifeRatio );
		front.draw( batch );
	}

	public void renderRear( SpriteBatch batch )
	{
		rear.setColor( 1, 1, 1, alphaRear * lifeRatio );
		rear.draw( batch );
	}

	public void setAlpha( float front, float rear )
	{
		alphaFront = front;
		alphaRear = rear;
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

	public void updateRatio()
	{
		if( life > 0 )
		{
			lifeRatio = life / maxLife;
		} else
		{
			life = 0f;
			lifeRatio = 0f;
		}
	}
}
