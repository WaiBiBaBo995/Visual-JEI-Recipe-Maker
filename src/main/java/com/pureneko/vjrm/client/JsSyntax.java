package com.pureneko.vjrm.client;

import java.util.*;
import com.pureneko.vjrm.client.JsonSyntax.*;

/** JavaScript lexical colors for the KubeJS editor, including comments and escaped quotes. */
public final class JsSyntax {
    private static final Set<String> KEYWORDS=Set.of("true","false","null","const","let","var","function","return","if","else","new","for","of","in","undefined");
    public static List<Token> tokens(String text){
        var out=new ArrayList<Token>();
        for(int i=0;i<text.length();){
            char c=text.charAt(i);if(Character.isWhitespace(c)){i++;continue;}int start=i++;Kind kind=Kind.PUNCTUATION;
            if(c=='/'&&i<text.length()&&(text.charAt(i)=='/'||text.charAt(i)=='*')){
                if(text.charAt(i++)=='/')while(i<text.length()&&text.charAt(i)!='\n')i++;
                else{int end=text.indexOf("*/",i);i=end<0?text.length():end+2;}kind=Kind.COMMENT;
            }else if(c=='\"'||c=='\''||c=='`'){
                boolean closed=false;while(i<text.length()){char next=text.charAt(i++);if(next=='\\'&&i<text.length())i++;else if(next==c){closed=true;break;}}kind=closed?Kind.STRING:Kind.ERROR;
            }else if(Character.isDigit(c)||c=='-'&&i<text.length()&&Character.isDigit(text.charAt(i))){
                while(i<text.length()&&"0123456789.eExXabcdefABCDEF_+-".indexOf(text.charAt(i))>=0)i++;kind=Kind.NUMBER;
            }else if(Character.isJavaIdentifierStart(c)){
                while(i<text.length()&&Character.isJavaIdentifierPart(text.charAt(i)))i++;String word=text.substring(start,i);int next=i;while(next<text.length()&&Character.isWhitespace(text.charAt(next)))next++;
                kind=KEYWORDS.contains(word)?Kind.LITERAL:next<text.length()&&text.charAt(next)=='('?Kind.FUNCTION:Kind.KEY;
            }
            out.add(new Token(start,i,kind));
        }
        return List.copyOf(out);
    }
    public static boolean inString(String text,int cursor){return tokens(text.substring(0,cursor)).stream().anyMatch(t->t.end()==cursor&&t.kind()==Kind.ERROR);}
    private JsSyntax(){}
}
