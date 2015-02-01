package animatedping;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.imageio.ImageIO;

import org.bukkit.configuration.file.YamlConfiguration;

import com.comphenix.protocol.wrappers.WrappedServerPing.CompressedImage;

public class Config {

	private AnimatedPing plugin;
	public Config(AnimatedPing plugin) {
		this.plugin = plugin;
	}

	private PingData[] pings;
	private int interval = 300;
	private int responsesLimit = -1;
	private HashSet<String> ignoredIPs = new HashSet<String>();

	public PingData[] getPings() {
		return pings;
	}

	public int getInterval() {
		return interval;
	}

	public HashSet<String> getIgnoredIPs() {
		return ignoredIPs;
	}

	public int getResponsesLimit() {
		return responsesLimit;
	}

	public void loadConfig() {
		File configfile = new File(plugin.getDataFolder(), "config.yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configfile);
		interval = config.getInt("interval", interval);
		ignoredIPs.clear();
		for (String ignoredIp : config.getStringList("ignoredIPs")) {
			ignoredIPs.add(ignoredIp);
		}
		responsesLimit = config.getInt("responsesLimit", responsesLimit);
		ArrayList<PingData> pings = new ArrayList<PingData>();
		for (String key : config.getKeys(false)) {
			if (key.equals("interval") || key.equals("ignoredIPs") || key.equals("responsesLimit")) {
				continue;
			}
			PingData pingData = new PingData(
				key,
				config.getString(key+".motd"),
				config.getString(key+".iconpath") != null ? new File(config.getString(key+".iconpath")) : null,
				config.getStringList(key+".players")
			);
			pings.add(pingData);
		}
		this.pings = pings.toArray(new PingData[0]);
		config = new YamlConfiguration();
		config.set("interval", 300);
		config.set("ignoredIPs", new ArrayList<String>(ignoredIPs));
		config.set("responsesLimit", responsesLimit);
		for (PingData ping : pings) {
			if (ping.getMotd() != null) {
				config.set(ping.getConfigName()+".motd", ping.getMotd());
			}
			if (ping.getImageFile() != null) {
				config.set(ping.getConfigName()+".iconpath", ping.getImageFile().getPath());
			}
			if (ping.getPlayers() != null) {
				config.set(ping.getConfigName()+".players", ping.getPlayers());
			}
		}
		try {
			config.save(configfile);
		} catch (IOException e) {
		}
	}

	public static class PingData {

		private String configname;
		private File imagePath;

		private String motd;
		private CompressedImage image;
		private List<String> players;

		public PingData(String configname, String motd, File imagePath, List<String> players) {
			this.configname = configname;
			this.motd = motd;
			this.imagePath = imagePath;
			this.players = players;
			try {
				this.image = CompressedImage.fromPng(ImageIO.read(imagePath));
			} catch (Exception e) {
			}
		}

		protected String getConfigName() {
			return configname;
		}

		protected File getImageFile() {
			return imagePath;
		}

		public String getMotd() {
			return motd;
		}

		public CompressedImage getImage() {
			return image;
		}

		public List<String> getPlayers() {
			return players;
		}

	}

}
