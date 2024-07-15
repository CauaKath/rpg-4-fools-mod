package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.network.packets.s2c.SeasonUpdatePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ModMessages {

  public static void registerC2SPackets() {

  }

  public static void registerS2CPackets() {
    PayloadTypeRegistry.playS2C().register(SeasonUpdatePacket.ID, SeasonUpdatePacket.CODEC);
    ClientPlayNetworking.registerGlobalReceiver(SeasonUpdatePacket.ID, SeasonUpdatePacket::receive);
  }
}
