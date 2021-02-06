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

    private Account connectedUserAccount;

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
                    //guarantees a user is logged in, even if the client says so
                    if (!userLoggedIn && (!request.equals("SIGNUP") && !request.equals("LOGIN")))
                        outputStream.writeObject(-1);
                    else {
                        outputStream.writeObject(0);
                        executeRequest(request);
                    }
            }
        } catch (Exception e) {
            System.err.println("Client Disconnected");
            e.printStackTrace();
            synchronized (this) {
                MailServer.activeConnections--;
                MailServer.activeThreads.remove(this);
            }
        }
    }

    private void executeRequest(String request) throws Exception {
        System.out.println("EXECUTING REQUEST: " + request + " from client with id: " + threadID);
        switch (request) {
            case "SIGNUP" -> userSignUp();
            case "LOGIN" -> userLogin();
            case "COMPOSE" -> newEmailBetweenUsers();
            case "INBOX" -> userInbox();
            case "READ" -> readEmail();
            case "DELETE" -> deleteEmail();
            case "LOGOUT" -> logout();
            default -> outputStream.writeObject(-1);
        }
    }

    private void userSignUp() throws Exception {
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
        connectedUserAccount = MailServer.UsernamesToAccountsMap.get(username);
        userLoggedIn = true;
    }

    private void newEmailBetweenUsers() throws Exception {
        String recipient = (String) inputStream.readObject();
        System.out.println("Got recipient: " + recipient);
        if (!MailServer.UsernamesToAccountsMap.containsKey(recipient)) {
            outputStream.writeObject(-1);
            return;
        }
        outputStream.writeObject(0); //recipient exists
        Email email = (Email) inputStream.readObject();
        Account recipientAccount = MailServer.UsernamesToAccountsMap.get(recipient);
        recipientAccount.receiveEmail(email);
    }

    private void userInbox() throws Exception {
        Account acc = MailServer.UsernamesToAccountsMap.get(connectedUsername);
        Vector<Email> emails = acc.getPersonalEmails();
        outputStream.writeObject(emails);
        outputStream.reset();
    }

    /*
        Gets an int value from the client that represents the emailId
        If -1 then client tried to read invalid index
        If emailId is not found that means the client has a stale email collection and has to resync
    */
    private void readEmail() throws Exception {
        outputStream.reset();
        int eid = (int) inputStream.readObject();
        if (eid == -1) {
            MailServer.log("Canceled READ, user input was bad");
            return;
        }
        Account acc = MailServer.UsernamesToAccountsMap.get(connectedUsername);
        Email selected = acc.getPersonalEmails().stream()
                .filter(e -> e.getEmailId() == eid)
                .findFirst()
                .orElse(null);
        if (selected == null) {
            MailServer.log("READ Operation failed, email not found in collection. Probably a sync error");
            return;
        }
        selected.read();
    }

    /*
    Handles a client's request to delete an email by its id
    */
    private void deleteEmail() throws Exception {
        int eid = (int) inputStream.readObject();
        if (eid == -1) {
            MailServer.log("Canceled DELETE, user input was bad");
            return;
        }
        Vector<Email> emails = connectedUserAccount.getPersonalEmails();
        boolean removed = emails.removeIf(e -> e.getEmailId() == eid);
        outputStream.writeObject(removed);
        if (removed)
            MailServer.log("DELETE Operation successful with for email-id: "+eid);
        else
            MailServer.log("DELETE operation failed for email-id: "+eid);
    }

    private void logout() throws Exception {
        connectedUsername = null;
        userLoggedIn = false;
        outputStream.writeObject(0);
    }
}