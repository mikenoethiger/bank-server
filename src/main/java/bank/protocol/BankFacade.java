package bank.protocol;

import bank.Account;
import bank.Bank;
import bank.InactiveException;
import bank.OverdrawException;

import java.io.IOException;
import java.util.Set;

/**
 * The BankFacade abstracts the very common pattern of validating a {@link Request},
 * translate it to a {@link Bank} call and return the results as a {@link Response}.
 *
 * Basically it's the implementation of these actions: https://github.com/mikenoethiger/bank-server-socket#actions
 */
public class BankFacade {

    private final Bank bank;

    public BankFacade(Bank bank) {
        this.bank = bank;
    }

    /* https://github.com/mikenoethiger/bank-server-socket#get-account-numbers-1 */
    public Response getAccountNumbers(Request request) throws IOException {
        Set<String> numbers = bank.getAccountNumbers();
        String[] data = numbers.toArray(new String[numbers.size()]);
        return new Response(Response.STATUS_OK, data);
    }

    /* https://github.com/mikenoethiger/bank-server-socket#get-account-2 */
    public Response getAccount(Request request) throws IOException {
        if (request.getArgs().length < 1) return Response.ERROR_BAD_REQUEST;

        Account acc = bank.getAccount(request.getArgs()[0]);

        if (acc == null) return Response.ERROR_ACCOUNT_DOES_NOT_EXIST;
        return new Response(Response.STATUS_OK, new String[]{acc.getNumber(), acc.getOwner(), String.valueOf(acc.getBalance()), acc.isActive() ? "1" : "0"});
    }

    /* https://github.com/mikenoethiger/bank-server-socket/blob/master/readme.md#create-account-3 */
    public Response createAccount(Request request) throws IOException {
        String number = bank.createAccount(request.getArgs()[0]);
        if (number == null) return Response.ERROR_ACCOUNT_COULD_NOT_BE_CREATED;
        Account acc = bank.getAccount(number);
        return new Response(Response.STATUS_OK, new String[]{acc.getNumber(), acc.getOwner(), String.valueOf(acc.getBalance()), acc.isActive() ? "1" : "0"});
    }

    /* https://github.com/mikenoethiger/bank-server-socket/blob/master/readme.md#close-account-4 */
    public Response closeAccount(Request request) throws IOException {
        if (request.getArgs().length < 1) return Response.ERROR_BAD_REQUEST;

        boolean success = bank.closeAccount(request.getArgs()[0]);

        if (!success) return Response.ERROR_ACCOUNT_COULD_NOT_BE_CLOSED;
        return new Response(Response.STATUS_OK, new String[]{});
    }

    /* https://github.com/mikenoethiger/bank-server-socket/blob/master/readme.md#transfer-5 */
    public Response transfer(Request request) throws IOException {
        if (request.getArgs().length < 3) return Response.ERROR_BAD_REQUEST;

        Account from = bank.getAccount(request.getArgs()[0]);
        Account to = bank.getAccount(request.getArgs()[1]);
        double amount;
        if (from == null || to == null) return Response.ERROR_ACCOUNT_DOES_NOT_EXIST;
        try {
            amount = Double.parseDouble(request.getArgs()[2]);
        } catch (NumberFormatException e) {
            return Response.ERROR_BAD_REQUEST;
        }

        try {
            bank.transfer(from, to, amount);
        } catch (OverdrawException e) {
            return Response.ERROR_ACCOUNT_OVERDRAW;
        } catch (InactiveException e) {
            return Response.ERROR_INACTIVE_ACCOUNT;
        } catch (IllegalArgumentException e) {
            return Response.ERROR_ILLEGAL_ARGUMENT;
        }

        return new Response(Response.STATUS_OK, new String[]{String.valueOf(from.getBalance()), String.valueOf(to.getBalance())});
    }

    /* Generic transaction handler (deposit and withdraw) */
    /* https://github.com/mikenoethiger/bank-server-socket/blob/master/readme.md#deposit-6 */
    /* https://github.com/mikenoethiger/bank-server-socket/blob/master/readme.md#withdraw-7 */
    public Response transaction(Request request) throws IOException {
        if (request.getArgs().length < 2) return Response.ERROR_BAD_REQUEST;

        String number = request.getArgs()[0];
        double amount;
        try {
            amount = Double.parseDouble(request.getArgs()[1]);
        } catch (NumberFormatException e) {
            return Response.ERROR_BAD_REQUEST;
        }
        Account acc = bank.getAccount(number);
        if (acc == null) return Response.ERROR_ACCOUNT_DOES_NOT_EXIST;

        try {
            /* actionId 6: deposit, actionId 7: withdraw */
            if (request.getActionId() == 6) acc.deposit(amount);
            else acc.withdraw(amount);
        } catch (OverdrawException e) {
            return Response.ERROR_ACCOUNT_OVERDRAW;
        } catch (InactiveException e) {
            return Response.ERROR_INACTIVE_ACCOUNT;
        } catch (IllegalArgumentException e) {
            return Response.ERROR_ILLEGAL_ARGUMENT;
        }

        return new Response(Response.STATUS_OK, new String[]{String.valueOf(acc.getBalance())});
    }
}
