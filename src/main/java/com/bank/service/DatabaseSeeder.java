package com.bank.service;

import com.bank.dao.AccountDAO;
import com.bank.dao.CustomerDAO;
import com.bank.dao.TransactionDAO;
import com.bank.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

// DatabaseSeeder: Seeds initial mock Indian banking dataset with customers, accounts, and transactions
public class DatabaseSeeder {

    // seedIfEmpty: Seeds database if customer registry is empty
    public static void seedIfEmpty(Bank bank) {
        if (bank.getAllCustomers().isEmpty()) {
            seed(bank, false);
        }
    }

    // seed: Seeds database with Indian customers, accounts in Rupees, and transactions
    public static void seed(Bank bank, boolean forceReset) {
        if (forceReset) {
            bank.resetDatabaseAndReload();
        }

        CustomerDAO customerDAO = bank.getCustomerDAO();
        AccountDAO accountDAO = bank.getAccountDAO();
        TransactionDAO transactionDAO = bank.getTransactionDAO();

        List<CustomerData> customerProfiles = List.of(
                new CustomerData("CUST-1001", "Aarav", "Sharma", "aarav.sharma@example.com", "+91-98201-10001", "123 Marine Drive, Mumbai, MH", 60),
                new CustomerData("CUST-1002", "Priya", "Patel", "priya.patel@example.com", "+91-98201-10002", "456 Brigade Road, Bengaluru, KA", 58),
                new CustomerData("CUST-1003", "Rohit", "Verma", "rohit.verma@example.com", "+91-98201-10003", "789 Connaught Place, New Delhi, DL", 55),
                new CustomerData("CUST-1004", "Ananya", "Iyer", "ananya.iyer@example.com", "+91-98201-10004", "12 Anna Salai, Chennai, TN", 52),
                new CustomerData("CUST-1005", "Vikram", "Malhotra", "vikram.malhotra@example.com", "+91-98201-10005", "888 Park Street, Kolkata, WB", 50),
                new CustomerData("CUST-1006", "Neha", "Reddy", "neha.reddy@example.com", "+91-98201-10006", "320 Banjara Hills, Hyderabad, TS", 48),
                new CustomerData("CUST-1007", "Aditya", "Gupta", "aditya.gupta@example.com", "+91-98201-10007", "100 FC Road, Pune, MH", 45),
                new CustomerData("CUST-1008", "Ishita", "Nair", "ishita.nair@example.com", "+91-98201-10008", "550 MG Road, Kochi, KL", 42),
                new CustomerData("CUST-1009", "Siddharth", "Mehta", "siddharth.mehta@example.com", "+91-98201-10009", "200 SG Highway, Ahmedabad, GJ", 40),
                new CustomerData("CUST-1010", "Kavita", "Rao", "kavita.rao@example.com", "+91-98201-10010", "440 Mall Road, Jaipur, RJ", 38),
                new CustomerData("CUST-1011", "Arjun", "Das", "arjun.das@example.com", "+91-98201-10011", "750 Salt Lake, Kolkata, WB", 35),
                new CustomerData("CUST-1012", "Meera", "Joshi", "meera.joshi@example.com", "+91-98201-10012", "16 Law Garden, Ahmedabad, GJ", 32),
                new CustomerData("CUST-1013", "Rajesh", "Kulkarni", "rajesh.kulkarni@example.com", "+91-98201-10013", "910 Deccan Gymkhana, Pune, MH", 30),
                new CustomerData("CUST-1014", "Sneha", "Sen", "sneha.sen@example.com", "+91-98201-10014", "500 Gariahat Road, Kolkata, WB", 28),
                new CustomerData("CUST-1015", "Varun", "Kapoor", "varun.kapoor@example.com", "+91-98201-10015", "230 South City, Gurugram, HR", 25),
                new CustomerData("CUST-1016", "Pooja", "Bhatia", "pooja.bhatia@example.com", "+91-98201-10016", "400 Indiranagar, Bengaluru, KA", 22)
        );

        for (CustomerData cd : customerProfiles) {
            Customer cust = new Customer(
                    cd.id, cd.first, cd.last, cd.email, cd.phone, cd.address,
                    LocalDateTime.now().minusDays(cd.daysAgo)
            );
            customerDAO.save(cust);
        }

        List<Account> seedAccounts = new ArrayList<>();

        // CUST-1001 Aarav Sharma
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5001", "CUST-1001", "Aarav Sharma",
                new BigDecimal("142500.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(59),
                new BigDecimal("0.0450"), new BigDecimal("5000.00"), new BigDecimal("50000.00")
        ));
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8001", "CUST-1001", "Aarav Sharma",
                new BigDecimal("38000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(59),
                new BigDecimal("25000.00"), new BigDecimal("0.1000")
        ));

        // CUST-1002 Priya Patel
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8002", "CUST-1002", "Priya Patel",
                new BigDecimal("14500.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(57),
                new BigDecimal("30000.00"), new BigDecimal("0.1200")
        ));
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5002", "CUST-1002", "Priya Patel",
                new BigDecimal("86000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(57),
                new BigDecimal("0.0400"), new BigDecimal("2500.00"), new BigDecimal("25000.00")
        ));

        // CUST-1003 Rohit Verma
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5003", "CUST-1003", "Rohit Verma",
                new BigDecimal("1450000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(54),
                new BigDecimal("0.0525"), new BigDecimal("10000.00"), new BigDecimal("250000.00")
        ));
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8003", "CUST-1003", "Rohit Verma",
                new BigDecimal("823000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(54),
                new BigDecimal("250000.00"), new BigDecimal("0.0800")
        ));

        // CUST-1004 Ananya Iyer
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5004", "CUST-1004", "Ananya Iyer",
                new BigDecimal("324000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(51),
                new BigDecimal("0.0420"), new BigDecimal("10000.00"), new BigDecimal("100000.00")
        ));
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8004", "CUST-1004", "Ananya Iyer",
                new BigDecimal("118500.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(51),
                new BigDecimal("100000.00"), new BigDecimal("0.0900")
        ));

        // CUST-1005 Vikram Malhotra
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8005", "CUST-1005", "Vikram Malhotra",
                new BigDecimal("589000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(49),
                new BigDecimal("300000.00"), new BigDecimal("0.0850")
        ));
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5005", "CUST-1005", "Vikram Malhotra",
                new BigDecimal("475000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(49),
                new BigDecimal("0.0480"), new BigDecimal("50000.00"), new BigDecimal("150000.00")
        ));

        // CUST-1006 Neha Reddy (Overdrawn demo)
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8006", "CUST-1006", "Neha Reddy",
                new BigDecimal("-16500.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(47),
                new BigDecimal("80000.00"), new BigDecimal("0.1200")
        ));
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5006", "CUST-1006", "Neha Reddy",
                new BigDecimal("28500.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(47),
                new BigDecimal("0.0380"), new BigDecimal("5000.00"), new BigDecimal("20000.00")
        ));

        // CUST-1007 Aditya Gupta
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8007", "CUST-1007", "Aditya Gupta",
                new BigDecimal("234000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(44),
                new BigDecimal("100000.00"), new BigDecimal("0.0950")
        ));
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5007", "CUST-1007", "Aditya Gupta",
                new BigDecimal("412000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(44),
                new BigDecimal("0.0475"), new BigDecimal("10000.00"), new BigDecimal("80000.00")
        ));

        // CUST-1008 Ishita Nair
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8008", "CUST-1008", "Ishita Nair",
                new BigDecimal("96500.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(41),
                new BigDecimal("150000.00"), new BigDecimal("0.1100")
        ));

        // CUST-1009 Siddharth Mehta
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5008", "CUST-1009", "Siddharth Mehta",
                new BigDecimal("84000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(39),
                new BigDecimal("0.0410"), new BigDecimal("5000.00"), new BigDecimal("30000.00")
        ));
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8009", "CUST-1009", "Siddharth Mehta",
                new BigDecimal("31500.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(39),
                new BigDecimal("20000.00"), new BigDecimal("0.1000")
        ));

        // CUST-1010 Kavita Rao
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8010", "CUST-1010", "Kavita Rao",
                new BigDecimal("261000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(37),
                new BigDecimal("100000.00"), new BigDecimal("0.0900")
        ));
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5009", "CUST-1010", "Kavita Rao",
                new BigDecimal("378000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(37),
                new BigDecimal("0.0460"), new BigDecimal("10000.00"), new BigDecimal("75000.00")
        ));

        // CUST-1011 Arjun Das
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8011", "CUST-1011", "Arjun Das",
                new BigDecimal("157500.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(34),
                new BigDecimal("120000.00"), new BigDecimal("0.0850")
        ));

        // CUST-1012 Meera Joshi
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5010", "CUST-1012", "Meera Joshi",
                new BigDecimal("289000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(31),
                new BigDecimal("0.0440"), new BigDecimal("20000.00"), new BigDecimal("60000.00")
        ));

        // CUST-1013 Rajesh Kulkarni (Overdrawn buffer demo)
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8012", "CUST-1013", "Rajesh Kulkarni",
                new BigDecimal("-28000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(29),
                new BigDecimal("200000.00"), new BigDecimal("0.1050")
        ));
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5011", "CUST-1013", "Rajesh Kulkarni",
                new BigDecimal("715000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(29),
                new BigDecimal("0.0500"), new BigDecimal("50000.00"), new BigDecimal("200000.00")
        ));

        // CUST-1014 Sneha Sen
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5012", "CUST-1014", "Sneha Sen",
                new BigDecimal("142000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(27),
                new BigDecimal("0.0430"), new BigDecimal("5000.00"), new BigDecimal("40000.00")
        ));

        // CUST-1015 Varun Kapoor
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8013", "CUST-1015", "Varun Kapoor",
                new BigDecimal("119000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(24),
                new BigDecimal("50000.00"), new BigDecimal("0.1000")
        ));

        // CUST-1016 Pooja Bhatia
        seedAccounts.add(new SavingsAccount(
                "SAV-INDB-001-5013", "CUST-1016", "Pooja Bhatia",
                new BigDecimal("650000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(21),
                new BigDecimal("0.0490"), new BigDecimal("25000.00"), new BigDecimal("120000.00")
        ));
        seedAccounts.add(new CurrentAccount(
                "CUR-INDB-001-8014", "CUST-1016", "Pooja Bhatia",
                new BigDecimal("224000.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusDays(21),
                new BigDecimal("150000.00"), new BigDecimal("0.0900")
        ));

        for (Account acc : seedAccounts) {
            accountDAO.save(acc);
        }

        List<Transaction> seedTransactions = List.of(
                // Aarav Sharma
                new Transaction("TXN-10101", "SAV-INDB-001-5001", null, LocalDateTime.now().minusDays(55),
                        TransactionType.DEPOSIT, new BigDecimal("100000.00"), new BigDecimal("100000.00"),
                        TransactionStatus.SUCCESS, "Initial savings deposit"),
                new Transaction("TXN-10102", "SAV-INDB-001-5001", null, LocalDateTime.now().minusDays(35),
                        TransactionType.DEPOSIT, new BigDecimal("40000.00"), new BigDecimal("140000.00"),
                        TransactionStatus.SUCCESS, "Quarterly equity dividend transfer"),
                new Transaction("TXN-10103", "SAV-INDB-001-5001", null, LocalDateTime.now().minusDays(15),
                        TransactionType.INTEREST_CREDIT, new BigDecimal("2500.00"), new BigDecimal("142500.00"),
                        TransactionStatus.SUCCESS, "Semi-annual interest credit @ 4.5%"),
                new Transaction("TXN-10104", "CUR-INDB-001-8001", null, LocalDateTime.now().minusDays(50),
                        TransactionType.DEPOSIT, new BigDecimal("55000.00"), new BigDecimal("55000.00"),
                        TransactionStatus.SUCCESS, "TCS Salary direct deposit"),
                new Transaction("TXN-10105", "CUR-INDB-001-8001", null, LocalDateTime.now().minusDays(40),
                        TransactionType.WITHDRAWAL, new BigDecimal("12000.00"), new BigDecimal("43000.00"),
                        TransactionStatus.SUCCESS, "Apartment rent via NEFT"),
                new Transaction("TXN-10106", "CUR-INDB-001-8001", "CUR-INDB-001-8002", LocalDateTime.now().minusDays(20),
                        TransactionType.TRANSFER_OUT, new BigDecimal("5000.00"), new BigDecimal("38000.00"),
                        TransactionStatus.SUCCESS, "UPI transfer to Priya Patel"),

                // Priya Patel
                new Transaction("TXN-10201", "CUR-INDB-001-8002", "CUR-INDB-001-8001", LocalDateTime.now().minusDays(20),
                        TransactionType.TRANSFER_IN, new BigDecimal("5000.00"), new BigDecimal("22500.00"),
                        TransactionStatus.SUCCESS, "UPI transfer received from Aarav Sharma"),
                new Transaction("TXN-10202", "CUR-INDB-001-8002", null, LocalDateTime.now().minusDays(10),
                        TransactionType.WITHDRAWAL, new BigDecimal("8000.00"), new BigDecimal("14500.00"),
                        TransactionStatus.SUCCESS, "Electricity and internet bill payment"),
                new Transaction("TXN-10203", "SAV-INDB-001-5002", null, LocalDateTime.now().minusDays(45),
                        TransactionType.DEPOSIT, new BigDecimal("80000.00"), new BigDecimal("80000.00"),
                        TransactionStatus.SUCCESS, "Emergency fund deposit"),
                new Transaction("TXN-10204", "SAV-INDB-001-5002", null, LocalDateTime.now().minusDays(12),
                        TransactionType.DEPOSIT, new BigDecimal("6000.00"), new BigDecimal("86000.00"),
                        TransactionStatus.SUCCESS, "Monthly automated recurring savings"),

                // Rohit Verma
                new Transaction("TXN-10301", "SAV-INDB-001-5003", null, LocalDateTime.now().minusDays(50),
                        TransactionType.DEPOSIT, new BigDecimal("1400000.00"), new BigDecimal("1400000.00"),
                        TransactionStatus.SUCCESS, "Mutual Fund redemption proceeds"),
                new Transaction("TXN-10302", "SAV-INDB-001-5003", null, LocalDateTime.now().minusDays(22),
                        TransactionType.INTEREST_CREDIT, new BigDecimal("50000.00"), new BigDecimal("1450000.00"),
                        TransactionStatus.SUCCESS, "Fixed Deposit interest payout"),
                new Transaction("TXN-10303", "CUR-INDB-001-8003", null, LocalDateTime.now().minusDays(48),
                        TransactionType.DEPOSIT, new BigDecimal("950000.00"), new BigDecimal("950000.00"),
                        TransactionStatus.SUCCESS, "Consulting retainer advance"),
                new Transaction("TXN-10304", "CUR-INDB-001-8003", null, LocalDateTime.now().minusDays(18),
                        TransactionType.WITHDRAWAL, new BigDecimal("127000.00"), new BigDecimal("823000.00"),
                        TransactionStatus.SUCCESS, "GST quarterly payment"),

                // Ananya Iyer
                new Transaction("TXN-10401", "SAV-INDB-001-5004", null, LocalDateTime.now().minusDays(45),
                        TransactionType.DEPOSIT, new BigDecimal("300000.00"), new BigDecimal("300000.00"),
                        TransactionStatus.SUCCESS, "Fixed deposit maturity credit"),
                new Transaction("TXN-10402", "SAV-INDB-001-5004", null, LocalDateTime.now().minusDays(8),
                        TransactionType.DEPOSIT, new BigDecimal("24000.00"), new BigDecimal("324000.00"),
                        TransactionStatus.SUCCESS, "Medical consulting honorarium"),
                new Transaction("TXN-10403", "CUR-INDB-001-8004", null, LocalDateTime.now().minusDays(30),
                        TransactionType.DEPOSIT, new BigDecimal("165000.00"), new BigDecimal("165000.00"),
                        TransactionStatus.SUCCESS, "Insurance claim reimbursement"),
                new Transaction("TXN-10404", "CUR-INDB-001-8004", null, LocalDateTime.now().minusDays(14),
                        TransactionType.WITHDRAWAL, new BigDecimal("46500.00"), new BigDecimal("118500.00"),
                        TransactionStatus.SUCCESS, "Clinic equipment maintenance"),

                // Vikram Malhotra
                new Transaction("TXN-10501", "CUR-INDB-001-8005", null, LocalDateTime.now().minusDays(40),
                        TransactionType.DEPOSIT, new BigDecimal("650000.00"), new BigDecimal("650000.00"),
                        TransactionStatus.SUCCESS, "Commercial property rent credit"),
                new Transaction("TXN-10502", "CUR-INDB-001-8005", null, LocalDateTime.now().minusDays(15),
                        TransactionType.WITHDRAWAL, new BigDecimal("61000.00"), new BigDecimal("589000.00"),
                        TransactionStatus.SUCCESS, "Property insurance annual premium"),
                new Transaction("TXN-10503", "SAV-INDB-001-5005", null, LocalDateTime.now().minusDays(42),
                        TransactionType.DEPOSIT, new BigDecimal("475000.00"), new BigDecimal("475000.00"),
                        TransactionStatus.SUCCESS, "Tenant security deposit escrow"),

                // Neha Reddy (Overdrawn)
                new Transaction("TXN-10601", "CUR-INDB-001-8006", null, LocalDateTime.now().minusDays(40),
                        TransactionType.DEPOSIT, new BigDecimal("35000.00"), new BigDecimal("35000.00"),
                        TransactionStatus.SUCCESS, "Restaurant UPI sales settlement"),
                new Transaction("TXN-10602", "CUR-INDB-001-8006", null, LocalDateTime.now().minusDays(25),
                        TransactionType.WITHDRAWAL, new BigDecimal("48000.00"), new BigDecimal("-13000.00"),
                        TransactionStatus.SUCCESS, "Inventory supplier payment (Overdraft used)"),
                new Transaction("TXN-10603", "CUR-INDB-001-8006", null, LocalDateTime.now().minusDays(5),
                        TransactionType.FEE_DEBIT, new BigDecimal("3500.00"), new BigDecimal("-16500.00"),
                        TransactionStatus.SUCCESS, "Overdraft monthly interest charge"),
                new Transaction("TXN-10604", "SAV-INDB-001-5006", null, LocalDateTime.now().minusDays(35),
                        TransactionType.DEPOSIT, new BigDecimal("28500.00"), new BigDecimal("28500.00"),
                        TransactionStatus.SUCCESS, "Emergency savings deposit"),

                // Aditya Gupta
                new Transaction("TXN-10701", "CUR-INDB-001-8007", null, LocalDateTime.now().minusDays(35),
                        TransactionType.DEPOSIT, new BigDecimal("280000.00"), new BigDecimal("280000.00"),
                        TransactionStatus.SUCCESS, "IT export services payment"),
                new Transaction("TXN-10702", "CUR-INDB-001-8007", null, LocalDateTime.now().minusDays(12),
                        TransactionType.WITHDRAWAL, new BigDecimal("46000.00"), new BigDecimal("234000.00"),
                        TransactionStatus.SUCCESS, "Cloud server infrastructure hosting"),
                new Transaction("TXN-10703", "SAV-INDB-001-5007", null, LocalDateTime.now().minusDays(30),
                        TransactionType.DEPOSIT, new BigDecimal("412000.00"), new BigDecimal("412000.00"),
                        TransactionStatus.SUCCESS, "Equity shares dividend transfer"),

                // Ishita Nair
                new Transaction("TXN-10801", "CUR-INDB-001-8008", null, LocalDateTime.now().minusDays(28),
                        TransactionType.DEPOSIT, new BigDecimal("142000.00"), new BigDecimal("142000.00"),
                        TransactionStatus.SUCCESS, "E-commerce store weekly payout"),
                new Transaction("TXN-10802", "CUR-INDB-001-8008", null, LocalDateTime.now().minusDays(7),
                        TransactionType.WITHDRAWAL, new BigDecimal("45500.00"), new BigDecimal("96500.00"),
                        TransactionStatus.SUCCESS, "Logistics & courier partner settlement"),

                // Siddharth Mehta
                new Transaction("TXN-10901", "SAV-INDB-001-5008", null, LocalDateTime.now().minusDays(30),
                        TransactionType.DEPOSIT, new BigDecimal("84000.00"), new BigDecimal("84000.00"),
                        TransactionStatus.SUCCESS, "Graphic design studio project advance"),
                new Transaction("TXN-10902", "CUR-INDB-001-8009", null, LocalDateTime.now().minusDays(20),
                        TransactionType.DEPOSIT, new BigDecimal("41500.00"), new BigDecimal("41500.00"),
                        TransactionStatus.SUCCESS, "Branding retainer fee"),
                new Transaction("TXN-10903", "CUR-INDB-001-8009", null, LocalDateTime.now().minusDays(6),
                        TransactionType.WITHDRAWAL, new BigDecimal("10000.00"), new BigDecimal("31500.00"),
                        TransactionStatus.SUCCESS, "Workstation upgrade"),

                // Kavita Rao
                new Transaction("TXN-11001", "CUR-INDB-001-8010", null, LocalDateTime.now().minusDays(25),
                        TransactionType.DEPOSIT, new BigDecimal("320000.00"), new BigDecimal("320000.00"),
                        TransactionStatus.SUCCESS, "Legal arbitration retainer"),
                new Transaction("TXN-11002", "CUR-INDB-001-8010", null, LocalDateTime.now().minusDays(9),
                        TransactionType.WITHDRAWAL, new BigDecimal("59000.00"), new BigDecimal("261000.00"),
                        TransactionStatus.SUCCESS, "Office lease payment"),
                new Transaction("TXN-11003", "SAV-INDB-001-5009", null, LocalDateTime.now().minusDays(24),
                        TransactionType.DEPOSIT, new BigDecimal("378000.00"), new BigDecimal("378000.00"),
                        TransactionStatus.SUCCESS, "Annual partner dividend distribution"),

                // Rajesh Kulkarni (Overdrawn)
                new Transaction("TXN-11301", "CUR-INDB-001-8012", null, LocalDateTime.now().minusDays(20),
                        TransactionType.DEPOSIT, new BigDecimal("120000.00"), new BigDecimal("120000.00"),
                        TransactionStatus.SUCCESS, "Import container shipping advance"),
                new Transaction("TXN-11302", "CUR-INDB-001-8012", null, LocalDateTime.now().minusDays(10),
                        TransactionType.WITHDRAWAL, new BigDecimal("145000.00"), new BigDecimal("-25000.00"),
                        TransactionStatus.SUCCESS, "Customs duty payment (Overdraft used)"),
                new Transaction("TXN-11303", "CUR-INDB-001-8012", null, LocalDateTime.now().minusDays(2),
                        TransactionType.FEE_DEBIT, new BigDecimal("3000.00"), new BigDecimal("-28000.00"),
                        TransactionStatus.SUCCESS, "Overdraft facility fee"),
                new Transaction("TXN-11304", "SAV-INDB-001-5011", null, LocalDateTime.now().minusDays(22),
                        TransactionType.DEPOSIT, new BigDecimal("715000.00"), new BigDecimal("715000.00"),
                        TransactionStatus.SUCCESS, "Hedging reserve fixed deposit"),

                // Pooja Bhatia
                new Transaction("TXN-11601", "SAV-INDB-001-5013", null, LocalDateTime.now().minusDays(18),
                        TransactionType.DEPOSIT, new BigDecimal("650000.00"), new BigDecimal("650000.00"),
                        TransactionStatus.SUCCESS, "Software patent licensing royalty"),
                new Transaction("TXN-11602", "CUR-INDB-001-8014", null, LocalDateTime.now().minusDays(15),
                        TransactionType.DEPOSIT, new BigDecimal("250000.00"), new BigDecimal("250000.00"),
                        TransactionStatus.SUCCESS, "AI advisory retainer"),
                new Transaction("TXN-11603", "CUR-INDB-001-8014", null, LocalDateTime.now().minusDays(4),
                        TransactionType.WITHDRAWAL, new BigDecimal("26000.00"), new BigDecimal("224000.00"),
                        TransactionStatus.SUCCESS, "Cloud GPU compute invoice")
        );

        for (Transaction txn : seedTransactions) {
            transactionDAO.save(txn);
        }

        bank.reloadFromDatabase();
    }

    // addMoreDemoUsers: Adds randomly generated Indian demo customers and accounts
    public static int addMoreDemoUsers(Bank bank, int count) {
        String[] firstNames = {"Aarav", "Ananya", "Rohan", "Pooja", "Vikram", "Neha", "Aditya", "Ishita", "Siddharth", "Kavita", "Rahul", "Deepika"};
        String[] lastNames = {"Sharma", "Verma", "Patel", "Iyer", "Reddy", "Gupta", "Nair", "Mehta", "Singh", "Chopra", "Deshmukh", "Pillai"};
        String[] cities = {"Mumbai, MH", "Bengaluru, KA", "New Delhi, DL", "Hyderabad, TS", "Pune, MH", "Chennai, TN", "Kolkata, WB", "Ahmedabad, GJ", "Jaipur, RJ"};

        Random rand = new Random();
        int added = 0;

        for (int i = 0; i < count; i++) {
            String first = firstNames[rand.nextInt(firstNames.length)];
            String last = lastNames[rand.nextInt(lastNames.length)];
            String email = first.toLowerCase() + "." + last.toLowerCase() + (rand.nextInt(900) + 100) + "@seedmail.com";
            String phone = "+91-98" + String.format("%08d", rand.nextInt(100000000));
            String city = cities[rand.nextInt(cities.length)];

            try {
                Customer c = bank.registerCustomer(first, last, email, phone, city);
                BigDecimal savDeposit = BigDecimal.valueOf(5000 + rand.nextInt(200000)).setScale(2, RoundingMode.HALF_UP);
                bank.openSavingsAccount(c.getCustomerId(), savDeposit);

                if (rand.nextBoolean()) {
                    BigDecimal curDeposit = BigDecimal.valueOf(10000 + rand.nextInt(100000)).setScale(2, RoundingMode.HALF_UP);
                    bank.openCurrentAccount(c.getCustomerId(), curDeposit);
                }
                added++;
            } catch (Exception ignored) {
            }
        }
        bank.reloadFromDatabase();
        return added;
    }

    private static class CustomerData {
        final String id;
        final String first;
        final String last;
        final String email;
        final String phone;
        final String address;
        final int daysAgo;

        CustomerData(String id, String first, String last, String email, String phone, String address, int daysAgo) {
            this.id = id;
            this.first = first;
            this.last = last;
            this.email = email;
            this.phone = phone;
            this.address = address;
            this.daysAgo = daysAgo;
        }
    }
}
