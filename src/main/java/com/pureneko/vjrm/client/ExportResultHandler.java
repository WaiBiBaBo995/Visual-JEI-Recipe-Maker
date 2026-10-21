package com.pureneko.vjrm.client;
import com.pureneko.vjrm.network.RecipeNetwork.ExportResult;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ExportResultHandler {
    public static void handle(ExportResult result){
        Component detail=result.detail().equals("permission")||result.detail().equals("rate")?Component.translatable("vjrm.result."+result.detail()):Component.literal(result.detail());
        Component text=Component.translatable(result.success()?"vjrm.server_exported":"vjrm.error.invalid",detail);
        if(Minecraft.getInstance().player!=null)Minecraft.getInstance().player.sendSystemMessage(text);
        if(Minecraft.getInstance().screen instanceof RecipeEditorScreen screen)screen.exportResult(result.success(),result.detail());
    }
}
