package tv.twitch.moonmoon.slashalive;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AliveCommand implements CommandExecutor {

    private static final String PAGE_HEADER = ChatColor.BLUE
        + "================= "
        + ChatColor.YELLOW
        + "Page %d/%d "
        + ChatColor.BLUE
        + "=================";

    private static final String GENERIC_ERROR =
        ChatColor.RED + "An unexpected error occurred. See console for details.";

    private final AliveDb db;
    private final Logger log;

    public AliveCommand(AliveDb db, Logger log) {
        this.db = Objects.requireNonNull(db);
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (args.length) {
            case 0: {
                return showAlive(sender, 1);
            }
            case 1: {
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
        List<AlivePlayer> players;

        try {
            players = db.selectAll().stream()
                .sorted(Comparator.comparing(AlivePlayer::getUsername))
                .collect(Collectors.toList());
        } catch (SQLException e) {
            sender.sendMessage(GENERIC_ERROR);
            log.warning(String.format("failed to list alive players: `%s`", e.getMessage()));
            return true;
        }

        String text = makeAliveMessage(players);
        ChatPaginator.ChatPage cp = ChatPaginator.paginate(text, page);
        int curPage = cp.getPageNumber();
        int maxPage = cp.getTotalPages();

        sender.sendMessage(String.format(PAGE_HEADER, curPage, maxPage));
        sender.sendMessage(cp.getLines());
        sender.spigot().sendMessage(makeFooter(curPage, maxPage));

        return true;
    }

    private TextComponent makeFooter(int curPage, int maxPage) {
        TextComponent footer = new TextComponent("==================== ");
        TextComponent prev = new TextComponent("\u226A");
        TextComponent next = new TextComponent("\u226B");
        TextComponent rightPad = new TextComponent(" ====================");

        footer.setColor(net.md_5.bungee.api.ChatColor.BLUE);
        prev.setBold(true);
        next.setBold(true);
        rightPad.setColor(net.md_5.bungee.api.ChatColor.BLUE);

        if (curPage > 1) {
            prev.setColor(net.md_5.bungee.api.ChatColor.GREEN);
            String cmd = "/alive " + (curPage - 1);
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
        } else {
            prev.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        }

        if (curPage < maxPage) {
            next.setColor(net.md_5.bungee.api.ChatColor.GREEN);
            String cmd = "/alive " + (curPage + 1);
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
        } else {
            next.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        }

        footer.addExtra(prev);
        footer.addExtra(" ");
        footer.addExtra(next);
        footer.addExtra(rightPad);

        return footer;
    }

    private String makeAliveMessage(Collection<AlivePlayer> players) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("com.Alvaeron.api.RPEngineAPI");
        } catch (ClassNotFoundException ignored) {
            log.warning("RPEngine not found, ignoring");
        }

        final Class<?> rpClass = clazz;

        return players.stream()
            .map(p -> {
                String username = p.getUsername();
                UUID uuid = UUID.fromString(p.getUUID());
                Player player = Bukkit.getPlayer(username);

                if (player == null) {
                    Bukkit.getOfflinePlayer(uuid);
                }

                if (player == null) {
                    return ChatColor.GRAY + p.getUsername();
                }

                String display;
                if (rpClass != null) {
                    try {
                        Method method = rpClass.getDeclaredMethod("getRpName", String.class);
                        String rpName = (String) method.invoke(null, username);

                        if (rpName == null || rpName.equalsIgnoreCase("NONE")) {
                            display = username;
                        } else {
                            display = String.format("%s (%s)", rpName, username);
                        }
                    } catch (IllegalAccessException
                            | InvocationTargetException
                            | NoSuchMethodException e) {
                        String message = "failed to invoke getRpName on RPEngine, " +
                            "falling back to username: `%s`";
                        log.warning(String.format(message, e.getMessage()));
                        display = username;
                    }
                } else {
                    display = username;
                }

                display = (player.isOnline() ? ChatColor.GREEN : ChatColor.GRAY) + display;

                return display;
            })
            .collect(Collectors.joining(ChatColor.WHITE + ", "));
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
