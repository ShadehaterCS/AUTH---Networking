package com.auth.networks;

import java.util.ArrayList;

public class User {
    private ArrayList<Email> personalEmails;
    private String userName;
    private String password;
    private String uid;

    private byte[] privateKey;
    private byte[] publicKey;

    public User(String userName, String password, String uid){
        this.userName = userName;
        this.password = password;
        this.uid = uid;

    }

}
