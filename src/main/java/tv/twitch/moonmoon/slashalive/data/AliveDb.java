package tv.twitch.moonmoon.slashalive.data;

import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class AliveDb {

    private final Connection conn;
    private final Logger log;

    public AliveDb(Connection conn, Logger log) throws SQLException {
        this.conn = Objects.requireNonNull(conn);
        this.log = Objects.requireNonNull(log);

        final String query =
            "CREATE TABLE IF NOT EXISTS living_players (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username VARCHAR(255) NOT NULL UNIQUE, " +
                "uuid VARCHAR(255) NOT NULL UNIQUE, " +
                "first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
        }
    }

    public void insertPlayer(String name, String uuid) throws SQLException {
        final String query =
            "INSERT OR IGNORE INTO living_players (username, uuid) VALUES (?, ?)";

        String msg = "Inserting player to database { name=%s, uuid=%s }";
        log.info(String.format(msg, name, uuid));

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, uuid);
            stmt.executeUpdate();
        }
    }

    public void insertPlayer(Player player) throws SQLException {
        insertPlayer(player.getName(), player.getUniqueId().toString());
    }

    public void deletePlayer(String username) throws SQLException {
        final String query = "DELETE FROM living_players WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    public List<AlivePlayer> selectAll() throws SQLException {
        final String query = "SELECT username, uuid FROM living_players";

        List<AlivePlayer> players = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet results = stmt.executeQuery(query)) {
            while (results.next()) {
                String username = results.getString("username");
                String uuid = results.getString("uuid");
                players.add(new AlivePlayer(username, uuid));
            }
        }

        return players;
    }

    public static AliveDb connect(Path path, Logger log) throws SQLException, IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            log.info(String.format("Created empty database file at `%s`", path));
        }

        String url = "jdbc:sqlite:" + path.toString();
        return new AliveDb(DriverManager.getConnection(url), log);
    }
}
