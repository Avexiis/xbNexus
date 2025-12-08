package server.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.Globals;
import server.bot.BotConstants;
import server.bot.BotDb;
import server.bot.BotLog;
import server.bot.SlashCommand;
import server.runtime.RuntimeStats;
import server.runtime.ServerService;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Objects;

public final class StatusCommand implements SlashCommand {
    private final BotDb db;
    private final ServerService server;

    public StatusCommand(BotDb db, ServerService server) {
        this.db = Objects.requireNonNull(db, "db");
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public String name() {
        return "status";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Returns total registered consoles, the server status, and uptime information.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, JDA jda) {
        event.deferReply().queue();
        int totalConsoles = 0;
        try {
            totalConsoles = db.getHighestIdFromClients();
        } catch (Exception ex) {
            BotLog.warn("DB getHighestIdFromClients failed: " + ex.getMessage());
        }
        int totalXke = 0;
        int totalXos = 0;
        try {
            BotDb.ChallengeTotals totals = db.getGlobalChallengeTotals();
            totalXke = totals.totalXke;
            totalXos = totals.totalXos;
        } catch (Exception ex) {
            BotLog.warn("DB getGlobalChallengeTotals failed: " + ex.getMessage());
        }
        int totalChallenges = totalXke + totalXos;
        final boolean serverRunning = server.isRunning();
        String onlineStatus = serverRunning ? "🟢 • Online!" : "🔴 • Offline!";
        String serverUptime = "N/A";
        if (serverRunning) {
            RuntimeStats.Snapshot snap = server.getStats().snapshot();
            long ms = (snap.startMillis == 0) ? 0 : (snap.nowMillis - snap.startMillis);
            serverUptime = formatUptime(ms);
        }
        long botMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String botUptime = formatUptime(botMs);
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(0x0E98EE)
                .setTitle(Globals.STEALTH_NAME + " Server Stats!")
                .setThumbnail(BotConstants.ICON_URL)
                .setDescription(
                        "__***Total Registered Consoles:***__\n" +
                                "📋 • **" + totalConsoles + "**\n\n" +
                                "__***" + Globals.STEALTH_NAME + " Server Status***__\n" +
                                BotConstants.COUNTRY_EMOJI + " *" + BotConstants.HOST_CITY + "*:\n" +
                                "**" + onlineStatus + "**\n\n" +
                                "__***Uptime Information***__\n" +
                                "🎮 **" + Globals.STEALTH_NAME + " Uptime:**\n" +
                                "⏱️ *" + serverUptime + "*\n" +
                                "🤖 **Bot Uptime:**\n" +
                                "⏱️ *" + botUptime + "*\n" +
                                "\n__***Challenges***__\n" +
                                "🧪 Total XKE: *" + totalXke + "*\n" +
                                "🧪 Total XOS: *" + totalXos + "*\n" +
                                "🧮 Total Challenges: *" + totalChallenges + "*\n"
                )
                .setFooter(BotConstants.COPYRIGHT, BotConstants.ICON_URL)
                .setTimestamp(Instant.now());
        event.getHook().editOriginalEmbeds(eb.build()).queue();
    }
    private static String formatUptime(long millis) {
        if (millis <= 0) return "N/A";
        long s = millis / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600;  s %= 3600;
        long m = s / 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d");
        if (h > 0 || d > 0) sb.append(h).append("h");
        sb.append(m).append("m");
        return sb.toString();
    }
}
