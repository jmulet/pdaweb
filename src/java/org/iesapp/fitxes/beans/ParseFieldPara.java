/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.fitxes.beans;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.iesapp.database.MyDatabase;
import org.iesapp.util.StringUtils;

/**
 *
 * @author Josep
 */
public class ParseFieldPara {
  
    private String txt;

    public ParseFieldPara(String txt) {
        this.txt = txt;
    }

    public String getText(MyDatabase mysql) {
        StringBuilder out = new StringBuilder(txt);

        if (txt.startsWith("[")) {
            out = new StringBuilder();
            String out2 = StringUtils.AfterFirst(txt, "[");
            out2 = StringUtils.BeforeLast(out2, "]");
            ArrayList<String> parsed = StringUtils.parseStringToArray(out2, ",", StringUtils.CASE_UPPER);
            out2 = null;
            for (int i = 0; i < parsed.size(); i++) {
                //S'ocupa de cercar el nom del professor assignat a l'abrev 
                String SQL1 = "SELECT nombre FROM sig_professorat WHERE abrev='"+parsed.get(i)+"'";
                
                try {
                    Statement st = mysql.createStatement();
                    ResultSet rs = mysql.getResultSet2(SQL1,st);
                    if(rs!=null && rs.next())
                    {
                        out.append(rs.getString(1)).append(";\n");
                    }
                    if(rs!=null) {
                        rs.close();
                        st.close();
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(ParseFieldPara.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return out.toString();

    }
        
}
 
