package fable.codenames.chat;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public record TeamChatMessage(UUID senderUuid, String senderName, String teamName, String content, long sentAtMillis) {
    public static TeamChatMessage fromNbt(NbtCompound nbt) {
        return new TeamChatMessage(
                nbt.getUuid("senderUuid"),
                nbt.getString("senderName"),
                nbt.getString("teamName"),
                nbt.getString("content"),
                nbt.getLong("sentAtMillis"));
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("senderUuid", this.senderUuid);
        nbt.putString("senderName", this.senderName);
        nbt.putString("teamName", this.teamName);
        nbt.putString("content", this.content);
        nbt.putLong("sentAtMillis", this.sentAtMillis);
        return nbt;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.senderUuid);
        buf.writeString(this.senderName);
        buf.writeString(this.teamName);
        buf.writeString(this.content);
        buf.writeLong(this.sentAtMillis);
    }

    public static TeamChatMessage read(PacketByteBuf buf) {
        return new TeamChatMessage(
                buf.readUuid(),
                buf.readString(),
                buf.readString(),
                buf.readString(512),
                buf.readLong());
    }
}
