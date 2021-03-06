package HomeSec;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import static javax.json.Json.createObjectBuilder;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

/**
 * Implementation of the HomeSecuritySystem. 
 * 
 * @author khaves
 */
public class HomeSecSystem {
    private final short SIZE = 5;
    int ActiveCam=0;
    static private String servlet_ctxt;
    Camera[] CamArray = new Camera[SIZE];
    
    public HomeSecSystem(String ctxt) {
        HomeSecSystem.servlet_ctxt = ctxt;
        for (int i = 0; i < SIZE; i++) {
            this.CamArray[i] = new Camera (0, "<empty>");
        }
    };

    private class Camera {
        int id;
        String ip;
        Thread ThreadHandle;

        public Camera(int i, String ip) {
            id = i;
            this.ip = ip;
            this.ThreadHandle = null;
        }
         
        public void reset () {
            id = 0;
            ip = "<empty>";
            ThreadHandle = null;
        }
    }


    /**
     * Add a Camera.
     * Adds a Camera to the Systems. 
     * 
     * @param ip IP of Camera 
     * @return TRUE if camera was addes to the HomeSecSystem.
               FALSE if no more cameras vould be added to the HomeSecSystem.
     */
    public int createEntry (String ip) {
        for (int i = 0; i < SIZE; i++) {
            if (CamArray[i].ip.equals("<empty>")){
               CamArray[i].id=i+1;
               CamArray[i].ip=ip;
               ActiveCam++;
               return CamArray[i].id;
            }
        }
        return 0;    
    };
    
    /**
     * Delete Entry by id.
     * 
     * @param idx Id of Camera that should be deleted
     * 
     * @return true Entry could be deleted/false Entry doesnt exist
     */
    public boolean deleteEntry (int idx) {
        if(CamArray[idx-1].id!=0 && idx < 5 && idx > 0){
            CamArray[idx-1].reset();
            ActiveCam--;
            return true;
        }
        else
            return false;
    }
    
    /**
     * Filling JSON Object with Information.
     * Searches through all images an writes latest for each camera in JSON Object
     * 
     * @param ImgDir Path to Project Directory
     * @return JSON Object
     */
    public String createJSON(String ImgDir){
        String  day, PathToLatestFile;
        PrintWriter writer=null;
        JsonObject json=null;
        JsonArrayBuilder builder=Json.createArrayBuilder();
        
        try {
            writer = new PrintWriter("/home/pi/NetBeansProjects/VSProjekt/Logs/JSON.txt","UTF-8");
            writer.println("ImgDir:"+ImgDir);

            day=new SimpleDateFormat("dd.MM.yyyy").format(Calendar.getInstance().getTime());

            for (int i=0; i<CamArray.length; i++) {
                if(CamArray[i].id==0) continue;
                
                PathToLatestFile=getLatestFilePath(ImgDir+CamArray[i].ip+"/"+day);
                builder.add(Json.createObjectBuilder()
                                .add("id",CamArray[i].id)
                                .add("ip",CamArray[i].ip)
                                .add("path",PathToLatestFile));                       
            }
            JsonArray arr=builder.build();
            
            json=createObjectBuilder()
                    .add("server",HomeSecSystem.servlet_ctxt)
                    .add("cameras",arr)
            .build();
            
            writer.println(json.toString());
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(HomeSecSystem.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
        return json.toString();
    } 
    
    
    /**
     * Finding latest file in referenced directory
     * 
     * @param dirPath Path to Files
     * @return Path to newest Object in directory
     */
    private String getLatestFilePath(String dirPath){
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        String lastModifiedFilePath=null;
        
        if (files == null || files.length == 0) {
            return null;
        }   
        File lastModifiedFile = files[0];
        for (int i = 1; i < files.length; i++) {
            if (lastModifiedFile.lastModified() < files[i].lastModified()) {
                lastModifiedFile = files[i];
            }
        }   
        //Modify String to fit needs
        lastModifiedFilePath=lastModifiedFile.getPath();
        lastModifiedFilePath=lastModifiedFilePath.substring(lastModifiedFilePath.indexOf("Images"));

    return lastModifiedFilePath;
}
    
    /**
     * Open Connection for incoming calls.
     * Starts thread for each camera to handle connection.
     * 
     * @param ImgDir Path to Image Directory
     * @param CamID ID of Client in CamArray
     * @return True if Connection was successfull else false
     * @throws IOException if an I/O error occurs
     */
    public boolean connectSocket(String ImgDir,int CamID) throws IOException{
        ServerSocket ServSock = null;
        int LastActiveCam=0, ArrayID=0;
        boolean bOK=true;
        
        PrintWriter writer = null;
        
        if(CamID != 0) ArrayID=CamID-1;
        
        //Create Server Socket
        try {
            writer = new PrintWriter(new FileWriter("/home/pi/NetBeansProjects/VSProjekt/Logs/Socket.txt",true));
            if(CamID !=0)
                writer.println("CamID: "+CamID+" Ip:"+CamArray[ArrayID].ip);
            else
                writer.println("Alle Kameras wurden angefordert!");
            
            ServSock = new ServerSocket(8998);
        } catch (Exception e) {
            writer.println("Server Socket kann nicht erzeugt werden:"+e);  
             writer.close();
             bOK=false;
        }
       
        
        //POST: CamID=new Camera ID / GET: CamID=0 ->get all cameras
        for(int i=ArrayID; i < CamArray.length; i++){
            writer.println("Loop betreten: "+i);
            //If GET check if Client cant be reached else reset client
            if(CamID==0 && CamArray[i].id !=0 
                    &&!InetAddress.getByName(CamArray[i].ip).isReachable(1000)){
                CamArray[i].reset();
            }
            //If Camera isnt set skip camera else set LastActiveCamera     
            if(CamArray[i].id == 0){
                writer.println("Continue!");
                continue;
            }else
                LastActiveCam=i;
            
            CamArray[i].ThreadHandle=new Thread(new HomeSecConnect(ServSock, CamArray[i].ip, ImgDir));
            CamArray[i].ThreadHandle.start();
            writer.println("Socketthread "+i+" gestartet mit "+CamArray[i].ip+"!");
            
            //If CamID is set only do this ID
            if(CamID != 0) break;
        }
        
        writer.println("LastActiveCam:"+LastActiveCam);
        //Wait for every Thread to finish and check if thread could be started, else reset client
        try {
            for(int i=LastActiveCam; i>=0; i--){
                if(CamArray[i].id != 0){
                    if(CamArray[i].ThreadHandle.isAlive())
                        CamArray[i].ThreadHandle.join();
                }
            }
        } catch (InterruptedException ex) {
            writer.println("Excpetion beim Warten auf Thread "+LastActiveCam+":"+ex);
            bOK=false;
        }  
        
        ServSock.close();
        writer.println("Funktion erfolgreich verlassen!\r\n");
        writer.close();
        return bOK;
    }
    
    /**
     ** Check if passed IP is valid.
     * 
     * @param ip String with IP to check
     * @return true=valid/false=invalid
     */
    public boolean validIP (String ip) {
    try {
        if ( ip == null || ip.isEmpty() ) {
            return false;
        }

        String[] parts = ip.split( "\\." );
        if ( parts.length != 4 ) {
            return false;
        }

        for ( String s : parts ) {
            int i = Integer.parseInt( s );
            if ( (i < 0) || (i > 255) ) {
                return false;
            }
        }
        if ( ip.endsWith(".") ) {
            return false;
        }

        return true;
    } catch (NumberFormatException nfe) {
        return false;
    }
}
}
