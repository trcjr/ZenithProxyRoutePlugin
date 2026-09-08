package com.trcjr.zenithroute.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.trcjr.zenithroute.RouteConfig.RouteStep;
import com.trcjr.zenithroute.module.OrderedRouteModule;
import com.zenith.command.api.*;
import com.zenith.discord.Embed;
import com.zenith.mc.block.BlockPos;

import static com.trcjr.zenithroute.OrderedRoutesPlugin.CONFIG;
import static com.zenith.command.brigadier.BlockPosArgument.blockPos;
import static com.zenith.command.brigadier.BlockPosArgument.getBlockPos;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class RouteCommand extends Command {
    private final OrderedRouteModule module;

    public RouteCommand(OrderedRouteModule module) { this.module = module; }

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("route")
            .category(CommandCategory.MODULE)
            .description("Single-bot ordered route runner with explicit portal transitions")
            .usageLines(
                "move <name> <dimension> <x> <y> <z>",
                "portal <name> <fromDimension> <targetDimension> <x> <y> <z>",
                "radius <blocks>",
                "list", "validate", "start", "status", "pause", "resume", "stop", "clear"
            )
            .aliases("rt")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("route")
            .then(literal("move")
                .then(argument("name", wordWithChars())
                .then(argument("dimension", wordWithChars())
                .then(argument("pos", blockPos()).executes((IExecutes<CommandContext>) c -> guarded(c, () -> {
                    BlockPos pos = getBlockPos(c, "pos");
                    module.addMove(getString(c, "name"), getString(c, "dimension"), pos.x(), pos.y(), pos.z());
                    c.getSource().getEmbed().title("Move Step Added").description(CONFIG.steps.getLast().toString());
                }))))))
            .then(literal("portal")
                .then(argument("name", wordWithChars())
                .then(argument("fromDimension", wordWithChars())
                .then(argument("targetDimension", wordWithChars())
                .then(argument("pos", blockPos()).executes((IExecutes<CommandContext>) c -> guarded(c, () -> {
                    BlockPos pos = getBlockPos(c, "pos");
                    module.addPortal(getString(c, "name"), getString(c, "fromDimension"),
                        getString(c, "targetDimension"), pos.x(), pos.y(), pos.z());
                    c.getSource().getEmbed().title("Portal Step Added").description(CONFIG.steps.getLast().toString());
                })))))))
            .then(literal("list").executes((IExecutes<CommandContext>) c -> guarded(c, () ->
                c.getSource().getEmbed().title("Ordered Route").description(routeList()))))
            .then(literal("radius").then(argument("blocks", integer(1, 8))
                .executes((IExecutes<CommandContext>) c -> guarded(c, () -> {
                    module.setMoveArrivalRadius(getInteger(c, "blocks"));
                    c.getSource().getEmbed().title("Move Arrival Radius Set")
                        .description(CONFIG.moveArrivalRadius + " blocks");
                }))))
            .then(literal("validate").executes((IExecutes<CommandContext>) c -> guarded(c, () ->
                c.getSource().getEmbed().title("Route Validation").description(module.validateConfiguredRoute()))))
            .then(literal("start").executes((IExecutes<CommandContext>) c -> guarded(c, () -> {
                module.startRoute();
                c.getSource().getEmbed().title("Ordered Route Started").description(module.describeStatus());
            })))
            .then(literal("status").executes((IExecutes<CommandContext>) c -> guarded(c, () ->
                c.getSource().getEmbed().title("Ordered Route Status").description(module.describeStatus()))))
            .then(literal("pause").executes((IExecutes<CommandContext>) c -> guarded(c, () -> {
                module.pauseRoute("paused by command");
                c.getSource().getEmbed().title("Ordered Route Paused");
            })))
            .then(literal("resume").executes((IExecutes<CommandContext>) c -> guarded(c, () -> {
                module.resumeRoute();
                c.getSource().getEmbed().title("Ordered Route Resumed").description(module.describeStatus());
            })))
            .then(literal("stop").executes((IExecutes<CommandContext>) c -> guarded(c, () -> {
                module.stopRoute();
                c.getSource().getEmbed().title("Ordered Route Stopped");
            })))
            .then(literal("clear").executes((IExecutes<CommandContext>) c -> guarded(c, () -> {
                module.clearRoute();
                c.getSource().getEmbed().title("Ordered Route Cleared");
            })));
    }

    @Override
    public void defaultEmbed(Embed embed) { embed.primaryColor(); }

    private void guarded(com.mojang.brigadier.context.CommandContext<CommandContext> context, Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException | IllegalArgumentException e) {
            context.getSource().getEmbed().title("Route Error").description(e.getMessage()).errorColor();
        }
    }

    private String routeList() {
        if (CONFIG.steps.isEmpty()) return "No steps configured; move radius=" + CONFIG.moveArrivalRadius;
        StringBuilder result = new StringBuilder();
        result.append("Move radius: ").append(CONFIG.moveArrivalRadius).append(" blocks\n");
        for (int i = 0; i < CONFIG.steps.size(); i++) {
            RouteStep step = CONFIG.steps.get(i);
            result.append(i + 1).append(". ").append(step);
            if (i == CONFIG.currentIndex && CONFIG.enabled) result.append(" <- current");
            if (i + 1 < CONFIG.steps.size()) result.append('\n');
        }
        return result.toString();
    }
}
