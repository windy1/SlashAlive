package tv.twitch.moonmoon.slashalive;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AlivePlayerComparator implements Comparator<AlivePlayer> {

    private final Map<String, Integer> casteSortOrder;
    private final Method getRpRace;
    private final Logger log;

    public AlivePlayerComparator(Logger log, List<String> casteSortOrder) {
        this.log = Objects.requireNonNull(log);
        this.casteSortOrder = IntStream
            .range(0, Objects.requireNonNull(casteSortOrder).size())
            .boxed()
            .collect(Collectors.toMap(casteSortOrder::get, i -> i));
        this.getRpRace = ReflectionUtils.getRpRaceMethod(log).orElse(null);
    }

    @Override
    public int compare(AlivePlayer a, AlivePlayer b) {
        return ((Comparator<AlivePlayer>) this::compareCastes)
            .thenComparing(AlivePlayer::getUsername)
            .compare(a, b);
    }

    private int compareCastes(AlivePlayer a, AlivePlayer b) {
        if (getRpRace == null) {
            return 0;
        }

        // TODO: getting RP info of OfflinePlayers is currently bugged in RPEngine

        // start hack
        boolean p1Online = Bukkit.getPlayer(a.getUsername()) != null;
        boolean p2Online = Bukkit.getPlayer(b.getUsername()) != null;

        if (!p1Online && !p2Online) {
            return 0;
        } else if (!p1Online) {
            return 1;
        } else if (!p2Online) {
            return -1;
        }
        // end hack

        String c1 = getCaste(log, getRpRace, a.getUsername());
        String c2 = getCaste(log, getRpRace, b.getUsername());

        Integer i1 = casteSortOrder.get(c1);
        Integer i2 = casteSortOrder.get(c2);

        if (i1 == null && i2 == null) {
            return 0;
        } else if (i1 == null) {
            return 1;
        } else if (i2 == null) {
            return -1;
        } else {
            return Integer.compare(i1, i2);
        }
    }

    private static String getCaste(Logger log, Method getRpRace, String username) {
        return ReflectionUtils.invokeSafe(log, getRpRace, username)
            .map(Object::toString)
            .filter(s -> !s.equalsIgnoreCase("NONE"))
            .orElse(null);
    }
}
