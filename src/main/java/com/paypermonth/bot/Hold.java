package com.paypermonth.bot;

import java.util.Objects;

public class Hold {

    String chatid,mese,anno;
    int remainArgs;
    boolean onhold;
    String type, nome;

    public Hold(String c, String t){

        chatid=c;
        type=t;
        if(t.equals("pagato") || t.equals("storno"))
            remainArgs=2;
        else remainArgs=1; /* se il type=check */

    }

    public String getChatid() {
        return chatid;
    }
    public int decrease(){
       remainArgs--;
       return this.remainArgs;
    }
    public int getRemainArgs(){
        return remainArgs;
    }
    public void setMese(String mese) {
        this.mese = mese;
    }
    public String getMese() {
        return mese;
    }
    public String getType() {
        return type;
    }
    public void setNomi(String nome) {
        this.nome = nome;
    }
    public String getNomi() {
        return nome;
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        Hold chat = (Hold) o;
        // field comparison
        return Objects.equals(chatid, chat.chatid)
                && Objects.equals(type, chat.type);
    }


    public void setAnno(String s) {
        anno=s;
    }
    public String getAnno(){
        return anno;
    }
}
