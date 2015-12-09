package Gateway;

import Protocol.Protocol;
import Server.ServerInfo;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Part;

/**
 *
 * @author 1
 */
@ManagedBean
@ApplicationScoped
public class ClientController implements Serializable {

    private Set<String> files;
    private Set<ServerInfo> servers;
    private Part file;

    /**
     * Aggregate file list from servers
     */
    @PostConstruct
    public void init() {
        files = new HashSet<>();
        servers = new LinkedHashSet<>();
        /*Initialize arbitrary list of file servers to connect
         for (int i = 0; i < Protocol.SERVER_NUMBER; i++) {
         servers.add(new ServerInfo("localhost", 1099 + i));
         }*/ /*Receive file list from servers and just upload in web browser
         servers.forEach(server -> {
         try (Socket connection = server.getSocket();
         OutputStream out = connection.getOutputStream()) {
         out.write(Protocol.FILE_LIST);
         out.flush();
         try (ObjectInputStream ois = new ObjectInputStream(connection.getInputStream())) {
         Set<String> fileNames = (Set<String>) ois.readObject();
         files.addAll(fileNames);
         System.out.println("File list retrieved from " + server);
         }
         } catch (ConnectException e) {
         System.out.println(e + " file list from " + server);
         } catch (ClassNotFoundException | IOException ex) {
         Logger.getLogger(ClientController.class.getName()).log(Level.SEVERE, null, ex);
         }
         });*/


    }

    /**
     * Uploads selected file to (2/3) + 1 of servers.
     */
    public void upload() {
        String filename = file.getSubmittedFileName();
        System.out.println("Uploading " + filename + " size " + file.getSize());
        int x = ((2 * Protocol.SERVER_NUMBER) / 3);
        int counter = 0;
        for (ServerInfo server : servers) {

            /*Connect to server*/
            try (OutputStream os = server.getSocket().getOutputStream();
                    PrintWriter pw = new PrintWriter(os);
                    InputStream is = file.getInputStream()) {

                os.write(Protocol.UPLOAD);
                pw.println(filename);
                pw.flush();
                int size = transferBytes(is, os);
                System.out.println("Uploaded " + filename + " at " + server + " size " + size);
                counter++;
                files.add(filename);
                //if 2/3 already. break.

                if (x == counter) {
                    break;
                }
            } catch (ConnectException e) {
                //Socket is closed
                System.out.println(e + " upload at " + server);
            } catch (IOException ex) {
                Logger.getLogger(ClientController.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void download(String fileName) {
        for (ServerInfo info : servers) {
            try (Socket server = info.getSocket();
                    OutputStream os = server.getOutputStream();
                    PrintWriter pw = new PrintWriter(os);
                    InputStream is = server.getInputStream()) {

                /*Protocol*/
                os.write(Protocol.DOWNLOAD);
                pw.println(fileName);
                pw.flush();

                /*Check if file exists, 1 if present, 0 if otherwise*/
                int reply = is.read();
                if (reply == 1) {

                    /*Response header*/
                    FacesContext fc = FacesContext.getCurrentInstance();
                    ExternalContext ec = fc.getExternalContext();
                    ec.responseReset();
                    ec.setResponseContentType(ec.getMimeType(fileName));
                    ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

                    /*Write bytes*/
                    transferBytes(is, ec.getResponseOutputStream());
                    fc.responseComplete();
                    System.out.println("File " + fileName + " was successfully downloaded with size " + file.getSize());
                    break;
                }
            } catch (ConnectException e) {
                //Socket is closed
                System.out.println(e + " download at " + info);
            } catch (IOException ex) {
                Logger.getLogger(ClientController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public Part getFile() {
        return file;
    }

    public Set<String> getFiles() {
        return files;
    }

    /**
     * Registers the file server in this gateway given parameters ip and port
     *
     * @return IP address and port of the file server
     */
    public String register() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        Map<String, String> parameter = ec.getRequestParameterMap();
        ServerInfo server = new ServerInfo(parameter.get("ip"), Integer.parseInt(parameter.get("port")));
        servers.add(server);
        System.out.println("server is add: " + server);
        return server.toString();
    }

    public void setFile(Part file) {
        this.file = file;
    }

    private int transferBytes(InputStream is, OutputStream os) throws IOException {
//        byte[] buffer = new byte[1024];
//        while (is.read(buffer) > -1) {
//            os.write(buffer);
//        }
//        os.flush();
        try (DataInputStream dis = new DataInputStream(is);
                DataOutputStream dos = new DataOutputStream(os)) {
            byte[] buffer = new byte[8192];
            int length;
            int total = 0;
            while ((length = dis.read(buffer, 0, buffer.length)) > 0) {
                dos.write(buffer, 0, length);
                total += length;
            }
            dos.flush();
            return total;
        }
    }
}
