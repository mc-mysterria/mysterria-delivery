package net.mysterria.delivery.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mysterria.delivery.MysterriaDelivery;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DeliveryCommand implements CommandExecutor, TabCompleter {
    
    private final MysterriaDelivery plugin;
    
    public DeliveryCommand(MysterriaDelivery plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mysterria.delivery.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /delivery reload", NamedTextColor.YELLOW));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage(Component.text("MysterriaDelivery configuration reloaded!", NamedTextColor.GREEN));
            return true;
        }
        
        sender.sendMessage(Component.text("Unknown subcommand. Use: /delivery reload", NamedTextColor.RED));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}