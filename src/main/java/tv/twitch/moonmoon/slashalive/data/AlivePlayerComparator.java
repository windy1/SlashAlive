package tv.twitch.moonmoon.slashalive.data;

import tv.twitch.moonmoon.slashalive.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
//        boolean p1Online = Bukkit.getPlayer(a.getUsername()) != null;
//        boolean p2Online = Bukkit.getPlayer(b.getUsername()) != null;
//
//        if (!p1Online && !p2Online) {
//            return 0;
//        } else if (!p1Online) {
//            return 1;
//        } else if (!p2Online) {
//            return -1;
//        }
        // end hack

        int i1 = a.getCaste().map(casteSortOrder::get).orElse(-1);
        int i2 = b.getCaste().map(casteSortOrder::get).orElse(-1);

        if (i1 == -1 && i2 == -1) {
            return 0;
        } else if (i1 == -1) {
            return 1;
        } else if (i2 == -1) {
            return -1;
        } else {
            return Integer.compare(i1, i2);
        }
    }
}