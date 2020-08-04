package com.encrypted.db.dao;

import com.encrypted.db.entity.Account;
import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Account Data Access
 */
public class AccountDAO {

    public static final String SCHEMA = "acct";
    public static final String TABLE_ACCOUNT = "account";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CUSTOMER_ID = "customer_id";
    private static final String COLUMN_ACCOUNT_NUMBER = "account_number";
    private static final String COLUMN_BALANCE = "balance";

    private static final String ALL_COLUMNS = String.format("%s, %s, %s, %s", COLUMN_ID, COLUMN_CUSTOMER_ID,
            COLUMN_ACCOUNT_NUMBER, COLUMN_BALANCE);

    private SQLServerDataSource dataSource;

    public AccountDAO(SQLServerDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Insert Account
     *
     * @param account account to insert
     * @throws SQLException thrown if error executing SQL
     */
    public void insertAccount(Account account) throws SQLException {
        String sql = String.format("INSERT INTO %s.%s (%s, %s, %s) VALUES (?, ?, ?)",
                SCHEMA, TABLE_ACCOUNT, COLUMN_CUSTOMER_ID, COLUMN_ACCOUNT_NUMBER, COLUMN_BALANCE);

        try (SQLServerConnection con = (SQLServerConnection) dataSource.getConnection()) {

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, account.getCustomerId());
            ps.setNString(2, account.getAccountNumber());
            ps.setDouble(3, account.getBalance());

            ps.executeUpdate();
        }
    }

    /**
     * Get account by Id
     *
     * @param id id of account
     * @return account if found
     * @throws SQLException thrown if error executing SQL
     */
    public Account getAccountById(int id) throws SQLException {
        String sql = String.format("SELECT %s FROM %s.%s WHERE %s = ?",
                ALL_COLUMNS, SCHEMA, TABLE_ACCOUNT, COLUMN_ID);

        try (SQLServerConnection con = (SQLServerConnection) dataSource.getConnection()) {

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);

            ps.execute();

            Account account = null;
            try (ResultSet resultSet = ps.getResultSet()) {
                if (resultSet.next()) {
                    account = convertFromResultSet(resultSet);
                }
            }

            return account;
        }
    }

    /**
     * Get list of Accounts by Customer Id
     *
     * @param customerId customer id
     * @param encrypted true if fields should be encrypted
     * @return list of accounts for customer id
     * @throws SQLException thrown if error executing SQL
     */
    public List<Account> getAccountByCustomerId(int customerId, boolean encrypted) throws SQLException {
        String sql = String.format("SELECT %s FROM %s.%s WHERE %s = ?",
                ALL_COLUMNS, SCHEMA, TABLE_ACCOUNT, COLUMN_CUSTOMER_ID);;
        if (encrypted) {
            sql = String.format("SELECT %s, %s, CONVERT(NVARCHAR, %s, 2) as account_number, %s FROM %s.%s WHERE %s = ?",
                    COLUMN_ID, COLUMN_CUSTOMER_ID, COLUMN_ACCOUNT_NUMBER, COLUMN_BALANCE, SCHEMA,
                    TABLE_ACCOUNT, COLUMN_CUSTOMER_ID);
        }

        try (SQLServerConnection con = (SQLServerConnection) dataSource.getConnection()) {

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, customerId);

            ps.execute();

            List<Account> accounts = new ArrayList<>();
            try (ResultSet resultSet = ps.getResultSet()) {
                while (resultSet.next()) {
                    accounts.add(convertFromResultSet(resultSet));
                }
            }

            return accounts;
        }
    }

    /**
     * Update Account
     * @param account account to update
     * @throws SQLException thrown if error executing SQL
     */
    public void updateAccount(Account account) throws SQLException {
        String sql = String.format("UPDATE %s.%s SET %s = ?, %s = ?, %s = ? WHERE %s = ? RETURNING %s",
                SCHEMA, TABLE_ACCOUNT, COLUMN_CUSTOMER_ID, COLUMN_ACCOUNT_NUMBER, COLUMN_BALANCE, COLUMN_ID,
                ALL_COLUMNS);

        try (SQLServerConnection con = (SQLServerConnection) dataSource.getConnection()) {

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, account.getCustomerId());
            ps.setNString(2, account.getAccountNumber());
            ps.setDouble(3, account.getBalance());
            ps.setInt(4, account.getId());

            ps.executeUpdate();
        }
    }

    /**
     * Convert ResultSet to Account
     *
     * @param resultSet result set
     * @return Account
     * @throws SQLException thrown if error converting result set
     */
    private Account convertFromResultSet(ResultSet resultSet) throws SQLException {
        return Account.builder()
                .id(resultSet.getInt(COLUMN_ID))
                .accountNumber(resultSet.getString(COLUMN_ACCOUNT_NUMBER))
                .balance(resultSet.getDouble(COLUMN_BALANCE))
                .customerId(resultSet.getInt(COLUMN_CUSTOMER_ID))
                .build();
    }

}
