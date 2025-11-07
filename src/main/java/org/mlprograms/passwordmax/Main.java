package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.util.Cryptographer;

public class Main {

    public static void main(String[] args) {
        final Cryptographer cryptographer = new Cryptographer();

        String password = "Minecraft";

        System.out.println("Verschlüsselt (Base64): " + cryptographer.encrypt(password));
        System.out.println("Entschlüsselt: " + cryptographer.decrypt(cryptographer.encrypt(password), password));
    }

}