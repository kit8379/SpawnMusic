package org.me.spawnmusic;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class SpawnMusic extends JavaPlugin implements Listener {

    private final HashMap<UUID, BukkitTask> musicTasks = new HashMap<>();
    private String soundName;
    private SoundCategory category;
    private float volume;
    private float pitch;
    private long intervalTicks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        soundName = config.getString("sound", "music_disc.otherside");
        category = SoundCategory.valueOf(config.getString("category", "MUSIC").toUpperCase());
        volume = (float) config.getDouble("volume", 1.0);
        pitch = (float) config.getDouble("pitch", 1.0);
        int intervalSeconds = config.getInt("interval_seconds", 300);
        intervalTicks = intervalSeconds * 20L;
    }

    @Override
    public void onDisable() {
        cancelAllTasks();
    }

    private void cancelAllTasks() {
        for (BukkitTask task : musicTasks.values()) {
            task.cancel();
        }
        musicTasks.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playConfiguredSound(player);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> playConfiguredSound(player), intervalTicks, intervalTicks);

        musicTasks.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BukkitTask task = musicTasks.remove(event.getPlayer().getUniqueId());
        if (task != null) task.cancel();
    }

    private void playConfiguredSound(Player player) {
        player.playSound(player.getLocation(), soundName, category, volume, pitch);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("spawnmusic.reload")) {
                sender.sendMessage("§cYou don't have permission to reload this plugin.");
                return true;
            }

            reloadConfig();
            loadConfigValues();

            cancelAllTasks();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playConfiguredSound(player);
                BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> playConfiguredSound(player), intervalTicks, intervalTicks);
                musicTasks.put(player.getUniqueId(), task);
            }

            sender.sendMessage("§aSpawnMusic config reloaded and tasks restarted.");
            return true;
        }

        sender.sendMessage("§eUsage: /spawnmusic reload");
        return true;
    }
}