package com.encrypted.db;

import com.encrypted.db.dao.AccountDAO;
import com.encrypted.db.dao.CustomerDAO;
import com.encrypted.db.entity.Account;
import com.encrypted.db.entity.Customer;
import com.microsoft.sqlserver.jdbc.SQLServerConnection;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountTest extends BaseTest {

    private static final Logger LOG = Logger.getLogger(AccountTest.class.getName());

    private static final String CREATE_TABLE_ACCOUNT = "CREATE TABLE [%s].[%s] (" +
            "[id] [int] IDENTITY NOT NULL, " +
            "[customer_id] [int] NOT NULL, " +
            "[account_number] [nvarchar](30) ENCRYPTED WITH (COLUMN_ENCRYPTION_KEY = [%s], " +
            "   ENCRYPTION_TYPE = Randomized, ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256') NOT NULL, " +
            "[balance] float)";

    private static final String CREATE_TABLE_CUSTOMER = "CREATE TABLE [%s].[%s] (" +
            "[id] [int] IDENTITY NOT NULL, " +
            "[ssn] [nvarchar](9) ENCRYPTED WITH (COLUMN_ENCRYPTION_KEY = [%s], " +
            "   ENCRYPTION_TYPE = Randomized, ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256') NOT NULL, " +
            "[first_name] [nvarchar](50) NOT NULL, " +
            "[middle_initial] [char], " +
            "[last_name] [nvarchar](50) NOT NULL)";

    private static final String DROP_TABLE = "DROP TABLE %s.%s";

    private CustomerDAO encryptedCustomerDAO;
    private CustomerDAO unencryptedCustomerDAO;

    @BeforeAll
    public static void init() throws Exception {
        BaseTest.init();

        SQLServerConnection msConn = getMSSQLConnection();

        msConn.prepareCall(String.format(CREATE_TABLE_ACCOUNT, AccountDAO.SCHEMA, AccountDAO.TABLE_ACCOUNT,
                COLUMN_ENCRYPTION_KEY_NAME)).execute();
        msConn.prepareCall(String.format(CREATE_TABLE_CUSTOMER, CustomerDAO.SCHEMA, CustomerDAO.TABLE_CUSTOMER,
                COLUMN_ENCRYPTION_KEY_NAME)).execute();

    }

    @BeforeEach
    public void setUp() {
        encryptedCustomerDAO = new CustomerDAO(msSqlServerAlwaysEncryptedEnabledTestDatasource());
        unencryptedCustomerDAO = new CustomerDAO(msSqlServerAlwaysEncryptedDisabledTestDatasource());
    }


    @Test
    void shouldRetrieveAccountRecords() throws SQLException {
        insertRecords();

        LOG.info("*************Retrieved Customer with Encryption Enabled*************");
        LOG.info(encryptedCustomerDAO.getCustomerById(1, false).toString());
        LOG.info("********************************************************************");

        LOG.info("*************Retrieved Customer with Encryption Disabled*************");
        LOG.info(unencryptedCustomerDAO.getCustomerById(1, true).toString());
        LOG.info("*********************************************************************");

    }

    private void insertRecords() throws SQLException {
        Account account1 = Account.builder()
                .accountNumber("12345ABCDE")
                .balance(12954.32)
                .build();

        Account account2 = Account.builder()
                .accountNumber("1234567890")
                .balance(965476.43)
                .build();

        Account account3 = Account.builder()
                .accountNumber("ABCDEFG")
                .balance(65.98)
                .build();

        Customer customer1 = Customer.builder()
                .firstName("Jane")
                .middleInitial("A")
                .lastName("Doe")
                .ssn("123456789")
                .accounts(Arrays.asList(account1, account2))
                .build();


        Customer customer2 = Customer.builder()
                .firstName("John")
                .middleInitial("B")
                .lastName("Smith")
                .ssn("987654321")
                .accounts(Collections.singletonList(account3))
                .build();

        encryptedCustomerDAO.insertCustomer(customer1);
        encryptedCustomerDAO.insertCustomer(customer2);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        SQLServerConnection msConn = getMSSQLConnection();

        msConn.prepareCall(String.format(DROP_TABLE, AccountDAO.SCHEMA, AccountDAO.TABLE_ACCOUNT)).execute();
        msConn.prepareCall(String.format(DROP_TABLE, CustomerDAO.SCHEMA, CustomerDAO.TABLE_CUSTOMER)).execute();
        BaseTest.tearDown();
    }

}
