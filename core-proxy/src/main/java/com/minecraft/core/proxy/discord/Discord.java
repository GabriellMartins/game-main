/*
 * Copyright (C) YoloMC, All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential.
 */

package com.minecraft.core.proxy.discord;

import com.minecraft.core.Constants;
import com.minecraft.core.proxy.discord.listener.DiscordListener;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

@Getter
public class Discord {

    private DiscordListener discordListener;
    private JDA JDA;

    public Discord start(String token) {
        try {
            JDA = JDABuilder.createDefault(token).setAutoReconnect(true).build();
            JDA.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            JDA.getPresence().setPresence(Activity.playing(Constants.SERVER_STORE), true);
            JDA.addEventListener(this.discordListener = new DiscordListener(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public void shutdown() {
        JDA.shutdown();
    }
}
