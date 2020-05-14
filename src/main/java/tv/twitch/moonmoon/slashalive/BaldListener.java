package tv.twitch.moonmoon.slashalive;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tv.twitch.moonmoon.slashalive.data.AliveDb;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class BaldListener implements Listener {

    private final AliveDb db;
    private final Logger log;

    public BaldListener(AliveDb db, Logger log) {
        this.db = Objects.requireNonNull(db);
        this.log = Objects.requireNonNull(log);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.getHealth() <= 0) {
            return;
        }

        db.insertPlayerAsync(player, r -> {
            Optional<String> err = r.getError();
            if (err.isPresent()) {
                String message = "failed to insert player into database: `%s`";
                log.warning(String.format(message, err.get()));
            }
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        db.deletePlayerAsync(event.getEntity().getName(), r -> {
            Optional<String> err = r.getError();
            if (err.isPresent()) {
                String message = "failed to delete player from database: `%s`";
                log.warning(String.format(message, err.get()));
            }
        });
    }
}
