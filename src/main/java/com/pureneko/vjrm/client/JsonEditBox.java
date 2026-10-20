package com.pureneko.vjrm.client;

import net.minecraft.Util;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;

/** Native multiline editing, selection and clipboard with colored tokens and editing aids. */
final class JsonEditBox extends AbstractScrollWidget {
    private static final int GUTTER=32;
    private final Font font;
    private final TextField text;
    private List<JsonSyntax.Token> tokens=List.of();
    private Map<Integer,Integer> brackets=Map.of();
    private final Deque<Snapshot> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private record Snapshot(String value,int cursor) { }
    private final class TextField extends MultilineTextField {
        TextField(){super(JsonEditBox.this.font,JsonEditBox.this.width-totalInnerPadding()-GUTTER);}
        List<int[]> lines(){var out=new ArrayList<int[]>();for(var line:iterateLines())out.add(new int[]{line.beginIndex(),line.endIndex()});return out;}
        int[] selection(){var selected=getSelected();return new int[]{selected.beginIndex(),selected.endIndex()};}
    }
    JsonEditBox(Font font,int x,int y,int width,int height,Component title){
        super(x,y,width,height,title);this.font=font;text=new TextField();
        text.setCursorListener(this::scrollToCursor);
        text.setValueListener(v->{tokens=JsonSyntax.tokens(v);brackets=JsonSyntax.brackets(v,tokens);});
    }
    void setValueListener(Consumer<String> listener){text.setValueListener(v->{tokens=JsonSyntax.tokens(v);brackets=JsonSyntax.brackets(v,tokens);listener.accept(v);});}
    void setValue(String value){text.setValue(value);}
    String getValue(){return text.value();}
    int cursor(){return text.cursor();}
    void replaceText(String value){edit(()->text.setValue(value));}
    private Snapshot snapshot(){return new Snapshot(text.value(),text.cursor());}
    private void edit(Runnable action){
        var before=snapshot();action.run();if(!before.value.equals(text.value())){undo.push(before);if(undo.size()>100)undo.removeLast();redo.clear();}
    }
    private void restore(Deque<Snapshot> from,Deque<Snapshot> to){
        if(from.isEmpty())return;to.push(snapshot());var state=from.pop();text.setSelecting(false);text.setValue(state.value);text.seekCursor(Whence.ABSOLUTE,state.cursor);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(!isFocused())return false;
        if(Screen.hasControlDown()&&key==GLFW.GLFW_KEY_Z){if(Screen.hasShiftDown())restore(redo,undo);else restore(undo,redo);return true;}
        if(Screen.hasControlDown()&&key==GLFW.GLFW_KEY_Y){restore(redo,undo);return true;}
        if(key==GLFW.GLFW_KEY_TAB){edit(()->text.insertText("  "));return true;}
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){
            edit(()->{
                String value=text.value();int cursor=text.cursor(),start=value.lastIndexOf('\n',Math.max(0,cursor-1))+1,end=start;
                while(end<cursor&&value.charAt(end)==' ')end++;
                String indent=value.substring(start,end);boolean opening=cursor>0&&"{[".indexOf(value.charAt(cursor-1))>=0&&!JsonSyntax.inString(value,cursor);
                boolean pair=opening&&cursor<value.length()&&value.charAt(cursor)==(value.charAt(cursor-1)=='{'?'}':']');
                String insertion="\n"+indent+(opening?"  ":"");
                text.insertText(insertion+(pair?"\n"+indent:""));if(pair)text.seekCursor(Whence.RELATIVE,-indent.length()-1);
            });return true;
        }
        if(key==GLFW.GLFW_KEY_BACKSPACE&&!text.hasSelection()&&text.cursor()>0&&text.cursor()<text.value().length()){
            String pair=text.value().substring(text.cursor()-1,text.cursor()+1);
            if(Set.of("{}","[]","\"\"").contains(pair)){edit(()->{text.deleteText(1);text.deleteText(-1);});return true;}
        }
        final boolean[] handled={false};edit(()->handled[0]=text.keyPressed(key));return handled[0];
    }
    @Override public boolean charTyped(char c,int modifiers){
        if(!visible||!isFocused()||!StringUtil.isAllowedChatCharacter(c))return false;
        edit(()->{
            String value=text.value();int cursor=text.cursor();boolean inside=JsonSyntax.inString(value,cursor);
            if(!text.hasSelection()&&cursor<value.length()&&value.charAt(cursor)==c&&(c=='}'||c==']'||c=='"'&&inside)){
                // Skip only a bracket known to have an opening partner, or a closing quote.
                if(c=='"'||brackets.containsKey(cursor)){text.seekCursor(Whence.RELATIVE,1);return;}
            }
            if(!inside&&(c=='{'||c=='['||c=='"')){
                String selected=text.getSelectedText();text.insertText(""+c+selected+(c=='{'?'}':c=='['?']':'"'));text.seekCursor(Whence.RELATIVE,-1);
            }else text.insertText(String.valueOf(c));
        });return true;
    }
    private void seek(double x,double y){text.seekCursorToPoint(x-getX()-innerPadding()-GUTTER,y-getY()-innerPadding()+scrollAmount());}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(withinContentAreaPoint(x,y)&&button==0){text.setSelecting(Screen.hasShiftDown());seek(x,y);return true;}return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(super.mouseDragged(x,y,button,dx,dy))return true;
        if(withinContentAreaPoint(x,y)&&button==0){text.setSelecting(true);seek(x,y);text.setSelecting(Screen.hasShiftDown());return true;}return false;
    }
    private void scrollToCursor(){
        int y=text.getLineAtCursor()*9,display=height-totalInnerPadding();
        if(y<scrollAmount())setScrollAmount(y);else if(y+9>scrollAmount()+display)setScrollAmount(y+9-display);
    }
    @Override protected int getInnerHeight(){return text.getLineCount()*9;}
    @Override protected double scrollRate(){return 18;}
    @Override public void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,Component.translatable("gui.narrate.editBox",getMessage(),text.value()));}
    @Override protected void renderContents(GuiGraphics g,int mx,int my,float delta){
        String value=text.value();int x=getX()+innerPadding()+GUTTER,y=getY()+innerPadding(),logical=1,previous=0,tokenIndex=0;
        int cursor=text.cursor(),bracket=brackets.containsKey(cursor)?cursor:brackets.containsKey(cursor-1)?cursor-1:-1;
        int match=brackets.getOrDefault(bracket,-1);int[] selection=text.selection();
        for(var line:text.lines()){
            int start=line[0],end=line[1];
            for(int i=previous;i<start;i++)if(value.charAt(i)=='\n')logical++;previous=start;
            if(withinContentAreaTopBottom(y,y+9)){
                if(start==0||value.charAt(start-1)=='\n')g.drawString(font,Integer.toString(logical),getX()+innerPadding(),y,0xFF6A737D,false);
                for(int at:new int[]{bracket,match})if(at>=start&&at<end){int bx=x+font.width(value.substring(start,at));g.fill(bx,y-1,bx+font.width(value.substring(at,at+1)),y+9,0x805599BB);}
                while(tokenIndex<tokens.size()&&tokens.get(tokenIndex).end()<=start)tokenIndex++;
                var styled=Component.empty();int position=start;
                for(int i=tokenIndex;i<tokens.size()&&tokens.get(i).start()<end;i++){
                    var token=tokens.get(i);int a=Math.max(start,token.start()),b=Math.min(end,token.end());
                    if(a>position)styled.append(Component.literal(value.substring(position,a)).withStyle(Style.EMPTY.withColor(0xD4D4D4)));
                    if(b>a)styled.append(Component.literal(value.substring(a,b)).withStyle(Style.EMPTY.withColor(token.kind().color)));position=b;
                }
                if(position<end)styled.append(Component.literal(value.substring(position,end)));
                g.drawString(font,styled,x,y,0xFFD4D4D4,false);
                if(isFocused()&&Util.getMillis()/500%2==0&&cursor>=start&&cursor<=end){int cx=x+font.width(value.substring(start,cursor));g.fill(cx,y-1,cx+1,y+9,0xFFFFFFFF);}
                if(selection[0]!=selection[1]&&selection[0]<=end&&selection[1]>=start){
                    int a=x+font.width(value.substring(start,Math.max(start,selection[0]))),b=x+font.width(value.substring(start,Math.min(end,selection[1])));
                    g.fill(RenderType.guiTextHighlight(),a,y,b,y+9,0xFF3366FF);
                }
            }
            y+=9;
        }
    }
}
