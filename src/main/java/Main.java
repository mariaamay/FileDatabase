import javax.swing.*;
import java.io.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String dbPath = "database.csv";
        String idFilePath = "id.csv";
        String directory = "db";

        SwingUtilities.invokeLater(() -> {
            FileDatabaseGUI gui = new FileDatabaseGUI(dbPath, idFilePath, directory);
            gui.setVisible(true);
        });

    }
}
