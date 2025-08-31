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
    public BigDecimal withdraw_fee = new BigDecimal("0.001");
    public String price_url = "https://api.coingecko.com/api/v3/simple/price?ids=advc&vs_currencies=btc,usd";


    // DB consts
    public String dbHost = "localhost";
    public Integer dbPort = 5432;
    public String dbUser = "postgres";
    public String dbPass = "password";
}
