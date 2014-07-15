//incoming Position attribute from our SpriteBatch
attribute vec4 a_position;

//the transformation matrix of our SpriteBatch
uniform mat4 u_projTrans;

void main() {
	//transform our 2D screen space position into 3D world space
	gl_Position = u_projTrans * a_position;
}