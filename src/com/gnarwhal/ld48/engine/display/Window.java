package com.gnarwhal.ld48.engine.display;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public class Window {
	
	public static int
		SCREEN_WIDTH,
		SCREEN_HEIGHT,
		REFRESH_RATE;
	
	public static final int
		BUTTON_RELEASED  = 0,
		BUTTON_UNPRESSED = 1,
		BUTTON_PRESSED   = 2,
		BUTTON_HELD      = 3,
		BUTTON_REPEAT    = 4;
	
	public static float SCALE;
	
	private long window;
	private int width, height;
	private boolean resized;
	
	private int[] mouseButtons   = new int[GLFW_MOUSE_BUTTON_LAST + 1];
	private int[] keys           = new int[GLFW_KEY_LAST + 1];
	private int[] gamepadButtons = new int[GLFW_GAMEPAD_BUTTON_LAST];
	private GLFWGamepadState gamepadState;
	
	public Window(String title, boolean vSync) {
		init(0, 0, title, vSync, false, false, false);
	}
	
	public Window(String title, boolean vSync, boolean resizable, boolean decorated) {
		init(800, 500, title, vSync, resizable, decorated, true);
	}
	
	public Window(int width, int height, String title, boolean vSync, boolean resizable, boolean decorated) {
		init(width, height, title, vSync, resizable, decorated, false);
	}
	
	public void init(int lwidth, int lheight, String title, boolean vSync, boolean resizable, boolean decorated, boolean maximized) {
		glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
		
		for (int i = 0; i < mouseButtons.length; i++)
			mouseButtons[i] = 0;
		
		if(!glfwInit()) {
			System.err.println("GLFW failed to initialize!");
			System.exit(-1);
		}
		
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
		
		glfwWindowHint(GLFW_SAMPLES, 8);
		glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
		glfwWindowHint(GLFW_DECORATED, decorated ? GLFW_TRUE : GLFW_FALSE);
		glfwWindowHint(GLFW_MAXIMIZED, maximized ? GLFW_TRUE : GLFW_FALSE);
		
		GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		SCREEN_WIDTH = vidMode.width();
		SCREEN_HEIGHT = vidMode.height();
		SCALE = SCREEN_HEIGHT / 1080f;
		REFRESH_RATE = vidMode.refreshRate();
		if(lwidth == 0 || lheight == 0) {
			width = vidMode.width();
			height = vidMode.height();
			window = glfwCreateWindow(width, height, title, glfwGetPrimaryMonitor(), 0);
		}
		else {
			this.width = lwidth;
			this.height = lheight;
			window = glfwCreateWindow(width, height, title, 0, 0);
		}
		
		glfwMakeContextCurrent(window);
		createCapabilities();
		 
		glfwSwapInterval(vSync ? 1 : 0);
		
		glfwSetWindowSizeCallback(window, (long window, int w, int h) -> {
			width = w;
			height = h;
			resized = true;
			glViewport(0, 0, width, height);
		});
		
		glfwSetMouseButtonCallback(window, (long window, int button, int action, int mods) -> {
			if (action == GLFW_RELEASE)
				mouseButtons[button] = BUTTON_RELEASED;
			if (action == GLFW_PRESS)
				mouseButtons[button] = BUTTON_PRESSED;
			if (action == GLFW_REPEAT)
				mouseButtons[button] = BUTTON_REPEAT;
		});
		
		glfwSetKeyCallback(window, (long window, int key, int scancode, int action, int mods) -> {
			if (key != -1) {
				if (action == GLFW_RELEASE)
					keys[key] = BUTTON_RELEASED;
				if (action == GLFW_PRESS)
					keys[key] = BUTTON_PRESSED;
				if (action == GLFW_REPEAT)
					keys[key] = BUTTON_REPEAT;
			}
		});

		activateClearColor();

		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		glEnable(GL_MULTISAMPLE);

		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
		
		int[] awidth = new int[1], aheight = new int[1];
		glfwGetWindowSize(window, awidth, aheight);
		width = awidth[0];
		height = aheight[0];

		gamepadState = GLFWGamepadState.create();
	}
	
	public void update() {
		for (int i = 0; i < mouseButtons.length; i++)
			if (mouseButtons[i] == BUTTON_RELEASED || mouseButtons[i] == BUTTON_PRESSED)
				++mouseButtons[i];
		for (int i = 0; i < keys.length; i++)
			if (keys[i] == BUTTON_RELEASED || keys[i] == BUTTON_PRESSED)
				++keys[i];
		if (glfwGetGamepadState(GLFW_JOYSTICK_1, gamepadState)) {
			for (int i = 0; i < gamepadButtons.length; ++i) {
				if (gamepadState.buttons(i) == GLFW_RELEASE) {
					if (gamepadButtons[i] == BUTTON_RELEASED) {
						gamepadButtons[i] = BUTTON_UNPRESSED;
					} else if (gamepadButtons[i] != BUTTON_UNPRESSED) {
						gamepadButtons[i] = BUTTON_RELEASED;
					}
				} else {
					if (gamepadButtons[i] == BUTTON_PRESSED) {
						gamepadButtons[i] = BUTTON_HELD;
					} else if (gamepadButtons[i] != BUTTON_HELD) {
						gamepadButtons[i] = BUTTON_PRESSED;
					}
				}
			}
		}
		resized = false;
		glfwPollEvents();
	}
	
	public void activateClearColor() {
		glClearColor(0, 0, 0, 1);
	}

	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void clear() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}
	
	public void swap() {
		glfwSwapBuffers(window);
	}
	
	public void close() {
		glfwSetWindowShouldClose(window, true);
	}
	
	public static void terminate() {
		glfwTerminate();
	}
	
	public boolean shouldClose() {
		return glfwWindowShouldClose(window);
	}
	
	public int keyPressed(int keyCode) {
		return keys[keyCode];
	}
	
	public Vector3f getMouseCoords(Camera camera) {
		double[] x = new double[1], y = new double[1];
		glfwGetCursorPos(window, x, y);
		Vector3f ret = new Vector3f((float) x[0], (float) y[0], 0);
		return ret.mul(camera.getWidth() / this.width, camera.getHeight() / this.height, 1);
	}
	
	public int mousePressed(int button) {
		return mouseButtons[button];
	}

	public boolean joystick(int joystick) {
		return glfwJoystickPresent(joystick) && glfwJoystickIsGamepad(joystick);
	}

	public float getJoystickAxis(int axis) {
		return gamepadState.axes(axis);
	}

	public int controllerButtonPressed(int button) {
		return gamepadButtons[button];
	}
	
	public boolean wasResized() {
		return resized;
	}
}
