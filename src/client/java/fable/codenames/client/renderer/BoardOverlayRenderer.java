package fable.codenames.client.renderer;

import fable.codenames.board.BoardCellType;
import fable.codenames.client.board.BoardClientState;
import fable.codenames.item.ModItems;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BoardOverlayRenderer {
    private static final float BADGE_SCALE = 0.014F;
    // Константа для второй фазы рендера (AFTER_TRANSLUCENT) 
    // чтобы точно быть поверх всего после Sodium
    private static final RenderLayer OVERLAY_LINES = RenderLayer.getLines();
    private static final RenderLayer OVERLAY_QUADS = RenderLayer.getDebugQuads();
    
    // Правильный способ получить RenderLayer для текста с SEE_THROUGH в 1.20.1
    private static final RenderLayer TEXT_SEE_THROUGH = RenderLayer.getTextSeeThrough(new Identifier("codenames", "text/see_through"));

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
        
        // Используем провайдер буферов напрямую, но с правильным порядком отрисовки
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        
        boolean showCells = !cells.isEmpty() && (isHoldingTool(player) || BoardClientState.canSeeAnswers());
        boolean showVotes = !voteIndicators.isEmpty();
        
        if (!showCells && !showVotes) {
            return;
        }

        // РИСУЕМ ЯЧЕЙКИ СНАЧАЛА
        if (showCells) {
            matrices.push();
            
            // Получаем буферы ДО отрисовки
            VertexConsumer lines = consumers.getBuffer(OVERLAY_LINES);
            VertexConsumer quads = consumers.getBuffer(OVERLAY_QUADS);
            
            for (Map.Entry<BlockPos, BoardCellType> entry : cells.entrySet()) {
                if (entry.getValue() == BoardCellType.UNASSIGNED) {
                    continue;
                }

                int color = entry.getValue().getColor();
                float red = ((color >> 16) & 255) / 255.0F;
                float green = ((color >> 8) & 255) / 255.0F;
                float blue = (color & 255) / 255.0F;

                // Рисуем основную рамку
                drawThickInsideBox(matrices, quads, entry.getKey(), cameraPos, red, green, blue, cells);
                
                // Рисуем дополнительные линии для нейтральных ячеек
                if (entry.getValue() == BoardCellType.NEUTRAL) {
                    drawNeutralExtraInsideBox(matrices, lines, entry.getKey(), cameraPos, red, green, blue);
                }
            }
            
            matrices.pop();
            
            // ВАЖНО: Рисуем quads ПЕРЕД lines
            consumers.draw(OVERLAY_QUADS);
            consumers.draw(OVERLAY_LINES);
        }

        // РИСУЕМ ИНДИКАТОРЫ ГОЛОСОВАНИЯ ОТДЕЛЬНО
        if (showVotes) {
            matrices.push();
            
            for (BoardClientState.VoteIndicator indicator : voteIndicators) {
                if (canPlayerSeeVoteIndicator(player, indicator)) {
                    drawVoteBadge(client, context, matrices, consumers, cameraPos, indicator);
                }
            }
            
            matrices.pop();
            
            // Рисуем текст и бейджи в правильном порядке
            consumers.draw(RenderLayer.getTextBackgroundSeeThrough());
            consumers.draw(TEXT_SEE_THROUGH);
        }
        
        // Финальная отрисовка всех оставшихся буферов
        consumers.draw();
    }

    /**
     * Проверяет, может ли игрок видеть индикатор голосования.
     * Индикатор виден только отгадывающим своей команды.
     * Лидеры не видят индикаторы своей команды.
     * Игроки не видят индикаторы чужой команды.
     */
    private static boolean canPlayerSeeVoteIndicator(PlayerEntity player, BoardClientState.VoteIndicator indicator) {
        AbstractTeam playerTeam = player.getScoreboardTeam();
        if (playerTeam == null) {
            return false;
        }
        
        // Лидеры не видят индикаторы своей команды сквозь блоки
        if (isPlayerLeader(player)) {
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
     * Проверяет, является ли игрок лидером.
     * Лидеры имеют experienceLevel > 0 (используется для XP-бара).
     */
    private static boolean isPlayerLeader(PlayerEntity player) {
        return player.experienceLevel > 0;
    }

    private static boolean isHoldingTool(PlayerEntity player) {
        return isConfigurator(player.getMainHandStack()) || isConfigurator(player.getOffHandStack());
    }

    private static void drawThickInsideBox(MatrixStack matrices, VertexConsumer quads, BlockPos pos, 
                                          Vec3d cameraPos, float red, float green, float blue, 
                                          Map<BlockPos, BoardCellType> cells) {
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        matrices.translate(pos.getX(), pos.getY(), pos.getZ());

        float thickness = 0.06f;

        for (Direction face : Direction.values()) {
            if (!shouldRenderFace(pos, face, cells)) {
                continue;
            }

            matrices.push();

            // Трансформация для каждой грани
            switch (face) {
                case DOWN -> {
                    matrices.translate(0, 0, 1);
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90));
                }
                case UP -> {
                    matrices.translate(0, 1, 0);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                }
                case SOUTH -> {
                    matrices.translate(1, 0, 1);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                }
                case WEST -> {
                    matrices.translate(0, 0, 1);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                }
                case EAST -> {
                    matrices.translate(1, 0, 0);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90));
                }
                default -> {}
            }

            // Рисуем грани в quads (DebugQuads) для лучшей видимости с Sodium
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

    /**
     * Рисует перекрытие граней используя quads (DebugQuads буфер)
     * Это более совместимо с Sodium, чем смешанное использование линий и quads
     */
    private static void drawFaceOverlayQuads(MatrixStack matrices, VertexConsumer consumer, 
                                            float thickness, float red, float green, float blue) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float alpha = 1.0f;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        
        // Рисуем тонкую рамку используя quads (альтернатива drawBox)
        // Каждая линия - это прямоугольник заданной толщины
        
        // Нижняя линия (y = 0, от x=0 до x=1)
        consumer.vertex(matrix, 0, 0, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 1, 0, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 1, thickness, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 0, thickness, 0).color(red, green, blue, alpha).light(light).next();
        
        // Верхняя линия (y = 1, от x=0 до x=1)
        consumer.vertex(matrix, 0, 1-thickness, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 1, 1-thickness, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 1, 1, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 0, 1, 0).color(red, green, blue, alpha).light(light).next();
        
        // Левая линия (x = 0, от y=0 до y=1)
        consumer.vertex(matrix, 0, 0, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, thickness, 0, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, thickness, 1, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 0, 1, 0).color(red, green, blue, alpha).light(light).next();
        
        // Правая линия (x = 1, от y=0 до y=1)
        consumer.vertex(matrix, 1-thickness, 0, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 1, 0, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 1, 1, 0).color(red, green, blue, alpha).light(light).next();
        consumer.vertex(matrix, 1-thickness, 1, 0).color(red, green, blue, alpha).light(light).next();
    }

    private static void drawNeutralExtraInsideBox(MatrixStack matrices, VertexConsumer lines, BlockPos pos,
                                                  Vec3d cameraPos,
                                                  float red, float green, float blue) {
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        double[] contracts = { 0.075, 0.081, 0.087, 0.093, 0.099, 0.105 };
        for (double contract : contracts) {
            Box box = new Box(pos).contract(contract);
            // Рисуем Box используя линии через WorldRenderer с правильными координатами
            WorldRenderer.drawBox(matrices, lines, 
                box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ,
                red, green, blue, 1.0F);
        }
        
        matrices.pop();
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
                                      BoardClientState.VoteIndicator indicator) {
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

        // Фон с SEE_THROUGH — видно своей команде сквозь блоки
        drawBackground(matrices, vertexConsumers, width, height, teamColor(indicator.teamName()), true);
        
        matrices.translate(0.0F, 0.0F, -0.8F);
        int textX = (width - textRenderer.getWidth(text)) / 2;
        int textY = (height - 8) / 2;
        
        // Текст с SEE_THROUGH — видно своей команде сквозь блоки
        textRenderer.draw(text, textX + 1, textY + 1, 0xEE000000, false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        textRenderer.draw(text, textX, textY, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers,
                TextRenderer.TextLayerType.SEE_THROUGH, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        matrices.pop();
    }

    private static Vec3d badgePosition(BlockPos pos, List<BlockPos> field, Vec3d cameraPos, int width, int height) {
        boolean sameX = field.stream().mapToInt(BlockPos::getX).distinct().count() == 1;
        boolean sameZ = field.stream().mapToInt(BlockPos::getZ).distinct().count() == 1;
        double inset = 0.12;
        double faceOffset = 0.12;
        double worldBadgeHeight = height * BADGE_SCALE;
        double x = pos.getX() + 1.0 - inset;
        double y = pos.getY() + inset + worldBadgeHeight;
        double z = pos.getZ() + 1.0 - inset;

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