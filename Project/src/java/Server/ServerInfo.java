/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

/**
 *
 * @author Octaviano
 */
public class ServerInfo {

    private final String ipAddress;
    private final int port;

    public ServerInfo(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.ipAddress);
        hash = 19 * hash + this.port;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ServerInfo) {
            final ServerInfo other = (ServerInfo) obj;
            return Objects.equals(ipAddress, other.ipAddress) && port == other.port;
        }
        return false;
    }

    public Socket getSocket() throws IOException {
        return new Socket(ipAddress, port);
    }

    @Override
    public String toString() {
        return ipAddress + ':' + port;
    }

    public int getPort() {
        return port;
    }
}
