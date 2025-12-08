package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.bot.BotDb;
import server.bot.SlashCommand;

import java.util.Optional;

public final class MyCpuKeyCommand implements SlashCommand {
    private final BotDb db;

    public MyCpuKeyCommand(BotDb db) {
        this.db = db;
    }

    @Override
    public String name() {
        return "mycpukey";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Fetch your CPU key.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, JDA jda) throws Exception {
        event.reply("Fetching CPU key...").setEphemeral(true).queue();
        Optional<BotDb.ClientRow> rowOpt = db.getUserByDiscordId(event.getUser().getId());
        if (rowOpt.isEmpty()) {
            event.getHook().editOriginal("You do not have a linked console. Please use `/link <cpukey>` first.").queue();
            return;
        }
        String cpu = rowOpt.get().cpukey;
        if (cpu == null || cpu.isEmpty()) {
            event.getHook().editOriginal("You do not have a console linked! If you are authorized to use xbNexus, please use **/link <cpukey>**").queue();
            return;
        }
        event.getHook().editOriginal("Your CPU key is: ||**`" + cpu + "`**||").queue();
    }
}
