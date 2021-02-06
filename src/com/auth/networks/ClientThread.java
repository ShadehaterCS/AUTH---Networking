package com.auth.networks;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
/**
 *  A clientThread handles the POST requests from a client
 *  It binds to a client through the clientSocket and in/output streams
 *  Every method is mirrored to the client
 *
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

    /**
     * Infinite loop to wait for the requests
     * Will break if the client gets disconnected
     * The thread removes itself from the activeThreads list in MailServer and the
     */
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
            MailServer.disconnectThread(this);
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
        //Check if username already exists
        if (MailServer.UsernamesToAccountsMap.containsKey(username)) {
            outputStream.writeObject(-1);
            return;
        }
        outputStream.writeObject(0);
        String uniqueID = UUID.randomUUID().toString();
        Account account = new Account(username, password, uniqueID);
        synchronized (MailServer.UsernamesToAccountsMap) {
            MailServer.UsernamesToAccountsMap.put(username, account);
            MailServer.log("SIGNUP Operation successful for username: "+username);
        }
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
        MailServer.log("LOGIN Operation successful for username: "+username);
    }

    /**
     * @use Gets the Email object from the client and calls the synchronized
     *      method receiveEmail on the recipient account.
     *      Synchronization is required because multiple emails from multiple threads could be
     *      received at the same time.
     */
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
        MailServer.log("COMPOSE Operation successful for : "+ connectedUsername + " -> " + recipient);
    }

    private void userInbox() throws Exception {
        Account acc = MailServer.UsernamesToAccountsMap.get(connectedUsername);
        ArrayList<Email> emails = acc.getPersonalEmails();
        outputStream.writeObject(emails);
        outputStream.reset();
        MailServer.log("Sent INBOX to user: "+connectedUsername);
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
        boolean read = connectedUserAccount.readEmail(eid);
        if (!read)
            MailServer.log("READ Operation failed, email not found in collection. Probably a sync error");
        else
            MailServer.log("READ Operation successful for email with id: "+eid);
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
        boolean removed = connectedUserAccount.deleteEmail(eid);
        if (removed)
            MailServer.log("DELETE Operation successful with for email-id: " + eid);
        else
            MailServer.log("DELETE operation failed for email-id: " + eid);
    }

    private void logout() throws Exception {
        connectedUsername = null;
        connectedUserAccount = null;
        userLoggedIn = false;
        outputStream.writeObject(0);
    }
}