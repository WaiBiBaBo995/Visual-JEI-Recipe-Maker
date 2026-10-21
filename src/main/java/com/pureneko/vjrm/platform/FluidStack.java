package com.pureneko.vjrm.platform;
import com.google.gson.*;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import java.util.*;
/** Fluid quantities use Fabric transfer units (81000 droplets per bucket), never Forge mB. */
public final class FluidStack {
    private final Fluid fluid;private final int amount;
    private net.minecraft.core.component.DataComponentPatch components=net.minecraft.core.component.DataComponentPatch.EMPTY;
    public FluidStack(Fluid fluid,int amount){this.fluid=fluid;this.amount=amount;}
    public FluidStack(net.minecraft.core.Holder<Fluid> fluid,int amount){this(fluid.value(),amount);}
    public Fluid getFluid(){return fluid;}public int getAmount(){return amount;}
    public boolean isEmpty(){return fluid==Fluids.EMPTY||amount<=0;}
    public Component getHoverName(){return FluidVariantAttributes.getName(variant());}
    public FluidVariant variant(){return FluidVariant.of(fluid,components);}
    public FluidStack copyWithAmount(int amount){var copy=new FluidStack(fluid,amount);copy.components=components;return copy;}
    public net.minecraft.core.component.DataComponentPatch getComponentsPatch(){return components;}
    public void applyComponents(net.minecraft.core.component.DataComponentPatch patch){components=patch;}
    public JsonElement componentsJson(net.minecraft.core.HolderLookup.Provider registries){return net.minecraft.core.component.DataComponentPatch.CODEC.encodeStart(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),components).getOrThrow();}
    public static List<FluidStack> values(JsonElement node,int amount,net.minecraft.core.HolderLookup.Provider registries){
        if(node.isJsonArray()){var out=new ArrayList<FluidStack>();for(var e:node.getAsJsonArray())out.addAll(values(e,amount,registries));return out;}
        JsonObject o=node.isJsonObject()?node.getAsJsonObject():new JsonObject();
        String value=node.isJsonPrimitive()&&node.getAsJsonPrimitive().isString()?node.getAsString():o.has("fluid")?o.get("fluid").getAsString():o.has("id")?o.get("id").getAsString():null;
        String tag=o.has("fluidTag")?o.get("fluidTag").getAsString():o.has("tag")?o.get("tag").getAsString():value!=null&&value.startsWith("#")?value.substring(1):null;
        if(tag!=null){var key=net.minecraft.tags.TagKey.create(Registries.FLUID,Identifier.parse(tag));return registries.lookupOrThrow(Registries.FLUID).get(key).map(set->set.stream().map(h->new FluidStack(h,amount)).toList()).orElseGet(List::of);}
        if(value==null)return List.of();var id=Identifier.parse(value);if(!BuiltInRegistries.FLUID.containsKey(id))return List.of();
        var stack=new FluidStack(BuiltInRegistries.FLUID.getValue(id),amount);if(o.has("components"))stack.applyComponents(net.minecraft.core.component.DataComponentPatch.CODEC.parse(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),o.get("components")).getOrThrow());return List.of(stack);
    }
}
