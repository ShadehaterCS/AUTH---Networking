package com.auth.networks;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

/*
    This class is meant to be run in separate threads for each individual client
    Handles every request from the bound client
     */
public class ClientThread extends Thread {
    private final Socket clientSocket;
    private final ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;
    private String connectedUsername;
    boolean userLoggedIn;
    public int id;

    public ClientThread(Socket clientSocket, InputStream inputStream, OutputStream outputStream, int id) throws IOException {
        this.clientSocket = clientSocket;
        this.inputStream = new ObjectInputStream(inputStream);
        this.outputStream = new ObjectOutputStream(outputStream);
        this.id = id;
        connectedUsername = null;
        userLoggedIn = false;
    }

    @Override
    public void run() {
        String request;
        try {
            while ((request = (String) inputStream.readObject()) != null) {
                if (!request.isEmpty())
                    if (!userLoggedIn && (!request.equals("REGISTER") && !request.equals("LOGIN"))) //guarantees a user is logged in
                        outputStream.writeObject(-1);
                    else if (userLoggedIn && request.equals("LOGIN")) //special case
                        outputStream.writeObject(100);
                    else {
                        outputStream.writeObject(0);
                        parseRequest(request);
                    }
            }
        } catch (Exception e) {
            System.err.println("Client Disconnected");
            e.printStackTrace();
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

    private void parseRequest(String request) throws Exception {
        System.out.println("PARSING REQUEST: " + request);
        switch (request) {
            case "REGISTER" -> registerNewUser();
            case "LOGIN" -> userLogin();
            case "NEW-EMAIL" -> newEmailBetweenUsers();
            case "INBOX" -> userInbox();
            case "READ-EMAIL" -> readEmail();
        }
    }

    private void registerNewUser() throws Exception {
        String username = (String) inputStream.readObject();
        String password = (String) inputStream.readObject();
        int response = 0;
        if (MailServer.UsernamesToAccountsMap.containsKey(username)) { //Check if username already exists
            System.out.println(username + " " + password);
            response = -1;
            outputStream.writeObject(response);
            MailServer.logMessage("REGISTER", "FAILED", "ALREADY_EXISTS");
            return;
        }
        outputStream.writeObject(response);
        String uniqueID = UUID.randomUUID().toString();
        Account account = new Account(username, password, uniqueID);
        MailServer.UsernamesToAccountsMap.put(username, account);
        MailServer.logMessage("REGISTER", "SUCCEEDED", username);
        outputStream.flush();
    }

    private void userLogin() throws Exception {
        if (connectedUsername != null) {
            outputStream.writeObject(-1);
            MailServer.logMessage("LOGIN", "FAILED", "NONE");
            return;
        }
        String username = (String) inputStream.readObject();
        String password = (String) inputStream.readObject();
        //if the username or password provided is wrong then send a fail code
        if (!MailServer.UsernamesToAccountsMap.containsKey(username)
        || !MailServer.UsernamesToAccountsMap.get(username).getPassword().equals(password)) {
            outputStream.writeObject(-1);
            MailServer.logMessage("LOGIN", "FAILED", username);
            return;
        }
        outputStream.writeObject(0);
        connectedUsername = username;
        userLoggedIn = true;
        MailServer.logMessage("LOGIN", "SUCCEEDED", connectedUsername);
    }

    private void newEmailBetweenUsers() throws Exception {
        String recipient = (String) inputStream.readObject();
        if (!MailServer.UsernamesToAccountsMap.containsKey(recipient)) {
            outputStream.writeObject(-1);
            MailServer.logMessage("NEW-EMAIL", "FAILED", connectedUsername);
            return;
        }
        outputStream.writeObject(0); //recipient exists
        Email email = (Email) inputStream.readObject();
        Account recipientAccount = MailServer.UsernamesToAccountsMap.get(recipient);
        recipientAccount.receiveEmail(email);
        MailServer.logMessage("NEW-EMAIL", "SUCCEEDED", connectedUsername);
    }

    private void userInbox() throws Exception{
        Account acc = MailServer.UsernamesToAccountsMap.get(connectedUsername);
        outputStream.writeObject(acc.getPersonalEmails());
    }

    /*
        Gets an int value from the client
        If -1 then client tried to read invalid index
        Synchronizes the associated account's email with read status
    */
    private void readEmail() throws Exception {
        int index = (Integer) inputStream.readObject();
        if (index == -1)
            return;
        Account acc = MailServer.UsernamesToAccountsMap.get(connectedUsername);
        acc.getPersonalEmails().elementAt(index).read();
        System.out.println("Updated e-mail at index: "+ index);
    }
}