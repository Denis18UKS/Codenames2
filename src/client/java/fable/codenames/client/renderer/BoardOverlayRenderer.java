package fable.codenames.client.renderer;

import fable.codenames.board.BoardCellType;
import fable.codenames.client.board.BoardClientState;
import fable.codenames.item.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BoardOverlayRenderer {
    private static final float BADGE_SCALE = 0.014F;
    private static final RenderLayer OVERLAY_LINES = RenderLayer.getLines();
    private static final RenderLayer OVERLAY_QUADS = RenderLayer.getLightning();
    private static final RenderLayer TEXT_SEE_THROUGH = RenderLayer.getTextSeeThrough(new Identifier("codenames", "text/see_through"));
    private static final RenderLayer TEXT_NORMAL = RenderLayer.getText(new Identifier("codenames", "text/normal"));

    private BoardOverlayRenderer() {
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        Map<BlockPos, BoardCellType> cells = BoardClientState.getCells();
        List<BoardClientState.VoteIndicator> voteIndicators = BoardClientState.getVoteIndicators();
        MatrixStack matrices = context.matrixStack();

        if (player == null || matrices == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        boolean isHoldingTool = isHoldingTool(player);
        boolean showCells = !cells.isEmpty() && (isHoldingTool || BoardClientState.canSeeAnswers());
        boolean showVotes = !voteIndicators.isEmpty();

        if (!showCells && !showVotes) {
            return;
        }

        if (showCells) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();
            matrices.push();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);

            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (Map.Entry<BlockPos, BoardCellType> entry : cells.entrySet()) {
                if (entry.getValue() == BoardCellType.UNASSIGNED && !isHoldingTool) {
                    continue;
                }

                int color = entry.getValue().getColor();
                float red = ((color >> 16) & 255) / 255.0F;
                float green = ((color >> 8) & 255) / 255.0F;
                float blue = (color & 255) / 255.0F;

                drawThickInsideBox(matrices, bufferBuilder, entry.getKey(), cameraPos, red, green, blue, cells);
            }

            tessellator.draw();

            RenderSystem.enableCull();
            RenderSystem.disableBlend();

            matrices.pop();

            boolean hasNeutral = cells.values().stream().anyMatch(t -> t == BoardCellType.NEUTRAL);
            if (hasNeutral) {
                matrices.push();

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.disableCull();
                RenderSystem.lineWidth(2.0F);
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);

                bufferBuilder.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

                for (Map.Entry<BlockPos, BoardCellType> entry : cells.entrySet()) {
                    if (entry.getValue() != BoardCellType.NEUTRAL) {
                        continue;
                    }

                    int color = entry.getValue().getColor();
                    float red = ((color >> 16) & 255) / 255.0F;
                    float green = ((color >> 8) & 255) / 255.0F;
                    float blue = (color & 255) / 255.0F;

                    drawNeutralExtraInsideBox(matrices, bufferBuilder, entry.getKey(), cameraPos, red, green, blue);
                }

                tessellator.draw();

                RenderSystem.lineWidth(1.0F);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();

                matrices.pop();
            }
        }

        if (showVotes) {
            matrices.push();

            boolean hasSeeThrough = false;
            boolean hasNormal = false;

            for (BoardClientState.VoteIndicator indicator : voteIndicators) {
                if (!isIndicatorFromPlayerTeam(player, indicator)) {
                    continue;
                }

                boolean seeThrough = shouldRenderSeeThrough(player, indicator);

                if (seeThrough) {
                    hasSeeThrough = true;
                } else {
                    hasNormal = true;
                }

                drawVoteBadge(client, context, matrices, consumers, cameraPos, indicator, seeThrough);
            }

            matrices.pop();

            if (hasNormal) {
                consumers.draw(RenderLayer.getTextBackground());
                consumers.draw(TEXT_NORMAL);
            }
            if (hasSeeThrough) {
                consumers.draw(RenderLayer.getTextBackgroundSeeThrough());
                consumers.draw(TEXT_SEE_THROUGH);
            }
        }

        consumers.draw();
    }

    /**
     * Проверяет, принадлежит ли индикатор команде игрока.
     */
    private static boolean isIndicatorFromPlayerTeam(PlayerEntity player, BoardClientState.VoteIndicator indicator) {
        AbstractTeam playerTeam = player.getScoreboardTeam();
        if (playerTeam == null) {
            return false;
        }

        String playerTeamName = playerTeam.getName().toLowerCase(Locale.ROOT);
        String indicatorTeamName = indicator.teamName().toLowerCase(Locale.ROOT);

        boolean playerIsRed = playerTeamName.contains("red") || playerTeamName.contains("крас");
        boolean playerIsBlue = playerTeamName.contains("blue") || playerTeamName.contains("син");
        boolean indicatorIsRed = indicatorTeamName.contains("red") || indicatorTeamName.contains("крас");
        boolean indicatorIsBlue = indicatorTeamName.contains("blue") || indicatorTeamName.contains("син");

        if (playerIsRed && indicatorIsRed) return true;
        if (playerIsBlue && indicatorIsBlue) return true;

        return playerTeamName.equals(indicatorTeamName);
    }

    /**
     * Определяет, должен ли индикатор рендериться в режиме SEE_THROUGH.
     * Отгадывающие видят SEE_THROUGH для своей команды.
     * Лидеры не видят SEE_THROUGH.
     */
    private static boolean shouldRenderSeeThrough(PlayerEntity player, BoardClientState.VoteIndicator indicator) {
        // Лидеры не видят SEE_THROUGH
        if (isPlayerLeader(player)) {
            return false;
        }

        // Отгадывающие видят SEE_THROUGH для своей команды
        AbstractTeam playerTeam = player.getScoreboardTeam();
        if (playerTeam == null) {
            return false;
        }

        String playerTeamName = playerTeam.getName().toLowerCase(Locale.ROOT);
        String indicatorTeamName = indicator.teamName().toLowerCase(Locale.ROOT);

        boolean playerIsRed = playerTeamName.contains("red") || playerTeamName.contains("крас");
        boolean playerIsBlue = playerTeamName.contains("blue") || playerTeamName.contains("син");
        boolean indicatorIsRed = indicatorTeamName.contains("red") || indicatorTeamName.contains("крас");
        boolean indicatorIsBlue = indicatorTeamName.contains("blue") || indicatorTeamName.contains("син");

        if (playerIsRed && indicatorIsRed) return true;
        if (playerIsBlue && indicatorIsBlue) return true;

        return playerTeamName.equals(indicatorTeamName);
    }

    private static boolean isPlayerLeader(PlayerEntity player) {
        return player.experienceLevel > 0;
    }

    private static boolean isHoldingTool(PlayerEntity player) {
        return isConfigurator(player.getMainHandStack()) || isConfigurator(player.getOffHandStack());
    }

    private static void drawThickInsideBox(MatrixStack matrices, VertexConsumer quads, BlockPos pos, Vec3d cameraPos, float red, float green, float blue, Map<BlockPos, BoardCellType> cells) {
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        matrices.translate(pos.getX(), pos.getY(), pos.getZ());
        float thickness = 0.06f;
        for (Direction face : Direction.values()) {
            if (!shouldRenderFace(pos, face, cells)) {
                continue;
            }

            matrices.push();

            switch (face) {
                case DOWN -> {
                    matrices.translate(0, -0.002, 1);
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90));
                }
                case UP -> {
                    matrices.translate(0, 1.002, 0);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                }
                case SOUTH -> {
                    matrices.translate(1, 0, 1.002);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                }
                case WEST -> {
                    matrices.translate(-0.002, 0, 1);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                }
                case EAST -> {
                    matrices.translate(1.002, 0, 0);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90));
                }
                case NORTH -> {
                    matrices.translate(0, 0, -0.002);
                }
            }

            drawFaceOverlayQuads(matrices, quads, thickness, red, green, blue);

            matrices.pop();
        }

        matrices.pop();
    }

    private static boolean shouldRenderFace(BlockPos pos, Direction face, Map<BlockPos, BoardCellType> cells) {
        BlockPos neighbor = pos.offset(face);
        if (!cells.containsKey(neighbor)) {
            return true;
        }
        return comparePos(pos, neighbor) < 0;
    }

    private static int comparePos(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) {
            return Integer.compare(a.getY(), b.getY());
        }
        if (a.getZ() != b.getZ()) {
            return Integer.compare(a.getZ(), b.getZ());
        }
        return Integer.compare(a.getX(), b.getX());
    }

    private static void drawFaceOverlayQuads(MatrixStack matrices, VertexConsumer consumer,
                                             float thickness, float red, float green, float blue) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float alpha = 1.00f;

        consumer.vertex(matrix, 0, 0, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 1, 0, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 1, thickness, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 0, thickness, 0).color(red, green, blue, alpha).next();

        consumer.vertex(matrix, 0, 1-thickness, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 1, 1-thickness, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 1, 1, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 0, 1, 0).color(red, green, blue, alpha).next();

        consumer.vertex(matrix, 0, 0, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, thickness, 0, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, thickness, 1, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 0, 1, 0).color(red, green, blue, alpha).next();

        consumer.vertex(matrix, 1-thickness, 0, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 1, 0, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 1, 1, 0).color(red, green, blue, alpha).next();
        consumer.vertex(matrix, 1-thickness, 1, 0).color(red, green, blue, alpha).next();
    }

    private static void drawNeutralExtraInsideBox(MatrixStack matrices, VertexConsumer lines, BlockPos pos, Vec3d cameraPos, float red, float green, float blue) {
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        double[] expands = { 0.003, 0.006, 0.009, 0.012, 0.015, 0.018 };
        for (double expand : expands) {
            Box box = new Box(pos).expand(expand);
            drawBoxEdges(matrix, lines, box, red, green, blue, 1.0F);
        }

        matrices.pop();
    }

    private static void drawBoxEdges(Matrix4f m, VertexConsumer c, Box b,
                                     float r, float g, float bl, float a) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        line(m, c, x0, y0, z0, x1, y0, z0, r, g, bl, a);
        line(m, c, x1, y0, z0, x1, y0, z1, r, g, bl, a);
        line(m, c, x1, y0, z1, x0, y0, z1, r, g, bl, a);
        line(m, c, x0, y0, z1, x0, y0, z0, r, g, bl, a);
        line(m, c, x0, y1, z0, x1, y1, z0, r, g, bl, a);
        line(m, c, x1, y1, z0, x1, y1, z1, r, g, bl, a);
        line(m, c, x1, y1, z1, x0, y1, z1, r, g, bl, a);
        line(m, c, x0, y1, z1, x0, y1, z0, r, g, bl, a);
        line(m, c, x0, y0, z0, x0, y1, z0, r, g, bl, a);
        line(m, c, x1, y0, z0, x1, y1, z0, r, g, bl, a);
        line(m, c, x1, y0, z1, x1, y1, z1, r, g, bl, a);
        line(m, c, x0, y0, z1, x0, y1, z1, r, g, bl, a);
    }

    private static void line(Matrix4f m, VertexConsumer c, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        c.vertex(m, x1, y1, z1).color(r, g, b, a).next();
        c.vertex(m, x2, y2, z2).color(r, g, b, a).next();
    }

    private static boolean isConfigurator(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(ModItems.BOARD_CONFIGURATOR.getItem());
    }

    private static int teamColor(String teamName) {
        String normalized = teamName.toLowerCase(Locale.ROOT);
        if (normalized.contains("blue") || normalized.contains("син")) {
            return 0xFF2563EB;
        }
        if (normalized.contains("red") || normalized.contains("крас")) {
            return 0xFFDC2626;
        }
        return 0xFFEF4444;
    }

    private static void drawVoteBadge(MinecraftClient client, WorldRenderContext context, MatrixStack matrices,
                                      VertexConsumerProvider.Immediate vertexConsumers, Vec3d cameraPos,
                                      BoardClientState.VoteIndicator indicator, boolean seeThrough) {
        TextRenderer textRenderer = client.textRenderer;
        String text = voteText(indicator.count());
        int width = Math.max(14, textRenderer.getWidth(text) + 6);
        int height = 14;

        matrices.push();
        BlockPos displayPos = indicator.displayPos();
        Vec3d badgePos = badgePosition(displayPos, BoardClientState.fieldFor(displayPos), cameraPos, width, height);
        matrices.translate(badgePos.x - cameraPos.x, badgePos.y - cameraPos.y, badgePos.z - cameraPos.z);
        matrices.multiply(context.camera().getRotation());
        matrices.scale(-BADGE_SCALE, -BADGE_SCALE, BADGE_SCALE);

        drawBackground(matrices, vertexConsumers, width, height, teamColor(indicator.teamName()), seeThrough);

        matrices.translate(0.0F, 0.0F, -0.8F);
        int textX = (width - textRenderer.getWidth(text)) / 2;
        int textY = (height - 8) / 2;

        TextRenderer.TextLayerType layerType = seeThrough
                ? TextRenderer.TextLayerType.SEE_THROUGH
                : TextRenderer.TextLayerType.NORMAL;

        textRenderer.draw(text, textX + 1, textY + 1, 0xEE000000, false, matrices.peek().getPositionMatrix(),
                vertexConsumers, layerType, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        textRenderer.draw(text, textX, textY, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers,
                layerType, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        matrices.pop();
    }

    private static Vec3d badgePosition(BlockPos pos, List<BlockPos> field, Vec3d cameraPos, int width, int height) {
        boolean sameX = field.stream().mapToInt(BlockPos::getX).distinct().count() == 1;
        boolean sameZ = field.stream().mapToInt(BlockPos::getZ).distinct().count() == 1;
        double inset = 0.12;
        double faceOffset = 0.25;
        double worldBadgeHeight = height * BADGE_SCALE;
        double x = pos.getX() + 1.0 - inset;
        double y = pos.getY() + inset + worldBadgeHeight;
        double z = pos.getZ() + 1.0 + faceOffset;

        if (sameX) {
            boolean eastSide = cameraPos.x >= pos.getX() + 0.5;
            x = eastSide ? pos.getX() + 1.0 + faceOffset : pos.getX() - faceOffset;
            z = eastSide ? pos.getZ() + inset : pos.getZ() + 1.0 - inset;
        } else if (sameZ) {
            boolean southSide = cameraPos.z >= pos.getZ() + 0.5;
            z = southSide ? pos.getZ() + 1.0 + faceOffset : pos.getZ() - faceOffset;
            x = southSide ? pos.getX() + 1.0 - inset : pos.getX() + inset;
        }

        return new Vec3d(x, y, z);
    }

    private static String voteText(int count) {
        int clamped = Math.max(1, Math.min(4, count));
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= clamped; i++) {
            if (i > 1) {
                builder.append(' ');
            }
            builder.append(i);
        }
        return builder.toString();
    }

    private static void drawBackground(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int width,
                                       int height, int color, boolean seeThrough) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer consumer = vertexConsumers.getBuffer(seeThrough
                ? RenderLayer.getTextBackgroundSeeThrough()
                : RenderLayer.getTextBackground());

        float a = 1.0F;
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        consumer.vertex(matrix, 0, height, 0).color(r, g, b, a).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).next();
        consumer.vertex(matrix, width, height, 0).color(r, g, b, a).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).next();
        consumer.vertex(matrix, width, 0, 0).color(r, g, b, a).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).next();
        consumer.vertex(matrix, 0, 0, 0).color(r, g, b, a).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).next();
    }
}