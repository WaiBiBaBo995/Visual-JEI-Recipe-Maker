package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.*;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.crafting.Recipe;
import java.util.*;
import java.util.stream.Stream;

final class JeiPreviewProvider implements RecipePreview.Provider {
    private final java.util.function.Supplier<mezz.jei.api.runtime.IJeiRuntime> runtime;
    JeiPreviewProvider(){runtime=VjrmJeiPlugin::runtime;}
    JeiPreviewProvider(mezz.jei.api.runtime.IJeiRuntime runtime){this.runtime=()->runtime;}

    @Override public boolean drawIngredientIcon(GuiGraphics g,Object ingredient,int x,int y){
        if(runtime.get()==null)return false;
        Object value=ingredient;
        if(ingredient instanceof net.minecraftforge.fluids.FluidStack fluid){var copy=fluid.copy();copy.setAmount(1000);value=copy;}
        if(value instanceof net.minecraft.world.item.ItemStack item){var copy=item.copy();copy.setCount(1);value=copy;}
        // Icons show a full sample; recipe amounts and components remain untouched.
        var renderer=runtime.get().getIngredientManager().getIngredientRenderer(value);
        g.pose().pushPose();
        try{g.pose().translate(x,y,0);float scale=16f/Math.max(16,Math.max(renderer.getWidth(),renderer.getHeight()));g.pose().scale(scale,scale,1);renderer.render(g,value,0,0);}
        finally{g.pose().popPose();}
        return true;
    }
    @Override public boolean drawCategoryIcon(GuiGraphics g,RecipePreview.Category category,int x,int y){
        var icon=((IRecipeCategory<?>)category.nativeCategory()).getIcon();if(icon==null)return false;
        g.pose().pushPose();
        try{g.pose().translate(x,y,0);float scale=16f/Math.max(16,Math.max(icon.getWidth(),icon.getHeight()));g.pose().scale(scale,scale,1);icon.draw(g,0,0);}
        finally{g.pose().popPose();}
        return true;
    }
    public RecipePreview.Category category(Object value) {
        var category=(IRecipeCategory<?>)value;
        return new RecipePreview.Category(category,new RecipePreview.Type(category.getRecipeType().getUid()),category.getTitle());
    }
    public Optional<RecipePreview.Layout> layout(Recipe<?> recipe,RecipePreview.Category preferred,RecipeDocument document) {
        return VjrmJeiPlugin.layout(recipe,preferred==null?null:(IRecipeCategory<?>)preferred.nativeCategory(),runtime.get()).map(JeiLayout::new);
    }
    public Optional<RecipePreview.Layout> sourceLayout(RecipePreview.Category category,Object display) {
        return VjrmJeiPlugin.sourceLayout((IRecipeCategory<?>)category.nativeCategory(),display,runtime.get()).map(JeiLayout::new);
    }
    public List<RecipePreview.Template> templates(List<RecipeSource.Sample> samples) {
        return VjrmJeiPlugin.templates(samples).stream().map(t->new RecipePreview.Template(t.serializer(),t.id(),category(t.category()),t.display())).toList();
    }
    private final class JeiLayout implements RecipePreview.Layout {
        private final IRecipeLayoutDrawable<?> layout;
        private final RecipePreview.Slots slots;
        JeiLayout(IRecipeLayoutDrawable<?> layout) {
            this.layout=layout;
            slots=new RecipePreview.Slots(layout.getRecipeSlotsView().getSlotViews().stream().filter(IRecipeSlotDrawable.class::isInstance).map(s->(RecipePreview.Slot)new JeiSlot((IRecipeSlotDrawable)s)).toList());
        }
        public Rect2i getRect(){return layout.getRect();}
        public RecipePreview.Category getRecipeCategory(){return category(layout.getRecipeCategory());}
        public RecipePreview.Slots getRecipeSlotsView(){return slots;}
        public void drawRecipe(GuiGraphics g,int x,int y){layout.drawRecipe(g,x,y);}
        public void setPosition(int x,int y){layout.setPosition(x,y);}
        public void tick(){layout.tick();}
        public Object nativeLayout(){return layout;}
    }
    private record JeiSlot(IRecipeSlotDrawable slot) implements RecipePreview.Slot {
        @SuppressWarnings("removal") public Rect2i getRect(){return slot.getRect();}
        public RecipePreview.Role getRole(){return RecipePreview.Role.valueOf(slot.getRole().name());}
        public Stream<RecipePreview.Value> getAllIngredients(){return slot.getAllIngredients().map(i->new RecipePreview.Value(i.getIngredient()));}
        public Optional<RecipePreview.Value> getDisplayedIngredient(){return slot.getDisplayedIngredient().map(i->new RecipePreview.Value(i.getIngredient()));}
        public boolean isEmpty(){return slot.isEmpty();}
        @SuppressWarnings("removal") public List<net.minecraft.network.chat.Component> tooltip(){return IngredientTooltips.identify(slot.getTooltip(),getDisplayedIngredient().map(RecipePreview.Value::getIngredient).orElse(null));}
        public void override(List<?> values){slot.createDisplayOverrides().addIngredientsUnsafe(values);}
        public Object nativeSlot(){return slot;}
    }
}
