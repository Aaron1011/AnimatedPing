package animatedping;

import org.bukkit.plugin.java.JavaPlugin;

public class AnimatedPing extends JavaPlugin {

	private Config config;
	public Config getConfiguration() {
		return config;
	}

	@Override
	public void onEnable() {
		config = new Config(this);
		config.loadConfig();
		new PingListener(this);
	}

}
