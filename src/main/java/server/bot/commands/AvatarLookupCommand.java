package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.bot.BotConstants;
import server.bot.SlashCommand;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class AvatarLookupCommand implements SlashCommand {

    @Override
    public String name() {
        return "avatarlookup";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Lookup an Xbox Live avatar by gamertag!").addOption(OptionType.STRING, "gamertag", "Enter the Xbox Live gamertag", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, JDA jda) throws Exception {
        String gamertag = event.getOption("gamertag").getAsString().trim();
        String profilePicURL = "http://avatar.xboxlive.com/avatar/" + url(gamertag) + "/avatarpic-l.png";
        String bodyPicURL = "http://avatar.xboxlive.com/avatar/" + url(gamertag) + "/avatar-body.png";
        if (!headOk(profilePicURL) || !headOk(bodyPicURL)) {
            event.reply("Please check your spelling or enter a different gamertag").setEphemeral(true).queue();
            return;
        }
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(0x0E98EE)
                .setTitle("Avatar for: " + gamertag)
                .setThumbnail(profilePicURL)
                .setImage(bodyPicURL)
                .setFooter(BotConstants.COPYRIGHT, BotConstants.ICON_URL)
                .setTimestamp(Instant.now());
        event.replyEmbeds(eb.build()).queue();
    }

    private static boolean headOk(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("HEAD");
            c.setConnectTimeout(7000);
            c.setReadTimeout(7000);
            int code = c.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String url(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            return s;
        }
    }
}
