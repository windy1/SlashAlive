package tv.twitch.moonmoon.slashalive;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AliveList {

    private static final String PAGE_HEADER = ChatColor.BLUE
        + "================= "
        + ChatColor.YELLOW
        + "Page %d/%d "
        + ChatColor.BLUE
        + "=================";

    private final String header;
    private final String[] content;
    private final TextComponent footer;

    private AliveList(String header, String[] content, TextComponent footer) {
        this.header = Objects.requireNonNull(header);
        this.content = Objects.requireNonNull(content);
        this.footer = Objects.requireNonNull(footer);
    }

    public void sendTo(CommandSender sender) {
        sender.sendMessage(header);
        sender.sendMessage(content);
        sender.spigot().sendMessage(footer);
    }

    public static AliveList make(Logger log, List<AlivePlayer> players, int page) {
        String text = makeAliveMessage(log, players);
        ChatPaginator.ChatPage cp = ChatPaginator.paginate(text, page);
        int curPage = cp.getPageNumber();
        int maxPage = cp.getTotalPages();

        return new AliveList(
            String.format(PAGE_HEADER, curPage, maxPage),
            cp.getLines(),
            makeFooter(curPage, maxPage)
        );
    }

    private static TextComponent makeFooter(int curPage, int maxPage) {
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

    private static String makeAliveMessage(Logger log, Collection<AlivePlayer> players) {
        Method getRpName = ReflectionUtils.getRpNameMethod(log).orElse(null);

        return players.stream()
            .map(p -> getDisplayName(log, p, getRpName))
            .collect(Collectors.joining(ChatColor.WHITE + ", "));
    }

    private static String getDisplayName(Logger log, AlivePlayer p, Method getRpName) {
        String username = p.getUsername();
        Player player = Bukkit.getPlayer(username);

        if (player == null) {
            return ChatColor.GRAY + p.getUsername();
        }

        // TODO: getting RP info of OfflinePlayers is currently bugged in RPEngine

        String display = Optional.ofNullable(getRpName)
            .flatMap(m -> ReflectionUtils.invokeSafe(log, m, username))
            .filter(o -> !((String) o).equalsIgnoreCase("NONE"))
            .map(o -> String.format("%s (%s)", o, username))
            .orElse(username);

        display = (player.isOnline() ? ChatColor.GREEN : ChatColor.GRAY) + display;

        return display;
    }
}
