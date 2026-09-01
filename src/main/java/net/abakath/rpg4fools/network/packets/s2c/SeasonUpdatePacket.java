package net.abakath.rpg4fools.network.packets.s2c;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.models.DayData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sends the current in game date to a player. This class is loaded on both sides, so it must not
 * reference any client only type. The receiving side lives in
 * {@link net.abakath.rpg4fools.client.ClientModMessages}.
 */
public class SeasonUpdatePacket implements CustomPacketPayload {
  public DayData dayData;

  public static final Type<SeasonUpdatePacket> ID = CustomPacketPayload.createType(Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, "season_update").toString());
  public static final StreamCodec<FriendlyByteBuf, SeasonUpdatePacket> CODEC = StreamCodec.ofMember((value, buf) -> {
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
  public Type<? extends CustomPacketPayload> type() {
    return ID;
  }
}
