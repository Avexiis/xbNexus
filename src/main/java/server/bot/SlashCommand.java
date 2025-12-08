package server.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface SlashCommand {
    String name();
    CommandData data();
    void execute(SlashCommandInteractionEvent event, JDA jda) throws Exception;
}
