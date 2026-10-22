package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.*;
import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;
import java.util.stream.Stream;

@EmiEntrypoint
public final class VjrmEmiPlugin implements EmiPlugin, RecipePreview.Provider {
    private RecipeEditorScreen initializedScreen;
    private int initializedWidth,initializedHeight;
    @Override public void initialized(RecipeEditorScreen screen){
        dev.emi.emi.screen.EmiScreenManager.addWidgets(screen);
        var search=dev.emi.emi.screen.EmiScreenManager.search;
        int right=screen.guiLeft()+screen.guiWidth(),rightSpace=screen.width-right,leftSpace=screen.guiLeft();
        boolean left=dev.emi.emi.config.EmiConfig.searchSidebar==dev.emi.emi.config.SidebarSide.LEFT;
        if((left?leftSpace:rightSpace)<40)left=leftSpace>rightSpace;
        int space=left?leftSpace:rightSpace;
        search.setX(left?3:right+3);search.setY(screen.height-22);search.setWidth(Math.max(20,space-6));
        search.setVisible(space>=40&&dev.emi.emi.config.EmiConfig.searchSidebar!=dev.emi.emi.config.SidebarSide.NONE);
        dev.emi.emi.screen.EmiScreenManager.forceRecalculate();
        initializedScreen=screen;initializedWidth=screen.width;initializedHeight=screen.height;
    }
    // EMI 1.1's automatic hooks only target AbstractContainerScreen. This editor is a Screen.
    @Override public void background(GuiGraphics g,int x,int y,float delta){
        if(Minecraft.getInstance().screen instanceof RecipeEditorScreen screen&&(screen!=initializedScreen||screen.width!=initializedWidth||screen.height!=initializedHeight))initialized(screen);
        dev.emi.emi.screen.EmiScreenManager.drawBackground(dev.emi.emi.runtime.EmiDrawContext.wrap(g),x,y,delta);
    }
    @Override public void overlay(GuiGraphics g,int x,int y,float delta){
        var context=dev.emi.emi.runtime.EmiDrawContext.wrap(g);g.pose().pushPose();g.pose().translate(0,0,400);
        dev.emi.emi.screen.EmiScreenManager.render(context,x,y,delta);
        dev.emi.emi.screen.EmiScreenManager.drawForeground(context,x,y,delta);g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){return dev.emi.emi.screen.EmiScreenManager.mouseClicked(x,y,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return dev.emi.emi.screen.EmiScreenManager.mouseReleased(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return dev.emi.emi.screen.EmiScreenManager.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return dev.emi.emi.screen.EmiScreenManager.mouseScrolled(x,y,vertical);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(!EmiApi.isSearchFocused()&&Minecraft.getInstance().screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox)return false;
        return dev.emi.emi.screen.EmiScreenManager.keyPressed(key,scan,modifiers);
    }
    @Override public boolean charTyped(char character,int modifiers){return EmiApi.isSearchFocused()&&dev.emi.emi.screen.EmiScreenManager.search.charTyped(character,modifiers);}
    @Override public void register(EmiRegistry registry) {
        RecipePreview.install(this);
        registry.addScreenBoundsProvider(RecipeEditorScreen.class,s->new Bounds(s.guiLeft(),s.guiTop(),s.guiWidth(),s.guiHeight()));
        registry.addExclusionArea(RecipeEditorScreen.class,(s,out)->{
            out.accept(new Bounds(s.guiLeft(),s.guiTop(),s.guiWidth(),s.guiHeight()));
            var search=dev.emi.emi.screen.EmiScreenManager.search;
            if(search.visible)out.accept(new Bounds(search.getX()-2,search.getY()-2,search.getWidth()+4,22));
        });
        registry.addDragDropHandler(RecipeEditorScreen.class,new EmiDragDropHandler<RecipeEditorScreen>() {
            @Override public boolean dropStack(RecipeEditorScreen screen,EmiIngredient stack,int x,int y){
                Object value=ingredient(stack);if(value==null)return false;
                for(var target:screen.dropTargets(value))if(target.area().contains(x,y)){screen.accept(target.field(),value);return true;}return false;
            }
            @Override public void render(RecipeEditorScreen screen,EmiIngredient stack,GuiGraphics g,int mx,int my,float delta){
                Object value=ingredient(stack);if(value==null)return;
                for(var target:screen.dropTargets(value)){var r=target.area();g.fill(r.getX(),r.getY(),r.getX()+r.getWidth(),r.getY()+r.getHeight(),0x6633BB77);}
            }
        });

    }
    /** Called after EMI assembles the real recipe page, independently of its dev decorators. */
    public static void addCopyButton(dev.emi.emi.screen.WidgetGroup group){
        if(group.recipe==null||sourceId(group.recipe)==null||group.widgets.stream().anyMatch(w->w instanceof VjrmEmiPlugin.CopyWidget))return;
        int x=group.getWidth()+5,y=0;
        boolean found=false;
        for(int side:new int[]{group.getWidth()+5,-17}){
            for(int row=0;row+12<=Math.max(12,group.getHeight());row+=14){
                final int cx=side,cy=row;
                boolean occupied=group.widgets.stream().map(Widget::getBounds).anyMatch(b->cx<b.x()+b.width()&&cx+12>b.x()&&cy<b.y()+b.height()&&cy+12>b.y());
                if(!occupied){x=side;y=row;found=true;break;}
            }if(found)break;
        }
        if(!found){x=group.getWidth()+19;y=0;}
        group.add(new CopyWidget(group.recipe,x,y));
    }
    public static Object ingredient(EmiIngredient ingredient){
        if(ingredient==null||ingredient.getEmiStacks().isEmpty())return null;
        var stack=ingredient.getEmiStacks().getFirst();
        if(stack.getKey() instanceof Fluid fluid){var value=new FluidStack(fluid,(int)Math.max(1,Math.min(Integer.MAX_VALUE,stack.getAmount())));value.applyComponents(stack.getComponentChanges());return value;}
        try{var value=stack.getClass().getField("ingredient").get(stack);if(ChemicalSupport.isStack(value))return ChemicalSupport.resized(value,stack.getAmount());}catch(ReflectiveOperationException ignored){}
        try{var value=stack.getClass().getMethod("getStack").invoke(stack);if(ChemicalSupport.isStack(value))return value;}catch(ReflectiveOperationException ignored){}
        var item=stack.getItemStack();return item.isEmpty()?null:item;
    }
    static EmiStack chemicalStack(Object value){
        if(!ChemicalSupport.isStack(value))return EmiStack.EMPTY;value=ChemicalSupport.unwrap(value);
        try{var type=Class.forName("mekanism.client.recipe_viewer.emi.ChemicalEmiStack");for(var constructor:type.getConstructors())if(constructor.getParameterCount()==1&&constructor.getParameterTypes()[0].isInstance(value))return (EmiStack)constructor.newInstance(value);}catch(ReflectiveOperationException ignored){}
        return net.neoforged.fml.ModList.get().isLoaded("jei")?EmiJemiPreview.stack(value):EmiStack.EMPTY;
    }
    private static ResourceLocation sourceId(EmiRecipe recipe){
        var mc=Minecraft.getInstance();if(mc.level==null)return null;
        if(net.neoforged.fml.ModList.get().isLoaded("jei")){
            var source=EmiJemiPreview.sourceId(recipe);if(source!=null)return source;
        }
        var backing=recipe.getBackingRecipe();if(backing!=null)return backing.id();
        var id=recipe.getId();if(id!=null&&recipe.getClass().getSimpleName().equals("GTEmiRecipe")&&recipe.getClass().getName().startsWith("com.gregtechceu.gtceu."))return id;return id!=null&&!id.getPath().startsWith("/")&&mc.level.getRecipeManager().byKey(id).isPresent()?id:null;
    }
    @Override public boolean drawIngredientIcon(GuiGraphics g,Object ingredient,int x,int y){
        dev.emi.emi.api.stack.EmiStack stack;
        if(ingredient instanceof net.minecraft.world.item.ItemStack item)stack=dev.emi.emi.api.stack.EmiStack.of(item);
        else if(ingredient instanceof net.neoforged.neoforge.fluids.FluidStack fluid)stack=dev.emi.emi.api.neoforge.NeoForgeEmiStack.of(fluid);else return false;
        stack.render(g,x,y,0,dev.emi.emi.api.stack.EmiIngredient.RENDER_ICON);return true;
    }
    @Override public boolean drawCategoryIcon(GuiGraphics g,RecipePreview.Category category,int x,int y){
        ((EmiRecipeCategory)category.nativeCategory()).render(g,x,y,0);return true;
    }
    public RecipePreview.Category category(Object value){
        var c=(EmiRecipeCategory)value;return new RecipePreview.Category(c,new RecipePreview.Type(c.getId()),c.getName());
    }
    public Optional<RecipePreview.Layout> layout(Recipe<?> recipe,RecipePreview.Category preferred,RecipeDocument document){
        if(com.pureneko.vjrm.recipe.GtRecipeSupport.isRecipe(recipe)&&preferred!=null&&preferred.nativeCategory() instanceof EmiRecipeCategory category){
            for(String name:List.of("com.gregtechceu.gtceu.integration.recipeviewer.emi.recipe.GTEmiRecipe","com.gregtechceu.gtceu.integration.emi.recipe.GTEmiRecipe"))try{
                var type=Class.forName(name);for(var constructor:type.getConstructors())if(constructor.getParameterCount()==2&&constructor.getParameterTypes()[0].isInstance(recipe)&&constructor.getParameterTypes()[1].isInstance(category))return Optional.of(new EmiNativePreview((EmiRecipe)constructor.newInstance(recipe,category)));
            }catch(ReflectiveOperationException|LinkageError ignored){}
        }
        var nativeLayout=EmiNativePreview.rebuild(recipe,document);
        if(nativeLayout.isPresent())return nativeLayout;
        if(net.neoforged.fml.ModList.get().isLoaded("jei"))return EmiJemiPreview.rebuild(recipe,preferred,document);
        return Optional.empty();
    }
    public Optional<RecipePreview.Layout> sourceLayout(RecipePreview.Category category,Object display){
        if(!(display instanceof EmiRecipe recipe))return Optional.empty();
        try{return Optional.of(new EmiNativePreview(recipe));}catch(RuntimeException ex){return Optional.empty();}
    }
    @Override public Optional<RecipePreview.Layout> fallbackLayout(Recipe<?> recipe,RecipePreview.Category category,RecipeDocument document){return Optional.of(new DocumentLayout(document,recipe,category));}
    public List<RecipePreview.Template> templates(List<RecipeSource.Sample> samples){
        var ids=new HashMap<ResourceLocation,String>();samples.forEach(s->ids.put(ResourceLocation.parse(s.id()),s.serializer()));
        var found=new TreeMap<String,RecipePreview.Template>();
        for(var recipe:EmiApi.getRecipeManager().getRecipes()){
            var id=sourceId(recipe);String serializer=ids.get(id);
            if(serializer!=null){var c=category(recipe.getCategory());found.putIfAbsent(serializer+"/"+c.getRecipeType().getUid(),new RecipePreview.Template(serializer,id,c,recipe));}
        }
        // JSON-backed serializers remain accessible even if a mod has no dedicated EMI category.
        for(var sample:samples)if(found.values().stream().noneMatch(t->t.serializer().equals(sample.serializer()))){
            var id=ResourceLocation.parse(sample.serializer());var c=new RecipePreview.Category(null,new RecipePreview.Type(id),Component.literal(sample.serializer()));
            found.put(sample.serializer(),new RecipePreview.Template(sample.serializer(),ResourceLocation.parse(sample.id()),c,null));
        }
        return List.copyOf(found.values());
    }
    /** Last-resort preview only for serializers with no native EMI or JEMI layout. */
    private final class DocumentLayout implements RecipePreview.Layout {
        private final List<RecipePreview.Slot> slots=new ArrayList<>();
        private final RecipePreview.Category category;
        private final RecipeDocument document;
        private final int rows;
        DocumentLayout(RecipeDocument document,Recipe<?> recipe,RecipePreview.Category preferred){
            this.document=document;
            var id=net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            category=preferred==null?new RecipePreview.Category(null,new RecipePreview.Type(id),Component.literal(id.toString())):preferred;
            boolean grid=recipe instanceof ShapedRecipe&&document.json().has("pattern")&&document.json().has("key");
            int input=0,output=0;
            if(grid){
                var pattern=document.json().getAsJsonArray("pattern");int width=pattern.get(0).getAsString().length(),height=pattern.size();
                for(int y=0;y<Math.max(3,height);y++)for(int x=0;x<Math.max(3,width);x++){
                    char symbol=y<height&&x<width?pattern.get(y).getAsString().charAt(x):' ';
                    String path=symbol==' '?"$crafting/"+(y*3+x):"key/"+symbol;
                    // Empty cells can only be inserted into vanilla 3x3 crafting recipes.
                    if(symbol==' '&&!document.type().equals("minecraft:crafting_shaped"))continue;
                    add(path,false,x*20,y*20);input=Math.max(input,(y+1)*Math.max(3,width));
                }
            }
            for(var field:document.fields()){
                if(grid&&field.path().getFirst().equals("key"))continue;
                int index=field.output()?output++:input++;
                add(field.label(),field.output(),field.output()?190+index%4*22:index%8*20,field.output()?index/4*22:index/8*22);
            }
            if(recipe instanceof ShapelessRecipe)for(int i=input;i<9;i++)add("$crafting/"+i,false,i%8*20,i/8*22);
            rows=Math.max(3,Math.max((input+7)/8,(output+3)/4));
        }
        private void add(String path,boolean output,int x,int y){
            var values=document.fields().stream().filter(f->f.label().equals(path)).findFirst().map(f->document.values(f,Minecraft.getInstance().level.registryAccess())).orElse(List.of());
            slots.add(new DocumentSlot(path,new Rect2i(x,y,18,18),output?RecipePreview.Role.OUTPUT:RecipePreview.Role.INPUT,values));
        }
        public Rect2i getRect(){int bottom=slots.stream().mapToInt(s->s.getRect().getY()+18).max().orElse(60);return new Rect2i(0,0,282,Math.max(rows*22,bottom)+24);}
        public RecipePreview.Category getRecipeCategory(){return category;}
        public RecipePreview.Slots getRecipeSlotsView(){return new RecipePreview.Slots(List.copyOf(slots));}
        public void drawRecipe(GuiGraphics g,int mx,int my){
            for(var slot:slots)((DocumentSlot)slot).widget.render(g,mx,my,0);
            var font=Minecraft.getInstance().font;g.drawString(font,"→",171,22,0xFFFFFF);
            String summary=document.numericFields().stream().filter(f->f.path().size()==1).limit(3).map(f->f.label()+": "+f.value()).reduce((a,b)->a+"  "+b).orElse("");
            g.drawString(font,font.plainSubstrByWidth(summary,282),0,getRect().getHeight()-12,0xFFE3C483);
        }
    }
    private static final class DocumentSlot implements RecipePreview.Slot {
        private final String path;private final Rect2i rect;private final RecipePreview.Role role;private List<?> values;private SlotWidget widget;
        DocumentSlot(String path,Rect2i rect,RecipePreview.Role role,List<?> values){this.path=path;this.rect=rect;this.role=role;override(values);}
        public Rect2i getRect(){return rect;}
        public RecipePreview.Role getRole(){return role;}
        public String fieldPath(){return path;}
        public Stream<RecipePreview.Value> getAllIngredients(){return values.stream().map(RecipePreview.Value::new);}
        public void override(List<?> values){
            this.values=List.copyOf(values);var stacks=values.stream().map(v->v instanceof ItemStack i?EmiStack.of(i):v instanceof FluidStack f?NeoForgeEmiStack.of(f):chemicalStack(v)).toList();
            widget=new SlotWidget(EmiIngredient.of(stacks,stacks.isEmpty()?1:stacks.getFirst().getAmount()),rect.getX(),rect.getY());
        }
    }
    private static final class CopyWidget extends Widget {
        private final EmiRecipe recipe;private final int x,y;
        CopyWidget(EmiRecipe recipe,int x,int y){this.recipe=recipe;this.x=x;this.y=y;}
        public Bounds getBounds(){return VjrmClientConfig.copyButton()?new Bounds(x,y,12,12):Bounds.EMPTY;}
        public void render(GuiGraphics g,int mx,int my,float delta){if(VjrmClientConfig.copyButton()){
            g.fill(x,y,x+12,y+12,getBounds().contains(mx,my)?0xFFB9D5BD:0xFF9B9B9B);g.renderOutline(x,y,12,12,0xFF373737);
            g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(0.75f,0.75f,1);g.renderItem(new ItemStack(Items.WRITABLE_BOOK),0,0);g.pose().popPose();
        }}
        public List<ClientTooltipComponent> getTooltip(int mx,int my){return VjrmClientConfig.copyButton()?List.of(ClientTooltipComponent.create(Component.translatable("vjrm.copy").getVisualOrderText())):List.of();}
        public boolean mouseClicked(int mx,int my,int button){
            if(button!=0||!VjrmClientConfig.copyButton()||!getBounds().contains(mx,my))return false;
            var id=sourceId(recipe);if(id==null)return false;
            var mc=Minecraft.getInstance();var parent=mc.screen;
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK,1f));
            RecipeSources.request(id,document->{if(mc.screen==parent)mc.setScreen(new RecipeEditorScreen(document,RecipePreview.category(recipe.getCategory()),id+"_copy",parent,recipe));},error->{if(mc.player!=null)mc.player.displayClientMessage(Component.translatable("vjrm.error.copy",Component.translatable(error)),false);});return true;
        }
    }
}
