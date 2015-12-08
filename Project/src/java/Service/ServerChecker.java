/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Octaviano
 */
public class ServerChecker {

    private static Path directory;

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {

        Socket socket;
        HashSet<Integer> offPORTS = new HashSet<>();
        String host = null;
        int serverPort = -1;

        File fXmlFile = new File("C:\\Users\\Octaviano\\Desktop\\Project\\src\\java\\Service\\Configuration.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("server");

        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ServerChecker.class.getName()).log(Level.SEVERE, null, ex);
            }

            for (int count = 0; count < nList.getLength(); count++) {
                Node nNode = nList.item(count);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    try {

                        host = eElement.getElementsByTagName("ip").item(0).getTextContent();
                        serverPort = Integer.parseInt(eElement.getElementsByTagName("port")
                                .item(0).getTextContent());
                        System.out.println("server connecting to " + serverPort);
                        socket = new Socket(host, serverPort);
                        socket.close();

                        if (offPORTS.contains(serverPort)) {
                            System.out.println("server: " + serverPort + " is now ONLINE");
                            offPORTS.remove(serverPort);
                            System.out.println("deleting all its file...");
                            String path = eElement.getElementsByTagName("directory").item(0).getTextContent();
                            directory = Paths.get(path, Integer.toString(serverPort));
                            
                            try {
                                Files.newDirectoryStream(directory).forEach(file -> {
                                    try {
                                        Files.delete(file);
                                    } catch (IOException exx) {
                                        throw new UncheckedIOException(exx);
                                    }
                                });
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } catch (Exception e) {

                        System.out.println("server: " + serverPort + " is OFFLINE");
                        System.out.println("add to offports");
                        offPORTS.add(serverPort);
                        e.printStackTrace();

                    }

                }
            }

        }
    }
}
