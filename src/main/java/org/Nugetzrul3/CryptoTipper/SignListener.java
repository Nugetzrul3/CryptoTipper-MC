package org.Nugetzrul3.CryptoTipper;

import com.google.gson.JsonNull;
import org.Nugetzrul3.CryptoTipper.db.UserRepository;
import org.Nugetzrul3.CryptoTipper.rpcclient.Methods;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.HangingSign;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class SignListener implements Listener {
    private final List<String> commands_arr = List.of(new String[]{
        "/pay",
        "/qwithdraw",
        "/bal"
    });
    private final Constants constants;
    private final Methods methods;
    private final UserRepository userRepository;
    private final JavaPlugin plugin;
    private final NamespacedKey ownerKey;


    public SignListener(JavaPlugin plugin) {
        this.constants = new Constants();
        this.methods = new Methods();
        this.userRepository = new UserRepository();
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "sign_owner");
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.isCancelled()) {
            event.getPlayer().sendMessage("Sign change cancelled");
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        BlockState blockState = block.getState();

        if (blockState instanceof HangingSign) return;

        String command = event.getLine(0);

        if (command == null) return;
        if (command.contains("/") && this.commands_arr.contains(command)) {
            if (blockState instanceof Sign sign) {
                PersistentDataContainer dc = sign.getPersistentDataContainer();
                String existingOwner = dc.get(ownerKey, PersistentDataType.STRING);

                if (existingOwner != null) {
                    if (!(existingOwner.equals(player.getUniqueId().toString()))) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You can't edit this sign! It belongs to someone else");
                        return;
                    }

                    event.setLine(0, "§a" + command);
                } else {
                    dc.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
                    sign.update();
                    event.setLine(0, "§a" + command);
                    player.sendMessage(ChatColor.GREEN + "Sign registered! You are now the owner of this sign.");
                }

                // Update state of player
                this.userRepository.upsertUser(
                    player.getUniqueId().toString(),
                    player.getName()
                );

            }
        }

    }

    @EventHandler
    public void onSignDestroy(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockState state = block.getState();

        if (!(state instanceof Sign sign) || state instanceof HangingSign) {
            return;
        }

        Player player = event.getPlayer();
        PersistentDataContainer dc = sign.getPersistentDataContainer();
        String owner = dc.get(ownerKey, PersistentDataType.STRING);

        if (owner != null) {
            if (!(owner.equals(player.getUniqueId().toString()))) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You can't destroy this sign!");
                return;
            }
        }

        player.sendMessage(ChatColor.GREEN + "Sign destroyed!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() == Material.AIR) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        BlockState state = block.getState();

        if (!(state instanceof Sign sign) || state instanceof HangingSign) {
            return;
        }

        // Update state of player
        this.userRepository.upsertUser(
            player.getUniqueId().toString(),
            player.getName()
        );

        SignSide signSide = sign.getTargetSide(player);
        String[] lines = signSide.getLines();

        String owner = sign.getPersistentDataContainer()
            .get(ownerKey, PersistentDataType.STRING);

        if (owner != null && lines.length > 0 && lines[0] != null &&
            commands_arr.contains(lines[0].replace("§a", ""))) {

            event.setCancelled(true);

            String command = lines[0].replace("§a", "");
            processSignCommand(sign, player, command, lines, owner);
        } else {
            player.sendMessage("Heyyy!!! " + String.join(",", lines));
        }
    }

    private void processSignCommand(Sign sign, Player player, String command, String[] lines, String ownerUUID) {
        boolean isOwner = player.getUniqueId().toString().equals(ownerUUID);

        switch (command) {
            case "/pay":
                if (lines.length < 2) {
                    player.sendMessage(ChatColor.RED + "This sign command is invalid! Try again");
                    player.sendMessage("Usage: First line should be /pay, second line should be an amount (1.5, 5, etc)");
                    sign.getBlock().breakNaturally();
                    return;
                }

                String amount =  lines[1];

                if (!Utils.isDouble(amount) || amount.isBlank()) {
                    player.sendMessage(ChatColor.RED + "Amount is invalid!");
                    sign.getBlock().breakNaturally();
                    return;
                }

                if (isOwner) {
                    player.sendMessage("§eYou can't pay yourself!");
                    return;
                }

                handlePaymentCommand(player, amount, ownerUUID);

                break;

            case "/bal":
                // Anyone can check their own balance
                player.sendMessage(ChatColor.AQUA + "Checking balance...");
                handleBalanceCommand(player);
                break;

            case "/qwithdraw":
                // Only owner can withdraw
                if (!isOwner) {
                    player.sendMessage("§cOnly the sign owner can withdraw!");
                    return;
                }
                player.sendMessage("Quick withdraw");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown command: " + command);
        }
    }

    private void handleBalanceCommand(Player player) {
        String account = player.getUniqueId().toString();

        this.methods.getUserBalance(
            account
        ).thenAccept(response -> {
            if (!(response.get("error") instanceof JsonNull)) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                    ChatColor.RED + "Error getting balance! Contact admins and show them this: \n" +
                        "Error: " + response.get("error").toString()
                ));

                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                ChatColor.AQUA + ChatColor.BOLD.toString() + "Your unconfirmed balance: " + response.get("unconfBal") + " " + constants.ticker + "\n"
                    + ChatColor.GREEN + ChatColor.BOLD + "Your confirmed balance: " + response.get("confBal") + " " + constants.ticker
            ));
        });

    }

    private void handlePaymentCommand(Player player, String amount, String owner) {
        this.methods.getUserBalance(
            player.getUniqueId().toString()
        ).thenAccept(balResponse -> {
            if (!(balResponse.get("error") instanceof JsonNull)) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                    ChatColor.RED + "Error processing withdrawal! Contact admins and show them this: \n" +
                        "Error: " + balResponse.get("error").toString()
                ));
                return;
            }

            if (balResponse.get("confBal").getAsDouble() < Double.parseDouble(amount)) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                    ChatColor.RED + "That amount exceeds how much " + this.constants.ticker + " you have\n"
                        + ChatColor.WHITE + "You're current balance: " + ChatColor.GREEN + balResponse.get("confBal").getAsString()
                        + " " + this.constants.ticker
                ));
                return;
            }

            this.methods.move(
                player.getUniqueId().toString(),
                owner,
                Double.parseDouble(amount)
            ).thenAccept(response -> {
                if (!(response.get("error") instanceof JsonNull)) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                        ChatColor.RED + "Error getting balance! Contact admins and show them this: \n" +
                            "Error: " + response.get("error").toString()
                    ));

                    return;
                }

                OfflinePlayer receiver = Bukkit.getOfflinePlayer(UUID.fromString(owner));

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(
                        ChatColor.GREEN + "Success! Payed " + receiver.getName() + " " + amount + " " + constants.ticker + "\n"
                    );

                    if (receiver.isOnline()) {
                        Player receiverPlayer = receiver.getPlayer();
                        receiverPlayer.sendMessage(
                            ChatColor.GREEN + player.getName() + " has paid " + amount + " " + constants.ticker + "\n"
                        );
                    }

                });
            });
        });
    }

}