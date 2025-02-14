/*
 * Copyright (C) YoloMC, All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential.
 */

package com.minecraft.core.proxy.command;

import com.minecraft.core.Constants;
import com.minecraft.core.account.Account;
import com.minecraft.core.account.AccountStorage;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Completer;
import com.minecraft.core.command.command.Context;
import com.minecraft.core.command.platform.Platform;
import com.minecraft.core.enums.Rank;
import com.minecraft.core.proxy.ProxyGame;
import com.minecraft.core.proxy.util.command.ProxyInterface;
import com.minecraft.core.proxy.util.server.ServerAPI;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GoCommand implements Listener, ProxyInterface {

    @Command(name = "go", rank = Rank.STREAMER_PLUS, platform = Platform.PLAYER, usage = "go <target>")
    public void handleCommand(Context<ProxiedPlayer> context, String username) {

        ProxiedPlayer sender = context.getSender();
        Account account = Account.fetch(sender.getUniqueId());

        async(() -> search(context, username, target -> {

            ProxiedPlayer player = BungeeCord.getInstance().getPlayer(target.getUniqueId());

            if (player == null) {
                context.info("target.not_found");
                return;
            }

            if (ServerAPI.getInstance().hasPendingConnection(player)) {
                context.info("command.go.target.pending_connection");
                return;
            }

            if (target.getRank().getCategory().getImportance() > account.getRank().getCategory().getImportance()) {
                context.info("target.not_found");
                return;
            }

            if (account.hasProperty("command.go.platform")) {
                context.info("command.go.pending_teleport");
                return;
            }

            try (Jedis jedis = Constants.getRedis().getResource()) {
                jedis.setex("route:" + context.getUniqueId(), 10, target.getUniqueId().toString());
            }

            if (sender.getServer().getInfo() == player.getServer().getInfo())
                sender.chat("/tp " + target.getUniqueId().toString());
            else {
                account.setProperty("command.go.platform", target.getUniqueId());
                sender.connect(player.getServer().getInfo());
            }
        }));
    }

    private ProxiedPlayer fetch(String text) {
        Account account;

        if (Constants.isUniqueId(text))
            account = Account.fetch(UUID.fromString(text));
        else
            account = AccountStorage.getAccountByName(text, false);
        if (account != null)
            return ProxyServer.getInstance().getPlayer(account.getUniqueId());
        return null;
    }


    @Completer(name = "go")
    public List<String> handleComplete(Context<CommandSender> context) {
        if (context.argsCount() == 1)
            return getOnlineNicknames(context);
        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerConnected(ServerConnectedEvent event) {
        ProxiedPlayer proxiedPlayer = event.getPlayer();
        Account account = Account.fetch(proxiedPlayer.getUniqueId());

        if (account == null)
            return;

        if (account.hasProperty("command.go.platform")) {
            BungeeCord.getInstance().getScheduler().schedule(ProxyGame.getInstance(), () -> {
                proxiedPlayer.chat("/tp " + account.getProperty("command.go.platform").getAs(UUID.class).toString());
                account.removeProperty("command.go.platform");
            }, 200, TimeUnit.MILLISECONDS);
        }
    }


}
