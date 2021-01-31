package com.auth.networks;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner s = new Scanner(System.in);
        while(true){
            String a = s.nextLine();
            System.out.println(a.hashCode());
        }
    }
}
