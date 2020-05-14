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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
                        return removePlayer(sender, args[1]);
                    }
                    case "add": {
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
        db.selectAllAsync(r -> {
            Optional<String> err = r.getError();
            if (err.isPresent()) {
                sender.sendMessage(GENERIC_ERROR);
                log.warning(String.format("failed to list alive players: `%s`", err.get()));
            } else {
                List<AlivePlayer> players = r.getResult()
                    .orElseThrow(() -> new IllegalStateException("unexpected null list"));
                onSelectAlive(players, sender, page);
            }
        });

        return true;
    }

    private void onSelectAlive(List<AlivePlayer> players, CommandSender sender, int page) {
        // TODO: move off main thread
//        fetchCastes(players);

        players = players.stream()
            .sorted(new AlivePlayerComparator(log, casteSortOrder))
            .collect(Collectors.toList());

        AliveList.make(log, players, page).sendTo(sender);
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
        db.deletePlayerAsync(username, r -> {
            Optional<String> err = r.getError();
            if (err.isPresent()) {
                sender.sendMessage(GENERIC_ERROR);
                String message = "failed to remove player from database: `%s`";
                log.warning(String.format(message, err.get()));
            } else {
                sender.sendMessage(ChatColor.GREEN + "Player removed");
            }
        });

        return true;
    }

    private boolean addPlayer(CommandSender sender, Player player) {
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found");
            return false;
        }

        db.insertPlayerAsync(player, r -> {
            Optional<String> err = r.getError();
            if (err.isPresent()) {
                sender.sendMessage(GENERIC_ERROR);
                String message = "failed to add player to database: `%s`";
                log.warning(String.format(message, err.get()));
            } else {
                sender.sendMessage(ChatColor.GREEN + "Player added");
            }
        });

        return true;
    }
}
