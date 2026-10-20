package com.pureneko.vjrm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** A non-vanilla serializer used only in dev/test runs; never packaged in the release jar. */
@EventBusSubscriber(modid="vjrm",bus=EventBusSubscriber.Bus.MOD)
public record TestMachineRecipe(SizedIngredient input,SizedFluidIngredient fluidInput,ItemStack result,int duration,float chance) implements Recipe<CraftingInput> {
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("vjrm","test_machine");
    public static final RecipeType<TestMachineRecipe> TYPE=new RecipeType<>(){public String toString(){return ID.toString();}};
    public static final MapCodec<TestMachineRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        SizedIngredient.NESTED_CODEC.fieldOf("input").forGetter(TestMachineRecipe::input),
        SizedFluidIngredient.NESTED_CODEC.fieldOf("fluid_input").forGetter(TestMachineRecipe::fluidInput),
        ItemStack.CODEC.fieldOf("result").forGetter(TestMachineRecipe::result),
        Codec.INT.fieldOf("duration").forGetter(TestMachineRecipe::duration),
        Codec.FLOAT.fieldOf("chance").forGetter(TestMachineRecipe::chance)
    ).apply(i,TestMachineRecipe::new));
    public static final RecipeSerializer<TestMachineRecipe> SERIALIZER=new RecipeSerializer<>(){
        public MapCodec<TestMachineRecipe> codec(){return CODEC;}
        public StreamCodec<RegistryFriendlyByteBuf,TestMachineRecipe> streamCodec(){return StreamCodec.of((b,r)->{
            SizedIngredient.STREAM_CODEC.encode(b,r.input);SizedFluidIngredient.STREAM_CODEC.encode(b,r.fluidInput);ItemStack.STREAM_CODEC.encode(b,r.result);b.writeVarInt(r.duration);b.writeFloat(r.chance);
        },b->new TestMachineRecipe(SizedIngredient.STREAM_CODEC.decode(b),SizedFluidIngredient.STREAM_CODEC.decode(b),ItemStack.STREAM_CODEC.decode(b),b.readVarInt(),b.readFloat()));}
    };
    @SubscribeEvent public static void register(RegisterEvent e){
        e.register(Registries.RECIPE_TYPE,h->h.register(ID,TYPE));
        e.register(Registries.RECIPE_SERIALIZER,h->h.register(ID,SERIALIZER));
    }
    public boolean matches(CraftingInput input,Level level){return false;}
    public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries){return result.copy();}
    public boolean canCraftInDimensions(int width,int height){return true;}
    public ItemStack getResultItem(HolderLookup.Provider registries){return result;}
    public RecipeSerializer<?> getSerializer(){return SERIALIZER;}
    public RecipeType<?> getType(){return TYPE;}
}
