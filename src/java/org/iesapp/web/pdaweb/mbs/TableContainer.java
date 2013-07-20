/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

import java.util.ArrayList;

/**
 *
 * @author Josep
 */
public class TableContainer {
    
    private ArrayList<String> columnNames;
    private ArrayList<Object> columns;

    public TableContainer()
    {
        columnNames = new ArrayList<String>();
        columns = new ArrayList<Object>();
        
        columnNames.add("Col1");
         columnNames.add("Col2");
          columnNames.add("Col3");
          
          columns.add("?");
          columns.add("?");
          columns.add("?");
        
    }
    
    public ArrayList<Object> getColumns() {
        return columns;
    }

    public void setColumns(ArrayList<Object> columns) {
        this.columns = columns;
    }

    public ArrayList<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(ArrayList<String> columnNames) {
        this.columnNames = columnNames;
    }
    
}
