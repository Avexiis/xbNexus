package server.bot;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CommandRegistry {
    private final Map<String, SlashCommand> cmds = new LinkedHashMap<>();

    public void register(SlashCommand cmd) {
        cmds.put(cmd.name(), cmd);
    }

    public SlashCommand get(String name) {
        return cmds.get(name);
    }

    public Collection<SlashCommand> all() {
        return cmds.values();
    }
}
