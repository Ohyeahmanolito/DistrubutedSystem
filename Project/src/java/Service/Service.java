/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Service;

import Protocol.Protocol;
import Server.ServerInfo;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Octaviano
 */
public class Service {

    private static Path directory;

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, InterruptedException {
        int totalServerNumber = 0;
        List<ServerInfo> information = new ArrayList<>();
        List<ServerInfo> offServerList = new ArrayList<>();
        int active_servers = 0;

        createDirectory();

        try {
            Files.lines(Paths.get("Configuration")).forEach(line -> {
                String[] token = line.split("\\s+");
                information.add(new ServerInfo(token[0], Integer.parseInt(token[1])));
            });
            totalServerNumber = information.size();
        } catch (IOException ex) {
            System.out.println("error in parsing " + ex);
        }

        int two_third_server = (totalServerNumber * 2) / 3;

        // start of monitoring the servers.
        while (true) {
            active_servers = 0;
            Thread.sleep(5000);
            //starting PORT is 1099
            //checking if server is running
            for (int count = 0; count < two_third_server; count++) {
                ServerInfo currentServer = information.get(count);

                try (Socket connection = currentServer.getSocket()) {
                    active_servers++;
                    System.out.println("active bank: " + currentServer);
                    if (offServerList.contains(currentServer)) {
                        offServerList.remove(currentServer);
                        System.out.println("removing that connection");
                        OutputStream output = connection.getOutputStream();
                        output.write(Protocol.DELETE_ALL);
                        output.flush();
                    }

                } catch (SocketException sEX) {
                    System.out.println(currentServer + " is close");

                    //if server is not in the offserverList, add it
                    if (!offServerList.contains(currentServer)) {
                        offServerList.add(currentServer);
                    }
                } catch (Exception ex) {
                    System.out.println("server " + ex);
                    //
                }
            }

            //==============================
            if (active_servers != two_third_server) {
                System.out.println("active servers: " + active_servers);

                int kulang_na_server = two_third_server - active_servers;

                for (int count = 0; count < totalServerNumber; count++) {
                    ServerInfo currentServer = information.get(count);
                    if (!offServerList.contains(currentServer)) {
                        Set<String> listOfFiles = null;

                        //download all its file in the repository.
                        System.out.println(currentServer);
                        try (Socket socket = currentServer.getSocket();
                                OutputStream output = socket.getOutputStream()) {
                            output.write(Protocol.FILE_LIST);
                            output.flush();
                            //=============================
                            try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                                System.out.println("Getting list of files from " + currentServer);
                                listOfFiles = (Set<String>) input.readObject();

                            } catch (Exception ex) {
                                System.out.println(ex);
                            }
                            //=============================
                        } catch (Exception ex) {
                            System.out.println("getting file list error " + ex);
                        }

                        //=============================
                        for (String fileName : listOfFiles) {
                            try (Socket socket = currentServer.getSocket();
                                    OutputStream output = socket.getOutputStream();) {
                                System.out.println("file downloading...");
                                fileDownload(fileName, output, socket);
                                //download once

                            } catch (Exception ex) {
                                System.out.println("getting error in socket connection " + ex);
                            }
                        }
                        //kung ilan ang kulang ipasa sa mga back-up
                        for (int counter = 0; counter < kulang_na_server; counter++) {
                            for (String fileName : listOfFiles) {
                                try (Socket socket = information.get(two_third_server + counter).getSocket();
                                        OutputStream output = socket.getOutputStream();) {
                                    System.out.println("uploading in " + socket.getPort());
                                    fileUpload(fileName, output);
                                }
                            }
                        }
                        break;
                        //=============================

                    }
                }
            }
        }
    }

    public static void fileDownload(String fileName, OutputStream output, Socket socket) throws IOException {
        PrintWriter writer = new PrintWriter(output);
        output.write(Protocol.DOWNLOAD);
        writer.println(fileName);
        writer.flush();

        try {
            InputStream input = socket.getInputStream();
            int input_read = input.read();
            if (1 == input_read) {
                System.out.println("Byte read: " + input_read);
                long size = Files.copy(input, directory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File " + fileName + " was uploaded to " + socket.getPort() + " with the size of " + size);

            }
        } catch (Exception ex) {
            System.out.println("Error occured in fileDownload method " + ex);
        }
    }

    private static void fileUpload(String fileName, OutputStream outputStream) throws IOException {

        PrintWriter pw = new PrintWriter(outputStream);
        outputStream.write(Protocol.UPLOAD);
        pw.println(fileName);
        pw.flush();

        InputStream is = Files.newInputStream(directory.resolve(fileName));
        transferBytes(is, outputStream);
        System.out.println(fileName + " is uploaded");

    }

    private static int transferBytes(InputStream is, OutputStream os) throws IOException {
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

    public static void receiveFile(InputStream inputStream, Path directory, String filename) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            Files.copy(inputStream, directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createDirectory() throws IOException {
        directory = Paths.get("C:\\Users\\Octaviano\\Desktop\\Operating_System", "Databank");

        /*Create directory to serve as the file repository of the file server*/
        if (Files.notExists(directory)) {
            Files.createDirectories(directory);
        }
    }
}
