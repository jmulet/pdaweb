/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iesapp.fitxes.reports;

import org.iesapp.core.util.Client;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.iesapp.clients.iesdigital.ICoreData;
import org.iesapp.util.StringUtils;

/**
 *
 * @author Josep
 */
public class Utils {

  
    // Cerca qui és el tutor (per aquest any) basat amb la taula de horaris actual
    public static String getTutor(Client client, int nexp)
    {
         String abrev = "";
         String any = StringUtils.anyAcademic_primer();
         Grup grup = getGrup(client, nexp, any);
         String abrevtutor = getTutor(client, grup);
         return abrevtutor;
    }

     public static String getTutor(Client client, Grup grup)
    {
         String abrev = "";
        // String any = StringUtils.anyAcademic();

         String SQL1 = "SELECT DISTINCT prof FROM sig_horaris WHERE curso='" + grup.getKCursInt() + 
                       "' AND grupo='" + grup.getKGrup() + "' AND nivel LIKE '"+grup.getKNivell() + 
                       "%' AND asig LIKE 'TUT%'";

         //System.out.println(SQL1);

       
         try {
            Statement st = client.getIesClient().getMysql().createStatement();
            ResultSet rs1 = client.getIesClient().getMysql().getResultSet(SQL1,st);
            if (rs1 != null && rs1.next()) {
                abrev = StringUtils.noNull(rs1.getString("prof"));
            }
            if(rs1!=null) {
                rs1.close();
                st.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }

         return abrev;
    }


    // Cerca a quin grup pertany un alumne en un curs donat
    public static Grup getGrup(Client client, int nexp, String any2)
    {
         Grup curso = new Grup();
        
         String SQL1 = "Select Estudis, Ensenyament, Grup from `"+ICoreData.core_mysqlDBPrefix+"`.xes_alumne_historic where Exp2='"+
                 nexp+"' and AnyAcademic='"+any2+"'";
         //System.out.println(SQL1);
        
         try {
            Statement st = client.getIesClient().getMysql().createStatement();
            ResultSet rs1 = client.getIesClient().getMysql().getResultSet(SQL1,st);
            if (rs1 != null && rs1.next()) {
                String ensenyament = StringUtils.noNull(rs1.getString("Ensenyament"));
                String estudis = StringUtils.noNull(rs1.getString("Estudis"));
                String grup = StringUtils.noNull(rs1.getString("Grup"));
                if(grup==null || grup.isEmpty()) {
                    grup="X";
                }
                curso = new Grup(Grup.XESTIB, ensenyament, estudis, grup);
             }

                if(rs1!=null)  {
                    rs1.close();
                    st.close();
                }
        } catch (SQLException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }

         return curso;
    }

    public static Image iconToImage(Icon icon) {
          if (icon instanceof ImageIcon) {
              return ((ImageIcon)icon).getImage();
          } else {
              int w = icon.getIconWidth();
              int h = icon.getIconHeight();
              GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
              GraphicsDevice gd = ge.getDefaultScreenDevice();
              GraphicsConfiguration gc = gd.getDefaultConfiguration();
              BufferedImage image = gc.createCompatibleImage(w, h);
              Graphics2D g = image.createGraphics();
              icon.paintIcon(null, g, 0, 0);
              g.dispose();
              return image;
          }
      }


    public static byte[] createByteArray(Image image) {

        try {

        int[] pix = new int[image.getWidth(null) * image.getHeight(null)];
        PixelGrabber pg = new PixelGrabber(image, 0, 0, image.getWidth(null), image.getHeight(null), pix, 0, image.getWidth(null));

        pg.grabPixels();

        byte[] pixels = new byte[image.getWidth(null) * image.getHeight(null)];
        for (int j = 0; j <
        pix.length; j++) {
        pixels[j] = new Integer(pix[j]).byteValue();
        }

        return pixels;
        } catch (InterruptedException ex) {
        return null;
        }
    }

    public static byte[] createByteArray(Icon icon) {

        Image img = iconToImage(icon);
        return createByteArray(img);
    }

}
