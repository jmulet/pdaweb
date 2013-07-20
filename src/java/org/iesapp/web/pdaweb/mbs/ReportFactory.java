/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

 
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;

/**
 *
 * @author Josep
 */
 
public class ReportFactory {
    
  
    private String jasperFile;
    private byte fileType;
    private HashMap map;
    private String pathRelativeToWEBINF="/reports/";
    private String fileName;
    private String contentDisposition="inline"; //attachment or inline

    public static final byte PDF=0;
    public static final byte XLS=1;
    public static final byte DOC=2;
    public static final byte HTML=3;
    private Collection<?> dataList;
    
    public ReportFactory(String jasperFile, byte fileType) 
    {
        this.jasperFile = jasperFile;
        this.fileType = fileType;
        String path =FacesContext.getCurrentInstance().getExternalContext().getRealPath(pathRelativeToWEBINF);       
        map = new HashMap();
        map.put("SUBREPORT_DIR", path+"\\");
        fileName = "generatedFile";
    }
    
    
    /**
     * 
     * @param path location of the .jasper file
     * @return
     * @throws JRException 
     */
    public void getGeneratedFile() {
     
        InputStream inputStream = null;
        String mime = "";
        String ext = "";

        try {

            ByteArrayOutputStream Teste = new ByteArrayOutputStream();
            String path = pathRelativeToWEBINF + jasperFile + ".jasper";
            InputStream resourceAsStream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(path.trim());

            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(resourceAsStream);

            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(dataList);
            JasperPrint print = JasperFillManager.fillReport(jasperReport, map, ds);

            JRExporter exporter = null;

            FacesContext fcontext = FacesContext.getCurrentInstance();

            ExternalContext econtext = fcontext.getExternalContext();
            HttpServletResponse response = (HttpServletResponse) econtext.getResponse();
            HttpServletRequest request = (HttpServletRequest) econtext.getRequest();
          
            switch (fileType) {
                case (PDF):
                    exporter = new net.sf.jasperreports.engine.export.JRPdfExporter();
                    mime = "application/pdf";
                    ext = "pdf";
         
                    break;
                case (XLS):
                    exporter = new net.sf.jasperreports.engine.export.JRXlsExporter();
                    mime = "application/xls";
                    ext = "xls";
                    break;
                case (DOC):
                    exporter = new net.sf.jasperreports.engine.export.JRRtfExporter();
                    mime = "application/doc";
                    ext = "doc";
                    break;
                case (HTML):
                    exporter = new JRHtmlExporter();
                    mime = "application/html";
                    ext = "html";
                   
                    exporter.setParameter(JRExporterParameter.OUTPUT_WRITER,  response.getWriter());

                    exporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, request.getContextPath() + "/jasperreports?image=");
                    break;
            }

                    response.setContentType(mime);
                    response.setHeader("Content-disposition", contentDisposition+";filename=\""
 					+ fileName+"."+ext + "\"");
                    
                    exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, response.getOutputStream());

                    exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
                    exporter.exportReport();

                    inputStream = new ByteArrayInputStream(Teste.toByteArray());

                    fcontext.responseComplete();
            }  catch (JRException ex) {
            System.out.println("Exception " + ex);
        } catch (IOException ex) {
            Logger.getLogger(ReportFactory.class.getName()).log(Level.SEVERE, null, ex);
        }


        //return new DefaultStreamedContent(inputStream, mime, fileName + "." + ext);
    }

    public byte getFileType() {
        return fileType;
    }

    public void setFileType(byte fileType) {
        this.fileType = fileType;
    }

    public HashMap getMap() {
        return map;
    }

    public void setMap(HashMap map) {
        this.map = map;
    }

    public String getJasperFile() {
        return jasperFile;
    }

    public void setJasperFile(String jasperFile) {
        this.jasperFile = jasperFile;
    }

    public String getPathRelativeToWEBINF() {
        return pathRelativeToWEBINF;
    }

    public void setPathRelativeToWEBINF(String pathRelativeToWEBINF) {
        this.pathRelativeToWEBINF = pathRelativeToWEBINF;
    }

    public Collection<?> getDataList() {
        return dataList;
    }

    public void setDataList(Collection<?> dataList) {
        this.dataList = dataList;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }
}
