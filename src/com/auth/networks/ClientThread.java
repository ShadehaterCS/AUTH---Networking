package com.auth.networks;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.Vector;

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
    public int threadID;

    public ClientThread(Socket clientSocket, InputStream inputStream, OutputStream outputStream, int id) throws IOException {
        this.clientSocket = clientSocket;
        this.inputStream = new ObjectInputStream(inputStream);
        this.outputStream = new ObjectOutputStream(outputStream);
        this.threadID = id;
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
        System.out.println("EXECUTING REQUEST: " + request + " from client with id: " + threadID);
        switch (request) {
            case "REGISTER" -> registerNewUser();
            case "LOGIN" -> userLogin();
            case "NEW-EMAIL" -> newEmailBetweenUsers();
            case "INBOX" -> userInbox();
            case "READ-EMAIL" -> readEmail();
            case "DELETE-EMAIL" -> deleteEmail();
            case "LOGOUT" -> logout();
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
            return;
        }
        outputStream.writeObject(response);
        String uniqueID = UUID.randomUUID().toString();
        Account account = new Account(username, password, uniqueID);
        MailServer.UsernamesToAccountsMap.put(username, account);
    }

    private void userLogin() throws Exception {
        if (connectedUsername != null) {
            outputStream.writeObject(-1);
            return;
        }
        String username = (String) inputStream.readObject();
        String password = (String) inputStream.readObject();
        //if the username or password provided is wrong then send a fail code
        if (!MailServer.UsernamesToAccountsMap.containsKey(username)
        || !MailServer.UsernamesToAccountsMap.get(username).getPassword().equals(password)) {
            outputStream.writeObject(-1);
            return;
        }
        outputStream.writeObject(0);
        connectedUsername = username;
        userLoggedIn = true;
    }

    private void newEmailBetweenUsers() throws Exception {
        String recipient = (String) inputStream.readObject();
        if (!MailServer.UsernamesToAccountsMap.containsKey(recipient)) {
            outputStream.writeObject(-1);
            return;
        }
        outputStream.writeObject(0); //recipient exists
        Email email = (Email) inputStream.readObject();
        Account recipientAccount = MailServer.UsernamesToAccountsMap.get(recipient);
        recipientAccount.receiveEmail(email);
    }

    private void userInbox() throws Exception{
        Account acc = MailServer.UsernamesToAccountsMap.get(connectedUsername);
        Vector<Email> emails = acc.getPersonalEmails();
        outputStream.writeObject(emails);
        outputStream.reset();
    }

    /*
        Gets an int value from the client
        If -1 then client tried to read invalid index
        Synchronizes the associated account's email with read status
    */
    private void readEmail() throws Exception {
        outputStream.writeObject(MailServer.UsernamesToAccountsMap.get(connectedUsername).getPersonalEmails());
        outputStream.reset();
        int index = (Integer) inputStream.readObject();
        if (index == -1)
            return;
        Account acc = MailServer.UsernamesToAccountsMap.get(connectedUsername);
        acc.getPersonalEmails().elementAt(index).read();
        System.out.println("Updated e-mail at index: "+ index);
    }

    private void deleteEmail() throws Exception{
        int index = (Integer) inputStream.readObject();
        Account acc = MailServer.UsernamesToAccountsMap.get(connectedUsername);
        Vector<Email> emails = acc.getPersonalEmails();
        if (index < 0 || index >= emails.size())
            outputStream.writeObject(-1);
        emails.removeElementAt(index);
        outputStream.writeObject(0);
        System.out.println("Deleted e-mail at index: "+ index);
    }

    private void logout() throws Exception{
        connectedUsername = null;
        userLoggedIn = false;
        outputStream.writeObject(0);
    }
}