package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.Globals;
import server.bot.BotConstants;
import server.bot.BotDb;
import server.Tools;
import server.bot.SlashCommand;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

public final class InfoCommand implements SlashCommand {
    private final BotDb db;

    public InfoCommand(BotDb db) {
        this.db = db;
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Returns your console info!");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, JDA jda) throws Exception {
        event.deferReply().queue();

        Optional<BotDb.ClientRow> rowOpt = db.getUserByDiscordId(event.getUser().getId());
        if (rowOpt.isEmpty()) {
            event.getHook().editOriginal("You do not have a linked console!\nIf you are authorized to use "+ Globals.STEALTH_NAME +",\nplease use **/link <cpukey>**").queue();
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
        String lastOnline = r.lastonline  != null ? "<t:" + (r.lastonline.toInstant().getEpochSecond())  + ":f>" : "N/A";
        String firstOnline = r.firstonline != null ? "<t:" + (r.firstonline.toInstant().getEpochSecond()) + ":f>" : "N/A";
        String gamertag  = r.gamertag != null ? r.gamertag : "Unknown";
        String kvDays = r.kvdays   != null ? String.valueOf(r.kvdays) : "N/A";
        boolean banned = r.banned   != null && r.banned == 1;
        int xke = r.totalxke != null ? r.totalxke : 0;
        int xos = r.totalxos != null ? r.totalxos : 0;
        int total = xke + xos;
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(0x0E98EE)
                .setTitle("Console Info for: " + event.getUser().getName())
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
}
