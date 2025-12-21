package org.Nugetzrul3.CryptoTipper;

/// Holds constants for RPC server and coin configurations
public class Constants {
    // RPC Server consts
    public static final String rpchost = "localhost";
    public static final Integer rpcport = 9982;
    public static final String rpcuser = "user";
    public static final String rpcpass = "pass";

    // Coin consts
    public static final String ticker = "ADVC";
    public static final String coinName = "AdventureCoin";
    public static final int conf = 6;
    public static final String explorer = "https://explorer.adventurecoin.quest/#/transaction/";
    public static final Double withdraw_fee = 0.0005;
    public static final Double withdraw_limit = 10.0;
    public static final Double min_withdraw = 5.0;

    // DB consts
    public static final String dbHost = "127.0.0.1";
    public static final Integer dbPort = 5432;
    public static final String dbUser = "postgres";
    public static final String dbPass = "postgres";
}
