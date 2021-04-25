package com.gnarwhal.ld48.engine.texture;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class Texture {

	protected int id, width, height;

	public Texture(String name) {
		this(name, GL_CLAMP);
	}

	public Texture(String name, int wrap) {
		BufferedImage bi = null;
		try {
			bi = ImageIO.read(new File(name));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (bi != null) {
			width = bi.getWidth();
			height = bi.getHeight();
			int[] pixels = bi.getRGB(0, 0, width, height, null, 0, width);
			ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					int pixel = pixels[i * width + j];
					buffer.put((byte)((pixel >> 16) & 0xFF)); // Red
					buffer.put((byte)((pixel >>  8) & 0xFF)); // Green
					buffer.put((byte)((pixel      ) & 0xFF)); // Blue
					buffer.put((byte)((pixel >> 24) & 0xFF)); // Alpha
				}
			}
			buffer.flip();

			id = glGenTextures();
			bind();
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
			unbind();
		}
	}
	
	public Texture(int id, int width, int height) {
		this.id = id;
		this.width = width;
		this.height = height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}

	public void bind() {
		bind(0);
	}

	public void bind(int activeTexture) {
		glActiveTexture(GL_TEXTURE0 + activeTexture);
		glBindTexture(GL_TEXTURE_2D, id);
	}
	
	public void unbind() {
		glBindTexture(GL_TEXTURE_2D, 0);
	}
	
	public void destroy() {
		glDeleteTextures(id);
	}
}
