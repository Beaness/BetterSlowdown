package me.beanes.betterslowdown.data;

import com.github.retrooper.packetevents.protocol.player.User;

public class PlayerData {
    private final User user;
    private byte lastUsefulBitmask;
    private double lastSpeed;
    private boolean clientSprintingState;

    public PlayerData(User user) {
        this.user = user;
        this.reset();
    }

    public void reset() {
        this.lastUsefulBitmask = (byte) -1;
        this.lastSpeed = -1.0D;
        this.clientSprintingState = false;
    }

    public User getUser() {
        return user;
    }

    public double getLastSpeed() {
        return lastSpeed;
    }

    public byte getLastUsefulBitmask() {
        return lastUsefulBitmask;
    }

    public boolean isClientSprintingState() {
        return clientSprintingState;
    }

    public void setLastUsefulBitmask(byte lastUsefulBitmask) {
        this.lastUsefulBitmask = lastUsefulBitmask;
    }

    public void setLastSpeed(double lastSpeed) {
        this.lastSpeed = lastSpeed;
    }

    public void setClientSprintingState(boolean clientSprintingState) {
        this.clientSprintingState = clientSprintingState;
    }
}
