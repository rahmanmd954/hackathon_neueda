// banking.test.js
import { quickDeposit, quickWithdraw, processTransaction } from './banking'; // Extract your logic to a separate file for testability

describe('Banking operations', () => {
  let balances;
  let currentAccount;

  beforeEach(() => {
    balances = { Checking: 2500.00, Savings: 1000.00 };
    currentAccount = 'Checking';
  });

  test('quickDeposit should increase balance by 100', () => {
    quickDeposit(balances, currentAccount);
    expect(balances.Checking).toBe(2600.00);
  });

  test('quickWithdraw should decrease balance by 50 if sufficient', () => {
    quickWithdraw(balances, currentAccount);
    expect(balances.Checking).toBe(2450.00);
  });

  test('quickWithdraw should not allow withdrawal if insufficient', () => {
    currentAccount = 'Savings';
    balances.Savings = 20;
    const result = quickWithdraw(balances, currentAccount);
    expect(result).toBe('Insufficient funds');
    expect(balances.Savings).toBe(20);
  });

  test('processTransaction should handle deposits', () => {
    const result = processTransaction(balances, currentAccount, 'Deposit', 300, 'Paycheck');
    expect(balances.Checking).toBe(2800.00);
    expect(result).toBe('Transaction processed successfully!');
  });

  test('processTransaction should reject withdrawals with low balance', () => {
    const result = processTransaction(balances, currentAccount, 'Withdraw', 999999, 'Overdraft');
    expect(result).toBe('Insufficient funds');
  });
});
