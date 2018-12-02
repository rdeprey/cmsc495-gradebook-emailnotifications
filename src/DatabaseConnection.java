import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    public Connection getConnection() throws Exception {
        // Get the file with the connection data
        File jarFile = new File(SendEmailNotication.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        String path = jarFile + File.separator + "dbconnection.txt";
        FileInputStream fileInputStream = new FileInputStream(path);
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(fileInputStream));

        String st;
        String host = "";
        String username = "";
        String password = "";
        int count = 0;
        while ((st = bufferedReader.readLine()) != null) {
            if (count == 0) {
                host = "jdbc:sqlserver://" + st;
            } else if (count == 1) {
                username = st;
            } else if (count == 2) {
                password = st;
            }
            count++;
        }

        String jdbcUrl = host + ":1433;DatabaseName=gradebookdb";

        try {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            throw new RuntimeException("Error connecting to the database", ex);
        }
    }
}