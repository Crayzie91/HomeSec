/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HomeSec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 * Class implements Thread to handle Connections.
 * Thread connects to WebService of Client ({@link #handleClient() handleClient}
 * ) to take and receive picture from Client.
 * Image is saved on Server.
 * To take picture with server camera a local function 
 * ({@link #handleServer() handlerServer}) is called to take and save image.
 * 
 * @author khaves
 */
public class HomeSecConnect implements Runnable{
    
    //Parameters are saved as instance variables
    ServerSocket ServSock;
    Socket CliSock;
    String ImgDir,PicName, ip;
    HomeSecClient hsc;
    
    HomeSecConnect(ServerSocket s, String ClientIP, String img){
        this.ServSock = s;
        this.ip=ClientIP;
        this.ImgDir = img;
    }
    
    @Override
    public void run(){
        if (this.ip.equals("localhost")) 
            handleServer();
        else
            handleClient();
    }
    
    public void handleServer(){
        String day,time,path,command;
        
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter("/home/pi/NetBeansProjects/VSProjekt/Logs/Thread.txt",true));
            writer.println("Lokal auf Server "+this.ip);
            day=new SimpleDateFormat("dd.MM.yyy").format(Calendar.getInstance().getTime());
            time=new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());
            //Create Directory an set path for image
            path=this.ImgDir+ip;
            new File(path).mkdirs();
            path=path+"/"+day;
            new File(path).mkdirs();
            path=path+"/"+time+".jpg";
            writer.println("Bilder auf Server: "+path);
            //Excute command to take picture
            command="raspistill -q 25 -h 1080 -w 1920 -hf -vf -o "+path;
            writer.println("Kommando ausgeführt: "+command);
            Process p=Runtime.getRuntime().exec(command);
            p.waitFor();
        } catch (IOException|InterruptedException ex) {
            writer.println("Exception in handleServer():"+ex);
        }  finally {
            writer.close();
        } 
    }
    
    public void handleClient(){
        String day, PicPath, ComplPath=null;
        Socket CliSock=null;

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter("/home/pi/NetBeansProjects/VSProjekt/Logs/Thread.txt",true));
            writer.println("\r\nVerbunden mit "+this.ip);            
            
            //Connect to WebService Client. End Thread if connection fails
            if(!getService(this.ip))
                return;
            writer.println("Service verbunden!");
            //Client takes Picture
            PicPath=takePicture(ImgDir);
            writer.println("Foto geschossen:"+PicPath);
            //Client sends Picture, Serverthread accepts ClientSocket
            boolean bOK=sendPicture(PicPath);
            CliSock=this.ServSock.accept();
            writer.println("Bild wurde gesendet und Client Socket akzeptiert!");
            
            //Set Date and create directory
            day=new SimpleDateFormat("dd.MM.yyyy").format(Calendar.getInstance().getTime());
            PicName=PicPath.substring(PicPath.lastIndexOf("/"));
            new File(ImgDir).mkdirs();
            new File(ImgDir+ip).mkdirs();
            new File(ImgDir+ip+"/"+day).mkdirs();
            ComplPath=ImgDir+ip+"/"+day+PicName;
            ComplPath=ComplPath.replaceAll("//","/");
            writer.println("Pfad:"+ComplPath);
            //read image
            BufferedImage img=ImageIO.read(ImageIO.createImageInputStream(CliSock.getInputStream()));
            writer.println("Image gelesen:"+img.getHeight()+"/"+img.getWidth());
            //save image with name in directory
            ImageIO.write(img, "JPG", new File(ComplPath));
            writer.println("Image gespeichert:"+ComplPath);            
            CliSock.close();
        } catch (IOException e) {
            writer.println("Exception in handleClient():"+e);
        } finally {
            writer.close();
        }      
    }
    
    public String takePicture(String ImgDir){
        try {
            String PicPath=hsc.takePicture(ImgDir);
            return PicPath;
        } catch (Exception e) {
            System.err.println("Exception in takePicture " + e);
        }
        return "";
    }
    
    public boolean sendPicture(String PicPath){
        try {
            hsc.sendPicture("192.168.0.110", PicPath);
            return true;
        } catch (Exception e) {
            System.err.println("Exception in send Picture" + e);
        }
        return false;
    }
    
    public boolean getService(String ip){
        try {
            URL wsdl = new URL("http://"+ip+":8180/HomeSecClientService?wsdl");
            QName serviceName = new QName("http://HomeSec/", "HomeSecClientService");
            Service service = Service.create(wsdl, serviceName);
            hsc = service.getPort(HomeSecClient.class);
            return true;
        } catch (Exception e) {
            System.err.println("Exception in getService" + e);
            //End Thread if Client could be connected
            Thread.currentThread().interrupt();
            return false;          
        }
    }   
}
