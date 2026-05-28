package fable.codenames.client.game;

import fable.codenames.game.CodenamesPhase;

public final class GameTimerClientState {

    private static boolean active;
    private static String teamName = "";
    private static CodenamesPhase phase = CodenamesPhase.STOPPED;

    private static long remainingTicks;
    private static long totalTicks;

    private static long syncMillis;
    private static boolean canPassTurn;

    private GameTimerClientState() {}

    public static void update(boolean active,
                              String teamName,
                              CodenamesPhase phase,
                              long remainingTicks,
                              long totalTicks,
                              boolean canPassTurn) {

        GameTimerClientState.active = active;
        GameTimerClientState.teamName = teamName == null ? "" : teamName;
        GameTimerClientState.phase = phase == null ? CodenamesPhase.STOPPED : phase;

        GameTimerClientState.remainingTicks = Math.max(0L, remainingTicks);
        GameTimerClientState.totalTicks = Math.max(0L, totalTicks);

        GameTimerClientState.canPassTurn = canPassTurn;

        // 🔥 КЛЮЧ: фикс времени синхронизации
        GameTimerClientState.syncMillis = System.currentTimeMillis();
    }

    public static void clear() {
        active = false;
        teamName = "";
        phase = CodenamesPhase.STOPPED;

        remainingTicks = 0L;
        totalTicks = 0L;
        canPassTurn = false;

        syncMillis = 0L;
    }

    public static boolean isActive() {
        // 🔥 FIX: теперь только active
        return active;
    }

    public static long getRemainingTicks() {

        // ❗ ЕСЛИ ТАЙМЕР НЕ АКТИВЕН — НЕ УМЕНЬШАЕМ
        if (!active) {
            return remainingTicks;
        }

        if (totalTicks <= 0L || syncMillis <= 0L) {
            return 0L;
        }

        long elapsed = (System.currentTimeMillis() - syncMillis) / 50L;
        long value = remainingTicks - elapsed;

        return Math.max(0L, value);
    }

    public static String getTeamName() {
        return teamName;
    }

    public static CodenamesPhase getPhase() {
        return phase;
    }

    public static long getTotalTicks() {
        return totalTicks;
    }

    public static boolean canPassTurn() {
        return canPassTurn;
    }
}