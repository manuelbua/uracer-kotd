#ifdef GL_ES
precision mediump float;
#endif

//attribute vec4 a_color;
attribute vec4 a_position;
attribute vec2 a_texCoord0;

//uniform mat4 u_proj;
//uniform mat4 u_trans;
uniform mat4 u_projTrans;

uniform float offset_x;
uniform float offset_y;

//varying vec4 v_color;
//varying vec2 v_texCoords;
varying vec2 v_texCoordsFx;

void main()
{
	//v_color = a_color;
	//v_texCoords = a_texCoord0;
	v_texCoordsFx = a_texCoord0 - vec2(offset_x, offset_y);
	gl_Position = u_projTrans * a_position;
}