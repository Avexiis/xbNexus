package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.bot.*;
import server.Tools;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public final class AdminInfoCommand implements SlashCommand {
    private final BotDb db;

    public AdminInfoCommand(BotDb db) {
        this.db = db;
    }

    @Override
    public String name() {
        return "admininfo";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Allows admins to view console info for another user.").addOption(OptionType.USER, "target", "Select the target user", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, JDA jda) throws Exception {
        if (!event.isFromGuild()) {
            event.reply("This command can only be used in a guild.").setEphemeral(true).queue();
            return;
        }
        Member member = event.getMember();
        if (member == null || member.getRoles().stream().noneMatch(r -> Arrays.asList(BotConstants.ALLOWED_ROLE_IDS).contains(r.getId()))) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }
        String targetId   = event.getOption("target").getAsUser().getId();
        String targetName = event.getOption("target").getAsUser().getName();
        event.deferReply().queue();
        Optional<BotDb.ClientRow> rowOpt = db.getUserByDiscordId(targetId);
        if (rowOpt.isEmpty()) {
            event.getHook().editOriginal("No user found with that Discord ID.").queue();
            return;
        }
        BotDb.ClientRow r = rowOpt.get();
        String titleId = (r.titleid == null || r.titleid.isEmpty() || "0".equals(r.titleid)) ? null : r.titleid;
        String titleDisplay;
        if (titleId == null) {
            titleDisplay = "*Unknown or Unavailable*";
        } else {
            String titleName = Tools.GetTitleMap().getOrDefault(titleId.toUpperCase(Locale.ROOT), titleId);
            titleDisplay = titleName.equalsIgnoreCase(titleId) ? titleId : (titleName + " (" + titleId + ")");
        }
        String lastOnline = r.lastonline  != null ? formatDiscordTimestamp(r.lastonline.toInstant())  : "N/A";
        String firstOnline = r.firstonline != null ? formatDiscordTimestamp(r.firstonline.toInstant()) : "N/A";
        String gamertag = r.gamertag != null ? r.gamertag : "Unknown";
        String kvDays = r.kvdays   != null ? String.valueOf(r.kvdays) : "N/A";
        boolean banned = r.banned   != null && r.banned == 1;
        int xke   = r.totalxke != null ? r.totalxke : 0;
        int xos   = r.totalxos != null ? r.totalxos : 0;
        int total = xke + xos;
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(0x0E98EE)
                .setTitle("[ADMIN] Console Info for: " + targetName)
                .setThumbnail(BotConstants.ICON_URL)
                .setDescription(
                        "**__Account Info__**\n" +
                                "👤 **Gamertag:** *" + gamertag + "*\n\n" +
                                "**__KV Information__**\n" +
                                "🌓 **Days:** *" + kvDays + "*\n" +
                                (banned ? "❌" : "✅") + " **Status:** *" + (banned ? "Banned" : "Unbanned") + "*\n\n" +
                                "**__Activity Information__**\n" +
                                "🕒 **Last Online:**\n" +
                                "📆 *" + lastOnline + "*\n" +
                                "🕒 **First Connection:**\n" +
                                "📆 *" + firstOnline + "*\n\n" +
                                "**__Last Known Title__**\n" +
                                "🎮 *" + titleDisplay + "*\n\n" +
                                "**__Challenges__**\n" +
                                "🧪 **XKE Challenges:** *" + xke + "*\n" +
                                "🧪 **XOS Challenges:** *" + xos + "*\n" +
                                "🧮 **Total Challenges:** *" + total + "*"
                )
                .setFooter(BotConstants.COPYRIGHT, BotConstants.ICON_URL)
                .setTimestamp(Instant.now());
        event.getHook().editOriginalEmbeds(eb.build()).queue();
    }

    private static String formatDiscordTimestamp(Instant instant) {
        long unix = instant.getEpochSecond();
        return "<t:" + unix + ":f>";
    }
}
