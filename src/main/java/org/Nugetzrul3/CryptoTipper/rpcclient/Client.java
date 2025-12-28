package org.Nugetzrul3.CryptoTipper.rpcclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.Nugetzrul3.CryptoTipper.Constants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class Client {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<HttpResponse<String>> sendRequest(String method) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", method);
        jsonObject.add("params", new JsonArray());
        jsonObject.addProperty("id", "crypto-tipper");

        String requestBody = jsonObject.toString();
        String auth = Constants.rpcuser + ":" + Constants.rpcpass;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://%s:%d", Constants.rpchost, Constants.rpcport)))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<HttpResponse<String>> sendRequest(String method, JsonArray params) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", method);
        jsonObject.addProperty("id", "crypto-tipper");
        jsonObject.add("params", params);

        String requestBody = jsonObject.toString();
        String auth = Constants.rpcuser + ":" + Constants.rpcpass;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://@%s:%d", Constants.rpchost, Constants.rpcport)))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
