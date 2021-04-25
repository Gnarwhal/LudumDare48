package com.gnarwhal.ld48.engine.display;

import com.gnarwhal.ld48.engine.texture.Texture;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.glFramebufferTexture;

public class Framebuffer {
	
	int fbo, rbo, width, height;
	int colorBuf, depthTex;
	float r, g, b, a;

	Framebuffer(int width, int height, float r, float g, float b, float a) {
		this.width = width;
		this.height = height;
		
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;

		fbo = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
		glDrawBuffer(GL_COLOR_ATTACHMENT0);

		rbo = 0;
		colorBuf = 0;
		depthTex = 0;
	}

	Framebuffer addColorAttachment(Texture texture) {
		if (colorBuf == 0) {
			int id = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, id);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
			texture = new Texture(id, width, height);
			colorBuf = 1;
		}
		return this;
	}

	Framebuffer addDepthTextureAttachment(Texture texture) {
		if (depthTex == 0) {
			int id = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, id);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
			glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, id, 0);
			texture = new Texture(id, width, height);
			depthTex = 1;
		}
		return this;
	}

	Framebuffer addDepthBufferAttachment() {
		if (rbo == 0) {
			rbo = glGenRenderbuffers();
			glBindRenderbuffer(GL_RENDERBUFFER, rbo);
			glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
			glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);
		}
		return this;
	}

	void bind() {
		glBindTexture(GL_TEXTURE_2D, 0);
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
		glViewport(0, 0, width, height);

		glClearColor(r, g, b, a);
	}

	void unbind(Window window) {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glViewport(0, 0, window.getWidth(), window.getHeight());
		window.activateClearColor();
	}

	void clear() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	public void cleanup() {
		if (rbo != 0)
			glDeleteRenderbuffers(rbo);
		glDeleteFramebuffers(fbo);
	}
}
