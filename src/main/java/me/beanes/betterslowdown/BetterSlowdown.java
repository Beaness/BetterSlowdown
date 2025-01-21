package me.beanes.betterslowdown;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.beanes.betterslowdown.listener.FilterListener;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterSlowdown extends JavaPlugin {
    private FallbackMode mode = FallbackMode.SPRINT;

    public FallbackMode getMode() {
        return mode;
    }

    @Override
    public void onLoad() {
        PacketEvents.getAPI().getEventManager().registerListener(new FilterListener(this), PacketListenerPriority.LOWEST);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            mode = FallbackMode.valueOf(getConfig().getString("mode").toUpperCase());
        } catch (Exception ex) {
            mode = FallbackMode.SERVER;
            getLogger().info("Failed to load config due to error");
        }

        getLogger().info("Slowdown fallback mode is set to " + mode);
    }
}
