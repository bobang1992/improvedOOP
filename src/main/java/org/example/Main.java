package org.example;

import java.util.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Predicate;

interface TransactionManager {
    void saveTransactions(String filename, List<Transaction> transactions);
    List<Transaction> loadTransactions(String filename);
}

class Transaction implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final int amount;
    private final LocalDate date;

    public Transaction(int amount, LocalDate date) {
        this.amount = amount;
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public void showInfo() {
        System.out.println("Amount: " + amount + " Date: " + date);
    }
}

class FileTransactionManager implements TransactionManager {
    @Override
    public void saveTransactions(String filename, List<Transaction> transactions) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(transactions);
            System.out.println("Transactions saved to file.");
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    @Override
    public List<Transaction> loadTransactions(String filename) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            return (List<Transaction>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}

class InputHandler {
    private final Scanner scanner = new Scanner(System.in);

    public int getInt(String prompt, Predicate<Integer> validator) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Enter an integer.");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        return validator.test(value) ? value : getInt(prompt, validator);
    }

    public String getFilename() {
        System.out.print("Enter filename: ");
        return scanner.nextLine().trim();
    }

    public LocalDate getDate(String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        while (true) {
            try {
                System.out.print("Enter date (" + format + "): ");
                return LocalDate.parse(scanner.nextLine().trim(), formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Try again.");
            }
        }
    }

    public char getChoice() {
        System.out.print("Choose action: ");
        return scanner.nextLine().trim().charAt(0);
    }
}

abstract class BankAccount {
    private int balance;
    protected List<Transaction> transactions = new ArrayList<>();
    private final TransactionManager transactionManager;

    public BankAccount(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public int getBalance() { return balance; }

    public void deposit(int amount) {
        if (amount > 0) {
            balance += amount;
            addTransaction(amount);
        }
    }

    public void withdraw(int amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            addTransaction(-amount);
        } else {
            System.out.println("Insufficient funds.");
        }
    }

    private void addTransaction(int amount) {
        transactions.add(new Transaction(amount, LocalDate.now()));
        System.out.println((amount > 0 ? "Deposited: " : "Withdrawn: ") + Math.abs(amount));
    }

    public void showTransactions(Predicate<Transaction> filter, String message) {
        System.out.println(message);
        transactions.stream().filter(filter).forEach(Transaction::showInfo);
    }

    public void saveTransactions(String filename) {
        transactionManager.saveTransactions(filename, transactions);
    }

    public void loadTransactions(String filename) {
        transactions = transactionManager.loadTransactions(filename);
    }

    public void deleteTransactions(Predicate<Transaction> filter, String message) {
        transactions.removeIf(filter);
        System.out.println(message);
    }

    public abstract void showMenu();
}

class SimpleBankAccount extends BankAccount {
    private final InputHandler inputHandler;

    public SimpleBankAccount(InputHandler inputHandler, TransactionManager transactionManager) {
        super(transactionManager);
        this.inputHandler = inputHandler;
    }

    @Override
    public void showMenu() {
        char choice;
        do {
            System.out.println("\n1: Check balance\n2: Deposit\n3: Withdraw\n4: Show all transactions\n5: Show by day\n6: Show by month\n7: Show by year\n8: Save\n9: Load\nA: Delete by day\nB: Delete all\n0: Exit");
            choice = inputHandler.getChoice();
            switch (choice) {
                case '1': System.out.println("Balance: " + getBalance()); break;
                case '2': deposit(inputHandler.getInt("Enter amount: ", amt -> amt > 0)); break;
                case '3': withdraw(inputHandler.getInt("Enter amount: ", amt -> amt > 0)); break;
                case '4': showTransactions(t -> true, "All Transactions:"); break;
                case '5': showTransactions(t -> t.getDate().equals(inputHandler.getDate("yyyy-MM-dd")), "Transactions on given day:"); break;
                case '6': showTransactions(t -> {
                    LocalDate date = inputHandler.getDate("yyyy-MM");
                    return t.getDate().getYear() == date.getYear() && t.getDate().getMonthValue() == date.getMonthValue();
                }, "Transactions for given month:");
                    break;
                case '7': showTransactions(t -> t.getDate().getYear() == inputHandler.getInt("Enter year: ", y -> y > 0), "Transactions for given year:"); break;
                case '8': saveTransactions(inputHandler.getFilename()); break;
                case '9': loadTransactions(inputHandler.getFilename()); break;
                case 'A': deleteTransactions(t -> t.getDate().equals(inputHandler.getDate("yyyy-MM-dd")), "Transactions deleted for day."); break;
                case 'B': deleteTransactions(t -> true, "All transactions deleted."); break;
                case '0': System.out.println("Exiting..."); break;
                default: System.out.println("Invalid choice.");
            }
        } while (choice != '0');
    }
}

public class Main {
    public static void main(String[] args) {
        InputHandler inputHandler = new InputHandler();
        TransactionManager fileTransactionManager = new FileTransactionManager();
        SimpleBankAccount account = new SimpleBankAccount(inputHandler, fileTransactionManager);
        account.showMenu();
    }
}
