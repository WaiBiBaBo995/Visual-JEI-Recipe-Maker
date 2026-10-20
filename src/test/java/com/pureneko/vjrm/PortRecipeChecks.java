package com.pureneko.vjrm;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.*;
import com.pureneko.vjrm.export.DatapackExporter;
import com.pureneko.vjrm.network.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraft.gametest.framework.*;
import java.nio.file.*;

@GameTestHolder("vjrm")
@PrefixGameTestTemplate(false)
public final class PortRecipeChecks {
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    @GameTest(template="empty",timeoutTicks=200)
    public static void recipes(GameTestHelper helper)throws Exception{
        var registries=helper.getLevel().registryAccess();int checks=0;

        var chemicalFields=new RecipeDocument(com.google.gson.JsonParser.parseString("{\"type\":\"mekanism:compressing\",\"input_chemical\":{\"gas\":\"mekanism:oxygen\",\"amount\":2},\"output\":{\"slurry\":\"mekanism:clean_iron\",\"amount\":4},\"conditions\":[{\"gas\":\"mekanism:hydrogen\"}]}").getAsJsonObject()).fields();
        if(chemicalFields.size()!=2||!chemicalFields.stream().anyMatch(f->f.label().equals("input_chemical")&&f.chemical().equals("gas"))||!chemicalFields.stream().anyMatch(f->f.output()&&f.chemical().equals("slurry")))throw new AssertionError("Chemical fields or conditions misclassified");checks++;

        for(String type:RecipeDocument.TYPES){
            var document=RecipeDocument.template(type);document.decode(registries);
            var result=document.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();
            document.accept(result,new ItemStack(Items.EMERALD),1,registries);
            check(document.decode(registries).getResultItem(registries).is(Items.EMERALD),"Wrong result "+type);checks++;
        }
        var stone=RecipeDocument.template("stonecutting");var out=stone.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();stone.accept(out,new ItemStack(Items.DIAMOND),3,registries);check(stone.decode(registries).getResultItem(registries).getCount()==3,"1.20.1 stonecutting root count lost");checks++;
        var cooking=RecipeDocument.template("smelting");var before=cooking.json();
        cooking.setNumber(cooking.numericFields().stream().filter(f->f.label().equals("cookingtime")).findFirst().orElseThrow(),"321",registries);
        check(((net.minecraft.world.item.crafting.AbstractCookingRecipe)cooking.decode(registries)).getCookingTime()==321,"Cooking time ignored");checks++;
        var old=cooking.json();try{cooking.setNumber(cooking.numericFields().stream().filter(f->f.label().equals("cookingtime")).findFirst().orElseThrow(),"0.5",registries);throw new AssertionError("Truncated time accepted");}catch(IllegalArgumentException expected){check(cooking.json().equals(old),"Rollback failed");checks++;}
        var server=helper.getLevel().getServer();var source=RecipeSource.read(server,new ResourceLocation("vjrm","source_check"));check(source.json().get("source_marker").getAsInt()==37,"Raw source metadata lost");checks++;
        var machine=RecipeSource.read(server,new ResourceLocation("vjrm","test_machine"));
        machine.setNumbers(java.util.Map.of(java.util.List.of("duration"),"777",java.util.List.of("energy"),"9223372036854775806"),registries);
        var decoded=(PortMachineRecipe)machine.decode(registries);check(decoded.duration()==777&&decoded.energy()==9223372036854775806L,"Machine numeric precision lost");checks++;
        var named=new ItemStack(Items.EMERALD);named.setTag(net.minecraft.nbt.TagParser.parseTag("{CustomModelData:37,display:{Name:'{\"text\":\"Port test\"}'}}"));
        var machineOutput=machine.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();machine.accept(machineOutput,named,3,registries);
        check(ItemStack.isSameItemSameTags(((PortMachineRecipe)machine.decode(registries)).result(),named)&&((PortMachineRecipe)machine.decode(registries)).result().getCount()==3,"Forge result NBT lost");checks++;
        var tagDoc=new RecipeDocument(JsonParser.parseString("{\"type\":\"create:mixing\",\"ingredients\":[{\"fluidTag\":\"minecraft:water\",\"amount\":250}]}").getAsJsonObject());
        var tagField=tagDoc.fields().get(0);check(tagField.fluid()&&!tagDoc.values(tagField,registries).isEmpty(),"1.20 Create fluidTag not recognized");checks++;
        tagDoc.accept(tagField,new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1),300,registries);check(tagDoc.get(tagField).getAsJsonObject().has("fluid")&&!tagDoc.get(tagField).getAsJsonObject().has("fluidTag"),"Old Create fluid tag replacement failed");checks++;
        machine.accept(machine.fields().stream().filter(RecipeDocument.Field::fluid).findFirst().orElseThrow(),new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1),500,registries);check(((PortMachineRecipe)machine.decode(registries)).fluid().getAmount()==500,"Forge fluid edit failed");checks++;
        check(RecipeSource.catalog(server).stream().anyMatch(s->s.serializer().equals("vjrm:test_machine")),"Machine source not catalogued");checks++;
        var root=Path.of("../../build/verification/pack").toAbsolutePath();DatapackExporter.writePackMeta(root);var file=DatapackExporter.export(root,"vjrm","port_check",machine.json(),true);
        check(file.toString().replace('\\','/').contains("/recipes/")&&Files.readString(root.resolve("pack.mcmeta")).contains("15"),"Wrong datapack version/path");checks++;
        new RecipeDocument(JsonParser.parseString(Files.readString(file)).getAsJsonObject()).decode(registries);checks++;
        try{DatapackExporter.export(root,"vjrm","../../escape",machine.json(),false);throw new AssertionError("Traversal accepted");}catch(java.io.IOException expected){checks++;}
        for(String path:java.util.List.of("../escape","/absolute","a/../escape","a//b")){try{DatapackExporter.export(root,"vjrm",path,machine.json(),false);throw new AssertionError("Invalid path accepted: "+path);}catch(java.io.IOException expected){checks++;}}
        var denied=new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"NoPermission")){@Override public boolean hasPermissions(int level){return level==0;}};
        var operator=new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Operator")){@Override public boolean hasPermissions(int level){return true;}};
        var request=new RecipeNetwork.ExportRequest("vjrm:permission_check",machine.json().toString(),true);
        check(RecipeNetwork.processExport(request,denied).detail().equals("permission"),"Unprivileged export accepted");checks++;
        check(RecipeNetwork.processExport(request,operator).success(),"Authorized export denied");checks++;
        check(RecipeNetwork.processExport(request,operator).detail().equals("rate"),"Export rate limit ignored");checks++;
        check(!RecipeNetwork.exportFits("中".repeat(11000)),"Oversize UTF-8 C2S payload accepted");checks++;
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{var packet=new RecipeSourceNetwork.Response(42,machine.json().toString(),"");packet.encode(buffer);check(RecipeSourceNetwork.Response.decode(buffer).equals(packet),"Source packet codec failed");checks++;}finally{buffer.release();}
        for(String invalid:java.util.List.of("{bad:1}","{\"a\":1,}","{\"a\":NaN}","[]")){try{com.pureneko.vjrm.client.JsonSyntax.parse(invalid);throw new AssertionError("Invalid JSON accepted");}catch(JsonParseException expected){checks++;}}
        var result=Path.of("../../build/verification/server/result.txt");Files.createDirectories(result.getParent());Files.writeString(result,"PASS: "+checks+" recipe/source/IO/network/JSON checks\n");
        System.out.println("PASS "+checks+" Forge 1.20.1 recipe/source/IO/network/JSON checks");helper.succeed();
    }
}
