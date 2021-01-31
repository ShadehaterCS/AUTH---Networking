package com.auth.networks;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Vector;

public class MailClient {
    public static void main(String[] args) {
        MailClient client = new MailClient();
        client.setUpConnection(args[0], Integer.parseInt(args[1]));
        client.showMenu();
    }
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;
    private Scanner scanner;

    private String currentUsername;
    private Vector<Email> cachedEmails;

    public MailClient() {
        scanner = new Scanner(System.in);
    }

    /*
    Loop everytime so a user can do stuff
     */
    public void setUpConnection(String host, int port){
        try {
            InetAddress a = InetAddress.getByName(host);
            socket = new Socket(host, port);

            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void showMenu() {
        try {
            int selection = -1;
            System.out.println("Welcome to the AUTH Mail Client. It just works");
            do {
                System.out.println("""
                        1 -> Register
                        2 -> Log In
                        3 -> Create new email
                        4 -> See your inbox
                        5 -> Read an E-mail
                        6 -> Delete an E-mail
                        7 -> Log out
                        8 -> EXIT :(                    
                        """);
                selection = Integer.parseInt(scanner.nextLine());

                if (selection == 1)
                    register();
                else if (selection == 2)
                    login();
                else if (selection == 3)
                    newEmail();
                else if (selection == 4)
                    showEmails();
                else if (selection == 5)
                    readEmail();
                else if (selection == 6)
                    deleteEmail();
                else if (selection == 7)
                    logOut();
                else if (selection == 8){ //implicit 8
                    System.out.println("Thank you for using the AUTH Mail Client. It was built with love");
                    exit();
                    break;
                }
            } while (true);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
 //todo change writeObject to sendRequest method
    public void register() throws IOException, ClassNotFoundException {
        outputStream.writeObject("REGISTER");
        if ((Integer) inputStream.readObject() == -1){
            System.err.println("You are already logged in");
            return;
        }
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

    public void login() throws Exception{
        outputStream.writeObject("LOGIN");
        if ((Integer) inputStream.readObject() == 100){
            System.err.println("You are already logged in");
            return;
        }
        String[] packet = new String[2];
        System.out.println("Please enter your email address: ");
        String username = scanner.nextLine();
        System.out.println("Please enter your password: ");
        String password = scanner.nextLine();
        //password = String.valueOf(password.hashCode());
        outputStream.writeObject(username);
        outputStream.writeObject(password);
        //Get response if username and password are correct
        if ((Integer) inputStream.readObject() == -1){
            System.err.println("Wrong username or password");
            return;
        }
        System.out.println("Log in successful");
        currentUsername = username;
    }

    public void newEmail() throws Exception {
        outputStream.writeObject("NEW-EMAIL");
        if ((Integer) inputStream.readObject() == -1){
            System.err.println("You are not currently logged in");
            return;
        }
        System.out.println("Please enter the recipient's email address");
        String recipientUsername = scanner.nextLine();
        outputStream.writeObject(recipientUsername);
        if ((Integer) inputStream.readObject() == -1){
            System.err.println("No such email address exists");
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
    public void showEmails() throws Exception{
        outputStream.writeObject("INBOX");
        if ((Integer) inputStream.readObject() == -1){
            System.err.println("You are not currently logged in");
            return;
        }
        cachedEmails = (Vector<Email>) inputStream.readObject();
        if (cachedEmails.size() == 0)
            System.out.println("No emails in your inbox");
        int i=0;
        for (Email e : cachedEmails){
            System.out.println("ID:\n" + i++ + " " + e.toStringCondensed());
        }
    }

    /*
    Uses the cached emails, showEmails() should be called to resync
    */
    public void readEmail() throws Exception{
        outputStream.writeObject("READ-EMAIL");
        if ((Integer) inputStream.readObject() == -1){
            System.err.println("You are not currently logged in");
            return;
        }
        System.out.println("Please enter the id of the email you'd like to read: ");
        int index = Integer.parseInt(scanner.nextLine());
        if (cachedEmails == null || index > cachedEmails.size()-1 || index < 0){
            System.err.println("Invalid id selected");
            outputStream.writeObject(-1);
            return;
        }
        Email selected = cachedEmails.elementAt(index);
        selected.read();
        System.out.println(selected.toString());

        outputStream.writeObject(index);
    }

    public void deleteEmail() throws Exception{

    }

    public void logOut() throws Exception{

    }

    public void exit() {
        System.out.println("Exiting");
        try {
            outputStream.writeObject("EXIT");
            inputStream.close();
            outputStream.close();
            socket.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
