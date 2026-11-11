package app;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {
    public static void main(String[] args) {
        String url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        String user = "sa";
        String pass = "";

        System.out.println("Подключение к базе данных H2...");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Подключение успешно!");

            Task1to3.run(conn);
            Task4to8.run(conn);

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
