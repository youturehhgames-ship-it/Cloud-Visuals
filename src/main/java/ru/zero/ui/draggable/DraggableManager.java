package ru.zero.ui.draggable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import ru.zero.Zero;
import ru.zero.util.render.animation.AnimationSystem;
import ru.zero.util.render.animation.Easings;
import ru.zero.util.render.animation.SpringConfig;
import ru.zero.util.render.animation.SpringFloatAnimator;
import ru.zero.util.render.core.Renderer2D;
import ru.zero.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
// CloudVisuals: HUD editor reworked after LiquidBounce's editor — 10px grid, magnetic snapping
// with guide lines (other elements' edges/centers and screen center), 3x3 anchor zones so elements
// keep their place on resize, live coordinates. Hold Shift while dragging to move freely.
public final class DraggableManager {
   private static final float GRID_BASE = 10.0F;
   private static final float MAGNET_BASE = 6.0F;
   private float uiScale = 1.0F;
   private boolean snapBypass;
   private Map<String, float[]> frameRects = new HashMap<>();
   private Map<String, float[]> lastFrameRects = new HashMap<>();
   private final Map<String, int[]> dragZones = new HashMap<>();
   private String activeDragId;
   private float activeX;
   private float activeY;
   private float activeW;
   private float activeH;
   private int activeZoneH;
   private int activeZoneV;
   private Float guideX;
   private Float guideY;
   private static final DraggableManager INSTANCE = new DraggableManager();
   private static final float DRAGGING_ALPHA_MULTIPLIER = 0.65F;
   private static final float RESTING_ALPHA_MULTIPLIER = 1.0F;
   private static final float MIN_ALPHA_MULTIPLIER = 0.0F;
   private static final float MAX_ALPHA_MULTIPLIER = 1.0F;
   private static final float ALPHA_POSITION_TOLERANCE = 5.0E-4F;
   private static final float ALPHA_VELOCITY_TOLERANCE = 5.0E-4F;
   private static final float ALPHA_APPLY_EPSILON = 0.001F;
   private static final SpringConfig DRAG_ALPHA_SPRING = SpringConfig.of(2.5F, 0.9F);
   private static final double[] SCRATCH_CX = new double[1];
   private static final double[] SCRATCH_CY = new double[1];
   private final Map<String, DraggableManager.DragState> dragStates = new HashMap<>();
   private final Map<String, DraggableManager.NormalizedPosition> persistedPositions = new ConcurrentHashMap<>();
   private final Set<String> yPinnedIds = ConcurrentHashMap.newKeySet();
   private Renderer2D renderer;
   private boolean frameActive;
   private float cursorX;
   private float cursorY;
   private boolean pointerValid;
   private boolean pointerPressed;
   private boolean uiOpen;
   private int viewportWidth;
   private int viewportHeight;

   private DraggableManager() {
   }

   public static DraggableManager getInstance() {
      return INSTANCE;
   }

   public void beginFrame(MinecraftClient client, Renderer2D renderer, int viewportWidth, int viewportHeight) {
      this.renderer = Objects.requireNonNull(renderer, "renderer");
      this.viewportWidth = Math.max(0, viewportWidth);
      this.viewportHeight = Math.max(0, viewportHeight);
      this.frameActive = true;
      this.lastFrameRects = this.frameRects;
      this.frameRects = new HashMap<>();
      this.activeDragId = null;
      this.guideX = null;
      this.guideY = null;
      this.uiScale = 1.0F;
      this.snapBypass = false;
      if (client != null && client.getWindow() != null) {
         this.uiScale = Math.max(0.5F, (float) client.getWindow().getScaleFactor() / 2.0F);
         long kh = client.getWindow().getHandle();
         if (kh != 0L) {
            this.snapBypass = GLFW.glfwGetKey(kh, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
               || GLFW.glfwGetKey(kh, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
         }
      }
      this.pointerValid = false;
      this.pointerPressed = false;
      this.uiOpen = false;
      this.cursorX = 0.0F;
      this.cursorY = 0.0F;
      if (client != null) {
         this.uiOpen = client.currentScreen != null;
         Window window = client.getWindow();
         if (window != null) {
            long handle = window.getHandle();
            if (handle != 0L) {
                double[] cx = SCRATCH_CX;
                double[] cy = SCRATCH_CY;
                GLFW.glfwGetCursorPos(handle, cx, cy);
                double rawX = cx[0];
                double rawY = cy[0];
               if (Double.isFinite(rawX) && Double.isFinite(rawY)) {
                  float fx = (float)rawX;
                  float fy = (float)rawY;
                  if (Float.isFinite(fx) && Float.isFinite(fy)) {
                     this.cursorX = fx;
                     this.cursorY = fy;
                     this.pointerValid = true;
                  }
               }

               if (this.pointerValid && this.uiOpen) {
                  this.pointerPressed = GLFW.glfwGetMouseButton(handle, 0) == 1;
               }
            }
         }
      }
   }

   public void endFrame() {
      this.frameActive = false;
      this.renderer = null;
      this.pointerValid = false;
      this.pointerPressed = false;
      this.uiOpen = false;
      this.viewportWidth = 0;
      this.viewportHeight = 0;
   }

   public DraggableManager.DragSession beginDrag(String id, float preferredX, float preferredY, float width, float height) {
      this.ensureFrame();
      Objects.requireNonNull(id, "id");
      if (Float.isFinite(preferredX)
         && Float.isFinite(preferredY)
         && Float.isFinite(width)
         && Float.isFinite(height)
         && !(width <= 0.0F)
         && !(height <= 0.0F)
         && this.viewportWidth > 0
         && this.viewportHeight > 0) {
         String persistedId = normalizeId(id);
         DraggableManager.NormalizedPosition persisted = persistedId == null ? null : this.persistedPositions.get(persistedId);
         DraggableManager.DragState state = this.dragStates.computeIfAbsent(id, key -> new DraggableManager.DragState());
         boolean wasDragging = state.dragging;
         boolean canInteract = this.pointerValid && this.uiOpen;
         float resolvedPreferredX = this.resolvePreferredX(persisted, preferredX, width);
         boolean yPinned = persistedId != null && this.yPinnedIds.contains(persistedId);
         float resolvedPreferredY = this.resolvePreferredY(persisted, preferredY, height, yPinned);
         if (!state.dragging) {
            state.positionX = resolvedPreferredX;
            state.positionY = resolvedPreferredY;
         }

         if (!this.pointerPressed || !canInteract) {
            state.dragging = false;
         } else if (!state.dragging) {
            boolean hovered = containsPoint(this.cursorX, this.cursorY, resolvedPreferredX, resolvedPreferredY, width, height);
            if (hovered) {
               state.dragging = true;
               state.offsetX = this.cursorX - resolvedPreferredX;
               state.offsetY = this.cursorY - resolvedPreferredY;
            }
         }

         float maxX = Math.max(0.0F, this.viewportWidth - width);
         float maxY = Math.max(0.0F, this.viewportHeight - height);
         boolean activelyDragging = state.dragging && this.pointerPressed && canInteract;
         float positionX;
         float positionY;
         if (activelyDragging) {
            float rawX = this.cursorX - state.offsetX;
            float rawY = this.cursorY - state.offsetY;
            float[] snapped = this.snap(id, rawX, rawY, width, height);
            positionX = clamp(snapped[0], 0.0F, maxX);
            positionY = clamp(snapped[1], 0.0F, maxY);
            this.activeDragId = id;
            this.activeX = positionX;
            this.activeY = positionY;
            this.activeW = width;
            this.activeH = height;
            this.activeZoneH = zoneOf(this.cursorX, this.viewportWidth);
            this.activeZoneV = zoneOf(this.cursorY, this.viewportHeight);
            this.dragZones.put(id, new int[] { this.activeZoneH, this.activeZoneV });
            state.positionX = positionX;
            state.positionY = positionY;
         } else {
            float restingX = state.positionX;
            float restingY = state.positionY;
            positionX = clamp(restingX, 0.0F, maxX);
            positionY = clamp(restingY, 0.0F, maxY);
            state.positionX = positionX;
            state.positionY = positionY;
            state.dragging = false;
         }
         state.lastWidth = width;
         state.lastHeight = height;
         this.frameRects.put(id, new float[] { positionX, positionY, width, height });

         if (wasDragging && !activelyDragging) {
            int[] zone = this.dragZones.remove(id);
            if (zone == null) {
               zone = new int[] { zoneOf(positionX + width / 2.0F, this.viewportWidth), zoneOf(positionY + height / 2.0F, this.viewportHeight) };
            }
            float ax = zone[0] / 2.0F;
            float ay = zone[1] / 2.0F;
            this.storePersistedPosition(persistedId, positionX + ax * width, positionY + ay * height, ax, ay);
         }

         float targetAlpha = activelyDragging ? 0.65F : 1.0F;
         state.alphaAnimator.setTarget(targetAlpha);
         float animatedAlpha = clamp01(state.alphaAnimator.getValue());
         boolean alphaAdjusted = false;
         if (shouldApplyAnimatedAlpha(state, animatedAlpha, targetAlpha, activelyDragging)) {
            this.renderer.pushAlpha(animatedAlpha);
            alphaAdjusted = true;
         }

         return new DraggableManager.DragSession(id, positionX, positionY, width, height, activelyDragging, alphaAdjusted);
      } else {
         this.disposeDragState(this.dragStates.remove(id));
         return new DraggableManager.DragSession(id, preferredX, preferredY, width, height, false, false);
      }
   }

   public void endDrag(DraggableManager.DragSession session) {
      if (session != null) {
         if (session.alphaAdjusted() && this.renderer != null) {
            this.renderer.popAlpha();
         }

         if (!session.dragging()) {
            DraggableManager.DragState state = this.dragStates.get(session.id());
            if (state == null) {
               this.dragStates.remove(session.id());
               return;
            }

            if (this.canDisposeState(state)) {
               this.disposeDragState(this.dragStates.remove(session.id()));
            }
         }
      }
   }

   public void loadNormalizedPositions(Map<String, DraggableManager.NormalizedPosition> positions) {
      this.disposeAllDragStates();
      this.dragStates.clear();
      this.persistedPositions.clear();
      if (positions != null && !positions.isEmpty()) {
         for (Entry<String, DraggableManager.NormalizedPosition> entry : positions.entrySet()) {
            String id = normalizeId(entry.getKey());
            DraggableManager.NormalizedPosition position = entry.getValue();
            if (id != null && position != null) {
               try {
                  this.persistedPositions.put(id, new DraggableManager.NormalizedPosition(position.x(), position.y(), position.ax(), position.ay()));
               } catch (IllegalArgumentException var7) {
               }
            }
         }
      }
   }

   public void setYPinned(String id) {
      if (id != null) {
         String trimmed = id.trim();
         if (!trimmed.isEmpty()) {
            this.yPinnedIds.add(trimmed);
         }
      }
   }

   public String findHitElement(float x, float y) {
      if (!Float.isFinite(x) || !Float.isFinite(y) || this.dragStates.isEmpty()) {
         return null;
      }

      String hitId = null;
      for (Entry<String, DraggableManager.DragState> entry : this.dragStates.entrySet()) {
         DraggableManager.DragState state = entry.getValue();
         if (state != null
            && state.lastWidth > 0.0F
            && state.lastHeight > 0.0F
            && containsPoint(x, y, state.positionX, state.positionY, state.lastWidth, state.lastHeight)) {
            hitId = entry.getKey();
         }
      }

      return hitId;
   }

   public Map<String, DraggableManager.NormalizedPosition> snapshotNormalizedPositions() {
      if (this.persistedPositions.isEmpty()) {
         return Map.of();
      } else {
         List<String> keys = new ArrayList<>(this.persistedPositions.keySet());
         keys.sort(String.CASE_INSENSITIVE_ORDER);
         Map<String, DraggableManager.NormalizedPosition> snapshot = new LinkedHashMap<>();

         for (String key : keys) {
            DraggableManager.NormalizedPosition position = this.persistedPositions.get(key);
            if (position != null) {
               snapshot.put(key, position);
            }
         }

         return Collections.unmodifiableMap(snapshot);
      }
   }

   private float[] snap(String id, float x, float y, float w, float h) {
      this.guideX = null;
      this.guideY = null;
      if (this.snapBypass) {
         return new float[] { x, y };
      }
      float grid = GRID_BASE * this.uiScale;
      float magnet = MAGNET_BASE * this.uiScale;
      float sx = Math.round(x / grid) * grid;
      float sy = Math.round(y / grid) * grid;
      float[] mx = this.magnetAxis(id, x, w, true, magnet);
      float[] my = this.magnetAxis(id, y, h, false, magnet);
      if (mx != null) {
         sx = mx[0];
         this.guideX = mx[1];
      }
      if (my != null) {
         sy = my[0];
         this.guideY = my[1];
      }
      return new float[] { sx, sy };
   }

   private float[] magnetAxis(String id, float start, float size, boolean horizontal, float threshold) {
      float viewport = horizontal ? this.viewportWidth : this.viewportHeight;
      float[] dragged = { start, start + size / 2.0F, start + size };
      float bestDist = threshold + 1.0F;
      float[] best = null;
      float center = viewport / 2.0F;
      float toCenter = center - dragged[1];
      if (Math.abs(toCenter) <= threshold) {
         bestDist = Math.abs(toCenter);
         best = new float[] { start + toCenter, center };
      }
      for (Entry<String, float[]> entry : this.lastFrameRects.entrySet()) {
         if (entry.getKey().equals(id)) {
            continue;
         }
         float[] r = entry.getValue();
         float tStart = horizontal ? r[0] : r[1];
         float tSize = horizontal ? r[2] : r[3];
         float[] targets = { tStart, tStart + tSize / 2.0F, tStart + tSize };
         for (float t : targets) {
            for (float p : dragged) {
               float dist = t - p;
               float moved = start + dist;
               if (Math.abs(dist) <= threshold && Math.abs(dist) < bestDist && moved >= 0.0F && moved + size <= viewport) {
                  bestDist = Math.abs(dist);
                  best = new float[] { moved, t };
               }
            }
         }
      }
      return best;
   }

   private static int zoneOf(float coordinate, int size) {
      if (coordinate < size / 3.0F) {
         return 0;
      }
      return coordinate > size * 2.0F / 3.0F ? 2 : 1;
   }

   /** Draws grid, anchor zones, magnet guides and coordinates while an element is being dragged. */
   public void renderEditorOverlay() {
      if (!this.frameActive || this.renderer == null || this.activeDragId == null || !this.uiOpen) {
         return;
      }
      Renderer2D r = this.renderer;
      int main = Renderer2D.ColorUtil.getMainColor(1, 1);
      int white = 0xFFFFFFFF;
      float w = this.viewportWidth;
      float h = this.viewportHeight;
      if (!this.snapBypass) {
         float grid = GRID_BASE * this.uiScale;
         int gridColor = ru.zero.util.color.ColorUtil.replAlpha(white, 16);
         for (float gx = 0.0F; gx <= w; gx += grid) {
            r.rect(gx, 0.0F, 1.0F, h, gridColor);
         }
         for (float gy = 0.0F; gy <= h; gy += grid) {
            r.rect(0.0F, gy, w, 1.0F, gridColor);
         }
      }
      float zw = w / 3.0F;
      float zh = h / 3.0F;
      for (int zx = 0; zx < 3; zx++) {
         for (int zy = 0; zy < 3; zy++) {
            boolean active = zx == this.activeZoneH && zy == this.activeZoneV;
            float x0 = zx * zw;
            float y0 = zy * zh;
            if (active) {
               r.rect(x0, y0, zw, zh, ru.zero.util.color.ColorUtil.replAlpha(main, 26));
               float px = x0 + zw * zx / 2.0F;
               float py = y0 + zh * zy / 2.0F;
               float dot = 5.0F * this.uiScale;
               r.rect(px - dot, py - dot, dot * 2.0F, dot * 2.0F, dot, main);
            }
            r.rectOutline(x0, y0, zw, zh, active ? ru.zero.util.color.ColorUtil.replAlpha(main, 170) : ru.zero.util.color.ColorUtil.replAlpha(white, 40), 1.0F);
         }
      }
      float line = Math.max(1.0F, this.uiScale);
      if (this.guideX != null) {
         r.rect(this.guideX - line / 2.0F, 0.0F, line, h, main);
      }
      if (this.guideY != null) {
         r.rect(0.0F, this.guideY - line / 2.0F, w, line, main);
      }
      String label = Math.round(this.activeX / this.uiScale) + ", " + Math.round(this.activeY / this.uiScale)
         + (this.snapBypass ? "" : "   Shift: без привязки");
      float textY = this.activeY > 30.0F * this.uiScale ? this.activeY - 6.0F * this.uiScale : this.activeY + this.activeH + 14.0F * this.uiScale;
      r.text(FontRegistry.INTER_MEDIUM, this.activeX, textY, 11.0F * this.uiScale, label, Renderer2D.ColorUtil.getTextColor(1, 1));
   }

   private void ensureFrame() {
      if (!this.frameActive || this.renderer == null) {
         throw new IllegalStateException("beginFrame must be called before beginDrag");
      }
   }

   private float resolvePreferredX(DraggableManager.NormalizedPosition persisted, float fallback, float width) {
      if (persisted != null && this.viewportWidth > 0) {
         float max = Math.max(0.0F, this.viewportWidth - width);
         return clamp(persisted.x() * this.viewportWidth - persisted.ax() * width, 0.0F, max);
      } else {
         return fallback;
      }
   }

   private float resolvePreferredY(DraggableManager.NormalizedPosition persisted, float fallback, float height, boolean yPinned) {
      if (!yPinned && persisted != null && this.viewportHeight > 0) {
         float max = Math.max(0.0F, this.viewportHeight - height);
         return clamp(persisted.y() * this.viewportHeight - persisted.ay() * height, 0.0F, max);
      } else {
         return fallback;
      }
   }

   private void storePersistedPosition(String normalizedId, float x, float y, float ax, float ay) {
      if (this.viewportWidth > 0 && this.viewportHeight > 0) {
         if (normalizedId != null) {
            float normalizedX = x / this.viewportWidth;
            float normalizedY = y / this.viewportHeight;
            if (Float.isFinite(normalizedX) && Float.isFinite(normalizedY)) {
               DraggableManager.NormalizedPosition updated;
               try {
                  updated = new DraggableManager.NormalizedPosition(normalizedX, normalizedY, ax, ay);
               } catch (IllegalArgumentException var10) {
                  return;
               }

               DraggableManager.NormalizedPosition previous = this.persistedPositions.get(normalizedId);
               if (!approximatelyEquals(previous, updated)) {
                  this.persistedPositions.put(normalizedId, updated);

                  try {
                     if (Zero.get != null && Zero.get.configManager != null) {
                        Zero.get.configManager.autoSave();
                     }
                  } catch (Exception var9) {
                  }
               }
            }
         }
      }
   }

   private static boolean approximatelyEquals(DraggableManager.NormalizedPosition first, DraggableManager.NormalizedPosition second) {
      if (first == second) {
         return true;
      } else {
         return first != null && second != null ? Math.abs(first.x() - second.x()) <= 5.0E-4F && Math.abs(first.y() - second.y()) <= 5.0E-4F && first.ax() == second.ax() && first.ay() == second.ay() : false;
      }
   }

   private static boolean approximatelyEquals(float first, float second, float tolerance) {
      return Math.abs(first - second) <= tolerance;
   }

   private static SpringFloatAnimator createAlphaAnimator() {
      SpringFloatAnimator animator = new SpringFloatAnimator(AnimationSystem.getInstance(), DRAG_ALPHA_SPRING, 1.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      animator.setOutputTransform(Easings.SMOOTH_STEP);
      return animator;
   }

   private static boolean shouldApplyAnimatedAlpha(DraggableManager.DragState state, float animatedAlpha, float targetAlpha, boolean activelyDragging) {
      if (state == null) {
         return false;
      } else if (activelyDragging) {
         return true;
      } else if (animatedAlpha < 0.999F) {
         return true;
      } else {
         return targetAlpha < 0.999F ? true : !state.alphaAnimator.isSettled();
      }
   }

   private boolean canDisposeState(DraggableManager.DragState state) {
      if (state == null) {
         return true;
      } else {
         float target = state.alphaAnimator.getTarget();
         float rawValue = state.alphaAnimator.getRawValue();
         boolean restingTarget = approximatelyEquals(target, 1.0F, 5.0E-4F);
         boolean restingValue = approximatelyEquals(rawValue, 1.0F, 5.0E-4F);
         return restingTarget && restingValue && state.alphaAnimator.isSettled();
      }
   }

   private void disposeAllDragStates() {
      if (!this.dragStates.isEmpty()) {
         for (DraggableManager.DragState state : this.dragStates.values()) {
            this.disposeDragState(state);
         }
      }
   }

   private void disposeDragState(DraggableManager.DragState state) {
      if (state != null) {
         state.alphaAnimator.snapTo(1.0F);
      }
   }

   private static boolean containsPoint(float px, float py, float x, float y, float width, float height) {
      return px >= x && px <= x + width && py >= y && py <= y + height;
   }

   private static float clamp(float value, float min, float max) {
      if (value < min) {
         return min;
      } else {
         return value > max ? max : value;
      }
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      } else {
         return value > 1.0F ? 1.0F : value;
      }
   }

   private static String normalizeId(String id) {
      if (id == null) {
         return null;
      } else {
         String trimmed = id.trim();
         return trimmed.isEmpty() ? null : trimmed;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class DragSession {
      private final String id;
      private final float positionX;
      private final float positionY;
      private final float width;
      private final float height;
      private final boolean dragging;
      private final boolean alphaAdjusted;

      private DragSession(String id, float positionX, float positionY, float width, float height, boolean dragging, boolean alphaAdjusted) {
         this.id = id;
         this.positionX = positionX;
         this.positionY = positionY;
         this.width = width;
         this.height = height;
         this.dragging = dragging;
         this.alphaAdjusted = alphaAdjusted;
      }

      public String id() {
         return this.id;
      }

      public float positionX() {
         return this.positionX;
      }

      public float positionY() {
         return this.positionY;
      }

      public float width() {
         return this.width;
      }

      public float height() {
         return this.height;
      }

      public boolean dragging() {
         return this.dragging;
      }

      public boolean alphaAdjusted() {
         return this.alphaAdjusted;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class DragState {
      private boolean dragging;
      private float offsetX;
      private float offsetY;
      private float positionX;
      private float positionY;
      private float lastWidth;
      private float lastHeight;
      private final SpringFloatAnimator alphaAnimator = DraggableManager.createAlphaAnimator();
   }

   @Environment(EnvType.CLIENT)
   public static final class NormalizedPosition {
      private final float x;
      private final float y;
      private final float ax;
      private final float ay;

      public NormalizedPosition(float x, float y) {
         this(x, y, 0.0F, 0.0F);
      }

      public NormalizedPosition(float x, float y, float ax, float ay) {
         if (Float.isFinite(x) && Float.isFinite(y)) {
            this.x = DraggableManager.clamp01(x);
            this.y = DraggableManager.clamp01(y);
            this.ax = Float.isFinite(ax) ? DraggableManager.clamp01(ax) : 0.0F;
            this.ay = Float.isFinite(ay) ? DraggableManager.clamp01(ay) : 0.0F;
         } else {
            throw new IllegalArgumentException("Normalized coordinates must be finite");
         }
      }

      public float ax() {
         return this.ax;
      }

      public float ay() {
         return this.ay;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }
   }
}
