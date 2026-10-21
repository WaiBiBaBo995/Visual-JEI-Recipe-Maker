package com.pureneko.vjrm.recipe;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
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
        if(node.isJsonPrimitive()&&(node.getAsJsonPrimitive().isNumber()||node.getAsJsonPrimitive().isString()&&!path.isEmpty()&&path.getLast().toLowerCase(Locale.ROOT).matches(".*(?:time|duration|mana|energy|power|cost|tick|chance|probability|heat|temperature|pressure|experience|amount|count|capacity|rate|speed|usage).*")&&node.getAsString().matches("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")))out.add(new NumericField(List.copyOf(path),node.getAsString()));
        else if(node.isJsonObject())for(var entry:node.getAsJsonObject().entrySet()){
            var next=new ArrayList<>(path);next.add(entry.getKey());scanNumbers(entry.getValue(),next,out);
        }else if(node.isJsonArray())for(int i=0;i<node.getAsJsonArray().size();i++){
            var next=new ArrayList<>(path);next.add(Integer.toString(i));scanNumbers(node.getAsJsonArray().get(i),next,out);
        }
    }
    public void setNumber(NumericField field,String value,HolderLookup.Provider registries) {
        setNumbers(Map.of(field.path(),value),registries);
    }
    public void setNumbers(Map<List<String>,String> changes,HolderLookup.Provider registries) {
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
            // A permissive integer codec may silently truncate/overflow a number. Check fields the
            // serializer also encodes, while retaining unknown extension fields in the original JSON.
            Optional<JsonElement> encoded;
            try{encoded=Recipe.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE),decoded).result();}catch(RuntimeException ex){encoded=Optional.empty();}
            if(encoded.isPresent()&&encoded.get().isJsonObject()){
                var roundtrip=new RecipeDocument(encoded.get().getAsJsonObject());
                for(var change:changes.entrySet()){
                    JsonElement value;
                    try{value=roundtrip.get(new Field(change.getKey(),false,false));}catch(RuntimeException ex){continue;}
                    if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())continue;
                    var expected=new java.math.BigDecimal(change.getValue());var actual=value.getAsBigDecimal();
                    var difference=expected.subtract(actual).abs();
                    boolean integral=expected.stripTrailingZeros().scale()<=0;
                    var tolerance=integral?java.math.BigDecimal.ZERO:expected.abs().max(java.math.BigDecimal.ONE).multiply(new java.math.BigDecimal("0.000001"));
                    if(difference.compareTo(tolerance)>0)throw new IllegalArgumentException("vjrm.error.number_range");
                }
            }
        }catch(RuntimeException ex){json=before;throw ex;}
    }
    public String type() { return json.get("type").getAsString(); }
    public Recipe<?> decode(HolderLookup.Provider registries) {
        return Recipe.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
    }
    public static RecipeDocument copy(Recipe<?> recipe, HolderLookup.Provider registries) {
        return new RecipeDocument(Recipe.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), recipe).getOrThrow().getAsJsonObject());
    }
    public static RecipeDocument template(String type) {
        JsonObject j = new JsonObject(); j.addProperty("type", "minecraft:" + type);
        JsonObject stone = JsonParser.parseString("{\"item\":\"minecraft:stone\"}").getAsJsonObject();
        j.add("result", JsonParser.parseString("{\"id\":\"minecraft:stone\",\"count\":1}"));
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
                j.add("result", JsonParser.parseString("{\"id\":\"minecraft:netherite_sword\",\"count\":1}"));
            }
            default -> {
                j.add("ingredient", stone);
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
            if(o.has("ingredient")&&(o.has("count")||o.has("amount"))){
                var inner=new ArrayList<Field>();scan(o.get("ingredient"),path,inner);
                if(inner.size()==1&&inner.getFirst().path().equals(path)){out.add(inner.getFirst());return;}
            }
            ResourceLocation stackId=o.has("id") && o.get("id").isJsonPrimitive()?ResourceLocation.tryParse(o.get("id").getAsString()):null;
            boolean fluid = o.has("amount") && o.has("id") || o.has("fluid") || o.has("amount") && o.has("type") && o.get("type").isJsonPrimitive() && o.get("type").getAsString().startsWith("neoforge:") && !o.has("item") || label.contains("fluid") && o.has("tag") || (stackId!=null && (label.contains("fluid") || BuiltInRegistries.FLUID.containsKey(stackId) && !BuiltInRegistries.ITEM.containsKey(stackId)));
            if(o.has("type")&&o.get("type").isJsonPrimitive()&&o.get("type").getAsString().contains("fluid"))fluid=true;
            boolean item = o.has("item") && o.get("item").isJsonPrimitive() || o.has("tag") || (stackId!=null&&BuiltInRegistries.ITEM.containsKey(stackId));
            if (!path.isEmpty() && (fluid || item) && !label.contains("condition") && (o.has("fluid")||item||stackId!=null)) {
                out.add(new Field(List.copyOf(path), fluid, label.contains("result") || label.contains("output"))); return;
            }
            for (var e : o.entrySet()) { var p = new ArrayList<>(path); p.add(e.getKey()); scan(e.getValue(), p, out); }
        } else if (node.isJsonArray()) {
            // Alternative ingredients form one field, not independent slots.
            JsonArray a = node.getAsJsonArray();
            if (!a.isEmpty() && a.get(0).isJsonObject() && (a.get(0).getAsJsonObject().has("item") || a.get(0).getAsJsonObject().has("tag")) && !path.getLast().equals("ingredients") && !path.getLast().equals("results") && !path.getLast().equals("outputs")) {
                out.add(new Field(List.copyOf(path), false, false)); return;
            }
            for (int i=0; i<a.size(); i++) { var p = new ArrayList<>(path); p.add(Integer.toString(i)); scan(a.get(i), p, out); }
        } else if(node.isJsonPrimitive()&&node.getAsJsonPrimitive().isString()&&!path.isEmpty()){
            String last=path.getLast();ResourceLocation id=ResourceLocation.tryParse(node.getAsString());
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
        String last = p.getLast();
        if (e.isJsonArray()) e.getAsJsonArray().set(Integer.parseInt(last), value.deepCopy()); else e.getAsJsonObject().add(last, value.deepCopy());
    }
    public int quantity(Field field){
        if(field.raw())return 1;
        var value=get(field);
        if(value.isJsonObject()){
            var object=value.getAsJsonObject();String key=field.fluid()?"amount":"count";
            if(object.has(key))return object.get(key).getAsInt();
            if(object.has("ingredient")&&object.has("amount"))return object.get("amount").getAsInt();
        }
        return field.fluid()&&!field.output()?1000:1;
    }
    public boolean matches(Field f, Object value, HolderLookup.Provider registries) {
        if(f.raw())return false;
        if(!f.chemical().isEmpty())return ChemicalSupport.isStack(value)&&values(f,registries).stream().anyMatch(v->ChemicalSupport.stackKind(v).equals(ChemicalSupport.stackKind(value))&&ChemicalSupport.id(v).equals(ChemicalSupport.id(value)));
        try {
            var ops = registries.createSerializationContext(JsonOps.INSTANCE);
            JsonElement atom=atom(get(f));
            if (value instanceof ItemStack item && !f.fluid()) {
                if(f.output()||atom.isJsonPrimitive())return values(f,registries).stream().filter(v->v instanceof ItemStack).map(v->(ItemStack)v).anyMatch(s->ItemStack.isSameItemSameComponents(s,item));
                return Ingredient.CODEC.parse(ops, atom).result().map(i -> i.test(item)).orElseGet(()->values(f,registries).stream().filter(v->v instanceof ItemStack).map(v->(ItemStack)v).anyMatch(s->ItemStack.isSameItemSameComponents(s,item)));
            }
            if (value instanceof FluidStack fluid && f.fluid()) {
                return values(f,registries).stream().filter(v->v instanceof FluidStack).map(v->(FluidStack)v).anyMatch(s->s.getFluid()==fluid.getFluid());
            }
        } catch (RuntimeException ignored) { }
        return false;
    }
    public void accept(Field f, Object value, int count, HolderLookup.Provider registries) {
        if(f.raw())throw new IllegalArgumentException("vjrm.error.ingredient");
        if(!f.chemical().isEmpty()){accept(f,value,(long)count,registries);return;}
        JsonElement root=get(f).deepCopy(),existing=atom(root);
        boolean nested=root!=existing;
        if(existing.isJsonPrimitive()){
            String id=value instanceof ItemStack s?BuiltInRegistries.ITEM.getKey(s.getItem()).toString():value instanceof FluidStack s?BuiltInRegistries.FLUID.getKey(s.getFluid()).toString():null;
            if(id==null)throw new IllegalArgumentException("vjrm.error.ingredient");
            if(nested)root.getAsJsonObject().addProperty("ingredient",id);else root=new JsonPrimitive(id);
        }else{
            JsonObject changed=existing.isJsonObject()?existing.getAsJsonObject():new JsonObject();
            if(value instanceof ItemStack stack&&!f.fluid()){
                boolean itemKey=changed.has("item")||!f.output()&&!changed.has("id");
                changed.remove("tag");changed.addProperty(itemKey?"item":"id",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                if(f.output()){
                    changed.remove("components");
                    var encoded=ItemStack.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE),stack.copyWithCount(count)).getOrThrow().getAsJsonObject();
                    if(encoded.has("components"))changed.add("components",encoded.get("components"));
                    changed.addProperty("count",count);
                }else if(changed.has("count"))changed.addProperty("count",count);
            }else if(value instanceof FluidStack fluid&&f.fluid()){
                changed.remove("tag");
                if(changed.has("type") && Set.of("neoforge:tag","neoforge:single").contains(changed.get("type").getAsString()))changed.addProperty("type","neoforge:single");
                changed.addProperty(changed.has("id")?"id":"fluid",BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
                changed.remove("components");
                var encoded=FluidStack.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE),fluid.copyWithAmount(count)).getOrThrow().getAsJsonObject();
                if(encoded.has("components"))changed.add("components",encoded.get("components"));
                if(!nested||changed.has("amount"))changed.addProperty("amount",count);
            }else throw new IllegalArgumentException("vjrm.error.ingredient");
            if(nested)root.getAsJsonObject().add("ingredient",changed);else root=changed;
        }
        if(nested){var object=root.getAsJsonObject();if(object.has("amount"))object.addProperty("amount",count);else if(object.has("count"))object.addProperty("count",count);}
        set(f,root);
    }
    public void accept(Field f,Object value,long count,HolderLookup.Provider registries) {
        if(f.chemical().isEmpty()){accept(f,value,Math.toIntExact(count),registries);return;}
        if(count<1||!ChemicalSupport.isStack(value)||!f.chemical().equals(ChemicalSupport.stackKind(value)))throw new IllegalArgumentException("vjrm.error.chemical");
        set(f,ChemicalSupport.replace(get(f),f.chemical(),f.output(),ChemicalSupport.id(value),false,count));
    }
    public long chemicalAmount(Field field){return ChemicalSupport.amount(get(field));}
    public void chemicalTag(Field field,String tag,long amount){
        if(field.output()||field.chemical().isEmpty()||ResourceLocation.tryParse(tag)==null||amount<1)throw new IllegalArgumentException("vjrm.error.tag");
        set(field,ChemicalSupport.replace(get(field),field.chemical(),false,tag,true,amount));
    }
    public void acceptRepeated(List<Field> group,Object value,int repetitions,HolderLookup.Provider registries) {
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
        for(var f:group)if(f.path().size()!=2 || !f.path().getFirst().equals("ingredients") || f.fluid() || f.output())throw new IllegalArgumentException("vjrm.error.ingredient");
        var indices=new TreeSet<Integer>();group.forEach(f->indices.add(Integer.parseInt(f.path().getLast())));
        var inputs=json.getAsJsonArray("ingredients");var replacement=inputs.get(indices.first()).deepCopy();var changed=new JsonArray();
        int retained=0;
        for(int i=0;i<inputs.size();i++){
            if(!indices.contains(i))changed.add(inputs.get(i));
            else if(retained++<repetitions)changed.add(inputs.get(i));
            if(i==indices.last())for(int extra=group.size();extra<repetitions;extra++)changed.add(replacement.deepCopy());
        }
        json.add("ingredients",changed);
    }
    private static JsonElement atom(JsonElement value){return value.isJsonObject()&&value.getAsJsonObject().has("ingredient")?value.getAsJsonObject().get("ingredient"):value;}
    public List<?> values(Field f,HolderLookup.Provider registries){
        if(f.raw())return List.of();
        if(!f.chemical().isEmpty())return ChemicalSupport.values(get(f),f.chemical(),f.output(),registries.createSerializationContext(JsonOps.INSTANCE));
        var ops=registries.createSerializationContext(JsonOps.INSTANCE);JsonElement raw=get(f),leaf=atom(raw);
        int quantity=quantity(f);
        if(f.fluid()){
            var ingredient=net.neoforged.neoforge.fluids.crafting.FluidIngredient.CODEC.parse(ops,leaf).result();
            if(ingredient.isPresent()){int amount=quantity;return Arrays.stream(ingredient.get().getStacks()).map(s->s.copyWithAmount(amount)).toList();}
            String id=leaf.isJsonPrimitive()?leaf.getAsString():leaf.isJsonObject()&&leaf.getAsJsonObject().has("id")?leaf.getAsJsonObject().get("id").getAsString():null;
            if(id!=null&&BuiltInRegistries.FLUID.containsKey(ResourceLocation.parse(id)))return List.of(new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse(id)),quantity));
        }else{
            if(!f.output()){
                var ingredient=Ingredient.CODEC.parse(ops,leaf).result();
                if(ingredient.isPresent()){int count=quantity;return Arrays.stream(ingredient.get().getItems()).map(s->s.copyWithCount(count)).toList();}
            }
            var stack=ItemStack.CODEC.parse(ops,leaf).result();if(stack.isPresent())return List.of(stack.get());
            String id=leaf.isJsonPrimitive()?leaf.getAsString():leaf.isJsonObject()&&leaf.getAsJsonObject().has("item")?leaf.getAsJsonObject().get("item").getAsString():null;
            if(id!=null&&BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)))return List.of(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)),quantity));
        }
        return List.of();
    }
    public void tag(Field f, String tag) {
        if(f.raw())throw new IllegalArgumentException("vjrm.error.tag");
        if(!f.chemical().isEmpty()){chemicalTag(f,tag,chemicalAmount(f));return;}
        if (f.output() || ResourceLocation.tryParse(tag) == null) throw new IllegalArgumentException("vjrm.error.tag");
        JsonElement root=get(f).deepCopy(),leaf=atom(root);
        JsonObject o=leaf.isJsonObject()?leaf.getAsJsonObject():new JsonObject();o.remove("item");o.remove("id");o.remove("fluid");o.addProperty("tag",tag);
        if(f.fluid() && o.has("type") && Set.of("neoforge:single","neoforge:tag").contains(o.get("type").getAsString())){o.addProperty("type","neoforge:tag");o.remove("components");}
        if(root!=leaf)root.getAsJsonObject().add("ingredient",o);else root=o;
        set(f,root);
    }
    public void tag(Field field,String tag,int count){
        if(count<1)throw new IllegalArgumentException("vjrm.error.amount");
        tag(field,tag);
        var value=get(field).getAsJsonObject();
        String key=field.fluid()?"amount":"count";
        if(field.fluid()||value.has(key))value.addProperty(key,count);
    }
    public void remove(Field f) {
        // A shaped key may appear more than once in the pattern: clearing it clears every occurrence.
        if (f.path().size() == 2 && f.path().getFirst().equals("key")) {
            String symbol = f.path().getLast(); JsonArray a = json.getAsJsonArray("pattern");
            for (int i=0;i<a.size();i++) a.set(i, new JsonPrimitive(a.get(i).getAsString().replace(symbol, " ")));
            json.getAsJsonObject("key").remove(symbol);
        } else if (f.path().size()==2 && f.path().getFirst().equals("ingredients")) {
            json.getAsJsonArray("ingredients").remove(Integer.parseInt(f.path().getLast()));
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
