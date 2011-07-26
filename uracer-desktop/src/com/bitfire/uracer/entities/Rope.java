package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Physics;

public class Rope extends Entity
{
	private final float SegmentWidth = 1f;
	private final float SegmentHeight = .5f;

	private Body ground;

	private int segments;
	private Array<RopeSegment> ropeSegments;

	// graphics
	private Sprite sprite;

	private class RopeSegment extends b2dEntity
	{
		public RopeSegment( Body prev, FixtureDef fd, RevoluteJointDef jd, Vector2 pos, Vector2 anchor )
		{
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DynamicBody;
			bd.position.set( pos );
			body = Physics.world.createBody( bd );
			body.createFixture( fd );

			jd.initialize( prev, body, anchor );
			Physics.world.createJoint( jd );
		}
	}

	private Rope( int numSegments, Body aGround )
	{
		this.segments = numSegments;
		this.ground = aGround;
		// this.batch = new SpriteBatch(100);

		ropeSegments = new Array<RopeSegment>( numSegments );

		// build physics
		PolygonShape shape = new PolygonShape();
		shape.setAsBox( SegmentWidth / 2, SegmentHeight / 2 );

		FixtureDef fd = new FixtureDef();
		fd.shape = shape;
		fd.density = 20.0f;
		fd.friction = 0.2f;

		RevoluteJointDef jd = new RevoluteJointDef();

		Body prevBody = ground;

		for( int i = 0; i < this.segments; i++ )
		{
			RopeSegment rs = new RopeSegment( prevBody, fd, jd, new Vector2( -(SegmentWidth * (segments - 1)) / 2 + SegmentWidth
					* i, 1f ), new Vector2( -(SegmentWidth * segments) / 2 + SegmentWidth * i, 1f ) );

			ropeSegments.add( rs );
			EntityManager.add( rs );

			prevBody = rs.body;
		}

		Vector2 anchor = new Vector2( -(SegmentWidth * segments) / 2 + SegmentWidth * this.segments, 1f );
		jd.initialize( prevBody, ground, anchor );
		Physics.world.createJoint( jd );
		shape.dispose();

		// build graphics
		sprite = new Sprite();
		sprite.setRegion( Art.rope );
		sprite.setSize( Physics.mt2px( SegmentWidth ), Physics.mt2px( SegmentHeight ) );
		sprite.setOrigin( sprite.getWidth() / 2, sprite.getHeight() / 2 );
	}

	// factory method
	public static Rope create( int numSegments, Body aGround )
	{
		Rope rope = new Rope( numSegments, aGround );
		EntityManager.add( rope );
		return rope;
	}

	@Override
	public void onRender( SpriteBatch batch )
	{
		for( int i = 0; i < ropeSegments.size; i++ )
		{
			EntityState renderState = ropeSegments.get( i ).getState();
			sprite.setPosition( renderState.position.x - sprite.getOriginX(), renderState.position.y - sprite.getOriginY() );
			sprite.setRotation( renderState.orientation );
			sprite.draw( batch );
		}
	}
}
