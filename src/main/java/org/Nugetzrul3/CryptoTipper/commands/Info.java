package org.Nugetzrul3.CryptoTipper.commands;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.Nugetzrul3.CryptoTipper.CommandWrapper;
import org.Nugetzrul3.CryptoTipper.Constants;
import org.Nugetzrul3.CryptoTipper.Utils;
import org.Nugetzrul3.CryptoTipper.rpcclient.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Info implements CommandExecutor {
    private final Methods methods;
    private final JavaPlugin plugin;
    private final Constants constants;

    public Info(JavaPlugin plugin) {
        if (plugin.getDescription().getCommands().containsKey("info")) {
            plugin.getCommand("info").setExecutor(new CommandWrapper(this, plugin));
            this.plugin = plugin;
            this.constants = new Constants();
            this.methods = new Methods();
        } else {
            throw new Error("Info command not found!");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        player.sendMessage(ChatColor.AQUA + ChatColor.BOLD.toString() + "Getting blockchain info...");
        this.methods.getBlockchainInfo()
                .thenAccept(response -> {
                    if (!(response.get("error") instanceof JsonNull)) {
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                                ChatColor.RED + "Error getting blockchain info! Contact admins and show them this: \n" +
                                        "Error: " + response.get("error").toString()
                        ));

                        return;
                    }

                    JsonObject resultJson = response.getAsJsonObject("result");
                    int blockHeight = resultJson.get("blocks").getAsInt();
                    JsonObject supplyAndRewardJson = Utils.calculateSupplyAndReward(blockHeight);
                    // Run on the main server thread
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                            ChatColor.AQUA + ChatColor.BOLD.toString() + constants.ticker + " Blockchain info: \n" +
                            ChatColor.GREEN + "Block height: " + blockHeight + "\n" +
                            ChatColor.GREEN + "Blockchain hashps: " + Utils.getHashFormat(resultJson.get("hashps").getAsFloat()) + "\n" +
                            ChatColor.GREEN + "Blockchain difficulty: " + resultJson.get("difficulty") + "\n" +
                            ChatColor.GREEN + "Current block reward: " + supplyAndRewardJson.get("reward").getAsString() + "\n" +
                            ChatColor.GREEN + "Number of halvings: " + supplyAndRewardJson.get("halvings") + "\n" +
                            ChatColor.GREEN + "Current circulating supply: " + supplyAndRewardJson.get("supply").getAsString()
                    ));
                });

        return false;
    }
}
