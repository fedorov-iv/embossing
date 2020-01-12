package ru.somebank.embossing.utils.encrypt;

import org.jasypt.util.text.BasicTextEncryptor;

public class Encryptor {

    private String password = "testPassword(196)_0003";

    private BasicTextEncryptor textEncryptor;

    private static Encryptor instance;

    public static Encryptor getInstance(){

        if(instance == null){
            instance = new Encryptor();

        }
        return instance;

    }

    private Encryptor(){
        textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(password);
    }

    public String encrypt(String input){
        return textEncryptor.encrypt(input);
    }

    public String decrypt(String input){

        return textEncryptor.decrypt(input);
    }


}
