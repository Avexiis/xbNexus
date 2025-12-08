package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.bot.BotDb;
import server.bot.SlashCommand;

import java.util.Optional;

public final class UnlinkCommand implements SlashCommand {
    private final BotDb db;

    public UnlinkCommand(BotDb db) {
        this.db = db;
    }

    @Override
    public String name() {
        return "unlink";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Unlinks your Discord account from your console.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, JDA jda) throws Exception {
        if (!event.isFromGuild()) {
            event.reply("Use this command in a server.").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();
        Optional<BotDb.ClientRow> rowOpt = db.getUserByDiscordId(event.getUser().getId());
        if (rowOpt.isEmpty()) {
            event.getHook().editOriginal("No user found with your Discord ID.").queue();
            return;
        }
        boolean ok = db.unlinkByDiscordId(event.getUser().getId());
        if (ok) event.getHook().editOriginal("Your Discord account has been unlinked.").queue();
        else event.getHook().editOriginal("Your account is not linked to a console.").queue();
    }
}
