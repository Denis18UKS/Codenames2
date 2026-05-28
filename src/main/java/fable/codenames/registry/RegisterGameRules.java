package fable.codenames.registry;

import fable.codenames.Codenames;
import fable.codenames.world.ModBooleanGameRules;
import fable.codenames.world.ModIntegerGameRules;
import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Arrays;

public class RegisterGameRules {
    public static final CustomGameRuleCategory Codenames_GAMERULE_CATEGORY = new CustomGameRuleCategory(new Identifier(Codenames.MOD_ID, Codenames.MOD_ID), Text.translatable("gamerule.category." + Codenames.MOD_ID).formatted(Formatting.YELLOW, Formatting.BOLD));

    public static void init() {
        Arrays.stream(ModBooleanGameRules.values()).forEach(Enum::describeConstable);
        Arrays.stream(ModIntegerGameRules.values()).forEach(Enum::describeConstable);
    }
}
