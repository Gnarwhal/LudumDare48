package com.gnarwhal.ld48.game;

import com.gnarwhal.ld48.engine.display.Camera;
import com.gnarwhal.ld48.engine.model.Vao;
import com.gnarwhal.ld48.engine.shaders.GradientShader;
import org.joml.Vector2f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Map {
	private static class Tile {
		public static abstract class RenderPass {
			protected float offset;

			protected RenderPass(float offset) {
				this.offset = offset;
			}

			public abstract void render(Camera camera, int x, int y);
		}

		public static class GradientPass extends RenderPass {
			private static GradientShader shader = null;

			private Vao vao;

			private float r, g, b, a;

			public GradientPass(Vao vao, float offset, float r, float g, float b, float a) {
				super(offset);

				if (shader == null) {
					shader = new GradientShader();
				}

				this.vao = vao;
				this.r   = r;
				this.g   = g;
				this.b   = b;
				this.a   = a;
			}

			public void render(Camera camera, int x, int y) {
				shader.enable();
				shader.setColor(r, g, b, a);
				shader.setMVP(camera.getMatrix().translate(x * TILE_DIMS, y * TILE_DIMS - offset, 0).scale(TILE_DIMS, TILE_DIMS, 1));
				vao.render();
			}
		}

		public static final int
			LEVEL_GROUND = 0,
			LEVEL_WALL   = 1;

		public int     level;
		public boolean solid;
		public ArrayList<RenderPass> render_passes;

		public Tile(int level, boolean solid, ArrayList<RenderPass> render_passes) {
			this.level = level;
			this.solid = solid;
			this.render_passes = render_passes;
		}

		public Tile clone() {
			return new Tile(
				this.level,
				this.solid,
				this.render_passes == null ? null : new ArrayList<>(this.render_passes)
			);
		}
	}

	public static float TILE_DIMS = 128.0f;

	private static final ArrayList<Tile.RenderPass> TOP_GROUND_RENDER = new ArrayList<>();
	private static final ArrayList<Tile.RenderPass> MID_GROUND_RENDER = new ArrayList<>();
	private static final ArrayList<Tile.RenderPass>[] WALL_BORDER_RENDER = new ArrayList[256];

	private static final Tile
		GROUND_TILE  = new Tile(Tile.LEVEL_GROUND, true, null),
		WALL_TILE    = new Tile(Tile.LEVEL_WALL,   true, null);

	private Tile[][] map;

	public Map() {
		if (TOP_GROUND_RENDER.size() == 0) {
			Vao vao = new Vao(
				new float[] {
					1, 0, 0,
					1, 1, 0,
					0, 1, 0,
					0, 0, 0
				},
				new int[] {
					0, 1, 3,
					1, 2, 3
				}
			);
			vao.addAttrib(
				new float[] { 0.5f, 0.5f, 0.5f, 0.5f },
				1
			);
			Tile.GradientPass floor_pass = new Tile.GradientPass(vao, 0, 1, 1, 1, 1);
			MID_GROUND_RENDER.add(floor_pass);

			vao = new Vao(
				new float[] {
					1, 0,     0,
					1, 0.75f, 0,
					0, 0.75f, 0,
					0, 0,     0
				},
				new int[] {
					0, 1, 3,
					1, 2, 3
				}
			);
			vao.addAttrib(
				new float[] { 0.35f, 1.0f, 1.0f, 0.35f },
				1
			);
			Tile.GradientPass wall_pass = new Tile.GradientPass(vao, TILE_DIMS * 0.75f, 1, 1, 1, 1);
			TOP_GROUND_RENDER.add(wall_pass);
			TOP_GROUND_RENDER.add(floor_pass);

			float[] vertices = new float[] {
				0,     0,     0,
				0.25f, 0,     0,
				0.75f, 0,     0,
				1,     0,     0,
				1,     0.15f, 0,
				1,     0.85f, 0,
				1,     1,     0,
				0.75f, 1,     0,
				0.25f, 1,     0,
				0,     1,     0,
				0,     0.85f, 0,
				0,     0.15f, 0,
				0.25f, 0.15f, 0,
				0.75f, 0.15f, 0,
				0.75f, 0.85f, 0,
				0.25f, 0.85f, 0
			};
			int[] indices = new int[] {
				0, 11, 12,
				0, 12, 1,
				1, 12, 2,
				12, 13, 2,
				2, 13, 3,
				3, 13, 4,
				4, 13, 5,
				13, 14, 5,
				5, 14, 6,
				6, 14, 7,
				7, 14, 8,
				14, 15, 8,
				8, 15, 9,
				9, 15, 10,
				10, 15, 11,
				15, 12, 11,
				12, 15, 13,
				13, 15, 14
			};

			float[] values = new float[16];
			for (int i = 0; i <= 0b11111111; ++i) {
				for (int j = 0; j < 16; ++j) {
					values[j] = 0;
				}
				int mask = i;
				for (int j = 0; j < 4; ++j) {
					if ((mask & 1) != 0) {
						for (int k = 0; k < 4; ++k) {
							values[(j * 3 + k) % 12] = 0.25f;
						}
					}
					mask >>= 1;
				}
				for (int j = 0; j < 4; ++j) {
					if ((mask & 1) != 0) {
						values[j * 3] = 0.25f;
					}
					mask >>= 1;
				}
				vao = new Vao(vertices, indices);
				vao.addAttrib(values, 1);

				ArrayList<Tile.RenderPass> render_pass = new ArrayList<>();
				render_pass.add(new Tile.GradientPass(vao, TILE_DIMS * 0.75f, 1, 1, 1, 1));
				WALL_BORDER_RENDER[i] = render_pass;
			}
		}

		try {
			final int WALL_COLOR   = 0xFF000000;
			final int GROUND_COLOR = 0xFFFFFFFF;

			BufferedImage map_layout = ImageIO.read(new File("res/map/layout.png"));
			map = new Tile[map_layout.getWidth()][map_layout.getHeight()];
			for (int x = 0; x < map_layout.getWidth(); ++x) {
				for (int y = 0; y < map_layout.getHeight(); ++y) {
					if (map_layout.getRGB(x, y) == WALL_COLOR) {
						map[x][y] = WALL_TILE.clone();
					} else if (map_layout.getRGB(x, y) == GROUND_COLOR) {
						map[x][y] = GROUND_TILE.clone();
						if (y > 0 && map[x][y - 1].level == Tile.LEVEL_WALL) {
							map[x][y].render_passes = TOP_GROUND_RENDER;
						} else {
							map[x][y].render_passes = MID_GROUND_RENDER;
						}
					}
				}
			}
			for (int x = 0; x < map_layout.getWidth(); ++x) {
				for (int y = 0; y < map_layout.getHeight(); ++y) {
					if (map[x][y].level == Tile.LEVEL_WALL) {
						int result = 0;
						if (                      y > 0                 && map[x    ][y - 1].level == Tile.LEVEL_GROUND) { result |= 1 << 0; }
						if (x < map.length - 1                          && map[x + 1][y    ].level == Tile.LEVEL_GROUND) { result |= 1 << 1; }
						if (                      y < map[0].length - 1 && map[x    ][y + 1].level == Tile.LEVEL_GROUND) { result |= 1 << 2; }
						if (x > 0                                       && map[x - 1][y    ].level == Tile.LEVEL_GROUND) { result |= 1 << 3; }
						if (x > 0              && y > 0                 && map[x - 1][y - 1].level == Tile.LEVEL_GROUND) { result |= 1 << 4; }
						if (x < map.length - 1 && y > 0                 && map[x + 1][y - 1].level == Tile.LEVEL_GROUND) { result |= 1 << 5; }
						if (x < map.length - 1 && y < map[0].length - 1 && map[x + 1][y + 1].level == Tile.LEVEL_GROUND) { result |= 1 << 6; }
						if (x > 0              && y < map[0].length - 1 && map[x - 1][y + 1].level == Tile.LEVEL_GROUND) { result |= 1 << 7; }
						map[x][y].render_passes = new ArrayList<>(WALL_BORDER_RENDER[result]);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void check_collisions(Player player) {
		Vector2f player_motion   = new Vector2f(player.velocity);
		Vector2f player_position = player.base_position.add(0, 0.5f * TILE_DIMS, new Vector2f());

		Vector2f player_pos_min = new Vector2f(
			Math.min(0, player_motion.x),
			Math.min(0, player_motion.y)
		).add(player_position).add(new Vector2f(-Player.PLAYER_DIMS * 0.5f));
		Vector2f player_pos_max = new Vector2f(
			Math.max(0, player_motion.x),
			Math.max(0, player_motion.y)
		).add(player_position).add(new Vector2f(Player.PLAYER_DIMS * 0.5f));

		int min_x = (int) (player_pos_min.x / TILE_DIMS);
		int min_y = (int) (player_pos_min.y / TILE_DIMS);
		int max_x = (int) (player_pos_max.x / TILE_DIMS);
		int max_y = (int) (player_pos_max.y / TILE_DIMS);

		float min_t = 0.0f;
		while (min_t < 1.0f) {
			player_position = player.base_position.add(0, 0.5f * TILE_DIMS, new Vector2f());
			min_t = 1.0f;

			float neutralization_direction = 0.0f;
			for (int x = min_x; x <= max_x; ++x) {
				for (int y = min_y; y <= max_y; ++y) {
					if (map[x][y].level == Tile.LEVEL_WALL) {
						Vector2f[] recall_distances = new Vector2f[] {
							check_intersection(x, y, player_position.add(-Player.PLAYER_DIMS * 0.5f, -Player.PLAYER_DIMS * 0.5f, new Vector2f()), player_motion),
							check_intersection(x, y, player_position.add( Player.PLAYER_DIMS * 0.5f, -Player.PLAYER_DIMS * 0.5f, new Vector2f()), player_motion),
							check_intersection(x, y, player_position.add(-Player.PLAYER_DIMS * 0.5f,  Player.PLAYER_DIMS * 0.5f, new Vector2f()), player_motion),
							check_intersection(x, y, player_position.add( Player.PLAYER_DIMS * 0.5f,  Player.PLAYER_DIMS * 0.5f, new Vector2f()), player_motion)
						};

						for (int i = 0; i < 4; ++i) {
							if (recall_distances[i] != null && recall_distances[i].x < min_t) {
								min_t = recall_distances[i].x;
								neutralization_direction = recall_distances[i].y;
							}
						}
					}
				}
			}

			if (min_t < 1.0f) {
				Vector2f valid_motion = player_motion.mul(min_t, new Vector2f());

				player.base_position.add(valid_motion);
				player_motion.sub(valid_motion).mul(neutralization_direction, 1 - neutralization_direction);

				player.proc_collision();

				if (player_motion.lengthSquared() < 0.001f) {
					min_t = 1.0f;
				}
			} else {
				player.base_position.add(player_motion);
			}
		}
	}

	public Vector2f check_intersection(int x, int y, Vector2f position, Vector2f motion) {
		Vector2f wall_start = new Vector2f(x * TILE_DIMS, y * TILE_DIMS);
		Vector2f wall_end   = new Vector2f(0, TILE_DIMS);

		Vector2f min = new Vector2f(1, 0);
		for (int i = 0; i < 4; ++i) {
			if (motion.dot(new Vector2f(-wall_end.y, wall_end.x)) < 0) {
				float t0, t1;
				if (motion.y == 0) {
					t0 = (motion.y * (wall_start.x - position.x) / motion.x + position.y - wall_start.y) / (wall_end.y - motion.y * wall_end.x / motion.x);
					t1 = (wall_end.x * t0 + wall_start.x - position.x) / motion.x;
				} else {
					t0 = (motion.x * (wall_start.y - position.y) / motion.y + position.x - wall_start.x) / (wall_end.x - motion.x * wall_end.y / motion.y);
					t1 = (wall_end.y * t0 + wall_start.y - position.y) / motion.y;
				}

				if (0 <  t0 && t0 < 1
				 && -0.0001f <= t1 && t1 < 1
				 && t1 < min.x) {
					min.set(t1, i % 2);
				}
			}
			/*
				a0 * t + b0 = c0 * s + d0
				a1 * t + b1 = c1 * s + d1

				s = (a0 * t + b0 - d0) / c0
				a1 * t + b1 = c1 * (a0 * t + b0 - d0) / c0 + d1
				a1 * t + b1 - c1 * a0 * t / c0 = c1 * (b0 - d0) / c0 + d1
				(a1 - c1 * a0 / c0) * t + b1 = c1 * (b0 - d0) / c0 + d1
				t = (c1 * (b0 - d0) / c0 + d1 - b1) / (a1 - c1 * a0 / c0)
			*/

			wall_start.add(wall_end);
			wall_end.set(wall_end.y, -wall_end.x);
		}
		if (min.x < 1) {
			return min;
		} else {
			return null;
		}
	}

	public void render_floor(Camera camera) {
		int min_x = (int) Math.max((camera.getX() / TILE_DIMS), 0);
		int min_y = (int) Math.max((camera.getY() / TILE_DIMS), 0);
		int max_x = (int) Math.min(((camera.getX() + camera.getWidth())  / TILE_DIMS), map.length - 1);
		int max_y = (int) Math.min(((camera.getY() + camera.getHeight()) / TILE_DIMS) + 1, map[0].length - 1);
		for (int x = min_x; x <= max_x; ++x) {
			for (int y = min_y; y <= max_y; ++y) {
				if (map[x][y].level == Tile.LEVEL_GROUND) {
					ArrayList<Tile.RenderPass> render_passes = map[x][y].render_passes;
					if (render_passes != null) {
						for (int i = 0; i < render_passes.size(); ++i) {
							render_passes.get(i).render(camera, x, y);
						}
					}
				}
			}
		}
	}

	public void render_walls(Camera camera) {
		int min_x = (int) Math.max((camera.getX() / TILE_DIMS), 0);
		int min_y = (int) Math.max(((camera.getY() + 0.75f * TILE_DIMS) / TILE_DIMS), 0);
		int max_x = (int) Math.min(((camera.getX() + camera.getWidth())  / TILE_DIMS), map.length - 1);
		int max_y = (int) Math.min(((camera.getY() + camera.getHeight() + 0.75f * TILE_DIMS) / TILE_DIMS), map[0].length - 1);
		for (int x = min_x; x <= max_x; ++x) {
			for (int y = min_y; y <= max_y; ++y) {
				if (map[x][y].level == Tile.LEVEL_WALL) {
					ArrayList<Tile.RenderPass> render_passes = map[x][y].render_passes;
					if (render_passes != null) {
						for (int i = 0; i < render_passes.size(); ++i) {
							render_passes.get(i).render(camera, x, y);
						}
					}
				}
			}
		}
	}
}
