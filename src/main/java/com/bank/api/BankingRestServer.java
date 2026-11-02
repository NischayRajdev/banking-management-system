package com.bank.api;

import com.bank.dao.*;
import com.bank.exception.*;
import com.bank.model.*;
import com.bank.service.Bank;
import com.bank.service.DatabaseSeeder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * High-performance Unified Web & REST API Server for the Banking Management System.
 * Serves the React single-page frontend application AND all core banking REST endpoints
 * on a single localhost port (default 8080).
 */
public class BankingRestServer {

    public static final int DEFAULT_PORT = 8080;
    private final int port;
    private final Bank bank;
    private HttpServer server;

    public BankingRestServer(int port) {
        this.port = port;
        DBConnection.initializeDatabase();
        this.bank = new Bank("India Bank", "INDB-001",
                new CustomerDAOImpl(),
                new AccountDAOImpl(),
                new TransactionDAOImpl());
        DatabaseSeeder.seedIfEmpty(this.bank);
    }

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        BankingRestServer restServer = new BankingRestServer(port);
        restServer.start();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Context 1: REST API endpoints under /api/
        server.createContext("/api/", new ApiDispatcher());

        // Context 2: Static frontend SPA handler under /
        server.createContext("/", new StaticSpaHandler());

        server.start();
        System.out.printf("""
            ======================================================================
               INDIA BANK UNIFIED SYSTEM (SINGLE LOCALHOST) STARTED
            ======================================================================
               Web Application : http://localhost:%d/
               REST API Base   : http://localhost:%d/api
               Health Check    : http://localhost:%d/api/health
            ======================================================================
               Access the complete dashboard and all features at:
               ==> http://localhost:%d/
            ======================================================================
            """, port, port, port, port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Serves static frontend assets (HTML, CSS, JS, SVG, Fonts) from frontend/dist
     * and handles client-side SPA routing (React Router) with index.html fallback.
     */
    private static class StaticSpaHandler implements HttpHandler {
        private final Path staticDir;

        StaticSpaHandler() {
            Path candidate = Path.of("frontend", "dist");
            if (!Files.exists(candidate)) {
                candidate = Path.of("bin", "public");
            }
            if (!Files.exists(candidate)) {
                candidate = Path.of("public");
            }
            this.staticDir = candidate.toAbsolutePath().normalize();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String reqPath = exchange.getRequestURI().getPath();
            if (reqPath.startsWith("/")) {
                reqPath = reqPath.substring(1);
            }

            Path target = staticDir.resolve(reqPath).normalize();

            // Prevent directory traversal
            if (!target.startsWith(staticDir)) {
                target = staticDir.resolve("index.html");
            }

            // Fallback for SPA routing or missing files
            if (!Files.exists(target) || Files.isDirectory(target)) {
                target = staticDir.resolve("index.html");
            }

            if (!Files.exists(target)) {
                String fallbackHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><title>India Bank - Setup</title></head>
                    <body style="font-family:sans-serif;padding:40px;text-align:center;">
                        <h2>India Bank Core Backend is Running</h2>
                        <p>Frontend assets not found at <code>frontend/dist</code>.</p>
                        <p>Please build the frontend using <code>cd frontend && npm run build</code>.</p>
                        <p>Core API is accessible at <a href="/api/health">/api/health</a>.</p>
                    </body>
                    </html>
                    """;
                byte[] bytes = fallbackHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            String contentType = probeContentType(target.getFileName().toString());
            exchange.getResponseHeaders().set("Content-Type", contentType);

            if (reqPath.startsWith("assets/")) {
                exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
            } else {
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            }

            byte[] fileBytes = Files.readAllBytes(target);
            exchange.sendResponseHeaders(200, "HEAD".equals(method) ? -1 : fileBytes.length);
            if (!"HEAD".equals(method)) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileBytes);
                }
            }
        }

        private String probeContentType(String filename) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
            if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript; charset=utf-8";
            if (lower.endsWith(".css")) return "text/css; charset=utf-8";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".ico")) return "image/x-icon";
            if (lower.endsWith(".json")) return "application/json; charset=utf-8";
            if (lower.endsWith(".woff2")) return "font/woff2";
            if (lower.endsWith(".woff")) return "font/woff";
            if (lower.endsWith(".ttf")) return "font/ttf";
            return "application/octet-stream";
        }
    }

    private class ApiDispatcher implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Apply CORS headers to all HTTP responses
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, X-Requested-With");
            exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");

            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> queryParams = parseQueryParams(query);

            try {
                dispatch(exchange, method, path, queryParams);
            } catch (BankingException be) {
                int status = (be instanceof CustomerNotFoundException || be instanceof AccountNotFoundException) ? 404 : 400;
                sendResponse(exchange, status, JsonUtil.errorJson(be.getMessage(), be.getErrorCode()));
            } catch (IllegalArgumentException iae) {
                sendResponse(exchange, 400, JsonUtil.errorJson(iae.getMessage(), "ERR_INVALID_ARGUMENT"));
            } catch (Exception ex) {
                ex.printStackTrace();
                sendResponse(exchange, 500, JsonUtil.errorJson("Internal server error: " + ex.getMessage(), "ERR_INTERNAL_SERVER"));
            }
        }

        private void dispatch(HttpExchange exchange, String method, String path, Map<String, String> queryParams)
                throws Exception {

            // Normalize path
            if (path.endsWith("/") && path.length() > 5) {
                path = path.substring(0, path.length() - 1);
            }

            // 1. Health
            if ("/api/health".equals(path) && "GET".equals(method)) {
                handleHealth(exchange);
                return;
            }

            // 2. Dashboard Summary
            if ("/api/dashboard/summary".equals(path) && "GET".equals(method)) {
                handleDashboardSummary(exchange);
                return;
            }

            // 3. Analytics
            if ("/api/analytics".equals(path) && "GET".equals(method)) {
                handleAnalytics(exchange);
                return;
            }

            // 4. Customers Collection
            if ("/api/customers".equals(path)) {
                if ("GET".equals(method)) {
                    handleListCustomers(exchange, queryParams);
                    return;
                } else if ("POST".equals(method)) {
                    handleCreateCustomer(exchange);
                    return;
                }
            }

            // 5. Single Customer Operations: /api/customers/{id} or /api/customers/{id}/accounts
            if (path.startsWith("/api/customers/")) {
                String sub = path.substring("/api/customers/".length());
                if (sub.endsWith("/accounts") && "GET".equals(method)) {
                    String customerId = sub.substring(0, sub.length() - "/accounts".length());
                    handleGetCustomerAccounts(exchange, customerId);
                    return;
                } else if (!sub.contains("/")) {
                    String customerId = sub;
                    if ("GET".equals(method)) {
                        handleGetCustomer(exchange, customerId);
                        return;
                    } else if ("PUT".equals(method)) {
                        handleUpdateCustomer(exchange, customerId);
                        return;
                    }
                }
            }

            // 6. Accounts Collection
            if ("/api/accounts".equals(path)) {
                if ("GET".equals(method)) {
                    handleListAccounts(exchange, queryParams);
                    return;
                }
            }

            // 7. Account Creation
            if ("/api/accounts/savings".equals(path) && "POST".equals(method)) {
                handleCreateSavingsAccount(exchange);
                return;
            }
            if ("/api/accounts/current".equals(path) && "POST".equals(method)) {
                handleCreateCurrentAccount(exchange);
                return;
            }

            // 8. Account Specific Sub-routes: /api/accounts/{accNo}/...
            if (path.startsWith("/api/accounts/")) {
                String sub = path.substring("/api/accounts/".length());
                if (sub.contains("/")) {
                    int slashIdx = sub.indexOf('/');
                    String accNo = sub.substring(0, slashIdx);
                    String action = sub.substring(slashIdx + 1);

                    if ("transactions".equals(action) && "GET".equals(method)) {
                        handleGetAccountTransactions(exchange, accNo);
                        return;
                    } else if ("apply-interest".equals(action) && "POST".equals(method)) {
                        handleApplyInterest(exchange, accNo);
                        return;
                    } else if ("status".equals(action) && ("PATCH".equals(method) || "PUT".equals(method))) {
                        handleUpdateAccountStatus(exchange, accNo);
                        return;
                    }
                } else {
                    String accNo = sub;
                    if ("GET".equals(method)) {
                        handleGetAccount(exchange, accNo);
                        return;
                    }
                }
            }

            // 9. Transactions Collection & Lookup
            if ("/api/transactions".equals(path) && "GET".equals(method)) {
                handleListTransactions(exchange, queryParams);
                return;
            }
            if (path.startsWith("/api/transactions/") && !path.startsWith("/api/transactions/deposit")
                    && !path.startsWith("/api/transactions/withdraw") && !path.startsWith("/api/transactions/transfer")) {
                String txnId = path.substring("/api/transactions/".length());
                if ("GET".equals(method)) {
                    handleGetTransaction(exchange, txnId);
                    return;
                }
            }

            // 10. Financial Operations
            if ("/api/transactions/deposit".equals(path) && "POST".equals(method)) {
                handleDeposit(exchange);
                return;
            }
            if ("/api/transactions/withdraw".equals(path) && "POST".equals(method)) {
                handleWithdraw(exchange);
                return;
            }
            if ("/api/transactions/transfer".equals(path) && "POST".equals(method)) {
                handleTransfer(exchange);
                return;
            }

            // 11. Database Management
            if ("/api/database/reseed".equals(path) && "POST".equals(method)) {
                handleDatabaseReseed(exchange);
                return;
            }
            if ("/api/database/reload".equals(path) && "POST".equals(method)) {
                handleDatabaseReload(exchange);
                return;
            }
            if ("/api/database/tables".equals(path) && "GET".equals(method)) {
                handleDatabaseTables(exchange);
                return;
            }
            if ("/api/queries".equals(path) && "GET".equals(method)) {
                handleGetQueries(exchange);
                return;
            }
            if ("/api/queries/run".equals(path) && "GET".equals(method)) {
                handleRunQuery(exchange);
                return;
            }

            sendResponse(exchange, 404, JsonUtil.errorJson("Endpoint not found: " + method + " " + path, "ERR_NOT_FOUND"));
        }

        // ==================== Handlers ====================

        private void handleHealth(HttpExchange exchange) throws IOException {
            String json = String.format("""
                {
                  "status": "UP",
                  "bankName": "%s",
                  "branchCode": "%s",
                  "serverTime": "%s",
                  "currency": "INR",
                  "currencySymbol": "₹"
                }
                """, JsonUtil.escape(bank.getBankName()), JsonUtil.escape(bank.getBranchCode()), LocalDateTime.now());
            sendResponse(exchange, 200, json);
        }

        private void handleDashboardSummary(HttpExchange exchange) throws IOException {
            List<Customer> customers = bank.getAllCustomers();
            List<Account> accounts = bank.getAllAccounts();
            List<Transaction> transactions = bank.getTransactionDAO().findAll();

            long totalCust = customers.size();
            long totalAcc = accounts.size();
            long totalTxn = transactions.size();

            BigDecimal totalDeposits = BigDecimal.ZERO;
            BigDecimal totalNetWorth = BigDecimal.ZERO;
            long activeAcc = 0;
            long frozenAcc = 0;
            long closedAcc = 0;
            long savingsCount = 0;
            long currentCount = 0;
            BigDecimal totalOverdraftSanctioned = BigDecimal.ZERO;
            BigDecimal totalOverdraftUtilized = BigDecimal.ZERO;

            for (Account acc : accounts) {
                totalNetWorth = totalNetWorth.add(acc.getBalance());
                if (acc.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    totalDeposits = totalDeposits.add(acc.getBalance());
                }

                if (acc.getStatus() == AccountStatus.ACTIVE) activeAcc++;
                else if (acc.getStatus() == AccountStatus.FROZEN) frozenAcc++;
                else if (acc.getStatus() == AccountStatus.CLOSED) closedAcc++;

                if (acc instanceof SavingsAccount) {
                    savingsCount++;
                } else if (acc instanceof CurrentAccount cur) {
                    currentCount++;
                    totalOverdraftSanctioned = totalOverdraftSanctioned.add(cur.getOverdraftLimit());
                    if (cur.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                        totalOverdraftUtilized = totalOverdraftUtilized.add(cur.getBalance().abs());
                    }
                }
            }

            // Recent 8 transactions
            List<Transaction> recent = transactions.stream()
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .limit(8)
                    .collect(Collectors.toList());

            String json = String.format("""
                {
                  "totalCustomers": %d,
                  "totalAccounts": %d,
                  "totalTransactions": %d,
                  "totalDeposits": %s,
                  "totalNetWorth": %s,
                  "activeAccountsCount": %d,
                  "frozenAccountsCount": %d,
                  "closedAccountsCount": %d,
                  "savingsAccountsCount": %d,
                  "currentAccountsCount": %d,
                  "totalOverdraftSanctioned": %s,
                  "totalOverdraftUtilized": %s,
                  "recentTransactions": %s
                }
                """,
                    totalCust, totalAcc, totalTxn,
                    totalDeposits.setScale(2, RoundingMode.HALF_UP),
                    totalNetWorth.setScale(2, RoundingMode.HALF_UP),
                    activeAcc, frozenAcc, closedAcc, savingsCount, currentCount,
                    totalOverdraftSanctioned.setScale(2, RoundingMode.HALF_UP),
                    totalOverdraftUtilized.setScale(2, RoundingMode.HALF_UP),
                    JsonUtil.transactionsToJson(recent)
            );

            sendResponse(exchange, 200, json);
        }

        private void handleAnalytics(HttpExchange exchange) throws IOException {
            List<Account> accounts = bank.getAllAccounts();
            List<Transaction> transactions = bank.getTransactionDAO().findAll();

            // 1. Account type split
            BigDecimal savingsTotal = BigDecimal.ZERO;
            BigDecimal currentTotal = BigDecimal.ZERO;
            int savingsCount = 0;
            int currentCount = 0;

            for (Account acc : accounts) {
                if (acc instanceof SavingsAccount) {
                    savingsCount++;
                    savingsTotal = savingsTotal.add(acc.getBalance());
                } else if (acc instanceof CurrentAccount) {
                    currentCount++;
                    currentTotal = currentTotal.add(acc.getBalance());
                }
            }

            // 2. Transaction status split
            long successTxn = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.SUCCESS).count();
            long failedTxn = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count();
            long reversedTxn = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.REVERSED).count();

            // 3. Transaction type volume
            BigDecimal totalDepositsVolume = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.DEPOSIT)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalWithdrawalsVolume = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalTransfersVolume = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.TRANSFER_OUT)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 4. Overdrawn accounts credit risk list
            List<Account> overdrawn = accounts.stream()
                    .filter(a -> a instanceof CurrentAccount && a.getBalance().compareTo(BigDecimal.ZERO) < 0)
                    .collect(Collectors.toList());

            // 5. Top accounts by balance
            List<Account> topAccounts = accounts.stream()
                    .sorted((a, b) -> b.getBalance().compareTo(a.getBalance()))
                    .limit(6)
                    .collect(Collectors.toList());

            String json = String.format("""
                {
                  "savingsCount": %d,
                  "currentCount": %d,
                  "savingsBalanceTotal": %s,
                  "currentBalanceTotal": %s,
                  "successTransactions": %d,
                  "failedTransactions": %d,
                  "reversedTransactions": %d,
                  "totalDepositsVolume": %s,
                  "totalWithdrawalsVolume": %s,
                  "totalTransfersVolume": %s,
                  "overdrawnAccounts": %s,
                  "topAccounts": %s
                }
                """,
                    savingsCount, currentCount,
                    savingsTotal.setScale(2, RoundingMode.HALF_UP),
                    currentTotal.setScale(2, RoundingMode.HALF_UP),
                    successTxn, failedTxn, reversedTxn,
                    totalDepositsVolume.setScale(2, RoundingMode.HALF_UP),
                    totalWithdrawalsVolume.setScale(2, RoundingMode.HALF_UP),
                    totalTransfersVolume.setScale(2, RoundingMode.HALF_UP),
                    JsonUtil.accountsToJson(overdrawn),
                    JsonUtil.accountsToJson(topAccounts)
            );

            sendResponse(exchange, 200, json);
        }

        private void handleListCustomers(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
            String q = queryParams.get("q");
            List<Customer> list = bank.searchCustomers(q);
            sendResponse(exchange, 200, JsonUtil.customersToJson(list));
        }

        private void handleGetCustomer(HttpExchange exchange, String customerId) throws Exception {
            Customer customer = bank.findCustomer(customerId);
            sendResponse(exchange, 200, JsonUtil.toJson(customer, true));
        }

        private void handleGetCustomerAccounts(HttpExchange exchange, String customerId) throws Exception {
            List<Account> accounts = bank.findAccountsByCustomer(customerId);
            sendResponse(exchange, 200, JsonUtil.accountsToJson(accounts));
        }

        private void handleCreateCustomer(HttpExchange exchange) throws Exception {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);

            String firstName = JsonUtil.getString(req, "firstName");
            String lastName = JsonUtil.getString(req, "lastName");
            String email = JsonUtil.getString(req, "email");
            String phone = JsonUtil.getString(req, "phone");
            String address = JsonUtil.getString(req, "address");

            Customer created = bank.registerCustomer(firstName, lastName, email, phone, address);
            sendResponse(exchange, 201, JsonUtil.toJson(created, false));
        }

        private void handleUpdateCustomer(HttpExchange exchange, String customerId) throws Exception {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);

            String firstName = JsonUtil.getString(req, "firstName");
            String lastName = JsonUtil.getString(req, "lastName");
            String phone = JsonUtil.getString(req, "phone");
            String address = JsonUtil.getString(req, "address");

            Customer updated = bank.updateCustomer(customerId, firstName, lastName, phone, address);
            sendResponse(exchange, 200, JsonUtil.toJson(updated, false));
        }

        private void handleListAccounts(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
            List<Account> list = bank.getAllAccounts();

            String type = queryParams.get("type");
            String status = queryParams.get("status");
            String q = queryParams.get("q");

            if (type != null && !type.isBlank()) {
                list = list.stream()
                        .filter(a -> a.getAccountType().name().equalsIgnoreCase(type.trim()))
                        .collect(Collectors.toList());
            }
            if (status != null && !status.isBlank()) {
                list = list.stream()
                        .filter(a -> a.getStatus().name().equalsIgnoreCase(status.trim()))
                        .collect(Collectors.toList());
            }
            if (q != null && !q.isBlank()) {
                String search = q.trim().toLowerCase();
                list = list.stream()
                        .filter(a -> a.getAccountNumber().toLowerCase().contains(search) ||
                                a.getAccountHolderName().toLowerCase().contains(search) ||
                                a.getCustomerId().toLowerCase().contains(search))
                        .collect(Collectors.toList());
            }

            sendResponse(exchange, 200, JsonUtil.accountsToJson(list));
        }

        private void handleGetAccount(HttpExchange exchange, String accountNumber) throws Exception {
            Account account = bank.findAccount(accountNumber);
            sendResponse(exchange, 200, JsonUtil.toJson(account));
        }

        private void handleCreateSavingsAccount(HttpExchange exchange) throws Exception {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);

            String customerId = JsonUtil.getString(req, "customerId");
            BigDecimal initialDeposit = JsonUtil.getBigDecimal(req, "initialDeposit");
            BigDecimal interestRate = JsonUtil.getBigDecimal(req, "interestRate");
            BigDecimal minBalance = JsonUtil.getBigDecimal(req, "minimumBalance");
            BigDecimal maxWithdrawal = JsonUtil.getBigDecimal(req, "maxWithdrawalLimit");

            SavingsAccount created;
            if (interestRate != null || minBalance != null || maxWithdrawal != null) {
                BigDecimal ir = interestRate != null ? interestRate : SavingsAccount.DEFAULT_INTEREST_RATE;
                BigDecimal mb = minBalance != null ? minBalance : SavingsAccount.DEFAULT_MINIMUM_BALANCE;
                BigDecimal mw = maxWithdrawal != null ? maxWithdrawal : SavingsAccount.DEFAULT_MAX_WITHDRAWAL_LIMIT;
                created = bank.openSavingsAccount(customerId, initialDeposit, ir, mb, mw);
            } else {
                created = bank.openSavingsAccount(customerId, initialDeposit);
            }

            sendResponse(exchange, 201, JsonUtil.toJson(created));
        }

        private void handleCreateCurrentAccount(HttpExchange exchange) throws Exception {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);

            String customerId = JsonUtil.getString(req, "customerId");
            BigDecimal initialDeposit = JsonUtil.getBigDecimal(req, "initialDeposit");
            BigDecimal overdraftLimit = JsonUtil.getBigDecimal(req, "overdraftLimit");
            BigDecimal overdraftInterestRate = JsonUtil.getBigDecimal(req, "overdraftInterestRate");

            CurrentAccount created;
            if (overdraftLimit != null || overdraftInterestRate != null) {
                BigDecimal od = overdraftLimit != null ? overdraftLimit : CurrentAccount.DEFAULT_OVERDRAFT_LIMIT;
                BigDecimal ir = overdraftInterestRate != null ? overdraftInterestRate : CurrentAccount.DEFAULT_OVERDRAFT_INTEREST_RATE;
                created = bank.openCurrentAccount(customerId, initialDeposit, od, ir);
            } else {
                created = bank.openCurrentAccount(customerId, initialDeposit);
            }

            sendResponse(exchange, 201, JsonUtil.toJson(created));
        }

        private void handleUpdateAccountStatus(HttpExchange exchange, String accountNumber) throws Exception {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);

            String statusStr = JsonUtil.getString(req, "status");
            if (statusStr == null) {
                throw new IllegalArgumentException("Target account status is required.");
            }
            AccountStatus status = AccountStatus.valueOf(statusStr.trim().toUpperCase());
            Account updated = bank.updateAccountStatus(accountNumber, status);
            sendResponse(exchange, 200, JsonUtil.toJson(updated));
        }

        private void handleGetAccountTransactions(HttpExchange exchange, String accountNumber) throws Exception {
            List<Transaction> statement = bank.getAccountStatement(accountNumber);
            sendResponse(exchange, 200, JsonUtil.transactionsToJson(statement));
        }

        private void handleApplyInterest(HttpExchange exchange, String accountNumber) throws Exception {
            Transaction txn = bank.applyInterest(accountNumber);
            sendResponse(exchange, 200, JsonUtil.toJson(txn));
        }

        private void handleListTransactions(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
            List<Transaction> list = bank.getTransactionDAO().findAll();

            String accNo = queryParams.get("accountNumber");
            String type = queryParams.get("type");
            String status = queryParams.get("status");
            String limitStr = queryParams.get("limit");

            if (accNo != null && !accNo.isBlank()) {
                list = list.stream()
                        .filter(t -> t.getAccountNumber().equalsIgnoreCase(accNo.trim()) ||
                                (t.getRelatedAccountNumber() != null && t.getRelatedAccountNumber().equalsIgnoreCase(accNo.trim())))
                        .collect(Collectors.toList());
            }
            if (type != null && !type.isBlank()) {
                list = list.stream()
                        .filter(t -> t.getType().name().equalsIgnoreCase(type.trim()))
                        .collect(Collectors.toList());
            }
            if (status != null && !status.isBlank()) {
                list = list.stream()
                        .filter(t -> t.getStatus().name().equalsIgnoreCase(status.trim()))
                        .collect(Collectors.toList());
            }

            list = list.stream()
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .collect(Collectors.toList());

            if (limitStr != null) {
                try {
                    int limit = Integer.parseInt(limitStr.trim());
                    if (limit > 0 && limit < list.size()) {
                        list = list.subList(0, limit);
                    }
                } catch (NumberFormatException ignored) {}
            }

            sendResponse(exchange, 200, JsonUtil.transactionsToJson(list));
        }

        private void handleGetTransaction(HttpExchange exchange, String txnId) throws Exception {
            Optional<Transaction> txn = bank.getTransactionDAO().findById(txnId);
            if (txn.isEmpty()) {
                sendResponse(exchange, 404, JsonUtil.errorJson("Transaction not found: " + txnId, "ERR_TXN_NOT_FOUND"));
            } else {
                sendResponse(exchange, 200, JsonUtil.toJson(txn.get()));
            }
        }

        private void handleDeposit(HttpExchange exchange) throws Exception {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);

            String accountNumber = JsonUtil.getString(req, "accountNumber");
            BigDecimal amount = JsonUtil.getBigDecimal(req, "amount");
            String remarks = JsonUtil.getString(req, "remarks");

            if (remarks == null || remarks.isBlank()) {
                remarks = "Deposit via Web Portal";
            }

            Transaction txn = bank.deposit(accountNumber, amount, remarks);
            sendResponse(exchange, 200, JsonUtil.toJson(txn));
        }

        private void handleWithdraw(HttpExchange exchange) throws Exception {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);

            String accountNumber = JsonUtil.getString(req, "accountNumber");
            BigDecimal amount = JsonUtil.getBigDecimal(req, "amount");
            String remarks = JsonUtil.getString(req, "remarks");

            if (remarks == null || remarks.isBlank()) {
                remarks = "Withdrawal via Web Portal";
            }

            Transaction txn = bank.withdraw(accountNumber, amount, remarks);
            sendResponse(exchange, 200, JsonUtil.toJson(txn));
        }

        private void handleTransfer(HttpExchange exchange) throws Exception {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);

            String source = JsonUtil.getString(req, "sourceAccountNumber");
            String destination = JsonUtil.getString(req, "destinationAccountNumber");
            BigDecimal amount = JsonUtil.getBigDecimal(req, "amount");
            String remarks = JsonUtil.getString(req, "remarks");

            if (remarks == null || remarks.isBlank()) {
                remarks = "Transfer via Web Portal";
            }

            List<Transaction> txns = bank.transfer(source, destination, amount, remarks);
            sendResponse(exchange, 200, JsonUtil.transactionsToJson(txns));
        }

        private void handleDatabaseReseed(HttpExchange exchange) throws IOException {
            DatabaseSeeder.seed(bank, true);
            sendResponse(exchange, 200, JsonUtil.successJson("Database schema reset and successfully re-seeded with demo data.", null));
        }

        private void handleDatabaseReload(HttpExchange exchange) throws IOException {
            bank.reloadFromDatabase();
            sendResponse(exchange, 200, JsonUtil.successJson("In-memory cache reloaded and synchronized with SQLite database.", null));
        }

        private void handleDatabaseTables(HttpExchange exchange) throws IOException {
            Map<String, String> descriptions = Map.of(
                    "customers", "Customer demographic & contact entities",
                    "accounts", "Polymorphic Savings & Current account records",
                    "transactions", "ACID financial ledger transaction history",
                    "loans", "Customer loan facilities & monthly EMI schedules",
                    "cards", "Debit & Credit cards with network security limits"
            );

            List<String> tableEntries = new ArrayList<>();
            try (java.sql.Connection conn = DBConnection.getConnection();
                 java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (!name.startsWith("sqlite_")) {
                        int rowCount = 0;
                        try (java.sql.Statement cStmt = conn.createStatement();
                             java.sql.ResultSet cRs = cStmt.executeQuery("SELECT COUNT(*) FROM " + name)) {
                            if (cRs.next()) rowCount = cRs.getInt(1);
                        } catch (Exception ignored) {}
                        String desc = descriptions.getOrDefault(name, "Relational banking entity");
                        tableEntries.add(String.format("{\"name\": \"%s\", \"rowCount\": %d, \"description\": \"%s\"}",
                                JsonUtil.escape(name), rowCount, JsonUtil.escape(desc)));
                    }
                }
            } catch (Exception e) {
                tableEntries.add(String.format("{\"name\":\"customers\",\"rowCount\":%d,\"description\":\"Customer demographics\"}", bank.getAllCustomers().size()));
                tableEntries.add(String.format("{\"name\":\"accounts\",\"rowCount\":%d,\"description\":\"Account records\"}", bank.getAllAccounts().size()));
                tableEntries.add(String.format("{\"name\":\"transactions\",\"rowCount\":%d,\"description\":\"Ledger records\"}", bank.getTransactionDAO().findAll().size()));
            }

            String json = String.format("""
                {
                  "databaseUrl": "%s",
                  "tables": [%s]
                }
                """,
                    JsonUtil.escape(DBConnection.getJdbcUrl()),
                    String.join(", ", tableEntries)
            );
            sendResponse(exchange, 200, json);
        }

        private void handleGetQueries(HttpExchange exchange) throws IOException {
            try {
                List<SqlRunner.QueryBlock> queries = SqlRunner.parseQueriesWithTitles("queries.sql");
                StringBuilder sb = new StringBuilder();
                sb.append("{\"queries\":[");
                for (int i = 0; i < queries.size(); i++) {
                    SqlRunner.QueryBlock q = queries.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"id\":%d,\"title\":\"%s\",\"sql\":\"%s\"}",
                            q.getIndex(), JsonUtil.escape(q.getTitle()), JsonUtil.escape(q.getSql())));
                }
                sb.append("]}");
                sendResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, JsonUtil.errorJson("Failed to parse queries: " + e.getMessage(), "ERR_QUERIES"));
            }
        }

        private void handleRunQuery(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            int targetId = 1;
            if (params.containsKey("id")) {
                try {
                    targetId = Integer.parseInt(params.get("id"));
                } catch (NumberFormatException ignored) {}
            }

            try {
                List<SqlRunner.QueryBlock> queries = SqlRunner.parseQueriesWithTitles("queries.sql");
                SqlRunner.QueryBlock target = null;
                for (SqlRunner.QueryBlock q : queries) {
                    if (q.getIndex() == targetId) {
                        target = q;
                        break;
                    }
                }

                if (target == null) {
                    sendResponse(exchange, 404, JsonUtil.errorJson("Query #" + targetId + " not found. Total queries: " + queries.size(), "ERR_NOT_FOUND"));
                    return;
                }

                long start = System.currentTimeMillis();
                try (java.sql.Connection conn = DBConnection.getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    boolean isRs = stmt.execute(target.getSql());
                    if (isRs) {
                        try (java.sql.ResultSet rs = stmt.getResultSet()) {
                            java.sql.ResultSetMetaData meta = rs.getMetaData();
                            int colCount = meta.getColumnCount();
                            List<String> colNames = new ArrayList<>();
                            for (int c = 1; c <= colCount; c++) {
                                colNames.add("\"" + JsonUtil.escape(meta.getColumnLabel(c)) + "\"");
                            }

                            List<String> rowList = new ArrayList<>();
                            while (rs.next()) {
                                List<String> cells = new ArrayList<>();
                                for (int c = 1; c <= colCount; c++) {
                                    String val = rs.getString(c);
                                    cells.add(val == null ? "null" : "\"" + JsonUtil.escape(val) + "\"");
                                }
                                rowList.add("[" + String.join(",", cells) + "]");
                            }
                            long elapsed = System.currentTimeMillis() - start;

                            String json = String.format("""
                                {
                                  "id": %d,
                                  "title": "%s",
                                  "columns": [%s],
                                  "rows": [%s],
                                  "rowCount": %d,
                                  "executionMs": %d
                                }
                                """,
                                    target.getIndex(),
                                    JsonUtil.escape(target.getTitle()),
                                    String.join(",", colNames),
                                    String.join(",", rowList),
                                    rowList.size(),
                                    elapsed
                            );
                            sendResponse(exchange, 200, json);
                        }
                    } else {
                        int affected = stmt.getUpdateCount();
                        long elapsed = System.currentTimeMillis() - start;
                        String json = String.format("{\"id\":%d,\"title\":\"%s\",\"rowsAffected\":%d,\"executionMs\":%d}",
                                target.getIndex(), JsonUtil.escape(target.getTitle()), affected, elapsed);
                        sendResponse(exchange, 200, json);
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, JsonUtil.errorJson("Query execution failed: " + e.getMessage(), "ERR_QUERY_EXEC"));
            }
        }

        // ==================== Response & Request Helpers ====================

        private void sendResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String readBody(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query == null || query.isBlank()) return params;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    params.put(k, v);
                } else if (!pair.isBlank()) {
                    params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            }
            return params;
        }
    }
}
