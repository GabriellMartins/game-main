/*
 * Copyright (C) YoloMC, All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential.
 */

package com.minecraft.hub.command;

import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.command.Context;
import com.minecraft.core.command.platform.Platform;
import com.minecraft.core.enums.Rank;
import com.minecraft.hub.user.User;
import org.bukkit.entity.Player;

public class BuildCommand {

    @Command(name = "build", platform = Platform.PLAYER, rank = Rank.PRIMARY_MOD)
    public void handleCommand(Context<Player> context) {

        Player sender = context.getSender();
        User user = User.fetch(sender.getUniqueId());

        boolean bool = !user.getAccount().getProperty("lobby.build", false).getAsBoolean();
        user.getAccount().setProperty("lobby.build", bool);

        context.sendMessage("§6self_build §ealterado para §b" + bool);
    }

}