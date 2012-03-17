#ifdef GL_ES
precision mediump float;
#endif

//varying vec4 v_color;
//varying vec2 v_texCoords;
varying vec2 v_texCoordsFx;

uniform sampler2D u_texture;

//uniform int blur_len;
//uniform int blur_start;
uniform float blur_div;
uniform float offset_x;
uniform float offset_y;
uniform float one_on_blurlen;


// precompute blur factors (faster, loops will be unrolled)
const int blur_len = 4;			// ctrl quality
const float blur_start = 1.0;
//const float blur_width = -0.08;		// ctrl quantity
//const float blur_div = blur_width / float(blur_len);



// performant version
void main()
{
	float scale = blur_start;
	vec2 o = vec2(offset_x, offset_y);

	vec4 c = vec4(0);
	for( int i = 0; i < blur_len; ++i )
	{
		c += texture2D(u_texture, (v_texCoordsFx * scale) + o);
		scale += blur_div;
	}

	gl_FragColor = c * one_on_blurlen;
}


/*
void main()
{
	float scale = float(blur_start);
	float blurlen_f = float(blur_len);

	vec2 o = vec2(offset_x, offset_y);
	vec2 tc = v_texCoordsFx.st;

	vec4 c = vec4(0);
	for( int i = 0; i < blur_len; ++i )
	{
		c += texture2D(u_texture, (tc * scale) + o);
		scale += blur_div;
	}

	c /= blurlen_f;
	gl_FragColor = c;
}
*/