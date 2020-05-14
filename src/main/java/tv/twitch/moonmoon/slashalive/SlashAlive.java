package tv.twitch.moonmoon.slashalive;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tv.twitch.moonmoon.slashalive.cmd.AliveCommand;
import tv.twitch.moonmoon.slashalive.data.AliveDb;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class SlashAlive extends JavaPlugin {

    private static final String DB_FILE_NAME = "alive.db";

    private Logger log;

    @Override
    public void onEnable() {
        log = getLogger();
        Path dbPath = getDataFolder().toPath().resolve(DB_FILE_NAME);

        // initialize database
        try {
            AliveDb.connect(this, dbPath, this::onDbConnect);
        } catch (IOException e) {
            log.warning(String.format("error initializing database: `%s`", e.getMessage()));
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void onDbConnect(Result<AliveDb> r) {
        Optional<String> err = r.getError();
        if (err.isPresent()) {
            log.warning(err.get());
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            AliveDb db = r.getResult()
                .orElseThrow(() -> new IllegalStateException("expected AliveDb instance"));
            init(db);
        }
    }

    private void init(AliveDb db) {
        // set up listeners
        BaldListener listener = new BaldListener(db, log);
        getServer().getPluginManager().registerEvents(listener, this);

        // set up commands
        Objects.requireNonNull(getCommand("alive"))
            .setExecutor(new AliveCommand(this, db));

        // debug
//        for (int i = 0; i < 200; i++) {
//            db.insertPlayerAsync("Test" + i, UUID.randomUUID().toString(), r -> {});
//        }
    }

    @Override
    public void onDisable() {
    }
}
