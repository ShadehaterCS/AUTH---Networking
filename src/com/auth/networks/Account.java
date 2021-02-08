package com.auth.networks;
import java.util.ArrayList;

public class Account {
    private final ArrayList<Email> personalEmails;
    private final String userName;
    private final String password;
    private final String uid;

    public Account(String userName, String password, String uid) {
        this.userName = userName;
        this.password = password;
        this.uid = uid;
        personalEmails = new ArrayList<>();
    }

    public ArrayList<Email> getPersonalEmails() {
        return personalEmails;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getUid() {
        return uid;
    }

    public synchronized void receiveEmail(Email email) {
        personalEmails.add(email);
    }

    public synchronized boolean readEmail(int emailId) {
        Email email = personalEmails.stream().filter(e -> e.getEmailId() == emailId).findFirst().orElse(null);
        if (email != null) {
            email.read();
            return true;
        }
        return false;
    }

    public synchronized boolean deleteEmail(int emailId) {
        return personalEmails.removeIf(e -> e.getEmailId() == emailId);
    }
}