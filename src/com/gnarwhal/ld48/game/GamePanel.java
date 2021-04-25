package com.gnarwhal.ld48.game;

import com.gnarwhal.ld48.engine.display.Camera;
import com.gnarwhal.ld48.engine.display.Window;

public class GamePanel {

	private Map map;
	private Player player;

	public GamePanel() {
		map = new Map();
		player = new Player();
	}
	
	public void update(Window window, Camera camera) {
		player.move(window);
		map.check_collisions(player);
		player.update(camera);
	}
	
	public void render(Camera camera) {
		map.render_floor(camera);
		player.render(camera);
		map.render_walls(camera);
	}
}
