package com.pureneko.vjrm.client;

import com.pureneko.vjrm.Vjrm;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.recipe.advanced.IRecipeButtonControllerFactory;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.buttons.*;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import net.minecraft.world.item.crafting.Recipe;
import com.pureneko.vjrm.recipe.RecipeDocument;
import java.util.*;

@JeiPlugin
public final class VjrmJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    private static final JeiPreviewProvider PREVIEW=new JeiPreviewProvider();
    @Override public ResourceLocation getPluginUid() { return new ResourceLocation(Vjrm.MOD_ID, "jei_plugin"); }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) { VjrmJeiPlugin.runtime = runtime; if(!net.minecraftforge.fml.ModList.get().isLoaded("emi"))RecipePreview.install(PREVIEW); }
    @Override public void onRuntimeUnavailable() { runtime = null; RecipePreview.uninstall(PREVIEW); }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(RecipeEditorScreen.class, s -> {
            if(s.width<2 || s.height<2 || s.panelWidth()<2 || s.panelHeight()<2)return null;
            return new EditorProperties(s.guiLeft(),s.guiTop(),s.guiWidth(),s.guiHeight(),s.width,s.height);
        });
        registration.addGhostIngredientHandler(RecipeEditorScreen.class, new IGhostIngredientHandler<>() {
            public <I> List<Target<I>> getTargetsTyped(RecipeEditorScreen screen, ITypedIngredient<I> ingredient, boolean start) {
                List<Target<I>> targets = new ArrayList<>();
                for (var target : screen.dropTargets(ingredient.getIngredient())) targets.add(new Target<>() {
                    public Rect2i getArea() { return target.area(); }
                    public void accept(I value) { screen.accept(target.field(), value); }
                });
                return targets;
            }
            public void onComplete() { }
        });
    }
    @Override public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeButtonFactory(new IRecipeButtonControllerFactory() {
            public <T> IIconButtonController createButtonController(IRecipeLayoutDrawable<T> layout) {
                Recipe<?> holder = source(layout);
                if (holder == null) return null;
                return new IIconButtonController() {
                    public boolean onPress(IJeiUserInput input) {
                        if (!input.isSimulate()) Minecraft.getInstance().execute(() -> {
                            var mc = Minecraft.getInstance();
                            var parent=mc.screen;
                            RecipeSources.request(holder.getId(),document->{
                                if(mc.screen==parent) mc.setScreen(new RecipeEditorScreen(document,PREVIEW.category(layout.getRecipeCategory()),holder.getId()+"_copy",parent,layout.getRecipe()));
                            },error->{if(mc.player!=null)mc.player.displayClientMessage(Component.translatable("vjrm.error.copy",Component.translatable(error)),false);});
                        });
                        return true;
                    }
                    public void initState(IButtonState state) {
                        state.setIcon(registration.getJeiHelpers().getGuiHelper().createDrawableItemStack(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK)));
                        updateState(state);
                    }
                    public void updateState(IButtonState state){state.setVisible(VjrmClientConfig.copyButton());}
                    public void getTooltips(ITooltipBuilder tooltip) { tooltip.add(Component.translatable("vjrm.copy")); }
                };
            }
        });
    }
    private static <T> Recipe<?> source(IRecipeLayoutDrawable<T> layout) {
        var mc = Minecraft.getInstance(); if (mc.level == null) return null;
        ResourceLocation id = sourceId(layout.getRecipeCategory(),layout.getRecipe());
        if(id==null)return null;
        var found=mc.level.getRecipeManager().byKey(id).orElse(null);
        if(found!=null)return found;
        if(com.pureneko.vjrm.recipe.GtRecipeSupport.isRecipe(layout.getRecipe()))return (Recipe<?>)layout.getRecipe();
        return null;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static ResourceLocation sourceId(IRecipeCategory category,Object display){
        var id=com.pureneko.vjrm.recipe.GtRecipeSupport.id(display);if(id!=null)return id;
        id=RecipeIdentities.id(display);if(id!=null)return id;
        try{return category.getRegistryName(display);}catch(RuntimeException ex){return null;}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static Optional<IRecipeLayoutDrawable<?>> sourceLayout(IRecipeCategory category,Object display){return sourceLayout(category,display,runtime);}
    @SuppressWarnings({"rawtypes","unchecked"})
    public static Optional<IRecipeLayoutDrawable<?>> sourceLayout(IRecipeCategory category,Object display,IJeiRuntime runtime){
        if(runtime==null||display==null)return Optional.empty();
        try{return (Optional)runtime.getRecipeManager().createRecipeLayoutDrawable(category,display,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());}catch(RuntimeException ex){return Optional.empty();}
    }
    public record Template(String serializer,ResourceLocation id,IRecipeCategory<?> category,Object display){
        public String label(){return serializer+" · "+category.getTitle().getString()+(proxy(category,serializer)?" · "+Component.translatable("vjrm.proxy").getString():"");}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static List<Template> templates(List<com.pureneko.vjrm.recipe.RecipeSource.Sample> samples){
        if(runtime==null)return List.of();
        Map<ResourceLocation,String> ids=new HashMap<>();samples.forEach(s->ids.put(new ResourceLocation(s.id()),s.serializer()));
        Map<String,Template> found=new TreeMap<>();
        for(var category:runtime.getRecipeManager().createRecipeCategoryLookup().get().toList()){
            try(var recipes=runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).get()){
                var iterator=recipes.iterator();
                while(iterator.hasNext()){
                    Object display=iterator.next();ResourceLocation id=sourceId(category,display);String serializer=ids.get(id);
                    if(serializer!=null){String key=serializer+"/"+category.getRecipeType().getUid();found.putIfAbsent(key,new Template(serializer,id,category,display));}
                }
            }catch(RuntimeException ignored){ }
        }
        return List.copyOf(found.values());
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Optional<IRecipeLayoutDrawable<?>> layout(Recipe<?> recipe, mezz.jei.api.recipe.category.IRecipeCategory<?> preferred) {return layout(recipe,preferred,runtime);}
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Optional<IRecipeLayoutDrawable<?>> layout(Recipe<?> recipe, mezz.jei.api.recipe.category.IRecipeCategory<?> preferred,IJeiRuntime runtime) {
        if (runtime == null) return Optional.empty();
        var categories = new ArrayList<mezz.jei.api.recipe.category.IRecipeCategory<?>>();
        if (preferred != null) categories.add(preferred);
        else categories.addAll(runtime.getRecipeManager().createRecipeCategoryLookup().get().toList());
        categories.sort(java.util.Comparator.comparingInt(c->categoryMatches(c,recipe,c==preferred)?0:1));
        for (var c : categories) {
            mezz.jei.api.recipe.category.IRecipeCategory category = c;
            Object value = recipe;
            if (!category.getRecipeType().getRecipeClass().isInstance(value)) continue;
            // Match the real recipe family, including JEI 15's vanilla category aliases.
            if(!categoryMatches(category,recipe,c==preferred))continue;
            try {
                if (!category.isHandled(value)) continue;
                var found = runtime.getRecipeManager().createRecipeLayoutDrawable(category, value, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
                if (found.isPresent()) return Optional.of((IRecipeLayoutDrawable<?>)found.get());
            } catch (RuntimeException ignored) { }
        }
        return Optional.empty();
    }
    /** Create exposes conversions under a different category while keeping the original source id. */
    public static boolean proxy(IRecipeCategory<?> category,String serializer){
        if(category==null)return false;
        var uid=category.getRecipeType().getUid();
        if(!uid.getNamespace().equals("create"))return false;
        if(uid.getPath().startsWith("automatic_") || uid.getPath().equals("block_cutting"))return true;
        return !uid.toString().equals(createCategory(serializer));
    }
    private static String createCategory(String serializer){
        return switch(serializer){
            case "create:cutting" -> "create:sawing";
            case "create:compacting" -> "create:packing";
            case "create:splashing" -> "create:fan_washing";
            case "create:haunting" -> "create:fan_haunting";
            case "create:filling" -> "create:spout_filling";
            case "create:emptying" -> "create:draining";
            default -> serializer;
        };
    }
    private static boolean categoryMatches(IRecipeCategory<?> category,Recipe<?> recipe,boolean preferred){
        String serializer=net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()).toString();
        var uid=category.getRecipeType().getUid();
        if(uid.getNamespace().equals("create"))return uid.toString().equals(createCategory(serializer));
        if(preferred)return true;
        var type=net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        if(type.toString().equals("minecraft:smelting"))return uid.equals(mezz.jei.api.constants.RecipeTypes.SMELTING.getUid());
        if(type.toString().equals("minecraft:campfire_cooking"))return uid.equals(mezz.jei.api.constants.RecipeTypes.CAMPFIRE_COOKING.getUid());
        return uid.equals(type);
    }
    public static IJeiRuntime runtime() { return runtime; }
    private record EditorProperties(int guiLeft,int guiTop,int guiXSize,int guiYSize,int screenWidth,int screenHeight) implements IGuiProperties {
        public Class<? extends net.minecraft.client.gui.screens.Screen> getScreenClass(){return RecipeEditorScreen.class;}
        public int getGuiLeft(){return guiLeft;}public int getGuiTop(){return guiTop;}
        public int getGuiXSize(){return guiXSize;}public int getGuiYSize(){return guiYSize;}
        public int getScreenWidth(){return screenWidth;}public int getScreenHeight(){return screenHeight;}
    }
}
