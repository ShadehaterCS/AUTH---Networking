package com.auth.networks;

import java.time.LocalDateTime;
import java.util.Date;

public class Logger {

    static void logMessage(String type, String status, String username){
        StringBuilder logBuild = new StringBuilder();
        logBuild.append(LocalDateTime.now());
        logBuild.append(" : ");
        logBuild.append(type);
        logBuild.append(" operation ");
        logBuild.append(status);
        logBuild.append("with username: ");
        logBuild.append(username);
        String log = logBuild.toString();
        MailServer.serverLog.add(log);
    }
}
