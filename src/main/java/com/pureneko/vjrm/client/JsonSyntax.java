package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.*;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

/** Lossless tokens: highlighting must never insert formatting codes into recipe text. */
public final class JsonSyntax {
    public enum Kind {
        COMMENT(0x6A9955),FUNCTION(0xDCDCAA),KEY(0x9CDCFE),STRING(0xCE9178),NUMBER(0xB5CEA8),LITERAL(0x569CD6),PUNCTUATION(0xD4D4D4),ERROR(0xF48771);
        public final int color;Kind(int color){this.color=color;}
    }
    public record Token(int start,int end,Kind kind) { }
    private static final Pattern NUMBER=Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
    public static List<Token> tokens(String text){
        var out=new ArrayList<Token>();
        for(int i=0;i<text.length();){
            char c=text.charAt(i);if(Character.isWhitespace(c)){i++;continue;}
            int start=i;
            if(c=='"'){
                boolean valid=true,closed=false;i++;
                while(i<text.length()){
                    c=text.charAt(i++);if(c=='"'){closed=true;break;}
                    if(c<32)valid=false;
                    if(c=='\\'){
                        if(i>=text.length()){valid=false;break;}
                        char escape=text.charAt(i++);
                        if(escape=='u')for(int n=0;n<4;n++){if(i>=text.length()||Character.digit(text.charAt(i),16)<0){valid=false;break;}i++;}
                        else if("\"\\/bfnrt".indexOf(escape)<0)valid=false;
                    }
                }
                int next=i;while(next<text.length()&&Character.isWhitespace(text.charAt(next)))next++;
                out.add(new Token(start,i,!valid||!closed?Kind.ERROR:next<text.length()&&text.charAt(next)==':'?Kind.KEY:Kind.STRING));
            }else if("{}[],:".indexOf(c)>=0){out.add(new Token(i,i+1,Kind.PUNCTUATION));i++;}
            else{
                while(i<text.length()&&!Character.isWhitespace(text.charAt(i))&&"{}[],:\"".indexOf(text.charAt(i))<0)i++;
                String value=text.substring(start,i);
                out.add(new Token(start,i,NUMBER.matcher(value).matches()?Kind.NUMBER:Set.of("true","false","null").contains(value)?Kind.LITERAL:Kind.ERROR));
            }
        }
        return List.copyOf(out);
    }
    public static JsonObject parse(String text){
        var value=parseElement(text);
        if(!value.isJsonObject())throw new JsonSyntaxException("Recipe JSON must be an object");
        return value.getAsJsonObject();
    }
    @SuppressWarnings("deprecation") public static JsonElement parseElement(String text){
        try(var reader=new JsonReader(new StringReader(text))){
            reader.setLenient(false);
            if(reader.peek()==JsonToken.END_DOCUMENT)throw new JsonSyntaxException("JSON value required");
            JsonElement value=Streams.parse(reader);
            if(reader.peek()!=JsonToken.END_DOCUMENT)throw new JsonSyntaxException("Trailing content at "+reader);
            return value;
        }catch(java.io.IOException ex){throw new JsonSyntaxException(ex.getMessage(),ex);}
    }
    public static boolean inString(String text,int cursor){
        boolean inside=false,escape=false;
        for(int i=0;i<cursor;i++){char c=text.charAt(i);if(escape){escape=false;continue;}if(inside&&c=='\\'){escape=true;continue;}if(c=='"')inside=!inside;}
        return inside;
    }
    public static Map<Integer,Integer> brackets(String text,List<Token> tokens){
        var stack=new ArrayDeque<Integer>();var out=new HashMap<Integer,Integer>();
        for(var token:tokens)if(token.kind()==Kind.PUNCTUATION){
            int i=token.start();char c=text.charAt(i);
            if(c=='{'||c=='['||c=='(')stack.push(i);
            else if(c=='}'||c==']'||c==')'){
                if(!stack.isEmpty()&&text.charAt(stack.peek())==(c=='}'?'{':c==']'?'[':'(')){int opening=stack.pop();out.put(opening,i);out.put(i,opening);}else stack.clear();
            }
        }
        return Map.copyOf(out);
    }
    private JsonSyntax(){ }
}
