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
            try (
                Connection conn = Database.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO users (uuid, username) VALUES (?, ?) " +
                     "ON CONFLICT (uuid) DO UPDATE SET username = EXCLUDED.username"
                )
            ) {
                // Try insert or update
                stmt.setString(1, uuid);
                stmt.setString(2, username);

                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public void tipUser(String senderUuid, String receiverUuid, Double amount) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = Database.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try (
                    PreparedStatement debit = conn.prepareStatement(
                        "UPDATE users SET balance = balance - ? WHERE id = ?"
                    );
                    PreparedStatement credit = conn.prepareStatement(
                        "UPDATE users SET balance = balance + ? WHERE id = ?"
                    )
                ) {
                    // simulate a 'move'
                    debit.setDouble(1, amount);
                    debit.setString(2, senderUuid);
                    debit.executeUpdate();

                    credit.setDouble(1, amount);
                    credit.setString(2, receiverUuid);
                    credit.executeUpdate();

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
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
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getDouble(6)
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
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getDouble(6)
                    );
                } else {
                    return null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void updateUserAddress(String uuid, String address, String type) {
        CompletableFuture.runAsync(() -> {
            try (
                    Connection conn = Database.getInstance().getConnection()
            ) {
                PreparedStatement statement;
                if (type.equals("withdraw")) {
                    statement = conn.prepareStatement("UPDATE users SET withdraw_addr = ? WHERE uuid = ?");
                } else if (type.equals("deposit")) {
                    statement = conn.prepareStatement("UPDATE users SET address = ? WHERE uuid = ?");
                } else {
                    return;
                }
                statement.setString(1, address);
                statement.setString(2, uuid);

                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // maybe for admin usages???
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
