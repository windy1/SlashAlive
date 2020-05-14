package tv.twitch.moonmoon.slashalive.data;

import java.util.Objects;
import java.util.Optional;

public class AlivePlayer {

    private final String username;
    private final String uuid;

    private String caste;

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

    public Optional<String> getCaste() {
        return Optional.ofNullable(caste);
    }

    public void setCaste(String caste) {
        this.caste = caste;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlivePlayer that = (AlivePlayer) o;
        return Objects.equals(username, that.username) &&
            Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, uuid);
    }

    @Override
    public String toString() {
        return String.format("AlivePlayer(username=%s, uuid=%s)", username, uuid);
    }
}
