package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.Tools;
import server.bot.BotConstants;
import server.bot.SlashCommand;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class TitleLookupCommand implements SlashCommand {

    @Override
    public String name() {
        return "titlelookup";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Lookup a game by TitleID or search for games by name!").addOption(OptionType.STRING, "query", "Enter a TitleID or game name to search", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, JDA jda) throws Exception {
        String q = event.getOption("query").getAsString().trim();
        if (q.length() < 4) {
            event.reply("Please provide at least 4 characters to search.").setEphemeral(true).queue();
            return;
        }
        Map<String, String> map = Tools.GetTitleMap();
        if (map.isEmpty()) {
            event.reply("No title database is loaded.").setEphemeral(true).queue();
            return;
        }
        String key = q.replace("0x", "").replace("0X","").toUpperCase(Locale.ROOT);
        if (map.containsKey(key)) {
            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(0x0E98EE)
                    .setTitle("Title Lookup Result")
                    .setThumbnail(BotConstants.ICON_URL)
                    .setDescription("📌 **TitleID:** " + key + "\n🎮 **Game Name:** " + map.get(key))
                    .setFooter(BotConstants.COPYRIGHT, BotConstants.ICON_URL)
                    .setTimestamp(Instant.now());
            event.replyEmbeds(eb.build()).queue();
            return;
        }
        String qLower = q.toLowerCase(Locale.ROOT);
        List<Map.Entry<String,String>> matches = map.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().toLowerCase(Locale.ROOT).contains(qLower))
                .limit(1000)
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            event.reply("No matching games found.").setEphemeral(true).queue();
            return;
        }
        String result = matches.stream()
                .map(e -> "🎮 **" + e.getValue() + "**\n- (TitleID: `" + e.getKey() + "`)")
                .collect(Collectors.joining("\n"));
        if (result.length() > 4000) {
            event.reply("Too many results, please refine your search and try again.").setEphemeral(true).queue();
            return;
        }
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(0x0E98EE)
                .setTitle("Title Lookup Results")
                .setThumbnail(BotConstants.ICON_URL)
                .setDescription(result)
                .setFooter(BotConstants.COPYRIGHT, BotConstants.ICON_URL)
                .setTimestamp(Instant.now());
        event.replyEmbeds(eb.build()).queue();
    }
}
