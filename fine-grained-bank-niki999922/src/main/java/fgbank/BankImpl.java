package fgbank;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BankImpl implements Bank {
    private final Account[] accounts;

    public BankImpl(int n) {
        accounts = new Account[n];
        for (int i = 0; i < n; i++) {
            accounts[i] = new Account();
        }
    }

    @Override
    public int getNumberOfAccounts() {
        return accounts.length;
    }

    @Override
    public long getAmount(int index) {
        return accounts[index].getAmount();
    }

    @Override
    public long getTotalAmount() {
        long sum = 0;
        for (Account account : accounts) {
            account.lockAccount();
        }
        for (Account account : accounts) {
            sum += account.amount;
        }
        for (Account account : accounts) {
            account.unlockAccount();
        }
        return sum;
    }

    @Override
    public long deposit(int index, long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        Account account = accounts[index];
        account.lockAccount();
        long accountAmount = account.amount;
        if (amount > MAX_AMOUNT || accountAmount + amount > MAX_AMOUNT) {
            account.unlockAccount();
            throw new IllegalStateException("Overflow");
        }
        accountAmount += amount;
        account.amount = accountAmount;
        account.unlockAccount();
        return accountAmount;
    }

    @Override
    public long withdraw(int index, long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        Account account = accounts[index];
        account.lockAccount();
        long accountAmount = account.amount;
        if (accountAmount - amount < 0) {
            account.unlockAccount();
            throw new IllegalStateException("Underflow");
        }
        accountAmount -= amount;
        account.amount = accountAmount;
        account.unlockAccount();
        return accountAmount;
    }

    @Override
    public void transfer(int fromIndex, int toIndex, long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        if (fromIndex == toIndex)
            throw new IllegalArgumentException("fromIndex == toIndex");
        Account to, from;
        if (fromIndex < toIndex) {
            from = accounts[fromIndex];
            from.lockAccount();
            to = accounts[toIndex];
            to.lockAccount();
        } else {
            to = accounts[toIndex];
            to.lockAccount();
            from = accounts[fromIndex];
            from.lockAccount();
        }
        if (amount > from.amount) {
            from.unlockAccount();
            to.unlockAccount();
            throw new IllegalStateException("Underflow");
        } else if (amount > MAX_AMOUNT || to.amount + amount > MAX_AMOUNT) {
            from.unlockAccount();
            to.unlockAccount();
            throw new IllegalStateException("Overflow");
        }
        from.amount -= amount;
        from.unlockAccount();
        to.amount += amount;
        to.unlockAccount();
    }

    private class Account {
        long amount = 0;
        private Lock lock = new ReentrantLock();

        long getAmount() {
            try {
                lock.lock();
                return amount;
            } finally {
                lock.unlock();
            }
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }

        void lockAccount() {
            lock.lock();
        }

        void unlockAccount() {
            lock.unlock();
        }
    }
}