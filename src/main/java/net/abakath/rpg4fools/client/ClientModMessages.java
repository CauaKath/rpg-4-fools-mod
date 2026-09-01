package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.network.packets.s2c.SeasonUpdatePacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client side packet receivers. Kept out of {@link net.abakath.rpg4fools.init.ModMessages} so the
 * common entrypoint never touches {@link ClientPlayNetworking}, which does not exist on a
 * dedicated server.
 */
@Environment(EnvType.CLIENT)
public final class ClientModMessages {
  private ClientModMessages() {
  }

  public static void registerS2CReceivers() {
    ClientPlayNetworking.registerGlobalReceiver(SeasonUpdatePacket.ID, ClientModMessages::onSeasonUpdate);
  }

  private static void onSeasonUpdate(SeasonUpdatePacket packet, ClientPlayNetworking.Context context) {
    if (packet.dayData == null) {
      return;
    }

    context.client().execute(() -> ClientSeasonState.update(
            packet.dayData.getYear(),
            packet.dayData.getMonth().ordinal(),
            packet.dayData.getDay()
    ));
  }
}
