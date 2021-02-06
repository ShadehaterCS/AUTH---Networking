package com.auth.networks;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MailServer {
    public static void main(String[] args) {
        MailServer server = new MailServer();
        server.coldStart();
    }

    public static ArrayList<Thread> activeThreads = new ArrayList<>();
    public static final ConcurrentHashMap<String, Account> UsernamesToAccountsMap = new ConcurrentHashMap<>();
    public static volatile int activeConnections = 0;
    private static int emailsIdCount;


    private int incrementalID;

    private static ArrayList<String> activityLog;

    public MailServer() {
        incrementalID = 0;
        activityLog = new ArrayList<>();
    }

    public static void log(String operationData) {
        activityLog.add(operationData);
        System.out.println(operationData);
    }

    public void coldStart() {
        try {
            initializeForTesting();
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

    public synchronized static int getNewEmailID(){
        return emailsIdCount++;
    }

    private void initializeForTesting(){
        Account a = new Account("shadehater", "liakouras", UUID.randomUUID().toString());
        a.receiveEmail(new Email("Liako bro","gamw re file", "Greg",
                "shadehater", "TI WRA"));
        Account b = new Account("greg", "greg", UUID.randomUUID().toString());
        b.receiveEmail(new Email("Grhgorakh", "omorfia mu esu", "shadehater",
                "greg", "TI WRA"));
        UsernamesToAccountsMap.put("shadehater", a);
        UsernamesToAccountsMap.put("greg", b);
    }
}