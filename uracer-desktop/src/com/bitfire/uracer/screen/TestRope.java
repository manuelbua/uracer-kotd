package com.bitfire.uracer.screen;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Physics;

public class TestRope
{
	private final float SegmentWidth = 2.15f;
	private final float SegmentHeight = 1.5f;

	private World world;
	private Body ground;

	private int segments;
	private Array<Body> ropeSegments = new Array<Body>();
	private SpriteBatch batch;

	// graphics
	private Sprite sprite;
	private Texture tex;

	TestRope( World b2dWorld, int numSegments, Body aGround )
	{
		this.world = b2dWorld;
		this.segments = numSegments;
		this.ground = aGround;
		this.batch = new SpriteBatch(100);

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
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DynamicBody;
			bd.position.set( -(SegmentWidth*(segments-1))/2 + SegmentWidth * i, 5.0f );
			Body body = world.createBody( bd );
			body.createFixture( fd );

			Vector2 anchor = new Vector2( -(SegmentWidth*segments)/2 + SegmentWidth * i, 5.0f );
			jd.initialize( prevBody, body, anchor );
			world.createJoint( jd );
			prevBody = body;
			ropeSegments.add( body );
		}

		Vector2 anchor = new Vector2( -(SegmentWidth*segments)/2 + SegmentWidth * this.segments, 5.0f );
		jd.initialize( prevBody, ground, anchor );
		world.createJoint( jd );
		shape.dispose();

		sprite = new Sprite();
		sprite.setRegion( Art.rope );
		sprite.setSize( Physics.w2s( SegmentWidth ), Physics.w2s( SegmentHeight ) );
		sprite.setOrigin( sprite.getWidth()/2, sprite.getHeight()/2 );
	}

	Vector2 pos = new Vector2();
	public void render( Camera camera )
	{
		batch.setProjectionMatrix( camera.projection );
		batch.setTransformMatrix( camera.view );

		batch.disableBlending();
		batch.begin();

		for( int i = 0; i < ropeSegments.size; i++ )
		{
			Body body = ropeSegments.get( i );
			pos = body.getPosition();
			pos = Physics.w2s( pos );
			sprite.setPosition( pos.x-sprite.getOriginX(), pos.y-sprite.getOriginY() );
			sprite.setRotation( body.getAngle()*MathUtils.radiansToDegrees );
			sprite.draw( batch, 0.4f );
		}

		batch.end();
	}
}
