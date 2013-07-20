/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import org.iesapp.web.cloudws.FacesUtil;

/**
 *
 * @author Josep
 */
public class MyPhaseListener implements PhaseListener {

    @Override
    public void afterPhase(PhaseEvent event) {
        FacesContext fc = event.getFacesContext();

        // Check to see if they are on the login page.
        boolean loginPage =
	  fc.getViewRoot().getViewId().lastIndexOf("login") > -1 ? true : false;
//       System.out.println("Phase listener-"+ fc.getViewRoot().getViewId());
       Object user = FacesUtil.getSessionMapValue("selectedUser");
       if(user!=null && loginPage)   
       {
            try {
                //already validated (must redirect)
                fc.getExternalContext().redirect("/pdaweb/passarllista.jsf");
            } catch (IOException ex) {
                Logger.getLogger(MyPhaseListener.class.getName()).log(Level.SEVERE, null, ex);
            }
       }
           
           
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        //
        
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }
    
}
