package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.bot.BotConstants;
import server.bot.BotDb;
import server.bot.SlashCommand;

import java.security.SecureRandom;
import java.util.Arrays;

public final class GenTokenCommand implements SlashCommand {
    private final BotDb db;

    public GenTokenCommand(BotDb db) {
        this.db = db;
    }

    @Override
    public String name() {
        return "gentoken";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Generate a new 16-character token and insert it into the tokens table.")
                .addOption(OptionType.STRING, "duration", "Select token duration: Lifetime, 1 Day, 1 Week, 1 Month, or Custom", false, true)
                .addOption(OptionType.INTEGER, "custom_days", "Provide a custom number of days (only if duration=custom)", false);
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

        String choice = event.getOption("duration") != null ? event.getOption("duration").getAsString() : "lifetime";
        int days;
        switch (choice.toLowerCase()) {
            case "day": days = 1; break;
            case "week": days = 7; break;
            case "month": days = 30; break;
            case "custom":
                Integer custom = event.getOption("custom_days") != null ? (int) event.getOption("custom_days").getAsLong() : null;
                if (custom == null || custom <= 0) {
                    event.reply("Please provide a valid custom number of days when using the custom option.").setEphemeral(true).queue();
                    return;
                }
                days = custom;
                break;
            default:
                days = 999;
        }

        String token = generate(16);
        db.insertToken(token, days, "00000000000000000000000000000000", 0);
        event.reply("Your new token is: || "+token+" || (Duration: "+days+" day"+(days==1?"":"s")+")").setEphemeral(true).queue();
    }

    private static String generate(int len) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i=0;i<len;i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
