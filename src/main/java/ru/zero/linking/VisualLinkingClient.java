package ru.zero.linking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * CloudVisuals: the original client polled the ZeroVisuals author's server with the
 * player's nickname and server address to unlock "Prime" features. That network call
 * is removed; this stub keeps the API so the GUI compiles, and reports no Prime access.
 */
@Environment(EnvType.CLIENT)
public final class VisualLinkingClient {
   public enum PrimeState {
      UNKNOWN,
      PRIME,
      NOT_PRIME
   }

   public String getDisplayRank() {
      return "Free";
   }

   public boolean isPrimeOnTargetServer() {
      return false;
   }

   public boolean isServerApiAuthorized() {
      return false;
   }
}
