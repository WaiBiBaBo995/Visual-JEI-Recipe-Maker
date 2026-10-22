package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.RecipeDocument;
import com.pureneko.vjrm.recipe.ChemicalSupport;
import com.pureneko.vjrm.recipe.RecipeDocument.Field;
import com.pureneko.vjrm.export.DatapackExporter;
import com.pureneko.vjrm.network.RecipeNetwork;
import com.pureneko.vjrm.client.RecipePreview.Layout;
import com.pureneko.vjrm.client.RecipePreview.Slot;
import com.pureneko.vjrm.client.RecipePreview.Role;
import com.pureneko.vjrm.client.RecipePreview.Category;
import com.pureneko.vjrm.client.LegacyGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import com.pureneko.vjrm.platform.FluidStack;

import java.util.*;

public final class RecipeEditorScreen extends VjrmScreen {
    public record DropTarget(Rect2i area, Field field) { }
    private final Screen parent;
    private RecipeDocument document;
    java.util.List<RecipeDocument.Field> annotationFields(){return document.manualFields();}
    private Category preferred;
    private Layout layout;
    private Object sourceDisplay;
    private RecipeDocument sourceDocument;
    private boolean usingSourceLayout;
    private final Map<String,List<Field>> repeatedBindings=new HashMap<>();
    private final Map<Integer,String> sourceBindings=new HashMap<>();
    private float uiScale=1;
    private ItemStack draggedInventory=ItemStack.EMPTY;
    private double dragStartX,dragStartY;
    private boolean inventoryDragging;
    private List<Field> fields = List.of();
    private final Map<Slot, Field> bindings = new LinkedHashMap<>();
    private final Map<Integer, String> manualBindings = new HashMap<>();
    private Field selected;
    private Slot selectedSlot;
    private EditBox idBox, valueBox, countBox;
    private Button fieldButton, overwriteButton, serverButton, clientButton;
    private String recipeId = "vjrm:my_recipe", value = "", amount = "1";
    private boolean overwrite;
    private Component status = Component.translatable("vjrm.help");
    private float previewScale = 1;
    private int previewX, previewY;
    private Component clickReason;
    private int reasonX,reasonY;
    private long reasonUntil;
    public RecipeEditorScreen() { this(RecipeDocument.template("crafting_shaped"), null, "vjrm:my_recipe", null); }
    public RecipeEditorScreen(RecipeDocument document, Object preferred, String id, Screen parent) {
        this(document,preferred,id,parent,null);
    }
    public RecipeEditorScreen(RecipeDocument document,Object preferred,String id,Screen parent,Object sourceDisplay){
        super(Component.translatable("screen.vjrm.editor"));this.document=document;this.preferred=RecipePreview.category(preferred);recipeId=id;this.parent=parent;this.sourceDisplay=sourceDisplay;sourceDocument=new RecipeDocument(document.json());
    }
    public int panelWidth() { return 322; }
    public int panelHeight() { return 414; }
    public int panelX() { return Math.round((width/uiScale-panelWidth())/2); }
    public int panelY() { return Math.round((height/uiScale-panelHeight())/2); }
    public int guiLeft(){return Math.round(panelX()*uiScale);}
    public int guiTop(){return Math.round(panelY()*uiScale);}
    public int guiWidth(){return Math.round(panelWidth()*uiScale);}
    public int guiHeight(){return Math.round(panelHeight()*uiScale);}
    private int controlsY() { return panelY()+panelHeight()-226; }
    public void loadTemplate(RecipeDocument document,Object category,Object display){
        this.document=new RecipeDocument(document.json());sourceDocument=new RecipeDocument(document.json());preferred=RecipePreview.category(category);sourceDisplay=display;sourceBindings.clear();manualBindings.clear();selected=null;status=t("template_hint");
    }
    private Component t(String key,Object... args) {
        Object[] translated=Arrays.stream(args).map(a->a instanceof String s && s.startsWith("vjrm.error.")?Component.translatable(s):a).toArray();
        return Component.translatable("vjrm."+key,translated);
    }
    private Button button(int x,int y,int w,String key,Button.OnPress press) { return addRenderableWidget(Button.builder(t(key),press).bounds(x,y,w,18).build()); }
    @Override protected void init() {
        uiScale=Math.max(0.2f,Math.min(1f,Math.min((height-12f)/panelHeight(),(width-12f)/panelWidth())));
        draggedInventory=ItemStack.EMPTY;inventoryDragging=false;
        int x=panelX()+8,y=panelY(),w=panelWidth()-16,c=controlsY();
        var typeButton=button(x,y+22,92,"type",b->minecraft.setScreen(new RecipeTypeScreen(this)));
        button(x+96,y+22,44,"numbers",b->minecraft.setScreen(new NumericFieldsScreen(this,document,()->{refresh();status=t("changed","JSON");})));
        typeButton.setTooltip(Tooltip.create(t("type.name",document.type())));
        idBox=addRenderableWidget(new EditBox(font,x+144,y+22,w-144,18,t("recipe_id")));idBox.setMaxLength(200);idBox.setValue(recipeId);idBox.setResponder(s->recipeId=s);idBox.setTooltip(Tooltip.create(t("recipe_id")));
        fieldButton=button(x,y+60,w-128,"field",b->cycleField());
        button(x+w-124,y+60,60,"manual.open",b->openManualFields());
        button(x+w-60,y+60,60,"bind",b->{if(selectedSlot!=null && selected!=null){manualBindings.put(drawSlots().indexOf(selectedSlot),selected.label());repeatedBindings.remove(selected.label());bindings.put(selectedSlot,selected);status=t("bound",selected.label());}else status=t("select_slot");});
        valueBox=addRenderableWidget(new EditBox(font,x,c+22,w-53,18,t("value")));valueBox.setMaxLength(com.pureneko.vjrm.recipe.RecipeSource.MAX_JSON);valueBox.setValue(value);valueBox.setHint(t("value"));valueBox.setResponder(s->value=s);
        countBox=addRenderableWidget(new EditBox(font,x+w-49,c+22,49,18,t("amount")));countBox.setMaxLength(19);countBox.setValue(amount);countBox.setResponder(s->amount=s);
        int bw=(w-4)/2;
        button(x,c+44,bw,"apply",b->applyValue());
        button(x+bw+4,c+44,w-bw-4,"json",b->minecraft.setScreen(new JsonDraftScreen(this,document.json(),j->{
            boolean changedType=!document.type().equals(j.get("type").getAsString());document.replace(j);selected=null;manualBindings.clear();sourceBindings.clear();
            if(changedType){preferred=null;sourceDisplay=null;sourceDocument=new RecipeDocument(j);}
        })));
        overwriteButton=button(x,c+66,w,"overwrite.off",b->{overwrite=!overwrite;overwriteButton.setMessage(t(overwrite?"overwrite.on":"overwrite.off"));});overwriteButton.setMessage(t(overwrite?"overwrite.on":"overwrite.off"));
        serverButton=button(x,c+88,(w-4)/2,"export.server",b->export(false));clientButton=button(x+(w-4)/2+4,c+88,(w-4)/2,"export.client",b->export(true));
        refresh();RecipePreview.hooks().initialized(this);
    }
    private List<Slot> drawSlots() {
        if(layout==null)return List.of();
        return layout.getRecipeSlotsView().getSlotViews();
    }
    private void refresh() {
        String path=selected==null?null:selected.label();fields=document.fields();selected=fields.stream().filter(f->f.label().equals(path)).findFirst().orElse(fields.isEmpty()?null:fields.getFirst());
        layout=null;bindings.clear();repeatedBindings.clear();selectedSlot=null;
        try {if(minecraft.level!=null){
            Recipe<?> recipe=document.decode(minecraft.level.registryAccess());layout=RecipePreview.layout(recipe,RecipePreview.proxy(preferred,document.type())?null:preferred,document).orElse(null);usingSourceLayout=false;
            if(layout==null&&preferred!=null&&sourceDisplay!=null){layout=RecipePreview.sourceLayout(preferred,sourceDisplay).orElse(null);usingSourceLayout=layout!=null;}
            if(layout==null){layout=RecipePreview.fallbackLayout(recipe,preferred,document).orElse(null);usingSourceLayout=false;}
            if(layout==null)status=t("error.layout");else{
                var r=layout.getRect();int previewTop=panelY()+84,space=Math.max(20,controlsY()+18-previewTop-(usingSourceLayout?20:0));
                previewScale=Math.min(1f,Math.min((panelWidth()-24f)/Math.max(1,r.getWidth()),space/(float)Math.max(1,r.getHeight())));
                previewX=panelX()+(panelWidth()-Math.round(r.getWidth()*previewScale))/2;previewY=previewTop+(space-Math.round(r.getHeight()*previewScale))/2;layout.setPosition(0,0);buildBindings(recipe);
                if(usingSourceLayout)overrideSourceSlots();
            }
        }}catch(RuntimeException ex){status=t("error.invalid",ex.getMessage());}
        updateField();if(clientButton!=null)clientButton.active=layout!=null;
        if(serverButton!=null)serverButton.active=layout!=null && minecraft.getConnection()!=null && com.pureneko.vjrm.network.VjrmNetworking.exportAvailable();
    }
    private void buildBindings(Recipe<?> recipe) {
        var slots=drawSlots();var ins=slots.stream().filter(s->s.getRole()==Role.INPUT).toList();
        boolean vanillaGrid=layout.getRecipeCategory().getRecipeType().getUid().toString().equals("minecraft:crafting");
        boolean mechanical=layout.getRecipeCategory().getRecipeType().getUid().toString().equals("create:mechanical_crafting");
        if(recipe instanceof ShapedRecipe && (vanillaGrid && ins.size()==9 || mechanical) && document.json().has("key")){
            var rows=document.json().getAsJsonArray("pattern");int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=-1,maxY=-1;
            for(int y=0;y<rows.size();y++)for(int x=0;x<rows.get(y).getAsString().length();x++)if(rows.get(y).getAsString().charAt(x)!=' '){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
            int rw=maxX-minX+1,rh=maxY-minY+1;
            for(int y=0;y<rh;y++)for(int x=0;x<rw;x++){
                char symbol=rows.get(y+minY).getAsString().charAt(x+minX);if(symbol==' ')continue;
                String path="key/"+symbol;Field f=fields.stream().filter(a->a.label().equals(path)).findFirst().orElse(null);
                if(f!=null){int index=mechanical?y*rw+x:craftingIndex(y*rw+x,rw,rh);if(index<ins.size())bindings.put(ins.get(index),f);}
            }
        }else if(recipe instanceof ShapelessRecipe && vanillaGrid && ins.size()==9){
            var inputs=fields.stream().filter(f->f.label().startsWith("ingredients/")).toList();int size=inputs.size()>4?3:inputs.size()>1?2:1;
            for(int i=0;i<inputs.size();i++)bindings.put(ins.get(craftingIndex(i,size,size)),inputs.get(i));
        }
        if(document.type().equals("create:sequenced_assembly") && layout.getRecipeCategory().getRecipeType().getUid().toString().equals("create:sequenced_assembly")){
            var visible=new ArrayList<Field>();findField("ingredient").ifPresent(visible::add);
            var steps=document.json().getAsJsonArray("sequence");
            for(int step=0;step<steps.size();step++){
                String type=steps.get(step).getAsJsonObject().get("type").getAsString(),prefix="sequence/"+step+"/ingredients/";
                if(type.equals("create:deploying"))findField(prefix+"1").ifPresent(visible::add);
                else if(type.equals("create:filling"))fields.stream().filter(f->f.fluid()&&f.label().startsWith(prefix)).findFirst().ifPresent(visible::add);
            }
            if(visible.size()==ins.size())for(int i=0;i<ins.size();i++)bindings.put(ins.get(i),visible.get(i));
            slots.stream().filter(slot->slot.getRole()==Role.OUTPUT).findFirst().ifPresent(slot->findField("results/0").ifPresent(f->bindings.put(slot,f)));
        }
        bindCreateProcessing(slots);
        for(int i=0;i<slots.size();i++){
            var s=slots.get(i);String explicit=manualBindings.get(i);
            if(explicit!=null){fields.stream().filter(f->f.label().equals(explicit)).findFirst().ifPresent(f->bindings.put(s,f));continue;}
            if(s.fieldPath()!=null){if(s.fieldPath().startsWith("$crafting/"))bindings.put(s,new Field(List.of("$crafting",s.fieldPath().substring(10)),false,false));else findField(s.fieldPath()).ifPresent(f->bindings.put(s,f));continue;}
            if(bindings.containsKey(s)||s.getRole()==Role.CATALYST||s.getRole()==Role.RENDER_ONLY)continue;
            if(usingSourceLayout&&sourceBindings.containsKey(i)){
                String path=sourceBindings.get(i);fields.stream().filter(f->f.label().equals(path)).findFirst().ifPresent(f->bindings.put(s,f));continue;
            }
            boolean output=s.getRole()==Role.OUTPUT;
            var matching=usingSourceLayout?sourceDocument:document;
            var candidates=matching.fields().stream().filter(f->fields.stream().anyMatch(current->current.label().equals(f.label()))).filter(f->f.output()==output && s.getAllIngredients().anyMatch(v->matching.matches(f,v.getIngredient(),minecraft.level.registryAccess()))).toList();
            if(candidates.size()==1)bindings.put(s,candidates.getFirst());
            else if(!output && !usingSourceLayout && Set.of("create:mixing","create:compacting").contains(document.type())){
                // Basin pages combine equal item ingredients into a single counted slot.
                var grouped=candidates.stream().filter(f->!f.fluid()&&f.path().size()==2&&f.path().getFirst().equals("ingredients")).toList();
                if(grouped.size()>1 && grouped.size()==candidates.size() && grouped.stream().allMatch(f->document.get(f).equals(document.get(grouped.getFirst())))){
                    bindings.put(s,grouped.getFirst());repeatedBindings.put(grouped.getFirst().label(),grouped);
                }
            }
            if(usingSourceLayout&&bindings.containsKey(s))sourceBindings.put(i,bindings.get(s).label());
        }
        if(vanillaGrid && (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) && ins.size()==9) {
            for(int i=0;i<ins.size();i++)if(ins.get(i).isEmpty()&&ins.get(i).fieldPath()==null)bindings.put(ins.get(i),new Field(List.of("$crafting",Integer.toString(i)),false,false));
        }
    }
    private void bindCreateProcessing(List<Slot> slots){
        if(usingSourceLayout || !Set.of("create:mixing","create:compacting","create:crushing","create:milling","create:pressing","create:cutting","create:splashing","create:haunting","create:filling","create:emptying","create:deploying","create:item_application","create:sandpaper_polishing").contains(document.type()))return;
        boolean basin=Set.of("create:mixing","create:compacting").contains(document.type());
        for(boolean output:new boolean[]{false,true})for(boolean fluid:new boolean[]{false,true}){
            var role=output?Role.OUTPUT:Role.INPUT;
            var typedSlots=slots.stream().filter(slot->slot.getRole()==role && slot.getAllIngredients().anyMatch(v->fluid?v.getIngredient() instanceof FluidStack:v.getIngredient() instanceof ItemStack)).toList();
            var members=fields.stream().filter(f->f.output()==output&&f.fluid()==fluid && f.path().getFirst().equals(output?"results":"ingredients")).toList();
            var groups=new ArrayList<List<Field>>();
            for(var f:members){
                if(basin&&!output&&!fluid){
                    var group=groups.stream().filter(g->document.get(g.getFirst()).equals(document.get(f))).findFirst();
                    if(group.isPresent()){group.get().add(f);continue;}
                }
                var group=new ArrayList<Field>();group.add(f);groups.add(group);
            }
            // Refuse unknown structures rather than assigning a slot to the wrong JSON field.
            if(typedSlots.size()!=groups.size())continue;
            for(int i=0;i<groups.size();i++){
                var group=groups.get(i);bindings.put(typedSlots.get(i),group.getFirst());
                if(basin&&!output&&!fluid)repeatedBindings.put(group.getFirst().label(),List.copyOf(group));
            }
        }
    }
    private Optional<Field> findField(String path){return fields.stream().filter(f->f.label().equals(path)).findFirst();}
    public boolean proxyRecipe(){return RecipePreview.proxy(preferred,document.type());}
    public Component sourceDescription(){return t(proxyRecipe()?"proxy_source":"source_type",RecipePreview.typeLabel(document.type(),proxyRecipe()?null:preferred!=null?preferred:layout==null?null:layout.getRecipeCategory()),preferred==null?"":RecipePreview.typeLabel(preferred.getRecipeType().getUid().toString(),preferred));}
    private void overrideSourceSlots(){
        for(var entry:bindings.entrySet()){
            try{var values=document.values(entry.getValue(),minecraft.level.registryAccess());if(!values.isEmpty())entry.getKey().override(values);}catch(RuntimeException ignored){ }
        }
    }
    private static int craftingIndex(int i,int w,int h){if(w==1)return h==1?4:i*3+1;if(h==1)return i+3;if(w==2)return i+(i>1?1:0)+(i>3?1:0);return h==2?i+3:i;}
    private void cycleField(){if(!fields.isEmpty()){selected=fields.get((fields.indexOf(selected)+1)%fields.size());updateField();}}
    private void openManualFields(){
        int index=selectedSlot==null?-1:drawSlots().indexOf(selectedSlot);
        Field initial=selectedSlot==null?selected:bindings.get(selectedSlot);
        minecraft.setScreen(new ManualFieldScreen(this,document,initial,field->{
            document.addManualField(field);selected=field;
            if(index>=0)manualBindings.put(index,field.label());
            status=t("manual.added","/"+field.label());
        },path->{
            document.removeManualField(path);
            manualBindings.values().removeIf(label->label.equals(com.pureneko.vjrm.recipe.JsonFieldPaths.label(path)));
            selected=null;status=t("manual.removed");
        }));
    }
    private void updateField(){
        if(fieldButton!=null){fieldButton.setMessage(selected==null?t("field"):Component.literal(font.plainSubstrByWidth(selected.label(),fieldButton.getWidth()-10)));fieldButton.setTooltip(selected==null?null:Tooltip.create(Component.literal("/"+selected.label())));}
        if(countBox!=null)countBox.active=selected==null||!selected.raw();
        if(selected!=null&&selected.raw()&&valueBox!=null)valueBox.setValue(document.get(selected).toString());
    }
    @SuppressWarnings("removal") private Rect2i screenRect(Slot slot){var r=slot.getRect();return new Rect2i(previewX+Math.round(r.getX()*previewScale),previewY+Math.round(r.getY()*previewScale),Math.max(1,Math.round(r.getWidth()*previewScale)),Math.max(1,Math.round(r.getHeight()*previewScale)));}
    public List<DropTarget> dropTargets(Object ingredient){
        List<DropTarget> out=new ArrayList<>();for(var e:bindings.entrySet())if(accepts(e.getValue(),ingredient))out.add(new DropTarget(physical(screenRect(e.getKey())),e.getValue()));
        if(selected!=null && accepts(selected,ingredient))out.add(new DropTarget(physical(new Rect2i(panelX()+8,controlsY()+22,panelWidth()-69,18)),selected));return out;
    }
    private Rect2i physical(Rect2i r){return new Rect2i(Math.round(r.getX()*uiScale),Math.round(r.getY()*uiScale),Math.max(1,Math.round(r.getWidth()*uiScale)),Math.max(1,Math.round(r.getHeight()*uiScale)));}
    private boolean accepts(Field f,Object ingredient){return !f.raw()&&(!f.chemical().isEmpty()?ChemicalSupport.isStack(ingredient)&&f.chemical().equals(ChemicalSupport.stackKind(ingredient)):f.fluid()?ingredient instanceof FluidStack:ingredient instanceof ItemStack);}
    public void accept(Field field,Object ingredient){accept(field,ingredient,false);}
    private void accept(Field field,Object ingredient,boolean editQuantity){
        try{long n=editQuantity?Long.parseLong(amount):field.path().getFirst().equals("$crafting")?1:!field.chemical().isEmpty()?document.chemicalAmount(field):document.quantity(field);if(n<1)throw new IllegalArgumentException("vjrm.error.amount");var old=document.json();
            try{
                if(field.path().getFirst().equals("$crafting") && ingredient instanceof ItemStack item) {document.addCraftingInput(Integer.parseInt(field.path().getLast()),item,layout!=null&&layout.usesEmiCraftingGrid());manualBindings.clear();}
                else if(repeatedBindings.containsKey(field.label())){var group=repeatedBindings.get(field.label());document.acceptRepeated(group,ingredient,editQuantity?Math.toIntExact(n):group.size(),minecraft.level.registryAccess());manualBindings.clear();}
                else document.accept(field,ingredient,n,minecraft.level.registryAccess());
                document.decode(minecraft.level.registryAccess());
            }catch(RuntimeException ex){document.replace(old);throw ex;}
            selected=field;refresh();status=t("changed",field.label());
        }catch(RuntimeException ex){status=t("error.invalid",ex.getMessage());}
    }
    private void applyValue(){
        if(selected==null)return;
        if(selected.raw()){
            var before=document.json();
            try{document.set(selected,JsonSyntax.parseElement(value));document.decode(minecraft.level.registryAccess());refresh();status=t("changed",selected.label());}
            catch(RuntimeException ex){document.replace(before);status=t("error.invalid",ex.getMessage());}
            return;
        }
        try{if(!selected.chemical().isEmpty()){
            if(value.startsWith("#")){var before=document.json();try{document.chemicalTag(selected,value.substring(1),Long.parseLong(amount));document.decode(minecraft.level.registryAccess());}catch(RuntimeException ex){document.replace(before);throw ex;}refresh();status=t("changed",selected.label());}
            else accept(selected,ChemicalSupport.fromId(value,1,selected.chemical(),minecraft.level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE)),true);
        }else if(value.startsWith("#")){var before=document.json();try{int n=Integer.parseInt(amount);if(repeatedBindings.containsKey(selected.label())){document.tagRepeated(repeatedBindings.get(selected.label()),value.substring(1),n);manualBindings.clear();}else document.tag(selected,value.substring(1),n);document.decode(minecraft.level.registryAccess());}catch(RuntimeException ex){document.replace(before);throw ex;}refresh();status=t("changed",selected.label());}
            else{var id=Identifier.parse(value);if(selected.fluid()){
                if(!net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(id))throw new IllegalArgumentException(value);accept(selected,new FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(id),1),true);
            }else{if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id))throw new IllegalArgumentException(value);accept(selected,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id)),true);}}
        }catch(RuntimeException ex){status=t("error.invalid",ex.getMessage());}
    }
    private void reject(Component reason,double x,double y){
        status=reason;clickReason=reason;reasonX=(int)x;reasonY=(int)y;
        reasonUntil=net.minecraft.util.Util.getMillis()+3500;
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.VILLAGER_NO,1f));
    }
    private void remove(double x,double y){
        if(selected==null)return;
        if(selected.path().getFirst().equals("$crafting")){reject(t("error.empty_slot"),x,y);return;}
        if(selected.output()){reject(t("error.output_required"),x,y);return;}
        boolean removable=selected.path().size()==2&&(selected.path().getFirst().equals("key")||selected.path().getFirst().equals("ingredients"));
        if(!removable){reject(t("error.required"),x,y);return;}
        var old=document.json();
        try{
            var group=new ArrayList<>(repeatedBindings.getOrDefault(selected.label(),List.of(selected)));Collections.reverse(group);
            for(var f:group)document.remove(f);
            try{document.decode(minecraft.level.registryAccess());}catch(RuntimeException ex){document.replace(old);reject(t("error.last_input",ex.getMessage()),x,y);return;}
            manualBindings.clear();refresh();status=t("changed","JSON");clickReason=null;
        }catch(RuntimeException ex){document.replace(old);reject(t("error.invalid",ex.getMessage()),x,y);}
    }
    private void export(boolean client){
        try{if(layout==null)throw new IllegalStateException("vjrm.error.layout");var id=Identifier.parse(recipeId);document.decode(minecraft.level.registryAccess());var json=document.json();
            if(client){var root=minecraft.gameDirectory.toPath().resolve("vjrm_exports/vjrm_generated").toAbsolutePath();DatapackExporter.writePackMeta(root);var file=DatapackExporter.export(root,id.getNamespace(),id.getPath(),json,overwrite);status=t("exported",file.toString());}
            else{if(json.toString().length()>24000)throw new IllegalArgumentException("vjrm.error.size");com.pureneko.vjrm.network.VjrmNetworking.sendToServer(new RecipeNetwork.ExportRequest(recipeId,json.toString(),overwrite));status=t("sending");}
        }catch(Exception ex){status=t("error.invalid",ex.getMessage());}
    }
    public void exportResult(boolean success,String detail){status=t(success?"exported":"error.invalid",detail.equals("permission")||detail.equals("rate")?t("result."+detail):detail);}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(RecipePreview.hooks().mouseClicked(x,y,button))return true;
        x/=uiScale;y/=uiScale;
        if(button==0&&minecraft.player!=null){
            int slot=inventorySlot(x,y);
            if(slot>=0){draggedInventory=minecraft.player.getInventory().getItem(slot).copy();dragStartX=x;dragStartY=y;inventoryDragging=false;return true;}
        }
        if(layout!=null)for(var slot:drawSlots())if(screenRect(slot).contains((int)x,(int)y)){
            selectedSlot=slot;if(bindings.containsKey(slot)){selected=bindings.get(slot);updateField();if(!selected.raw())slot.getDisplayedIngredient().ifPresent(i->{Object v=i.getIngredient();if(v instanceof ItemStack s){valueBox.setValue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString());countBox.setValue(Integer.toString(s.getCount()));}else if(v instanceof FluidStack f){valueBox.setValue(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(f.getFluid()).toString());countBox.setValue(Integer.toString(f.getAmount()));}else if(ChemicalSupport.isStack(v)){valueBox.setValue(ChemicalSupport.id(v));countBox.setValue(Long.toString(document.chemicalAmount(selected)));}});if(button==1)remove(x,y);else status=t("selected",selected.label());}else {Component reason=t(slot.getRole()==Role.CATALYST?"error.catalyst":slot.getRole()==Role.RENDER_ONLY?"error.display_slot":"unbound");if(button==1)reject(reason,x,y);else status=reason;}return true;
        }return super.mouseClicked(x,y,button);
    }
    private Rect2i inventoryRect(int index){
        int col=index%9,row=index<9?3:index/9-1;
        return new Rect2i(panelX()+(panelWidth()-162)/2+col*18,controlsY()+128+row*18+(row==3?4:0),18,18);
    }
    private int inventorySlot(double x,double y){for(int i=0;i<36;i++)if(inventoryRect(i).contains((int)x,(int)y))return i;return -1;}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(RecipePreview.hooks().mouseDragged(x,y,button,dx,dy))return true;
        if(button==0&&!draggedInventory.isEmpty()){
            if(Math.hypot(x/uiScale-dragStartX,y/uiScale-dragStartY)>2)inventoryDragging=true;
            return true;
        }
        return super.mouseDragged(x/uiScale,y/uiScale,button,dx/uiScale,dy/uiScale);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(RecipePreview.hooks().mouseReleased(x,y,button))return true;
        if(button==0&&!draggedInventory.isEmpty()){
            var stack=draggedInventory;draggedInventory=ItemStack.EMPTY;
            if(inventoryDragging)for(var target:dropTargets(stack))if(target.area().contains((int)x,(int)y)){accept(target.field(),stack);break;}
            inventoryDragging=false;return true;
        }
        return super.mouseReleased(x/uiScale,y/uiScale,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){return RecipePreview.hooks().mouseScrolled(x,y,dx,dy)||super.mouseScrolled(x/uiScale,y/uiScale,dx,dy);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){return RecipePreview.hooks().keyPressed(key,scan,modifiers)||super.keyPressed(key,scan,modifiers);}
    @Override public boolean charTyped(char character,int modifiers){return RecipePreview.hooks().charTyped(character,modifiers)||super.charTyped(character,modifiers);}
    @Override public void tick(){if(!RecipePreview.available()){layout=null;bindings.clear();}else if(layout!=null){layout.tick();if(usingSourceLayout)overrideSourceSlots();}}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override protected float backgroundScale(){return uiScale;}
    @Override public void renderBackground(LegacyGraphics g,int mx,int my,float pt){g.fill(panelX(),panelY(),panelX()+panelWidth(),panelY()+panelHeight(),0xF01B2029);}
    @Override public void render(LegacyGraphics g,int mx,int my,float pt){
        int physicalX=mx,physicalY=my;RecipePreview.hooks().background(g,mx,my,pt);
        if(!RecipePreview.available()){layout=null;bindings.clear();}
        mx=(int)(mx/uiScale);my=(int)(my/uiScale);
        g.pose().pushPose();g.pose().scale(uiScale,uiScale,1);
        // Screen.render draws the background. Call it once, before the JEI layout.
        super.render(g,mx,my,pt);g.drawString(font,title,panelX()+8,panelY()+7,0xFFFFFF);
        g.drawString(font,font.plainSubstrByWidth(sourceDescription().getString(),panelWidth()-16),panelX()+8,panelY()+45,proxyRecipe()?0xFFE3C483:0xFFCCCCCC);
        if(my>=panelY()+44&&my<panelY()+57&&mx>=panelX()&&mx<panelX()+panelWidth())g.renderTooltip(font,font.split(sourceDescription(),panelWidth()),mx,my);
        if(layout!=null){g.pose().pushPose();g.pose().translate(previewX,previewY,0);g.pose().scale(previewScale,previewScale,1);layout.drawRecipe(g,(int)((mx-previewX)/previewScale),(int)((my-previewY)/previewScale));g.pose().popPose();
            for(var s:drawSlots()){var r=screenRect(s);int color=s==selectedSlot?0xFFFFFF66:bindings.containsKey(s)?0xFF66AA88:0xFFCF9944;g.renderOutline(r.getX()-1,r.getY()-1,r.getWidth()+2,r.getHeight()+2,color);}
        }else g.drawWordWrap(font,t("error.layout"),panelX()+8,panelY()+48,panelWidth()-16,0xFFFFAA66);
        if(usingSourceLayout&&layout!=null)g.drawString(font,t("wrapper_preview"),panelX()+8,controlsY()+2,0xFFE3C483);
        g.drawString(font,t("inventory"),panelX()+8,controlsY()+114,0xFFFFFF);
        if(minecraft.player!=null)for(int i=0;i<36;i++){
            var r=inventoryRect(i);g.fill(r.getX(),r.getY(),r.getX()+18,r.getY()+18,0xFF0E1117);g.renderOutline(r.getX(),r.getY(),18,18,0xFF747C87);
            ItemStack stack=minecraft.player.getInventory().getItem(i);g.renderItem(stack,r.getX()+1,r.getY()+1);g.renderItemDecorations(font,stack,r.getX()+1,r.getY()+1);
            if(r.contains(mx,my))g.fill(r.getX()+1,r.getY()+1,r.getX()+17,r.getY()+17,0x50FFFFFF);
        }
        g.drawString(font,font.plainSubstrByWidth(status.getString(),panelWidth()-16),panelX()+8,panelY()+panelHeight()-13,0xFFFFFF);
        if(mx>=panelX() && mx<panelX()+panelWidth() && my>=panelY()+panelHeight()-17 && my<panelY()+panelHeight())g.renderTooltip(font,font.split(status,panelWidth()),mx,my);

        int hovered=inventorySlot(mx,my);
        if(hovered>=0&&minecraft.player!=null&&!inventoryDragging){var stack=minecraft.player.getInventory().getItem(hovered);if(!stack.isEmpty())g.renderTooltip(font,stack,mx,my);}
        if(inventoryDragging&&!draggedInventory.isEmpty()){
            g.pose().pushPose();g.pose().translate(0,0,300);g.renderItem(draggedInventory,mx-8,my-8);g.renderItemDecorations(font,draggedInventory,mx-8,my-8);g.pose().popPose();
        }
        if(clickReason!=null&&net.minecraft.util.Util.getMillis()<reasonUntil)g.renderTooltip(font,font.split(clickReason,Math.max(80,Math.min(280,(int)(width/uiScale)-24))),reasonX,reasonY);
        g.pose().popPose();
        RecipePreview.hooks().overlay(g,physicalX,physicalY,pt);
        if(!inventoryDragging&&layout!=null&&(clickReason==null||net.minecraft.util.Util.getMillis()>=reasonUntil)){
            for(var slot:drawSlots())if(screenRect(slot).contains(mx,my)){
                var lines=new ArrayList<Component>(slot.tooltip());
                var binding=bindings.get(slot);
                if(binding!=null)lines.add(t("bound",binding.label()).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
                if(!lines.isEmpty())g.renderComponentTooltip(font,lines,physicalX,physicalY);
                break;
            }
        }
    }
}
