package com.auth.networks;

import java.util.Vector;

public class Account {
    private Vector<Email> personalEmails;
    private String userName;
    private String password;
    private String uid;

    public Account(String userName, String password, String uid){
        this.userName = userName;
        this.password = password;
        this.uid = uid;
        personalEmails = new Vector<>();
    }

    public Vector<Email> getPersonalEmails() {
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

    public synchronized void receiveEmail(Email email){
        personalEmails.add(email);
    }
}
