package t.me.p1azmer.engine.config;

import t.me.p1azmer.engine.api.config.JOption;

public class EngineConfig {
    public static final JOption<Boolean> RESPECT_PLAYER_DISPLAYNAME = JOption.create("Engine.Respect_Player_DisplayName",
            false,
            "Sets whether or not 'Player#getDisplayName' can be used to find & get players in addition to regular 'Player#getName'.");
}
