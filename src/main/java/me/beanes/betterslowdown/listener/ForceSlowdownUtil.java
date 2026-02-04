package me.beanes.betterslowdown.listener;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import io.netty.channel.EventLoop;
import me.beanes.betterslowdown.data.PlayerData;
import me.beanes.betterslowdown.data.PlayerDataManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ForceSlowdownUtil {
    private static Set<EventLoop> EVENTLOOP_DONE = new HashSet<>();

    public static void repeatInEventLoop(PlayerDataManager manager, EventLoop eventLoop, int repeatTime) {
        if (EVENTLOOP_DONE.add(eventLoop)) {
            eventLoop.scheduleAtFixedRate(() -> {
                for (PlayerData data : manager.getAll()) {
                    sendSlowdown(data);
                }
            }, 0, repeatTime, TimeUnit.MILLISECONDS);
        }
    }

    private static void sendSlowdown(PlayerData data) {
        if (data.getUser().getName() != null && data.getUser().getConnectionState() == ConnectionState.PLAY) { // Bit of a hack :/
            List<EntityData<?>> forcedNoSprint = Collections.singletonList(new EntityData<>(0, EntityDataTypes.BYTE, (byte) data.getLastUsefulBitmask()));
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(data.getUser().getEntityId(), forcedNoSprint);
            data.getUser().sendPacketSilently(wrapper);
        }
    }
}
