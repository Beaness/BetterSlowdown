package me.beanes.betterslowdown;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.beanes.betterslowdown.listener.FilterListener;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterSlowdown extends JavaPlugin {
    private FallbackMode mode = FallbackMode.SPRINT;
    private boolean alwaysAddSprint = false;
    private PacketListenerCommon listener;

    public FallbackMode getMode() {
        return mode;
    }

    public boolean isAlwaysAddSprint() {
        return alwaysAddSprint;
    }

    @Override
    public void onLoad() {
        listener = PacketEvents.getAPI().getEventManager().registerListener(new FilterListener(this), PacketListenerPriority.LOWEST);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            mode = FallbackMode.valueOf(getConfig().getString("mode").toUpperCase());
            alwaysAddSprint = getConfig().getBoolean("always-add-sprint");
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
