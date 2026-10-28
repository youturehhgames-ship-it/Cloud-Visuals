package com.simplevisuals.client.gui;

import com.simplevisuals.client.cit.CitEntry;
import com.simplevisuals.client.cit.CitRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class VisualsScreen extends Screen {

    private final Screen parent;
    private final Item currentItem;
    private final List<CitEntry> allEntries;

    private final List<CitEntryWidget> gridWidgets = new ArrayList<>();
    private TextFieldWidget searchField;
    private ButtonWidget backButton;
    private ButtonWidget recipeCloseButton;

    private CitEntry selectedEntry = null;

    private static final int ENTRY_SIZE = 32;
    private static final int COLUMNS = 5;

    public VisualsScreen(Screen parent, Item currentItem) {
        super(Text.literal("Visuals Selection"));
        this.parent = parent;
        this.currentItem = currentItem;
        this.allEntries = CitRegistry.getEntries(currentItem);
    }

    @Override
    protected void init() {
        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 80, 40, 160, 20, Text.literal("Search"));
        this.searchField.setChangedListener(this::updateSearch);
        this.addDrawableChild(this.searchField);

        this.backButton = ButtonWidget.builder(Text.literal("Schließen"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 30, 100, 20)
                .build();
        this.addDrawableChild(this.backButton);

        this.recipeCloseButton = ButtonWidget.builder(Text.literal("Zurück zur Auswahl"), button -> showRecipe(null))
                .dimensions(this.width / 2 - 60, this.height - 50, 120, 20)
                .build();
        this.recipeCloseButton.visible = false;
        this.addDrawableChild(this.recipeCloseButton);

        rebuildGrid("");
    }

    private void updateSearch(String query) {
        rebuildGrid(query.toLowerCase());
    }

    private void rebuildGrid(String query) {
        for (CitEntryWidget widget : gridWidgets) {
            this.remove(widget);
        }
        gridWidgets.clear();

        List<CitEntry> filtered = new ArrayList<>();
        for (CitEntry entry : this.allEntries) {
            boolean matchesTag = entry.tags().stream().anyMatch(t -> t.toLowerCase().contains(query));
            if (entry.name().toLowerCase().contains(query) || matchesTag) {
                filtered.add(entry);
            }
        }

        int startX = this.width / 2 - (COLUMNS * ENTRY_SIZE) / 2;
        int startY = 80;

        for (int i = 0; i < filtered.size(); i++) {
            CitEntry entry = filtered.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + col * ENTRY_SIZE;
            int y = startY + row * ENTRY_SIZE;

            CitEntryWidget widget = new CitEntryWidget(x, y, ENTRY_SIZE, ENTRY_SIZE, entry);
            widget.visible = (selectedEntry == null);
            this.addDrawableChild(widget);
            gridWidgets.add(widget);
        }
    }

    private void showRecipe(CitEntry entry) {
        this.selectedEntry = entry;
        boolean showingRecipe = (entry != null);

        this.searchField.visible = !showingRecipe;
        this.backButton.visible = !showingRecipe;
        this.recipeCloseButton.visible = showingRecipe;

        for (CitEntryWidget widget : gridWidgets) {
            widget.visible = !showingRecipe;
            widget.active = !showingRecipe;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // FIX: Farbe 0xFFFFFFFF (Volle Deckkraft) statt 0xFFFFFF (Durchsichtig)
        context.drawCenteredTextWithShadow(this.textRenderer, "Simple Visuals", this.width / 2, 15, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);

        if (this.selectedEntry != null) {
            renderRecipe(context, mouseX, mouseY);
        }
    }

    private void renderRecipe(DrawContext context, int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Hintergrundbox
        context.fill(centerX - 100, centerY - 60, centerX + 100, centerY + 20, 0xAA000000);

        // Rahmen (Weiß, volle Deckkraft)
        context.drawStrokedRectangle(centerX - 100, centerY - 60, 200, 80, 0xFFFFFFFF);

        // Linkes Item
        ItemStack originalStack = new ItemStack(currentItem);
        context.drawItem(originalStack, centerX - 60, centerY - 20);

        // FIX: Farben korrigiert (0xFF...)
        context.drawCenteredTextWithShadow(this.textRenderer, "Umbenennen zu:", centerX, centerY - 40, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, "➡", centerX, centerY - 16, 0xFFFFFFFF);

        // Name in Gelb
        Text nameText = Text.literal(selectedEntry.name()).formatted(Formatting.YELLOW);
        context.drawCenteredTextWithShadow(this.textRenderer, nameText, centerX, centerY - 5, 0xFFFFFF00);

        // Rechtes Item (Ergebnis) - Größer skalieren
        ItemStack resultStack = new ItemStack(currentItem);
        resultStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(selectedEntry.name()));

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX + 40, centerY - 24);
        context.getMatrices().scale(1.5f, 1.5f);
        context.drawItem(resultStack, 0, 0);
        context.getMatrices().popMatrix();

        // Info Text in Grau
        context.drawCenteredTextWithShadow(this.textRenderer, "Lege das Item in den Amboss", centerX, centerY + 40, 0xFF808080);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    class CitEntryWidget extends PressableWidget {
        private final CitEntry entry;

        public CitEntryWidget(int x, int y, int width, int height, CitEntry entry) {
            super(x, y, width, height, Text.literal(entry.name()));
            this.entry = entry;
        }

        @Override
        public void onPress(AbstractInput input) {
            showRecipe(entry);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            this.appendDefaultNarrations(builder);
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            if (!this.visible) return;

            int color = (this.isSelected() || this.isHovered()) ? 0x80FFFFFF : 0x40000000;
            context.fill(getX(), getY(), getX() + width, getY() + height, color);

            ItemStack renderStack = new ItemStack(currentItem);
            renderStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(entry.name()));
            context.drawItem(renderStack, getX() + 8, getY() + 8);

            if (this.isHovered()) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.literal(entry.name()).formatted(Formatting.YELLOW));
                entry.tags().forEach(tag -> tooltip.add(Text.literal("#" + tag).formatted(Formatting.GRAY)));
                context.drawTooltip(VisualsScreen.this.textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }
}