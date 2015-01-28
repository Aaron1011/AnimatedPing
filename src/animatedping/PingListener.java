package animatedping;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

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

	protected static final ProtocolManager manager = ProtocolLibrary.getProtocolManager();

	private final Timer timer = new Timer();

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
							timer.schedule(new PingResponseTask(event.getPlayer(), event.getPacket().getServerPings().read(0), pluginRef.getConfiguration().getPings()), 0, pluginRef.getConfiguration().getInterval());
						}
					}
				}
			}
		);
	}

	private static class PingResponseTask extends TimerTask {

		private Player player;
		private PingData[] pingResponses;
		private WrappedServerPing originalResponse;

		private int currentPingToDisplay;

		public PingResponseTask(Player player, WrappedServerPing originalResponce, PingData[] pingDatas) {
			this.player = player;
			this.originalResponse = originalResponce;
			this.pingResponses = pingDatas;
			this.originalResponse.setVersionProtocol(-1);
		}

		@Override
		public void run() {
			try {
				if (!player.isOnline()) {
					cancel();
					return;
				}
				PacketContainer serverInfo = manager.createPacket(PacketType.Status.Server.OUT_SERVER_INFO);
				this.originalResponse.setVersionName(ChatColor.GRAY.toString()+Bukkit.getOnlinePlayers().size()+"/"+this.originalResponse.getPlayersMaximum());
				PingData toDisplay = pingResponses[currentPingToDisplay];
				if (toDisplay.getImage() != null) { 
					originalResponse.setFavicon(toDisplay.getImage());
				}
				if (toDisplay.getMotd() != null) {
					originalResponse.setMotD(WrappedChatComponent.fromText(ChatColor.translateAlternateColorCodes('&', pingResponses[currentPingToDisplay].getMotd())));
				}
				if (toDisplay.getPlayers() != null) {
					ArrayList<WrappedGameProfile> profiles = new ArrayList<WrappedGameProfile>();
					for (String player : toDisplay.getPlayers()) {
						WrappedGameProfile profile = new WrappedGameProfile(UUID.randomUUID(), ChatColor.translateAlternateColorCodes('&', player));
						profiles.add(profile);
					}
					originalResponse.setPlayers(profiles);
				}
				serverInfo.getServerPings().write(0, originalResponse);
				manager.sendServerPacket(player, serverInfo, false);
				manager.recieveClientPacket(player, new PacketContainer(PacketType.Status.Client.IN_PING));
				currentPingToDisplay++;
				if (currentPingToDisplay >= pingResponses.length) {
					currentPingToDisplay = 0;
				}
			} catch (Throwable e) {
				cancel();
			}
		}

		@Override
		public boolean cancel() {
			player = null;
			originalResponse = null;
			return super.cancel();
		}

	}

}
