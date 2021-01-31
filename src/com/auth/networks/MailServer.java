package com.auth.networks;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class MailServer {
    public static void main(String[] args) {
        MailServer server = new MailServer();
        server.coldStart();
    }

    public static Vector<String> serverLog = new Vector<>(100);
    public static Vector<Thread> activeThreads = new Vector<>();
    public static ConcurrentHashMap<String, String> usersPublicKeysMap = new ConcurrentHashMap<>();
    public static Vector<User> usersList = new Vector<>(10);
    public static volatile int activeConnections = 0;

    private int incrementalID;

    public MailServer() {
        incrementalID = 0;
    }

    public void coldStart() {
        try {
            ServerSocket serverSocket = new ServerSocket(3000);
            while (true) {
                Socket clientSocket;
                clientSocket = serverSocket.accept();
                Thread clientThread = new ClientThread(clientSocket, clientSocket.getInputStream(),
                        clientSocket.getOutputStream(), incrementalID++);
                clientThread.start();
                activeThreads.add(clientThread);

                System.out.println("New client connected with id: " + activeThreads.indexOf(clientThread));
                System.out.println("---------------------");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    This class is meant to be run in separate threads for each individual client
    Handles every request from the bound client
     */
    class ClientThread extends Thread {
        private final Socket clientSocket;
        private final ObjectInputStream inputStream;
        private final ObjectOutputStream outputStream;
        public int id;

        public ClientThread(Socket clientSocket, InputStream inputStream, OutputStream outputStream, int id) throws IOException {
            this.clientSocket = clientSocket;
            this.inputStream = new ObjectInputStream(inputStream);
            this.outputStream = new ObjectOutputStream(outputStream);
            this.id = id;
        }

        @Override
        public void run() {
            String request;
            try {
                while ((request = (String) inputStream.readObject()) != null) {
                    if (!request.isEmpty())
                        parseRequest(request);
                }
            } catch (Exception e) {
                System.err.println("Client Disconnected");
                synchronized (this) {
                    MailServer.activeConnections--;
                    MailServer.activeThreads.remove(this);
                }
                try {
                    this.join();
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }

        private void parseRequest(String request) throws IOException, ClassNotFoundException {
            System.out.println("PARSING REQUEST: " + request);
            if (request.equals("REGISTER"))
                registerNewUser();
        }

        private void registerNewUser() throws IOException, ClassNotFoundException {
            String username = (String) inputStream.readObject();
            if (usersPublicKeysMap.containsKey(username)) { //Check if username already exists
                outputStream.writeObject(-1);
                Logger.logMessage("REGISTER", "FAILED", "NULL");
                return;
            }
            outputStream.writeObject(0);
            String password = (String) inputStream.readObject();
            username += "@csd.auth.gr";
            String uniqueID = UUID.randomUUID().toString();
            MailServer.usersList.add(new User(username, password, uniqueID));
            MailServer.usersPublicKeysMap.put(username, uniqueID);
            Logger.logMessage("REGISTER", "SUCCEEDED", username);
        }

        /*
        Will search in the set and
         */
        private void getUserPublicKey(String userId) throws IOException {
            String publicKey = MailServer.usersPublicKeysMap.get(userId);
            if (!publicKey.isEmpty())
                outputStream.write(publicKey.getBytes(StandardCharsets.UTF_8));
            else
                System.out.println("User ID not found");
        }
    }
}