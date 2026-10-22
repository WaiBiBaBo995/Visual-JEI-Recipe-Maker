package com.pureneko.vjrm.client;
import com.pureneko.vjrm.recipe.*;
import net.minecraft.client.Minecraft;
final class GtJeiSource {
 @SuppressWarnings({"rawtypes","unchecked"}) static Object[] choose(){
  var runtime=VjrmJeiPlugin.runtime();
  for(mezz.jei.api.recipe.category.IRecipeCategory category:runtime.getRecipeManager().createRecipeCategoryLookup().get().toList())if(category.getRecipeType().getUid().getPath().equals("chemical_reactor")){
   for(Object value:runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).get().toList()){
    if(!GtRecipeSupport.isRecipe(value))continue;
    var doc=GtRecipeSupport.copy((net.minecraft.world.item.crafting.Recipe<?>)value,Minecraft.getInstance().level.registryAccess());
    if(doc.fields().stream().anyMatch(RecipeDocument.Field::fluid)&&doc.fields().stream().anyMatch(f->!f.fluid())){
     var layout=VjrmJeiPlugin.sourceLayout(category,value).orElseThrow();
     var factory=runtime.getRecipeManager().getRecipeButtonControllerFactories().stream().filter(f->f.getClass().getName().startsWith(VjrmJeiPlugin.class.getName()+"$")).findFirst().orElseThrow();
     if(factory.createButtonController(layout)==null)throw new AssertionError("GT JEI copy button missing");
     return new Object[]{VjrmJeiPlugin.sourceId(category,value),RecipePreview.category(category),value};
    }
   }
  }return null;
 }
}
