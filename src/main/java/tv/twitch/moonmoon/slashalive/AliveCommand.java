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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AliveCommand implements CommandExecutor {

    private static final String PAGE_HEADER = ChatColor.BLUE
        + "================= "
        + ChatColor.YELLOW
        + "Page %d/%d  "
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
                        return addPlayer(sender, args[1]);
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
        try {
            List<String> usernames = db.selectAllUsernames();
            Collections.sort(usernames);

            String text = makeAliveMessage(usernames);

            ChatPaginator.ChatPage cp = ChatPaginator.paginate(text, page);

            String header = String.format(PAGE_HEADER, cp.getPageNumber(), cp.getTotalPages());
            sender.sendMessage(header);

            sender.sendMessage(ChatPaginator.paginate(text, page).getLines());

            sender.spigot().sendMessage(makeFooter(cp.getPageNumber(), cp.getTotalPages()));
        } catch (SQLException e) {
            sender.sendMessage(GENERIC_ERROR);
            log.warning(String.format("failed to list alive players: `%s`", e.getMessage()));
        }
        return true;
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

    private boolean addPlayer(CommandSender sender, String username) {
        if (Bukkit.getPlayer(username) == null) {
            sender.sendMessage(ChatColor.RED + "Player does not exist");
            return false;
        }

        try {
            db.insertPlayer(username);
            sender.sendMessage(ChatColor.GREEN + "Player added");
        } catch (SQLException e) {
            sender.sendMessage(GENERIC_ERROR);
            String message = "failed to add player to database: `%s`";
            log.warning(String.format(message, e.getMessage()));
        }

        return true;
    }

    private TextComponent makeFooter(int curPage, int maxPage) {
        TextComponent footer = new TextComponent("==================== ");
        footer.setColor(net.md_5.bungee.api.ChatColor.BLUE);

        TextComponent prev = new TextComponent("\u226A");
        prev.setBold(true);

        if (curPage > 0) {
            prev.setColor(net.md_5.bungee.api.ChatColor.GREEN);
            String cmd = "/alive " + (curPage - 1);
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
        } else {
            prev.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        }

        footer.addExtra(prev);
        footer.addExtra(" ");

        TextComponent next = new TextComponent("\u226B");
        next.setBold(true);

        if (curPage < maxPage) {
            next.setColor(net.md_5.bungee.api.ChatColor.GREEN);
            String cmd = "/alive " + (curPage + 1);
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
        } else {
            next.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        }

        footer.addExtra(next);

        TextComponent rightPad = new TextComponent(" ====================");
        rightPad.setColor(net.md_5.bungee.api.ChatColor.BLUE);

        footer.addExtra(rightPad);

        return footer;
    }

    private String makeAliveMessage(Collection<String> usernames) {
        return usernames.stream()
            .map(u -> {
                Player player = Bukkit.getPlayer(u);
                if (player != null && player.isOnline()) {
                    return ChatColor.GREEN + u;
                } else {
                    return ChatColor.GRAY + u;
                }
            })
            .collect(Collectors.joining(ChatColor.WHITE + ", "));
    }
}
