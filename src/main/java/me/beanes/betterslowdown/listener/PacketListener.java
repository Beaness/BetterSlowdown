package me.beanes.betterslowdown.listener;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import me.beanes.betterslowdown.BetterSlowdown;
import me.beanes.betterslowdown.FallbackMode;
import me.beanes.betterslowdown.data.PlayerData;
import me.beanes.betterslowdown.data.PlayerDataManager;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class PacketListener implements com.github.retrooper.packetevents.event.PacketListener {
    private static final UUID SPRINT_MODIFIER_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    private static final WrapperPlayServerUpdateAttributes.PropertyModifier SPRINT_MODIFIER = new WrapperPlayServerUpdateAttributes.PropertyModifier(
            SPRINT_MODIFIER_UUID,
            0.30000001192092896D,
            WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_TOTAL
    );
    private static final List<WrapperPlayServerUpdateAttributes.Property> NO_SLOWDOWN_PROPERTY = Collections.singletonList(new WrapperPlayServerUpdateAttributes.Property(Attributes.ATTACK_DAMAGE, 0, Collections.emptyList()));
    private final BetterSlowdown plugin;
    private final PlayerDataManager manager;

    public PacketListener(BetterSlowdown plugin) {
        this.plugin = plugin;
        this.manager = new PlayerDataManager();
    }


    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // We keep track of the current client sprinting metadata
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);

            PlayerData data = manager.get(event.getUser());

            if (wrapper.getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
                data.setClientSprintingState(true);
            } else if (wrapper.getAction() == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                data.setClientSprintingState(false);
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Listen for metadata packets
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);

            // Only check metadata packets about the player itself
            if (wrapper.getEntityId() == event.getUser().getEntityId()) {
                Iterator<EntityData<?>> iterator = wrapper.getEntityMetadata().iterator();

                while (iterator.hasNext()) {
                    EntityData entityData = iterator.next();

                    // Check if the entity metadata bitmask is in this packet
                    if (entityData.getIndex() == MetadataValues.BITMASK_INDEX) {
                        byte bitmask = (byte) entityData.getValue();
                        // Calculate the useful bitmask
                        bitmask &= ~MetadataValues.FLAG_CROUCHING; // Remove crouching
                        bitmask &= ~MetadataValues.FLAG_SPRINTING; // Remove sprinting
                        bitmask &= ~MetadataValues.FLAG_USING; // Remove using

                        // Check if the "useful" bitmask has changed, which means either the client is on fire or invisible
                        PlayerData data = manager.get(event.getUser());
                        byte lastUsefulBitmask = data.getLastUsefulBitmask();

                        if (lastUsefulBitmask != bitmask) {
                            data.setLastUsefulBitmask(bitmask);

                            // Get the fallback mode as we are 100% sending the bitmask
                            FallbackMode mode = plugin.getMode();

                            if (mode != FallbackMode.SERVER) {
                                if (mode == FallbackMode.SPRINT
                                    || (mode == FallbackMode.CLIENT && data.isClientSprintingState())) {
                                    // Rewrite with sprinting = true
                                    entityData.setValue((byte) ((byte) entityData.getValue() | MetadataValues.FLAG_SPRINTING));
                                } else if (mode == FallbackMode.NO_SPRINT
                                        || (mode == FallbackMode.CLIENT && !data.isClientSprintingState())) {
                                    // Rewrite with sprinting = false
                                    entityData.setValue((byte) ((byte) entityData.getValue() & ~MetadataValues.FLAG_SPRINTING));
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
                Iterator<WrapperPlayServerUpdateAttributes.Property> iterator = wrapper.getProperties().iterator();

                while (iterator.hasNext()) {
                    WrapperPlayServerUpdateAttributes.Property snapshot = iterator.next();

                    if (snapshot.getAttribute().equals(Attributes.MOVEMENT_SPEED)) {
                        // Calculate the movement speed without sprint
                        boolean exists = snapshot.getModifiers().removeIf(modifier -> modifier.getUUID().equals(SPRINT_MODIFIER_UUID));
                        if (exists) {
                            snapshot.setDirty();
                        }

                        double speed = snapshot.calcValue();

                        PlayerData data = manager.get(user);
                        double lastSpeed = data.getLastSpeed();

                        if (lastSpeed == speed) {
                            // Cancel this attribute packet as it only changes the sprint attribute
                            event.setCancelled(true);
                        } else {
                            // Update the speed
                            data.setLastSpeed(speed);

                            if (exists) {
                                // Re-add the modifier if packetevents is on default re-encode
                                snapshot.addModifier(SPRINT_MODIFIER);
                            } else if (plugin.isAlwaysAddSprint() && data.isClientSprintingState()) {
                                // Add sprint modifier is the client claimed he was sprinting and always add sprint is enabled
                                snapshot.addModifier(SPRINT_MODIFIER);
                                event.markForReEncode(true);
                            }
                        }
                    } else if (snapshot.getAttribute().equals(Attributes.ATTACK_DAMAGE) && plugin.isNoSlowdown()) {
                        iterator.remove();
                        event.markForReEncode(true);
                    }
                }

                if (wrapper.getProperties().isEmpty()) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN || event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            User user = event.getUser();

            // Reset last as the entity is recreated
            manager.get(user).reset();

            if (plugin.isNoSlowdown()) {
                event.getTasksAfterSend().add(() -> {
                    user.sendPacketSilently(new WrapperPlayServerUpdateAttributes(user.getEntityId(), NO_SLOWDOWN_PROPERTY));
                });
            }
        }
    }

    @Override
    public void onUserConnect(UserConnectEvent event) {
        User user = event.getUser();
        manager.cache(user, new PlayerData());
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        manager.remove(event.getUser());
    }
}
