package com.pureneko.vjrm.recipe;

import com.google.gson.JsonElement;
import com.mojang.serialization.*;
import net.minecraft.world.item.ItemStack;

/** Let GT choose its sized/component-aware item representation on each supported branch. */
final class GtIngredientSupport {
    @SuppressWarnings("unchecked") static JsonElement item(ItemStack stack,int count,DynamicOps<JsonElement> ops){
        if(count<1)throw new IllegalArgumentException("vjrm.error.amount");
        try{
            var type=Class.forName("com.gregtechceu.gtceu.api.recipe.content.SerializerIngredient");
            var serializer=type.getField("INSTANCE").get(null);
            var ingredient=type.getMethod("of",Object.class).invoke(serializer,stack.copyWithCount(count));
            var codec=(Codec<Object>)type.getMethod("codec").invoke(serializer);
            return codec.encodeStart(ops,ingredient).result().orElseThrow(()->new IllegalArgumentException("vjrm.error.ingredient"));
        }catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.error.ingredient",ex);}
    }
    private GtIngredientSupport(){}
}
