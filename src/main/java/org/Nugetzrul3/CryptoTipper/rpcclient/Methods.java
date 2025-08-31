package org.Nugetzrul3.CryptoTipper.rpcclient;

import com.google.gson.*;
import org.Nugetzrul3.CryptoTipper.Constants;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/// Contains RPC methods that are used throughout the tip plugin
public class Methods {
    private final Client client;

    public Methods() {
        this.client = new Client();
    }

    public CompletableFuture<JsonObject> getBlockchainInfo() {
        CompletableFuture<HttpResponse<String>> blockchainInfo = this.client.sendRequest("getblockchaininfo");
        CompletableFuture<HttpResponse<String>> networkHashPs = this.client.sendRequest("getnetworkhashps");

        return blockchainInfo.thenCombine(networkHashPs, (blockchainResp, networkResp) -> {
            JsonObject blockchainJson = JsonParser.parseString(blockchainResp.body()).getAsJsonObject();
            JsonObject networkJson = JsonParser.parseString(networkResp.body()).getAsJsonObject();

            // Handle error responses gracefully
            if (blockchainResp.statusCode() != 200 || !(blockchainJson.get("error") instanceof JsonNull)) {
                return blockchainJson; // contains error info from Bitcoin RPC
            }

            if (networkResp.statusCode() != 200 || !(networkJson.get("error") instanceof JsonNull)) {
                return networkJson; // contains error info
            }

            JsonObject resultJson = blockchainJson.getAsJsonObject("result");
            resultJson.add("hashps", networkJson.get("result"));
            blockchainJson.add("result", resultJson);

            return blockchainJson;
        });
    }

    public CompletableFuture<JsonObject> getUserBalance(String uuid) {
        JsonArray params =  new JsonArray();
        params.add(uuid);
        params.add(0);

        CompletableFuture<HttpResponse<String>> userBalanceUnconfirmed = this.client.sendRequest("getbalance", params);

        Constants constants = new Constants();
        params.remove(1);
        params.add(constants.conf);

        CompletableFuture<HttpResponse<String>> userBalanceConfirmed = this.client.sendRequest("getbalance", params);

        return userBalanceUnconfirmed.thenCombine(userBalanceConfirmed, (unconfirmedResp, confirmedResp) -> {
            JsonObject unconfirmedJson = JsonParser.parseString(unconfirmedResp.body()).getAsJsonObject();
            JsonObject confirmedJson = JsonParser.parseString(confirmedResp.body()).getAsJsonObject();

            // Handle error responses gracefully
            if (unconfirmedResp.statusCode() != 200 || !(unconfirmedJson.get("error") instanceof JsonNull)) {
                return unconfirmedJson; // contains error info from Bitcoin RPC
            }

            if (confirmedResp.statusCode() != 200 || !(confirmedJson.get("error") instanceof JsonNull)) {
                return confirmedJson; // contains error info
            }

            JsonElement confResult = confirmedJson.get("result");
            JsonElement unconfResult = unconfirmedJson.get("result");
            JsonObject resultJson = new JsonObject();

            resultJson.add("confBal", confResult);
            resultJson.add("unconfBal", unconfResult);
            resultJson.add("error", null);

            return resultJson;
        });

    }

    public CompletableFuture<JsonObject> getDepositAddress(String uuid) {
        JsonArray params =  new JsonArray();
        params.add(uuid);

        return this.client.sendRequest(
                "getaccountaddress",
                params
        ).thenApply(response -> JsonParser.parseString(response.body()).getAsJsonObject());
    }

    public CompletableFuture<JsonObject> withdraw(String address, Double amount, String uuid) {
        JsonArray params = new JsonArray();
        params.add(uuid);
        params.add(address);
        params.add(amount);

        return this.client.sendRequest(
                "sendfrom",
                params
        ).thenApply(response -> JsonParser.parseString(response.body()).getAsJsonObject());
    }
}
