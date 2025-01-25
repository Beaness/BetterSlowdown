package me.beanes.betterslowdown.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import me.beanes.betterslowdown.BetterSlowdown;
import me.beanes.betterslowdown.FallbackMode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class FilterListener implements PacketListener {
    private static final UUID SPRINT_MODIFIER_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    private static final WrapperPlayServerUpdateAttributes.PropertyModifier SPRINT_MODIFIER = new WrapperPlayServerUpdateAttributes.PropertyModifier(SPRINT_MODIFIER_UUID, 0.30000001192092896D, WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_TOTAL);
    private final BetterSlowdown plugin;
    private final ThreadLocal<Map<User, Byte>> lastUsefulBitmaskThreadLocal;
    private final ThreadLocal<Map<User, Double>> lastSpeedThreadLocal;

    public FilterListener(BetterSlowdown plugin) {
        this.plugin = plugin;
        this.lastUsefulBitmaskThreadLocal = ThreadLocal.withInitial(HashMap::new);
        this.lastSpeedThreadLocal = ThreadLocal.withInitial(HashMap::new);
    }


    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Listen for metadata packets
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
            User user = event.getUser();

            // Only check metadata packets about the player itself
            if (wrapper.getEntityId() == event.getUser().getEntityId()) {
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
                                    data.setValue((byte) ((byte) data.getValue() | 0x08)); // Rewrite with sprinting = true
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
        } else if (event.getPacketType() == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
            WrapperPlayServerUpdateAttributes wrapper = new WrapperPlayServerUpdateAttributes(event);
            User user = event.getUser();

            if (wrapper.getEntityId() == user.getEntityId()) {
                for (WrapperPlayServerUpdateAttributes.Property snapshot : wrapper.getProperties()) {
                    if (snapshot.getAttribute().getName().getKey().equals("movement_speed")) {
                        // Calculate the movement speed without sprint
                        boolean exists = snapshot.getModifiers().removeIf(modifier -> modifier.getUUID().equals(SPRINT_MODIFIER_UUID));
                        if (exists) {
                            snapshot.setDirty();
                        }

                        double speed = snapshot.calcValue();

                        Map<User, Double> lastSpeed = lastSpeedThreadLocal.get();

                        if (lastSpeed.getOrDefault(user, -1.0D) == speed) {
                            // Cancel this attribute packet as it only changes the sprint attribute
                            event.setCancelled(true);
                        } else {
                            // Update the speed
                            lastSpeed.put(user, speed);

                            if (exists) {
                                snapshot.addModifier(SPRINT_MODIFIER); // Re-add the modifier if packetevents is on default re-encode
                            } else if (plugin.isAlwaysAddSprint()) {
                                // TODO: check if the player is "allowed" to sprint, keep food value, and check START_SPRINTING and STOP_SPRINTING to prevent omnisprint?
                                snapshot.addModifier(SPRINT_MODIFIER);
                                event.markForReEncode(true);
                            }
                        }
                    }
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            User user = event.getUser();

            // Reset last as the entity is recreated
            this.lastUsefulBitmaskThreadLocal.get().remove(user);
            this.lastSpeedThreadLocal.get().remove(user);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        this.lastUsefulBitmaskThreadLocal.get().remove(event.getUser());
        this.lastSpeedThreadLocal.get().remove(event.getUser());
    }
}