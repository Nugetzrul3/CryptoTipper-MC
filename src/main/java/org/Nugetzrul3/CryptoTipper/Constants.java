package org.Nugetzrul3.CryptoTipper;

import java.math.BigDecimal;

/// Holds constants for RPC server and coin configurations
public class Constants {
    // RPC Server consts
    public final String rpchost = "localhost";
    public final Integer rpcport = 9982;
    public final String rpcuser = "user";
    public final String rpcpass = "password";

    // Coin consts
    public String ticker = "ADVC";
    public String coinName = "AdventureCoin";
    public int conf = 6;
    public String explorer = "https://explorer.adventurecoin.quest/#/transaction/";
    public Double withdraw_fee = 0.0005;


    // DB consts
    public String dbHost = "localhost";
    public Integer dbPort = 5432;
    public String dbUser = "postgres";
    public String dbPass = "password";
}
