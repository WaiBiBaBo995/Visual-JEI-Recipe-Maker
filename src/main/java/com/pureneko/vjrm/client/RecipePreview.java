package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import com.pureneko.vjrm.recipe.RecipeSource;
import com.pureneko.vjrm.client.LegacyGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import java.util.*;
import java.util.stream.Stream;

/** Viewer-neutral boundary. Optional viewer API classes stay in their own integrations. */
public final class RecipePreview {
    public enum Role { INPUT, OUTPUT, CATALYST, RENDER_ONLY }
    public record Type(Identifier getUid) { }
    public record Category(Object nativeCategory, Type getRecipeType, Component getTitle) { }
    public record Value(Object getIngredient) { }
    public record Slots(List<Slot> getSlotViews) { }
    public record Template(String serializer, Identifier id, Category category, Object display) {
        public String label() { return serializer+" · "+category.getTitle().getString()+(proxy(category,serializer)?" · "+Component.translatable("vjrm.proxy").getString():""); }
    }
    public interface Slot {
        Rect2i getRect();
        Role getRole();
        Stream<Value> getAllIngredients();
        default Optional<Value> getDisplayedIngredient() { return getAllIngredients().findFirst(); }
        default boolean isEmpty() { return getDisplayedIngredient().isEmpty(); }
        void override(List<?> values);
        default List<Component> tooltip(){return IngredientTooltips.of(getDisplayedIngredient().map(Value::getIngredient).orElse(null));}
        default String fieldPath() { return null; }
        default Object nativeSlot() { return this; }
    }
    public interface Layout {
        Rect2i getRect();
        Category getRecipeCategory();
        Slots getRecipeSlotsView();
        void drawRecipe(LegacyGraphics graphics, int mouseX, int mouseY);
        default void setPosition(int x, int y) { }
        default void tick() { }
        default Object nativeLayout() { return this; }
        default boolean usesEmiCraftingGrid() { return false; }
    }
    public interface Hooks {
        default void initialized(RecipeEditorScreen screen) { }
        default void background(LegacyGraphics g,int x,int y,float delta) { }
        default void overlay(LegacyGraphics g,int x,int y,float delta) { }
        default boolean mouseClicked(double x,double y,int button) { return false; }
        default boolean mouseReleased(double x,double y,int button) { return false; }
        default boolean mouseDragged(double x,double y,int button,double dx,double dy) { return false; }
        default boolean mouseScrolled(double x,double y,double horizontal,double vertical) { return false; }
        default boolean keyPressed(int key,int scan,int modifiers) { return false; }
        default boolean charTyped(char character,int modifiers) { return false; }
    }
    public interface Provider extends Hooks {
        Category category(Object category);
        Optional<Layout> layout(Recipe<?> recipe, Category preferred, RecipeDocument document);
        Optional<Layout> sourceLayout(Category category, Object display);
        default Optional<Layout> fallbackLayout(Recipe<?> recipe,Category category,RecipeDocument document) { return Optional.empty(); }
        List<Template> templates(List<RecipeSource.Sample> samples);
    }
    private static volatile Provider provider;
    private static final Hooks NO_HOOKS=new Hooks() { };
    public static Hooks hooks() { var current=provider;return current==null?NO_HOOKS:current; }
    public static void install(Provider value) { provider=value; }
    public static void uninstall(Provider value) { if(provider==value)provider=null; }
    public static boolean available() { return provider!=null; }
    public static Category category(Object value) { return value==null?null:value instanceof Category c?c:provider==null?null:provider.category(value); }
    public static Optional<Layout> layout(Recipe<?> recipe, Category preferred, RecipeDocument document) { return provider==null?Optional.empty():provider.layout(recipe,preferred,document); }
    public static Optional<Layout> sourceLayout(Category category,Object display) { return provider==null?Optional.empty():provider.sourceLayout(category,display); }
    public static Optional<Layout> fallbackLayout(Recipe<?> recipe,Category category,RecipeDocument document) { return provider==null?Optional.empty():provider.fallbackLayout(recipe,category,document); }
    public static List<Template> templates(List<RecipeSource.Sample> samples) { return provider==null?List.of():provider.templates(samples); }
    public static Component typeLabel(String id,Category category){
        Component name=category==null?Component.empty():category.getTitle();
        if(name.getString().isBlank()||name.getString().equals(id)){
            String path=id.startsWith("minecraft:")?id.substring(10):"";
            if(RecipeDocument.TYPES.contains(path))name=Component.translatable("vjrm.type."+path);
        }
        return Component.literal(id).append(name.getString().isBlank()?Component.empty():Component.literal(" · ").append(name));
    }
    public static boolean proxy(Category category,String serializer) {
        if(category==null)return false;
        var id=category.getRecipeType().getUid();
        if(!id.getNamespace().equals("create"))return false;
        if(id.getPath().startsWith("automatic_")||id.getPath().equals("block_cutting"))return true;
        String expected=switch(serializer){
            case "create:cutting"->"create:sawing";case "create:compacting"->"create:packing";
            case "create:splashing"->"create:fan_washing";case "create:haunting"->"create:fan_haunting";
            case "create:filling"->"create:spout_filling";case "create:emptying"->"create:draining";
            default->serializer;
        };
        return !id.toString().equals(expected);
    }
    private RecipePreview() { }
}
