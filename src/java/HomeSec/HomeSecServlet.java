package HomeSec;

import java.io.IOException;
import java.io.PrintWriter;
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
        String caller_ip = request.getRemoteAddr();
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        //Connects with all cameras (ArrayID=0) to get current pictures
        sys.connectSocket(ImgDir,0);
        
        String table = sys.createJSON(ImgDir);
        try {
            out.println(table);
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
        String table;
        PrintWriter out = response.getWriter();
        //Read IP of new camera and ID of new entry
        String ClientIp = request.getParameter("ip");       
        
        if(sys.validIP(ClientIp)){
            if(ClientIp.equals(request.getLocalAddr())) ClientIp="localhost";
            int CamID=sys.createEntry(ClientIp);
        
            //Connects with new camera to get first picture(CamID is set)    
            if(sys.connectSocket(ImgDir,CamID)){        
                //create and send JSON
                table = sys.createJSON(ImgDir);
            }
            else
                table="client";
        }
        else
            table="format";
        
        try {
            out.println(table);
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
        int id=Integer.valueOf(request.getHeader("id"));
        sys.deleteEntry(id);
        String table = sys.createJSON(ImgDir);
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();       
        out.println(table);           
    }
}
