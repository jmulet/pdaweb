/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.beans;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import javax.swing.filechooser.FileSystemView;
import org.iesapp.util.StringUtils;

/**
 *
 * @author Josep
 */
public class FileDescriptor {


    protected String iconPath="images/ext/";
    protected String icon;
    protected String name="";
    protected String ext = "";
    protected String realPath="";
    protected String relativePath="";
    protected String owner="";
    protected String size="";
    protected String link="";
    protected boolean directory=false;
    
    protected boolean belongs = false;
    protected boolean canRename=false;
    protected boolean canDelete=false;
    protected boolean canShow=true;
    protected boolean canMkdir=false;
    protected boolean canShowOwner=false;
    public static String baseUrl="/";
    
    public static final long KB = 1024;
    public static final long MB = KB*1024;
    public static final long GB = MB*1024;
     
    protected static List<String> images = Arrays.asList(new String[]{"jpg","jpeg","gif","png","tif","bmp"});
    protected static List<String> document = Arrays.asList(new String[]{"txt","doc","docx","log","odt"});
    protected static List<String> movies = Arrays.asList(new String[]{"mp4","qt","avi","mov"});
    protected static List<String> compressed = Arrays.asList(new String[]{"zip","7z","tar","rar"});
    protected final File file;
    protected final String ownerId;
    protected final static FileSystemView fsv = FileSystemView.getFileSystemView();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm");
    protected String type;
    protected String lastModified;
    
    @Override
    public FileDescriptor clone()
    {
        FileDescriptor fd = new FileDescriptor(this.file, this.getOwnerId(), this.owner, this.belongs);
        fd.canDelete = this.canDelete;
        fd.canMkdir = this.canMkdir;
        fd.canRename = this.canRename;
        fd.canShow = this.canShow;
        fd.canShowOwner = this.canShowOwner;
        return fd;
       
    }

    public FileDescriptor(final File file, final String ownerId, final String owner, final boolean belongs) {
       
        this.file = file;
        this.ownerId = ownerId;
        this.name = file.getName();
        this.belongs = belongs;
        this.realPath = file.getAbsolutePath();
        this.size = getFileSize(file);
        this.directory = file.isDirectory();
        this.canDelete = belongs;
        this.type = fsv.getSystemTypeDescription(file); 
        java.util.Date date = new java.util.Date(file.lastModified());
        
        this.lastModified = dateFormat.format(date);
      
        if(this.directory)
        {
            this.icon = iconPath+"folder.png";
            this.canShow = false;
        }
        else
        {
            this.ext = StringUtils.AfterLast(name, ".");
            if(images.contains(this.ext))
            {
                this.icon = iconPath+"images.png";
            }
            else if(document.contains(this.ext))
            {
                this.icon = iconPath+"document.png";
            }
            else if(movies.contains(this.ext))
            {
                this.icon = iconPath+"movies.png";
            }
            else if(compressed.contains(this.ext))
            {
                this.icon = iconPath+"zip.png";
            }
            else if(this.ext.equals("xls"))
            {
                this.icon = iconPath+"xls.png";
            }
            else if(this.ext.equals("pdf"))
            {
                this.icon = iconPath+"pdf.png";
            }
            else  
            {
                this.icon = iconPath+"file.png";
            }
        }
         
        
        String txt = StringUtils.AfterFirst(realPath, ownerId);
        this.relativePath = ownerId + txt;
        this.link = baseUrl + relativePath;
        this.link = this.link.replaceAll(" ", "%20").replaceAll("\\\\", "/");
        this.owner = owner;
     
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRealPath() {
        return realPath;
    }

    public void setRealPath(String realPath) {
        this.realPath = realPath;
    }

   

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
 
     private String getFileSize(java.io.File file)
     {
         String str = "";
              long length = file.length();
             if(length<KB)
             {
                 str = length+" B";
             }
             else if(length>KB && length<MB)
             {
                 str = round(length/(1.*KB))+" KB";
             }
             else if(length>MB && length<GB)
             {
                 str = round(length/(1.*MB))+" MB";
             }
             else
             {
                 str = round(length/(1.*GB))+" GB";
             }
         return str;   
             
     }
     
     private double round(double x)
     {
         return (Math.round(x*10)/10.);
     }


    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public boolean isCanRename() {
        return canRename;
    }

    public void setCanRename(boolean canRename) {
        this.canRename = canRename;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean isCanShow() {
        return canShow;
    }

    public void setCanShow(boolean canShow) {
        this.canShow = canShow;
    }

    public boolean isCanMkdir() {
        return canMkdir;
    }

    public void setCanMkdir(boolean canMkdir) {
        this.canMkdir = canMkdir;
    }

    public boolean isCanShowOwner() {
        return canShowOwner;
    }

    public void setCanShowOwner(boolean canShowOwner) {
        this.canShowOwner = canShowOwner;
    }

    public boolean isBelongs() {
        return belongs;
    }

    public void setBelongs(boolean belongs) {
        this.belongs = belongs;
    }

    public File getFile() {
        return file;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getType() {
        return type;
    }

    public String getLastModified() {
        return lastModified;
    }
    
   

}