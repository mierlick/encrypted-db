package com.encrypted.db.dao;

import com.encrypted.db.entity.Customer;
import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Customer Data Access
 */
public class CustomerDAO {

    public static final String SCHEMA = "acct";
    public static final String TABLE_CUSTOMER = "customer";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SSN = "ssn";
    private static final String COLUMN_FIRST_NAME = "first_name";
    private static final String COLUMN_MIDDLE_INITIAL = "middle_initial";
    private static final String COLUMN_LAST_NAME = "last_name";

    private static final String ALL_COLUMNS = String.format("%s, %s, %s, %s, %s", COLUMN_ID, COLUMN_SSN,
            COLUMN_FIRST_NAME, COLUMN_MIDDLE_INITIAL, COLUMN_LAST_NAME);

    private SQLServerDataSource dataSource;
    private AccountDAO accountDAO;

    public CustomerDAO(SQLServerDataSource dataSource) {
        this.dataSource = dataSource;
        accountDAO = new AccountDAO(dataSource);
    }

    /**
     * Insert Customer
     *
     * @param customer customer to insert
     * @throws SQLException thrown if error executing SQL
     */
    public void insertCustomer(Customer customer) throws SQLException {
        String sql = String.format("INSERT INTO %s.%s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                SCHEMA, TABLE_CUSTOMER, COLUMN_SSN, COLUMN_FIRST_NAME, COLUMN_MIDDLE_INITIAL,
                COLUMN_LAST_NAME);

        try (SQLServerConnection con = (SQLServerConnection) dataSource.getConnection()) {

            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setNString(1, (String) customer.getSsn());
            ps.setNString(2, customer.getFirstName());
            ps.setString(3, customer.getMiddleInitial());
            ps.setNString(4, customer.getLastName());

            ps.executeUpdate();

            try (ResultSet resultSet = ps.getGeneratedKeys()) {
                if (resultSet.next()) {
                    int customerId = resultSet.getInt(1);

                    if (customer.getAccounts() != null) {
                        customer.getAccounts().forEach(account -> {
                            try {
                                account.setCustomerId(customerId);
                                accountDAO.insertAccount(account);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }


        }
    }

    /**
     * Get Customer by Id
     *
     * @param id id of customer
     * @param encrypted true if fields should be encrypted
     * @return customer if found
     * @throws SQLException thrown if error executing SQL
     */
    public Customer getCustomerById(int id, boolean encrypted) throws SQLException {
        String sql = String.format("SELECT %s FROM %s.%s WHERE %s = ?", ALL_COLUMNS, SCHEMA, TABLE_CUSTOMER, COLUMN_ID);
        if (encrypted) {
            sql = String.format("SELECT %s, CONVERT(NVARCHAR, %s, 2) as ssn, %s, %s, %s FROM %s.%s WHERE %s = ?",
                    COLUMN_ID, COLUMN_SSN, COLUMN_FIRST_NAME, COLUMN_MIDDLE_INITIAL, COLUMN_LAST_NAME, SCHEMA,
                    TABLE_CUSTOMER, COLUMN_ID);
        }

        try (SQLServerConnection con = (SQLServerConnection) dataSource.getConnection()) {

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);

            ps.execute();

            Customer customers = null;
            try (ResultSet resultSet = ps.getResultSet()) {
                if (resultSet.next()) {
                    customers = convertFromResultSet(resultSet, encrypted);
                }
            }

            return customers;
        }
    }

    /**
     * Update Customer
     *
     * @param customer customer to update
     * @throws SQLException thrown if error executing SQL
     */
    public void updateCustomer(Customer customer) throws SQLException {
        String sql = String.format("UPDATE %s.%s SET %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ? RETURNING %s",
                SCHEMA, TABLE_CUSTOMER, COLUMN_SSN, COLUMN_FIRST_NAME, COLUMN_MIDDLE_INITIAL, COLUMN_LAST_NAME,
                COLUMN_ID, ALL_COLUMNS);

        try (SQLServerConnection con = (SQLServerConnection) dataSource.getConnection()) {

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setObject(1, customer.getSsn());
            ps.setNString(2, customer.getFirstName());
            ps.setNString(3, customer.getMiddleInitial());
            ps.setNString(3, customer.getLastName());
            ps.setInt(5, customer.getId());

            ps.executeUpdate();
        }
    }

    /**
     * Convert ResultSet to Customer
     *
     * @param resultSet result set
     * @param encrypted true if fields should be encrypted
     * @return Customer
     * @throws SQLException thrown if error converting result set
     */
    private Customer convertFromResultSet(ResultSet resultSet, boolean encrypted) throws SQLException {
        int customerId = resultSet.getInt(COLUMN_ID);

        return Customer.builder()
                .id(customerId)
                .ssn(resultSet.getString(COLUMN_SSN))
                .firstName(resultSet.getNString(COLUMN_FIRST_NAME))
                .middleInitial(resultSet.getString(COLUMN_MIDDLE_INITIAL))
                .lastName(resultSet.getNString(COLUMN_LAST_NAME))
                .accounts(accountDAO.getAccountByCustomerId(customerId, encrypted))
                .build();
    }
}
