package com.pureneko.vjrm;

import com.google.gson.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@EventBusSubscriber(modid="vjrm",bus=EventBusSubscriber.Bus.MOD)
public record PortMachineRecipe(ResourceLocation id,Ingredient input,FluidStack fluid,ItemStack result,int duration,float chance,long energy) implements Recipe<Container> {
    public static final ResourceLocation ID=new ResourceLocation("vjrm","test_machine");
    public static final RecipeType<PortMachineRecipe> TYPE=new RecipeType<>(){};
    public static final RecipeSerializer<PortMachineRecipe> SERIALIZER=new RecipeSerializer<>(){
        public PortMachineRecipe fromJson(ResourceLocation id,JsonObject json){
            var fluid=json.getAsJsonObject("fluid_input");
            return new PortMachineRecipe(id,Ingredient.fromJson(json.get("input")),new FluidStack(net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluid.get("fluid").getAsString())),fluid.get("amount").getAsBigDecimal().intValueExact()),CraftingHelper.getItemStack(json.getAsJsonObject("result"),true),json.get("duration").getAsBigDecimal().intValueExact(),json.get("chance").getAsFloat(),json.get("energy").getAsBigDecimal().longValueExact());
        }
        public PortMachineRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return new PortMachineRecipe(id,Ingredient.fromNetwork(b),b.readFluidStack(),b.readItem(),b.readVarInt(),b.readFloat(),b.readLong());}
        public void toNetwork(FriendlyByteBuf b,PortMachineRecipe r){r.input.toNetwork(b);b.writeFluidStack(r.fluid);b.writeItem(r.result);b.writeVarInt(r.duration);b.writeFloat(r.chance);b.writeLong(r.energy);}
    };
    @SubscribeEvent public static void register(RegisterEvent event){event.register(Registries.RECIPE_TYPE,h->h.register(ID,TYPE));event.register(Registries.RECIPE_SERIALIZER,h->h.register(ID,SERIALIZER));}
    public boolean matches(Container c,Level level){return false;}
    public ItemStack assemble(Container c,RegistryAccess access){return result.copy();}
    public boolean canCraftInDimensions(int width,int height){return true;}
    public ItemStack getResultItem(RegistryAccess access){return result.copy();}
    public ResourceLocation getId(){return id;}
    public RecipeSerializer<?> getSerializer(){return SERIALIZER;}
    public RecipeType<?> getType(){return TYPE;}
}
