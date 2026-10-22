package com.pureneko.vjrm.client;

import net.minecraft.client.gui.Font;
import java.util.*;

/** Wrapping, caret placement and hit testing share the same virtual icon widths. */
final class AnnotatedTextLayout {
    static final int LINE_HEIGHT=18,ICON_WIDTH=18;
    record Line(int start,int end) { }
    final String value;
    final List<Line> lines;
    private final Font font;
    private final Set<Integer> icons;
    AnnotatedTextLayout(Font font,String value,int width,List<IngredientAnnotations.Icon> annotations){
        this.font=font;this.value=value;icons=new HashSet<>();for(var icon:annotations)icons.add(icon.end());
        var rows=new ArrayList<Line>();int start=0,x=0;
        for(int at=0;at<value.length();){
            int next=at+Character.charCount(value.codePointAt(at));
            if(value.charAt(at)=='\n'){rows.add(new Line(start,at));start=next;x=0;at=next;continue;}
            int advance=font.width(value.substring(at,next))+(icons.contains(next)?ICON_WIDTH:0);
            if(x+advance>Math.max(ICON_WIDTH,width)&&at>start){rows.add(new Line(start,at));start=at;x=0;}
            x+=advance;at=next;
        }
        rows.add(new Line(start,value.length()));lines=List.copyOf(rows);
    }
    int x(int start,int end){
        int width=font.width(value.substring(start,end));for(int icon:icons)if(icon>start&&icon<=end)width+=ICON_WIDTH;return width;
    }
    int row(int cursor){for(int i=lines.size()-1;i>=0;i--)if(cursor>=lines.get(i).start)return i;return 0;}
    int hit(int row,double x){
        var line=lines.get(Math.max(0,Math.min(lines.size()-1,row)));int previous=0;
        for(int at=line.start;at<line.end;){int next=at+Character.charCount(value.codePointAt(at)),right=x(line.start,next);if(x<(previous+right)/2.0)return at;previous=right;at=next;}
        return line.end;
    }
}
