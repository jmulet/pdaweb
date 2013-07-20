/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.core.util;

import org.iesapp.fitxes.reports.ReportingClass;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.iesapp.clients.sgd7.SgdClient;
import org.iesapp.clients.sgd7.base.SgdBase;

/**
 *
 * @author Josep
 */
public class Client {
    private SgdClient sgdClient;
    private final CoreCfg coreCfg;
    private IesClient iesClient;
    private final String remoteAddr;
    private final String remoteHost;
    private ReportingClass reportingClass;
    
   public Client()
   {
      HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
      remoteAddr = httpServletRequest.getRemoteAddr();
      remoteHost = httpServletRequest.getRemoteHost();
      
     //Crea les connexions a les bases de dades
      coreCfg = new CoreCfg();
      coreCfg.remoteAddr = remoteAddr;
      coreCfg.remoteHost = remoteHost;
   }
   
   public SgdClient getSgdClient()
   {
       if(sgdClient==null)
       {
           // public SgdClient(MyDatabase mysql, MyDatabase sgd, int anyAcademic, String currentDBPrefix, String configDB, String host, String hostIp)
           String currentDBPrefix = (String) CoreCfg.configTableMap.get("sgdDBPrefix");
           sgdClient = new SgdClient(coreCfg.getMysql(), coreCfg.getSgd(), coreCfg.anyAcademic, currentDBPrefix, "config", remoteAddr, remoteHost);
       }
       return sgdClient;
   }
   
   public IesClient getIesClient()
   {
       if(iesClient==null)
       {
           iesClient = new IesClient(coreCfg);
       }
       return iesClient;
   }
   
   
    public ReportingClass getReportingClass() {
        if(reportingClass==null)
        {
            reportingClass = new ReportingClass(this);
        }
        return reportingClass;
    }
   
   public void close()
   {
       coreCfg.close();
       
       if (SgdBase.getSgd() != null) {
           SgdBase.getSgd().close();
       }
       if (SgdBase.getMysql() != null) {
           SgdBase.getMysql().close();
       }
       
       coreCfg.mysql = null;
       coreCfg.sgd = null;
       sgdClient = null;
       iesClient = null; 
       reportingClass = null;
   }
}
