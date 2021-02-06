@echo off
start "Server" java -jar MailServer-1.jar 3002

start "Client 1" java -jar MailClient-1.jar 127.0.0.1 3002
start "Client 2" java -jar MailClient-1.jar 127.0.0.1 3002