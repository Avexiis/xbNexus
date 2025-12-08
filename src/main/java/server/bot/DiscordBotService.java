package server.bot;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import server.bot.commands.*;
import server.runtime.ServerService;
import server.ui.BotConsolePanel;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

public final class DiscordBotService extends ListenerAdapter implements Runnable {
    private final BotDb db;
    private final CommandRegistry registry;
    private final BotConsolePanel consolePanel;
    private final ServerService serverService;
    private Thread thread;
    private volatile boolean running = false;
    private volatile JDA jda;
    private final PrintStream botOut;
    private OutputStreamAppender<ILoggingEvent> botAppender;

    public DiscordBotService(BotDb db, BotConsolePanel consolePanel, ServerService serverService) {
        this.db = Objects.requireNonNull(db, "db");
        this.consolePanel = Objects.requireNonNull(consolePanel, "consolePanel");
        this.serverService = Objects.requireNonNull(serverService, "serverService");
        this.registry = new CommandRegistry();
        ////////REGISTER COMMANDS HERE/////////
        registry.register(new AdminInfoCommand(db));
        registry.register(new AvatarLookupCommand());
        //registry.register(new GenTokenCommand(db));
        registry.register(new GetCpuKeyCommand(db));
        registry.register(new InfoCommand(db));
        registry.register(new LinkCommand(db));
        registry.register(new MyCpuKeyCommand(db));
        registry.register(new StatusCommand(db, serverService));
        registry.register(new TitleLookupCommand());
        registry.register(new UnlinkCommand(db));
        //registry.register(new XboxStatusCommand());
        this.botOut = consolePanel.stream();
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void startAsync() {
        if (running) return;
        running = true;
        thread = new Thread(this, "discord-bot");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        try {
            if (jda != null) {
                jda.shutdown();
                jda.awaitShutdown();
            }
        } catch (InterruptedException ignored) {
        } finally {
            jda = null;
        }
        detachSlf4jAppender();
        if (thread != null) {
            try {
                thread.join(1000);
            } catch (InterruptedException ignored) {
            }
            thread = null;
        }
        botOut.println("[DISCORD] Discord bot stopped");
    }

    @Override
    public void run() {
        try {
            initSlf4jToBotConsole();
            JDALogger.setFallbackLoggerEnabled(false);
            botOut.println("[INFO] Logging into Discord...");
            EnumSet<GatewayIntent> intents = EnumSet.of(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_VOICE_STATES,
                    GatewayIntent.GUILD_EXPRESSIONS,
                    GatewayIntent.SCHEDULED_EVENTS
            );
            jda = JDABuilder.createDefault(BotConstants.DISCORD_TOKEN, intents).addEventListeners(this).build();
            jda.awaitReady();
            botOut.println("[DISCORD] Discord login successful");
            List<CommandData> datas = new ArrayList<>();
            for (SlashCommand c : registry.all()) {
                datas.add(c.data());
                botOut.println("[DISCORD] Loaded Command - " + c.name());
            }
            jda.updateCommands().addCommands(datas).queue(
                    __  -> botOut.println("[DISCORD] Slash commands refreshed with Discord API."),
                    err -> botOut.println("[ERROR] Failed to refresh slash commands: " + err.getMessage())
            );
            jda.getPresence().setActivity(Activity.watching(BotConstants.ACTIVITY_TEXT));
            while (running) {
                try { Thread.sleep(750); } catch (InterruptedException ignored) {}
            }
        } catch (Throwable t) {
            botOut.println("[ERROR] DiscordBotService fatal error: " + t.getMessage());
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        botOut.println("[DISCORD] Loaded events - Ready");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        SlashCommand cmd = registry.get(event.getName());
        if (cmd == null) {
            event.reply("Unknown command.").setEphemeral(true).queue();
            return;
        }
        String userTag = event.getUser().getAsTag();
        String userId = event.getUser().getId();
        String guild = event.getGuild() != null ? event.getGuild().getName() : "DM";
        botOut.println("[DISCORD] Executed /" + cmd.name() + " by " + userTag + " (" + userId + ") in " + guild);
        try {
            cmd.execute(event, jda);
        } catch (Throwable t) {
            botOut.println("[ERROR] Command error in /" + cmd.name() + ": " + t.getMessage());
            if (!event.isAcknowledged()) {
                event.reply("There was an error while executing this command.").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String raw = event.getMessage().getContentRaw();
        if (!raw.startsWith("~")) return;
        botOut.println("[DISCORD] Received prefix message from " + event.getAuthor().getAsTag() + ": " + raw);
    }
    
    private void initSlf4jToBotConsole() {
        try {
            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            ctx.reset();
            PatternLayoutEncoder enc = new PatternLayoutEncoder();
            enc.setContext(ctx);
            enc.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} -- %msg%n");
            enc.start();
            OutputStream os = consolePanel.outputStream();
            OutputStreamAppender<ILoggingEvent> app = new OutputStreamAppender<>();
            app.setContext(ctx);
            app.setEncoder(enc);
            app.setName("BOT_CONSOLE");
            app.setOutputStream(os);
            app.start();
            Logger root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.INFO);
            root.addAppender(app);
            Logger jdaLog = ctx.getLogger("net.dv8tion.jda");
            if (jdaLog.getLevel() == null) jdaLog.setLevel(Level.INFO);
            this.botAppender = app;
        } catch (Throwable t) {
            botOut.println("[WARN] Could not attach Logback appender: " + t.getMessage());
        }
    }

    private void detachSlf4jAppender() {
        if (botAppender == null) return;
        try {
            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.detachAppender(botAppender);
            botAppender.stop();
        } catch (Throwable ignored) {
        } finally {
            botAppender = null;
        }
    }
}
