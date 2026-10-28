package com.bank.dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// SqlRunner: Executes SQL scripts directly against the SQLite database
public class SqlRunner {

    // main: Executes SQL file passed as argument or default schema.sql
    public static void main(String[] args) {
        String sqlFilePath = (args.length > 0) ? args[0] : "schema.sql";
        System.out.println("Executing SQL script: " + sqlFilePath);

        try {
            executeSqlFile(sqlFilePath);
            System.out.println("SQL execution completed successfully!");
            printTableSummary();
        } catch (Exception e) {
            System.err.println("SQL execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // executeSqlFile: Reads and executes SQL statements from a file path or resource
    public static void executeSqlFile(String filePath) throws Exception {
        List<String> statements = loadStatements(filePath);

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql);
                }
            }
        }
    }

    // loadStatements: Parses SQL file content into individual statement strings
    private static List<String> loadStatements(String filePath) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("--")) {
                    sb.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            // Fallback to classpath resource
            try (InputStream in = SqlRunner.class.getClassLoader().getResourceAsStream(filePath)) {
                if (in == null) {
                    throw new IllegalArgumentException("Could not find SQL file: " + filePath);
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.startsWith("--")) {
                            sb.append(line).append("\n");
                        }
                    }
                }
            }
        }

        List<String> result = new ArrayList<>();
        for (String statement : sb.toString().split(";")) {
            String s = statement.trim();
            if (!s.isEmpty()) {
                result.add(s);
            }
        }
        return result;
    }

    // printTableSummary: Prints tables currently present in the database
    public static void printTableSummary() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")) {

            System.out.println("\nTables in database (" + DBConnection.getJdbcUrl() + "):");
            while (rs.next()) {
                String tableName = rs.getString("name");
                if (!tableName.startsWith("sqlite_")) {
                    try (Statement countStmt = conn.createStatement();
                         ResultSet countRs = countStmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                        countRs.next();
                        System.out.printf("  - %-15s : %d rows%n", tableName, countRs.getInt(1));
                    }
                }
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("Could not display table summary: " + e.getMessage());
        }
    }
}

