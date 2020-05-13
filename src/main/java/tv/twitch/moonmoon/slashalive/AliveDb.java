package tv.twitch.moonmoon.slashalive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class AliveDb {

    private final Connection conn;

    public AliveDb(Connection conn) throws SQLException {
        this.conn = Objects.requireNonNull(conn);

        final String query =
            "CREATE TABLE IF NOT EXISTS living_players (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username VARCHAR(255) NOT NULL UNIQUE, " +
                "first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
        }
    }

    public void insertPlayer(String username) throws SQLException {
        final String query =
            "INSERT OR IGNORE INTO living_players (username) VALUES (?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    public void deletePlayer(String username) throws SQLException {
        final String query = "DELETE FROM living_players WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    public List<String> selectAllUsernames() throws SQLException {
        final String query = "SELECT username FROM living_players";

        List<String> usernames = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet results = stmt.executeQuery(query)) {
            while (results.next()) {
                usernames.add(results.getString("username"));
            }
        }

        return usernames;
    }

    public void clear() throws SQLException {
        final String query = "DELETE FROM living_players";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
        }
    }

    public static AliveDb connect(Path path, Logger log) throws SQLException, IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            log.info(String.format("Created empty database file at `%s`", path));
        }

        String url = "jdbc:sqlite:" + path.toString();
        return new AliveDb(DriverManager.getConnection(url));
    }
}
