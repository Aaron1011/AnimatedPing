package animatedping;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.v1_7_R4.MinecraftServer;
import net.minecraft.server.v1_7_R4.NetworkManager;
import net.minecraft.server.v1_7_R4.ServerConnection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import animatedping.Config.PingData;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;

public class PingListener {

	private static final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
	private static final List<NetworkManager> managers = getNetworkManagers();

	@SuppressWarnings("unchecked")
	private static List<NetworkManager> getNetworkManagers() {
		try {
			Field ffield = ServerConnection.class.getDeclaredField("f");
			ffield.setAccessible(true);
			return (List<NetworkManager>) ffield.get(MinecraftServer.getServer().getServerConnection());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public PingListener(final AnimatedPing pluginRef)  {
		manager.addPacketListener(
			new PacketAdapter(
				PacketAdapter
				.params(pluginRef, PacketType.Status.Server.OUT_SERVER_INFO, PacketType.Status.Server.OUT_PING)
				.listenerPriority(ListenerPriority.HIGHEST)
			) {
				@Override
				public void onPacketSending(PacketEvent event) {
					if (pluginRef.getConfiguration().getPings().length > 0) {
						event.setCancelled(true);
						if (event.getPacket().getType() == PacketType.Status.Server.OUT_SERVER_INFO) {
							new PingResponceThread(event.getPlayer(), event.getPacket().getServerPings().read(0), 300, pluginRef.getConfiguration().getPings()).start();
						}
					}
				}
			}
		);
	}

	private static class PingResponceThread extends Thread {

		private static final UUID randomUUID = UUID.randomUUID();

		private Player player;
		private int interval;
		private PingData[] pingDatas;
		private WrappedServerPing originalResponce;

		private int currentPingToDisplay;

		public PingResponceThread(Player player, WrappedServerPing originalResponce, int interval, PingData[] pingDatas) {
			this.player = player;
			this.originalResponce = originalResponce;
			this.interval = interval;
			this.pingDatas = pingDatas;
		}

		@Override
		public void run() {
			do {
				try {
					manager.recieveClientPacket(player, new PacketContainer(PacketType.Status.Client.IN_PING));
					PacketContainer serverInfo = manager.createPacket(PacketType.Status.Server.OUT_SERVER_INFO);
					originalResponce.setPlayersOnline(Bukkit.getOnlinePlayers().size());
					PingData toDisplay = pingDatas[currentPingToDisplay];
					if (toDisplay.getImage() != null) { 
						originalResponce.setFavicon(toDisplay.getImage());
					}
					if (toDisplay.getMotd() != null) {
						originalResponce.setMotD(WrappedChatComponent.fromText(ChatColor.translateAlternateColorCodes('&', pingDatas[currentPingToDisplay].getMotd())));
					}
					if (toDisplay.getPlayers() != null) {
						List<WrappedGameProfile> profiles = new ArrayList<WrappedGameProfile>();
						for (String player : toDisplay.getPlayers()) {
							WrappedGameProfile profile = new WrappedGameProfile(randomUUID, ChatColor.translateAlternateColorCodes('&', player));
							profiles.add(profile);
						}
						originalResponce.setPlayers(profiles);
					}
					serverInfo.getServerPings().write(0, originalResponce);
					manager.sendServerPacket(player, serverInfo, false);
					currentPingToDisplay++;
					if (currentPingToDisplay >= pingDatas.length) {
						currentPingToDisplay = 0;
					}
					Thread.sleep(interval);
				} catch (Exception e) {
					break;
				}
			} while (isConnected(player.getAddress()));
			player = null;
			originalResponce = null;
		}

		private boolean isConnected(InetSocketAddress address) {
			for (NetworkManager nm : managers) {
				if (nm.getSocketAddress().equals(address)) {
					return true;
				}
			}
			return false;
		}

	}

}
