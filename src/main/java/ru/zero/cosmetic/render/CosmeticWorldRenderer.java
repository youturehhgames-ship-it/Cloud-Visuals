package ru.zero.cosmetic.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import ru.zero.cosmetic.Cosmetic;
import ru.zero.cosmetic.CosmeticSlot;
import ru.zero.cosmetic.CosmeticState;
import ru.zero.cosmetic.model.BedrockModel;
import ru.zero.cosmetic.model.ModelQuad;
import ru.zero.event.EventInit;
import ru.zero.event.render.WorldRenderEvent;
import ru.zero.util.render.world.WorldRenderer;

/**
 * Attaches equipped wings / backpacks / hats to the local player in the world.
 * Placement constants are a best-effort port of EvoVis' attachment offsets and may
 * need tuning in-game.
 */
@Environment(EnvType.CLIENT)
public final class CosmeticWorldRenderer {
   private static final CosmeticWorldRenderer INSTANCE = new CosmeticWorldRenderer();
   private static final Map<String, Identifier> TEXTURE_IDS = new HashMap<>();

   private CosmeticWorldRenderer() {
   }

   // CloudVisuals diagnostics: each distinct message is printed once.
   private static final java.util.Set<String> LOGGED = new java.util.HashSet<>();

   private static void logOnce(String message) {
      if (LOGGED.add(message)) {
         System.out.println("[CloudVisuals][Cosmetics] " + message);
      }
   }

   public static CosmeticWorldRenderer getInstance() {
      return INSTANCE;
   }

   @EventInit
   public void onWorldRender(WorldRenderEvent event) {
      MinecraftClient client = event.client();
      if (client.world == null || client.player == null) {
         return;
      }
      logOnce("world render event received");
      if (!CosmeticState.isEnabled()) {
         logOnce("skipped: cosmetics disabled (Cosmetics module off)");
         return;
      }
      if (client.options.getPerspective() == Perspective.FIRST_PERSON) {
         logOnce("skipped: first person (wings/backpacks/hats render only in F5)");
         return;
      }

      WorldRenderer renderer = event.worldRenderer();
      float partial = renderer.tickDelta();
      Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
      PlayerEntity player = client.player;

      for (CosmeticSlot slot : new CosmeticSlot[] { CosmeticSlot.WINGS, CosmeticSlot.BACKPACKS, CosmeticSlot.HATS }) {
         Cosmetic cosmetic = CosmeticState.equipped(slot);
         if (cosmetic != null) {
            logOnce("equipped " + slot + " = " + cosmetic.key + " (texture " + cosmetic.texture + ")");
            render(renderer, client, player, cosmetic, slot, partial, camera);
         } else {
            logOnce("nothing equipped in " + slot);
         }
      }
   }

   private void render(WorldRenderer renderer, MinecraftClient client, PlayerEntity player, Cosmetic cosmetic,
         CosmeticSlot slot, float partial, Vec3d camera) {
      BedrockModel model = CosmeticRenderer.model(cosmetic);
      if (model == null) {
         logOnce("FAIL " + cosmetic.key + ": model could not be loaded");
         return;
      }

      Identifier texture = textureId(cosmetic);
      if (texture == null) {
         logOnce("FAIL " + cosmetic.key + ": no texture");
         return;
      }

      float time = (player.age + partial) / 20.0F;
      List<ModelQuad> quads = CosmeticRenderer.flattened(cosmetic, time);
      if (quads.isEmpty()) {
         logOnce("FAIL " + cosmetic.key + ": model has no quads");
         return;
      }
      logOnce("drawing " + cosmetic.key + ": " + quads.size() + " quads, texture " + texture);

      double x = player.lastRenderX + (player.getX() - player.lastRenderX) * partial;
      double y = player.lastRenderY + (player.getY() - player.lastRenderY) * partial;
      double z = player.lastRenderZ + (player.getZ() - player.lastRenderZ) * partial;

      float yaw = player.getBodyYaw();
      if (slot == CosmeticSlot.HATS) {
         yaw = player.getYaw(partial);
      }

      float baseY;
      float baseZ = 0.0F;
      if (slot == CosmeticSlot.HATS) {
         baseY = player.getHeight() - 0.35F - (player.isSneaking() ? 0.2F : 0.0F);
      } else if (slot == CosmeticSlot.BACKPACKS) {
         baseY = 1.0F + cosmetic.offsetY;
      } else {
         baseY = 1.15F + cosmetic.offsetY / 16.0F;
         baseZ = cosmetic.offsetZ / 16.0F;
      }

      float scale = cosmetic.scale;
      double yawRad = Math.toRadians(yaw);
      float cos = (float) Math.cos(yawRad);
      float sin = (float) Math.sin(yawRad);
      float rotX = (float) Math.toRadians(cosmetic.rotationX);
      float rotY = (float) Math.toRadians(cosmetic.rotationY);
      float rotZ = (float) Math.toRadians(cosmetic.rotationZ);

      double bx0 = 1e9, by0 = 1e9, bz0 = 1e9, bx1 = -1e9, by1 = -1e9, bz1 = -1e9;
      for (ModelQuad quad : quads) {
         Vec3d[] points = new Vec3d[4];
         for (int i = 0; i < 4; i++) {
            float[] local = rotateZYX(quad.pos[i][0] * scale, quad.pos[i][1] * scale, quad.pos[i][2] * scale,
                  rotX, rotY, rotZ);
            float lx = local[0];
            float ly = local[1];
            float lz = local[2];
            float wx = lx * cos - lz * sin;
            float wz = lx * sin + lz * cos;
            // CloudVisuals: the emitter subtracts the camera position itself, pass absolute world coords
            points[i] = new Vec3d(x + wx, y + baseY + ly, z + wz + baseZ);
            bx0 = Math.min(bx0, wx); bx1 = Math.max(bx1, wx);
            by0 = Math.min(by0, baseY + ly); by1 = Math.max(by1, baseY + ly);
            bz0 = Math.min(bz0, wz + baseZ); bz1 = Math.max(bz1, wz + baseZ);
         }

         renderer.drawTexturedQuad(texture, points[0], points[1], points[2], points[3],
               u(quad, model, 0), v(quad, model, 0),
               u(quad, model, 1), v(quad, model, 1),
               u(quad, model, 2), v(quad, model, 2),
               u(quad, model, 3), v(quad, model, 3),
               0xFFFFFFFF, true);
      }
      if (LOGGED.add("bounds:" + cosmetic.key)) logOnce(String.format(java.util.Locale.ROOT, "bounds %s rel. feet (blocks): x[%.2f..%.2f] y[%.2f..%.2f] z[%.2f..%.2f], scale %.2f, camera dist %.2f", cosmetic.key, bx0, bx1, by0, by1, bz0, bz1, scale, Math.sqrt(Math.pow(x - camera.x, 2) + Math.pow(y - camera.y, 2) + Math.pow(z - camera.z, 2))));
   }

   private static float[] rotateZYX(float x, float y, float z, float rx, float ry, float rz) {
      if (rx == 0.0F && ry == 0.0F && rz == 0.0F) {
         return new float[] { x, y, z };
      }
      org.joml.Matrix4f m = new org.joml.Matrix4f().rotateZYX(rz, ry, rx);
      org.joml.Vector3f v = new org.joml.Vector3f(x, y, z);
      m.transformPosition(v);
      return new float[] { v.x, v.y, v.z };
   }

   private static float u(ModelQuad quad, BedrockModel model, int i) {
      return quad.uv[i][0] / model.textureWidth;
   }

   private static float v(ModelQuad quad, BedrockModel model, int i) {
      return quad.uv[i][1] / model.textureHeight;
   }

   private static Identifier textureId(Cosmetic cosmetic) {
      if (cosmetic.texture == null) {
         return null;
      }

      return TEXTURE_IDS.computeIfAbsent(cosmetic.texture, path -> {
         String relative = path.startsWith("assets/zero/") ? path.substring("assets/zero/".length()) : path;
         return Identifier.of("zero", relative);
      });
   }
}
