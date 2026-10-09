package com.blakube.bktops.plugin.listener;

import com.blakube.bktops.api.queue.Priority;
import com.blakube.bktops.api.registry.TopRegistry;
import com.blakube.bktops.api.top.Top;
import com.blakube.bktops.api.config.ConfigContainer;
import com.blakube.bktops.api.config.ConfigType;
import com.blakube.bktops.plugin.BKTops;
import com.blakube.bktops.plugin.cache.PlayerNameCache;
import com.blakube.bktops.plugin.reward.PendingRewardService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    static final long DEFAULT_REWARD_DELAY_TICKS = 60L;

    private final BKTops plugin;
    private final TopRegistry<UUID> registry;
    private final PendingRewardService pendingRewardService;

    public PlayerJoinListener(TopRegistry<UUID> registry, @Nullable PendingRewardService pendingRewardService) {
        this.plugin = JavaPlugin.getPlugin(BKTops.class);
        this.registry = registry;
        this.pendingRewardService = pendingRewardService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerNameCache.put(uuid, player.getName());

        for (Top<UUID> top : registry.getAll()) {
            top.enqueue(List.of(uuid), Priority.HIGH, "player_join");
        }

        if (pendingRewardService != null) {
            long delay = resolveRewardDelay(plugin.getConfigService().provide(ConfigType.CONFIG));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    pendingRewardService.deliverPending(player);
                }
            }, delay);
        }
    }

    static long resolveRewardDelay(ConfigContainer config) {
        return Math.max(0L, config.getLong("rewards.join-delivery-delay-ticks", DEFAULT_REWARD_DELAY_TICKS));
    }
}
