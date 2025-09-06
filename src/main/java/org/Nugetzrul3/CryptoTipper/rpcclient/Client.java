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
    private final HttpClient client;
    private final Constants constants;

    public Client() {
        client = HttpClient.newHttpClient();
        constants = new Constants();
    }

    public CompletableFuture<HttpResponse<String>> sendRequest(String method) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", method);
        jsonObject.add("params", new JsonArray());
        jsonObject.addProperty("id", "crypto-tipper");

        String requestBody = jsonObject.toString();
        String auth = constants.rpcuser + ":" + constants.rpcpass;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://%s:%d", constants.rpchost, constants.rpcport)))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public CompletableFuture<HttpResponse<String>> sendRequest(String method, JsonArray params) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", method);
        jsonObject.addProperty("id", "crypto-tipper");
        jsonObject.add("params", params);

        String requestBody = jsonObject.toString();
        String auth = constants.rpcuser + ":" + constants.rpcpass;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://@%s:%d", constants.rpchost, constants.rpcport)))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
