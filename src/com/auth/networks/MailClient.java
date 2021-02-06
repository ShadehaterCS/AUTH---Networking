package com.auth.networks;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;

public class MailClient {
    public static void main(String[] args) {
        try {
            MailClient client = new MailClient();
            client.setUpConnection(args[0], Integer.parseInt(args[1]));
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;
    private final Scanner scanner;
    private String currentUsername;

    private ArrayList<Email> cachedEmails;
    public MailClient() {
        scanner = new Scanner(System.in);
        currentUsername = null;
    }

    /*
    Loop everytime so a user can do stuff
     */
    public void setUpConnection(String host, int port) throws Exception {
        InetAddress a = InetAddress.getByName(host);
        socket = new Socket(host, port);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        cachedEmails = null;
    }

    public void run() throws Exception{
        while(true){
            if (currentUsername == null)
                showLandingMenu();
            else
                showConnectedMenu();
        }
    }

    private void showLandingMenu() throws Exception {
        System.out.println("""
                =================
                MAIL SERVER
                =================""");
        System.out.println("""
                        CONNECTED AS GUEST
                        1 -> Sign Up
                        2 -> Log In
                        3 -> EXIT :(                    
                        """);
        int selection = Integer.parseInt(scanner.nextLine());
        switch (selection){
            case 1 -> signUp();
            case 2 -> login();
            case 3 -> exit();
            default -> System.out.println("Wrong input");
        }
    }

    private void showConnectedMenu() throws Exception{
        System.out.println("""
                =================
                MAIL SERVER
                =================""");
        System.out.printf("""
                CONNECTED AS %s
                1 -> Compose New Email
                2 -> Inbox
                3 -> Read an Email
                4 -> Delete an Email
                5 -> Log out       
                6 -> Exit :(
                """,currentUsername);
        int selection = Integer.parseInt(scanner.nextLine());
        switch (selection){
            case 1-> compose();
            case 2 -> inbox();
            case 3-> readEmail();
            case 4-> deleteEmail();
            case 5 -> logOut();
            case 6 -> exit();
        }
    }

    private boolean POST(String request) throws Exception{
        outputStream.writeObject(request);
        int response = (Integer) inputStream.readObject();
        if (response == -1) {
            System.err.println("You are already logged in");
            return false;
        }
        return true;
    }

    public void signUp() throws Exception {
        if (!POST("SIGNUP"))
            return;
        System.out.println("Enter your username: ");
        outputStream.writeObject(scanner.nextLine());
        System.out.println("Enter your password: ");
        outputStream.writeObject(scanner.nextLine());

        if ((Integer) inputStream.readObject() == -1) {
            System.err.println("Username already exists");
            return;
        }

        System.out.println("Account registered successfully, you can now log in");
    }

    public void login() throws Exception {
        if (!POST("LOGIN"))
            return;
        String[] packet = new String[2];
        System.out.println("Please enter your email address: ");
        String username = scanner.nextLine();
        System.out.println("Please enter your password: ");
        String password = scanner.nextLine();
        //password = String.valueOf(password.hashCode());
        outputStream.writeObject(username);
        outputStream.writeObject(password);
        //Get response if username and password are correct
        if ((Integer) inputStream.readObject() == -1) {
            System.err.println("Wrong username or password");
            return;
        }
        System.out.println("Log in successful");
        currentUsername = username;
    }

    public void compose() throws Exception {
        if (!POST("COMPOSE"))
            return;
        System.out.println("Please enter the recipient's email address: ");
        String recipientUsername = scanner.nextLine();
        outputStream.writeObject(recipientUsername);
        if((int)inputStream.readObject() == -1){
            System.out.println("Invalid recipient address");
            return;
        }

        //recipient exists
        System.out.println("Please enter a subject: ");
        String subject = scanner.nextLine();
        System.out.println("Please type your message: ");
        String message = scanner.nextLine();
        Email email = new Email(subject, message, currentUsername, recipientUsername, LocalDateTime.now().toString());
        outputStream.writeObject(email);
        System.out.println("Email sent successfully");
    }

    /*
    Sync the inbox with the server every time it's called
     */
    public void inbox() throws Exception {
        if (!POST("INBOX"))
            return;
        cachedEmails = (ArrayList<Email>) inputStream.readObject();
        if (cachedEmails.size() == 0) {
            System.out.println("No emails in your inbox");
            return;
        }
        int i = 1;
        System.out.println("ID\t\tFROM\t\tSubject");
        for (Email e : cachedEmails)
            System.out.println(i++ + e.toStringCondensed());
    }

    public void readEmail() throws Exception {
        if (cachedEmails ==null){
            System.out.println("Please sync your inbox before trying to read an email");
            return;
        }
        if (!POST("READ"))
            return;
        System.out.println("Please enter the id of the email you'd like to read: ");
        int index = Integer.parseInt(scanner.nextLine());
        index = index > 0 && index <= cachedEmails.size() ? index - 1 : -1;
        if (cachedEmails == null || index == -1) {
            outputStream.writeObject(-1);
            return;
        }
        Email selected = cachedEmails.get(index);
        outputStream.writeObject(selected.getEmailId());
        System.out.println("STATUS\tFROM\tSubject");
        System.out.println("------------------------");
        selected.read();
        System.out.println(selected.toString());
    }

    public void deleteEmail() throws Exception {
        if (cachedEmails ==null){
            System.out.println("Please sync your inbox before trying to read an email");
            return;
        }
        if (!POST("DELETE"))
            return;
        System.out.println("Please enter the id of the email you'd like to delete: ");
        int index = Integer.parseInt(scanner.nextLine());
        index = index > 0 && index <= cachedEmails.size() ? index - 1 : -1;
        if (cachedEmails == null || index == -1) {
            outputStream.writeObject(-1);
            return;
        }
        //send email id to the server
        outputStream.writeObject(cachedEmails.get(index).getEmailId());
        cachedEmails.remove(index);
        System.out.println("Deletion successful");
    }

    public void logOut() throws Exception {
        if (!POST("LOGOUT"))
            return;
        if ((Integer) inputStream.readObject() == 0)
            System.out.println("Logged out of account: " + currentUsername);
        currentUsername = null;
    }

    /*
    @use Closes the socket and its streams
    The server understands when the socket is closed and handles it
    */
    public void exit() {
        System.out.println("Thank you for using the AUTH Mail Client. It was built with love");
        System.out.println("Exiting");
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
