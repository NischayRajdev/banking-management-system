package com.bank.api;

import com.bank.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Lightweight, zero-dependency JSON serializer and parser tailored for the Banking REST API.
 */
public final class JsonUtil {

    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private JsonUtil() {}

    // ==================== String Escaping ====================

    public static String escape(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32 || c >= 127) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    // ==================== Domain Serialization ====================

    public static String toJson(Customer c, boolean includeAccounts) {
        if (c == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"customerId\":\"").append(escape(c.getCustomerId())).append("\",");
        sb.append("\"firstName\":\"").append(escape(c.getFirstName())).append("\",");
        sb.append("\"lastName\":\"").append(escape(c.getLastName())).append("\",");
        sb.append("\"fullName\":\"").append(escape(c.getFullName())).append("\",");
        sb.append("\"email\":\"").append(escape(c.getEmail())).append("\",");
        sb.append("\"phoneNumber\":\"").append(escape(c.getPhoneNumber())).append("\",");
        sb.append("\"address\":\"").append(escape(c.getAddress())).append("\",");
        sb.append("\"createdAt\":\"").append(c.getCreatedAt() != null ? c.getCreatedAt().format(ISO_DT) : "").append("\",");
        sb.append("\"accountCount\":").append(c.getAccounts().size()).append(",");
        sb.append("\"totalPortfolioBalance\":").append(c.getTotalPortfolioBalance());

        if (includeAccounts) {
            sb.append(",\"accounts\":[");
            List<Account> accs = c.getAccounts();
            for (int i = 0; i < accs.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(accs.get(i)));
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String toJson(Account acc) {
        if (acc == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"accountNumber\":\"").append(escape(acc.getAccountNumber())).append("\",");
        sb.append("\"customerId\":\"").append(escape(acc.getCustomerId())).append("\",");
        sb.append("\"accountHolderName\":\"").append(escape(acc.getAccountHolderName())).append("\",");
        sb.append("\"balance\":").append(acc.getBalance()).append(",");
        sb.append("\"accountType\":\"").append(acc.getAccountType().name()).append("\",");
        sb.append("\"accountTypeDisplay\":\"").append(escape(acc.getAccountType().getDisplayName())).append("\",");
        sb.append("\"status\":\"").append(acc.getStatus().name()).append("\",");
        sb.append("\"createdAt\":\"").append(acc.getCreatedAt() != null ? acc.getCreatedAt().format(ISO_DT) : "").append("\"");

        if (acc instanceof SavingsAccount sav) {
            sb.append(",\"interestRate\":").append(sav.getInterestRate());
            sb.append(",\"minimumBalance\":").append(sav.getMinimumBalance());
            sb.append(",\"maxWithdrawalPerTransaction\":").append(sav.getMaxWithdrawalPerTransaction());
            sb.append(",\"annualInterestAccrual\":").append(sav.calculateInterest());
        } else if (acc instanceof CurrentAccount cur) {
            BigDecimal bal = cur.getBalance();
            BigDecimal overdrawn = bal.compareTo(BigDecimal.ZERO) < 0 ? bal.abs() : BigDecimal.ZERO;
            BigDecimal remainingOverdraft = cur.getOverdraftLimit().subtract(overdrawn);

            sb.append(",\"overdraftLimit\":").append(cur.getOverdraftLimit());
            sb.append(",\"overdraftInterestRate\":").append(cur.getOverdraftInterestRate());
            sb.append(",\"utilizedOverdraft\":").append(overdrawn);
            sb.append(",\"availableOverdraft\":").append(remainingOverdraft);
            sb.append(",\"totalAvailableLiquidity\":").append(cur.getTotalAvailableLiquidity());
            sb.append(",\"annualInterestAccrual\":").append(cur.calculateInterest());
        }

        sb.append("}");
        return sb.toString();
    }

    public static String toJson(Transaction t) {
        if (t == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"transactionId\":\"").append(escape(t.getTransactionId())).append("\",");
        sb.append("\"accountNumber\":\"").append(escape(t.getAccountNumber())).append("\",");
        sb.append("\"relatedAccountNumber\":").append(t.getRelatedAccountNumber() != null ? "\"" + escape(t.getRelatedAccountNumber()) + "\"" : "null").append(",");
        sb.append("\"type\":\"").append(t.getType().name()).append("\",");
        sb.append("\"typeDescription\":\"").append(escape(t.getType().getDescription())).append("\",");
        sb.append("\"amount\":").append(t.getAmount()).append(",");
        sb.append("\"balanceAfter\":").append(t.getBalanceAfter()).append(",");
        sb.append("\"status\":\"").append(t.getStatus().name()).append("\",");
        sb.append("\"remarks\":\"").append(escape(t.getRemarks())).append("\",");
        sb.append("\"timestamp\":\"").append(t.getTimestamp() != null ? t.getTimestamp().format(ISO_DT) : "").append("\"");
        sb.append("}");
        return sb.toString();
    }

    public static String customersToJson(List<Customer> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i), false));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String accountsToJson(List<Account> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String transactionsToJson(List<Transaction> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String errorJson(String message, String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":false,\"error\":\"").append(escape(message)).append("\"");
        if (code != null) {
            sb.append(",\"code\":\"").append(escape(code)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String successJson(String message, String dataJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true,\"message\":\"").append(escape(message)).append("\"");
        if (dataJson != null) {
            sb.append(",\"data\":").append(dataJson);
        }
        sb.append("}");
        return sb.toString();
    }

    // ==================== Simple JSON Parser ====================

    public static Map<String, Object> parseJsonObject(String json) {
        if (json == null) return Collections.emptyMap();
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return Collections.emptyMap();
        }
        return new JsonParser(json).parseObject();
    }

    private static class JsonParser {
        private final String src;
        private int idx = 0;

        JsonParser(String src) {
            this.src = src;
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (idx >= src.length() || src.charAt(idx) != '{') return map;
            idx++; // skip '{'

            while (idx < src.length()) {
                skipWhitespace();
                if (idx < src.length() && src.charAt(idx) == '}') {
                    idx++;
                    break;
                }
                String key = parseString();
                skipWhitespace();
                if (idx < src.length() && src.charAt(idx) == ':') {
                    idx++;
                }
                skipWhitespace();
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (idx < src.length() && src.charAt(idx) == ',') {
                    idx++;
                } else if (idx < src.length() && src.charAt(idx) == '}') {
                    idx++;
                    break;
                }
            }
            return map;
        }

        private Object parseValue() {
            skipWhitespace();
            if (idx >= src.length()) return null;
            char c = src.charAt(idx);
            if (c == '"') {
                return parseString();
            } else if (c == '{') {
                return parseObject();
            } else if (c == '[') {
                return parseArray();
            } else if (c == 't' || c == 'f') {
                return parseBoolean();
            } else if (c == 'n') {
                return parseNull();
            } else {
                return parseNumber();
            }
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            idx++; // skip '['
            while (idx < src.length()) {
                skipWhitespace();
                if (idx < src.length() && src.charAt(idx) == ']') {
                    idx++;
                    break;
                }
                list.add(parseValue());
                skipWhitespace();
                if (idx < src.length() && src.charAt(idx) == ',') {
                    idx++;
                } else if (idx < src.length() && src.charAt(idx) == ']') {
                    idx++;
                    break;
                }
            }
            return list;
        }

        private String parseString() {
            if (idx >= src.length() || src.charAt(idx) != '"') return "";
            idx++; // skip opening quote
            StringBuilder sb = new StringBuilder();
            while (idx < src.length()) {
                char c = src.charAt(idx++);
                if (c == '"') {
                    break;
                } else if (c == '\\' && idx < src.length()) {
                    char esc = src.charAt(idx++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (idx + 4 <= src.length()) {
                                String hex = src.substring(idx, idx + 4);
                                sb.append((char) Integer.parseInt(hex, 16));
                                idx += 4;
                            }
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", idx)) {
                idx += 4;
                return Boolean.TRUE;
            } else if (src.startsWith("false", idx)) {
                idx += 5;
                return Boolean.FALSE;
            }
            return null;
        }

        private Object parseNull() {
            if (src.startsWith("null", idx)) {
                idx += 4;
            }
            return null;
        }

        private Object parseNumber() {
            int start = idx;
            if (idx < src.length() && (src.charAt(idx) == '-' || src.charAt(idx) == '+')) {
                idx++;
            }
            boolean isDecimal = false;
            while (idx < src.length()) {
                char c = src.charAt(idx);
                if (Character.isDigit(c)) {
                    idx++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    isDecimal = true;
                    idx++;
                } else {
                    break;
                }
            }
            String numStr = src.substring(start, idx).trim();
            try {
                if (isDecimal) {
                    return new BigDecimal(numStr);
                } else {
                    return Long.parseLong(numStr);
                }
            } catch (Exception e) {
                return numStr;
            }
        }

        private void skipWhitespace() {
            while (idx < src.length() && Character.isWhitespace(src.charAt(idx))) {
                idx++;
            }
        }
    }

    public static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString().trim() : null;
    }

    public static BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(v.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
}

