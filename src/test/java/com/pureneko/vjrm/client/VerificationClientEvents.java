package com.pureneko.vjrm.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Load optional viewer test classes only when the selected viewer is on the runtime classpath. */
@EventBusSubscriber(modid="vjrm",value=Dist.CLIENT)
public final class VerificationClientEvents {
    private static void dispatch(String target,String method,Object event){
        try{Class.forName("com.pureneko.vjrm.client."+target).getMethod(method,event.getClass()).invoke(null,event);}
        catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("vjrm.verifyClient"))return;
        dispatch(Boolean.getBoolean("vjrm.verifyEmi")?"EmiClientChecks":"ClientChecks","tick",event);
    }
    @SubscribeEvent public static void rendered(RenderFrameEvent.Post event){
        if(!Boolean.getBoolean("vjrm.verifyClient"))return;
        dispatch(Boolean.getBoolean("vjrm.verifyEmi")?"EmiClientChecks":"ClientChecks","rendered",event);
        if(Boolean.getBoolean("vjrm.verifyCreate")&&!Boolean.getBoolean("vjrm.verifyEmi"))dispatch("CreateClientChecks","rendered",event);
    }
}
