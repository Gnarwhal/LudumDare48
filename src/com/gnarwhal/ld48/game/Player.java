package com.gnarwhal.ld48.game;

import com.gnarwhal.ld48.engine.display.Camera;
import com.gnarwhal.ld48.engine.display.Window;
import com.gnarwhal.ld48.engine.model.Vao;
import com.gnarwhal.ld48.engine.shaders.PlayerShader;
import com.gnarwhal.ld48.engine.texture.Texture;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class Player {
	private static class Particle {
		public Vector2f position;
		public Vector2f velocity;
		public float dimensions;

		public float kill_hour;
		public float clock;
	}

	public static final float PLAYER_DIMS = 64.0f;

	public static final int
		EXPR_NORMAL       = 0,
		EXPR_THREE_SMOAKS = 1,
		EXPR_CONFUSED     = 2,
		EXPR_SQUINT       = 3;

	private static final float
		HOVER_FLUCTUATION = 42.0f,
		HOVER_CYCLE_RATE  = 2.5f;

	private static final int
		NO_ACTION    = 0,
		QUICK_ATTACK = 1,
		DASH         = 2;

	public float hover_offset;
	public float hover_clock;
	public Vector2f base_position;
	public Vector2f position;
	public Vector2f velocity;

	private static PlayerShader shader;
	private static Vao vao;

	private Texture body;
	private Texture[] eyes;
	private Texture particle;

	private int expression;
	private float eye_rotation;
	private float direction;

	private float rate_bias;
	private float spawn_trigger;
	private float position_bias;
	private float target_interp_clock;
	private float particle_spawn_offset;
	private Vector2f particle_base_target;
	private Vector2f particle_target;
	private ArrayList<Particle> particles;

	private int performing_action;
	private float action_clock;
	private float action_progress;
	private boolean cancel_action;

	private float quick_attack_rotation;
	private float vertical_offset;
	private Vector2f quick_attack_direction;

	private Vector2f dash_direction;

	public Player() {
		if (vao == null) {
			shader = new PlayerShader();
			vao = new Vao(
				new float[] {
					 0.5f, -0.5f, 0, // Top left
					 0.5f,  0.5f, 0, // Bottom left
					-0.5f,  0.5f, 0, // Bottom right
					-0.5f, -0.5f, 0  // Top right
				},
				new int[] {
					0, 1, 3,
					1, 2, 3
				}
			);
			vao.addAttrib(
				new float[] {
					1, 0,
					1, 1,
					0, 1,
					0, 0
				},
				2
			);
		}

		body = new Texture("res/img/player/body.png");
		eyes = new Texture[] {
			new Texture("res/img/player/normal_eyes.png"),
			new Texture("res/img/player/three_smoaks_eyes.png"),
			new Texture("res/img/player/confused_eyes.png"),
			new Texture("res/img/player/squint_eyes.png")
		};
		particle = new Texture("res/img/player/particle.png");

		hover_offset = 0.0f;
		base_position = new Vector2f(5 * Map.TILE_DIMS + PLAYER_DIMS * 0.5f, 8 * Map.TILE_DIMS);
		position      = new Vector2f(base_position);
		velocity      = new Vector2f();

		expression = EXPR_NORMAL;
		direction  = 1.0f;

		particles = new ArrayList<>();
		rate_bias = 1.0f;
		spawn_trigger         = 0.0f;
		target_interp_clock   = 1.0f;
		particle_spawn_offset = 0.0f;
		particle_base_target  = new Vector2f(-PLAYER_DIMS, -PLAYER_DIMS * 1.5f);
		particle_target       = new Vector2f(particle_base_target);
	}

	public float rescale(float progress, float start, float end) {
		return (progress - start) / (end - start);
	}

	public void move(Window window) {
		final float RUN_VELOCITY          = Map.TILE_DIMS * 3.5f;
		final float WALK_VELOCITY         = Map.TILE_DIMS * 1.5f;
		final float QUICK_ATTACK_VELOCITY = Map.TILE_DIMS * 0.5f;
		final float VERTICAL_VELOCITY_SCALAR = 0.75f;

		float target_velocity = RUN_VELOCITY;
		if (window.keyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) >= Window.BUTTON_PRESSED || window.controllerButtonPressed(GLFW_GAMEPAD_BUTTON_B) >= Window.BUTTON_PRESSED) {
			target_velocity = WALK_VELOCITY;
		}
		if (performing_action == QUICK_ATTACK) {
			target_velocity = QUICK_ATTACK_VELOCITY;
		} else if (performing_action == DASH) {
			target_velocity = 0.0f;
		}

		Vector2f input_velocity = new Vector2f(0);
		if (window.joystick(GLFW_JOYSTICK_1)) {
			input_velocity.x = window.getJoystickAxis(GLFW_GAMEPAD_AXIS_LEFT_X);
			input_velocity.y = window.getJoystickAxis(GLFW_GAMEPAD_AXIS_LEFT_Y);
			if (Math.abs(input_velocity.x) < 0.25f) { input_velocity.x = 0; }
			if (Math.abs(input_velocity.y) < 0.25f) { input_velocity.y = 0; }
		}

		if (input_velocity.lengthSquared() == 0) {
			if (window.keyPressed(GLFW.GLFW_KEY_A) >= Window.BUTTON_PRESSED) {
				input_velocity.x -= 1;
			}
			if (window.keyPressed(GLFW.GLFW_KEY_D) >= Window.BUTTON_PRESSED) {
				input_velocity.x += 1;
			}
			if (window.keyPressed(GLFW.GLFW_KEY_W) >= Window.BUTTON_PRESSED) {
				input_velocity.y -= 1;
			}
			if (window.keyPressed(GLFW.GLFW_KEY_S) >= Window.BUTTON_PRESSED) {
				input_velocity.y += 1;
			}
		}

		if (input_velocity.lengthSquared() != 0) {
			input_velocity.normalize(target_velocity);
			input_velocity.y *= VERTICAL_VELOCITY_SCALAR;
		}

		if (action_clock < 0.0f) {
			action_clock += Main.dtime;
		} else if (performing_action == NO_ACTION) {
			if (window.keyPressed(GLFW.GLFW_KEY_SPACE) == Window.BUTTON_PRESSED || window.controllerButtonPressed(GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER) == Window.BUTTON_PRESSED) {
				performing_action = QUICK_ATTACK;
				action_clock      = 0.0f;
				if (input_velocity.lengthSquared() != 0) {
					quick_attack_direction = input_velocity.normalize(new Vector2f());
				} else {
					quick_attack_direction = new Vector2f(direction, 0);
				}
			} else if (window.keyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) == Window.BUTTON_PRESSED || window.controllerButtonPressed(GLFW_GAMEPAD_BUTTON_LEFT_BUMPER) == Window.BUTTON_PRESSED) {
				performing_action = DASH;
				action_clock      = 0.0f;
				if (input_velocity.lengthSquared() != 0) {
					dash_direction = input_velocity.normalize(new Vector2f());
				} else {
					dash_direction = new Vector2f(direction, 0);
				}
			}
		}

		final float MAX_BIAS = 0.25f;
		rate_bias = Math.max(MAX_BIAS, lerp(MAX_BIAS, 1.0f, 1 - (input_velocity.length() / RUN_VELOCITY)));

		if (performing_action == DASH) {
			action_clock += Main.dtime;

			final float DASH_TIME     = 0.15f;
			final float DASH_COOLDOWN = 0.25f;
			if (cancel_action) {
				action_clock  = DASH_TIME;
				cancel_action = false;
			}
			if (action_clock >= DASH_TIME) {
				performing_action = NO_ACTION;
				action_clock      = -DASH_COOLDOWN;
				expression        = EXPR_NORMAL;
				rate_bias = 1.0f;

				particle_spawn_offset = 0;
			} else {
				rate_bias = 0.01f;
				particle_spawn_offset = dash_direction.angle(new Vector2f(direction, -1)) * 2.0f / (float) Math.PI;
				final float DASH_SPEED = Map.TILE_DIMS * 15;
				input_velocity.set(dash_direction).mul(DASH_SPEED);
			}
		}

		if (performing_action == NO_ACTION && direction * input_velocity.x < 0) {
			direction = -direction;
			target_interp_clock = 0.0f;
		}

		velocity = input_velocity.mul((float) Main.dtime);

		if (window.keyPressed(GLFW_KEY_Q) == Window.BUTTON_PRESSED) {
			velocity.x = -400;
			velocity.y = 0;
		}
	}

	public void proc_collision() {}

	private float lerp(float start, float end, float lerp) {
		return start + (end - start) * lerp;
	}

	public void update(Camera camera) {
		camera.setCenter(base_position.x, base_position.y);

		if (performing_action == NO_ACTION) {
			hover_clock = (hover_clock + (float) Main.dtime) % HOVER_CYCLE_RATE;
			hover_offset = (float) Math.sin(2 * Math.PI * hover_clock * (1 / HOVER_CYCLE_RATE)) * HOVER_FLUCTUATION * 0.5f;
		}

		position.set(base_position);
		if (performing_action == QUICK_ATTACK) {
			action_clock += Main.dtime;

			final float QUICK_ATTACK_TIME     = 0.5f;
			final float QUICK_ATTACK_COOLDOWN = 0.1f;
			if (cancel_action) {
				action_clock  = QUICK_ATTACK_TIME;
				cancel_action = false;
			}
			if (action_clock >= QUICK_ATTACK_TIME) {
				performing_action = NO_ACTION;
				action_clock      = -QUICK_ATTACK_COOLDOWN;
				expression        = EXPR_NORMAL;
			}

			action_progress = action_clock / QUICK_ATTACK_TIME;

			vertical_offset = 0;
			quick_attack_rotation = 0;
			eye_rotation = 0;
			particle_spawn_offset = 0;
			particle_target.set(particle_base_target);

			if (action_progress < 0.5f) {
				float smooth = (1 - (float) Math.cos(Math.PI * 2 * rescale(action_progress, 0, 0.6f))) * 0.5f;

				final float RECOIL_AMOUNT = PLAYER_DIMS * 0.4f;
				vertical_offset = -smooth * RECOIL_AMOUNT;

				position.y += vertical_offset;
			}

			if (action_progress > 0.25f) {
				float progress = rescale(action_progress, 0.15f, 1);
				quick_attack_rotation = (float) Math.PI * 2 * progress * direction;
				eye_rotation = progress;
				particle_spawn_offset = progress * 4 * -direction;

				final float PARTICLE_BOOST = 2;

				rate_bias = 0.1f;
				float sin = (float) Math.sin(quick_attack_rotation);
				float cos = (float) Math.cos(quick_attack_rotation);
				particle_target.set(
				   cos * particle_base_target.x - sin * particle_base_target.y,
				   sin * particle_base_target.x + cos * particle_base_target.y
				).mul(PARTICLE_BOOST);

				final float ORTHOGONAL = PLAYER_DIMS * 0.5f;
				final float PARALLEL = PLAYER_DIMS * 1.15f;

				position
				   .add(quick_attack_direction.mul((1 - (float) Math.cos(quick_attack_rotation)) * PARALLEL, new Vector2f()))
				   .add(new Vector2f(-quick_attack_direction.y, quick_attack_direction.x).mul((float) Math.sin(quick_attack_rotation) * ORTHOGONAL));
			}
		}

		//////// PARTICLE SYSTEM ////////

		for (int i = 0; i < particles.size(); ++i) {
			Particle p = particles.get(i);
			p.clock += Main.dtime;
			if (p.clock >= p.kill_hour) {
				particles.remove(i);
				--i;
			} else {
				final float PARTICLE_MIN_DIMS = 16.0f;
				final float PARTICLE_MAX_DIMS = 28.0f;

				float interp = p.clock / p.kill_hour;
				p.dimensions = lerp(PARTICLE_MAX_DIMS, PARTICLE_MIN_DIMS, interp);
				p.position.add(p.velocity.mul((float) Main.dtime, new Vector2f()));
			}
		}

		final float TURN_RATE = 0.7f;
		target_interp_clock = Math.min(target_interp_clock + (float) Main.dtime / TURN_RATE, 1);

		spawn_trigger -= Main.dtime;
		if (spawn_trigger < 0) {
			Particle p = new Particle();

			final float PARTICLE_MIN_LIFETIME = 0.5f;
			final float PARTICLE_MAX_LIFETIME = 1.0f;
			p.kill_hour = lerp(PARTICLE_MIN_LIFETIME, PARTICLE_MAX_LIFETIME, (float) Math.random());

			Vector2f spawn_position = new Vector2f(position).add(0, hover_offset);
			position_bias = (4.5f + direction * 0.5f + (float) Math.random() * 2 - 1 + particle_spawn_offset) % 4;
			if (0 <= position_bias && position_bias < 1) {
				spawn_position.add(
					-PLAYER_DIMS * 0.4f * (position_bias * 2.0f - 1.0f),
					-PLAYER_DIMS * 0.4f
				);
			} else if (1 <= position_bias && position_bias < 2) {
				position_bias -= 1;
				spawn_position.add(
					-PLAYER_DIMS * 0.4f,
					 PLAYER_DIMS * 0.4f * (position_bias * 2.0f - 1.0f)
				);
			} else if (2 <= position_bias && position_bias < 3) {
				position_bias -= 2;
				spawn_position.add(
					PLAYER_DIMS * 0.4f * (position_bias * 2.0f - 1.0f),
					PLAYER_DIMS * 0.4f
				);
			} else {
				position_bias -= 3;
				spawn_position.add(
					 PLAYER_DIMS * 0.4f,
					-PLAYER_DIMS * 0.4f * (position_bias * 2.0f - 1.0f)
				);
			}
			p.position = spawn_position;

			p.velocity = new Vector2f(particle_target).mul((2 * target_interp_clock - 1) * direction, 1).add(position).sub(spawn_position).add(velocity.mul(0.1f, new Vector2f()));

			particles.add(p);

			final float BASE_SPAWN_RATE = 0.15f;
			spawn_trigger = BASE_SPAWN_RATE * rate_bias;
		}
	}

	public void render(Camera camera) {
		shader.enable();

		particle.bind();
		for (int i = 0; i < particles.size(); ++i) {
			Particle p = particles.get(i);
			shader.setMVP(
				camera
					.getMatrix()
					.translate(
						p.position.x,
						p.position.y,
						0
					)
					.scaleXY(
						p.dimensions,
						p.dimensions
					)
			);
			vao.render();
		}

		shader.setMVP(
			camera
				.getMatrix()
				.translate(
					position.x,
					position.y + hover_offset,
					0
				)
				.scaleXY(direction * PLAYER_DIMS, PLAYER_DIMS));

		shader.setRotation(0);
		body.bind();
		vao.render();

		shader.setRotation(eye_rotation);
		eyes[expression].bind();
		vao.render();
	}
}
