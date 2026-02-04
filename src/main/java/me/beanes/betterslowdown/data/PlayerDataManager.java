package me.beanes.betterslowdown.data;

import com.github.retrooper.packetevents.protocol.player.User;

import java.util.*;

public class PlayerDataManager {

    // A thread local variable so we don't need a ConcurrentHashMap (which does useless locking!) for the player data
    private final ThreadLocal<Map<User, PlayerData>> cacheThreadLocal = ThreadLocal.withInitial(HashMap::new);
    // This could even use a FastThreadLocal https://netty.io/4.0/api/io/netty/util/concurrent/FastThreadLocal.html

    public void cache(User user, PlayerData data) {
        Map<User, PlayerData> cache = cacheThreadLocal.get();
        cache.put(user, data);
    }

    public void remove(User user) {
        Map<User, PlayerData> cache = cacheThreadLocal.get();
        cache.remove(user);
    }

    public PlayerData get(User user) {
        Map<User, PlayerData> cache = cacheThreadLocal.get();

        PlayerData data = cache.get(user);

        if (data == null) {
            data = new PlayerData(user);
            cache.put(user, data);
        }

        return data;
    }

    public Collection<PlayerData> getAll() {
        return cacheThreadLocal.get().values();
    }
}
