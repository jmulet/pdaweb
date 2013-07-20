/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.cloudws;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.iesapp.clients.sgd7.profesores.Profesores;
import org.iesapp.util.StringUtils;

@Path("/cloud")
//@ApplicationScoped
public class GenericResource extends GenericResourceUtil {

    private final ServletContext context;
    private Wrapper wrapper;

   
    /**
     * Creates a new instance of GenericResource
     */
    public GenericResource(@Context ServletContext context) {
        this.context = context;
        wrapper = (Wrapper) context.getAttribute("wrapper");
        
        if (wrapper == null) {
            wrapper = new Wrapper(context);
            context.setAttribute("wrapper", wrapper);
        }
  
    }

    /**
     * Retrieves representation of an instance of cloudws.GenericResource
     * @return an instance of java.lang.String
     */
    @GET
    @Path("/authenticate")
    public Response getSessionId(@QueryParam("user") String user, @QueryParam("pwd") String pwd) {
        Response response;
        //TODO return proper representation object
        String sessionId = "";
        //Check connection first
        
        System.out.println("authenticate:");
        
        if(wrapper.getSgd().isClosed())
        {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).type(MediaType.TEXT_PLAIN).entity("Mysql connection unavailable").build();
        }
        
        String validPwd = null;
        
        String SQL1 = " SELECT DISTINCT "
                + "prof.nombre,   "
                + "conf.usuario, "
                + "prof.id, "
                + "prof.claveUp,  "
                + "prof.fechaBloqueoMyClass,  "
                + "prof.enviarMsgUp,  "              
                + "prof.idUnidadesPersonales  "            
                + "FROM config.usuarios AS conf  "
                + "INNER JOIN "
                + "curso"+wrapper.getAnyAcademic()+".profesores AS prof "
                + "ON prof.id=conf.idProfesores AND prof.claveUp=conf.clave "
                + " WHERE conf.usuario='"+user+"' ";
         try {
            Statement st = wrapper.getSgd().createStatement();
            ResultSet rs1 = wrapper.getSgd().getResultSet(SQL1,st);
            if(rs1!=null && rs1.next())
            {
                validPwd = StringUtils.noNull(rs1.getString("claveUp"));
            }
            if(rs1!=null) {
                 rs1.close();
                 st.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Profesores.class.getName()).log(Level.SEVERE, null, ex);
        }
        
     
        
        if(validPwd!=null)
        {
            //user has been found at least
            if(validPwd.equals(pwd))
            {
                //pwd match --> validation success
                sessionId = UUID.randomUUID().toString();
                wrapper.getSessions().put(sessionId, new Session(sessionId, user, pwd));
                checkIntegrity(wrapper.getFileHostingDir(), user, wrapper.getAnyAcademic());
                HashMap<String,String> map = new HashMap<String,String>();
               
                map.put("id", sessionId);
                map.put("path", wrapper.getFileHostingDir());
                map.put("home", wrapper.getFileHostingDir()+user+File.separator);
                map.put("uploadlimit", wrapper.getFileHostingLimitBytes()+"");
                Gson gson = new Gson();
                String toJson = gson.toJson(map);
                
                response = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(toJson).build();
  
            }
            else
            {
                response = Response.status(Response.Status.UNAUTHORIZED).type(MediaType.TEXT_PLAIN).entity("Invalid p").build();
            }
        }
        else
        {
                response = Response.status(Response.Status.UNAUTHORIZED).type(MediaType.TEXT_PLAIN).entity("Invalid user").build();
        }
        
        
        return response;
    }
   

    @GET
    @Path("/dir")
    public Response dirContents(@QueryParam("sessionId") String id, @QueryParam("path") String relativePath) {
       
        Response response;
        
        ArrayList<FileInfo> llistFds = new ArrayList<FileInfo>();
        if (wrapper.getSessions().containsKey(id)) {
            if (relativePath == null) {
                relativePath = "";
            }
            File f = new File(wrapper.getFileHostingDir() + relativePath);
            File[] list = f.listFiles(wrapper.getFileFilter());

            for (File file : list) {
                llistFds.add(new FileInfo(wrapper.getFileHostingDir(), wrapper.getFileHostingDownloadPath(), file));
            }

            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<FileInfo>>() {}.getType();
            String toJson = gson.toJson(llistFds, type);

            response = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(toJson).build();

        } else {
            response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
        }

        return response;
    }

    @GET
    @Path("/tree")
    public Response dirTree(@QueryParam("sessionId") String id, @QueryParam("path") String relativePath, @QueryParam("onlyDir") String sonlyDir) {
       
        Response response;
        final boolean onlyDir = sonlyDir!=null && sonlyDir.equalsIgnoreCase("yes");
        
       if (wrapper.getSessions().containsKey(id)) {
            if (relativePath == null) {
                relativePath = "";
            }
         
           FileFilter fileFilter2 = new FileFilter() {
               @Override
               public boolean accept(File pathname) {
                   return (!(onlyDir) || (onlyDir && pathname.isDirectory())) && !pathname.getName().startsWith(".");
               }
           };
           
            File f = new File(wrapper.getFileHostingDir() + relativePath);
            File[] list = f.listFiles(fileFilter2);
           
            FileInfo fd = new FileInfo(wrapper.getFileHostingDir(), wrapper.getFileHostingDownloadPath(), f);
            Gson gson = new Gson();
            JsonObject obj = new JsonObject();
            JsonArray children = new JsonArray();
            obj.add("children", children);
            obj.add("value", gson.toJsonTree(fd, FileInfo.class));
  
           
           for (File file : list) {
               FileInfo fd2 = new FileInfo(wrapper.getFileHostingDir(), wrapper.getFileHostingDownloadPath(), file);
               JsonObject obj2 = new JsonObject();
               JsonArray children2 = new JsonArray();
               obj2.add("children", children2);
               obj2.add("value", gson.toJsonTree(fd2, FileInfo.class));
               children.add(obj2);
               if (file.isDirectory()) {
                   //Recursive
                   addFilesRecursive(children2, wrapper.getFileHostingDir(), wrapper.getFileHostingDownloadPath(), file, fileFilter2);
               }
           }
            
            //No funciona be
            String toJson = obj.toString();

            response = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(toJson).build();

        } else {
            response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
        }

        return response;
    }
    
    
    private void addFilesRecursive(JsonArray children, String var1, String var2, File f, FileFilter fileFilter) {
      File[] listFiles = f.listFiles(fileFilter);
      for(File file: listFiles)
      {
               Gson gson = new Gson();
               FileInfo fd2 = new FileInfo(wrapper.getFileHostingDir(), wrapper.getFileHostingDownloadPath(), file);
               JsonObject obj2 = new JsonObject();
               JsonArray children2 = new JsonArray();
               obj2.add("children", children2);
               obj2.add("value", gson.toJsonTree(fd2, FileInfo.class));
               children.add(obj2);
               if (file.isDirectory()) {
                   //Recursive
                   addFilesRecursive(children2, wrapper.getFileHostingDir(), wrapper.getFileHostingDownloadPath(), file, fileFilter);
               }
      }
    }

    @GET
    @Path("/delete")
    public Response delete(@QueryParam("sessionId") String id, @QueryParam("path") String dir) {
        
       Response response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
       
        dir = dir==null?"":dir;
       
       if(wrapper.getSessions().containsKey(id))
        {
            Session session = wrapper.getSessions().get(id);
            String user = session.getUser();
            
            //Make sure you reach a deletable directory
            if(dir.startsWith(user) && !dir.equals(user) && 
               !dir.equalsIgnoreCase(user+File.separator+"Public") &&
               !dir.equalsIgnoreCase(user+File.separator+".attachments"))
                
            {
                File f = new File(wrapper.getFileHostingDir()+dir);
                if(f.exists())
                {
                    if(f.isDirectory())
                    {
                        try {
                            FileUtils.deleteDirectory(f);
                            response = Response.status(Response.Status.OK).entity("Directory deleted").type(MediaType.TEXT_PLAIN).build();
    
                        } catch (IOException ex) {
                            Logger.getLogger(GenericResource.class.getName()).log(Level.SEVERE, null, ex);
                        }
                     
                    }
                    else
                    {
                        f.delete();
                        response = Response.status(Response.Status.OK).entity("File deleted").type(MediaType.TEXT_PLAIN).build();
    
                    }
                    
                }
                else
                {
                    response = Response.status(Response.Status.NOT_FOUND).entity("File does not exist").type(MediaType.TEXT_PLAIN).build();  
                }
                
            }
            else
            {
                response = Response.status(Response.Status.FORBIDDEN).entity("Read only").type(MediaType.TEXT_PLAIN).build();
            }
        }
        else
       {
          response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
       }
         
       return response;
    }
    
    
    @GET
    @Path("/mkdir")
    public Response mkdir(@QueryParam("sessionId") String id,@QueryParam("path") String dir) {
        
        Response response;
       
        if(wrapper.getSessions().containsKey(id))
        {
            Session session = wrapper.getSessions().get(id);
            if(dir.startsWith(session.getUser()))
            {
                File f = new File(wrapper.getFileHostingDir()+dir);
                if(f.exists())
                {
                    int i = 1;
                    while(f.exists())
                    {
                        f = new File(wrapper.getFileHostingDir()+dir+"_"+i);
                        i += 1;
                    }
                 }
                
                
                 f.mkdirs();
                
                
                response = Response.status(Response.Status.OK).entity("Directory created").type(MediaType.TEXT_PLAIN).build();
                
            }
            else
            {
                   response = Response.status(Response.Status.FORBIDDEN).entity("Read-only directory").type(MediaType.TEXT_PLAIN).build();
            }
        }
        else
        {
             response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
        }
        return response;
    }
    
    @GET
    @Path("/copy")
    public Response copy(@QueryParam("sessionId") String id,
                         @QueryParam("from_path") String from_path,
                         @QueryParam("to_path") String to_path) {
        
        Response response;
       
        System.out.println("method copy "+id+" "+from_path+" "+ to_path);
        
        if(wrapper.getSessions().containsKey(id))
        {
            Session session = wrapper.getSessions().get(id);
            if(to_path.startsWith(session.getUser()))
            {
                File f = new File(wrapper.getFileHostingDir()+from_path);
                File f2 = new File(wrapper.getFileHostingDir()+to_path);
                
                if(f.exists())
                {
                    //Determine what is about to be copied
                    if(f.isFile())
                    {
                        //Check if f2 contains a file with the same name
                        File f3 = new File(f2.getAbsolutePath()+File.separator+f.getName());
                        int i = 1;
                        while(f3.exists())
                        {
                            String file = f.getName();
                            String fileext = StringUtils.AfterLast(file, ".");
                            String filename = StringUtils.BeforeLast(file, ".");
                            f3 = new File(f2.getAbsolutePath()+File.separator+filename+"_"+i+"."+fileext);
                            i +=1;
                        }
                        try {
                            org.apache.commons.io.FileUtils.copyFile(f, f3);
                            System.out.println("copied file");
                            response = Response.status(Response.Status.OK).entity("File copied").type(MediaType.TEXT_PLAIN).build();

                        } catch (IOException ex) {
                            response = Response.status(Response.Status.CONFLICT).entity("Error copying file : "+ex).type(MediaType.TEXT_PLAIN).build();
                            Logger.getLogger(GenericResource.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else
                    {
                       //Check if f2 contains a directory with the same name
                        File f3 = new File(f2.getAbsolutePath()+File.separator+f.getName());
                        int i = 1;
                        while(f3.exists())
                        {
                            f3 = new File(f2.getAbsolutePath()+File.separator+f.getName()+"_"+i);
                            i += 1;
                        }
                      try {
                            org.apache.commons.io.FileUtils.copyDirectory(f, f3);
                            System.out.println("copied directory");
                            response = Response.status(Response.Status.OK).entity("Directory copied").type(MediaType.TEXT_PLAIN).build();

                        } catch (IOException ex) {
                            response = Response.status(Response.Status.CONFLICT).entity("Error copying directory : "+ex).type(MediaType.TEXT_PLAIN).build();
                            Logger.getLogger(GenericResource.class.getName()).log(Level.SEVERE, null, ex);
                        }   
                    }
                   
                }
                else
                {
                     response = Response.status(Response.Status.NOT_FOUND).entity("from file doesn't exist").type(MediaType.TEXT_PLAIN).build();
                }
            }
            else
            {
                   response = Response.status(Response.Status.FORBIDDEN).entity("Read-only directory").type(MediaType.TEXT_PLAIN).build();
            }
        }
        else
        {
             response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
        }
        return response;
    }
   
    
    //
    // to path must be a directory
    
    @GET
    @Path("/move")
    public Response move(@QueryParam("sessionId") String id,
                         @QueryParam("from_path") String from_path,
                         @QueryParam("to_path") String to_path) {
        
        Response response;
       
        if(wrapper.getSessions().containsKey(id))
        {
            Session session = wrapper.getSessions().get(id);
            if(to_path.startsWith(session.getUser()))
            {
                File f = new File(wrapper.getFileHostingDir()+from_path);
                File f2 = new File(wrapper.getFileHostingDir()+to_path);
                
                if(f.exists())
                {
                    //Determine what is about to be moved
                    if(f.isFile())
                    {
                        try {
                            org.apache.commons.io.FileUtils.moveFileToDirectory(f, f2, false);
                            response = Response.status(Response.Status.OK).entity("File moved").type(MediaType.TEXT_PLAIN).build();

                        } catch (IOException ex) {
                            response = Response.status(Response.Status.CONFLICT).entity("Error moving file : "+ex).type(MediaType.TEXT_PLAIN).build();
                            Logger.getLogger(GenericResource.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else
                    {
                      try {
                            org.apache.commons.io.FileUtils.moveDirectoryToDirectory(f, f2, false);
                            response = Response.status(Response.Status.OK).entity("Directory moved").type(MediaType.TEXT_PLAIN).build();

                        } catch (IOException ex) {
                            response = Response.status(Response.Status.CONFLICT).entity("Error moving directory : "+ex).type(MediaType.TEXT_PLAIN).build();
                            Logger.getLogger(GenericResource.class.getName()).log(Level.SEVERE, null, ex);
                        }   
                    }
                   
                }
                else
                {
                     response = Response.status(Response.Status.NOT_FOUND).entity("from file doesn't exist").type(MediaType.TEXT_PLAIN).build();
                }
            }
            else
            {
                   response = Response.status(Response.Status.FORBIDDEN).entity("Read-only directory").type(MediaType.TEXT_PLAIN).build();
            }
        }
        else
        {
             response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
        }
        return response;
    }
    
    
    
    @GET
    @Path("/rename")
    public Response reaname(@QueryParam("sessionId") String id,
                         @QueryParam("from_path") String from_path,
                         @QueryParam("to_path") String to_path) {
        
        Response response;
       
        if(wrapper.getSessions().containsKey(id))
        {
            Session session = wrapper.getSessions().get(id);
            if(to_path.startsWith(session.getUser()))
            {
                File f = new File(wrapper.getFileHostingDir()+from_path);
                File f2 = new File(wrapper.getFileHostingDir()+to_path);
                
                if(f.exists())
                {
                    f.renameTo(f2);
                    response = Response.status(Response.Status.OK).entity("File renamed").type(MediaType.TEXT_PLAIN).build();
 
                }
                else
                {
                     response = Response.status(Response.Status.NOT_FOUND).entity("from file doesn't exist").type(MediaType.TEXT_PLAIN).build();
                }
            }
            else
            {
                   response = Response.status(Response.Status.FORBIDDEN).entity("Read-only directory").type(MediaType.TEXT_PLAIN).build();
            }
        }
        else
        {
             response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
        }
        return response;
    }
    
    @GET
    @Path("/close")
   
    public Response closeSession(@QueryParam("sessionId") String id) 
    {
        Response response;
    
        if(wrapper.getSessions().containsKey(id))
        {
            wrapper.getSessions().remove(id);
            response = Response.status(Response.Status.OK).entity("Session closed").type(MediaType.TEXT_PLAIN).build();
        
        }
        else
        {
           response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
        }
        
        return response;
    }
    
    
    
        /*
         * Produces a json with this data
         * upload.result
         * clamscan.result
         * clamscan.status
         * clamscan.signature
         * 
         */
        @POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
                @FormDataParam("file") InputStream uploadedInputStream,
		@FormDataParam("file") FormDataContentDisposition fileDetail,
                @FormDataParam("path") String path,
                @FormDataParam("filename") String filename,
                @FormDataParam("sessionId") String sessionId) {
 
            
                HashMap<String,String> map = new HashMap<String,String>();
                map.put("upload.result", "");
                map.put("clamscan.result", "");
                map.put("clamscan.status", "");
                map.put("clamscan.signature", "");
                int uploadStatus = 200;
              
                path = path==null?"":path;
                filename = filename==null?"":filename;
              
                if(!path.endsWith(File.separator))
                {
                    path += File.separator;
                }
                
                Response response;
                
       
            if (!wrapper.getSessions().containsKey(sessionId)) {                
                uploadStatus = Response.Status.UNAUTHORIZED.getStatusCode();
                map.put("upload.result","Invalid session id "+sessionId);
            }
            else
            {  
                try{
                //Trying to upload in a read-only directory
                  
                if(!path.startsWith(wrapper.getSessions().get(sessionId).getUser()))
                {
                    //Upload will be performed in my home dir
                    path = wrapper.getSessions().get(sessionId).getUser()+ File.separator;
                }
              
                String uploadedFileLocation = wrapper.getFileHostingDir() + path + filename;
                
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                org.apache.commons.io.IOUtils.copy(uploadedInputStream, baos);
                byte[] bytes = baos.toByteArray();
              
                if(bytes.length > wrapper.getFileHostingLimitBytes())
                {
                    map.put("clamscan.result","Maximum file size exceeded");
                    uploadStatus = Response.Status.NOT_ACCEPTABLE.getStatusCode();     
                }
                else
                {
                boolean virusValidation = true;
                
                if(wrapper.isFileHostingUseClamscan())
                {
                    //Check viruses
                    ByteArrayInputStream copy = new ByteArrayInputStream(bytes);
                    ScanResult scan1 = checkViruses(copy);
                    if(scan1!=null)
                    {
                        map.put("clamscan.result",scan1.getResult());
                        map.put("clamscan.signature",scan1.getSignature());
                        map.put("clamscan.status",scan1.getStatus().name());
                    }
                    else
                    {
                        map.put("clamscan.result","Can't reach Virus scan server at\n"+
                                wrapper.getClamscanAddr()+":"+wrapper.getClamscanPort());
                    }
                
                    virusValidation = (scan1!=null && scan1.getStatus().equals(ScanResult.Status.PASSED));
                }
                
                if(virusValidation)
                {
                    // save it
                    ByteArrayInputStream copy = new ByteArrayInputStream(bytes);
                    //Rename-it in case of destination already exists.
                    int i = 1;
                    while(new File(uploadedFileLocation).exists())
                    {
                        String file_ext = StringUtils.AfterLast(filename, ".");
                        String file_name = StringUtils.BeforeLast(filename, ".");
                        uploadedFileLocation = wrapper.getFileHostingDir() + path + file_name+"_"+i+"."+file_ext;
                        i += 1;
                    }
                    writeToFile(copy, uploadedFileLocation);
                    
                    //We must provide a result which is the descriptor for the uploaded file
                    //including its URL if applicable
                    File file = new File(uploadedFileLocation);
                    FileInfo fi = new FileInfo(wrapper.getFileHostingDir(), wrapper.getFileHostingDownloadPath(), file);
                    Gson gson = new Gson();
                    map.put("upload.filedescriptor", gson.toJson(fi, FileInfo.class));

                    map.put("upload.result","File updloaded to location "+uploadedFileLocation);
                    uploadStatus = Response.Status.OK.getStatusCode();
                    
                }
                else
                {
                    uploadStatus = Response.Status.NOT_ACCEPTABLE.getStatusCode();
                    map.put("upload.result","Virus scan not passed. Check scan result");
                }
                }
                }
                catch(Exception ex)
                {
                    //
                }
            }
            
            String result="";
            Gson gson = new Gson();
            
            result = gson.toJson(map, HashMap.class);
            response = Response.status(uploadStatus).entity(result).type(MediaType.APPLICATION_JSON).build();
            return response;
 
	}
    
        @GET
	@Path("/download")
	public Response download(
                @QueryParam("path") String path,
                @QueryParam("disposition") String disposition,
                @QueryParam("sessionId") String sessionId) {
 
            
            //If sessionId we can download any file
            //If no sessionId, free user -> only public files;
            
              
            path = path==null?"":path;
            disposition = disposition==null?"inline":disposition;
         
            Response response;
       
            boolean containsKey = wrapper.getSessions().containsKey(sessionId);
       
            if (!containsKey && !path.contains("Public")) {                
                response = Response.status(Response.Status.FORBIDDEN).entity("Invalid session").type(MediaType.TEXT_PLAIN).build();
            }
            else
            {  
                 File file = new File(wrapper.getFileHostingDir()+path);
                 if(file.exists() && file.isFile())
                 {
                     int length = 0;
                    try {
                         InputStream fileInputStream = new FileInputStream(file);
                    
                     response = 
                             Response.status(Response.Status.OK).type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                             .entity(fileInputStream).header("Content-Disposition",
                             disposition+"; filename=\""+file.getName()+"\"").build();
                     
                             //System.out.println(response.getEntity()+"\n"+response.getMetadata());
                     } catch (FileNotFoundException ex) {
                        Logger.getLogger(GenericResource.class.getName()).log(Level.SEVERE, null, ex);
                        response = Response.status(Response.Status.NOT_FOUND).entity("File not found or directory").type(MediaType.TEXT_PLAIN).build();
                    }
                    
                 }
                 else
                 {
                     response = Response.status(Response.Status.NOT_FOUND).entity("File not found or directory").type(MediaType.TEXT_PLAIN).build();
                 }
            }
            
            return response;
 
	}
 
        private ScanResult checkViruses(InputStream stream)
        {
       
         ScanResult result = null;
         
         ClamScan scan = new ClamScan(wrapper.getClamscanAddr(),
                 wrapper.getClamscanPort(),wrapper.getClamscanTimeout());
         try {
                result = scan.scan(stream);
               
         } catch (IOException ex) {
            //Logger.getLogger(GenericResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         return result;
        }
        
        
	// save uploaded file to new location
	private void writeToFile(InputStream uploadedInputStream,String uploadedFileLocation) {
 
		try {
			int read = 0;
			byte[] bytes = new byte[1024];
                        System.out.println("Saving to ... "+uploadedFileLocation);
			OutputStream  out = new FileOutputStream(new File(uploadedFileLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
		} catch (IOException e) {
 
			e.printStackTrace();
		}
 
	}
        
       public static byte[] getBytes(InputStream is) throws IOException {

        int len;
        int size = 1024;
        byte[] buf;

        if (is instanceof ByteArrayInputStream) {
            size = is.available();
            buf = new byte[size];
            len = is.read(buf, 0, size);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buf = new byte[size];
            while ((len = is.read(buf, 0, size)) != -1) {
                bos.write(buf, 0, len);
            }
            buf = bos.toByteArray();
        }
        return buf;
    }

    
    
}
