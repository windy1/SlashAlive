package tv.twitch.moonmoon.slashalive;

import java.util.Objects;

public class AlivePlayer {

    private final String username;
    private final String uuid;

    public AlivePlayer(String username, String uuid) {
        this.username = Objects.requireNonNull(username);
        this.uuid = Objects.requireNonNull(uuid);
    }

    public String getUsername() {
        return username;
    }

    public String getUUID() {
        return uuid;
    }
}
