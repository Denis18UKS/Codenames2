package fable.codenames.client.register;

import fable.codenames.client.hud.GameTimerHud;
import fable.codenames.client.hud.TeamListHud;

public class RegisterHud {
    public static void init() {
        TeamListHud.init();
        GameTimerHud.init();
    }
}
