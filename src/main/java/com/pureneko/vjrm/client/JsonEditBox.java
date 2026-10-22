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
    private List<IngredientAnnotations.Icon> ingredientIcons=List.of();
    private AnnotatedTextLayout visual;
    void setIngredientIcons(List<IngredientAnnotations.Icon> icons){ingredientIcons=List.copyOf(icons);visual=null;scrollToCursor();}
    private AnnotatedTextLayout visual(){
        if(visual==null||!visual.value.equals(text.value()))visual=new AnnotatedTextLayout(font,text.value(),width-totalInnerPadding()-GUTTER,ingredientIcons);
        return visual;
    }
    private boolean navigate(int key){
        if((key==GLFW.GLFW_KEY_HOME||key==GLFW.GLFW_KEY_END)&&Screen.hasControlDown())return false;
        var layout=visual();int row=layout.row(text.cursor()),target;
        switch(key){
            case GLFW.GLFW_KEY_HOME -> target=layout.lines.get(row).start();
            case GLFW.GLFW_KEY_END -> target=layout.lines.get(row).end();
            case GLFW.GLFW_KEY_UP,GLFW.GLFW_KEY_DOWN,GLFW.GLFW_KEY_PAGE_UP,GLFW.GLFW_KEY_PAGE_DOWN -> {
                int amount=(key==GLFW.GLFW_KEY_PAGE_UP||key==GLFW.GLFW_KEY_PAGE_DOWN)?Math.max(1,(height-totalInnerPadding())/18):1;
                int next=row+((key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_PAGE_UP)?-amount:amount);
                target=layout.hit(next,layout.x(layout.lines.get(row).start(),text.cursor()));
            }
            default -> {return false;}
        }
        text.setSelecting(Screen.hasShiftDown());text.seekCursor(Whence.ABSOLUTE,target);return true;
    }
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
        text.setValueListener(v->{ingredientIcons=List.of();visual=null;tokens=JsonSyntax.tokens(v);brackets=JsonSyntax.brackets(v,tokens);});
    }
    void setValueListener(Consumer<String> listener){text.setValueListener(v->{ingredientIcons=List.of();visual=null;tokens=JsonSyntax.tokens(v);brackets=JsonSyntax.brackets(v,tokens);listener.accept(v);});}
    void setValue(String value){text.setValue(value);}
    String getValue(){return text.value();}
    void moveToStart(){text.setSelecting(false);text.seekCursor(Whence.ABSOLUTE,0);}
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
        if(navigate(key))return true;
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
        if(!visible||!isFocused()||!net.minecraft.SharedConstants.isAllowedChatCharacter(c))return false;
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
    private void seek(double x,double y){text.seekCursor(Whence.ABSOLUTE,visual().hit((int)((y-getY()-innerPadding()+scrollAmount())/18),x-getX()-innerPadding()-GUTTER));}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(withinContentAreaPoint(x,y)&&button==0){text.setSelecting(Screen.hasShiftDown());seek(x,y);return true;}return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(super.mouseDragged(x,y,button,dx,dy))return true;
        if(withinContentAreaPoint(x,y)&&button==0){text.setSelecting(true);seek(x,y);text.setSelecting(Screen.hasShiftDown());return true;}return false;
    }
    private void scrollToCursor(){
        int y=visual().row(text.cursor())*18,display=height-totalInnerPadding();
        if(y<scrollAmount())setScrollAmount(y);else if(y+18>scrollAmount()+display)setScrollAmount(y+18-display);
    }
    @Override protected int getInnerHeight(){return visual().lines.size()*18;}
    @Override protected double scrollRate(){return 18;}
    @Override public void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,Component.translatable("gui.narrate.editBox",getMessage(),text.value()));}
    @Override protected void renderContents(GuiGraphics g,int mx,int my,float delta){
        String value=text.value();var layout=visual();int x=getX()+innerPadding()+GUTTER,y=getY()+innerPadding()+4,logical=1,previous=0;
        int cursor=text.cursor(),bracket=brackets.containsKey(cursor)?cursor:brackets.containsKey(cursor-1)?cursor-1:-1;
        int match=brackets.getOrDefault(bracket,-1);int[] selection=text.selection();
        for(var line:layout.lines){
            int start=line.start(),end=line.end();
            for(int i=previous;i<start;i++)if(value.charAt(i)=='\n')logical++;previous=start;
            if(withinContentAreaTopBottom(y-4,y+14)){
                if(start==0||value.charAt(start-1)=='\n')g.drawString(font,Integer.toString(logical),getX()+innerPadding(),y,0xFF6A737D,false);
                for(int pos:new int[]{bracket,match})if(pos>=start&&pos<end){int bx=x+layout.x(start,pos);g.fill(bx,y-1,bx+font.width(value.substring(pos,pos+1)),y+9,0x805599BB);}
                // Break runs at icon boundaries; punctuation and subsequent text move with the icon.
                var stops=new TreeSet<Integer>();stops.add(start);stops.add(end);
                for(var token:tokens){if(token.start()>start&&token.start()<end)stops.add(token.start());if(token.end()>start&&token.end()<end)stops.add(token.end());}
                for(var icon:ingredientIcons)if(icon.end()>start&&icon.end()<end)stops.add(icon.end());
                int from=start,tokenIndex=0;
                for(int to:stops){
                    if(to<=from)continue;
                    while(tokenIndex<tokens.size()&&tokens.get(tokenIndex).end()<=from)tokenIndex++;
                    int color=tokenIndex<tokens.size()&&tokens.get(tokenIndex).start()<=from?tokens.get(tokenIndex).kind().color:0xD4D4D4;
                    g.drawString(font,value.substring(from,to),x+layout.x(start,from),y,0xFF000000|color,false);from=to;
                }
                for(var icon:ingredientIcons)if(icon.end()>start&&icon.end()<=end)RecipePreview.drawIngredientIcon(g,icon.ingredient(),x+layout.x(start,icon.end())-17,y-4);
                if(isFocused()&&Util.getMillis()/500%2==0&&layout.row(cursor)==layout.lines.indexOf(line)){int cx=x+layout.x(start,cursor);g.fill(cx,y-1,cx+1,y+9,0xFFFFFFFF);}
                if(selection[0]!=selection[1]&&selection[0]<=end&&selection[1]>=start){
                    int a=x+layout.x(start,Math.max(start,selection[0])),b=x+layout.x(start,Math.min(end,selection[1]));
                    g.fill(RenderType.guiTextHighlight(),a,y,b,y+10,0xFF3366FF);
                }
            }
            y+=18;
        }
    }
}
