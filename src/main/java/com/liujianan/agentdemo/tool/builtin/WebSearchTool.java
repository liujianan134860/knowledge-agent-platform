package com.liujianan.agentdemo.tool.builtin;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liujianan.agentdemo.tool.ToolInputParser;

@Service
public class WebSearchTool {
    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final int MAX_RESULTS = 10;
    private static final int DEFAULT_COUNT = 5;

    @Tool(name = "web.search", description = "Search the internet via DuckDuckGo or Wikipedia. " +
            "Input JSON: {\"query\":\"...\", \"source\":\"web|wikipedia\", \"count\":5}")
    public String search(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String query = (String) params.get("query");
            if (query == null || query.isBlank()) return "Error: query is required";

            String source = (String) params.getOrDefault("source", "web");
            int count = Math.min(MAX_RESULTS, params.containsKey("count")
                    ? Integer.parseInt(params.get("count").toString()) : DEFAULT_COUNT);

            if ("wikipedia".equalsIgnoreCase(source)) {
                return searchWikipedia(query, count);
            }
            return searchDuckDuckGo(query, count);
        } catch (Exception e) {
            log.error("web.search failed", e);
            return "Error searching web: " + e.getMessage();
        }
    }

    private String searchDuckDuckGo(String query, int count) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://lite.duckduckgo.com/lite?q=" + encoded;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "KnowledgeAgent/1.0")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        String html = response.body();

        List<SearchResult> results = parseDuckDuckGoLite(html, count);
        if (results.isEmpty()) return "No results found for: " + query;

        StringBuilder sb = new StringBuilder("Search results for \"" + query + "\" (source: DuckDuckGo):\n\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append(i + 1).append(". ").append(r.title()).append("\n");
            sb.append("   ").append(r.snippet()).append("\n");
            sb.append("   URL: ").append(r.url()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private List<SearchResult> parseDuckDuckGoLite(String html, int count) {
        List<SearchResult> results = new ArrayList<>();
        // DuckDuckGo Lite wraps results in <a> links with class "result-link"
        // and snippets in <td class="result-snippet">
        Pattern linkPattern = Pattern.compile(
                "<a[^>]*href=\"([^\"]*)\"[^>]*class=\"[^\"]*result-link[^\"]*\"[^>]*>([^<]*)</a>",
                Pattern.CASE_INSENSITIVE);
        Pattern snippetPattern = Pattern.compile(
                "<td[^>]*class=\"[^\"]*result-snippet[^\"]*\"[^>]*>([^<]*(?:<[^/][^>]*>[^<]*</[^>]*>[^<]*)*)</td>",
                Pattern.CASE_INSENSITIVE);

        // Simpler approach: find all result rows
        Pattern rowPattern = Pattern.compile(
                "<tr[^>]*class=\"[^\"]*result[^\"]*\"[^>]*>(.*?)</tr>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher rowMatcher = rowPattern.matcher(html);
        while (rowMatcher.find() && results.size() < count) {
            String row = rowMatcher.group(1);
            Matcher linkMatcher = linkPattern.matcher(row);
            Matcher snippetMatcher = snippetPattern.matcher(row);

            String title = "";
            String url = "";
            if (linkMatcher.find()) {
                url = linkMatcher.group(1);
                title = linkMatcher.group(2).trim();
            }
            String snippet = "";
            if (snippetMatcher.find()) {
                snippet = snippetMatcher.group(1).replaceAll("<[^>]+>", "").trim();
            }
            // Also try plain <a> links as fallback
            if (title.isEmpty()) {
                Matcher fallbackLink = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>")
                        .matcher(row);
                if (fallbackLink.find()) {
                    url = fallbackLink.group(1);
                    title = fallbackLink.group(2).trim();
                }
            }
            if (!title.isEmpty()) {
                results.add(new SearchResult(title, snippet, url));
            }
        }

        // Fallback: simple <a> tag extraction if regex patterns didn't match
        if (results.isEmpty()) {
            Pattern simpleLink = Pattern.compile(
                    "<a[^>]*href=\"(https?://[^\"]+)\"[^>]*>([^<]+)</a>");
            Matcher m = simpleLink.matcher(html);
            while (m.find() && results.size() < count) {
                String url = m.group(1);
                String title = m.group(2).trim();
                if (!url.contains("duckduckgo.com") && !title.isBlank()) {
                    results.add(new SearchResult(title, "", url));
                }
            }
        }
        return results;
    }

    private String searchWikipedia(String query, int count) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch="
                + encoded + "&format=json&srlimit=" + count;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "KnowledgeAgent/1.0")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = MAPPER.readTree(response.body());
        JsonNode searchResults = root.path("query").path("search");

        if (!searchResults.isArray() || searchResults.isEmpty()) {
            return "No Wikipedia results found for: " + query;
        }

        StringBuilder sb = new StringBuilder("Search results for \"" + query + "\" (source: Wikipedia):\n\n");
        for (int i = 0; i < searchResults.size(); i++) {
            JsonNode r = searchResults.get(i);
            String title = r.path("title").asText();
            String snippet = r.path("snippet").asText().replaceAll("<[^>]+>", "");
            String pageUrl = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(
                    title.replace(' ', '_'), StandardCharsets.UTF_8);
            sb.append(i + 1).append(". ").append(title).append("\n");
            sb.append("   ").append(snippet).append("\n");
            sb.append("   URL: ").append(pageUrl).append("\n\n");
        }
        return sb.toString().trim();
    }

    private record SearchResult(String title, String snippet, String url) {}
}
