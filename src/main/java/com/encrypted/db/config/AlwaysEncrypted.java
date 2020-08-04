package com.encrypted.db.config;

import com.microsoft.sqlserver.jdbc.SQLServerColumnEncryptionJavaKeyStoreProvider;
import com.microsoft.sqlserver.jdbc.SQLServerColumnEncryptionKeyStoreProvider;
import com.microsoft.sqlserver.jdbc.SQLServerException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This program demonstrates how to create a column encryption key programmatically for the Java Key Store.
 */
public class AlwaysEncrypted {
    // Alias of the key stored in the keystore.
    private static final String KEY_ALIAS = "AlwaysEncryptedKey";

    // Name by which the column master key will be known in the database.
    private static final String COLUMN_MASTER_KEY_NAME = "AlwaysEncryptedCMK";

    // Name by which the column encryption key will be known in the database.
    private static final String COLUMN_ENCRYPTION_KEY = "AlwaysEnryptedCEK";

    // The location of the keystore.
    private static final String KEY_STORE_LOCATION = "<PATH_TO_KEY_STORE>/KeyStore.jks";

    // The password of the keystore and the key.
    private static final char[] KEY_STORE_SECRET = "changeit".toCharArray();

    // The username for the database connection string
    private static final String DB_USER = "<DB_USER>";

    // The password for the database connection string
    private static final String DB_PASSWORD = "<DB_PASSWORD>";

    // The server url / ip for the database connection string
    private static final String DB_SERVER = "<DB_SERVER>";

    // The port for the database connection string
    private static final String DB_PORT = "<DB_PORT>";

    /*
     * Name of the encryption algorithm used to encrypt the value of the column encryption key.
     * The algorithm for the system providers must be RSA_OAEP.
     */
    private static final String ALGORITHM = "RSA_OAEP";

    public static void main(String[] args) {
        String connectionUrl = String.format("jdbc:sqlserver://%s:%s;user=%s;password=%s;columnEncryptionSetting=Enabled;",
                DB_SERVER, DB_PORT, DB_USER, DB_PASSWORD);

        try (Connection connection = DriverManager.getConnection(connectionUrl);
             Statement statement = connection.createStatement();) {

            // Instantiate the Java Key Store provider.
            SQLServerColumnEncryptionKeyStoreProvider storeProvider =
                    new SQLServerColumnEncryptionJavaKeyStoreProvider(KEY_STORE_LOCATION, KEY_STORE_SECRET);

            byte[] encryptedCEK = getEncryptedCEK(storeProvider);

            /*
             * Create column encryption key For more details on the syntax, see:
             * https://docs.microsoft.com/sql/t-sql/statements/create-column-encryption-key-transact-sql
             * Encrypted column encryption key first needs to be converted into varbinary_literal from bytes,
             * for which byteArrayToHex() is used.
             */
            String createCEKSQL = "CREATE COLUMN ENCRYPTION KEY "
                    + COLUMN_ENCRYPTION_KEY
                    + " WITH VALUES ( "
                    + " COLUMN_MASTER_KEY = "
                    + COLUMN_MASTER_KEY_NAME
                    + " , ALGORITHM =  '"
                    + ALGORITHM
                    + "' , ENCRYPTED_VALUE =  0x"
                    + byteArrayToHex(encryptedCEK)
                    + " ) ";
            statement.executeUpdate(createCEKSQL);
            System.out.println("Column encryption key created with name : " + COLUMN_ENCRYPTION_KEY);
        }
        // Handle any errors that may have occurred.
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static byte[] getEncryptedCEK(SQLServerColumnEncryptionKeyStoreProvider storeProvider)
            throws SQLServerException {

        String plainTextKey = "You need to give your plain text";

        // plainTextKey has to be 32 bytes with current algorithm supported
        byte[] plainCEK = plainTextKey.getBytes();

        // This will give us encrypted column encryption key in bytes
        byte[] encryptedCEK = storeProvider.encryptColumnEncryptionKey(KEY_ALIAS, ALGORITHM, plainCEK);

        return encryptedCEK;
    }

    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b).toUpperCase());
        return sb.toString();
    }
}
