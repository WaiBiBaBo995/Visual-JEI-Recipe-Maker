package com.pureneko.vjrm.client;
final class GtEmiSource {
 static Object[] choose()throws Exception{
  for(var recipe:dev.emi.emi.api.EmiApi.getRecipeManager().getRecipes())if(recipe.getClass().getSimpleName().equals("GTEmiRecipe")&&recipe.getCategory().getId().getPath().equals("chemical_reactor")){
   var stacks=recipe.getInputs().stream().flatMap(i->i.getEmiStacks().stream()).toList();
   if(stacks.stream().anyMatch(s->!s.getItemStack().isEmpty())&&stacks.stream().anyMatch(s->s.getItemStack().isEmpty())){
    var method=VjrmEmiPlugin.class.getDeclaredMethod("sourceId",dev.emi.emi.api.recipe.EmiRecipe.class);method.setAccessible(true);
    if(!recipe.getId().equals(method.invoke(null,recipe)))throw new AssertionError("GT EMI copy source missing");
    return new Object[]{recipe.getId(),RecipePreview.category(recipe.getCategory()),recipe};
   }
  }return null;
 }
}
