package app;

import java.sql.*;

public class Task1to3 {

    public static void run(Connection conn) throws SQLException {
        System.out.println("\n--- Задания 1-3: Музыка ---");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS study");

            String sqlScript = new String(Task1to3.class.getResourceAsStream("/music-create.sql").readAllBytes());

            for (String sql : sqlScript.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка подготовки БД: " + e.getMessage());
            return;
        }

        //Задание 1
        System.out.println("\n--- № 1. Список музыкальных композиций ---");
        String sqlSelect = "SELECT * FROM study.music";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlSelect)) {
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ": " + rs.getString("name"));
            }
        }

        //Задание 2
        System.out.println("\n--- № 2. Фильтрация (нет букв 'm' и 't') ---");
        String sqlFilter = "SELECT * FROM study.music WHERE LOCATE('m', LOWER(name)) = 0 AND LOCATE('t', LOWER(name)) = 0";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlFilter)) {
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ": " + rs.getString("name"));
            }
        }

        //Задание 3
        System.out.println("\n--- № 3. Добавление композиции ---");
        String sqlInsert = "INSERT INTO study.music (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setString(1, "Моя любимая песня");
            int rows = ps.executeUpdate();
            System.out.println("Добавлено строк: " + rows);
        }
    }
}
