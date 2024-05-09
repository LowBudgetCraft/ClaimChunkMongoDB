package de.glowman554.claimchunkmongodb.commands;

import de.glowman554.claimchunkmongodb.ClaimChunkMongoDB;
import de.glowman554.claimchunkmongodb.database.MongoDataHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class CacheCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length != 1) {
            commandSender.sendMessage("Usage: /cache <subcommand>");
            return false;
        }

        MongoDataHandler dataHandler = ClaimChunkMongoDB.getInstance().getDataHandler();

        switch (strings[0]) {
            case "info":
                int sizePlayer = dataHandler.getPlayerHandler().getCache().size();
                int sizeClaim = dataHandler.getClaimHandler().getCache().size();

                commandSender.sendMessage("Player cache size is " + sizePlayer);
                commandSender.sendMessage("Claim cache size is " + sizeClaim);
                break;
            case "clear":
                dataHandler.getClaimHandler().clear();
                dataHandler.getPlayerHandler().clear();
                commandSender.sendMessage("Successfully cleared caches.");
                break;
            default:
                commandSender.sendMessage("Unknown subcommand " + strings[0]);
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1) {
            return List.of(
                    "info",
                    "clear"
            );
        }
        return null;
    }
}
