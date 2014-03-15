package box2dLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public abstract class PositionalLight extends Light {

	private Body body;
	private float bodyOffsetX;
	private float bodyOffsetY;
	final float sin[];
	final float cos[];

	final Vector2 start = new Vector2();
	final float endX[];
	final float endY[];

	/**
	 * attach positional light to automatically follow body. Position is fixed
	 * to given offset.
	 */
	@Override
	public void attachToBody( Body body, float offsetX, float offSetY ) {
		this.body = body;
		bodyOffsetX = offsetX;
		bodyOffsetY = offSetY;
		if( staticLight )
			staticUpdate();
	}

	@Override
	public Vector2 getPosition() {
		tmpPosition.x = start.x;
		tmpPosition.y = start.y;
		return tmpPosition;
	}

	@Override
	public Body getBody() {
		return body;
	}

	/**
	 * horizontal starting position of light in world coordinates.
	 */
	@Override
	public float getX() {
		return start.x;
	}

	/**
	 * vertical starting position of light in world coordinates.
	 */
	@Override
	public float getY() {
		return start.y;
	}

	private final Vector2 tmpEnd = new Vector2();

	@Override
	public void setPosition( float x, float y ) {
		start.x = x;
		start.y = y;
		if( staticLight )
			staticUpdate();
	}

	@Override
	public void setPosition( Vector2 position ) {
		start.x = position.x;
		start.y = position.y;
		if( staticLight )
			staticUpdate();
	}

	@Override
	void update() {
		if( body != null && !staticLight ) {
			final Vector2 vec = body.getPosition();
			float angle = body.getAngle();
			final float cos = MathUtils.cos( angle );
			final float sin = MathUtils.sin( angle );
			final float dX = bodyOffsetX * cos - bodyOffsetY * sin;
			final float dY = bodyOffsetX * sin + bodyOffsetY * cos;
			start.x = vec.x + dX;
			start.y = vec.y + dY;
			setDirection( angle * MathUtils.radiansToDegrees );
		}

		if( rayHandler.culling ) {
			culled = ((!rayHandler.intersect( start.x, start.y, distance + softShadowLenght )));
			if( culled )
				return;
		}

		if( staticLight )
			return;

		for( int i = 0; i < rayNum; i++ ) {
			rayHandler.m_index = i;
			rayHandler.m_f[i] = 1f;
			tmpEnd.x = endX[i] + start.x;
			rayHandler.m_x[i] = tmpEnd.x;
			tmpEnd.y = endY[i] + start.y;
			rayHandler.m_y[i] = tmpEnd.y;
			if( /* rayHandler.world != null && */!xray ) {
				rayHandler.doRaycast( this, start, tmpEnd );
			}
		}
		setMesh();
	}

	void setMesh() {

		// ray starting point
		int size = 0;
		final float seg[] = rayHandler.m_segments;
		final float m_x[] = rayHandler.m_x;
		final float m_y[] = rayHandler.m_y;
		final float m_f[] = rayHandler.m_f;

		seg[size++] = start.x;
		seg[size++] = start.y;
		seg[size++] = colorF;
		seg[size++] = 1;
		// rays ending points.
		for( int i = 0; i < rayNum; i++ ) {
			seg[size++] = m_x[i];
			seg[size++] = m_y[i];
			seg[size++] = colorF;
			seg[size++] = 1 - m_f[i];
		}
		lightMesh.setVertices( seg, 0, size );

		if( !soft || xray )
			return;

		size = 0;
		// rays ending points.

		for( int i = 0; i < rayNum; i++ ) {
			seg[size++] = m_x[i];
			seg[size++] = m_y[i];
			seg[size++] = colorF;
			final float s = (1 - m_f[i]);
			seg[size++] = s;
			seg[size++] = m_x[i] + s * softShadowLenght * cos[i];
			seg[size++] = m_y[i] + s * softShadowLenght * sin[i];
			seg[size++] = zero;
			seg[size++] = 0f;
		}
		softShadowMesh.setVertices( seg, 0, size );
	}

	@Override
	void render() {
		if( rayHandler.culling && culled )
			return;

		rayHandler.lightRenderedLastFrame++;
		lightMesh.render( rayHandler.lightShader, GL20.GL_TRIANGLE_FAN, 0, vertexNum );
		if( soft && !xray ) {
			softShadowMesh.render( rayHandler.lightShader, GL20.GL_TRIANGLE_STRIP, 0, (vertexNum - 1) * 2 );
		}
	}

	PositionalLight( RayHandler rayHandler, int rays, Color color, float distance, float x, float y, float directionDegree ) {
		super( rayHandler, rays, color, directionDegree, distance );
		start.x = x;
		start.y = y;
		sin = new float[ rays ];
		cos = new float[ rays ];
		endX = new float[ rays ];
		endY = new float[ rays ];

		lightMesh = new Mesh( VertexDataType.VertexArray, staticLight, vertexNum, 0, new VertexAttribute( Usage.Position, 2,
				"vertex_positions" ), new VertexAttribute( Usage.ColorPacked, 4, "quad_colors" ), new VertexAttribute(
				Usage.Generic, 1, "s" ) );
		softShadowMesh = new Mesh( VertexDataType.VertexArray, staticLight, vertexNum * 2, 0, new VertexAttribute(
				Usage.Position, 2, "vertex_positions" ), new VertexAttribute( Usage.ColorPacked, 4, "quad_colors" ),
				new VertexAttribute( Usage.Generic, 1, "s" ) );

		setMesh();
	}

}
