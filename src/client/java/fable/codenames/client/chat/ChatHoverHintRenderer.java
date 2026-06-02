package fable.codenames.client.chat;

import fable.codenames.block.TeamChatBlock;
import fable.codenames.block.entity.TeamChatBlockEntity;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class ChatHoverHintRenderer {

    private static final float SPEED = 0.08f;
    private static final double BASE_OFFSET_X = 8.0D / 16.0D;
    private static final double BANNER_WIDTH = 2.0D;
    private static final double BANNER_HEIGHT = 4.0D;
    private static final double WALL_Z = -0.492D;
    private static final double HOVER_REACH = 6.0D;
    private static final double BANNER_HIT_PADDING = 0.05D;
    private static final int BANNER_SEARCH_RADIUS = 6;
    private static float hoverProgress = 0.0f;

    public static void init() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> renderHud(context, tickDelta));
    }

    private static void renderHud(DrawContext context, float tickDelta) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return;

        boolean hovering = TeamChatClientState.isLeader()
                && !TeamChatClientState.hasActiveBannerInput()
                && isLookingAtTeamChat(client, tickDelta);

        if (hovering)
            hoverProgress += SPEED;
        else
            hoverProgress -= SPEED;

        hoverProgress = Math.max(0.0f, Math.min(1.0f, hoverProgress));

        if (hoverProgress <= 0.01f)
            return;
        int alpha = (int) (hoverProgress * 180);
        String message = "Нажмите ПКМ, чтобы писать в командный чат";

        int textWidth = client.textRenderer.getWidth(message);
        int textHeight = client.textRenderer.fontHeight;

        int paddingX = 10;
        int paddingY = 6;

        int boxWidth = textWidth + paddingX * 2;
        int boxHeight = textHeight + paddingY * 2;

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();

        int x = (screenW - boxWidth) / 2;
        int y = screenH - 60;

        int bgColor = (alpha << 24) | 0x000000;

        context.fill(
                x,
                y,
                x + boxWidth,
                y + boxHeight,
                bgColor);

        int textColor = (alpha << 24) | 0xFFFFFF;

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(message),
                x + paddingX,
                y + paddingY,
                textColor);
    }

    private static boolean isLookingAtTeamChat(MinecraftClient client, float tickDelta) {
        Vec3d rayStart = client.player.getCameraPosVec(tickDelta);
        Vec3d rayDirection = client.player.getRotationVec(tickDelta).normalize();
        Vec3d searchPoint = rayStart.add(rayDirection.multiply(HOVER_REACH));
        BlockPos searchCenter = BlockPos.ofFloored(searchPoint.x, searchPoint.y, searchPoint.z);

        if (client.crosshairTarget instanceof BlockHitResult hit) {
            if (TeamChatBlock.resolveTeamChat(client.world, hit) != null) {
                return true;
            }
            searchCenter = hit.getBlockPos();
        }

        for (BlockPos pos : BlockPos.iterate(
                searchCenter.add(-BANNER_SEARCH_RADIUS, -BANNER_SEARCH_RADIUS, -BANNER_SEARCH_RADIUS),
                searchCenter.add(BANNER_SEARCH_RADIUS, BANNER_SEARCH_RADIUS, BANNER_SEARCH_RADIUS))) {
            BlockState state = client.world.getBlockState(pos);
            if (!(state.getBlock() instanceof TeamChatBlock)) {
                continue;
            }

            BlockEntity blockEntity = client.world.getBlockEntity(pos);
            if (!(blockEntity instanceof TeamChatBlockEntity teamChatBlockEntity)) {
                continue;
            }

            if (rayIntersectsBanner(pos, state.get(TeamChatBlock.FACING), teamChatBlockEntity, rayStart, rayDirection)) {
                return true;
            }
        }

        return false;
    }

    private static boolean rayIntersectsBanner(BlockPos pos, Direction facing, TeamChatBlockEntity entity, Vec3d rayStart, Vec3d rayDirection) {
        Vec3d rightAxis = rightAxis(facing);
        Vec3d normalAxis = normalAxis(facing);
        double panelX = BASE_OFFSET_X + entity.getBannerOffsetXPixels() / 16.0D - BANNER_WIDTH / 2.0D;
        Vec3d leftBottom = Vec3d.of(pos)
                .add(0.5D, entity.getBannerOffsetYPixels() / 16.0D, 0.5D)
                .add(rightAxis.multiply(panelX))
                .add(normalAxis.multiply(WALL_Z));

        double denominator = normalAxis.dotProduct(rayDirection);
        if (Math.abs(denominator) < 1.0E-5D) {
            return false;
        }

        double distance = leftBottom.subtract(rayStart).dotProduct(normalAxis) / denominator;
        if (distance < 0.0D || distance > HOVER_REACH) {
            return false;
        }

        Vec3d intersection = rayStart.add(rayDirection.multiply(distance));
        Vec3d local = intersection.subtract(leftBottom);
        double x = local.dotProduct(rightAxis);
        double y = local.y;

        return x >= -BANNER_HIT_PADDING
                && x <= BANNER_WIDTH + BANNER_HIT_PADDING
                && y >= -BANNER_HIT_PADDING
                && y <= BANNER_HEIGHT + BANNER_HIT_PADDING;
    }

    private static Vec3d rightAxis(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3d(-1.0D, 0.0D, 0.0D);
            case SOUTH -> new Vec3d(1.0D, 0.0D, 0.0D);
            case WEST -> new Vec3d(0.0D, 0.0D, 1.0D);
            case EAST -> new Vec3d(0.0D, 0.0D, -1.0D);
            default -> new Vec3d(1.0D, 0.0D, 0.0D);
        };
    }

    private static Vec3d normalAxis(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3d(0.0D, 0.0D, -1.0D);
            case SOUTH -> new Vec3d(0.0D, 0.0D, 1.0D);
            case WEST -> new Vec3d(-1.0D, 0.0D, 0.0D);
            case EAST -> new Vec3d(1.0D, 0.0D, 0.0D);
            default -> new Vec3d(0.0D, 0.0D, 1.0D);
        };
    }
}
