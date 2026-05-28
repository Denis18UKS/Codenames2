package fable.codenames.board;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BoardState extends PersistentState {
    public static final String KEY = "codenames_board";

    private final LinkedHashMap<BlockPos, BoardCellType> cells = new LinkedHashMap<>();
    private final java.util.List<java.util.List<BlockPos>> fields = new java.util.ArrayList<>();

    public static BoardState createFromNbt(NbtCompound nbt) {
        BoardState state = new BoardState();
        NbtList cellsList = nbt.getList("cells", 10);
        for (int i = 0; i < cellsList.size(); i++) {
            NbtCompound cell = cellsList.getCompound(i);
            BlockPos pos = new BlockPos(cell.getInt("x"), cell.getInt("y"), cell.getInt("z"));
            BoardCellType type = BoardCellType.fromId(cell.getString("type"));
            state.cells.put(pos, type);
        }
        NbtList fieldsList = nbt.getList("fields", 9);
        for (int i = 0; i < fieldsList.size(); i++) {
            NbtList fieldList = fieldsList.getList(i);
            java.util.List<BlockPos> positions = new java.util.ArrayList<>();
            for (int j = 0; j < fieldList.size(); j++) {
                NbtCompound posNbt = fieldList.getCompound(j);
                positions.add(new BlockPos(posNbt.getInt("x"), posNbt.getInt("y"), posNbt.getInt("z")));
            }
            if (!positions.isEmpty() && (state.fields.isEmpty() || positions.size() == state.fields.get(0).size())) {
                state.fields.add(List.copyOf(positions));
            }
        }
        if (state.fields.isEmpty() && !state.cells.isEmpty()) {
            state.fields.add(List.copyOf(state.cells.keySet()));
        }
        return state;
    }

    public Map<BlockPos, BoardCellType> getCells() {
        return Map.copyOf(this.cells);
    }

    public List<BlockPos> getOrderedPositions() {
        return List.copyOf(this.cells.keySet());
    }

    public List<List<BlockPos>> getFields() {
        if (this.fields.isEmpty() && !this.cells.isEmpty()) {
            return List.of(List.copyOf(this.cells.keySet()));
        }
        return this.fields.stream().map(List::copyOf).toList();
    }

    public List<BlockPos> getPrimaryFieldPositions() {
        return List.copyOf(primaryField());
    }

    public BlockPos resolvePosition(BlockPos pos) {
        if (this.cells.containsKey(pos)) {
            return pos.toImmutable();
        }

        java.util.List<BlockPos> primary = primaryField();
        for (java.util.List<BlockPos> field : this.fields) {
            for (int i = 0; i < field.size() && i < primary.size(); i++) {
                if (field.get(i).equals(pos)) {
                    BlockPos canonical = primary.get(i);
                    return this.cells.containsKey(canonical) ? canonical : null;
                }
            }
        }
        return null;
    }

    public List<BlockPos> getLinkedPositions(BlockPos pos) {
        BlockPos canonical = resolvePosition(pos);
        if (canonical == null) {
            return List.of();
        }

        int index = primaryField().indexOf(canonical);
        if (index < 0) {
            return List.of(canonical);
        }

        java.util.List<BlockPos> linked = new java.util.ArrayList<>();
        for (java.util.List<BlockPos> field : getFields()) {
            if (index < field.size()) {
                linked.add(field.get(index));
            }
        }
        if (linked.isEmpty()) {
            linked.add(canonical);
        }
        return List.copyOf(linked);
    }

    public BoardCellType getType(BlockPos pos) {
        BlockPos canonical = resolvePosition(pos);
        return canonical == null ? BoardCellType.UNASSIGNED : this.cells.getOrDefault(canonical, BoardCellType.UNASSIGNED);
    }

    public boolean contains(BlockPos pos) {
        return resolvePosition(pos) != null;
    }

    public int size() {
        return this.cells.size();
    }

    public boolean repairCellsFromFields() {
        java.util.List<BlockPos> primary = primaryField();
        if (primary.isEmpty() || this.cells.size() == primary.size()) {
            return false;
        }

        LinkedHashMap<BlockPos, BoardCellType> repaired = new LinkedHashMap<>();
        for (BlockPos pos : primary) {
            repaired.put(pos.toImmutable(), this.cells.getOrDefault(pos, BoardCellType.UNASSIGNED));
        }
        this.cells.clear();
        this.cells.putAll(repaired);
        markDirty();
        return true;
    }

    public void setBoard(List<BlockPos> positions) {
        java.util.List<BlockPos> immutablePositions = positions.stream().map(BlockPos::toImmutable).toList();
        if (this.cells.isEmpty() || samePositions(getOrderedPositions(), immutablePositions)) {
            this.cells.clear();
            for (BlockPos pos : immutablePositions) {
                this.cells.put(pos, BoardCellType.UNASSIGNED);
            }
            this.fields.clear();
            this.fields.add(immutablePositions);
            markDirty();
            return;
        }

        if (immutablePositions.size() != this.cells.size()) {
            return;
        }
        for (java.util.List<BlockPos> field : this.fields) {
            if (samePositions(field, immutablePositions)) {
                return;
            }
        }
        this.fields.add(immutablePositions);
        markDirty();
    }

    public void setCells(Map<BlockPos, BoardCellType> cells) {
        this.cells.clear();
        cells.forEach((pos, type) -> this.cells.put(pos.toImmutable(), type));
        if (this.fields.isEmpty()) {
            this.fields.add(List.copyOf(this.cells.keySet()));
        }
        markDirty();
    }

    public void clear() {
        this.cells.clear();
        this.fields.clear();
        markDirty();
    }

    public void setType(BlockPos pos, BoardCellType type) {
        BlockPos canonical = resolvePosition(pos);
        if (canonical == null) {
            return;
        }
        this.cells.put(canonical, type);
        markDirty();
    }

    public void removeCell(BlockPos pos) {
        BlockPos canonical = resolvePosition(pos);
        if (canonical != null && this.cells.containsKey(canonical)) {
            this.cells.put(canonical, BoardCellType.UNASSIGNED);
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList cellsList = new NbtList();
        for (Map.Entry<BlockPos, BoardCellType> entry : this.cells.entrySet()) {
            NbtCompound cell = new NbtCompound();
            cell.putInt("x", entry.getKey().getX());
            cell.putInt("y", entry.getKey().getY());
            cell.putInt("z", entry.getKey().getZ());
            cell.put("type", NbtString.of(entry.getValue().getId()));
            cellsList.add(cell);
        }
        nbt.put("cells", cellsList);
        NbtList fieldsList = new NbtList();
        for (java.util.List<BlockPos> field : getFields()) {
            NbtList fieldList = new NbtList();
            for (BlockPos pos : field) {
                NbtCompound posNbt = new NbtCompound();
                posNbt.putInt("x", pos.getX());
                posNbt.putInt("y", pos.getY());
                posNbt.putInt("z", pos.getZ());
                fieldList.add(posNbt);
            }
            fieldsList.add(fieldList);
        }
        nbt.put("fields", fieldsList);
        return nbt;
    }

    private static boolean samePositions(List<BlockPos> first, List<BlockPos> second) {
        return first.size() == second.size() && first.equals(second);
    }

    private java.util.List<BlockPos> primaryField() {
        if (!this.fields.isEmpty()) {
            return this.fields.get(0);
        }
        return List.copyOf(this.cells.keySet());
    }
}
