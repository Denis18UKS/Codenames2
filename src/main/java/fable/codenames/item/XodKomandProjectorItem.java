package fable.codenames.item;

import fable.codenames.entity.HologramProjectorEntity;
import fable.codenames.entity.ModMiscEntityTypes;
import fable.codenames.entity.XodKomandProjectorEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class XodKomandProjectorItem extends Item {

    private static final String FACE_KEY = "HologramFace";
    private static final Direction[] FACES = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    public XodKomandProjectorItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.isSneaking()) {
            cycleFace(stack);

            if (!world.isClient()) {
                user.sendMessage(faceText(stack), true);
            }

            return TypedActionResult.success(stack, world.isClient());
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {

        if (context.getPlayer() != null && context.getPlayer().isSneaking()) {
            cycleFace(context.getStack());

            if (!context.getWorld().isClient()) {
                context.getPlayer().sendMessage(faceText(context.getStack()), true);
            }

            return ActionResult.SUCCESS;
        }

        if (!(context.getWorld() instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }

        BlockPos blockPos = context.getBlockPos().offset(context.getSide());
        Vec3d spawnPos = Vec3d.ofBottomCenter(blockPos);

        EntityType<?> type = ModMiscEntityTypes.XOD_KOMAND_PROJECTOR.getEntityType();

        XodKomandProjectorEntity entity =
                (XodKomandProjectorEntity) type.create(serverWorld);

        if (entity == null) {
            return ActionResult.FAIL;
        }

        Direction face = getFace(
                context.getStack(),
                context.getPlayer() == null
                        ? Direction.SOUTH
                        : context.getPlayer().getHorizontalFacing()
        );

        entity.refreshPositionAndAngles(
                spawnPos.x,
                spawnPos.y,
                spawnPos.z,
                face.asRotation(),
                0.0f
        );

        serverWorld.spawnEntity(entity);

        ItemStack stack = context.getStack();

        if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
            stack.decrement(1);
        }

        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Shift + ПКМ: изменить face").formatted(Formatting.GRAY));
        tooltip.add(faceText(stack).copy().formatted(Formatting.YELLOW));
    }

    private static void cycleFace(ItemStack stack) {
        Direction current = getFace(stack, Direction.SOUTH);

        int index = 0;
        for (int i = 0; i < FACES.length; i++) {
            if (FACES[i] == current) {
                index = i;
                break;
            }
        }

        Direction next = FACES[(index + 1) % FACES.length];
        stack.getOrCreateNbt().putString(FACE_KEY, next.asString());
    }

    private static Direction getFace(ItemStack stack, Direction fallback) {
        if (stack.hasNbt()) {
            Direction face = Direction.byName(stack.getNbt().getString(FACE_KEY));

            if (face != null && face.getAxis().isHorizontal()) {
                return face;
            }
        }
        return fallback;
    }

    private static Text faceText(ItemStack stack) {
        Direction face = getFace(stack, Direction.SOUTH);
        return Text.literal("Face: " + face.asString());
    }
}