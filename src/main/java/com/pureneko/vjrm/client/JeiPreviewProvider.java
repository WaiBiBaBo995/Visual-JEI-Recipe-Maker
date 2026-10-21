package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.*;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.category.IRecipeCategory;
import com.pureneko.vjrm.client.LegacyGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.crafting.Recipe;
import java.util.*;
import java.util.stream.Stream;

final class JeiPreviewProvider implements RecipePreview.Provider {
    private final java.util.function.Supplier<mezz.jei.api.runtime.IJeiRuntime> runtime;
    JeiPreviewProvider(){runtime=VjrmJeiPlugin::runtime;}
    JeiPreviewProvider(mezz.jei.api.runtime.IJeiRuntime runtime){this.runtime=()->runtime;}

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
        public void drawRecipe(LegacyGraphics g,int x,int y){layout.drawRecipe(g.nativeGraphics,x,y);}
        public void setPosition(int x,int y){layout.setPosition(x,y);}
        public void tick(){layout.tick();}
        public Object nativeLayout(){return layout;}
    }
    private final class JeiSlot implements RecipePreview.Slot {
        private final IRecipeSlotDrawable slot;private final Rect2i bounds;
        JeiSlot(IRecipeSlotDrawable slot){this.slot=slot;this.bounds=interactiveBounds(slot);}
        public Rect2i getRect(){return bounds;}
        private static Rect2i interactiveBounds(IRecipeSlotDrawable slot){
            // JEI's decorative area can extend back to the layout origin. Use its public hit test
            // to keep bindings and hover regions on this ingredient, rather than adjacent slots.
            var area=slot.getAreaIncludingBackground();
            for(int y=area.getY();y<area.getY()+area.getHeight();y++)for(int x=area.getX();x<area.getX()+area.getWidth();x++){
                if(!slot.isMouseOver(x+.5,y+.5))continue;
                int right=x+1,bottom=y+1;
                while(right<area.getX()+area.getWidth()&&slot.isMouseOver(right+.5,y+.5))right++;
                while(bottom<area.getY()+area.getHeight()&&slot.isMouseOver(x+.5,bottom+.5))bottom++;
                return new Rect2i(x,y,right-x,bottom-y);
            }
            return area;
        }
        public RecipePreview.Role getRole(){return RecipePreview.Role.valueOf(slot.getRole().name());}
        public Stream<RecipePreview.Value> getAllIngredients(){return slot.getAllIngredients().map(i->new RecipePreview.Value(FabricFluidBridge.wrap(i.getIngredient())));}
        public Optional<RecipePreview.Value> getDisplayedIngredient(){return slot.getDisplayedIngredient().map(i->new RecipePreview.Value(FabricFluidBridge.wrap(i.getIngredient())));}
        public boolean isEmpty(){return slot.isEmpty();}
        @SuppressWarnings("removal") public List<net.minecraft.network.chat.Component> tooltip(){return IngredientTooltips.identify(slot.getTooltip(),getDisplayedIngredient().map(RecipePreview.Value::getIngredient).orElse(null));}
        public void override(List<?> values){slot.createDisplayOverrides().addIngredientsUnsafe(values.stream().map(v->FabricFluidBridge.unwrap(v,runtime.get().getJeiHelpers())).toList());}
        public Object nativeSlot(){return slot;}
    }
}
