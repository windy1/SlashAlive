package tv.twitch.moonmoon.slashalive.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tv.twitch.moonmoon.slashalive.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class AliveDb {

    private final Plugin plugin;
    private final Logger log;
    private final String url;

    private Connection conn;

    private AliveDb(Plugin plugin, String url) {
        this.plugin = Objects.requireNonNull(plugin);
        this.url = Objects.requireNonNull(url);
        this.log = plugin.getLogger();
    }

    public void insertPlayerAsync(String name, String uuid, Consumer<Result<Void>> callback) {
        Bukkit.getScheduler()
            .runTaskAsynchronously(plugin, () -> callback.accept(insertPlayer(name, uuid)));
    }

    public void insertPlayerAsync(Player player, Consumer<Result<Void>> callback) {
        insertPlayerAsync(player.getName(), player.getUniqueId().toString(), callback);
    }

    public void deletePlayerAsync(String username, Consumer<Result<Void>> callback) {
        Bukkit.getScheduler()
            .runTaskAsynchronously(plugin, () -> callback.accept(deletePlayer(username)));
    }

    public void selectAllAsync(Consumer<Result<List<AlivePlayer>>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> callback.accept(selectAll()));
    }

    public static void connect(
        Plugin plugin,
        Path path,
        Consumer<Result<AliveDb>> callback
    ) throws IOException {
        Logger log = plugin.getLogger();

        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            log.info(String.format("Created empty database file at `%s`", path));
        }

        String url = "jdbc:sqlite:" + path.toString();
        AliveDb db = new AliveDb(plugin, url);

        db.init(r -> {
            Optional<String> err = r.getError();
            if (err.isPresent()) {
                callback.accept(Result.err(err.get()));
            } else {
                callback.accept(Result.ok(db));
            }
        });
    }

    private void init(Consumer<Result<Void>> callback) {
        connectAsync(r -> {
            // check if error connecting
            Optional<String> err = r.getError();
            if (err.isPresent()) {
                callback.accept(Result.err(err.get()));
                return;
            }

            conn = r.getResult()
                .orElseThrow(() -> new IllegalStateException("expected non-null Connection"));

            updateTableAsync(s -> {
                // check if error updating
                Optional<String> updateErr = s.getError();
                if (updateErr.isPresent()) {
                    callback.accept(Result.err(updateErr.get()));
                } else {
                    // success
                    callback.accept(Result.ok(null));
                }
            });
        });
    }

    private void connectAsync(Consumer<Result<Connection>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> callback.accept(connect()));
    }

    private Result<Connection> connect() {
        try {
            return Result.ok(DriverManager.getConnection(url));
        } catch (SQLException e) {
            String message = "error connecting to database: `%s`";
            return Result.err(String.format(message, e.getMessage()));
        }
    }

    private void updateTableAsync(Consumer<Result<Void>> callback) {
        Bukkit.getScheduler()
            .runTaskAsynchronously(plugin, () -> callback.accept(updateTable()));
    }

    private Result<Void> updateTable() {
        final String query =
            "CREATE TABLE IF NOT EXISTS living_players (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username VARCHAR(255) NOT NULL UNIQUE, " +
                "uuid VARCHAR(255) NOT NULL UNIQUE, " +
                "first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
            return Result.ok(null);
        } catch (SQLException e) {
            String message = "error updating database: `%s`";
            return Result.err(message);
        }
    }

    private Result<Void> insertPlayer(String name, String uuid) {
        final String query =
            "INSERT OR IGNORE INTO living_players (username, uuid) VALUES (?, ?)";

        String msg = "Inserting player to database { name=%s, uuid=%s }";
        log.info(String.format(msg, name, uuid));

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, uuid);
            stmt.executeUpdate();
            return Result.ok(null);
        } catch (SQLException e) {
            String message = "error inserting player: `%s`";
            return Result.err(String.format(message, e.getMessage()));
        }
    }

    private Result<Void> deletePlayer(String username) {
        final String query = "DELETE FROM living_players WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
            return Result.ok(null);
        } catch (SQLException e) {
            String message = "error deleting player: `%s`";
            return Result.err(String.format(message, e.getMessage()));
        }
    }

    private Result<List<AlivePlayer>> selectAll() {
        final String query = "SELECT username, uuid FROM living_players";

        List<AlivePlayer> players = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet results = stmt.executeQuery(query)) {
            while (results.next()) {
                String username = results.getString("username");
                String uuid = results.getString("uuid");
                players.add(new AlivePlayer(username, uuid));
            }

            return Result.ok(players);
        } catch (SQLException e) {
            String message = "error retrieving players: `%s`";
            return Result.err(String.format(message, e.getMessage()));
        }
    }
}
