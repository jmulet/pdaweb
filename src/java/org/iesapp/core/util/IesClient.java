/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.core.util;

import org.iesapp.fitxes.reports.ReportingClass;
import org.iesapp.clients.iesdigital.ICoreData;
import org.iesapp.clients.iesdigital.professorat.IUser;
import org.iesapp.database.MyDatabase;

/**
 *
 * @author Josep
 * Encapsulates iesdigitalclient in order to be able to extend this class
 */
public class IesClient extends org.iesapp.clients.iesdigital.IesDigitalClient {
    protected final CoreCfg coreCfg;
//    private GuardiasCollection guardiasCollection;
    private ReportingClass reportingClass;
//    private InformesSGD informesSGD;

    public IesClient(CoreCfg coreCfg) {
        super(coreCfg.getMysql(), coreCfg.getSgd(), (ICoreData) coreCfg);
        this.coreCfg = coreCfg;
        //coreCfg.initializeAnuncis((ICfg) this);
    }

    @Override
    public MyDatabase getMysql() {
        return coreCfg.getMysql();
    }

    @Override
    public MyDatabase getSgd() {
        return coreCfg.getSgd();
    }
 
    public CoreCfg getCoreCfg() {
        return coreCfg;
    }

//    @Override
//    public GuardiasCollection getGuardiasCollection() {
//        if(guardiasCollection==null)
//        {
//            guardiasCollection = new GuardiasCollection((ICfg) this);
//        }
//        return guardiasCollection;
//    }
//    

//    
//    @Override
//    public InformesSGD getInformesSGD() {
//        if(informesSGD==null)
//        {
//            informesSGD = new InformesSGD((ICfg) this);
//        }
//        return informesSGD;
//    }

    public void setIuser(String abrev) {
        this.iuser = new IUser(abrev, this);
    }
}
