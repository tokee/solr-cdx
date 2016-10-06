/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.solrcdx;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Bare-bones webserver that translates CDX Server API requests & responses to and from Solr.
 */
public class Server implements HttpHandler {
    private static Log log = LogFactory.getLog(Server.class);
    private static final String BASE;
    static {
        String base;
        try {
            base = Util.fetchString("index.html");
        } catch (IOException e) {
            base = "Internal error: Unable to resolve index.html";
        }
        BASE = Util.expand(base);
    }


    public static void main(String[] args) throws IOException {
        int port = Config.getInt("solrcdx.port");
        String message = "Running CDX-server at http://localhost:" + port + "/ connecting to " + SOLR_SELECT;
        log.info(message);
        System.out.println(message);
        // port, backlog (queue size)
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 100);
        server.createContext("/", new Server());
        server.setExecutor(null); // creates a default executor
        server.start(); // HttpServer is not in daemon-mode, so it runs until the JVM is shut down
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params = getParams(exchange);
        // https://archive.org/web/researcher/cdx_file_format.php
        if (params.containsKey("url")) {
            handleSimpleURL(exchange, params);
        } else {
            sendResponse(exchange, 200, BASE);
        }
    }

    private void handleSimpleURL(HttpExchange exchange, Map<String, String> params) throws IOException {
        String url = URLDecoder.decode(params.get("url"), "utf-8");
        log.debug("simpleURL(url='" + url + "')");
        handleSolr(exchange, "q", "ourl:\"" + solrEscape(url) + "\"");
        sendResponse(exchange, 200, "Simple URL: " + url);
    }

    final String SOLR_DEFAULTS = "fl=" + Config.getString("solr.fl") +
                                 "&rows=" + Config.getInt("solr.rows") +
                                 "&wt=csv";
    private void handleSolr(HttpExchange exchange, String... keyValues) throws IOException {
        if (keyValues.length%2 != 0) {
            throw new IllegalArgumentException("There was an uneven number of key-value entries: " + keyValues.length);
        }
        StringBuilder params = new StringBuilder();
        params.append(SOLR_DEFAULTS);
        for (int i = 0 ; i < keyValues.length ; i+=2) {
            if (params.length() != 0) {
                params.append("&");
            }
            params.append(keyValues[0]).append("=").append(URLEncoder.encode(keyValues[1], "utf-8"));
        }
        pipeURL(exchange, new URL(SOLR_SELECT + params));
        //http://localhost:50001/solr/cdx9/select?fl=ourl&indent=on&q=ourl:%22http://ing.dk/rank/rtx_telecom?nocache=1?nocache=1?nocache=1%22&wt=csv
    }

    private void pipeURL(HttpExchange exchange, URL url) throws IOException {
        log.debug("Piping URL: " + url);
        String result;
        try {
            result = Util.fetchString(url);
        } catch (IOException e) {
            String error = "Exception sending request '" + url + "': " + e.getMessage();
            sendResponse(exchange, 500, error);
            return;
        }
        sendResponse(exchange, 200, result);
    }

    private String solrEscape(String value) {
        return value.replace("\"", "\\\"");
    }

    private Map<String, String> getParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String[] l1 = exchange.getRequestURI().toString().split("[?]", 2);
        if (l1.length != 2) {
            return params;
        }
        for (String kv: l1[1].split("&")) {
            String[] kvt = kv.split("=", 2);
            if (kvt.length == 2) {
                params.put(kvt[0], kvt[1]);
            }
        }
        return params;
    }

    private void sendResponse(HttpExchange exchange, int code, String content) throws IOException {
        exchange.sendResponseHeaders(code, content.length());
        OutputStream os = exchange.getResponseBody();
        os.write(content.getBytes("utf-8"));
        os.close();
    }

    private String getRequestBody(HttpExchange t) throws IOException {
        final Charset UTF8 = Charset.forName("UTF-8");
        InputStream is = t.getRequestBody();
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, 1, UTF8));
        }
        return sb.toString();
    }

    public static final String SOLR;
    private static final String SOLR_SELECT;
    static {
        // http://localhost:50001/solr/cdx9/select?
        // fl=ourl&indent=on&q=ourl:%22http://ing.dk/rank/rtx_telecom?nocache=1?nocache=1?nocache=1%22&wt=json
        StringBuilder sb = new StringBuilder();
        {
            String s = Config.getString("solr.machine");
            if (!s.startsWith("http")) {
                sb.append("http://");
            }
            sb.append(s); // http://localhost
        }
        sb.append(":").append(Integer.toString(Config.getInt("solr.port"))); // http://localhost:50001
        sb.append("/").append(Config.getString("solr.path")); // http://localhost:50001/solr
        sb.append("/").append(Config.getString("solrcdx.collection")); // http://localhost:50001/solr/cdx
        sb.append("/");
        SOLR = sb.toString();

        sb.append(Config.getString("solr.handler")); // http://localhost:50001/solr/cdx/select
        sb.append("?"); // http://localhost:50001/solr/cdx/select?
        SOLR_SELECT = sb.toString();
    }

}
