package net.abakath.rpg4fools.network.packets.s2c;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.models.DayData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Sends the current in game date to a player. This class is loaded on both sides, so it must not
 * reference any client only type. The receiving side lives in
 * {@link net.abakath.rpg4fools.client.ClientModMessages}.
 */
public class SeasonUpdatePacket implements CustomPayload {
  public DayData dayData;

  public static final Id<SeasonUpdatePacket> ID = CustomPayload.id(new Identifier(RPG4Fools.MOD_ID, "season_update").toString());
  public static final PacketCodec<PacketByteBuf, SeasonUpdatePacket> CODEC = PacketCodec.of((value, buf) -> {
    buf.writeNullable(value.dayData, (buffer, data) -> {
      buffer.writeInt(data.getYear());
      buffer.writeInt(data.getMonth().ordinal());
      buffer.writeInt(data.getDay());
      buffer.writeLong(data.getDayTime());
    });
  }, buf -> new SeasonUpdatePacket(buf.readNullable(buffer -> new DayData(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readLong()))));

  public SeasonUpdatePacket(DayData dayData) {
    this.dayData = dayData;
  }

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
