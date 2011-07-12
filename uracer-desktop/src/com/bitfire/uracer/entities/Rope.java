package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Physics;

public class Rope extends Entity
{
	private final float SegmentWidth = 1f;
	private final float SegmentHeight = .5f;

	private Body ground;

	private int segments;
	private Array<RopeSegment> ropeSegments;

	private Vector2 pos = new Vector2();
	private EntityScreenState screenState = new EntityScreenState();

	// graphics
	private Sprite sprite;


	private class RopeSegment extends SubframeInterpolableEntity
	{
		public Body body;

		public RopeSegment(Body prev, FixtureDef fd, RevoluteJointDef jd, Vector2 pos, Vector2 anchor )
		{
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DynamicBody;
			bd.position.set( pos );
			body = Physics.world.createBody( bd );
			body.createFixture( fd );

			jd.initialize( prev, body, anchor );
			Physics.world.createJoint( jd );

			resetState();
		}

		@Override
		public void saveStateTo( EntityScreenState state )
		{
			state.position.set( body.getPosition() );
			state.orientation = body.getAngle();
		}

		@Override
		public boolean isSubframeInterpolated()
		{
			return Config.SubframeInterpolation;
		}
	}


	private Rope( int numSegments, Body aGround )
	{
		this.segments = numSegments;
		this.ground = aGround;
//		this.batch = new SpriteBatch(100);

		ropeSegments = new Array<RopeSegment>(numSegments);

		// build physics
		PolygonShape shape = new PolygonShape();
		shape.setAsBox( SegmentWidth/2, SegmentHeight/2 );

		FixtureDef fd = new FixtureDef();
		fd.shape = shape;
		fd.density = 20.0f;
		fd.friction = 0.2f;

		RevoluteJointDef jd = new RevoluteJointDef();

		Body prevBody = ground;

		for( int i = 0; i < this.segments; i++ )
		{
			RopeSegment rs = new RopeSegment(
					prevBody, fd, jd,
					new Vector2( -(SegmentWidth * (segments - 1)) / 2 + SegmentWidth * i, 1f ),
					new Vector2( -(SegmentWidth * segments) / 2 + SegmentWidth * i, 1f ) );

			ropeSegments.add( rs );
			EntityManager.add( rs );

			prevBody = rs.body;
		}

		Vector2 anchor = new Vector2( -(SegmentWidth*segments)/2 + SegmentWidth * this.segments, 1f );
		jd.initialize( prevBody, ground, anchor );
		Physics.world.createJoint( jd );
		shape.dispose();

		// build graphics
		sprite = new Sprite();
		sprite.setRegion( Art.rope );
		sprite.setSize( Physics.w2s( SegmentWidth ), Physics.w2s( SegmentHeight ) );
		sprite.setOrigin( sprite.getWidth()/2, sprite.getHeight()/2 );
	}

	// factory method
	public static Rope create( int numSegments, Body aGround )
	{
		Rope rope = new Rope( numSegments, aGround );
		EntityManager.add( rope );
		return rope;
	}

	@Override
	public void onRender( SpriteBatch batch, Camera screen, Camera world, float temporalAliasingFactor )
	{
		for( int i = 0; i < ropeSegments.size; i++ )
		{
			screenState.set( ropeSegments.get( i ).getState() );

			pos = Physics.w2s( screenState.position );
			float ang = screenState.orientation * MathUtils.radiansToDegrees;

			sprite.setPosition( pos.x - sprite.getOriginX(), pos.y - sprite.getOriginY() );
			sprite.setRotation( ang );
			sprite.draw( batch );
		}
	}
}
