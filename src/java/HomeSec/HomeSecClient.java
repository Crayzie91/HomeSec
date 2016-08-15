package HomeSec;

import javax.jws.WebService;

/**
 * Java Interface defines Client Web Service. 
 * 
 * @author khaves
 */
@WebService
public interface HomeSecClient {
    public String takePicture(String path);
    public boolean sendPicture(String ip, String path);
    public void stopSvc ();
}
