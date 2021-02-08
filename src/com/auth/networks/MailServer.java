package com.auth.networks;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MailServer {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        MailServer server = new MailServer(port);
        server.coldStart();
    }

    public static ArrayList<ClientHandler> activeThreads = new ArrayList<>();
    public static volatile int activeConnections = 0;

    public static final ConcurrentHashMap<String, Account> UsernamesToAccountsMap = new ConcurrentHashMap<>();
    private static int emailsIdCount;

    private int incrementalID;
    private int port;
    private static ArrayList<String> activityLog;

    public MailServer(int port) {
        incrementalID = 0;
        this.port = port;
        activityLog = new ArrayList<>();
    }

    public static void log(String operationData) {
        activityLog.add(operationData);
        System.out.println(operationData);
    }

    public void coldStart() {
        try {
            initializeForTesting();
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server is online");
            while (true) {
                Socket clientSocket;
                clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientSocket.getInputStream(),
                        clientSocket.getOutputStream(), incrementalID++);
                clientHandler.start();
                activeThreads.add(clientHandler);

                System.out.println("New client connected with id: " + activeThreads.indexOf(clientHandler));
                System.out.println("---------------------");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void disconnectThread(ClientHandler t) {
        MailServer.activeConnections--;
        MailServer.activeThreads.remove(t);
    }

    public synchronized static int getNewEmailID(){
        return emailsIdCount++;
    }

    private void initializeForTesting(){
        Account student1 = new Account("eliaskordoulas@csd.auth.gr",
                String.valueOf("liakouras".hashCode()), UUID.randomUUID().toString());
        Account student2 = new Account("gregMan@csd.auth.gr",
                String.valueOf("gregoulis".hashCode()), UUID.randomUUID().toString());

        student1.receiveEmail(new Email("Themata diktuwn", "idk",
                student2.getUserName(),student1.getUserName(), LocalDate.now().toString()));
        student1.receiveEmail(new Email("Life lessons", "not much",
                student2.getUserName(),student1.getUserName(), LocalDate.now().toString()));
        student1.receiveEmail(new Email("Exam season mean", "NaN",
                student2.getUserName(),student1.getUserName(), LocalDate.now().toString()));

        student2.receiveEmail(new Email("RE:Exam season mean", "That's mean",
                student1.getUserName(),student2.getUserName(), LocalDate.now().toString()));
        student2.receiveEmail(new Email("Ergasia diktywn", "That was fun and nice",
                student1.getUserName(),student2.getUserName(), LocalDate.now().toString()));
        student2.receiveEmail(new Email("Java", "Here's 10 things you didn't know about coffee:",
                student1.getUserName(),student2.getUserName(), LocalDate.now().toString()));

        MailServer.UsernamesToAccountsMap.put("eliaskordoulas@csd.auth.gr", student1);
        MailServer.UsernamesToAccountsMap.put("gregMan@csd.auth.gr", student2);
        System.out.println("Initial data complete");
    }
}