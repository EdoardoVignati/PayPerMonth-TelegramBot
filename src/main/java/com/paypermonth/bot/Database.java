package com.paypermonth.bot;

import java.sql.*;

public class Database {

    Connection conn; /* Connessione al db */
    Statement st; /* Creazione statement */
    ResultSet rs; /* Risultato */


    public Database(){
        try {
            conn = Database.getConnection();
        }catch(Exception econn){econn.printStackTrace();}
        st = null;
        rs = null;
    }


    protected synchronized static Connection getConnection() throws SQLException {
        //Class.forName("org.postgresql.Driver").newInstance();
        String dbUrl = System.getenv("JDBC_DATABASE_URL");
        return DriverManager.getConnection(dbUrl);
    }


    public synchronized ResultSet execQuery(PreparedStatement query, String type) throws SQLException {

        if (type.equals("SELECT"))
            rs = query.executeQuery();
        else
           query.executeUpdate();
        return rs;
    }

    public synchronized void closeConnection() throws SQLException {
        if((conn!=null) & (!conn.isClosed()))
            conn.close();
    }

}
