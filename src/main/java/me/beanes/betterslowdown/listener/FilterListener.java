package me.beanes.betterslowdown.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.beanes.betterslowdown.BetterSlowdown;
import me.beanes.betterslowdown.FallbackMode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FilterListener implements PacketListener {
    private final BetterSlowdown plugin;
    private final ThreadLocal<Map<User, Byte>> lastUsefulBitmaskThreadLocal;

    public FilterListener(BetterSlowdown plugin) {
        this.plugin = plugin;
        this.lastUsefulBitmaskThreadLocal = ThreadLocal.withInitial(HashMap::new);
    }


    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Listen for metadata packets
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
            User user = event.getUser();

            // Only check metadata packets about the player itself
            if (wrapper.getEntityId() != event.getUser().getEntityId()) {
                Iterator<EntityData> iterator = wrapper.getEntityMetadata().iterator();

                while (iterator.hasNext()) {
                    EntityData data = iterator.next();

                    // Check if the entity metadata bitmask is in this packet
                    if (data.getIndex() == 0) {
                        byte bitmask = (byte) data.getValue();
                        // Calculate the useful bitmask
                        bitmask &= ~0x02; // Remove crouching
                        bitmask &= ~0x08; // Remove sprinting
                        bitmask &= ~0x10; // Remove using

                        // Check if the "useful" bitmask has changed, which means either the client is on fire or invisible
                        Map<User, Byte> lastUsefulBitmask = lastUsefulBitmaskThreadLocal.get();
                        if (lastUsefulBitmask.getOrDefault(user, (byte) 0) != bitmask) {
                            lastUsefulBitmask.put(user, bitmask);

                            if (plugin.getMode() != FallbackMode.SERVER) {
                                if (plugin.getMode() == FallbackMode.SPRINT) {
                                    data.setValue((byte) ((byte) data.getValue() | 0x08)); // Rewrite with sprinting=true
                                } else if (plugin.getMode() == FallbackMode.NO_SPRINT) {
                                    data.setValue((byte) ((byte) data.getValue() & ~0x08)); // Rewrite with sprinting = false
                                }

                                // Needs re-encode
                                event.markForReEncode(true);
                            }
                        } else {
                            // The bitmask update wasn't useful, either remove this part if there's more metadata or cancel the packet
                            if (wrapper.getEntityMetadata().size() == 1) {
                                event.setCancelled(true);
                            } else {
                                iterator.remove();

                                // Needs re-encode
                                event.markForReEncode(true);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        this.lastUsefulBitmaskThreadLocal.get().remove(event.getUser());
    }
}