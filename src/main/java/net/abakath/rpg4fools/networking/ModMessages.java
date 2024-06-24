package net.abakath.rpg4fools.networking;

import net.abakath.rpg4fools.networking.packets.SeasonUpdateS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ModMessages {

  public static void registerC2SPackets() {

  }

  public static void registerS2CPackets() {
    PayloadTypeRegistry.playS2C().register(SeasonUpdateS2CPacket.ID, SeasonUpdateS2CPacket.CODEC);
    ClientPlayNetworking.registerGlobalReceiver(SeasonUpdateS2CPacket.ID, SeasonUpdateS2CPacket::receive);
  }
}
