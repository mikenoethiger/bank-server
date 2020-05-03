package bank.protocol;

import bank.Account;
import bank.InactiveException;
import bank.OverdrawException;

import java.io.IOException;

/**
 * Default account implementation.
 *
 * You may want to override {@link DefaultAccount#deposit(double)} and {@link DefaultAccount#withdraw(double)}.
 */
public class DefaultAccount implements Account {

    private String number;
    private String owner;
    private double balance;
    private boolean active = true;

    public DefaultAccount() {}

    @Override
    public double getBalance() {
        return balance;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getNumber() {
        return number;
    }

    @Override
    public boolean isActive() {
        return active;
    }


    @Override
    public void deposit(double amount) throws IOException, InactiveException {
        if (!isActive()) throw new InactiveException();
        if (amount < 0) throw new IllegalArgumentException("negative amount not allowed");
        balance += amount;
    }

    @Override
    public void withdraw(double amount) throws IOException, InactiveException, OverdrawException {
        if (amount < 0) throw new IllegalArgumentException("negative amount not allowed");
        if (amount > balance) throw new OverdrawException();
        if (!isActive()) throw new InactiveException();
        balance -= amount;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        /* common structure in responses, e.g. https://github.com/mikenoethiger/bank-server-socket#get-account-2 */
        return String.format("%s\n%s\n%s\n%s", number, owner, balance, active);
    }
}
