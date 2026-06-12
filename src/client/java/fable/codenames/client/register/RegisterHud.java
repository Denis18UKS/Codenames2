package fable.codenames.client.register;

import fable.codenames.client.hud.GameTimerHud;
import fable.codenames.client.hud.TeamListHud;
import fable.codenames.client.hud.TurnHud;

public class RegisterHud {
    public static void init() {
        TeamListHud.init();
        GameTimerHud.init();
        TurnHud.init();
    }
}