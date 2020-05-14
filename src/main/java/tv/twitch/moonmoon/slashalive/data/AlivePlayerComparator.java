package tv.twitch.moonmoon.slashalive.data;

import org.bukkit.Bukkit;

import java.util.Comparator;

public class AlivePlayerComparator implements Comparator<AlivePlayer> {

    @Override
    public int compare(AlivePlayer a, AlivePlayer b) {
        return ((Comparator<AlivePlayer>) this::compareOnline)
            .thenComparing(AlivePlayer::getUsername)
            .compare(a, b);
    }

    private int compareOnline(AlivePlayer a, AlivePlayer b) {
        boolean p1Online = Bukkit.getPlayer(a.getUsername()) != null;
        boolean p2Online = Bukkit.getPlayer(b.getUsername()) != null;

        if (!p1Online && !p2Online) {
            return 0;
        } else if (!p1Online) {
            return 1;
        } else {
            return -1;
        }
    }
}
