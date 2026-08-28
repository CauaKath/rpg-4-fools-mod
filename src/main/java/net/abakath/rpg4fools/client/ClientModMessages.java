package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.network.packets.s2c.SeasonUpdatePacket;
import net.abakath.rpg4fools.utils.IEntityDataSaver;
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

    context.client().execute(() -> {
      IEntityDataSaver player = (IEntityDataSaver) context.player();
      player.getPersistentData().putInt("rpg4fools.year", packet.dayData.getYear());
      player.getPersistentData().putInt("rpg4fools.month", packet.dayData.getMonth().ordinal());
      player.getPersistentData().putInt("rpg4fools.day", packet.dayData.getDay());
      player.getPersistentData().putLong("rpg4fools.dayTime", packet.dayData.getDayTime());

      ClientSeasonState.update(packet.dayData.getMonth().ordinal());
    });
  }
}
