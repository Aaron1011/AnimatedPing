package animatedping;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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

	protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "AnimatedPingScheduler");
			thread.setDaemon(true);
			return thread;
		}
	});

	public PingListener(final AnimatedPing pluginRef)  {
		manager.addPacketListener(
			new PacketAdapter(
				PacketAdapter
				.params(pluginRef, PacketType.Status.Server.OUT_SERVER_INFO, PacketType.Status.Server.OUT_PING)
				.listenerPriority(ListenerPriority.HIGHEST)
				.optionAsync()
			) {
				@Override
				public void onPacketSending(PacketEvent event) {
					if (
						pluginRef.getConfiguration().getPings().length > 0 &&
						!pluginRef.getConfiguration().getIgnoredIPs().contains(event.getPlayer().getAddress().getHostString())
					) {
						event.setCancelled(true);
						if (event.getPacket().getType() == PacketType.Status.Server.OUT_SERVER_INFO) {
							PingResponseTask task = new PingResponseTask(
								event.getPlayer(), event.getPacket().getServerPings().read(0),
								pluginRef.getConfiguration().getPings(),
								pluginRef.getConfiguration().getResponsesLimit()
							);
							scheduler.scheduleAtFixedRate(task, 1, pluginRef.getConfiguration().getInterval(), TimeUnit.MILLISECONDS);
						}
					}
				}
			}
		);
	}


	private static class PingResponseTask implements Runnable {

		private static final UUID randomUUID = UUID.randomUUID();
		private static final RuntimeException exception = new RuntimeException("Task cancelled");

		private Player player;
		private PingData[] pingResponses;
		private WrappedServerPing originalResponse;
		private int responsesLimit;

		private int responsesCount;

		public PingResponseTask(Player player, WrappedServerPing originalResponce, PingData[] pingDatas, int responsesLimit) {
			this.player = player;
			this.originalResponse = originalResponce;
			this.pingResponses = pingDatas;
			this.responsesLimit = responsesLimit;
			this.originalResponse.setVersionProtocol(-1);
		}

		@Override
		public void run() {
			try {
				//cancel task if response limit reached
				if (responsesLimit != -1 && responsesCount >= responsesLimit) {
                    PacketContainer serverInfo = manager.createPacket(PacketType.Status.Server.OUT_PING);
                    serverInfo.getLongs().write(0, 30L);
                    manager.sendServerPacket(player, serverInfo, false);
					cancel();
				}
				//cancel task if connection is closed
				if (!player.isOnline()) {
					cancel();
				}
				//user version name to display player count
				this.originalResponse.setVersionName("" + Bukkit.getOnlinePlayers().size()+"/"+this.originalResponse.getPlayersMaximum());
				//set ping data
				PingData toDisplay = pingResponses[responsesCount & (pingResponses.length - 1)];
				if (toDisplay.getImage() != null) { 
					originalResponse.setFavicon(toDisplay.getImage());
				}
				if (toDisplay.getMotd() != null) {
					originalResponse.setMotD(WrappedChatComponent.fromText(ChatColor.translateAlternateColorCodes('&', toDisplay.getMotd())));
				}
				if (toDisplay.getPlayers() != null) {
					ArrayList<WrappedGameProfile> profiles = new ArrayList<WrappedGameProfile>();
					for (String player : toDisplay.getPlayers()) {
						WrappedGameProfile profile = new WrappedGameProfile(randomUUID, ChatColor.translateAlternateColorCodes('&', player));
						profiles.add(profile);
					}
					originalResponse.setPlayers(profiles);
				}
				//send packet
				PacketContainer serverInfo = manager.createPacket(PacketType.Status.Server.OUT_SERVER_INFO);
				serverInfo.getServerPings().write(0, originalResponse);
				manager.sendServerPacket(player, serverInfo, false);
				//increment ping selector
				responsesCount++;
			} catch (Throwable e) {
				cancel();
			}
		}

		public void cancel() {
			player = null;
			originalResponse = null;
			throw exception;
		}

	}

}
