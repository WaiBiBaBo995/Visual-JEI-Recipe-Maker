package com.pureneko.vjrm.client;
import com.pureneko.vjrm.network.RecipeNetwork.ExportResult;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ExportResultHandler {
    public static void handle(ExportResult result,IPayloadContext context){
        Component detail=result.detail().equals("permission")||result.detail().equals("rate")?Component.translatable("vjrm.result."+result.detail()):Component.literal(result.detail());
        Component text=Component.translatable(result.success()?"vjrm.server_exported":"vjrm.error.invalid",detail);
        if(context.player()!=null)context.player().displayClientMessage(text,false);
        if(Minecraft.getInstance().screen instanceof RecipeEditorScreen screen)screen.exportResult(result.success(),result.detail());
    }
}
