package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.ChemicalSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** Ingredient identity is available even when advanced tooltips are disabled. */
final class IngredientTooltips {
    static List<Component> of(Object value){
        var lines=new ArrayList<Component>();
        if(value instanceof ItemStack item&&!item.isEmpty())lines.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(),item));
        else if(value instanceof FluidStack fluid&&!fluid.isEmpty()){
            lines.add(fluid.getHoverName());lines.add(Component.translatable("vjrm.tooltip.amount",fluid.getAmount()+" mB").withStyle(ChatFormatting.GRAY));
        }else if(com.pureneko.vjrm.recipe.AspectSupport.isStack(value)){
            lines.add(com.pureneko.vjrm.recipe.AspectSupport.name(value));lines.add(Component.translatable("vjrm.tooltip.amount",com.pureneko.vjrm.recipe.AspectSupport.amount(value)).withStyle(ChatFormatting.GRAY));
        }else if(ChemicalSupport.isStack(value)){
            Object chemical=ChemicalSupport.unwrap(value);
            try{lines.add((Component)chemical.getClass().getMethod("getTextComponent").invoke(chemical));lines.add(Component.translatable("vjrm.tooltip.amount",chemical.getClass().getMethod("getAmount").invoke(chemical)).withStyle(ChatFormatting.GRAY));}
            catch(ReflectiveOperationException ex){lines.add(Component.literal(ChemicalSupport.id(value)));}
        }
        return identify(lines,value);
    }
    static List<Component> identify(List<Component> nativeLines,Object value){
        var lines=new ArrayList<Component>(nativeLines);Identifier id=null;
        if(value instanceof ItemStack item&&!item.isEmpty())id=BuiltInRegistries.ITEM.getKey(item.getItem());
        else if(value instanceof FluidStack fluid&&!fluid.isEmpty())id=BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        else if(com.pureneko.vjrm.recipe.AspectSupport.isStack(value))id=Identifier.tryParse(com.pureneko.vjrm.recipe.AspectSupport.id(value));
        else if(ChemicalSupport.isStack(value))id=Identifier.tryParse(ChemicalSupport.id(value));
        if(id!=null){
            String key=id.toString(),namespace=id.getNamespace();
            if(lines.stream().noneMatch(c->c.getString().contains(key)))lines.add(Component.literal(key).withStyle(ChatFormatting.DARK_GRAY));
            String mod=namespace.equals("minecraft")?"Minecraft":net.neoforged.fml.ModList.get().getModContainerById(namespace).map(c->c.getModInfo().getDisplayName()).orElse(namespace);
            if(lines.stream().noneMatch(c->c.getString().equalsIgnoreCase(mod)))lines.add(Component.literal(mod).withStyle(ChatFormatting.BLUE,ChatFormatting.ITALIC));
        }
        return lines;
    }
    private IngredientTooltips(){}
}
