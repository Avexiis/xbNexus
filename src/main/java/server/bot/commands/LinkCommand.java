package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.bot.BotDb;
import server.bot.BotLog;
import server.bot.SlashCommand;

import java.util.Optional;

public final class LinkCommand implements SlashCommand {
    private final BotDb db;

    public LinkCommand(BotDb db) {
        this.db = db;
    }

    @Override
    public String name() {
        return "link";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Links your console to your Discord account using your CPU key.")
                .addOption(OptionType.STRING, "cpukey", "Your CPU key (will never be shown back)", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, JDA jda) throws Exception {
        if (!event.isFromGuild()) {
            event.reply("Use this command in a server.").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();
        String cpu = event.getOption("cpukey").getAsString().trim();
        Optional<BotDb.ClientRow> rowOpt = db.getUserByCpuKey(cpu);
        if (rowOpt.isEmpty()) {
            event.getHook().editOriginal("No user found with that CPU key.").queue();
            return;
        }
        BotDb.ClientRow row = rowOpt.get();
        if (row.discordId != null && !row.discordId.isEmpty()) {
            BotLog.info("Link attempt on already-linked CPU key by user " + event.getUser().getId());
            event.getHook().editOriginal("This console is already linked to a Discord account.").queue();
            return;
        }
        boolean ok = db.linkDiscordIdToCpuKey(event.getUser().getId(), cpu);
        if (ok) event.getHook().editOriginal("Your Discord account has been linked.").queue();
        else event.getHook().editOriginal("An error occurred while linking your account.").queue();
    }
}
