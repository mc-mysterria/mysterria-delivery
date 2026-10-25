package net.mysterria.delivery.command.admin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mysterria.delivery.MysterriaDelivery;
import net.mysterria.delivery.domain.delivery.model.QueuedDelivery;
import org.bukkit.command.CommandSender;

import java.util.List;

public class DeliveryCommand {

    private final MysterriaDelivery plugin;

    public DeliveryCommand(MysterriaDelivery plugin) {
        this.plugin = plugin;
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                Commands.literal("delivery")
                        .requires(source -> source.getSender().hasPermission("mysterria.delivery.admin"))
                        .then(Commands.literal("reload").executes(this::reload))
                        .then(Commands.literal("status").executes(this::status))
                        .build(),
                "MysterriaDelivery administration"
        ));
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        plugin.reload();
        context.getSource().getSender().sendMessage(Component.text("MysterriaDelivery reloaded.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int status(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        List<QueuedDelivery> failed = plugin.getDeliveryService().failedDeliveries();
        sender.sendMessage(Component.text("Queued deliveries: " + plugin.getDeliveryService().queuedCount(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Failed deliveries: " + failed.size(), failed.isEmpty() ? NamedTextColor.GREEN : NamedTextColor.RED));
        failed.forEach(delivery -> sender.sendMessage(Component.text(
                "- " + delivery.getPurchaseId() + " for " + delivery.getPlayerName() + ": " + delivery.getFailureReason(), NamedTextColor.GRAY)));
        return Command.SINGLE_SUCCESS;
    }
}
