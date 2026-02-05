package me.beanes.betterslowdown;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.beanes.betterslowdown.listener.PacketListener;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterSlowdown extends JavaPlugin {
    private FallbackMode mode = FallbackMode.SPRINT;
    private boolean alwaysAddSprint = false;
    private boolean noSlowdown = false;
    private PacketListenerCommon listener;

    public FallbackMode getMode() {
        return mode;
    }

    public boolean isAlwaysAddSprint() {
        return alwaysAddSprint;
    }

    public boolean isNoSlowdown() {
        return noSlowdown;
    }

    @Override
    public void onLoad() {
        listener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListener(this), PacketListenerPriority.LOWEST);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            mode = FallbackMode.valueOf(getConfig().getString("mode").toUpperCase());
            alwaysAddSprint = getConfig().getBoolean("always-add-sprint");
            noSlowdown = getConfig().getBoolean("no-slowdown", false);
        } catch (Exception ex) {
            mode = FallbackMode.SERVER;
            getLogger().info("Failed to load config due to error");
        }

        getLogger().info("Slowdown fallback mode is set to " + mode);
        getLogger().info("Always add sprint is set to " + alwaysAddSprint);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().getEventManager().unregisterListener(listener);
    }
}
