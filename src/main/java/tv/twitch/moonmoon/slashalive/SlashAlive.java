package tv.twitch.moonmoon.slashalive;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public final class SlashAlive extends JavaPlugin {

    @Override
    public void onEnable() {
        AliveDb db;
        try {
            db = AliveDb.connect(getDataFolder().toPath().resolve("alive.db"), getLogger());
        } catch (SQLException | IOException e) {
            String message = "failed to connect to SQLite database: `%s`";
            getLogger().warning(String.format(message, e.getMessage()));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        BaldListener listener = new BaldListener(db, getLogger());
        getServer().getPluginManager().registerEvents(listener, this);

        Objects.requireNonNull(getCommand("alive"))
            .setExecutor(new AliveCommand(db, getLogger()));
    }

    @Override
    public void onDisable() {
    }
}
