/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

import org.iesapp.web.cloudws.ClamScan;
import org.iesapp.web.cloudws.FileInfo;
import org.iesapp.web.cloudws.GenericResource;
import org.iesapp.web.cloudws.ScanResult;
import org.iesapp.core.util.Client;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.iesapp.clients.sgd7.profesores.BeanProfesor;
import org.iesapp.util.StringUtils;
import org.primefaces.event.DragDropEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.UploadedFile;
import org.iesapp.web.pdaweb.beans.FileDescriptor;
import org.iesapp.web.cloudws.FacesUtil;

/**
 *
 * @author Josep
 */
@ManagedBean(name = "mbFileHosting")
@SessionScoped
public class mbFileHosting implements java.io.Serializable {

    private final BeanProfesor selectedUser;
    private final String filePath;
    protected ArrayList<FileDescriptor> fileList;
    protected DefaultTreeNode root;
    protected FileDescriptor[] selectedFiles;
    protected TreeNode selectedNode;
    protected FileDescriptor currentPath;
    protected ArrayList<FileDescriptor> backPaths;
    protected ArrayList<FileDescriptor> nextPaths;
    private DefaultTreeNode userNode;
   
    protected String fileLimit="unlimited";
    private String requestURI;
    private final FileFilter filter;
    private ArrayList<FileDescriptor> clipboardFiles;
    protected boolean renderUploader = false;
    private FileDescriptor currentRenameFD;
    private String action;
    protected String displayableURL;
    private static final int BUFSIZE = 4096;
   
//    private final CloudClientSession cloudSession;
    private final String fileHostingDownloadPath;
    private final String fileHostingLimit;
    private long fileHostingLimitBytes;
    private final String clamscanAddr;
    private final int clamscanPort;
    private final int clamscanTimeout;
    private final boolean fileHostingUseClamscan;
    private final Client client;
    
            
  
    public mbFileHosting() {
         
        selectedUser = (BeanProfesor) FacesUtil.getSessionMapValue("selectedUser");
        client = (Client) FacesUtil.getSessionMapValue("client");
        //We need a session to download files
//        CloudClientSession.baseURL = (String) CoreCfg.configTableMap.get("cloudBaseURL");
//        cloudSession = new CloudClientSession(selectedUser.getSystemUser(), selectedUser.getClaveUP());
//        FacesUtil.setSessionMapValue("cloudSession", cloudSession);
        
        //Enable navigation
        backPaths = new ArrayList<FileDescriptor>();
        nextPaths = new ArrayList<FileDescriptor>();
        
        //Retreive initialization parameters
        String fileHostingDir = StringUtils.noNull((String) FacesUtil.getWebXmlParam("pdaweb.fileHostingDir")).trim();
        if (!fileHostingDir.endsWith(File.separator)) {
            fileHostingDir += File.separator;
        }

        //UPLOAD/DOWNLOAD - Limits de pujada de fitxers
        fileHostingDownloadPath = (String) FacesUtil.getWebXmlParam("pdaweb.fileHostingDownloadPath");
        if (!fileHostingDir.endsWith(File.separator)) {
            fileHostingDir += File.separator;
        }

        fileHostingLimit = ((String) FacesUtil.getWebXmlParam("pdaweb.fileHostingLimit")).trim();
        fileHostingLimitBytes = 0;

        if (!fileHostingLimit.equalsIgnoreCase("unlimited")) {
            //System.out.println("fileHostingLimit = " + fileHostingLimit);
            int len = fileHostingLimit.length();
            String value = fileHostingLimit.substring(0, len - 1);
            String magnitud = fileHostingLimit.substring(len - 1, len);
            int ival = Integer.parseInt(value);
            long factor = 1;
            if (magnitud.equalsIgnoreCase("k")) {
                factor = FileInfo.KB;
            } else if (magnitud.equalsIgnoreCase("m")) {
                factor = FileInfo.MB;
            } else if (magnitud.equalsIgnoreCase("g")) {
                factor = FileInfo.GB;
            }
           // System.out.println("fileHostingLimit ival & factor " + ival + "&" + factor);

            fileHostingLimitBytes = ival * factor;
        }

            
            //Antivirus parameters
            clamscanAddr = (String) FacesUtil.getWebXmlParam("pdaweb.clamscanAddr");
            clamscanPort = Integer.parseInt((String) FacesUtil.getWebXmlParam("pdaweb.clamscanPort"));
            clamscanTimeout = Integer.parseInt((String) FacesUtil.getWebXmlParam("pdaweb.clamscanTimeout"));
            fileHostingUseClamscan = FacesUtil.getWebXmlParam("pdaweb.fileHostingUseClamscan").toUpperCase().startsWith("Y");
            
        
        
        
        
        
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        requestURI = request.getRequestURL().toString();
        requestURI = StringUtils.BeforeLast(requestURI, "/")+"/cloud/";
        FileDescriptor.baseUrl = requestURI;
        
        filter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() && !f.getName().startsWith(".");
            }
        };
         
        if(fileHostingDir.isEmpty()){
            filePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("") 
                    + File.separator + "filehosting" + File.separator;
        }
        else
        {
            filePath = fileHostingDir;
        }
        
        
         
        String tmp =  StringUtils.noNull( (String) FacesUtil.getWebXmlParam("pdaweb.fileHostingLimit") ).trim();
        if(tmp.isEmpty() || tmp.equalsIgnoreCase("unlimited")){
           fileLimit = "unlimited";
        }
        else
        {
            String charAt = tmp.substring(tmp.length()-1);
            int value= Integer.parseInt(tmp.substring(0,tmp.length()-1));
            if(charAt.equalsIgnoreCase("k"))
            {
                fileLimit = ""+value*FileDescriptor.KB;
            }
            else if(charAt.equalsIgnoreCase("m"))
            {
                fileLimit = ""+value*FileDescriptor.MB;
            }
            else if(charAt.equalsIgnoreCase("g"))
            {
                fileLimit = ""+value*FileDescriptor.GB;
            }
            else
            {
                fileLimit = tmp;
            }
        }
       
         
          
        //Comprova la consistencia d'aquest usuari
        GenericResource.checkIntegrity(filePath, selectedUser.getSystemUser(), client.getIesClient().getCoreCfg().anyAcademic);
     
        loadTree(null);
    }

 
     
    private void loadTree(FileDescriptor displayIt) {
        root = new DefaultTreeNode("files", null);
        TreeNode displayItNode = null;
        
        //list directories
        File[] listDir = new File(filePath).listFiles(filter);

        for (File d : listDir) {

            String userDir = d.getName();  // this is the windows user name
            //Retrieve userName
            org.iesapp.clients.sgd7.profesores.Profesores prof = client.getSgdClient().getProfesores();
            prof.loadByWinUser(userDir);
            
            
              //This is the user dir
            boolean editable = false;
            if (userDir.equals(selectedUser.getSystemUser())) {
                editable = true;
            }
            
            //(final File file, final String ownerId, final String owner, final boolean belongs)
            FileDescriptor descriptor = new FileDescriptor(d, userDir, prof.getNombre(), editable);
            descriptor.setCanDelete(false);
            descriptor.setCanRename(false);
            descriptor.setCanShow(false);
            descriptor.setCanShowOwner(true);
            descriptor.setCanMkdir(editable);
            DefaultTreeNode node = new DefaultTreeNode(descriptor, getRoot());
            
            if(editable){
                userNode = node;
                userNode.setExpanded(true);
                selectedNode = node;
            }

            //Now include all its contents (including folders - must be recursively)
            File[] listFiles = d.listFiles(filter);

            for (File f : listFiles) {
                FileDescriptor descriptor2 = new FileDescriptor(f, userDir, prof.getNombre(), editable);
                descriptor2.setCanRename(editable);
                descriptor2.setCanShow(f.isFile());
                descriptor2.setCanShowOwner(false);
                descriptor2.setCanMkdir(editable && f.isDirectory());
                DefaultTreeNode folderNode = new DefaultTreeNode(descriptor2, node);          
                if(f.isDirectory())
                {
                    recursivelyAddDirToTree(f, folderNode, userDir, prof.getNombre(), editable, displayIt, displayItNode);
                }
                if(displayIt!=null && f.getAbsolutePath().equals(displayIt.getFile().getAbsolutePath()))
                {
                    displayItNode = folderNode;
                }
            }
        }
        
        if(selectedNode!=null && displayItNode==null)
        {
            loadFilesInDir(((FileDescriptor) selectedNode.getData()));
        }
        else if(displayItNode!=null)
        {
             loadFilesInDir(((FileDescriptor) displayItNode.getData()));
             displayItNode.setSelected(true);
             //We must expand all parent nodes to correctly display this node
             
             displayItNode.setExpanded(true);
             TreeNode parent = displayItNode.getParent();
             while(parent!=null)
             {
                 parent.setExpanded(true);
                 parent = parent.getParent();
             }
        }

    }

    public void handleFileUpload(FileUploadEvent event) throws IOException {

        String goodFilePath;
        UploadedFile file = event.getFile();
        
        //Decide where to updload
        if(currentPath!=null)
        {
            FileDescriptor fd = currentPath;
            if(fd.isBelongs())
            {
                if(fd.isDirectory())
                {
                    goodFilePath = fd.getFile().getAbsolutePath()+File.separator;
                }
                else
                {
                    File parentFile = fd.getFile().getParentFile();
                    goodFilePath = parentFile.getAbsolutePath()+File.separator;
                }
            }
            else
            {
                 goodFilePath = filePath + selectedUser.getSystemUser() + File.separator;
            }
        }
        else
        {
             goodFilePath = filePath + selectedUser.getSystemUser() + File.separator;
        }
        
        //application code
        //System.out.println("uploading file " + file.getFileName() + " to dir "+goodFilePath);
        
        
        //check if this file already exists
        int id = 1;
        String fullFilePath = goodFilePath + file.getFileName();
        while (new File(fullFilePath).exists()) {
            String parsedName = StringUtils.BeforeLast(file.getFileName(), ".");
            String extension = StringUtils.AfterLast(file.getFileName(), ".");
            fullFilePath = goodFilePath + parsedName+"_"+id+"."+extension;
            id +=1;
        }
        
        //Copy the input stream and check for viruses
         InputStream uploadedInputStream = file.getInputstream();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         org.apache.commons.io.IOUtils.copy(uploadedInputStream, baos);
         byte[] bytes = baos.toByteArray();
         
         FacesContext fc = FacesContext.getCurrentInstance();
           
        if (bytes.length > fileHostingLimitBytes) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,"Maximum file size exceeded","")); 
            return;
        } else {
            
            boolean virusValidation = true;
            ScanResult scan1 = null;

            if (fileHostingUseClamscan) {
                //Check viruses
                ByteArrayInputStream copy = new ByteArrayInputStream(bytes);
                scan1 = checkViruses(copy);
                if (scan1 == null) {
                     fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                             "Can't reach Virus scan server at\n"
                            + clamscanAddr + ":" + clamscanPort,"")); 
                    return;
                }

                virusValidation = (scan1 != null && scan1.getStatus().equals(ScanResult.Status.PASSED));
            }

            if (virusValidation) {
                // save it
                ByteArrayInputStream copy = new ByteArrayInputStream(bytes);
                writeToFile(copy, fullFilePath);
            }
            else
            {
                //Virus found
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                             "Virus! "+scan1.getResult()+" : "+scan1.getSignature() ,"")); 
                return;
                
            }
                

        }

        loadFilesInDir(currentPath);
        renderUploader = false;
  
    }
            
    // save uploaded file to new location
    private void writeToFile(InputStream uploadedInputStream, String uploadedFileLocation) {

        try {
            int read = 0;
            byte[] bytes = new byte[1024];
            OutputStream out = new FileOutputStream(new File(uploadedFileLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    private ScanResult checkViruses(InputStream stream) {

        ScanResult result = null;

        ClamScan scan = new ClamScan(clamscanAddr,
                clamscanPort, clamscanTimeout);
        try {
            result = scan.scan(stream);

        } catch (IOException ex) {
            //Logger.getLogger(GenericResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public ArrayList<FileDescriptor> getFileList() {
        return fileList;
    }

    public void setFileList(ArrayList<FileDescriptor> fileList) {
        this.fileList = fileList;
    }

    public DefaultTreeNode getRoot() {
        return root;
    }
  
//  
//    public void prepareDelete(ActionEvent event){
//
//	for(UIComponent component : event.getComponent().getChildren()){
//		
//		if( component instanceof UIParameter ){
//
//			UIParameter param = (UIParameter) component;
//			
//			if(param.getName().equals("selectedFile")){
//                            selectedFile = (FileDescriptor) param.getValue();
//			}
//		}
//	}
//    }
//
//    public void delete()
//    {
//          if(selectedFile==null)
//        {
//            //System.out.println("Returning");
//            return;
//        }
//        //selectedFile = (FileDescriptor) selectedNode.getData();
//        //Deleting files
//        //System.out.println("Deleting "+selectedFile.getRealPath());
//        File file = new File(selectedFile.getRealPath());
//        if(file.exists())
//        {
//            if(file.isDirectory())
//            {
//                deleteDir(file);
//            }
//            else
//            {
//                file.delete();
//            }
//        }
//        loadTree();
//    }
//    
     public boolean deleteDir(File dir) {
                if (dir.isDirectory()) {
                        String[] children = dir.list();
                        for (int i = 0; i < children.length; i++) {
                                boolean success = deleteDir(new File(dir, children[i]));
                                if (!success) {
                                        return false;
                                }
                        }
                }
                return dir.delete();
        }

    
//    public void updateMyFiles()
//    {
//        ArrayList<TreeNode> children = new ArrayList<TreeNode>();
//       //Now include all its contents
//        String userDir = selectedUser.getSystemUser();
//        File[] listFiles = new File(filePath + userDir).listFiles();
//        
//            for (File f : listFiles) {
//                TreeNode folderNode = new DefaultTreeNode(new FileDescriptor(f.getName(), f.getAbsolutePath(),
//                         requestURI+"filehosting/"+userDir+"/"+f.getName(), getFileSize(f), true), userNode);
//                children.add(folderNode);
//            }
//        userNode.setChildren(children);
//                
//    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }
           
    
    public void onShowDocument(FileDescriptor fd)
    {
      
     if(fd==null)
        {
            return;
        }
       //selectedFile = (FileDescriptor) selectedNode.getData();
        //Deleting files
        
        if(fd.getFile().isFile())
        {
            //Nomes serveix per Public
            //FacesUtil.sendRedirect(fd.getLink());
            
            //Intentem utilitzar (*)
            FacesContext fcontext = FacesContext.getCurrentInstance();
            ExternalContext econtext = fcontext.getExternalContext();
            HttpServletResponse response = (HttpServletResponse) econtext.getResponse();
            //HttpServletRequest request = (HttpServletRequest) econtext.getRequest();
           
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "inline; filename=\"" + fd.getName()+"\"");
            
            File file = new File(fd.getRealPath());
            int length = (int) file.length();
            response.setContentLength(length);
            try{
            ServletOutputStream outStream = response.getOutputStream();
            byte[] byteBuffer = new byte[BUFSIZE];
            DataInputStream in = new DataInputStream(new FileInputStream(file));

            // reads the file's bytes and writes them to the response stream
            while ((in != null) && ((length = in.read(byteBuffer)) != -1)) {
                outStream.write(byteBuffer, 0, length);
            }
            in.close();
            outStream.close();
            }
            catch(Exception e)
            {
                //
            }
            fcontext.responseComplete();
        }
        else
        {
            //Directory has been choosed
            currentPath = fd.clone();
            //Try to choose the correct treeNode
            TreeNode selection = null;
            TreeNode node = findNode(fd, null);
            if(node!=null)
            {
                node.setExpanded(true);
                node.setSelected(true);
                selectedNode = node;
            }
            loadFilesInDir(currentPath);
        }
    }
//    
//    
//      public void mkdir(ActionEvent event)
//    {
//        
//	for(UIComponent component : event.getComponent().getChildren()){
//		
//		if( component instanceof UIParameter ){
//
//			UIParameter param = (UIParameter) component;
//			
//			if(param.getName().equals("selectedFile")){
//                            selectedFile = (FileDescriptor) param.getValue();
//			}
//		}
//	}
//        
//        //System.out.println("i am on show doc");
//        if(selectedFile==null)
//        {
//            //System.out.println("Returning");
//            return;
//        }
//        
//        //System.out.println("Trying to mkdir to "+selectedFile.getRealPath());
//        File newFolder = new File(selectedFile.getRealPath()+File.separator+"NewFolder");
//        int id=1;
//        while(newFolder.exists())
//        {
//             newFolder = new File(selectedFile.getRealPath()+File.separator+"NewFolder_"+id);
//             id +=1;
//        }
//        
//        newFolder.mkdir();
//        loadTree();
//    }
//    
     public void onRename() //(String newname, String oldpath)
     {
        String newname = currentRenameFD.getName();
        String oldpath = currentRenameFD.getRealPath();
        File file = new File(oldpath);
         if (file.exists()) {
             File parentFile = file.getParentFile();
             File newFile = new File(parentFile.getAbsolutePath() + File.separator + newname);
             if (!newFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                 file.renameTo(newFile);
                 loadTree(currentPath.clone());
             }
        }
        
    }
     
     public void onCancelRename()
     {
        loadFilesInDir(currentPath);
     }
     
     
    

    public String getFileLimit() {
        return fileLimit;
    }

    private void recursivelyAddDirToTree(File d, DefaultTreeNode parentNode, String userDir, String nombre,
            boolean editable, final FileDescriptor displayIt, TreeNode displayItNode) {
        File[] listFiles = d.listFiles(filter);

        for (File f : listFiles) {
            FileDescriptor descriptor2 = new FileDescriptor(f, userDir, nombre, editable);
            descriptor2.setCanRename(editable);
            descriptor2.setCanShow(f.isFile());
            descriptor2.setCanShowOwner(false);
            descriptor2.setCanMkdir(editable && f.isDirectory());
            DefaultTreeNode folderNode = new DefaultTreeNode(descriptor2, parentNode);
            if (f.isDirectory()) {
                recursivelyAddDirToTree(f, folderNode, userDir, nombre, editable, displayIt, displayItNode);
            }
            if(displayIt!=null && displayIt.getFile().getAbsolutePath().equals(f.getAbsolutePath()))
            {
                displayItNode = folderNode;
            }
        }

    }
    
    public void onDragDrog(DragDropEvent event)
    {
       // //System.out.println("DragId:"+event.getDragId()+"  "+"Drop Id:"+event.getDropId());
      //  //System.out.println(event.getData());
    }
    
    public void onNodeSelect(NodeSelectEvent event)
    {
       selectedNode = event.getTreeNode();
       FileDescriptor fd = (FileDescriptor) selectedNode.getData();
       loadFilesInDir(fd);
       
    }

    private void loadFilesInDir(FileDescriptor fd) {
        if(currentPath!=null)
        {
            backPaths.add(fd.clone());
        }
        currentPath = fd.clone();
        displayableURL = "";
        
        FileFilter filter2 = new FileFilter()
        {
         @Override
            public boolean accept(File f) {
                return f.isFile() || (f.isDirectory() && !f.getName().startsWith("."));
            }
            
        };
        File[] listFiles = fd.getFile().listFiles(filter2);
        String userDir = fd.getOwnerId();
        String nombre = fd.getOwner();
        boolean editable = fd.isBelongs();
        fileList = new ArrayList<FileDescriptor>();
        
        //Always add back action
        for(File f: listFiles)
        {
            FileDescriptor descriptor2 = new FileDescriptor(f, userDir, nombre, editable);
            descriptor2.setCanRename(editable);
            descriptor2.setCanShow(f.isFile());
            descriptor2.setCanShowOwner(false);
            descriptor2.setCanMkdir(editable && f.isDirectory());
            fileList.add(descriptor2);
        }
    }

  
    
    public void onCopyAction()
    {
        action = "COPY";
        if(getSelectedFiles()!=null)
        {
            clipboardFiles = new ArrayList<FileDescriptor>();
            for(FileDescriptor fd: getSelectedFiles())
            {
                clipboardFiles.add(fd.clone());
            }
            
        //    //System.out.println("copied # files ..."+clipboardFiles.size());
        }
    }
    
    public void onPasteAction()
    {
        if(clipboardFiles!=null && currentPath!=null)
        {
            if(currentPath.isBelongs())
            {
                for(FileDescriptor fd: clipboardFiles)
                {
                    
                    
                    File fileOrig = fd.getFile(); //original file
                    File folderDesti = currentPath.getFile(); //destination folder 
                    File fileOutput = new File(folderDesti.getAbsolutePath()+File.separator+fileOrig.getName());
                            
                    try {
                        int id = 1;
                        while (FileUtils.directoryContains(folderDesti, fileOutput)) {
                            fileOutput = new File(folderDesti.getAbsolutePath()+File.separator+renameFile(fileOrig.getName(),id));        
                            id += 1;
                        }
                         
                        if(fileOrig.isFile())
                        {                           
                          //  //System.out.println("About to copy file ..."+fileOrig.getAbsolutePath()+" to "+ fileOutput.getAbsolutePath());
                            org.apache.commons.io.FileUtils.copyFile(fileOrig, fileOutput);
                            if(action.equals("CUT") && fd.isBelongs()) //remove original files if belongs to me
                            {
                                fileOrig.delete();
                            }
                        }
                        else if(fileOrig.isDirectory())
                        {
                            //System.out.println("About to copy directory ..."+fileOrig.getAbsolutePath()+" to "+ fileOutput.getAbsolutePath());
                            org.apache.commons.io.FileUtils.copyDirectory(fileOrig, fileOutput);
                            if(action.equals("CUT") && fd.isBelongs()) //remove original files if belongs to me
                            {
                                FileUtils.deleteDirectory(fileOrig);
                            }
                        }
                        } catch (IOException ex) {
                        Logger.getLogger(mbFileHosting.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            else
            {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Read only", "Can't write on this folder"));  
                return; //can't paste on a folder that is not mine
            }
        }
        clipboardFiles = null;
        action = "";
        loadTree(currentPath.clone());
    }

    public FileDescriptor[] getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(FileDescriptor[] selectedFiles) {
        this.selectedFiles = selectedFiles;
    }

    public FileDescriptor getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(FileDescriptor currentPath) {
        this.currentPath = currentPath;
    }

    private String renameFile(String filename, int id) {
        String newName = filename;
        if(filename.contains("."))
        {
            String name = StringUtils.BeforeLast(filename, ".");
            String ext = StringUtils.AfterLast(filename, ".");
            newName = name+"_"+id+"."+ext;
        }
        else
        {
             newName = filename+"_"+id;          
        }
        return newName;
    }
    
    public void onDelAction()
    {
        if(getSelectedFiles()!=null && currentPath!=null)
        {
            
            for(FileDescriptor fd: getSelectedFiles())
            {
                if(!fd.isBelongs())
                {
                    //Can't delete a file that is not mine
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Read only", "Can't delete on this folder"));  
                    return;
                }
                
                File file = fd.getFile();
                if(file.isFile())
                {
                    file.delete();
                }
                else
                {
                    try {
                        FileUtils.deleteDirectory(file);
                    } catch (IOException ex) {
                        Logger.getLogger(mbFileHosting.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            
                //System.out.println("deleted # files ..."+selectedFiles.length);
                loadTree(currentPath.clone());
            }
            
            
            
        }
    }
    
    public void onCutAction()
    {
        action = "CUT";
        if(getSelectedFiles()!=null)
        {
            clipboardFiles = new ArrayList<FileDescriptor>();
            for(FileDescriptor fd: getSelectedFiles())
            {
                clipboardFiles.add(fd.clone());
            }
            
            //System.out.println("copied # files  to clipboard..."+clipboardFiles.size());
        }
    }
    
    
    public void onNewFolder()
    {
        if(currentPath==null)
        {
            return;
        }
        
        //System.out.println("Trying to mkdir to "+currentPath.getRealPath());
        File newFolder = new File(currentPath.getRealPath()+File.separator+"NewFolder");
        int id=1;
        while(newFolder.exists())
        {
             newFolder = new File(currentPath.getRealPath()+File.separator+"NewFolder_"+id);
             id +=1;
        }
        
        newFolder.mkdir();
        loadTree(currentPath.clone());
        
    }
    
//     public void rename(String oldpath, String newname)
//     {
//        
//        //System.out.println("i am on rename to "+oldpath+"->"+newname);
//        
//       //selectedFile = (FileDescriptor) selectedNode.getData();
//         
//        File file = new File(oldpath);
//        //System.out.println("Real path : "+selectedFile.getRealPath());
//        if(file.exists())
//        {
//            File parentFile = file.getParentFile();
//            File newFile = new File(parentFile.getAbsolutePath()+File.separator+newname);
//            //System.out.println("Renaming to : "+newFile.getAbsolutePath());
//            file.renameTo(newFile);
//        }
//        loadTree(current);
//    }
//    }

    public boolean isRenderUploader() {
        return renderUploader;
    }

    public void setRenderUploader(boolean renderUploader) {
        this.renderUploader = renderUploader;
    }
    
    public void setCurrentRename(FileDescriptor fd)
    {
        currentRenameFD = fd;
        //System.out.println("setting currentRenameFD to "+fd);
    }
    
    public void onDataTableSelection(SelectEvent event)
    {
        //System.out.println("onDT selection object is :"+event.getObject());
    }
            
    
    private TreeNode findNode(FileDescriptor fd, TreeNode startNode)
    {
        TreeNode nodeFound = null;
        if(startNode == null)
        {
            startNode = root;
        }
        List<TreeNode> children = startNode.getChildren();
        for(TreeNode node: children)
        {
            if( ((FileDescriptor) node.getData()).getRealPath().equals(fd.getRealPath()) )
            {
                nodeFound = node;
                break;
            }
            else
            {
                TreeNode findNode = findNode(fd, node);
                if(findNode!=null)
                {
                    nodeFound = node;
                    break;
                }
            }
        }
        return nodeFound;
    }

    public String getDisplayableURL() {
        return displayableURL;
    }

    public void setDisplayableURL(String displayableURL) {
        this.displayableURL = displayableURL;
    }
    
    
    public void displayURL(SelectEvent event)
    {
        FileDescriptor fd = (FileDescriptor) event.getObject();
        String relativePath = fd.getRelativePath();
        if(fd.getFile().isFile() && relativePath.startsWith(selectedUser.getSystemUser()+File.separator+"Public"))
        {
            this.displayableURL = fd.getLink(); 
        }
        else
        {
            this.displayableURL = "";
        }
    }
    
    /**
     * Actions parent 
     * @param event 
     */
    public void action(ActionEvent event)
    {
         String idComponent = event.getComponent().getId();
         String userDir = currentPath.getOwnerId();
         String userNombre = currentPath.getOwner();
       
         if(idComponent.equals("backbtn"))
         {
             //currentPath = 
             
             if(!backPaths.isEmpty())
             {
                 //System.out.println("BACK BUTTONS REGISTRY");
                 for(FileDescriptor fd: backPaths)
                 {
                     //System.out.print(fd.getRealPath()+" -> ");
                 }
                 //System.out.print("\n");
                 nextPaths.add(currentPath.clone());
                 int idx = backPaths.size()-1;
                 currentPath = backPaths.get(idx).clone();
                 backPaths.remove(idx);
             }
         }
         else if(idComponent.equals("nextbtn"))
         {
             if(!nextPaths.isEmpty())
             {
                 //System.out.println("NEXT BUTTONS REGISTRY");
                 for(FileDescriptor fd: nextPaths)
                 {
                     //System.out.print(fd.getRealPath()+" -> ");
                 }
                 //System.out.print("\n");
                 int idx = nextPaths.size()-1;
                 //backPaths.add(currentPath.clone());
                 currentPath = nextPaths.get(idx).clone();
                 nextPaths.remove(idx);
             }
         }
         else if(idComponent.equals("upbtn"))  // Move one directory up -- parent (maximum parent is filePath)
         {
             File f = currentPath.getFile().getParentFile();
             if(!f.getAbsolutePath().equals(filePath))
             {
                currentPath = new FileDescriptor(f, userDir, userNombre, true);
                //currentPath.setCanDelete(false);
                //currentPath.setCanRename(false);      
             }
         }
         else if(idComponent.equals("homebtn"))  //Go to home
         {
             File f = new File(filePath+selectedUser.getSystemUser());
             currentPath = new FileDescriptor(f, selectedUser.getSystemUser(), selectedUser.getNombre(), true);
             currentPath.setCanDelete(false);
             currentPath.setCanRename(false);      
         }
         else if(idComponent.equals("refreshbtn"))
         {
             //Do nothing reload the current path
         }
         
         loadTree(currentPath);
    }
}

