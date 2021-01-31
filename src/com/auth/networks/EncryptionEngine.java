package com.auth.networks;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class EncryptionEngine {
    KeyPair pair;
    Byte[] publicKey;
    Byte[] privateKey;

    public Byte[] getPublicKey() { return publicKey; }
    public Byte[] getPrivateKey() { return privateKey; }

    public EncryptionEngine(){
        //Needed because the client and server are using object streams. Can't autobox byte array so this is manual
        try {
            pair = generateKeypair();
            byte[] temp = pair.getPublic().getEncoded();
            publicKey = new Byte[temp.length];

            int i = 0;
            for (byte b : temp)
                publicKey[i++] = b;

            temp = pair.getPrivate().getEncoded();
            privateKey = new Byte[temp.length];
            for (byte b : temp)
                privateKey[i++] = b;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        /*(
        byte[] encryptedData = encrypt(publicKey,
                "I love u baby, and if it's quite alright".getBytes());

        byte[] decryptedData = decrypt(privateKey, encryptedData);

        System.out.println(new String(decryptedData)); */
        EncryptionEngine e = new EncryptionEngine();
        System.out.println();
    }

    public KeyPair generateKeypair() throws Exception{
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keygen.initialize(512, random);
            return keygen.generateKeyPair();
    }

    public byte[] encrypt(Byte[] publicKey, byte[] inputData) throws Exception {
        byte[] key = new byte[publicKey.length];
        int i = 0;
        for (Byte b : publicKey)
            key[i++] = b;

        PublicKey pKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(key));

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pKey);

        return cipher.doFinal(inputData);
    }

    public byte[] decrypt(Byte[] privateKey, byte[] inputData) throws Exception {
        byte[] key = new byte[privateKey.length];
        int i = 0;
        for (Byte b : privateKey)
            key[i++] = b;

        PrivateKey pKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(key));

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, pKey);

        return cipher.doFinal(inputData);
    }
}
