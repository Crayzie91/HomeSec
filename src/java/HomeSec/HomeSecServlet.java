package HomeSec;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Kevin Haves
 */
@WebServlet(urlPatterns = {"/HomeSecServer"})
public class HomeSecServlet extends HttpServlet {
    private final HomeSecSystem sys = new HomeSecSystem ("HomeSecServer");
    String ImgDir="/home/pi/Pictures/Images/";
    
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String output;
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        //Connects with all cameras (ArrayID=0) to get current pictures
        if(sys.connectSocket(ImgDir,0))       
            output = sys.createJSON(ImgDir);
        else
            output="error";
        
        try {
            out.println(output);
        } finally {
            out.close();
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * Creates a new camera entry. Connects to web service of client
     * to take a picture. Opens Socket to reveive it. Creates JSON with update.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String output;
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        //Read IP of new camera and ID of new entry
        String ClientIp = request.getParameter("ip");
        
        if(ClientIp.equals(request.getLocalAddr())) ClientIp="localhost";
        //Check if IP format is valid
        if(sys.validIP(ClientIp) || ClientIp.equals("localhost")){
            //Check if client is reachable
            if(InetAddress.getByName(ClientIp).isReachable(1000)){
                //Create Entry
                int CamID=sys.createEntry(ClientIp);

                //Connects with new camera to get first picture(CamID is set)    
                if(sys.connectSocket(ImgDir,CamID)){        
                    //create and send JSON
                    output = sys.createJSON(ImgDir);
                }
                else
                    output="error";
            }
            else
                output="client";
        }
        else
            output="format";
        
        try {
            out.println(output);
        } finally {
            out.close();
        }
    }

     /**
     * Handles the HTTP <code>DELETE</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String output;
        int id=Integer.valueOf(request.getHeader("id"));
        
        if(sys.deleteEntry(id))
            output = sys.createJSON(ImgDir);
        else
            output = "error";
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();       
        out.println(output);           
    }
}
