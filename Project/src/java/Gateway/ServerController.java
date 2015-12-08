package Gateway;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import Server.ServerDeployer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

/**
 *
 * @author Octaviano
 */
@ManagedBean(eager = false)
@ApplicationScoped
public class ServerController {

    private ServerDeployer serverDeployer;

    @PostConstruct
    public void init() {
        try {
            serverDeployer = new ServerDeployer();
        } catch (IOException ex) {
            Logger.getLogger(ServerController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ServerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            serverDeployer.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ServerDeployer getFarm() {
        return serverDeployer;
    }
}
