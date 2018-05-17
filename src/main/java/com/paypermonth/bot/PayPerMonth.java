package com.paypermonth.bot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;


public class PayPerMonth extends TelegramLongPollingBot {
    Database db = null;
    ArrayList<Hold> holdRequests = new ArrayList();
    Logger logger = Logger.getLogger("PaypermonthBotLog");
    String[] mesi = {"Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno","Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre","Tutto"};
    String sorryMessage="Scusa, non ho capito.\nUsa /help";

    @Override
    public synchronized void onUpdateReceived(Update update) {

        try {
            db = new Database();
        } catch (Exception edb) { logger.info("Errore di dichiarazione Database");}

        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            // Set variables
            String chat_id = Long.toString(update.getMessage().getChatId());
            String gettedMessage=update.getMessage().getText(), nome, mese, anno, opcode;


            if(gettedMessage.trim().equals("/start")||gettedMessage.trim().equals("/start@PaypermonthBot"))
            {
                String username=update.getMessage().getFrom().getUserName();
                deliverMessage(chat_id,getStartMessage(username));
            }
            else if(gettedMessage.trim().equals("/help")||gettedMessage.trim().equals("/help@PaypermonthBot"))
                deliverMessage(chat_id,getHelpMessage());
            else if(gettedMessage.contains("/pagato")||gettedMessage.contains("/pagato@PaypermonthBot")||
                    gettedMessage.contains("/storno")||gettedMessage.contains("/storno@PaypermonthBot"))
            {
                String type=gettedMessage;

                /* Se le richieste non contengono nome e mese crea un Hold */
                if(     type.trim().equals("/pagato")||type.trim().equals("/storno")||
                        type.trim().equals("/pagato@PaypermonthBot")||type.trim().equals("/storno@PaypermonthBot")){

                    clearHolding(chat_id);

                    holdRequests.add(new Hold(chat_id,type.substring(1,7)));

                    /* Triggera sui primi argomenti (mese e anno) */
                    deliverMessage(chat_id,"\uD83D\uDCC5 Specifica il mese e l'anno:");

                }
                else {
                    /* Altrimenti i campi sono stati settati ed esegui le query */
                    try{

                        opcode = gettedMessage.split("[ /]+")[1];
                        mese = gettedMessage.split("[ /]+")[2];
                        anno = gettedMessage.split("[ /]+")[3];
                        int numNomi=gettedMessage.split("[ ,/]+").length;

                        if ((anno.length() != 4) || !(anno.chars().allMatch(Character::isDigit)))
                            throw new Exception();


                        for(int i = 4; i<numNomi; i++) {
                            nome = gettedMessage.split("[ ,/]+")[i];

                            if(opcode.equals("pagato") || opcode.equals("pagato@PaypermonthBot"))
                                deliverMessage(chat_id,this.paga(chat_id,nome, getMeseNumber(mese), anno));
                            if(opcode.equals("storno") || opcode.equals("storno@PaypermonthBot"))
                                deliverMessage(chat_id,this.storna(chat_id,nome,getMeseNumber(mese), anno));
                        }


                    }catch(Exception e){
                        clearHolding(chat_id);
                        e.printStackTrace();
                        deliverMessage(chat_id,sorryMessage);
                    }

                }
            }
            else if(gettedMessage.contains("/check")||gettedMessage.contains("/check@PaypermonthBot"))
            {
                clearHolding(chat_id);

                if(gettedMessage.trim().equals("/check") || gettedMessage.trim().equals("/check@PaypermonthBot")){
                    holdRequests.add(new Hold(chat_id,"check"));

                    /* Triggera l'hold sul primo argomento (anno) */
                    deliverMessage(chat_id,"\uD83D\uDCC5 Specifica l'anno:");
                }
                else{
                    try{
                    anno = gettedMessage.split(" +")[1];

                        if ((anno.length() == 4) && (anno.chars().allMatch(Character::isDigit)))
                           deliverMessage(chat_id,this.status(chat_id, anno));
                        else throw new Exception();

                    }catch(Exception e){
                        clearHolding(chat_id);
                        e.printStackTrace();
                        deliverMessage(chat_id,sorryMessage);
                    }
                }

            }

            else {
                /* Sta arrivando un messaggio da controllare */
                /* Imposto nel caso siano degli arg di opcode  */
                int indexOfHold;
                String message;
                Hold tmp;

                    /* Se in holdRequest è settato qualcosa */
                    if(     (indexOfHold=holdRequests.indexOf(new Hold(chat_id,"pagato")))>=0 ||
                            (indexOfHold=holdRequests.indexOf(new Hold(chat_id,"storno")))>=0)
                    {


                        /* Trovalo nella lista quello in hold */
                        tmp=holdRequests.get(indexOfHold);

                        /* Decrementa il numero di arg cmq sia */
                        tmp.decrease();

                        if(tmp.getRemainArgs()==1){
                                /* Hold esiste e nome già settato */
                                /* Decidi se il mese è numerico o no */
                                    mese = gettedMessage.split("[ /]+")[0];
                                    anno = gettedMessage.split("[ /]+")[1];

                            try{
                                 if ((anno.length() != 4) && !(anno.chars().allMatch(Character::isDigit)))
                                  throw new Exception();

                                tmp.setMese(getMeseNumber(mese));

                                tmp.setAnno(anno);

                                deliverMessage(chat_id,"\uD83D\uDC65 Specifica i nomi:");

                            }catch(Exception e){
                                            clearHolding(chat_id);
                                            e.printStackTrace();
                                            deliverMessage(chat_id,sorryMessage);
                                }
                        }
                        else {

                            /* Mi hanno mandato i nomi */
                            try{

                                tmp.setNomi(gettedMessage);

                                    String[] numNomi=tmp.getNomi().split("[ ,]+");


                                    for(int i = 0; i<numNomi.length; i++) {

                                        if(tmp.getType().equals("pagato"))
                                            deliverMessage(chat_id,this.paga(chat_id,numNomi[i], tmp.getMese(), tmp.getAnno()));
                                        if(tmp.getType().equals("storno"))
                                            deliverMessage(chat_id,this.storna(chat_id,numNomi[i],tmp.getMese(), tmp.getAnno()));
                                    }



                                holdRequests.remove(indexOfHold);
                            }catch (Exception e){
                                clearHolding(chat_id);
                                e.printStackTrace();
                                deliverMessage(chat_id,sorryMessage);
                            }

                        }

                    }else if ((indexOfHold=holdRequests.indexOf(new Hold(chat_id,"check")))>=0) {
                        /* Trovalo nella lista quello in hold */
                        tmp = holdRequests.get(indexOfHold);

                        /* Decrementa il numero di arg cmq sia */
                        tmp.decrease();

                        try {
                            if (gettedMessage.trim().length() == 4 && (gettedMessage.trim().chars().allMatch(Character::isDigit)))
                            {
                                tmp.setAnno(gettedMessage.trim());
                                holdRequests.remove(indexOfHold);
                                deliverMessage(chat_id,this.status(tmp.getChatid(), tmp.getAnno()));
                            }
                            else throw new Exception();

                        }catch (Exception e){
                            clearHolding(chat_id);
                            e.printStackTrace();
                            deliverMessage(chat_id,sorryMessage);
                        }

                    }
                }
            }

            /* Chiudi la connessione solo se esiste e non era già chiusa */
            try {
                if((db != null) || (!db.conn.isClosed()))
                    db.closeConnection();
            } catch (SQLException e){ logger.info("Errore di database"); }
        }


    @Override
    public String getBotUsername() {
        // Return bot username
        return "Pay per month";
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        return System.getenv("bot_token");
    }

    public String getPrintableMese(String translate_mese) throws IndexOutOfBoundsException{

            if(translate_mese.equals("13"))
                return "tutto l'anno";

            for(int i=0; i<12; i++)
                if(translate_mese.trim().toUpperCase().equals(mesi[i].toUpperCase()))
                    return mesi[i];
                else if(Integer.parseInt(translate_mese)<=12 && Integer.parseInt(translate_mese)>=1)
                    return mesi[Integer.parseInt(translate_mese)-1];

             throw new IndexOutOfBoundsException();
    }


    public String getMeseNumber(String mese) throws Exception{


	for(int i=0; i<13; i++)
            if(mese.trim().toUpperCase().equals(mesi[i].toUpperCase()))
                return(Integer.toString(i+1));


        if (Integer.parseInt(mese) >= 1 && Integer.parseInt(mese) <= 12)
            return(Integer.toString(Integer.parseInt(mese)));



        throw new Exception();

    }


        public String paga(String chat_id, String nome, String mese, String anno){
        try {

            int start= Integer.parseInt(mese);
            int end=Integer.parseInt(mese);

            if(mese.equals("13"))
            {
                start = 1;
                end = 12;
            }


            for(int i=start;i<=end; i++)
            {
                /* Prepara ed esegui la query */
                PreparedStatement prep_sql=null;
                String query="INSERT INTO payments (chatid, nome, mese, anno) VALUES ( ? , ? , ? , ? );";
                prep_sql = db.conn.prepareStatement(query);
                prep_sql.setString(1,chat_id);
                prep_sql.setString(2,nome);
                prep_sql.setString(3,Integer.toString(i));
                prep_sql.setString(4,anno);

                db.execQuery(prep_sql, "INSERT");

            }

            return("\u2705 Fantastico *" + nome + "*,\n" +
                    "Hai pagato anche per *" + this.getPrintableMese(mese) + " " + anno + "*");

        }catch(Exception epagato){
            return("Errore! Ti ricordo che il comando è:\n/pagato mese anno nome1, nome2, ...");
        }
    }

    public String storna(String chat_id, String nome, String mese, String anno) {
        try {

            int start= Integer.parseInt(mese);
            int end=Integer.parseInt(mese);

            if(mese.equals("13"))
            {
                start = 1;
                end = 12;
            }

            for(int i=start;i<=end; i++) {

                /* Prepara ed esegui la query */
                PreparedStatement prep_sql = null;
                String query = "DELETE FROM payments WHERE chatid = ?  AND nome = ?  AND mese = ? AND anno = ?;";
                prep_sql = db.conn.prepareStatement(query);
                prep_sql.setString(1, chat_id);
                prep_sql.setString(2, nome);
                prep_sql.setString(3, Integer.toString(i));
                prep_sql.setString(4, anno);

                db.execQuery(prep_sql, "DELETE");

            }
            return("\u274E Il pagamento di *" + nome + "* per *" +
                    this.getPrintableMese(mese) +  " " + anno + "* è stato rimosso correttamente.");


        } catch (Exception epagato) {
            return("Errore! Ti ricordo che il comando è:\n/storno mese anno nome1, nome2, ...");
        }
    }

    public String status(String chat_id, String anno){

        String returnString="", meseString="", header,columnValue;
        ResultSet rs;
        try {

            returnString="Nessun pagamento per l'anno " + anno;
            for(int i=1; i<=12; i++){

                /* Prepara ed esegui la query */
                PreparedStatement prep_sql=null;
                String query="SELECT * FROM payments WHERE chatid = ? AND mese = ? AND anno = ? ORDER BY nome ASC;";
                prep_sql = db.conn.prepareStatement(query);
                prep_sql.setString(1,chat_id);
                prep_sql.setString(2,Integer.toString(i));
                prep_sql.setString(3,anno);

                rs=db.execQuery(prep_sql, "SELECT");

                header = "*" + getPrintableMese(Integer.toString(i)) + "*";
                int contatore=0;
                while (rs.next()) {
                    columnValue = rs.getString(3);
                    meseString+=("  ◦ " + columnValue + " pagato ✓\n");
                    /* stampa il nome della colonna rsmd.getColumnName(i) */
                    contatore++;
                }

                if(contatore==0)
                    header="";
                else returnString+=header + " *(" + contatore + ")*\n"  + meseString + "\n";

                contatore=0;
                meseString="";

            }


            if(returnString.length()>32)
                returnString=returnString.substring(32);

            return(returnString);

        }catch(Exception estatus){
            return("Errore! Ti ricordo che il comando è:\n/check anno");
        }
    }

    public String getHelpMessage(){
        return("*Inizia la conversazione*\n » /start \n" +
                "*Apri il manuale*\n » /help\n" +
                "*Registra un pagamento*\n » /pagato mese anno nome1, nome2, ... \n" +
                "*Annulla un pagamento*\n » /storno mese anno nome1, nome2, ... \n" +
                "*Stato dei pagamenti*\n » /check anno\n" +
                "\n► Decidi tu come scrivere i mesi, se per nome o per numero" +
                "\n► Puoi gestire pagamenti e storni di tutto l'anno scrivendo al posto del *mese* la parola '*tutto*'" +
                "\n\nPer maggiori info e richieste scrivi a Edoardo!\n\uD83C\uDF10 edoardovignati.it");

    }

    public String getStartMessage (String username){
        return("Ciao *" + username + "*! \n" +
                "Con questo bot potrai tenere traccia di tutti i pagamenti mensili" +
                " e abbonamenti ad account condivisi come Netflix, Spotify etc... \n" +
                "Usa /help per maggiori info.\n");
    }

    public void clearHolding(String chat_id){
        while(holdRequests.remove(new Hold(chat_id,"pagato"))){};
        while(holdRequests.remove(new Hold(chat_id,"storno"))){};
        while(holdRequests.remove(new Hold(chat_id,"check"))){};
    }

    public void deliverMessage(String chatid,String messagetext){

        SendMessage message = new SendMessage() // Create a message object object
                .enableMarkdown(true)
                .setChatId(Long.valueOf(chatid))
                .setText(messagetext);

        try {
            execute(message); // Sending our message object to user
        } catch (TelegramApiException e) { logger.info("Telegram error"); }

    }
}
