package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.bot.BotConstants;
import server.bot.BotDb;
import server.bot.SlashCommand;

import java.util.Arrays;
import java.util.Optional;

public final class GetCpuKeyCommand implements SlashCommand {
    private final BotDb db;

    public GetCpuKeyCommand(BotDb db) {
        this.db = db;
    }

    @Override
    public String name() {
        return "getcpukey";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Allows admins to view the CPU key for another user.").addOption(OptionType.USER, "target", "Select the target user", true);
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

        String targetId = event.getOption("target").getAsUser().getId();
        event.reply("Fetching CPU key for user...").setEphemeral(true).queue();

        Optional<BotDb.ClientRow> rowOpt = db.getUserByDiscordId(targetId);
        if (rowOpt.isEmpty()) {
            event.getHook().editOriginal("No user found with that Discord ID.").queue();
            return;
        }
        String cpu = rowOpt.get().cpukey;
        if (cpu == null || cpu.isEmpty()) {
            event.getHook().editOriginal("The selected user does not have a CPU key linked.").queue();
            return;
        }
        event.getHook().editOriginal("CPU key: ||**`" + cpu + "`**||").queue();
    }
}
