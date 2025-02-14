/*
 * Copyright (C) YoloMC, All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential.
 */

package com.minecraft.core.proxy.command;

import com.minecraft.core.Constants;
import com.minecraft.core.account.Account;
import com.minecraft.core.account.AccountStorage;
import com.minecraft.core.clan.Clan;
import com.minecraft.core.clan.communication.ClanIntegrationMessage;
import com.minecraft.core.clan.invite.Invite;
import com.minecraft.core.clan.member.Member;
import com.minecraft.core.clan.member.role.Role;
import com.minecraft.core.clan.service.ClanService;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Completer;
import com.minecraft.core.command.command.Context;
import com.minecraft.core.command.platform.Platform;
import com.minecraft.core.database.enums.Columns;
import com.minecraft.core.database.redis.Redis;
import com.minecraft.core.enums.Rank;
import com.minecraft.core.proxy.util.chat.ChatType;
import com.minecraft.core.proxy.util.command.ProxyInterface;
import lombok.Getter;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClanCommand implements ProxyInterface {

    private static final ClanService clanService = Constants.getClanService();

    @Command(name = "clan", platform = Platform.PLAYER)
    public void handleCommand(Context<ProxiedPlayer> context) {

        try {
            if (context.argsCount() == 0) {
                Argument.HELP.execute(context);
            } else {

                Argument argument = Argument.fetch(context.getArg(0));

                if (argument == null) {
                    Argument.HELP.execute(context);
                    return;
                }

                if (argument.getMinimumArgs() > context.argsCount()) {
                    Argument.HELP.execute(context);
                    return;
                }
                argument.execute(context);
            }
        } catch (SQLException e) {
            context.info("unexpected_error");
            e.printStackTrace();
        }
    }

    @Command(name = "cc", platform = Platform.PLAYER)
    public void clanChatCommand(Context<ProxiedPlayer> context) {
        try {
            Argument.CHAT.execute(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Completer(name = "clan")
    public List<String> completer(Context<CommandSender> context) {
        String[] args = context.getArgs();
        if (args.length == 1) {
            List<String> stringList = new ArrayList<>();
            for (Argument arg : Argument.values()) {

                if (arg == Argument.HELP)
                    continue;

                for (String str : arg.getField()) {
                    if (startsWith(str, args[0])) {
                        stringList.add(str);
                    }
                }
            }
            return stringList;
        }
        return Collections.emptyList();
    }

    @Getter
    public enum Argument implements ProxyInterface {

        HELP(0, "help") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {
                context.sendMessage("§cUso do /clan:");
                context.sendMessage("§c * /clan ver [clan] - §eVeja as informações de um clan.");
                context.sendMessage("§c * /clan membros [clan] - §eVeja os membros de um clan.");
                context.sendMessage("§c * /clan criar <nome> <tag> - §eCrie um clan.");
                context.sendMessage("§c * /clan apagar - §eAcabe com o seu clan.");
                context.sendMessage("§c * /clan sair - §eSaia do seu clan.");
                context.sendMessage("§c * /clan convidar <alvo> - §eConvide jogadores para seu clan.");
                context.sendMessage("§c * /clan transferir <alvo> - §eTransfira a posse do clan para outro membro.");
                context.sendMessage("§c * /clan promover <alvo> - §ePromova um membro do clan.");
                context.sendMessage("§c * /clan rebaixar <alvo> - §eRebaixe um administrador do clan.");
                context.sendMessage("§c * /clan expulsar <alvo> - §eExpulse um membro do clan.");
                context.sendMessage("§c * /clan convites - §eVisualize os convites recentes.");
                context.sendMessage("§c * /clan aceitar <clan> - §eAceite um convite de clan recebido.");
                context.sendMessage("§c * /clan negar <clan> - §eNegue um convite de clan recebido.");
                context.sendMessage("§c * /clan chat - §eEntre em um bate-papo particular com seu clan.");
            }
        },

        CHAT(1, "chat") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();
                ChatType chatType = account.getProperty("chat_type", ChatType.NORMAL).getAs(ChatType.class);

                if (chatType != ChatType.CLAN) {
                    account.setProperty("chat_type", ChatType.CLAN);
                    account.setProperty("old_chat_type", chatType);
                    context.sendMessage("§aVocê entrou no bate-papo do clan.");
                } else {

                    ChatType type = account.getProperty("old_chat_type", ChatType.NORMAL).getAs(ChatType.class);

                    account.setProperty("chat_type", type == ChatType.CLAN ? ChatType.NORMAL : type);
                    context.sendMessage("§cVocê saiu do bate-papo do clan.");
                }
            }
        },

        CREATE(3, "criar", "create") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account creator = context.getAccount();

                if (creator.hasClan()) {
                    context.sendMessage("§cVocê já faz parte de um clan.");
                    return;
                }

                String name = context.getArg(1);
                String tag = context.getArg(2);

                if (name.length() < 3 || name.length() > 16) {
                    context.sendMessage("§cO nome do clan deve ter entre 3 e 16 caracteres.");
                    return;
                }

                if (tag.length() < 3 || tag.length() > 8) {
                    context.sendMessage("§cA tag do clan deve ter entre 3 e 8 caracteres.");
                    return;
                }

                if (!Constants.isValid(name)) {
                    context.sendMessage("§cO nome do clan contém caracteres não alfa-numéricos.");
                    return;
                }

                if (!Constants.isValid(tag)) {
                    context.sendMessage("§cA tag do clan contém caracteres não alfa-numéricos.");
                    return;
                }

                if (clanService.isClanExists(name, tag)) {
                    context.sendMessage("§cJá existe um clan com esse nome ou tag.");
                    return;
                }

                int cost = 8000;

                if (creator.getRank().getId() < 1) {
                    creator.getDataStorage().loadColumns(Collections.singletonList(Columns.HG_COINS));

                    if (creator.getData(Columns.HG_COINS).getAsInt() < cost) {
                        context.sendMessage("§cVocê precisa de no mínimo " + cost + " coins no HG. VIPs podem criar clans de graça.");
                        return;
                    }
                } else cost = 0;

                Clan clan = new Clan(0, name, tag, 18, System.currentTimeMillis(), 0, "GRAY");

                try {
                    clanService.register(clan);
                } catch (SQLException e) {
                    context.info("unexpected_error");
                    e.printStackTrace();
                    return;
                }

                clan.join(creator, Role.OWNER);
                creator.getData(Columns.CLAN).setData(clan.getIndex());
                creator.getDataStorage().saveColumn(Columns.CLAN);
                clanService.pushClan(clan);
                context.sendMessage("§aO clan " + clan.getName() + " foi criado com sucesso.");

                ClanIntegrationMessage message = ClanIntegrationMessage.builder()
                        .messageCause(ClanIntegrationMessage.MessageCause.CREATION)
                        .clanTag(clan.getTag())
                        .index(clan.getIndex()).clanName(clan.getName())
                        .cost(cost).target(clan.getMember(creator.getUniqueId())).build();

                Constants.getRedis().publish(Redis.CLAN_INTEGRATION_CHANNEL, Constants.GSON.toJson(message));
            }
        },

        DELETE(1, "apagar", "delete") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                if (context.argsCount() > 1 && account.getRank().getId() >= Rank.DEVELOPER_ADMIN.getId()) {

                    String clanName = context.getArg(1);

                    if (!clanService.isClanExists(clanName, clanName)) {
                        context.info("object.not_found", "Clan");
                        return;
                    }

                    Clan clan = clanService.getClan(clanName);

                    if (clan == null) {
                        context.info("object.not_found", "Clan");
                        return;
                    }

                    try {
                        if (clanService.delete(clan)) {

                            for (Member clanMember : clan.getMembers()) {

                                Account memberAccount = Account.fetch(clanMember.getUniqueId());

                                if (memberAccount != null) {
                                    memberAccount.getData(Columns.CLAN).setData(-1);
                                    memberAccount.getDataStorage().saveColumn(Columns.CLAN);
                                }
                            }

                            ClanIntegrationMessage message = ClanIntegrationMessage.builder()
                                    .messageCause(ClanIntegrationMessage.MessageCause.DISBAND)
                                    .clanTag(clan.getTag())
                                    .index(clan.getIndex())
                                    .clanName(clan.getName()).build();

                            Constants.getRedis().publish(Redis.CLAN_INTEGRATION_CHANNEL, Constants.GSON.toJson(message));
                            context.sendMessage("§cClan '" + clan.getName() + "' apagada com sucesso.");
                        }
                    } catch (Exception e) {
                        context.info("unexpected_error");
                        e.printStackTrace();
                    }

                } else {
                    if (!account.hasClan()) {
                        context.sendMessage("§cVocê não faz parte de nenhum clan.");
                        return;
                    }

                    Clan clan = account.getClan();
                    Member member = clan.getMember(account.getUniqueId());

                    if (member.getRole() != Role.OWNER) {
                        context.sendMessage("§cApenas o dono do clan pode desfazê-lo.");
                        return;
                    }

                    try {
                        if (clanService.delete(clan)) {
                            sendMessage(clan, account.getUsername() + " acabou com o clan.");

                            for (Member clanMember : clan.getMembers()) {

                                Account memberAccount = Account.fetch(clanMember.getUniqueId());

                                if (memberAccount != null) {
                                    memberAccount.getData(Columns.CLAN).setData(-1);
                                    memberAccount.getDataStorage().saveColumn(Columns.CLAN);
                                }
                            }

                            ClanIntegrationMessage message = ClanIntegrationMessage.builder()
                                    .messageCause(ClanIntegrationMessage.MessageCause.DISBAND)
                                    .clanTag(clan.getTag())
                                    .index(clan.getIndex())
                                    .clanName(clan.getName()).build();

                            Constants.getRedis().publish(Redis.CLAN_INTEGRATION_CHANNEL, Constants.GSON.toJson(message));
                        }
                    } catch (Exception e) {
                        context.info("unexpected_error");
                        e.printStackTrace();
                    }
                }


            }
        },

        INVITE(2, "convidar", "invite") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                if (!account.hasClan()) {
                    context.sendMessage("§cVocê não faz parte de nenhum clan.");
                    return;
                }

                Clan clan = account.getClan();
                Member member = clan.getMember(account.getUniqueId());

                if (!member.isAdmin()) {
                    context.sendMessage("§cApenas administradores do clan podem convidar novos membros.");
                    return;
                }

                Account target = AccountStorage.getAccountByName(context.getArg(1), false);

                if (target == null) {
                    context.info("target.not_found");
                    return;
                }

                ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(target.getUniqueId());

                if (targetPlayer == null) {
                    context.info("target.not_found");
                    return;
                }

                if (target.hasClan()) {
                    context.sendMessage("§cEste jogador já faz parte de um clan.");
                    return;
                }

                if (clan.hasPendingInvite(target.getUniqueId())) {
                    context.sendMessage("§cJá há um convite pendente para este jogador.");
                    return;
                }

                if (clan.hasRecentInvite(target.getUniqueId())) {
                    context.sendMessage("§cEste jogador foi convidado recentemente, aguarde alguns minutos para convidá-lo novamente.");
                    return;
                }

                if (clan.isFull()) {
                    context.sendMessage("§cO clan está lotado.");
                    return;
                }

                Invite invite = new Invite(target.getUsername(), target.getUniqueId(), Invite.Status.PENDING, member);
                clan.getInvites().add(invite);
                sendMessage(clan, target.getDisplayName() + " foi convidado para o clan.");

                TextComponent interactable = new TextComponent(TextComponent
                        .fromLegacyText("§ePara aceitar o convite, §b§lCLIQUE AQUI"));
                interactable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/clan accept " + clan.getName().toLowerCase()));

                targetPlayer.sendMessage("");
                targetPlayer.sendMessage(TextComponent.fromLegacyText("§eVocê foi convidado para participar do clan §b" + clan.getName()));
                targetPlayer.sendMessage(interactable);
                targetPlayer.sendMessage("");
            }
        },

        TRANSFER(2, "transferir", "transfer") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                if (!account.hasClan()) {
                    context.sendMessage("§cVocê não faz parte de nenhum clan.");
                    return;
                }

                Clan clan = account.getClan();
                Member member = clan.getMember(account.getUniqueId());

                if (member.getRole() != Role.OWNER) {
                    context.sendMessage("§cApenas donos podem transferir a posse do clan.");
                    return;
                }

                Member target = clan.getMember(context.getArg(1));

                if (target == null) {
                    context.sendMessage("§cEste jogador não faz parte do clan.");
                    return;
                }

                target.setRole(Role.OWNER);
                member.setRole(Role.MEMBER);
                sendMessage(clan, target.getName() + " agora é dono do clan.");
                clanService.pushClan(clan);
            }
        },

        PROMOTE(2, "promover", "promote") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                if (!account.hasClan()) {
                    context.sendMessage("§cVocê não faz parte de nenhum clan.");
                    return;
                }

                Clan clan = account.getClan();
                Member member = clan.getMember(account.getUniqueId());

                if (member.getRole() != Role.OWNER) {
                    context.sendMessage("§cApenas o dono do clan pode promover membros.");
                    return;
                }

                Member target = clan.getMember(context.getArg(1));

                if (target == null) {
                    context.sendMessage("§cEste jogador não faz parte do clan.");
                    return;
                }

                if (target.isAdmin()) {
                    context.sendMessage("§cEste jogador já foi promovido.");
                    return;
                }

                target.setRole(Role.ADMINISTRATOR);
                sendMessage(clan, target.getName() + " foi promovido para Administrador.");

                try {
                    clanService.pushClan(clan);
                } catch (SQLException e) {
                    context.info("§cHouve um erro inesperado, o jogador voltou ao seu cargo anterior por segurança.");
                    target.setRole(Role.MEMBER);
                    e.printStackTrace();
                }
            }
        },

        DEMOTE(2, "rebaixar", "demote") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                if (!account.hasClan()) {
                    context.sendMessage("§cVocê não faz parte de nenhum clan.");
                    return;
                }

                Clan clan = account.getClan();
                Member member = clan.getMember(account.getUniqueId());

                if (member.getRole() != Role.OWNER) {
                    context.sendMessage("§cApenas donos podem rebaixar administradores.");
                    return;
                }

                Member target = clan.getMember(context.getArg(1));

                if (target == null) {
                    context.sendMessage("§cEste jogador não faz parte do clan.");
                    return;
                }

                if (target.getRole() != Role.ADMINISTRATOR) {
                    context.sendMessage("§cEste jogador não é administrador do clan.");
                    return;
                }

                target.setRole(Role.MEMBER);
                sendMessage(clan, target.getName() + " não é mais um Administrador.");
                clanService.pushClan(clan);
            }
        },

        KICK(2, "expulsar", "kick") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                if (!account.hasClan()) {
                    context.sendMessage("§cVocê não faz parte de nenhum clan.");
                    return;
                }

                Clan clan = account.getClan();
                Member member = clan.getMember(account.getUniqueId());

                if (!member.isAdmin()) {
                    context.sendMessage("§cApenas administradores podem expulsar jogadores.");
                    return;
                }

                Member target = clan.getMember(context.getArg(1));

                if (target == null) {
                    context.sendMessage("§cEste jogador não faz parte do clan.");
                    return;
                }

                if (member.getRole().getId() <= target.getRole().getId()) {
                    context.sendMessage("§cVocê não pode expulsar este membro.");
                    return;
                }

                sendMessage(clan, target.getName() + " foi expulso do clan.");

                clan.quit(target.getUniqueId());

                Account accountTarget = Account.fetch(target.getUniqueId());

                if (accountTarget != null) {
                    accountTarget.getDataStorage().getData(Columns.CLAN).setData(-1);
                    accountTarget.getDataStorage().saveColumn(Columns.CLAN);
                }

                clanService.pushClan(clan);

                ClanIntegrationMessage message = ClanIntegrationMessage.builder()
                        .messageCause(ClanIntegrationMessage.MessageCause.MEMBER_LEFT)
                        .clanTag(clan.getTag())
                        .index(clan.getIndex()).clanName(clan.getName())
                        .target(target).build();

                Constants.getRedis().publish(Redis.CLAN_INTEGRATION_CHANNEL, Constants.GSON.toJson(message));
            }
        },

        QUIT(1, "sair", "quit") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                if (!account.hasClan()) {
                    context.sendMessage("§cVocê não faz parte de nenhum clan.");
                    return;
                }

                Clan clan = account.getClan();
                Member member = clan.getMember(account.getUniqueId());

                if (member.getRole() == Role.OWNER) {
                    context.sendMessage("§cVocê não pode sair do seu próprio clan.");
                    return;
                }

                clan.quit(account.getUniqueId());
                account.getData(Columns.CLAN).setData(-1);
                account.getDataStorage().saveColumn(Columns.CLAN);
                sendMessage(clan, account.getUsername() + " saiu do clan.");
                context.sendMessage("§aVocê saiu do clan " + clan.getName() + " com sucesso.");
                clanService.pushClan(clan);

                ClanIntegrationMessage message = ClanIntegrationMessage.builder()
                        .messageCause(ClanIntegrationMessage.MessageCause.MEMBER_LEFT)
                        .clanTag(clan.getTag()).index(clan.getIndex())
                        .clanName(clan.getName())
                        .target(member).build();

                Constants.getRedis().publish(Redis.CLAN_INTEGRATION_CHANNEL, Constants.GSON.toJson(message));

            }
        },

        ACCEPT(2, "aceitar", "accept") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                if (account.hasClan()) {
                    context.sendMessage("§cVocê já faz parte de um clan.");
                    return;
                }

                String clanName = context.getArg(1);
                Clan clan = clanService.getClan(clanName);

                if (clan == null) {
                    context.info("object.not_found", "Clan");
                    return;
                }

                if (!clan.hasPendingInvite(account.getUniqueId())) {
                    context.sendMessage("§cNenhum convite pendente encontrado.");
                    return;
                }

                if (clan.isFull()) {
                    context.sendMessage("§cO clan " + clan.getName() + " está lotado.");
                    return;
                }

                Invite invite = clan.getPendingInvite(account.getUniqueId());
                invite.setStatus(Invite.Status.ACCEPTED);

                clan.join(account);
                account.getData(Columns.CLAN).setData(clan.getIndex());
                account.getDataStorage().saveColumn(Columns.CLAN);

                ClanIntegrationMessage message = ClanIntegrationMessage.builder()
                        .messageCause(ClanIntegrationMessage.MessageCause.MEMBER_JOIN)
                        .clanTag(clan.getTag()).index(clan.getIndex())
                        .clanName(clan.getName())
                        .target(clan.getMember(account.getUniqueId())).build();

                Constants.getRedis().publish(Redis.CLAN_INTEGRATION_CHANNEL, Constants.GSON.toJson(message));

                sendMessage(clan, account.getUsername() + " entrou no clan.");
                clanService.pushClan(clan);
            }
        },

        DECLINE(2, "negar", "decline") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                String clanName = context.getArg(1);
                Clan clan = clanService.getClan(clanName);

                if (clan == null) {
                    context.info("object.not_found", "Clan");
                    return;
                }

                if (!clan.hasPendingInvite(account.getUniqueId())) {
                    context.sendMessage("§cNenhum convite pendente foi encontrado.");
                    return;
                }

                Invite invite = clan.getPendingInvite(account.getUniqueId());
                invite.setStatus(Invite.Status.DECLINED);
                context.sendMessage("§cVocê negou o convite do clan " + clan.getName());
            }
        },

        INFO(1, "ver", "info") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();
                Clan clan;

                if (context.argsCount() == 1 && account.hasClan())
                    clan = account.getClan();
                else if (context.argsCount() > 1) {

                    String clanName = context.getArg(1);

                    if (!clanService.isClanExists(clanName, clanName)) {
                        context.info("object.not_found", "Clan");
                        return;
                    }

                    clan = clanService.getClan(clanName);
                } else {
                    context.sendMessage("§cVocê não faz parte de nenhum clan.");
                    return;
                }

                if (clan == null) {
                    context.info("object.not_found", "Clan");
                    return;
                }

                context.sendMessage("§aInformações de " + clan.getName() + ":");
                context.sendMessage(" §aTag: " + ChatColor.valueOf(clan.getColor()) + "[" + clan.getTag().toUpperCase() + "]");
                context.sendMessage(" §aMembros: §f" + clan.getMembers().size() + "/" + clan.getSlots());
                context.sendMessage(" §aPontos: §f" + clan.getPoints());
                context.sendMessage(" §aCriado em: §f" + account.getLanguage().getDateFormat().format(clan.getCreation()));

            }
        },

        INVITES(1, "convites", "invites") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();

                if (account.hasClan()) {
                    Clan clan = account.getClan();
                    Member member = clan.getMember(account.getUniqueId());

                    if (member.isAdmin()) {

                        if (clan.getInvites().isEmpty()) {
                            context.sendMessage("§cNenhum convite recente encontrado.");
                            return;
                        }

                        for (Invite invite : clan.getInvites()) {
                            context.sendMessage("§aConvite: §f" + invite.getInviteName());
                            context.sendMessage("  §7Emitido em: " + account.getLanguage().getDateFormat().format(invite.getRelease()));
                            context.sendMessage("  §7Emitido por: " + invite.getInvitor().getName());
                            context.sendMessage("  §7Status: " + invite.getStatus().getName());
                        }
                        return;
                    }

                    context.sendMessage("§cApenas administradores podem visualizar convites recentes.");
                } else {

                    int count = 0;

                    for (Clan clan : clanService.getClans()) {

                        if (!clan.hasPendingInvite(account.getUniqueId()))
                            continue;

                        count++;

                        TextComponent interactable = new TextComponent(TextComponent
                                .fromLegacyText("§b§lCLIQUE AQUI §epara entrar no clan §6" + clan.getName()));
                        interactable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/clan accept " + clan.getName().toLowerCase()));
                        ProxiedPlayer proxiedPlayer = BungeeCord.getInstance().getPlayer(account.getUniqueId());
                        proxiedPlayer.sendMessage(interactable);
                    }

                    if (count == 0)
                        context.sendMessage("§cNenhum convite pendente encontrado.");
                }
            }
        },

        MEMBERS(1, "membros", "members") {
            @Override
            public void execute(Context<ProxiedPlayer> context) throws SQLException {

                Account account = context.getAccount();
                Clan clan;

                if (context.argsCount() == 1 && account.hasClan())
                    clan = account.getClan();
                else if (context.argsCount() > 1) {

                    String clanName = context.getArg(1);

                    if (!clanService.isClanExists(clanName, clanName)) {
                        context.info("object.not_found", "Clan");
                        return;
                    }

                    clan = clanService.getClan(clanName);
                } else {
                    context.sendMessage("§cVocê não faz parte de nenhum clan.");
                    return;
                }

                if (clan == null) {
                    context.info("object.not_found", "Clan");
                    return;
                }

                context.sendMessage("");
                context.sendMessage("§eMembros de " + clan.getName() + ":");

                List<Member> ordered = new ArrayList<>(clan.getMembers());
                ordered.sort((a, b) -> Integer.compare(b.getRole().getId(), a.getRole().getId()));

                for (Member member : ordered) {
                    context.sendMessage(" §e" + member.getName() + " - §b" + member.getRole().getDisplay());
                }
            }
        };

        private final int minimumArgs;
        private final String[] field;

        public abstract void execute(Context<ProxiedPlayer> context) throws SQLException;

        Argument(int minimumArgs, java.lang.String... strings) {
            this.minimumArgs = minimumArgs;
            this.field = strings;
        }

        private final String MESSAGE_PREFIX = "§9[CLAN]§7 ";

        public void sendMessage(Clan clan, String msg) {
            clan.getMembers().forEach(c -> {
                ProxiedPlayer player = BungeeCord.getInstance().getPlayer(c.getUniqueId());

                if (player != null)
                    player.sendMessage(TextComponent.fromLegacyText(MESSAGE_PREFIX + msg));
            });
        }

        public static Argument fetch(String s) {
            for (Argument arg : values()) {
                for (String key : arg.getField()) {
                    if (key.equalsIgnoreCase(s))
                        return arg;
                }
            }
            return null;
        }
    }
}