import java.sql.*;

public class CheckDerbySchema {
    public static void main(String[] args) {
        String url = "jdbc:derby:./data/memberDB;create=false";
        try (Connection conn = DriverManager.getConnection(url)) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, "SA", "MEMBERS", null);
            System.out.println("Columns in MEMBERS table:");
            System.out.println("--------------------------------------------------");
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String isNullable = rs.getString("IS_NULLABLE"); // "NO" means NOT NULL
                String typeName = rs.getString("TYPE_NAME");
                System.out.printf("%-25s | %-10s | %s%n", columnName, isNullable, typeName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
