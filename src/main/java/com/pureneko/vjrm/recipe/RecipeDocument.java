package com.pureneko.vjrm.recipe;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** A lossless JSON draft. Only explicitly selected fields are replaced. */
public final class RecipeDocument {
    public record Field(List<String> path, boolean fluid, boolean output, String chemical, boolean raw) {
        public Field{path=List.copyOf(path);}
        public Field(List<String> path,boolean fluid,boolean output,String chemical){this(path,fluid,output,chemical,false);}
        public Field(List<String> path,boolean fluid,boolean output){this(path,fluid,output,"");}
        public String label() { return JsonFieldPaths.label(path); }
    }
    public static final List<String> TYPES = List.of("crafting_shaped", "crafting_shapeless", "smelting", "blasting", "smoking", "campfire_cooking", "stonecutting", "smithing_transform");
    private JsonObject json;
    private final Map<List<String>,Field> manualFields=new LinkedHashMap<>();
    public RecipeDocument(JsonObject json) { this.json = json.deepCopy(); }
    public JsonObject json() { return json.deepCopy(); }
    public void replace(JsonObject json) {
        if(!Objects.equals(this.json.get("type"),json.get("type")))manualFields.clear();
        this.json=json.deepCopy();manualFields.keySet().removeIf(path->!JsonFieldPaths.exists(this.json,path));
    }
    public List<Field> manualFields(){return List.copyOf(manualFields.values());}
    public void addManualField(Field field){
        if(field.path().isEmpty()||field.path().equals(List.of("type"))||!Set.of("","gas","infuse_type","pigment","slurry","chemical").contains(field.chemical()))throw new IllegalArgumentException("vjrm.error.field_path");
        JsonFieldPaths.get(json,field.path());manualFields.put(field.path(),field);
    }
    public void removeManualField(List<String> path){manualFields.remove(path);}
    public record NumericField(List<String> path, String value) {
        public String label() { return String.join("/", path); }
    }
    /** Includes nested machine parameters and array entries without guessing a mod's schema. */
    public List<NumericField> numericFields() {
        var out=new ArrayList<NumericField>();scanNumbers(json,List.of(),out);return List.copyOf(out);
    }
    private static void scanNumbers(JsonElement node,List<String> path,List<NumericField> out) {
        if(node.isJsonPrimitive()&&(node.getAsJsonPrimitive().isNumber()||node.getAsJsonPrimitive().isString()&&!path.isEmpty()&&path.get(path.size()-1).toLowerCase(Locale.ROOT).matches(".*(?:time|duration|mana|energy|power|cost|tick|chance|probability|heat|temperature|pressure|experience|amount|count|capacity|rate|speed|usage).*")&&node.getAsString().matches("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")))out.add(new NumericField(List.copyOf(path),node.getAsString()));
        else if(node.isJsonObject())for(var entry:node.getAsJsonObject().entrySet()){
            var next=new ArrayList<>(path);next.add(entry.getKey());scanNumbers(entry.getValue(),next,out);
        }else if(node.isJsonArray())for(int i=0;i<node.getAsJsonArray().size();i++){
            var next=new ArrayList<>(path);next.add(Integer.toString(i));scanNumbers(node.getAsJsonArray().get(i),next,out);
        }
    }
    public void setNumber(NumericField field,String value,net.minecraft.core.RegistryAccess registries) {
        setNumbers(Map.of(field.path(),value),registries);
    }
    public void setNumbers(Map<List<String>,String> changes,net.minecraft.core.RegistryAccess registries) {
        var before=json.deepCopy();
        try{
            var known=new HashSet<List<String>>();numericFields().forEach(f->known.add(f.path()));
            for(var change:changes.entrySet()){
                String value=change.getValue();
                if(!known.contains(change.getKey())||value==null||!value.matches("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?"))throw new IllegalArgumentException("vjrm.error.number");
                var field=new Field(change.getKey(),false,false);boolean string=get(field).getAsJsonPrimitive().isString();
                set(field,string?new JsonPrimitive(value):new JsonPrimitive(new java.math.BigDecimal(value)));
            }
            var decoded=decode(registries);
            // Forge 1.20.1 has fromJson/toNetwork serializers, not a universal JSON encoder.
            // Validate the known vanilla integer field against the value the serializer decoded.
            if(decoded instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe cooking&&changes.containsKey(List.of("cookingtime"))){
                if(new java.math.BigDecimal(changes.get(List.of("cookingtime"))).compareTo(java.math.BigDecimal.valueOf(cooking.getCookingTime()))!=0)throw new IllegalArgumentException("vjrm.error.number_range");
            }
        }catch(RuntimeException ex){json=before;throw ex;}
    }
    public String type() { return json.get("type").getAsString(); }
    public Recipe<?> decode(net.minecraft.core.RegistryAccess registries) {
        return decode(registries,new ResourceLocation("vjrm","preview"));
    }
    public Recipe<?> decode(net.minecraft.core.RegistryAccess registries,ResourceLocation id) {
        var recipe=net.minecraft.world.item.crafting.RecipeManager.fromJson(id,json);
        if(recipe==null)throw new IllegalArgumentException("vjrm.error.no_source");return recipe;
    }
    public static RecipeDocument template(String type) {
        JsonObject j = new JsonObject(); j.addProperty("type", "minecraft:" + type);
        JsonObject stone = JsonParser.parseString("{\"item\":\"minecraft:stone\"}").getAsJsonObject();
        j.add("result", JsonParser.parseString("{\"item\":\"minecraft:stone\",\"count\":1}"));
        switch (type) {
            case "crafting_shaped" -> {
                JsonArray pattern = new JsonArray(); pattern.add("ABC"); pattern.add("DEF"); pattern.add("GHI"); j.add("pattern", pattern);
                JsonObject key = new JsonObject(); for (char c = 'A'; c <= 'I'; c++) key.add(String.valueOf(c), stone.deepCopy()); j.add("key", key);
            }
            case "crafting_shapeless" -> { JsonArray a = new JsonArray(); a.add(stone); j.add("ingredients", a); }
            case "smithing_transform" -> {
                j.add("template", JsonParser.parseString("{\"item\":\"minecraft:netherite_upgrade_smithing_template\"}"));
                j.add("base", JsonParser.parseString("{\"item\":\"minecraft:diamond_sword\"}"));
                j.add("addition", JsonParser.parseString("{\"item\":\"minecraft:netherite_ingot\"}"));
                j.add("result", JsonParser.parseString("{\"item\":\"minecraft:netherite_sword\",\"count\":1}"));
            }
            default -> {
                j.add("ingredient", stone);j.addProperty("result","minecraft:stone");
                if(type.equals("stonecutting"))j.addProperty("count",1);
                if (!type.equals("stonecutting")) { j.addProperty("experience", 0.0); j.addProperty("cookingtime", type.equals("campfire_cooking") ? 600 : 200); }
            }
        }
        return new RecipeDocument(j);
    }
    public List<Field> fields() {
        List<Field> out=new ArrayList<>();scan(json,new ArrayList<>(),out);
        manualFields.keySet().removeIf(path->!JsonFieldPaths.exists(json,path));
        out.removeIf(f->manualFields.containsKey(f.path()));out.addAll(manualFields.values());
        return List.copyOf(out);
    }
    private void scan(JsonElement node, List<String> path, List<Field> out) {
        String label = String.join("/", path).toLowerCase(Locale.ROOT);
        String chemical=ChemicalSupport.kind(node,path,json);
        if(!chemical.isEmpty()){out.add(new Field(List.copyOf(path),false,label.contains("result")||label.contains("output"),chemical));return;}
        if (node.isJsonObject()) {
            JsonObject o = node.getAsJsonObject();
            if(GtRecipeSupport.isDocument(json)&&label.contains("fluid")&&path.size()==4&&path.get(3).equals("content")){out.add(new Field(List.copyOf(path),true,label.contains("output")));return;}
            if(o.has("ingredient")&&(o.has("count")||o.has("amount"))){
                var inner=new ArrayList<Field>();scan(o.get("ingredient"),path,inner);
                if(inner.size()==1&&inner.get(0).path().equals(path)){out.add(inner.get(0));return;}
            }
            if(o.has("type")&&o.get("type").isJsonPrimitive()&&label.contains("item")&&!label.contains("condition")&&!o.has("fluid")){
                String ingredientType=o.get("type").getAsString();
                if(ingredientType.startsWith("neoforge:")||ingredientType.startsWith("forge:")||ingredientType.startsWith("gtceu:")){out.add(new Field(List.copyOf(path),false,label.contains("output")));return;}
            }
            ResourceLocation stackId=o.has("id") && o.get("id").isJsonPrimitive()?ResourceLocation.tryParse(o.get("id").getAsString()):null;
            boolean fluid = o.has("amount") && o.has("id") || o.has("fluid") && o.get("fluid").isJsonPrimitive() || o.has("fluidTag") || label.contains("fluid") && o.has("tag") || (stackId!=null && (label.contains("fluid") || BuiltInRegistries.FLUID.containsKey(stackId) && !BuiltInRegistries.ITEM.containsKey(stackId)));
            if(o.has("type")&&o.get("type").isJsonPrimitive()&&o.get("type").getAsString().contains("fluid"))fluid=true;
            boolean item = o.has("item") && o.get("item").isJsonPrimitive() || o.has("tag") || (stackId!=null&&BuiltInRegistries.ITEM.containsKey(stackId));
            if (!path.isEmpty() && (fluid || item) && !label.contains("condition") && (o.has("fluid")||o.has("fluidTag")||item||stackId!=null)) {
                out.add(new Field(List.copyOf(path), fluid, label.contains("result") || label.contains("output"))); return;
            }
            for (var e : o.entrySet()) { var p = new ArrayList<>(path); p.add(e.getKey()); scan(e.getValue(), p, out); }
        } else if (node.isJsonArray()) {
            // Alternative ingredients form one field, not independent slots.
            JsonArray a = node.getAsJsonArray();
            if (!a.isEmpty() && a.get(0).isJsonObject() && (a.get(0).getAsJsonObject().has("item") || a.get(0).getAsJsonObject().has("tag")) && !path.get(path.size()-1).equals("ingredients") && !path.get(path.size()-1).equals("results") && !path.get(path.size()-1).equals("outputs")) {
                out.add(new Field(List.copyOf(path), false, label.contains("result")||label.contains("output"))); return;
            }
            for (int i=0; i<a.size(); i++) { var p = new ArrayList<>(path); p.add(Integer.toString(i)); scan(a.get(i), p, out); }
        } else if(node.isJsonPrimitive()&&node.getAsJsonPrimitive().isString()&&!path.isEmpty()){
            String last=path.get(path.size()-1);ResourceLocation id=ResourceLocation.tryParse(node.getAsString());
            if(id!=null&&!Set.of("type","tag","group","category","id","item","fluid").contains(last)&&
                (label.contains("result")||label.contains("output")||label.contains("ingredient")||label.contains("input"))&&
                (BuiltInRegistries.ITEM.containsKey(id)||BuiltInRegistries.FLUID.containsKey(id))){
                boolean fluid=label.contains("fluid")||!BuiltInRegistries.ITEM.containsKey(id);
                out.add(new Field(List.copyOf(path),fluid,label.contains("result")||label.contains("output")));
            }
        }
    }
    public JsonElement get(Field f) {
        return JsonFieldPaths.get(json,f.path());
    }
    public void set(Field f, JsonElement value) {
        JsonElement e = json; var p = f.path();
        for (int i=0; i<p.size()-1; i++) e = e.isJsonArray() ? e.getAsJsonArray().get(Integer.parseInt(p.get(i))) : e.getAsJsonObject().get(p.get(i));
        String last = p.get(p.size()-1);
        if (e.isJsonArray()) e.getAsJsonArray().set(Integer.parseInt(last), value.deepCopy()); else e.getAsJsonObject().add(last, value.deepCopy());
    }
    public int quantity(Field field){
        if(field.raw())return 1;
        if(field.fluid()&&GtRecipeSupport.isDocument(json))return GtFluidSupport.amount(get(field));
        var value=get(field);
        if(value.isJsonObject()){
            var object=value.getAsJsonObject();String key=field.fluid()?"amount":"count";
            if(object.has(key))return object.get(key).getAsInt();
            if(object.has("ingredient")&&object.has("amount"))return object.get("amount").getAsInt();
        }
        if(field.output()&&field.label().equals("result")&&json.has("count"))return json.get("count").getAsInt();
        return field.fluid()&&!field.output()?1000:1;
    }
    public boolean matches(Field f, Object value, net.minecraft.core.RegistryAccess registries) {
        if(f.raw())return false;
        if(!f.chemical().isEmpty())return ChemicalSupport.isStack(value)&&values(f,registries).stream().anyMatch(v->ChemicalSupport.stackKind(v).equals(ChemicalSupport.stackKind(value))&&ChemicalSupport.id(v).equals(ChemicalSupport.id(value)));
        try {

            JsonElement atom=atom(get(f));
            if (value instanceof ItemStack item && !f.fluid()) {
                if(f.output()||atom.isJsonPrimitive())return values(f,registries).stream().filter(v->v instanceof ItemStack).map(v->(ItemStack)v).anyMatch(s->ItemStack.isSameItemSameTags(s,item));
                return Ingredient.fromJson(atom).test(item);
            }
            if (value instanceof FluidStack fluid && f.fluid()) {
                return values(f,registries).stream().filter(v->v instanceof FluidStack).map(v->(FluidStack)v).anyMatch(s->s.getFluid()==fluid.getFluid());
            }
        } catch (RuntimeException ignored) { }
        return false;
    }
    public void accept(Field f, Object value, int count, net.minecraft.core.RegistryAccess registries) {
        if(f.raw())throw new IllegalArgumentException("vjrm.error.ingredient");
        if(GtRecipeSupport.isDocument(json)&&!f.fluid()&&value instanceof ItemStack stack){set(f,GtIngredientSupport.item(stack,count,com.mojang.serialization.JsonOps.INSTANCE));return;}
        if(f.fluid()&&GtRecipeSupport.isDocument(json)){if(!(value instanceof FluidStack stack))throw new IllegalArgumentException("vjrm.error.ingredient");set(f,GtFluidSupport.replace(get(f),BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(),false,count));return;}
        if(!f.chemical().isEmpty()){accept(f,value,(long)count,registries);return;}
        JsonElement root=get(f).deepCopy(),existing=atom(root);
        boolean nested=root!=existing;
        if(existing.isJsonPrimitive()){
            String id=value instanceof ItemStack s?BuiltInRegistries.ITEM.getKey(s.getItem()).toString():value instanceof FluidStack s?BuiltInRegistries.FLUID.getKey(s.getFluid()).toString():null;
            if(id==null)throw new IllegalArgumentException("vjrm.error.ingredient");
            if(f.output()&&f.label().equals("result")){
                if(json.has("count"))json.addProperty("count",count);
                else if(type().startsWith("minecraft:")&&count!=1)throw new IllegalArgumentException("vjrm.error.fixed_count");
            }
            if(nested)root.getAsJsonObject().addProperty("ingredient",id);else root=new JsonPrimitive(id);
        }else{
            JsonObject changed=existing.isJsonObject()&&!(value instanceof ItemStack&&existing.getAsJsonObject().has("type"))?existing.getAsJsonObject():new JsonObject();
            if(value instanceof ItemStack stack&&!f.fluid()){
                boolean itemKey=changed.has("item")||!changed.has("id");
                changed.remove("tag");changed.addProperty(itemKey?"item":"id",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                if(f.output()&&!GtRecipeSupport.isDocument(json)){
                    changed.remove("nbt");if(stack.hasTag())changed.addProperty("nbt",stack.getTag().toString());
                    changed.addProperty("count",count);
                }else if(changed.has("count"))changed.addProperty("count",count);
            }else if(value instanceof FluidStack fluid&&f.fluid()){
                changed.remove("tag");changed.remove("fluidTag");
                changed.addProperty(changed.has("id")?"id":"fluid",BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
                changed.remove("nbt");if(fluid.hasTag())changed.addProperty("nbt",fluid.getTag().toString());
                if(!nested||changed.has("amount"))changed.addProperty("amount",count);
            }else throw new IllegalArgumentException("vjrm.error.ingredient");
            if(nested)root.getAsJsonObject().add("ingredient",changed);else root=changed;
        }
        if(nested){var object=root.getAsJsonObject();if(object.has("amount"))object.addProperty("amount",count);else if(object.has("count"))object.addProperty("count",count);}
        set(f,root);
    }
    public void accept(Field f,Object value,long count,net.minecraft.core.RegistryAccess registries) {
        if(f.chemical().isEmpty()){accept(f,value,Math.toIntExact(count),registries);return;}
        if(count<1||!ChemicalSupport.isStack(value)||!f.chemical().equals(ChemicalSupport.stackKind(value)))throw new IllegalArgumentException("vjrm.error.chemical");
        set(f,ChemicalSupport.replace(get(f),f.chemical(),f.output(),ChemicalSupport.id(value),false,count));
    }
    public long chemicalAmount(Field field){return ChemicalSupport.amount(get(field));}
    public void chemicalTag(Field field,String tag,long amount){
        if(field.output()||field.chemical().isEmpty()||ResourceLocation.tryParse(tag)==null||amount<1)throw new IllegalArgumentException("vjrm.error.tag");
        set(field,ChemicalSupport.replace(get(field),field.chemical(),false,tag,true,amount));
    }
    public void acceptRepeated(List<Field> group,Object value,int repetitions,net.minecraft.core.RegistryAccess registries) {
        // Update members individually so extension fields survive a replacement.
        for(var f:group)accept(f,value,1,registries);
        repeat(group,repetitions);
    }
    public void tagRepeated(List<Field> group,String tag,int repetitions){
        for(var f:group)tag(f,tag);
        repeat(group,repetitions);
    }
    private void repeat(List<Field> group,int repetitions){
        if(group.isEmpty() || repetitions<1 || repetitions>64)throw new IllegalArgumentException("vjrm.error.amount");
        for(var f:group)if(f.path().size()!=2 || !f.path().get(0).equals("ingredients") || f.fluid() || f.output())throw new IllegalArgumentException("vjrm.error.ingredient");
        var indices=new TreeSet<Integer>();group.forEach(f->indices.add(Integer.parseInt(f.path().get(f.path().size()-1))));
        var inputs=json.getAsJsonArray("ingredients");var replacement=inputs.get(indices.first()).deepCopy();var changed=new JsonArray();
        int retained=0;
        for(int i=0;i<inputs.size();i++){
            if(!indices.contains(i))changed.add(inputs.get(i));
            else if(retained++<repetitions)changed.add(inputs.get(i));
            if(i==indices.last())for(int extra=group.size();extra<repetitions;extra++)changed.add(replacement.deepCopy());
        }
        json.add("ingredients",changed);
    }
    private static JsonElement atom(JsonElement value){return value.isJsonObject()&&value.getAsJsonObject().has("ingredient")&&(value.getAsJsonObject().has("count")||value.getAsJsonObject().has("amount"))?value.getAsJsonObject().get("ingredient"):value;}
    public List<?> values(Field f,net.minecraft.core.RegistryAccess registries){
        if(f.raw())return List.of();
        if(f.fluid()&&GtRecipeSupport.isDocument(json))return GtFluidSupport.values(get(f));
        if(!f.chemical().isEmpty())return ChemicalSupport.values(get(f),f.chemical(),f.output(),com.mojang.serialization.JsonOps.INSTANCE);
        JsonElement raw=get(f),leaf=atom(raw);int quantity=quantity(f);
        if(f.fluid()){
            JsonObject object=leaf.isJsonObject()?leaf.getAsJsonObject():new JsonObject();
            String tag=object.has("fluidTag")?object.get("fluidTag").getAsString():object.has("tag")?object.get("tag").getAsString():null;
            if(tag!=null){
                var key=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID,new ResourceLocation(tag));
                return java.util.stream.StreamSupport.stream(BuiltInRegistries.FLUID.getTagOrEmpty(key).spliterator(),false).map(h->new FluidStack(h.value(),quantity)).toList();
            }
            String id=leaf.isJsonPrimitive()?leaf.getAsString():object.has("fluid")?object.get("fluid").getAsString():object.has("id")?object.get("id").getAsString():null;
            if(id!=null&&BuiltInRegistries.FLUID.containsKey(new ResourceLocation(id))){
                var stack=new FluidStack(BuiltInRegistries.FLUID.get(new ResourceLocation(id)),quantity);
                if(object.has("nbt"))try{stack.setTag(net.minecraft.nbt.TagParser.parseTag(object.get("nbt").getAsString()));}catch(Exception ignored){}
                return List.of(stack);
            }
        }else{
            if(!f.output()||GtRecipeSupport.isDocument(json))try{return Arrays.stream(Ingredient.fromJson(leaf).getItems()).map(item->item.copyWithCount(quantity)).toList();}catch(RuntimeException ignored){}
            JsonObject object=leaf.isJsonObject()?leaf.getAsJsonObject():new JsonObject();
            String id=leaf.isJsonPrimitive()?leaf.getAsString():object.has("item")?object.get("item").getAsString():object.has("id")?object.get("id").getAsString():null;
            if(id!=null&&BuiltInRegistries.ITEM.containsKey(new ResourceLocation(id))){
                var stack=new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id)),quantity);
                if(object.has("nbt"))try{stack.setTag(net.minecraft.nbt.TagParser.parseTag(object.get("nbt").getAsString()));}catch(Exception ignored){}
                return List.of(stack);
            }
        }
        return List.of();
    }
    public void tag(Field f, String tag) {
        if(f.raw())throw new IllegalArgumentException("vjrm.error.tag");
        if(f.fluid()&&GtRecipeSupport.isDocument(json)){if(f.output()||ResourceLocation.tryParse(tag)==null)throw new IllegalArgumentException("vjrm.error.tag");set(f,GtFluidSupport.replace(get(f),tag,true,quantity(f)));return;}
        if(!f.chemical().isEmpty()){chemicalTag(f,tag,chemicalAmount(f));return;}
        if (f.output() || ResourceLocation.tryParse(tag) == null) throw new IllegalArgumentException("vjrm.error.tag");
        JsonElement root=get(f).deepCopy(),leaf=atom(root);
        JsonObject o=leaf.isJsonObject()&&!(leaf.getAsJsonObject().has("type")&&!f.fluid())?leaf.getAsJsonObject():new JsonObject();o.remove("item");o.remove("id");o.remove("fluid");o.remove("fluidTag");o.addProperty(f.fluid()&&type().startsWith("create:")?"fluidTag":"tag",tag);
        if(root!=leaf)root.getAsJsonObject().add("ingredient",o);else root=o;
        set(f,root);
    }
    public void tag(Field field,String tag,int count){
        if(count<1)throw new IllegalArgumentException("vjrm.error.amount");
        tag(field,tag);
        var value=get(field).getAsJsonObject();
        if(GtRecipeSupport.isDocument(json)&&!field.fluid()&&!value.has("ingredient")){
            var wrapper=new JsonObject();wrapper.addProperty("type","gtceu:sized");
            wrapper.add("ingredient",value.deepCopy());wrapper.addProperty("count",count);set(field,wrapper);return;
        }
        String key=field.fluid()?"amount":"count";
        if(field.fluid()||value.has(key))value.addProperty(key,count);
    }
    /** Adds a capability entry without flattening chance/condition data. */
    public Field addGtContent(boolean output,boolean fluid,boolean perTick,String value,String amount,net.minecraft.core.RegistryAccess registries){
        if(!GtRecipeSupport.isDocument(json))throw new IllegalArgumentException("vjrm.js.type");
        int count=Integer.parseInt(amount);if(count<1)throw new IllegalArgumentException("vjrm.error.amount");
        String id=value.isBlank()?(fluid?"minecraft:water":"minecraft:stone"):value;
        boolean tag=id.startsWith("#");if(tag)id=id.substring(1);
        var key=ResourceLocation.tryParse(id);if(key==null||(!tag&&!(fluid?BuiltInRegistries.FLUID.containsKey(key):BuiltInRegistries.ITEM.containsKey(key))))throw new IllegalArgumentException("vjrm.error.ingredient");
        if(output&&tag)throw new IllegalArgumentException("vjrm.error.tag");
        String io=perTick?(output?"tickOutputs":"tickInputs"):(output?"outputs":"inputs");if(!json.has(io))json.add(io,new JsonObject());var map=json.getAsJsonObject(io);
        String capability=fluid?"fluid":"item";if(!map.has(capability))map.add(capability,new JsonArray());var entries=map.getAsJsonArray(capability);
        GtRecipeSupport.checkCapacity(json,output,fluid,entries.size());
        JsonObject ingredient=new JsonObject();ingredient.addProperty(tag?"tag":fluid?"fluid":"item",id);
        JsonObject content=new JsonObject();if(fluid){content.add("value",ingredient);content.addProperty("amount",count);}else{content.addProperty("type","gtceu:sized");content.add("ingredient",ingredient);content.addProperty("count",count);}
        JsonObject entry=new JsonObject();entry.add("content",content);entry.addProperty("chance",10000);entry.addProperty("maxChance",10000);entries.add(entry);
        return new Field(List.of(io,capability,Integer.toString(entries.size()-1),"content"),fluid,output);
    }
    public void remove(Field f) {
        if(GtRecipeSupport.isDocument(json)&&f.path().size()==4&&Set.of("inputs","outputs","tickInputs","tickOutputs").contains(f.path().get(0))){
            json.getAsJsonObject(f.path().get(0)).getAsJsonArray(f.path().get(1)).remove(Integer.parseInt(f.path().get(2)));return;
        }
        // A shaped key may appear more than once in the pattern: clearing it clears every occurrence.
        if (f.path().size() == 2 && f.path().get(0).equals("key")) {
            String symbol = f.path().get(f.path().size()-1); JsonArray a = json.getAsJsonArray("pattern");
            for (int i=0;i<a.size();i++) a.set(i, new JsonPrimitive(a.get(i).getAsString().replace(symbol, " ")));
            json.getAsJsonObject("key").remove(symbol);
        } else if (f.path().size()==2 && f.path().get(0).equals("ingredients")) {
            json.getAsJsonArray("ingredients").remove(Integer.parseInt(f.path().get(f.path().size()-1)));
        } else throw new IllegalArgumentException("vjrm.error.required");
    }
    public void addCraftingInput(int gridIndex,ItemStack stack) {
        addCraftingInput(gridIndex,stack,false);
    }
    public void addCraftingInput(int gridIndex,ItemStack stack,boolean emiGrid) {
        JsonObject ingredient=new JsonObject();ingredient.addProperty("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if(type().equals("minecraft:crafting_shapeless")) {
            var inputs=json.getAsJsonArray("ingredients");if(inputs.size()>=9)throw new IllegalArgumentException("vjrm.error.required");inputs.add(ingredient);return;
        }
        JsonArray pattern=json.getAsJsonArray("pattern");JsonObject keys=json.getAsJsonObject("key");
        int minX=3,minY=3,maxX=-1,maxY=-1;
        for(int y=0;y<pattern.size();y++)for(int x=0;x<pattern.get(y).getAsString().length();x++)if(pattern.get(y).getAsString().charAt(x)!=' '){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
        int w=maxX-minX+1,h=maxY-minY+1;char[] cells="         ".toCharArray();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)cells[emiGrid?emiCraftingIndex(y*w+x,w,h):craftingIndex(y*w+x,w,h)]=pattern.get(y+minY).getAsString().charAt(x+minX);
        char symbol='A';while(keys.has(String.valueOf(symbol)))symbol++;
        cells[gridIndex]=symbol;keys.add(String.valueOf(symbol),ingredient);
        JsonArray rows=new JsonArray();for(int y=0;y<3;y++)rows.add(new String(cells,y*3,3));json.add("pattern",rows);
    }
    public static int craftingIndex(int i,int w,int h) {
        if(w==1)return h==1?4:i*3+1;if(h==1)return i+3;if(w==2)return i+(i>1?1:0)+(i>3?1:0);return h==2?i+3:i;
    }
    public static int emiCraftingIndex(int i,int w,int h) { return i/w*3+i%w+(w==1?1:0)+(h==1?3:0); }
}
