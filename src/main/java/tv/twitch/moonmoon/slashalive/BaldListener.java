package tv.twitch.moonmoon.slashalive;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tv.twitch.moonmoon.slashalive.data.AliveDb;

import java.sql.SQLException;
import java.util.Objects;
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

        // TODO: move off main thread
        try {
            db.insertPlayer(player);
        } catch (SQLException e) {
            String message = "failed to insert player into database: `%s`";
            log.warning(String.format(message, e.getMessage()));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // TODO: move off main thread
        try {
            db.deletePlayer(event.getEntity().getName());
        } catch (SQLException e) {
            String message = "failed to delete player from database: `%s`";
            log.warning(String.format(message, e.getMessage()));
        }
    }
}
