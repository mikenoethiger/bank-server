package bank.protocol;

import bank.Account;
import bank.Bank;
import bank.InactiveException;
import bank.OverdrawException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In memory {@link Bank implementation} (i.e. {@link Account}'s are stored in a {@link HashMap}.)
 */
public class InMemoryBank implements Bank {

    private static final String IBAN_PREFIX = "CH56";
    private static long nextAccountNumber = 1000_0000_0000_0000_0L;

    private final Map<String, DefaultAccount> accounts = new HashMap<>();

    @Override
    public Set<String> getAccountNumbers() {
        return accounts.values().stream().filter(DefaultAccount::isActive).map(DefaultAccount::getNumber).collect(Collectors.toSet());
    }

    @Override
    synchronized public String createAccount(String owner) throws IOException {
        DefaultAccount acc = new DefaultAccount();
        acc.setOwner(owner);
        acc.setBalance(0);
        acc.setActive(true);
        acc.setNumber(IBAN_PREFIX + nextAccountNumber++);
        accounts.put(acc.getNumber(), acc);
        return acc.getNumber();
    }

    @Override
    public boolean closeAccount(String number) throws IOException {
        if (!accounts.containsKey(number)) return false;
        DefaultAccount a = accounts.get(number);
        if (!a.isActive()) return false;
        if (a.getBalance() > 0) return false;
        a.setActive(false);
        return true;
    }

    @Override
    public bank.Account getAccount(String number) {
        return accounts.get(number);
    }

    @Override
    public void transfer(bank.Account from, bank.Account to, double amount)
            throws IOException, InactiveException, OverdrawException {
        if (amount < 0) throw new IllegalArgumentException("negative amount not allowed");
        if (!from.isActive() || !to.isActive()) throw new InactiveException();
        if (from.getBalance() < amount) throw new OverdrawException();
        from.withdraw(amount);
        to.deposit(amount);
    }

}
