package Server;

import Protocol.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Octaviano
 */
public class ServerDeployer {
    
    private FileServer[] servers;
    private ExecutorService pool;
    
    public ServerDeployer() throws IOException, InterruptedException {
        pool = Executors.newFixedThreadPool(Protocol.SERVER_NUMBER);
        servers = new FileServer[Protocol.SERVER_NUMBER];

        /*Start all servers*/
        for (int i = 0; i < Protocol.SERVER_NUMBER; i++) {
            servers[i] = new FileServer(1099 + i);
            start(servers[i]);
            Thread.sleep(500);
        }
    }

    /**
     * Closes all file servers and shutdowns the thread pool.
     *
     * @throws java.io.IOException If there is a problem closing file servers
     * and pool shutdown
     */
    public void close() throws IOException {
        System.out.println("Stopping all file servers...");
        for (FileServer server : servers) {
            server.stop();
        }
        pool.shutdown();
        System.out.println("Thread pool shutdown.");
    }
    
    public FileServer[] getServers() {
        return servers;
    }
    
    public final void start(FileServer server) {
        pool.execute(server);
    }
    
    public static void main(String[] args) {
        try {
            ServerDeployer servers = new ServerDeployer();
            System.out.println("File servers initialized...");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                String input;
                do {
                    Thread.sleep(2000);
                    System.out.println();
                    Arrays.stream(servers.servers)
                            .forEach(server -> System.out.println(server.getPort() + " " + (server.isClosed() ? "closed" : "open")));
                    System.out.println("Syntax [stop|start] <port number> or \"exit\" to quit.");
                    System.out.print("Enter input: ");    //stop 1099
                    input = br.readLine();

                    /*Get the file server based on the second input after "stop" or "start" keyword*/
                    if (input != null && !"exit".equalsIgnoreCase(input)) {
                        String[] token = input.split("\\s");
                        int port = Integer.parseInt(token[1]);
                        FileServer server = Arrays.stream(servers.servers)
                                .filter(fileServer -> fileServer.getPort() == port)
                                .findAny()
                                .get();

                        /*Operation commands based on the first argument*/
                        if ("stop".equalsIgnoreCase(token[0])) {
                            server.stop();
                        } else if ("start".equalsIgnoreCase(token[0])) {
                            servers.pool.execute(server);
                        }
                    }
                } while (input != null && !"exit".equalsIgnoreCase(input));
            }
            servers.close();
            System.out.println("Program exit.");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
