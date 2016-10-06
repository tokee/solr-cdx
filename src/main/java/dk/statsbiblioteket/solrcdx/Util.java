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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    private static Log log = LogFactory.getLog(Util.class);

    public static String fetchExpandedString(String resource) throws IOException {
        return fetchExpandedString(resolveURL(resource));
    }
    public static String fetchString(String resource) throws IOException {
        return fetchString(resolveURL(resource));
    }

    public static String fetchExpandedString(URL url) throws IOException {
        return Util.expand(fetchString(url));
    }

    public static String fetchString(URL url) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(inputLine);
            }
        }
        return sb.toString();
    }

    public static URL resolveURL(String resource) {
        try {
            Path file = Paths.get(resource);
            if (Files.exists(file)) {
                return file.toUri().toURL();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception resolving '" + resource + "' as file", e);
        }

        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url != null) {
            return url;
        }

        try {
            return new URL(resource);
        } catch (MalformedURLException e) {
            return null;
            //throw new IllegalArgumentException("Exception resolving '" + resource + "' as URL", e);
        }
    }

    private static final Pattern EXPAND = Pattern.compile("[$][{]([^}]+)[}]");
    public static String expand(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        StringBuffer expanded = new StringBuffer();
        Matcher matcher = EXPAND.matcher(s);
        while (matcher.find()) {
            String expVal = Config.getString(matcher.group(1));
            if (expVal == null) {
                log.warn("Unable to expand variable \"" + matcher.group(1) + "\"");
//                System.out.println("Group " + matcher.group(1) + " input " + s);
                matcher.appendReplacement(expanded, "");
                expanded.append("${").append(matcher.group(1)).append("}");
            } else {
                matcher.appendReplacement(expanded, expVal);
            }
        }
        matcher.appendTail(expanded);
        return expanded.toString();
    }
}
