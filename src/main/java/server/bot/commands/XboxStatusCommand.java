package server.bot.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.bot.BotConstants;
import server.bot.BotLog;
import server.bot.SlashCommand;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;

public final class XboxStatusCommand implements SlashCommand {

    @Override
    public String name() {
        return "xboxstatus";
    }

    @Override
    public CommandData data() {
        return Commands.slash(name(), "Fetches current Xbox LIVE status.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, JDA jda) throws Exception {
        event.deferReply().queue();
        final Map<String,String> emoji = Map.of(
                "FullyOperational", "🟢",
                "MostlyOperational", "🟡",
                "HardlyOperational", "🟠",
                "Inoperational", "🔴",
                "Unknown", "⚪"
        );
        String url = "wss://kvchecker.com/ws/LIVEAuthentication";
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> firstMessage = new CompletableFuture<>();
        WebSocket ws = client.newWebSocketBuilder()
                .header("Origin", "https://xblstatus.com")
                .header("User-Agent", "Mozilla/5.0")
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(url), new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        try { firstMessage.complete(data.toString()); }
                        catch (Exception ignored) {}
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }
                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        firstMessage.completeExceptionally(error);
                        WebSocket.Listener.super.onError(webSocket, error);
                    }
                }).join();
        String payload;
        try {
            payload = firstMessage.orTimeout(30, TimeUnit.SECONDS).join();
        } catch (CompletionException e) {
            try {
                ws.abort();
            } catch (Exception ignored) {
            }
            event.getHook().editOriginal("No valid data received within 30 seconds.").queue();
            BotLog.warn("XboxStatus timeout or error: " + e.getMessage());
            return;
        } finally {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            } catch (Exception ignored) {
            }
        }
        try {
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
            if (!obj.has("message_type") || !"xbl_status".equals(obj.get("message_type").getAsString())) {
                event.getHook().editOriginal("Unexpected data received from status endpoint.").queue();
                return;
            }
            JsonArray services = obj.getAsJsonArray("services");
            StringBuilder desc = new StringBuilder();
            for (JsonElement el : services) {
                JsonObject s = el.getAsJsonObject();
                String name = s.get("name").getAsString();
                String description = s.get("description").getAsString();
                String em = emoji.getOrDefault(description, "⚪");
                desc.append(em).append(" **").append(name).append("**: ").append(description).append("\n");
            }
            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(0x0E98EE)
                    .setTitle("__Xbox LIVE Status!__")
                    .setThumbnail(BotConstants.ICON_URL)
                    .setDescription(desc.toString() + "\n-# This data is provided by\n-# https://xblstatus.com/")
                    .setFooter(BotConstants.COPYRIGHT, BotConstants.ICON_URL)
                    .setTimestamp(Instant.now());
            event.getHook().editOriginalEmbeds(eb.build()).queue();
        } catch (Exception ex) {
            BotLog.error("XboxStatus parse error: " + ex.getMessage());
            event.getHook().editOriginal(
                    "Error connecting to the xblstatus endpoint.\n" +
                            "This can happen if Xbox LIVE is having connection issues and is not a reflection of the status of xbNexus or https://xblstatus.com/\n" +
                            "Please click the link above to view the last known status."
            ).queue();
        }
    }
}
