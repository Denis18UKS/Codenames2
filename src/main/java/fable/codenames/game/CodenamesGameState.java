package fable.codenames.game;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class CodenamesGameState extends PersistentState {

    public static final String KEY = "codenames_game";

    private CodenamesPhase phase = CodenamesPhase.STOPPED;

    private String activeTeam = "";

    private String clueWord = "";

    private int clueCount;

    private long phaseStartTick;

    private long phaseEndTick;

    private String disputeTeam = "";

    private long disputeEndTick;

    private int guessesThisTurn;

    // =========================================================
    // ВАЖНО:
    // таймер запускается только после первой подсказки лидера
    // =========================================================
    private boolean guessingTimerUnlocked = false;

    public static CodenamesGameState createFromNbt(NbtCompound nbt) {

        CodenamesGameState state = new CodenamesGameState();

        state.phase = CodenamesPhase.fromId(
                nbt.getString("phase"));

        state.activeTeam = nbt.getString("activeTeam");

        state.clueWord = nbt.getString("clueWord");

        state.clueCount = nbt.getInt("clueCount");

        state.phaseStartTick = nbt.getLong("phaseStartTick");

        state.phaseEndTick = nbt.getLong("phaseEndTick");

        state.disputeTeam = nbt.getString("disputeTeam");

        state.disputeEndTick = nbt.getLong("disputeEndTick");

        state.guessesThisTurn = nbt.getInt("guessesThisTurn");

        state.guessingTimerUnlocked =
                nbt.getBoolean("guessingTimerUnlocked");

        return state;
    }

    public CodenamesPhase getPhase() {
        return this.phase;
    }

    public String getActiveTeam() {
        return this.activeTeam;
    }

    public String getClueWord() {
        return this.clueWord;
    }

    public int getClueCount() {
        return this.clueCount;
    }

    public long getPhaseEndTick() {
        return this.phaseEndTick;
    }

    public String getDisputeTeam() {
        return this.disputeTeam;
    }

    public long getDisputeEndTick() {
        return this.disputeEndTick;
    }

    public int getGuessesThisTurn() {
        return this.guessesThisTurn;
    }

    // =========================================================
    // НОВЫЕ МЕТОДЫ
    // =========================================================

    public boolean isGuessingTimerUnlocked() {
        return this.guessingTimerUnlocked;
    }

    public void unlockGuessingTimer() {

        if (!this.guessingTimerUnlocked) {

            this.guessingTimerUnlocked = true;

            markDirty();
        }
    }

    // =========================================================

    public void setPhase(
            CodenamesPhase phase,
            String activeTeam,
            long startTick,
            long durationTicks) {

        this.phase = phase;

        this.activeTeam =
                activeTeam == null
                        ? ""
                        : activeTeam;

        this.phaseStartTick = startTick;

        this.phaseEndTick =
                durationTicks <= 0
                        ? 0
                        : startTick + durationTicks;

        markDirty();
    }

    public void resetPhaseTimer(
            long startTick,
            long durationTicks) {

        this.phaseStartTick = startTick;

        this.phaseEndTick =
                durationTicks <= 0
                        ? 0
                        : startTick + durationTicks;

        markDirty();
    }

    public void clearGuessesThisTurn() {

        this.guessesThisTurn = 0;

        markDirty();
    }

    public void incrementGuessesThisTurn() {

        this.guessesThisTurn++;

        markDirty();
    }

    public void setClue(String word, int count) {

        this.clueWord = word;

        this.clueCount = count;

        clearDispute();

        markDirty();
    }

    public void clearClue() {

        this.clueWord = "";

        this.clueCount = 0;

        clearDispute();

        markDirty();
    }

    public void startDispute(
            String teamName,
            long endTick) {

        this.disputeTeam =
                teamName == null
                        ? ""
                        : teamName;

        this.disputeEndTick = endTick;

        markDirty();
    }

    public void clearDispute() {

        this.disputeTeam = "";

        this.disputeEndTick = 0;

        markDirty();
    }

    public void stop() {

        this.phase = CodenamesPhase.STOPPED;

        this.activeTeam = "";

        this.clueWord = "";

        this.clueCount = 0;

        this.phaseStartTick = 0;

        this.phaseEndTick = 0;

        this.disputeTeam = "";

        this.disputeEndTick = 0;

        this.guessesThisTurn = 0;

        // =====================================================
        // СБРОС
        // =====================================================
        this.guessingTimerUnlocked = false;

        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {

        nbt.putString("phase", this.phase.getId());

        nbt.putString("activeTeam", this.activeTeam);

        nbt.putString("clueWord", this.clueWord);

        nbt.putInt("clueCount", this.clueCount);

        nbt.putLong("phaseStartTick", this.phaseStartTick);

        nbt.putLong("phaseEndTick", this.phaseEndTick);

        nbt.putString("disputeTeam", this.disputeTeam);

        nbt.putLong("disputeEndTick", this.disputeEndTick);

        nbt.putInt("guessesThisTurn", this.guessesThisTurn);

        nbt.putBoolean(
                "guessingTimerUnlocked",
                this.guessingTimerUnlocked);

        return nbt;
    }
}