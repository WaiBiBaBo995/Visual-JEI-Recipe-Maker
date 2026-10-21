package com.pureneko.vjrm.client;

import net.minecraft.client.gui.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;
import java.util.List;

/** Converts the editor's drawing operations into 26.1 GUI render-state extraction. */
public final class LegacyGraphics {
    private static int textColor(int color){return (color&0xFF000000)==0?color|0xFF000000:color;}
    public final GuiGraphicsExtractor nativeGraphics;
    public LegacyGraphics(GuiGraphicsExtractor graphics){nativeGraphics=graphics;}
    public final class Pose {
        public void pushPose(){nativeGraphics.pose().pushMatrix();}
        public void popPose(){nativeGraphics.pose().popMatrix();}
        public void translate(double x,double y,double z){nativeGraphics.pose().translate((float)x,(float)y);}
        public void scale(float x,float y,float z){nativeGraphics.pose().scale(x,y);}
    }
    private final Pose pose=new Pose();
    public Pose pose(){return pose;}
    public void fill(int x1,int y1,int x2,int y2,int color){nativeGraphics.fill(x1,y1,x2,y2,color);}
    public void renderOutline(int x,int y,int w,int h,int color){nativeGraphics.outline(x,y,w,h,color);}
    public void drawString(Font font,String text,int x,int y,int color){nativeGraphics.text(font,text,x,y,textColor(color));}
    public void drawString(Font font,Component text,int x,int y,int color){nativeGraphics.text(font,text,x,y,textColor(color));}
    public void drawString(Font font,Component text,int x,int y,int color,boolean shadow){nativeGraphics.text(font,text,x,y,textColor(color),shadow);}
    public void drawString(Font font,String text,int x,int y,int color,boolean shadow){nativeGraphics.text(font,text,x,y,textColor(color),shadow);}
    public void drawCenteredString(Font font,Component text,int x,int y,int color){nativeGraphics.centeredText(font,text,x,y,textColor(color));}
    public void drawWordWrap(Font font,FormattedText text,int x,int y,int w,int color){nativeGraphics.textWithWordWrap(font,text,x,y,w,textColor(color));}
    public void renderItem(ItemStack stack,int x,int y){nativeGraphics.item(stack,x,y);}
    public void renderItemDecorations(Font font,ItemStack stack,int x,int y){nativeGraphics.itemDecorations(font,stack,x,y);}
    private Vector2f point(int x,int y){return nativeGraphics.pose().transformPosition(new Vector2f(x,y));}
    public void renderTooltip(Font font,List<? extends FormattedCharSequence> lines,int x,int y){var p=point(x,y);nativeGraphics.setTooltipForNextFrame(font,lines,(int)p.x,(int)p.y);}
    public void renderTooltip(Font font,ItemStack stack,int x,int y){var p=point(x,y);nativeGraphics.setTooltipForNextFrame(font,stack,(int)p.x,(int)p.y);}
    public void renderComponentTooltip(Font font,List<Component> lines,int x,int y){var p=point(x,y);nativeGraphics.setComponentTooltipForNextFrame(font,lines,(int)p.x,(int)p.y);}
}
