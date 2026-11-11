package app;

import app.models.Book;
import app.models.Visitor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;

public class Task4to8 {

    public static void run(Connection conn) throws Exception {
        System.out.println("\n--- Задания 4-8: Библиотека ---");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS visitors (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100), surname VARCHAR(100), phone VARCHAR(20), UNIQUE(phone))");
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), author VARCHAR(255), publishing_year INT, " +
                    "isbn VARCHAR(50), publisher VARCHAR(255), UNIQUE(isbn))");
            stmt.execute("CREATE TABLE IF NOT EXISTS visitor_books (" +
                    "visitor_id INT, book_id INT, " +
                    "PRIMARY KEY (visitor_id, book_id), " +
                    "FOREIGN KEY (visitor_id) REFERENCES visitors(id), " +
                    "FOREIGN KEY (book_id) REFERENCES books(id))");
        }

        //Задание 4
        System.out.println("\n--- № 4. Загрузка данных из books.json ---");
        try (InputStream is = Task4to8.class.getResourceAsStream("/books.json")) {
            if (is == null) {
                throw new RuntimeException("Файл books.json не найден в ресурсах!");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Visitor>>() {}.getType();
            List<Visitor> visitors = new Gson().fromJson(json, listType);

            String insertBookSql = "INSERT INTO books (name, author, publishing_year, isbn, publisher) " +
                    "SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM books WHERE isbn = ?)";
            String insertVisitorSql = "INSERT INTO visitors (name, surname, phone) " +
                    "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM visitors WHERE phone = ?)";
            String linkSql = "INSERT INTO visitor_books (visitor_id, book_id) " +
                    "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM visitor_books WHERE visitor_id = ? AND book_id = ?)";

            try (PreparedStatement psBook = conn.prepareStatement(insertBookSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psVisitor = conn.prepareStatement(insertVisitorSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psLink = conn.prepareStatement(linkSql)) {

                for (Visitor v : visitors) {
                    psVisitor.setString(1, v.getName());
                    psVisitor.setString(2, v.getSurname());
                    psVisitor.setString(3, v.getPhone());
                    psVisitor.setString(4, v.getPhone());
                    psVisitor.executeUpdate();
                    int visitorId = getGeneratedId(psVisitor, conn, "visitors", "phone", v.getPhone());

                    for (Book b : v.getFavoriteBooks()) {
                        psBook.setString(1, b.getName());
                        psBook.setString(2, b.getAuthor());
                        psBook.setInt(3, b.getPublishingYear());
                        psBook.setString(4, b.getIsbn());
                        psBook.setString(5, b.getPublisher());
                        psBook.setString(6, b.getIsbn());
                        psBook.executeUpdate();
                        int bookId = getGeneratedId(psBook, conn, "books", "isbn", b.getIsbn());

                        psLink.setInt(1, visitorId);
                        psLink.setInt(2, bookId);
                        psLink.setInt(3, visitorId);
                        psLink.setInt(4, bookId);
                        psLink.executeUpdate();
                    }
                }
            }
            System.out.println("Данные загружены.");
        }

        //Задание 5
        System.out.println("\n--- № 5. Сортировка книг по году ---");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM books ORDER BY publishing_year ASC")) {
            while (rs.next()) {
                System.out.println(rs.getInt("publishing_year") + " - " + rs.getString("name"));
            }
        }

        //Задание 6
        System.out.println("\n--- № 6. Книги до 2000 года ---");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM books WHERE publishing_year < 2000")) {
            while (rs.next()) {
                System.out.println(rs.getInt("publishing_year") + " - " + rs.getString("name"));
            }
        }

        //Задание 7
        System.out.println("\n--- № 7. Добавление себя и любимых книг ---");
        String myPhone = "+79990000000";
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO visitors (name, surname, phone) " +
                "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM visitors WHERE phone = ?)")) {
            ps.setString(1, "Арина");
            ps.setString(2, "Гуляева");
            ps.setString(3, myPhone);
            ps.setString(4, myPhone);
            ps.executeUpdate();
        }
        int myId = getIdByPhone(conn, myPhone);

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO books (name, author, publishing_year, isbn, publisher) " +
                "SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM books WHERE isbn = ?)")) {
            ps.setString(1, "Моя любимая книга");
            ps.setString(2, "Автор");
            ps.setInt(3, 2024);
            ps.setString(4, "123-4567890123");
            ps.setString(5, "Издательство");
            ps.setString(6, "123-4567890123");
            ps.executeUpdate();
        }
        int myBookId = getIdByIsbn(conn, "123-4567890123");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO visitor_books (visitor_id, book_id) VALUES (?, ?)")) {
            ps.setInt(1, myId);
            ps.setInt(2, myBookId);
            ps.executeUpdate();
        }

        System.out.println("Добавленные данные:");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT v.name, v.surname, b.name as book_name, b.publishing_year " +
                        "FROM visitors v JOIN visitor_books vb ON v.id = vb.visitor_id " +
                        "JOIN books b ON vb.book_id = b.id WHERE v.phone = ?")) {
            ps.setString(1, myPhone);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString("name") + " " + rs.getString("surname") +
                        " -> " + rs.getString("book_name") + " (" + rs.getInt("publishing_year") + ")");
            }
        }

        //Задание 8
        System.out.println("\n--- № 8. Удаление таблиц ---");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS visitor_books");
            stmt.execute("DROP TABLE IF EXISTS books");
            stmt.execute("DROP TABLE IF EXISTS visitors");
            System.out.println("Таблицы удалены.");
        }
    }

    private static int getGeneratedId(PreparedStatement ps, Connection conn, String table, String column, String value) throws SQLException {

        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return getIdByColumn(conn, table, column, value);
    }

    private static int getIdByColumn(Connection conn, String table, String column, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM " + table + " WHERE " + column + " = ?")) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }

    private static int getIdByPhone(Connection conn, String phone) throws SQLException {
        return getIdByColumn(conn, "visitors", "phone", phone);
    }

    private static int getIdByIsbn(Connection conn, String isbn) throws SQLException {
        return getIdByColumn(conn, "books", "isbn", isbn);
    }
}
