package com.gnarwhal.ld48.engine.shaders;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;

public class PlayerShader extends Shader {

	private int rotation_loc;

	public PlayerShader() {
		super("res/shaders/player/vert.gls", "res/shaders/player/frag.gls");
		getUniforms();
	}

	@Override
	protected void getUniforms() {
		rotation_loc = glGetUniformLocation(program, "rotation");
	}

	public void setRotation(float rotation) {
		rotation = (rotation % 1) * 2;
		if (rotation > 1) {
			rotation -= 2;
		}
		glUniform1f(rotation_loc, rotation);
	}
}