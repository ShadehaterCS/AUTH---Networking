package com.auth.networks;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
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
    public static ConcurrentHashMap<String, Account> UsernamesToAccountsMap = new ConcurrentHashMap<>();
    public static volatile int activeConnections = 0;

    private int incrementalID;

    public MailServer() {
        incrementalID = 0;
    }

    public void coldStart() {
        try {
            Account a = new Account("shadehater", "liakouras", UUID.randomUUID().toString());
            a.receiveEmail(new Email("Liako bro","gamw re file", "mellontikos",
                    "shadehater", "TI WRA"));
            a.receiveEmail(new Email("pills","get some dogecoin and gme please", "liakouras",
                    "shadehater", "TI WRA"));

            UsernamesToAccountsMap.put("shadehater", a);
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
    public static synchronized void logMessage(String type, String status, String username){
        StringBuilder logBuild = new StringBuilder();
        logBuild.append(LocalDateTime.now());
        logBuild.append(" : ");
        logBuild.append(type);
        logBuild.append(" operation ");
        logBuild.append(status);
        logBuild.append("with username: ");
        logBuild.append(username);
        String log = logBuild.toString();
        MailServer.serverLog.add(log);
    }
}