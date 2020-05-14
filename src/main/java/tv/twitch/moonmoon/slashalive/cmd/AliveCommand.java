package tv.twitch.moonmoon.slashalive.cmd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tv.twitch.moonmoon.slashalive.ReflectionUtils;
import tv.twitch.moonmoon.slashalive.data.AliveDb;
import tv.twitch.moonmoon.slashalive.data.AlivePlayer;
import tv.twitch.moonmoon.slashalive.data.AlivePlayerComparator;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AliveCommand implements CommandExecutor {

    public static final String GENERIC_ERROR =
        ChatColor.RED + "An unexpected error occurred. See console for details.";

    private final AliveDb db;
    private final Logger log;
    private final List<String> casteSortOrder;

    public AliveCommand(AliveDb db, Logger log, List<String> casteSortOrder) {
        this.db = Objects.requireNonNull(db);
        this.log = Objects.requireNonNull(log);
        this.casteSortOrder = Objects.requireNonNull(casteSortOrder);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (args.length) {
            case 0: {
                // show first page
                return showAlive(sender, 1);
            }
            case 1: {
                // show numbered page
                try {
                    return showAlive(sender, Integer.parseInt(args[0]));
                } catch (NumberFormatException e) {
                    String message = String.format("Invalid page: `%s`", args[0]);
                    sender.sendMessage(ChatColor.RED + message);
                    return false;
                }
            }
            case 2: {
                switch (args[0]) {
                    case "remove":
                    case "rm": {
                        // TODO: move off main thread
                        return removePlayer(sender, args[1]);
                    }
                    case "add": {
                        // TODO: move off main thread
                        return addPlayer(sender, Bukkit.getPlayer(args[1]));
                    }
                    default: {
                        return false;
                    }
                }
            }
            default: {
                return false;
            }
        }
    }

    private boolean showAlive(CommandSender sender, int page) {
        List<AlivePlayer> players;

        try {
            // TODO: move off main thread
            players = db.selectAll();
        } catch (SQLException e) {
            sender.sendMessage(GENERIC_ERROR);
            log.warning(String.format("failed to list alive players: `%s`", e.getMessage()));
            return true;
        }

        // TODO: move off main thread
        fetchCastes(players);

        players = players.stream()
            .sorted(new AlivePlayerComparator(log, casteSortOrder))
            .collect(Collectors.toList());

        AliveList.make(log, players, page).sendTo(sender);

        return true;
    }

    private void fetchCastes(List<AlivePlayer> players) {
        log.info("fetching castes");

        Method getRpRace = ReflectionUtils.getRpRaceMethod(log).orElse(null);
        if (getRpRace == null) {
            return;
        }

        for (AlivePlayer player : players) {
            String caste = getCaste(log, getRpRace, player.getUsername());

            log.info("player " + player.getUsername());
            log.info("caste " + caste);

            player.setCaste(caste);
        }
    }

    private static String getCaste(Logger log, Method getRpRace, String username) {
        return ReflectionUtils.invokeSafe(log, getRpRace, username)
            .map(Object::toString)
            .filter(s -> !s.equalsIgnoreCase("NONE"))
            .orElse(null);
    }

    private boolean removePlayer(CommandSender sender, String username) {
        try {
            db.deletePlayer(username);
            sender.sendMessage(ChatColor.GREEN + "Player removed");
        } catch (SQLException e) {
            sender.sendMessage(GENERIC_ERROR);
            String message = "failed to remove player from database: `%s`";
            log.warning(String.format(message, e.getMessage()));
        }

        return true;
    }

    private boolean addPlayer(CommandSender sender, Player player) {
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found");
            return false;
        }

        try {
            db.insertPlayer(player);
            sender.sendMessage(ChatColor.GREEN + "Player added");
        } catch (SQLException e) {
            sender.sendMessage(GENERIC_ERROR);
            String message = "failed to add player to database: `%s`";
            log.warning(String.format(message, e.getMessage()));
        }

        return true;
    }
}
