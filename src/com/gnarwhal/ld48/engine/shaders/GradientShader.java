package com.gnarwhal.ld48.engine.shaders;

import static org.lwjgl.opengl.GL20.*;

public class GradientShader extends Shader {

	private int color_loc;

	public GradientShader() {
		super("res/shaders/gradient/vert.gls", "res/shaders/gradient/frag.gls");
		getUniforms();
	}

	@Override
	protected void getUniforms() {
		color_loc = glGetUniformLocation(program, "input_color");
	}

	public void setColor(float r, float g, float b, float a) {
		glUniform4f(color_loc, r, g, b, a);
	}
}