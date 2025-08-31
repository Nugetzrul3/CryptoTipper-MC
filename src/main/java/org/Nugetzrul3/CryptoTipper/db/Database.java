package org.Nugetzrul3.CryptoTipper.db;

import org.Nugetzrul3.CryptoTipper.Constants;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/// Class that holds database connection object.
public final class Database {
    private static Database instance;
    private DataSource dataSource;
    private final Constants constants = new Constants();

    private Database() {
    }

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }

        return instance;
    }

    /// Initialise connection.
    public void initDb() throws SQLException {
        PGSimpleDataSource ds =  new PGSimpleDataSource();
        String postgresUrl = "jdbc:postgresql://" + constants.dbHost + ":" + constants.dbPort + "/CryptoTipper";
        ds.setUser(constants.dbUser);
        ds.setPassword(constants.dbPass);
        ds.setUrl(postgresUrl);
        this.dataSource = ds;
        try (
                Connection connection = getConnection();
                Statement statement = connection.createStatement()
        ) {
            String createScript = """
                    CREATE TABLE IF NOT EXISTS users(
                        id SERIAL PRIMARY KEY,
                        uuid TEXT NOT NULL UNIQUE,
                        username TEXT,
                        address TEXT DEFAULT NULL
                    )
                    """;
            statement.execute(createScript);
        }
    }

    /// Returns a fresh connection object that can be used by other classes
    ///
    /// Note: unless using a try-with-resource block, ensure that the
    /// connection is closed to prevent leaks.
    public Connection getConnection() {
        if (this.dataSource == null) {
            throw new IllegalStateException("Database has not been initialised");
        }

        Connection con = null;

        try {
            con = this.dataSource.getConnection();
        } catch (SQLException e) {
            System.out.println("Error connecting to database");
            e.printStackTrace(System.err);
        }

        return con;
    }

}
