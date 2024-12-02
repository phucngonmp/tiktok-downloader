package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        String name = "seoyong0";

        File newDir = new File("D:\\workspace\\work\\vids\\" + name);
        if (!newDir.exists()) {
            newDir.mkdirs();  // Creates the directory if it doesn't exist
        }
        File txtFile = new File(newDir, name + ".txt");
        if (!txtFile.exists()) {
            try {
                txtFile.createNewFile();  // Creates the file
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        FileWriter writer = new FileWriter(txtFile, true);
        for (int i = 0; i < 5; i++) {
            writer.write("hello\n");
            writer.flush();
        }

    }
}
