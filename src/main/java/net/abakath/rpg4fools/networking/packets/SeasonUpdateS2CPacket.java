package net.abakath.rpg4fools.networking.packets;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.models.DayData;
import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class SeasonUpdateS2CPacket implements CustomPayload {
  public DayData dayData;

  public static final Id<SeasonUpdateS2CPacket> ID = CustomPayload.id(new Identifier(RPG4Fools.MOD_ID, "season_update").toString());
  public static final PacketCodec<PacketByteBuf, SeasonUpdateS2CPacket> CODEC = PacketCodec.of((value, buf) -> {
    buf.writeNullable(value.dayData, (buffer, data) -> {
      buffer.writeInt(data.getYear());
      buffer.writeInt(data.getMonth().ordinal());
      buffer.writeInt(data.getDay());
    });
  }, buf -> new SeasonUpdateS2CPacket(buf.readNullable(buffer -> new DayData(buffer.readInt(), buffer.readInt(), buffer.readInt()))));

  public SeasonUpdateS2CPacket(DayData dayData) {
    this.dayData = dayData;
  }

  public static void receive(SeasonUpdateS2CPacket packet, ClientPlayNetworking.Context context) {
    IEntityDataSaver player = (IEntityDataSaver) context.player();
    player.getPersistentData().putInt("rpg4fools.year", packet.dayData.getYear());
    player.getPersistentData().putInt("rpg4fools.month", packet.dayData.getMonth().ordinal());
    player.getPersistentData().putInt("rpg4fools.day", packet.dayData.getDay());
  }

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
