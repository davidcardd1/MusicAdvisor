package advisor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class Main {
    static String accessCode = "";
    static String clientID = "56502cd7a6db4c23af1106af7816f416";
    static String clientSecret = "3cfe41c6093741f1b095af24ac74c6f5";

    static String authServerPath = "https://accounts.spotify.com";
    static String apiServerPath = "https://api.spotify.com";

    static int pageEntries = 5;
    static boolean isAuthorized = false;
    public static void main(String[] args) throws IOException, InterruptedException {

        Scanner scanner = new Scanner(System.in);
        String[] opt = scanner.nextLine().split(" ");

        if (args.length > 1) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-access")) {
                    authServerPath = args[i + 1];
                }
                if (args[i].equals("-resource")) {
                    apiServerPath = args[i + 1];
                }
                if (args[i].equals("-page")) {
                    pageEntries = Integer.parseInt(args[i + 1]);
                }
            }
            //System.out.println(authServerPath + " " + apiServerPath + " " + pageEntries);
        }

        menu(scanner, opt);


    }

    private static void menu(Scanner scanner, String[] opt) throws IOException, InterruptedException {
        while (!opt[0].equals("exit")) {
            if (isAuthorized) {
                switch (opt[0]) {
                    case "new":
                        newCommand();
                        break;
                    case "featured":
                        featuredCommand();
                        break;
                    case "categories":
                        categoriesCommand();
                        break;
                    case "playlists":
                        String cName = "";
                        for (int i = 1; i < opt.length; i++) {
                            cName += opt[i] + " ";
                        }
                        playlistsCommand(cName);
                        break;
                    case "exit":
                        System.out.println("---GOODBYE!---");
                        return;
                }
            } else {
                switch (opt[0]) {
                    case "auth":
                        authorize();
                        break;
                    case "exit":
                        System.out.println("---GOODBYE!---");
                        return;
                    default:
                        System.out.println("Please, provide access for application.");
                }
            }

            opt = scanner.nextLine().split(" ");
        }
    }

    private static void categoriesCommand() throws IOException, InterruptedException {
        String responseJson = apiRequest(apiServerPath + "/v1/browse/categories");
        JsonObject jo = JsonParser.parseString(responseJson).getAsJsonObject();
        errorCheck(jo);
        jo = jo.getAsJsonObject("categories");

        List <String> categories = new ArrayList<>();
        for (JsonElement category : jo.getAsJsonArray("items")) {
            JsonObject categoryO = category.getAsJsonObject();
            String name = categoryO.get("name").getAsString();
            categories.add(name);
        }
        printResponse(categories);
    }


    public static List<String> getPage(List<String> responseArray, int currentPage) {
        if (pageEntries <= 0) {
            throw new IllegalArgumentException("invalid page size");
        }
        int index = currentPage * pageEntries;
        return responseArray.subList(index, Math.min(index + pageEntries, responseArray.size()));
    }

    public static void printResponse(List<String> responseArray) {
        int pages = (responseArray.size() + pageEntries - 1 ) / pageEntries;
        int i = 0;
        Scanner scanner = new Scanner(System.in);

        printLines(responseArray, i, pages);
        String in = "";

        while (!in.equals("exit")) {
            in = scanner.nextLine();

            if (in.equals("next")) {
                if (i >= pages - 1) {
                    System.out.println("No more pages.");
                } else {
                    i++;
                    printLines(responseArray, i, pages);
                }
            }

            if (in.equals("prev")) {
                if (i <= 0) {
                    System.out.println("No more pages.");
                } else {
                    i--;
                    printLines(responseArray, i, pages);
                }
            }
        }
    }

    private static void printLines(List<String> responseArray, int i, int pages) {
        for (String line : getPage(responseArray, i)) {
            System.out.println(line);
        }
        System.out.println("---PAGE " + (i + 1) + " OF " + pages + "---");
    }


    private static void playlistsCommand(String cName) throws IOException, InterruptedException {
        String responseJson = apiRequest(apiServerPath + "/v1/browse/categories");
        JsonObject jo = JsonParser.parseString(responseJson).getAsJsonObject();
        errorCheck(jo);
        jo = jo.getAsJsonObject("categories");
        String catID = "";

        for (JsonElement category : jo.getAsJsonArray("items")) {
            JsonObject categoryO = category.getAsJsonObject();
            String name = categoryO.get("name").getAsString();
            if (name.equals(cName.trim())) {
                catID = categoryO.get("id").getAsString();
                break;
            }
        }

        if (catID.isBlank()) {
            System.out.println("Unknown category name.");
        } else {
            responseJson = apiRequest(apiServerPath + "/v1/browse/categories/" + catID + "/playlists");
            jo = JsonParser.parseString(responseJson).getAsJsonObject();
            errorCheck(jo);
            printPlaylists(jo);
        }
    }

    private static boolean errorCheck(JsonObject jo) {
        if (jo.has("error")) {
            String errorMsg = jo.getAsJsonObject("error").get("message").getAsString();
            System.out.println(errorMsg);
            return true;
        }
        return false;
    }

    private static void printPlaylists(JsonObject jo) {
        if (jo.has("playlists")) {
            jo = jo.getAsJsonObject("playlists");
            List <String> playlists = new ArrayList();
            for (JsonElement playlist : jo.getAsJsonArray("items")) {
                JsonObject playlistO = playlist.getAsJsonObject();
                String name = playlistO.get("name").getAsString();
                String link = playlistO.getAsJsonObject("external_urls").get("spotify").getAsString();

                playlists.add(name + "\n" + link + "\n");
            }
            printResponse(playlists);
        }
    }

    private static void featuredCommand() throws IOException, InterruptedException {
        String responseJson = apiRequest(apiServerPath + "/v1/browse/featured-playlists");
        JsonObject jo = JsonParser.parseString(responseJson).getAsJsonObject();
        errorCheck(jo);
        printPlaylists(jo);
    }

    private static void newCommand() throws IOException, InterruptedException {
        String responseJson = apiRequest(apiServerPath + "/v1/browse/new-releases");
        JsonObject jo = JsonParser.parseString(responseJson).getAsJsonObject();
        errorCheck(jo);
        jo = jo.getAsJsonObject("albums");

        List<String> albums = new ArrayList<>();
        for (JsonElement album : jo.getAsJsonArray("items")) {
            JsonObject albumO = album.getAsJsonObject();
            String name = albumO.get("name").getAsString();
            String link = albumO.getAsJsonObject("external_urls").get("spotify").getAsString();

            List<String> artists = new ArrayList<>();
            for (JsonElement artist : albumO.getAsJsonArray("artists")) {
                artists.add(artist.getAsJsonObject().get("name").getAsString());
            }


            albums.add(name + "\n" + artists + "\n" + link + "\n");
        }
        printResponse(albums);
    }

    private static void authorize() throws IOException, InterruptedException {
        System.out.println("use this link to request the access code:\n" +
                authServerPath + "/authorize?client_id=" + clientID + "&redirect_uri=http://localhost:8080/&response_type=code\n" +
                "waiting for code...");
        // server
        HttpServer server = HttpServer.create();
        server.bind(new InetSocketAddress(8080), 0);
        server.createContext("/",
                new HttpHandler() {
                    public void handle(HttpExchange exchange) throws IOException {
                        String query = exchange.getRequestURI().getQuery();
                        String possibleCode = query != null ? query.split("=")[1] : "";

                        if (query != null && query.split("=")[0].equals("code")) {
                            accessCode = possibleCode;
                            query = "Got the code. Return back to your program.";
                        } else {
                            query = "Authorization code not found. Try again.";
                        }

                        exchange.sendResponseHeaders(200, query.length());
                        exchange.getResponseBody().write(query.getBytes());
                        exchange.getResponseBody().close();
                    }
                }
        );
        server.start();

        while (accessCode.isEmpty()) {
            Thread.sleep(10);
        }
        server.stop(10);
        System.out.println("code received");

        // client
        HttpClient client = HttpClient.newBuilder().build();


        if (!accessCode.isEmpty()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientID + ":" + clientSecret).getBytes()))
                    .uri(URI.create(authServerPath + "/api/token"))
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=authorization_code&code=" + accessCode + "&redirect_uri=http://localhost:8080/"))
                    .build();

            HttpResponse<String>  response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response: \n" + response.body());
            JsonObject jo = JsonParser.parseString(response.body()).getAsJsonObject();
            accessCode = jo.get("access_token").getAsString();
            System.out.println("---SUCCESS---");
            isAuthorized = true;
        }
    }

    private static String apiRequest(String apiPath) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + accessCode)
                .uri(URI.create(apiPath))
                .GET()
                .build();
        HttpClient client = HttpClient.newBuilder().build();
        HttpResponse<String>  response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}

