package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.network.packets.s2c.SeasonUpdatePacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ModMessages {

  public static void registerC2SPackets() {

  }

  /**
   * Registers the payload type on both sides. The client side receiver lives in
   * {@link net.abakath.rpg4fools.client.ClientModMessages}, since it is only available on the
   * client.
   */
  public static void registerS2CPackets() {
    PayloadTypeRegistry.clientboundPlay().register(SeasonUpdatePacket.ID, SeasonUpdatePacket.CODEC);
  }
}
