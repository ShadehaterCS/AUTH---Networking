package main.java.com.auth.networks;

import java.io.Serializable;

public class Email implements Serializable {
    boolean isNew;
    private final String subject;
    private final String mainBody;
    private final String sender;
    private final String recipient;
    private final String timestamp;
    private final int emailId;

    public Email(String subject, String mainBody, String sender, String recipient, String timestamp) {
        this.subject = subject;
        this.mainBody = mainBody;
        this.sender = sender;
        this.recipient = recipient;
        this.timestamp = timestamp;
        isNew = true;
        emailId = MailServer.getNewEmailID();
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String toString(){
        String readStatus = isNew ? "NEW" : "READ";
        return "["+readStatus+"]\t" + sender +"\t"+subject+"\nContent:\n" + mainBody;
    }

    public String toStringCondensed(){
        String readStatus = isNew ? "NEW" : "READ";
        return " ["+readStatus+"]\t" + sender +"\t"+subject;
    }

    public int getEmailId() { return emailId; }
    public void read(){ isNew = false; }
}
