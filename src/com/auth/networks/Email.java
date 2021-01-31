package com.auth.networks;

import java.io.Serializable;

public class Email implements Serializable {
    boolean isNew;
    private final String subject;
    private final String mainBody;
    private final String sender;
    private final String recipient;
    private final String timestamp;

    public Email(String subject, String mainBody, String sender, String recipient, String timestamp) {
        this.subject = subject;
        this.mainBody = mainBody;
        this.sender = sender;
        this.recipient = recipient;
        this.timestamp = timestamp;
        isNew = true;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String toString(){ //todo fix the visuals of this
        String readStatus = isNew ? "NEW" : "READ";
        return "READ STATUS:" + readStatus +
                "\nSender:\t" + sender +
                "\nSubject:\t" + subject +
                "\nContent:\t" + mainBody +
                "\nTimestamp:\t" + timestamp;
    }

    public String toStringCondensed(){
        String readStatus = isNew ? "NEW" : "READ";
        return "READ STATUS:" + readStatus +
                "\nSender:\t" + sender +
                "\nSubject:\t" + subject +
                "\nTimestamp:\t" + timestamp;
    }

    public void read(){ isNew = false; }
}
