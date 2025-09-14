package org.Nugetzrul3.CryptoTipper;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.Nugetzrul3.CryptoTipper.db.UserRepository;
import org.Nugetzrul3.CryptoTipper.db.WithdrawRepository;
import org.Nugetzrul3.CryptoTipper.rpcclient.Methods;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.sign.Side;
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

import java.sql.Date;
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
    private final WithdrawRepository withdrawRepository;
    private final JavaPlugin plugin;
    private final NamespacedKey ownerKey;


    public SignListener(JavaPlugin plugin) {
        this.constants = new Constants();
        this.methods = new Methods();
        this.userRepository = new UserRepository();
        this.withdrawRepository = new WithdrawRepository();
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "sign_owner");
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.isCancelled()) {
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
        Player player = event.getPlayer();

        // Check if the broken block IS a sign
        if (state instanceof Sign sign && !(state instanceof HangingSign)) {
            PersistentDataContainer dc = sign.getPersistentDataContainer();
            String owner = dc.get(ownerKey, PersistentDataType.STRING);

            if (owner != null) {
                if (!(owner.equals(player.getUniqueId().toString()))) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You can't destroy this sign!");
                    return;
                }
            }
        }

        // Check if breaking this block would destroy any protected signs
        checkForAttachedProtectedSigns(event, block, player);
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

        // Get both sides to check for commands
        SignSide frontSide = sign.getSide(Side.FRONT);
        SignSide backSide = sign.getSide(Side.BACK);

        String[] frontLines = frontSide.getLines();
        String[] backLines = backSide.getLines();

        String owner = sign.getPersistentDataContainer()
            .get(ownerKey, PersistentDataType.STRING);

        boolean currentSideHasCommand = lines.length > 0 && lines[0] != null &&
            commands_arr.contains(lines[0].replace("§a", ""));

        boolean otherSideHasCommand;
        if (signSide == frontSide) {
            otherSideHasCommand = backLines.length > 0 && backLines[0] != null &&
                commands_arr.contains(backLines[0].replace("§a", ""));
        } else {
            otherSideHasCommand = frontLines.length > 0 && frontLines[0] != null &&
                commands_arr.contains(frontLines[0].replace("§a", ""));
        }

        if (owner != null && otherSideHasCommand && !currentSideHasCommand) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "This sign has a command on the other side! Walk around to use it.");
            return;
        }

        if (owner != null && currentSideHasCommand) {
            event.setCancelled(true);
            String command = lines[0].replace("§a", "");
            processSignCommand(sign, player, command, lines, owner);
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

                String payAmount =  lines[1];

                if (!Utils.isDouble(payAmount) || payAmount.isBlank()) {
                    player.sendMessage(ChatColor.RED + "Amount is invalid!");
                    sign.getBlock().breakNaturally();
                    return;
                }

                if (isOwner) {
                    player.sendMessage(ChatColor.RED + "You can't pay yourself!");
                    return;
                }

                handlePaymentCommand(player, payAmount, ownerUUID);

                break;

            case "/bal":
                // Anyone can check their own balance
                player.sendMessage(ChatColor.AQUA + "Checking balance...");
                handleBalanceCommand(player);
                break;

            case "/qwithdraw":
                if (lines.length < 2) {
                    player.sendMessage(ChatColor.RED + "This sign command is invalid! Try again");
                    player.sendMessage("Usage: First line should be /qwithdraw, second line should be an amount (1.5, 5, etc)");
                    sign.getBlock().breakNaturally();
                    return;
                }

                String withdrawAmount =  lines[1];

                if (!Utils.isDouble(withdrawAmount) || withdrawAmount.isBlank()) {
                    player.sendMessage(ChatColor.RED + "Amount is invalid!");
                    sign.getBlock().breakNaturally();
                    return;
                }

                // Only owner can withdraw
                if (!isOwner) {
                    player.sendMessage(ChatColor.RED + "Only the sign owner can withdraw!");
                    return;
                }

                handleQuickWithdrawCommand(player, withdrawAmount);
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
                ChatColor.AQUA + ChatColor.BOLD.toString() + "Your unconfirmed balance: " + String.format("%.8f", response.get("unconfBal").getAsDouble() - response.get("confBal").getAsDouble()) + " " + constants.ticker + "\n"
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
                    ChatColor.RED + "Error sending payment! Contact admins and show them this: \n" +
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
                        ChatColor.GREEN + "Success! Paid " + receiver.getName() + " " + amount + " " + constants.ticker + "\n"
                    );

                    if (receiver.isOnline()) {
                        Player receiverPlayer = receiver.getPlayer();
                        if (receiverPlayer != null) {
                            receiverPlayer.sendMessage(
                                ChatColor.GREEN + player.getName() + " has paid " + amount + " " + constants.ticker + "\n"
                            );
                        }
                    }

                });
            });
        });
    }

    private void handleQuickWithdrawCommand(Player player, String amount) {
        double reqAmount = Double.parseDouble(amount);

        if (reqAmount < this.constants.min_withdraw) {
            player.sendMessage(ChatColor.RED + "The minimum withdrawal amount is " + this.constants.min_withdraw + " " + this.constants.ticker + "!");
            return;
        }

        this.withdrawRepository.getCurrentWithdrawAmount(
            player.getUniqueId().toString(),
            new Date(System.currentTimeMillis())
        ).thenAccept(currWithdrawAmount -> {
            if (currWithdrawAmount >= this.constants.withdraw_limit) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                    ChatColor.RED + "You have hit your withdrawal limit of " + this.constants.withdraw_limit + " " + this.constants.ticker + "\n"
                        + ChatColor.GREEN + "Please wait till the next day to withdraw more :)"
                ));
                return;
            }

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

                if (Double.parseDouble(balResponse.get("confBal").getAsString()) < reqAmount) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                        ChatColor.RED + "That amount exceeds how much " + this.constants.ticker + " you have\n"
                            + ChatColor.WHITE + "You're current balance: " + ChatColor.GREEN + balResponse.get("confBal").getAsString()
                            + " " + this.constants.ticker
                    ));
                    return;
                }
                double sendAmount = reqAmount - this.constants.withdraw_fee;

                this.userRepository.getUserByUuid(
                    player.getUniqueId().toString()
                ).thenAccept(user -> {
                    if (user.withdraw_addr() == null || user.withdraw_addr().isEmpty() || user.withdraw_addr().equals("null")) {
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.RED + "You don't have a quick withdraw address! Please use /withdraw command first to set one!"));
                        return;
                    }

                    this.methods.withdraw(
                        user.withdraw_addr(),
                        sendAmount,
                        player.getUniqueId().toString()
                    ).thenAccept(response -> {
                        if (!(response.get("error") instanceof JsonNull)) {
                            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                                ChatColor.RED + "Error processing withdrawal! Contact admins and show them this: \n" +
                                    "Error: " + response.get("error").toString()
                            ));
                            return;
                        }

                        JsonObject withdrawResponse = response.get("result").getAsJsonObject();
                        String txid = withdrawResponse.get("txid").getAsString();

                        this.withdrawRepository.insertWithdraw(
                            txid,
                            user.withdraw_addr(),
                            reqAmount,
                            user.id()
                        );

                        TextComponent tc = new TextComponent();

                        tc.setText(
                            ChatColor.GREEN + "TXID:  " + ChatColor.UNDERLINE + txid
                        );
                        tc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, constants.explorer + txid));
                        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new Text(ChatColor.GRAY + "Click to copy to open transaction")));

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(
                                ChatColor.GREEN + "Success! Withdrew " + amount + " " + constants.ticker + "\n"
                                    + ChatColor.GRAY + "Note: Transaction may not be reflected on explorer yet. But \n"
                                    + "rest assured, you're " + constants.ticker + " has been sent"
                            );
                            player.spigot().sendMessage(tc);
                        });

                    });
                });
            });
        });
    }

    // Helper methods
    private void checkForAttachedProtectedSigns(BlockBreakEvent event, Block supportingBlock, Player player) {
        // Get all blocks that might have signs attached to this block
        Block[] adjacentBlocks = {
            supportingBlock.getRelative(BlockFace.NORTH),
            supportingBlock.getRelative(BlockFace.SOUTH),
            supportingBlock.getRelative(BlockFace.EAST),
            supportingBlock.getRelative(BlockFace.WEST),
            supportingBlock.getRelative(BlockFace.UP)
        };

        for (Block adjacentBlock : adjacentBlocks) {
            BlockState adjacentState = adjacentBlock.getState();

            if (adjacentState instanceof Sign sign && !(adjacentState instanceof HangingSign)) {
                if (isSignAttachedToBlock(sign, supportingBlock)) {
                    PersistentDataContainer dc = sign.getPersistentDataContainer();
                    String owner = dc.get(ownerKey, PersistentDataType.STRING);

                    if (owner != null && !owner.equals(player.getUniqueId().toString())) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You can't destroy this block! It would destroy a protected sign.");
                        return;
                    }
                }
            }
        }
    }

    private boolean isSignAttachedToBlock(Sign sign, Block supportingBlock) {
        Block signBlock = sign.getBlock();
        Material signType = signBlock.getType();

        if (signType == Material.OAK_SIGN || signType == Material.BIRCH_SIGN ||
            signType == Material.SPRUCE_SIGN || signType == Material.JUNGLE_SIGN ||
            signType == Material.ACACIA_SIGN || signType == Material.DARK_OAK_SIGN ||
            signType == Material.CRIMSON_SIGN || signType == Material.WARPED_SIGN ||
            signType == Material.MANGROVE_SIGN || signType == Material.BAMBOO_SIGN ||
            signType == Material.CHERRY_SIGN) {

            return signBlock.getRelative(BlockFace.DOWN).equals(supportingBlock);
        }

        if (signType.toString().contains("WALL_SIGN")) {
            org.bukkit.block.data.type.WallSign wallSign = (org.bukkit.block.data.type.WallSign) signBlock.getBlockData();
            BlockFace attachedFace = wallSign.getFacing().getOppositeFace();
            return signBlock.getRelative(attachedFace).equals(supportingBlock);
        }

        return false;
    }

}