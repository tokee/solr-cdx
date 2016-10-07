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

import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import static org.junit.Assert.*;

public class ServerTest {

    // Manual testing indicates that chunked streaming works, but URL.openStream() in the test cannot handle chunking
    // TODO: Make the unit test support chunked streams
    public void testStreamingConversion() throws IOException {
        final String SAMPLE = "sample_small.cdx";
        HttpServer server = Server.create();

        URL cdx = Util.resolveURL(SAMPLE);
        if (cdx == null) {
            throw new FileNotFoundException("Unable to locate " + SAMPLE);
        }
        URL csv = new URL("http://localhost:" + Config.getInt("solrcdx.port") + "/convert?stream.url=" +
                          URLEncoder.encode(cdx.toString(), "utf-8"));
        String output = Util.fetchString(csv);
        assertEquals("The CDX sample should be converted correctly", "expected", output);
        server.stop(1);
    }
}