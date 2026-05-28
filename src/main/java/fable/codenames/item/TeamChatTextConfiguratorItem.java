package fable.codenames.item;

import fable.codenames.block.TeamChatBlock;
import fable.codenames.chat.TeamChatPackets;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeamChatTextConfiguratorItem extends Item {
    public TeamChatTextConfiguratorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        if (!player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("Настраивать текст может только оператор.").formatted(Formatting.YELLOW), true);
            return ActionResult.FAIL;
        }

        if (!(context.getWorld().getBlockState(context.getBlockPos()).getBlock() instanceof TeamChatBlock)) {
            player.sendMessage(Text.literal("Кликни по чат-баннеру.").formatted(Formatting.YELLOW), true);
            return ActionResult.PASS;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, TeamChatPackets.OPEN_TEXT_LAYOUT, buf);
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("ПКМ по чат-баннеру: настроить позицию текста").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Настройка сохраняется в мире (на сервере).").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("Только для операторов.").formatted(Formatting.DARK_GRAY));
    }
}
