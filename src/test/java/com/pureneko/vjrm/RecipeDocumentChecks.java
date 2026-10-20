package com.pureneko.vjrm;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.pureneko.vjrm.recipe.RecipeDocument;
import com.pureneko.vjrm.export.DatapackExporter;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.*;

/** Serializer/IO integration checks; no client window or world writes needed. */
@net.neoforged.neoforge.gametest.GameTestHolder("vjrm")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class RecipeDocumentChecks {
    @net.minecraft.gametest.framework.GameTest(template="empty", timeoutTicks=200)
    public static void recipesAndExport(net.minecraft.gametest.framework.GameTestHelper helper) throws Exception {
        var registries=helper.getLevel().registryAccess();
        int checks=0;

        var chemicalFields=new RecipeDocument(com.google.gson.JsonParser.parseString("{\"type\":\"mekanism:compressing\",\"input_chemical\":{\"gas\":\"mekanism:oxygen\",\"amount\":2},\"output\":{\"slurry\":\"mekanism:clean_iron\",\"amount\":4},\"conditions\":[{\"gas\":\"mekanism:hydrogen\"}]}").getAsJsonObject()).fields();
        if(chemicalFields.size()!=2||!chemicalFields.stream().anyMatch(f->f.label().equals("input_chemical")&&f.chemical().equals("gas"))||!chemicalFields.stream().anyMatch(f->f.output()&&f.chemical().equals("slurry")))throw new AssertionError("Chemical fields or conditions misclassified");checks++;

        for(String type:RecipeDocument.TYPES){
            var doc=RecipeDocument.template(type);var recipe=doc.decode(registries);
            if(doc.fields().isEmpty())throw new AssertionError("No fields: "+type);
            var output=doc.fields().stream().filter(f->f.output()&&!f.fluid()).findFirst().orElseThrow();
            doc.accept(output,new ItemStack(Items.DIAMOND),3,registries);
            var edited=doc.decode(registries);
            if(!edited.getResultItem(registries).is(Items.DIAMOND)||edited.getResultItem(registries).getCount()!=3)throw new AssertionError("Wrong result: "+type);
            var roundtrip=RecipeDocument.copy(edited,registries);roundtrip.decode(registries);
            System.out.println("PASS serializer: "+type);checks++;
        }
        var shaped=RecipeDocument.template("crafting_shaped");shaped.remove(shaped.fields().stream().filter(f->f.label().equals("key/A")).findFirst().orElseThrow());shaped.decode(registries);checks++;
        shaped.addCraftingInput(0,new ItemStack(Items.DIAMOND));
        if(!shaped.decode(registries).getIngredients().getFirst().test(new ItemStack(Items.DIAMOND)))throw new AssertionError("Empty grid cell did not accept input");checks++;
        var shapeless=RecipeDocument.template("crafting_shapeless");shapeless.addCraftingInput(0,new ItemStack(Items.DIAMOND));
        if(shapeless.decode(registries).getIngredients().size()!=2)throw new AssertionError("Shapeless insert failed");checks++;
        var extra=RecipeDocument.template("smelting").json();extra.addProperty("custom_extension",12);extra.addProperty("cookingtime",79);
        var preserved=new RecipeDocument(extra);preserved.accept(preserved.fields().stream().filter(f->f.output()).findFirst().orElseThrow(),new ItemStack(Items.GOLD_INGOT),2,registries);
        if(preserved.json().get("custom_extension").getAsInt()!=12||preserved.json().get("cookingtime").getAsInt()!=79)throw new AssertionError("Lost extra fields");checks++;
        Path root=Path.of("build/verification/datapack").toAbsolutePath();Files.createDirectories(root);
        String name="check_"+System.nanoTime();DatapackExporter.writePackMeta(root);
        Path file=DatapackExporter.export(root,"test",name,preserved.json(),false);
        new RecipeDocument(JsonParser.parseString(Files.readString(file)).getAsJsonObject()).decode(registries);checks++;
        try{DatapackExporter.export(root,"test",name,preserved.json(),false);throw new AssertionError("Overwrite accepted");}catch(FileAlreadyExistsException expected){checks++;}
        try{DatapackExporter.export(root,"test","../../../../escape",preserved.json(),false);throw new AssertionError("Traversal accepted");}catch(java.io.IOException expected){checks++;}
        var fluid=new RecipeDocument(JsonParser.parseString("{\"type\":\"test:fluid\",\"input\":{\"id\":\"minecraft:water\",\"amount\":1000}}").getAsJsonObject());
        var fluidField=fluid.fields().getFirst();if(!fluidField.fluid())throw new AssertionError("Fluid classified as item");
        fluid.accept(fluidField,new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,100),500,registries);
        if(fluid.get(fluidField).getAsJsonObject().get("amount").getAsInt()!=500)throw new AssertionError("Wrong fluid amount");checks++;
        var numbers=RecipeDocument.template("smelting");
        numbers.setNumber(numbers.numericFields().stream().filter(f->f.label().equals("cookingtime")).findFirst().orElseThrow(),"321",registries);
        if(((net.minecraft.world.item.crafting.AbstractCookingRecipe)numbers.decode(registries)).getCookingTime()!=321)throw new AssertionError("Cooking time edit ignored");checks++;
        var numericJson=numbers.json();numericJson.add("machine",JsonParser.parseString("{\"energy\":9007199254740993,\"mana\":70,\"steps\":[{\"duration\":12.5}]}"));numbers.replace(numericJson);
        numbers.setNumber(numbers.numericFields().stream().filter(f->f.label().equals("machine/energy")).findFirst().orElseThrow(),"9223372036854775806",registries);
        numbers.setNumber(numbers.numericFields().stream().filter(f->f.label().equals("machine/steps/0/duration")).findFirst().orElseThrow(),"3.125e2",registries);
        if(!numbers.json().getAsJsonObject("machine").get("energy").getAsString().equals("9223372036854775806"))throw new AssertionError("Large energy precision lost");checks++;
        var stringJson=numbers.json();stringJson.getAsJsonObject("machine").addProperty("string_energy","1000");numbers.replace(stringJson);
        numbers.setNumber(numbers.numericFields().stream().filter(f->f.label().equals("machine/string_energy")).findFirst().orElseThrow(),"12345678901234567890.125",registries);
        if(!numbers.json().getAsJsonObject("machine").get("string_energy").getAsJsonPrimitive().isString())throw new AssertionError("Numeric-string energy lost its JSON type");checks++;
        for(String bad:java.util.List.of("0.5","2147483648")){
            var beforeBad=numbers.json();try{numbers.setNumber(numbers.numericFields().stream().filter(f->f.label().equals("cookingtime")).findFirst().orElseThrow(),bad,registries);throw new AssertionError("Truncated or overflowing integer accepted");}catch(IllegalArgumentException expected){if(!beforeBad.equals(numbers.json()))throw new AssertionError("Overflow edit did not roll back");checks++;}
        }
        var numericBefore=numbers.json();
        try{numbers.setNumber(numbers.numericFields().getFirst(),"NaN",registries);throw new AssertionError("Invalid number accepted");}catch(IllegalArgumentException expected){if(!numbers.json().equals(numericBefore))throw new AssertionError("Invalid edit changed document");checks++;}
        for(String invalid:java.util.List.of("{\"type\":NaN}","{type:1}","{\"x\":1,}","{\"x\":1} true","{\"x\":/*comment*/1}","[]")){
            try{com.pureneko.vjrm.client.JsonSyntax.parse(invalid);throw new AssertionError("Invalid JSON accepted: "+invalid);}catch(com.google.gson.JsonParseException expected){checks++;}
        }
        String highlighted="{\"name\":\"a\\\"b\",\"energy\":9e3,\"enabled\":true,\"nested\":[null]}";
        var syntax=com.pureneko.vjrm.client.JsonSyntax.tokens(highlighted);
        if(syntax.stream().anyMatch(t->t.kind()==com.pureneko.vjrm.client.JsonSyntax.Kind.ERROR)||syntax.stream().noneMatch(t->t.kind()==com.pureneko.vjrm.client.JsonSyntax.Kind.KEY)||syntax.stream().noneMatch(t->t.kind()==com.pureneko.vjrm.client.JsonSyntax.Kind.NUMBER))throw new AssertionError("JSON token colors misclassified");
        if(com.pureneko.vjrm.client.JsonSyntax.brackets(highlighted,syntax).get(0)!=highlighted.length()-1)throw new AssertionError("Bracket matching failed");checks++;
        System.out.println("PASS numeric fields, large integer precision, strict JSON syntax and token/bracket handling");
        var server=helper.getLevel().getServer();
        var sourceId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("vjrm","source_check");
        var original=com.pureneko.vjrm.recipe.RecipeSource.read(server,sourceId);
        if(original.json().get("source_marker").getAsInt()!=37)throw new AssertionError("Original resource fields were lost");checks++;
        var syncedBuffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),registries);
        try{
            net.minecraft.world.item.crafting.RecipeSerializer.SHAPED_RECIPE.streamCodec().encode(syncedBuffer,(net.minecraft.world.item.crafting.ShapedRecipe)original.decode(registries));
            var synced=net.minecraft.world.item.crafting.RecipeSerializer.SHAPED_RECIPE.streamCodec().decode(syncedBuffer);
            try{RecipeDocument.copy(synced,registries);throw new AssertionError("Expected the vanilla unpacked recipe failure");}
            catch(IllegalStateException expected){if(!expected.getMessage().contains("Cannot encode unpacked recipe"))throw expected;}
            var fromSource=com.pureneko.vjrm.recipe.RecipeSource.read(server,sourceId);
            fromSource.accept(fromSource.fields().stream().filter(f->f.output()).findFirst().orElseThrow(),new ItemStack(Items.EMERALD),4,registries);
            if(!fromSource.decode(registries).getResultItem(registries).is(Items.EMERALD)||fromSource.json().get("source_marker").getAsInt()!=37)throw new AssertionError("Source copy/edit failed");
            System.out.println("PASS client-synced unpacked recipe reproduction and source copy");checks++;
        }finally{syncedBuffer.release();}
        var machineId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("vjrm","test_machine");
        var machine=com.pureneko.vjrm.recipe.RecipeSource.read(server,machineId);
        machine.accept(machine.fields().stream().filter(f->f.label().equals("input")).findFirst().orElseThrow(),new ItemStack(Items.GOLD_INGOT),5,registries);
        machine.accept(machine.fields().stream().filter(f->f.fluid()).findFirst().orElseThrow(),new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1),750,registries);
        var modded=(TestMachineRecipe)machine.decode(registries);
        if(modded.input().count()!=5||!modded.input().ingredient().test(new ItemStack(Items.GOLD_INGOT))||modded.fluidInput().amount()!=750||modded.fluidInput().getFluids()[0].getFluid()!=net.minecraft.world.level.material.Fluids.LAVA||modded.duration()!=123||modded.chance()!=0.75f||machine.json().get("source_marker").getAsInt()!=37)throw new AssertionError("Modded nested ingredients or metadata lost");
        System.out.println("PASS non-vanilla serializer, nested sized items/fluids and preserved metadata");checks++;
        machine.tag(machine.fields().stream().filter(f->f.label().equals("input")).findFirst().orElseThrow(),"minecraft:logs");
        machine.tag(machine.fields().stream().filter(f->f.fluid()).findFirst().orElseThrow(),"minecraft:water");
        var tagged=(TestMachineRecipe)machine.decode(registries);
        if(tagged.input().count()!=5||tagged.fluidInput().amount()!=750)throw new AssertionError("Tag edit lost sized wrapper quantities");checks++;
        if(com.pureneko.vjrm.recipe.RecipeSource.catalog(server).stream().noneMatch(s->s.id().equals(machineId.toString())))throw new AssertionError("Modded serializer missing from catalog");checks++;
        try{com.pureneko.vjrm.recipe.RecipeSource.read(server,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("vjrm","missing"));throw new AssertionError("Missing resource accepted");}catch(java.io.IOException expected){checks++;}
        var manager=server.getRecipeManager();var existing=java.util.List.copyOf(manager.getRecipes());
        var programmatic=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("vjrm","programmatic_only");
        try{
            var withRuntimeRecipe=new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(existing);
            withRuntimeRecipe.add(new net.minecraft.world.item.crafting.RecipeHolder<>(programmatic,modded));manager.replaceRecipes(withRuntimeRecipe);
            try{com.pureneko.vjrm.recipe.RecipeSource.read(server,programmatic);throw new AssertionError("Programmatic recipe was accepted without a datapack resource");}catch(java.io.IOException expected){checks++;}
        }finally{manager.replaceRecipes(existing);}
        var denied=new net.minecraft.server.level.ServerPlayer(server,helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Denied"),net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override public boolean hasPermissions(int level){return false;}
        };
        var granted=new net.minecraft.server.level.ServerPlayer(server,helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Allowed"),net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override public boolean hasPermissions(int level){return true;}
        };
        String recipeId="test:network_"+System.nanoTime();
        var request=new com.pureneko.vjrm.network.RecipeNetwork.ExportRequest(recipeId,preserved.json().toString(),false);
        var deniedResult=send(request,denied);
        if(deniedResult.success()||!deniedResult.detail().equals("permission"))throw new AssertionError("Non-OP export accepted");checks++;
        var acceptedResult=send(request,granted);
        if(!acceptedResult.success())throw new AssertionError("OP export failed: "+acceptedResult.detail());checks++;
        var throttled=send(request,granted);if(throttled.success()||!throttled.detail().equals("rate"))throw new AssertionError("Export rate limit failed");checks++;
        var sourceResult=sendSource(new com.pureneko.vjrm.network.RecipeSourceNetwork.Request(42,sourceId.toString()),denied);
        if(sourceResult.token()!=42||!sourceResult.error().isEmpty()||!sourceResult.json().contains("source_marker"))throw new AssertionError("Source response failed");checks++;
        var missingResult=sendSource(new com.pureneko.vjrm.network.RecipeSourceNetwork.Request(43,"vjrm:missing"),denied);
        if(missingResult.error().isEmpty()||!missingResult.json().isEmpty())throw new AssertionError("Missing source response accepted");checks++;
        var responseBuffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),registries);
        try{
            com.pureneko.vjrm.network.RecipeSourceNetwork.Response.CODEC.encode(responseBuffer,sourceResult);
            if(!com.pureneko.vjrm.network.RecipeSourceNetwork.Response.CODEC.decode(responseBuffer).equals(sourceResult))throw new AssertionError("Source payload roundtrip failed");checks++;
        }finally{responseBuffer.release();}
        if(net.neoforged.fml.ModList.get().isLoaded("create"))checks+=CreateChecks.run(server);
        System.out.println("PASS "+checks+" recipe/IO integration checks");
        helper.succeed();
    }
    private static com.pureneko.vjrm.network.RecipeSourceNetwork.Response sendSource(com.pureneko.vjrm.network.RecipeSourceNetwork.Request request,net.minecraft.server.level.ServerPlayer player){
        var result=new java.util.concurrent.atomic.AtomicReference<com.pureneko.vjrm.network.RecipeSourceNetwork.Response>();
        var context=(net.neoforged.neoforge.network.handling.IPayloadContext)java.lang.reflect.Proxy.newProxyInstance(RecipeDocumentChecks.class.getClassLoader(),new Class<?>[]{net.neoforged.neoforge.network.handling.IPayloadContext.class},(proxy,method,args)->{
            if(method.getName().equals("player"))return player;
            if(method.getName().equals("reply")){result.set((com.pureneko.vjrm.network.RecipeSourceNetwork.Response)args[0]);return null;}
            throw new UnsupportedOperationException(method.getName());
        });
        com.pureneko.vjrm.network.RecipeSourceNetwork.handle(request,context);return java.util.Objects.requireNonNull(result.get());
    }
    static com.pureneko.vjrm.network.RecipeNetwork.ExportResult send(com.pureneko.vjrm.network.RecipeNetwork.ExportRequest request,net.minecraft.server.level.ServerPlayer player) {
        var result=new java.util.concurrent.atomic.AtomicReference<com.pureneko.vjrm.network.RecipeNetwork.ExportResult>();
        var context=(net.neoforged.neoforge.network.handling.IPayloadContext)java.lang.reflect.Proxy.newProxyInstance(RecipeDocumentChecks.class.getClassLoader(),new Class<?>[]{net.neoforged.neoforge.network.handling.IPayloadContext.class},(proxy,method,args)->{
            if(method.getName().equals("player"))return player;
            if(method.getName().equals("reply")){result.set((com.pureneko.vjrm.network.RecipeNetwork.ExportResult)args[0]);return null;}
            throw new UnsupportedOperationException(method.getName());
        });
        com.pureneko.vjrm.network.RecipeNetwork.handleExport(request,context);return java.util.Objects.requireNonNull(result.get(),"Missing export response");
    }
}
