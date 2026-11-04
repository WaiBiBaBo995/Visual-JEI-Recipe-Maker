package com.pureneko.vjrm.client;

import net.minecraft.client.gui.Font;
import com.pureneko.vjrm.client.LegacyGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import java.util.*;
import org.lwjgl.glfw.GLFW;

/** A prefix list rendered above the editor field, using its scaled UI coordinates. */
final class DatapackNameBox extends EditBox {
    private final Font labelFont;
    private final SortedMap<String,Integer> names=new TreeMap<>();
    private List<String> matches=List.of();
    private boolean open,requested;
    private int selected,start,popupX,popupY,popupWidth;
    private static final int ROW=14,MAX_ROWS=5;
    DatapackNameBox(Font font,int x,int y,int width,int height){super(font,x,y,width,height,Component.translatable("vjrm.manage.name"));labelFont=font;}
    void addName(String name,boolean world){names.merge(name,world?2:1,(a,b)->a|b);refresh(false);}
    void changed(){refresh(true);}
    void dismiss(){open=false;requested=false;setSuggestion(null);}
    private void refresh(boolean edited){
        matches=names.keySet().stream().filter(name->name.startsWith(getValue())).toList();
        if(edited){selected=0;start=0;requested=isFocused();}
        open=requested&&isFocused()&&!matches.isEmpty();
        selected=Math.max(0,Math.min(selected,matches.size()-1));
        if(isFocused()&&open)tail();
    }
    private void tail(){
        if(matches.isEmpty()||!isFocused()){setSuggestion(null);return;}
        String name=matches.get(selected);setSuggestion(getCursorPosition()==getValue().length()?name.substring(getValue().length()):null);
    }
    boolean showing(){return visible&&isFocused()&&open&&!matches.isEmpty();}
    private void accept(){String name=matches.get(selected);setValue(name);moveCursorToEnd(false);dismiss();}
    boolean popupKey(int key){
        if(!visible||!isFocused())return false;
        if(key==GLFW.GLFW_KEY_ESCAPE&&showing()){dismiss();return true;}
        if(key==GLFW.GLFW_KEY_DOWN||key==GLFW.GLFW_KEY_UP){
            if(matches.isEmpty())return false;
            if(!open){open=true;requested=true;selected=key==GLFW.GLFW_KEY_DOWN?0:matches.size()-1;}
            else selected=Math.floorMod(selected+(key==GLFW.GLFW_KEY_DOWN?1:-1),matches.size());
            start=Math.max(0,Math.min(start,selected));if(selected>=start+MAX_ROWS)start=selected-MAX_ROWS+1;tail();return true;
        }
        if((key==GLFW.GLFW_KEY_TAB||key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)&&showing()){accept();return true;}
        return false;
    }
    boolean popupClick(double x,double y,int button){
        if(!showing())return false;
        int count=Math.min(MAX_ROWS,matches.size());
        if(x>=popupX&&x<popupX+popupWidth&&y>=popupY&&y<popupY+count*ROW){
            if(button==0){selected=start+(int)(y-popupY)/ROW;accept();}return true;
        }
        dismiss();return false;
    }
    boolean popupScroll(double x,double y,double amount){
        if(!showing()||x<popupX||x>=popupX+popupWidth||y<popupY||y>=getY())return false;
        start=Math.max(0,Math.min(Math.max(0,matches.size()-MAX_ROWS),start-(int)Math.signum(amount)));
        selected=Math.max(start,Math.min(selected,start+MAX_ROWS-1));tail();return true;
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick){
        int button=event.button();boolean hit=super.mouseClicked(event,doubleClick);if(hit&&button==0){requested=true;open=!matches.isEmpty();tail();}return hit;
    }
    void renderPopup(LegacyGraphics g,int mouseX,int mouseY,int minX,int maxX){
        if(!isFocused()){dismiss();return;}if(!showing())return;tail();
        int count=Math.min(MAX_ROWS,matches.size());popupWidth=Math.min(274,maxX-minX);popupX=Math.max(minX,Math.min(getX(),maxX-popupWidth));popupY=getY()-count*ROW-2;
        g.pose().pushPose();g.pose().translate(0,0,500);
        g.fill(popupX-1,popupY-1,popupX+popupWidth+1,popupY+count*ROW+1,0xFF91969F);
        g.fill(popupX,popupY,popupX+popupWidth,popupY+count*ROW,0xFF10141C);
        for(int i=0;i<count;i++){
            int index=start+i,y=popupY+i*ROW;String name=matches.get(index);
            if(index==selected||mouseX>=popupX&&mouseX<popupX+popupWidth&&mouseY>=y&&mouseY<y+ROW)g.fill(popupX,y,popupX+popupWidth,y+ROW,0xFF39485C);
            int origin=names.get(name);String label=Component.translatable("vjrm.manage.completion."+origin).getString();int labelWidth=labelFont.width(label);
            g.drawString(labelFont,labelFont.plainSubstrByWidth(name,Math.max(20,popupWidth-labelWidth-12)),popupX+4,y+3,0xFFFFFFFF);
            g.drawString(labelFont,label,popupX+popupWidth-labelWidth-4,y+3,0xFFB0BECD);
        }
        g.pose().popPose();
    }
}
