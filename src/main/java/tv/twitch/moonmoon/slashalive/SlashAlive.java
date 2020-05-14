package tv.twitch.moonmoon.slashalive;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tv.twitch.moonmoon.slashalive.cmd.AliveCommand;
import tv.twitch.moonmoon.slashalive.data.AliveDb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class SlashAlive extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Logger log = getLogger();

        // initialize database
        // TODO: move off main thread
        AliveDb db;
        try {
            db = AliveDb.connect(getDataFolder().toPath().resolve("alive.db"), log);
        } catch (SQLException | IOException e) {
            String message = "failed to connect to SQLite database: `%s`";
            log.warning(String.format(message, e.getMessage()));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // set up listeners
        BaldListener listener = new BaldListener(db, log);
        getServer().getPluginManager().registerEvents(listener, this);

        // set up commands
        Objects.requireNonNull(getCommand("alive"))
            .setExecutor(new AliveCommand(db, log, getCasteSortOrder()));

        // debug
//        for (int i = 0; i < 200; i++) {
//            try {
//                db.insertPlayer("Test" + i, UUID.randomUUID().toString());
//            } catch (SQLException ignored) {
//            }
//        }
    }

    private List<String> getCasteSortOrder() {
        return getConfig().getStringList("casteSortOrder");
    }

    @Override
    public void onDisable() {
    }
}
