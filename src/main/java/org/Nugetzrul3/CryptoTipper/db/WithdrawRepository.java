package org.Nugetzrul3.CryptoTipper.db;

import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class WithdrawRepository {
    public void insertWithdraw(
        String txid,
        String withdraw_addr,
        Double amount,
        Integer user_id
    ) {
        CompletableFuture.runAsync(() -> {
            try (
                Connection conn = Database.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO withdraws(txid, withdraw_addr, amount, user_id) VALUES (?, ?, ?, ?)"
                )
            ) {
                stmt.setString(1, txid);
                stmt.setString(2, withdraw_addr);
                stmt.setDouble(3, amount);
                stmt.setInt(4, user_id);

                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void insertWithdraw(
        String txid,
        String withdraw_addr,
        Double amount,
        String uuid
    ) {
        CompletableFuture.runAsync(() -> {
            try (
                Connection conn = Database.getInstance().getConnection()
            ) {
                PreparedStatement getUserStmt = conn.prepareStatement("SELECT id from users where uuid = ?");
                getUserStmt.setString(1, uuid);
                ResultSet rs = getUserStmt.executeQuery();
                int user_id;

                if (rs.next()) {
                    user_id = rs.getInt(1);
                } else {
                    return;
                }

                PreparedStatement insertWithdrawStmt = conn.prepareStatement("INSERT INTO withdraws(txid, withdraw_addr, amount, user_id) VALUES (?, ?, ?, ?)");
                insertWithdrawStmt.setString(1, txid);
                insertWithdrawStmt.setString(2, withdraw_addr);
                insertWithdrawStmt.setDouble(3, amount);
                insertWithdrawStmt.setInt(4, user_id);
                insertWithdrawStmt.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Double> getCurrentWithdrawAmount(String uuid, Date date) {
        return CompletableFuture.supplyAsync(() -> {
            try (
                Connection conn = Database.getInstance().getConnection()
            ) {
                PreparedStatement userStatement = conn.prepareStatement(
                    "SELECT id FROM users WHERE uuid = ?"
                );

                userStatement.setString(1, uuid);

                ResultSet userRs = userStatement.executeQuery();
                int user_id;

                if (userRs.next()) {
                    user_id = userRs.getInt(1);
                } else {
                    return null;
                }

                PreparedStatement withdrawStatement = conn.prepareStatement(
                    "SELECT SUM(amount) FROM withdraws WHERE user_id = ? AND time = ?"
                );

                withdrawStatement.setInt(1, user_id);
                withdrawStatement.setDate(2, date);

                ResultSet withdrawRs = withdrawStatement.executeQuery();
                double sum = 0.0;

                if (withdrawRs.next()) {
                    sum = withdrawRs.getDouble(1);
                }

                return sum;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
