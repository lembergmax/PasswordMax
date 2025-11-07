package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.security.Cryptographer;

public class Main {

    public static void main(String[] args) {
        final Cryptographer cryptographer = new Cryptographer();

        String password = "Minecraft";

        System.out.println("Verschlüsselt: " + cryptographer.encrypt(password));
        System.out.println("Entschlüsselt: " + cryptographer.decrypt(cryptographer.encrypt(password), password));
    }

}