package com.encrypted.db;

import com.encrypted.db.dao.AccountDAO;
import com.microsoft.sqlserver.jdbc.SQLServerColumnEncryptionJavaKeyStoreProvider;
import com.microsoft.sqlserver.jdbc.SQLServerColumnEncryptionKeyStoreProvider;
import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MSSQLServerContainer;

public class BaseTest {

    private static final MSSQLServerContainer MS_SQL_SERVER =
            new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2019-latest");

    private static final String CREATE_SCHEMA_SQL = "CREATE SCHEMA %s";
    private static final String DROP_SCHEMA_SQL = "DROP SCHEMA %s";

    private static final String KEY_ALIAS = "AlwaysEncryptedKey";
    private static final String COLUMN_MASTER_KEY_NAME = "AlwaysEncryptedCMK";
    protected static final String COLUMN_ENCRYPTION_KEY_NAME = "AlwaysEncryptedCEK";
    private static final String KEYSTORE_LOCATION = "<KEYSTORE_LOCATION>/KeyStore.jks";
    private static final char[] KEYSTORE_SECRET = "changeit".toCharArray();
    private static final String ALGORITHM = "RSA_OAEP";

    private static final String CREATE_CMK = "CREATE COLUMN MASTER KEY [%s] WITH " +
            "(KEY_STORE_PROVIDER_NAME = N'MSSQL_JAVA_KEYSTORE', KEY_PATH = N'%s')";
    private static final String CREATE_CEK = "CREATE COLUMN ENCRYPTION KEY %s WITH VALUES " +
            "(COLUMN_MASTER_KEY = %s, ALGORITHM =  '%s', ENCRYPTED_VALUE =  0x%s)";

    @BeforeAll
    public static void init() throws Exception {
        MS_SQL_SERVER.start();

        System.out.println("User: " + MS_SQL_SERVER.getUsername());
        System.out.println("PW: " + MS_SQL_SERVER.getPassword());
        System.out.println("JDBC: " + MS_SQL_SERVER.getJdbcUrl());

        SQLServerConnection msConn = getMSSQLConnection();

        msConn.prepareCall(String.format(CREATE_SCHEMA_SQL, AccountDAO.SCHEMA)).execute();
        msConn.prepareCall(String.format(CREATE_CMK, COLUMN_MASTER_KEY_NAME, KEY_ALIAS)).execute();

        SQLServerColumnEncryptionKeyStoreProvider storeProvider =
                new SQLServerColumnEncryptionJavaKeyStoreProvider(KEYSTORE_LOCATION, KEYSTORE_SECRET);

        byte[] encryptedCEK = getEncryptedCEK(storeProvider);

        msConn.prepareCall(String.format(CREATE_CEK, COLUMN_ENCRYPTION_KEY_NAME, COLUMN_MASTER_KEY_NAME,
                ALGORITHM, byteArrayToHex(encryptedCEK))).execute();

    }

    protected static SQLServerConnection getMSSQLConnection() throws Exception {
        return (SQLServerConnection) msSqlServerAlwaysEncryptedEnabledTestDatasource().getConnection();
    }

    protected static SQLServerDataSource msSqlServerAlwaysEncryptedEnabledTestDatasource() {
        SQLServerDataSource sqlServerDataSource = new SQLServerDataSource();
        sqlServerDataSource.setURL(MS_SQL_SERVER.getJdbcUrl());
        sqlServerDataSource.setPassword(MS_SQL_SERVER.getPassword());
        sqlServerDataSource.setUser(MS_SQL_SERVER.getUsername());
        sqlServerDataSource.setColumnEncryptionSetting("Enabled");
        sqlServerDataSource.setKeyStoreLocation(KEYSTORE_LOCATION);
        sqlServerDataSource.setKeyStoreSecret("changeit");
        sqlServerDataSource.setKeyStoreAuthentication("JavaKeyStorePassword");

        return sqlServerDataSource;
    }

    protected static SQLServerDataSource msSqlServerAlwaysEncryptedDisabledTestDatasource() {
        SQLServerDataSource sqlServerDataSource = new SQLServerDataSource();
        sqlServerDataSource.setURL(MS_SQL_SERVER.getJdbcUrl());
        sqlServerDataSource.setPassword(MS_SQL_SERVER.getPassword());
        sqlServerDataSource.setUser(MS_SQL_SERVER.getUsername());

        return sqlServerDataSource;
    }

    private static byte[] getEncryptedCEK(SQLServerColumnEncryptionKeyStoreProvider storeProvider) throws SQLServerException {
        String plainTextKey = "You need to give your plain text";

        // plainTextKey has to be 32 bytes with current algorithm supported
        byte[] plainCEK = plainTextKey.getBytes();

        // This will give us encrypted column encryption key in bytes
        return storeProvider.encryptColumnEncryptionKey(KEY_ALIAS, ALGORITHM, plainCEK);
    }

    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b).toUpperCase());
        return sb.toString();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        getMSSQLConnection().prepareCall(String.format(DROP_SCHEMA_SQL, AccountDAO.SCHEMA)).execute();
        MS_SQL_SERVER.stop();
    }
}
