package org.Nugetzrul3.CryptoTipper.db;
import org.Nugetzrul3.CryptoTipper.db.dao.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class UserRepository {
    public CompletableFuture<Void> upsertUser(String uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = Database.getInstance().getConnection()) {
                // Try insert or update, and return the row
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO users (uuid, username) VALUES (?, ?) " +
                                "ON CONFLICT (uuid) DO UPDATE SET username = EXCLUDED.username"
                )) {
                    stmt.setString(1, uuid);
                    stmt.setString(2, username);

                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public CompletableFuture<User> getUserByUuid(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (
                    Connection conn = Database.getInstance().getConnection();
                    PreparedStatement statement = conn.prepareStatement("SELECT * FROM users WHERE uuid = ?")
            ) {
                statement.setString(1, uuid);
                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    return new User(
                           rs.getInt(0),
                           rs.getString(1),
                           rs.getString(2)
                    );
                } else {
                    return null;
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<User> getUserByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try (
                    Connection conn = Database.getInstance().getConnection();
                    PreparedStatement statement = conn.prepareStatement("SELECT * FROM users WHERE username = ?")
            ) {
                statement.setString(1, username);
                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    return new User(
                            rs.getInt(0),
                            rs.getString(1),
                            rs.getString(2)
                    );
                } else {
                    return null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> deleteUser(String uuid) {
        return CompletableFuture.runAsync(() -> {
            try (
                    Connection conn = Database.getInstance().getConnection();
                    PreparedStatement statement = conn.prepareStatement("DELETE FROM users WHERE uuid = ?")
            ) {
                statement.setString(1, uuid);
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
