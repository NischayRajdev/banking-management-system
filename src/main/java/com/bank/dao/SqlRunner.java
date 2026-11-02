package com.bank.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SqlRunner: High-performance SQL script runner and query execution connection.
 * Connects to SQLite via DBConnection, executes queries, and renders formatted
 * ASCII tabular results for SELECT queries and DDL/DML execution status.
 */
public class SqlRunner {

    public static class QueryBlock {
        private final int index;
        private final String title;
        private final String sql;

        public QueryBlock(int index, String title, String sql) {
            this.index = index;
            this.title = title;
            this.sql = sql;
        }

        public int getIndex() { return index; }
        public String getTitle() { return title; }
        public String getSql() { return sql; }
    }

    public static void main(String[] args) {
        String filePath = (args.length > 0) ? args[0] : "queries.sql";
        Integer targetIndex = null;

        if (args.length > 1) {
            try {
                targetIndex = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("================================================================================");
        System.out.println("           INDIA BANKING MANAGEMENT SYSTEM - SQL QUERY RUNNER");
        System.out.println("================================================================================");
        System.out.println(" Database Connection : " + DBConnection.getJdbcUrl());
        System.out.println(" Target Script File  : " + filePath);
        if (targetIndex != null) {
            System.out.println(" Filter Target Query : #" + targetIndex);
        }
        System.out.println("================================================================================\n");

        try {
            if ("schema.sql".equalsIgnoreCase(new File(filePath).getName())) {
                executeSchemaFile(filePath);
            } else {
                executeAndDisplayQueryFile(filePath, targetIndex);
            }
        } catch (Exception e) {
            System.err.println("\nExecution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Executes queries from a SQL file and renders formatted results.
     */
    public static void executeAndDisplayQueryFile(String filePath, Integer targetIndex) throws Exception {
        List<QueryBlock> queries = parseQueriesWithTitles(filePath);

        if (queries.isEmpty()) {
            System.out.println("No executable SQL statements found in: " + filePath);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            int executedCount = 0;

            for (QueryBlock q : queries) {
                if (targetIndex != null && q.getIndex() != targetIndex) {
                    continue;
                }

                executedCount++;
                printQueryHeader(q);

                long start = System.currentTimeMillis();
                try (Statement stmt = conn.createStatement()) {
                    boolean isResultSet = stmt.execute(q.getSql());

                    if (isResultSet) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            printResultSet(rs, System.currentTimeMillis() - start);
                        }
                    } else {
                        int count = stmt.getUpdateCount();
                        System.out.printf("  Execution successful. %d row(s) affected in %d ms.\n\n",
                                count, (System.currentTimeMillis() - start));
                    }
                } catch (SQLException ex) {
                    System.err.println("  [ERROR executing query #" + q.getIndex() + "]: " + ex.getMessage() + "\n");
                }
            }

            if (executedCount == 0 && targetIndex != null) {
                System.out.println("No query matching index #" + targetIndex + " was found. Available queries: 1 to " + queries.size());
            }
        }
    }

    /**
     * Executes single arbitrary SQL query string and prints tabular output.
     */
    public static void executeAndDisplaySingleQuery(String sql, String queryTitle) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" Query: " + (queryTitle != null ? queryTitle : "Ad-hoc SQL Query"));
        System.out.println("=".repeat(80));
        System.out.println(sql.trim());
        System.out.println("-".repeat(80));

        long start = System.currentTimeMillis();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            boolean isResultSet = stmt.execute(sql);
            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    printResultSet(rs, System.currentTimeMillis() - start);
                }
            } else {
                int count = stmt.getUpdateCount();
                System.out.printf("  Execution successful. %d row(s) affected in %d ms.\n\n",
                        count, (System.currentTimeMillis() - start));
            }
        } catch (SQLException e) {
            System.err.println("  Query Error: " + e.getMessage() + "\n");
        }
    }

    /**
     * Formats and prints a JDBC ResultSet into an ASCII table.
     */
    public static void printResultSet(ResultSet rs, long elapsedMs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        String[] headers = new String[colCount];
        int[] colWidths = new int[colCount];

        for (int i = 1; i <= colCount; i++) {
            headers[i - 1] = meta.getColumnLabel(i);
            colWidths[i - 1] = Math.max(headers[i - 1].length(), 8);
        }

        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            String[] row = new String[colCount];
            for (int i = 1; i <= colCount; i++) {
                String val = rs.getString(i);
                if (val == null) {
                    val = "NULL";
                }
                row[i - 1] = val;
                colWidths[i - 1] = Math.max(colWidths[i - 1], val.length());
            }
            rows.add(row);
        }

        // Cap maximum column width to 45 characters for compact terminal readability
        for (int i = 0; i < colCount; i++) {
            if (colWidths[i] > 45) {
                colWidths[i] = 45;
            }
        }

        // Print table top border
        StringBuilder border = new StringBuilder("+");
        for (int w : colWidths) {
            border.append("-".repeat(w + 2)).append("+");
        }
        System.out.println(border);

        // Print header
        StringBuilder headerLine = new StringBuilder("|");
        for (int i = 0; i < colCount; i++) {
            headerLine.append(" ").append(padRight(truncate(headers[i], colWidths[i]), colWidths[i])).append(" |");
        }
        System.out.println(headerLine);
        System.out.println(border);

        // Print data rows
        if (rows.isEmpty()) {
            int totalLen = border.length() - 2;
            String emptyMsg = "No matching records found.";
            System.out.println("| " + padRight(emptyMsg, Math.max(0, totalLen - 1)) + "|");
        } else {
            for (String[] row : rows) {
                StringBuilder rowLine = new StringBuilder("|");
                for (int i = 0; i < colCount; i++) {
                    String val = truncate(row[i], colWidths[i]);
                    rowLine.append(" ").append(padRight(val, colWidths[i])).append(" |");
                }
                System.out.println(rowLine);
            }
        }

        System.out.println(border);
        System.out.printf("  ==> %d record(s) returned in %d ms.\n\n", rows.size(), elapsedMs);
    }

    private static void printQueryHeader(QueryBlock q) {
        System.out.println("=" .repeat(80));
        System.out.printf(" QUERY [%d]: %s\n", q.getIndex(), q.getTitle());
        System.out.println("=" .repeat(80));
        System.out.println(q.getSql().trim());
        System.out.println("-" .repeat(80));
    }

    /**
     * Parses SQL statements while preserving titles extracted from comments.
     */
    public static List<QueryBlock> parseQueriesWithTitles(String filePath) throws Exception {
        List<QueryBlock> queries = new ArrayList<>();
        StringBuilder sqlBuffer = new StringBuilder();
        StringBuilder titleBuffer = new StringBuilder();
        int queryCounter = 1;

        try (BufferedReader reader = openReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.startsWith("--")) {
                    String commentText = trimmed.substring(2).trim();
                    if (!commentText.isEmpty() && !commentText.startsWith("===") && !commentText.startsWith("---")) {
                        // Strip leading index if present like "1. " or "10. "
                        commentText = commentText.replaceFirst("^\\d+\\.\\s*", "");
                        if (!titleBuffer.isEmpty()) titleBuffer.append(" - ");
                        titleBuffer.append(commentText);
                    }
                    continue;
                }

                if (trimmed.isEmpty()) {
                    continue;
                }

                sqlBuffer.append(line).append("\n");

                if (trimmed.endsWith(";")) {
                    String sql = sqlBuffer.toString().trim();
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1).trim();
                    }

                    if (!sql.isEmpty()) {
                        String title = !titleBuffer.isEmpty()
                                ? titleBuffer.toString()
                                : "Query #" + queryCounter;
                        queries.add(new QueryBlock(queryCounter++, title, sql));
                        titleBuffer.setLength(0);
                    }
                    sqlBuffer.setLength(0);
                }
            }
        }

        return queries;
    }

    /**
     * Executes DDL schema files (e.g., schema.sql) and outputs table counts.
     */
    public static void executeSchemaFile(String filePath) throws Exception {
        System.out.println("Applying schema script: " + filePath);
        List<String> statements = loadStatements(filePath);

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql);
                }
            }
        }
        System.out.println("Schema applied successfully!\n");
        printTableSummary();
    }

    private static List<String> loadStatements(String filePath) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = openReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("--")) {
                    sb.append(line).append("\n");
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

    private static BufferedReader openReader(String filePath) throws Exception {
        File file = new File(filePath);
        if (file.exists()) {
            return new BufferedReader(new FileReader(file));
        }
        InputStream in = SqlRunner.class.getClassLoader().getResourceAsStream(filePath);
        if (in != null) {
            return new BufferedReader(new InputStreamReader(in));
        }
        throw new IllegalArgumentException("SQL file not found at path or classpath: " + filePath);
    }

    /**
     * Prints live summary of all tables present in the database.
     */
    public static void printTableSummary() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")) {

            System.out.println("Database Tables Summary (" + DBConnection.getJdbcUrl() + "):");
            String border = "+----------------------+----------------+";
            System.out.println(border);
            System.out.printf("| %-20s | %-14s |\n", "Table Name", "Row Count");
            System.out.println(border);

            while (rs.next()) {
                String tableName = rs.getString("name");
                if (!tableName.startsWith("sqlite_")) {
                    try (Statement countStmt = conn.createStatement();
                         ResultSet countRs = countStmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                        countRs.next();
                        System.out.printf("| %-20s | %10d rows |\n", tableName, countRs.getInt(1));
                    }
                }
            }
            System.out.println(border + "\n");
        } catch (Exception e) {
            System.err.println("Could not display table summary: " + e.getMessage());
        }
    }

    private static String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, Math.max(0, maxLen - 3)) + "...";
    }
}
