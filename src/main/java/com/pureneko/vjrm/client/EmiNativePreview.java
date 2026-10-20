package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.widget.*;
import dev.emi.emi.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

/** Renders the recipe's complete EMI widget tree, including JEMI backgrounds and extras. */
final class EmiNativePreview implements RecipePreview.Layout,WidgetHolder {
    private final EmiRecipe recipe;
    private final List<Widget> widgets=new ArrayList<>();
    private final RecipePreview.Slots slots;
    private final RecipePreview.Category category;
    EmiNativePreview(EmiRecipe recipe){this(recipe,null);}
    EmiNativePreview(EmiRecipe recipe,RecipeDocument document){
        this.recipe=recipe;
        var c=recipe.getCategory();category=new RecipePreview.Category(c,new RecipePreview.Type(c.getId()),c.getName());
        recipe.addWidgets(this);
        var out=new ArrayList<RecipePreview.Slot>();int input=0;
        for(var widget:widgets)if(widget instanceof SlotWidget slot){
            var role=role(slot);String path=null;
            if(document!=null&&recipe instanceof EmiCraftingRecipe&&role==RecipePreview.Role.INPUT){
                int cell=input++;
                if(document.type().equals("minecraft:crafting_shaped")){
                    var pattern=document.json().getAsJsonArray("pattern");int minX=3,minY=3,maxX=-1,maxY=-1;
                    for(int y=0;y<pattern.size();y++)for(int x=0;x<pattern.get(y).getAsString().length();x++)if(pattern.get(y).getAsString().charAt(x)!=' '){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
                    int width=maxX-minX+1,height=maxY-minY+1;
                    for(int i=0;i<width*height;i++)if(RecipeDocument.emiCraftingIndex(i,width,height)==cell){char symbol=pattern.get(minY+i/width).getAsString().charAt(minX+i%width);if(symbol!=' ')path="key/"+symbol;}
                }else if(cell<document.json().getAsJsonArray("ingredients").size())path="ingredients/"+cell;
                if(path==null)path="$crafting/"+cell;
            }
            out.add(new NativeSlot(slot,role,path));
        }
        slots=new RecipePreview.Slots(List.copyOf(out));
    }
    static Optional<RecipePreview.Layout> rebuild(Recipe<?> recipe,RecipeDocument document){
        // Vanilla constructors must not swallow modded subclasses such as Create mechanical crafting.
        EmiRecipe display=switch(document.type()){
            case "minecraft:crafting_shaped"->new EmiShapedRecipe((ShapedRecipe)recipe);
            case "minecraft:crafting_shapeless"->new EmiShapelessRecipe((ShapelessRecipe)recipe);
            case "minecraft:smelting"->new EmiCookingRecipe((AbstractCookingRecipe)recipe,VanillaEmiRecipeCategories.SMELTING,1,false);
            case "minecraft:blasting"->new EmiCookingRecipe((AbstractCookingRecipe)recipe,VanillaEmiRecipeCategories.BLASTING,2,false);
            case "minecraft:smoking"->new EmiCookingRecipe((AbstractCookingRecipe)recipe,VanillaEmiRecipeCategories.SMOKING,2,false);
            case "minecraft:campfire_cooking"->new EmiCookingRecipe((AbstractCookingRecipe)recipe,VanillaEmiRecipeCategories.CAMPFIRE_COOKING,1,true);
            case "minecraft:stonecutting"->new EmiStonecuttingRecipe((StonecutterRecipe)recipe);
            case "minecraft:smithing_transform"->{
                yield new EmiSmithingRecipe(ingredient(document,"template"),ingredient(document,"base"),ingredient(document,"addition"),EmiStack.of(recipe.getResultItem(Minecraft.getInstance().level.registryAccess())),null);
            }
            default->null;
        };
        return display==null?Optional.empty():Optional.of(new EmiNativePreview(display,document));
    }
    private static EmiIngredient ingredient(RecipeDocument document,String path){
        var field=document.fields().stream().filter(f->f.label().equals(path)).findFirst().orElseThrow();
        return EmiIngredient.of(document.values(field,Minecraft.getInstance().level.registryAccess()).stream().filter(ItemStack.class::isInstance).map(ItemStack.class::cast).map(EmiStack::of).toList());
    }
    public int getWidth(){return Math.max(1,recipe.getDisplayWidth());}
    public int getHeight(){return Math.max(1,recipe.getDisplayHeight());}
    public <T extends Widget> T add(T widget){widgets.add(widget);return widget;}
    public Rect2i getRect(){return new Rect2i(0,0,getWidth(),getHeight());}
    public RecipePreview.Category getRecipeCategory(){return category;}
    public RecipePreview.Slots getRecipeSlotsView(){return slots;}
    public Object nativeLayout(){return recipe;}
    public boolean usesEmiCraftingGrid(){return recipe instanceof EmiCraftingRecipe;}
    public void drawRecipe(GuiGraphics g,int mx,int my){
        float delta=Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        for(var widget:widgets){g.pose().pushPose();try{widget.render(g,mx,my,delta);}finally{g.pose().popPose();}}
    }
    private RecipePreview.Role role(SlotWidget slot){
        if(ModList.get().isLoaded("jei")){
            var jemi=EmiJemiPreview.role(slot);if(jemi.isPresent())return jemi.get();
        }
        if(slot.getRecipe()!=null)return RecipePreview.Role.OUTPUT;
        var stack=slot.getStack();
        if(recipe.getCatalysts().stream().anyMatch(s->EmiIngredient.areEqual(s,stack)))return RecipePreview.Role.CATALYST;
        if(recipe instanceof EmiCraftingRecipe||recipe.getInputs().stream().anyMatch(s->EmiIngredient.areEqual(s,stack)))return RecipePreview.Role.INPUT;
        return RecipePreview.Role.RENDER_ONLY;
    }
    private static List<Object> values(EmiIngredient value){
        return value.getEmiStacks().stream().map(VjrmEmiPlugin::ingredient).filter(Objects::nonNull).toList();
    }
    private static final class NativeSlot implements RecipePreview.Slot {
        private final SlotWidget widget;
        private final RecipePreview.Role role;
        private final String path;
        private List<?> override;
        private static Field stackField;
        NativeSlot(SlotWidget widget,RecipePreview.Role role,String path){this.widget=widget;this.role=role;this.path=path;}
        public String fieldPath(){return path;}
        public Rect2i getRect(){var b=widget.getBounds();return new Rect2i(b.x(),b.y(),b.width(),b.height());}
        public RecipePreview.Role getRole(){return role;}
        public Stream<RecipePreview.Value> getAllIngredients(){return (override==null?values(widget.getStack()):override).stream().map(RecipePreview.Value::new);}
        public Object nativeSlot(){return widget;}
        public List<net.minecraft.network.chat.Component> tooltip(){return IngredientTooltips.identify(widget.getStack().getEmiStacks().isEmpty()?List.of():widget.getStack().getEmiStacks().get(0).getTooltipText(),getDisplayedIngredient().map(RecipePreview.Value::getIngredient).orElse(null));}
        public void override(List<?> values){
            // Only freshly created preview widgets are changed. Reuse their own renderers, tank
            // geometry, backgrounds and overlays instead of replacing them with generic slots.
            var stacks=values.stream().map(v->v instanceof ItemStack i?EmiStack.of(i):v instanceof FluidStack f?NeoForgeEmiStack.of(f):VjrmEmiPlugin.chemicalStack(v)).toList();
            EmiIngredient changed=EmiIngredient.of(stacks,stacks.isEmpty()?1:stacks.getFirst().getAmount()).setChance(widget.getStack().getChance());
            if(EmiIngredient.areEqual(widget.getStack(),changed)&&widget.getStack().getAmount()==changed.getAmount()){override=List.copyOf(values);return;}
            try{
                if(stackField==null){stackField=SlotWidget.class.getDeclaredField("stack");stackField.setAccessible(true);}
                stackField.set(widget,changed);
                if(!EmiIngredient.areEqual(widget.getStack(),changed))throw new IllegalStateException("This generated EMI slot cannot display an override");
                override=List.copyOf(values);
            }catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot update the EMI preview slot",ex);}
        }
    }
}
