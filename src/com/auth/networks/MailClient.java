package com.auth.networks;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

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

    private boolean signedIn;
    public MailClient() {
        scanner = new Scanner(System.in);
    }

    /*
    Loop everytime so a user can do stuff
     */
    public void setUpConnection(String host, int port){
        try {
            InetAddress a = InetAddress.getByName(host);
            socket = new Socket(host, 3000);

            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        signedIn = false;
    }
    public void showMenu() {
        try {
            int selection = -1;
            do {
                System.out.println("THIS WILL BE THE MENU"); //TODO ADD MENU
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
                else { //implicit 8
                    System.out.println("Thank you for using the AUTH Mail Client. It was built with love");
                    exit();
                    break;
                }
            } while (true);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void register() throws IOException, ClassNotFoundException {
        outputStream.writeObject("REGISTER");
        System.out.println("Enter your username: (domain @csd.auth.gr)will be added automatically");
        outputStream.writeObject(scanner.nextLine());
        if ((Integer)inputStream.readObject() == -1) {
            System.err.println("Username already exists");
            return;
        }
        System.out.println("Enter your password: ");
        outputStream.writeObject(scanner.nextLine().hashCode()); //.hashcode because we're good like that

        System.out.println("Account registered successfully, you can now log in");
    }

    public void login() {
        if (signedIn) {
            System.out.println("You are already signed in");
            return;
        }
    }

    public void newEmail() {

    }

    public void showEmails() {

    }

    public void readEmail() {

    }

    public void deleteEmail() {

    }

    public void logOut() {

    }

    public void exit() {
        System.out.println("Exiting");
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
