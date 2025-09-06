package org.Nugetzrul3.CryptoTipper.rpcclient;

import com.google.gson.*;
import org.Nugetzrul3.CryptoTipper.Constants;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/// Contains RPC methods that are used throughout the tip plugin
public class Methods {
    private final Client client;
    private final Constants constants;

    public Methods() {
        this.client = new Client();
        this.constants = new Constants();
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

        CompletableFuture<HttpResponse<String>> userBalanceUnconfirmed = this.client.sendRequest(
            "getbalance",
            params
        );

        Constants constants = new Constants();
        params.remove(1);
        params.add(constants.conf);

        CompletableFuture<HttpResponse<String>> userBalanceConfirmed = this.client.sendRequest(
            "getbalance",
            params
        );

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

    public CompletableFuture<JsonObject> validateAddress(String address) {
        JsonArray params = new JsonArray();
        params.add(address);

        return this.client.sendRequest(
            "validateaddress",
            params
        ).thenApply(response -> JsonParser.parseString(response.body()).getAsJsonObject());

    }

    public CompletableFuture<JsonObject> withdraw(String address, Double amount, String uuid) {
        JsonArray params = new JsonArray();
        params.add(uuid);
        params.add(address);
        params.add(amount);

        Double withdrawalFee = this.constants.withdraw_fee;

        return this.client.sendRequest("sendfrom", params)
            .thenApply(response -> JsonParser.parseString(response.body()).getAsJsonObject())
            .thenCompose(response -> {
                if (!(response.get("error") instanceof JsonNull)) {
                    return CompletableFuture.completedFuture(response);
                }

                String txid = response.get("result").getAsString();

                // Chain all the operations together
                return getTransactionDetails(txid)
                    .thenCompose(txDetails -> chargeFees(uuid, txDetails, withdrawalFee))
                    .thenApply(feeResult -> {
                        // Create final response
                        JsonObject finalResponse = new JsonObject();
                        finalResponse.add("error", null);

                        JsonObject result = new JsonObject();
                        result.addProperty("txid", txid);
                        result.addProperty("withdrawalFee", withdrawalFee);
                        result.add("transaction", feeResult.get("transaction"));
                        result.addProperty("networkFee", feeResult.get("networkFee").getAsDouble());

                        finalResponse.add("result", result);
                        return finalResponse;
                    });
            });
    }

    private CompletableFuture<JsonObject> getTransactionDetails(String txid) {
        JsonArray txParams = new JsonArray();
        txParams.add(txid);

        return this.client.sendRequest("gettransaction", txParams)
            .thenApply(txResponse -> JsonParser.parseString(txResponse.body()).getAsJsonObject())
            .thenApply(txResponse -> {
                if (!(txResponse.get("error") instanceof JsonNull)) {
                    throw new RuntimeException("Failed to get transaction details: " + txResponse.get("error"));
                }
                return txResponse.getAsJsonObject("result");
            });
    }

    private CompletableFuture<JsonObject> chargeFees(String uuid, JsonObject txDetails, Double withdrawalFee) {
        Double txFee = txDetails.get("fee").getAsDouble();

        // Charge withdrawal fee
        JsonArray moveParams1 = new JsonArray();
        moveParams1.add(uuid);
        moveParams1.add("crypto-tipper-main");
        moveParams1.add(withdrawalFee);

        return this.client.sendRequest("move", moveParams1)
            .thenApply(response -> JsonParser.parseString(response.body()).getAsJsonObject())
            .thenCompose(moveResponse1 -> {
                if (!(moveResponse1.get("error") instanceof JsonNull)) {
                    throw new RuntimeException("Failed to charge withdrawal fee: " + moveResponse1.get("error"));
                }

                // Refund network fee
                JsonArray moveParams2 = new JsonArray();
                moveParams2.add("crypto-tipper-main");
                moveParams2.add(uuid);
                moveParams2.add(-txFee);

                return this.client.sendRequest("move", moveParams2)
                    .thenApply(response -> JsonParser.parseString(response.body()).getAsJsonObject())
                    .thenApply(moveResponse2 -> {
                        if (!(moveResponse2.get("error") instanceof JsonNull)) {
                            throw new RuntimeException("Failed to refund network fee: " + moveResponse2.get("error"));
                        }

                        // Return combined result
                        JsonObject result = new JsonObject();
                        result.add("transaction", txDetails);
                        result.addProperty("networkFee", txFee);
                        return result;
                    });
            });
    }


}
