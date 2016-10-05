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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class Convert {
    private static Log log = LogFactory.getLog(Convert.class);

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || "-h".equals(args[0])) {
            usage();
            return;
        }
        for (String arg: args) {
            convertFile(arg);
        }
    }

    public static final String CDX_HEADER = " CDX N b a m s k r M S V g";
    public static final String CSV_HEADER= "id,url,date,ourl,mime,response,newdigest,redirect,offset,arc,sdomain,path";
    public static final int CDX_COLUMN_COUNT = CDX_HEADER.trim().split(" ").length - 1;
    /*
        # In:   N b a m s k r M S V g
        # Solr: A b e a m s c k r V v D d g M n
        # Mix:  N b a m s k r V g sdomain path
        # Name: url,date,ourl,mime,response,newdigest,redirect,offset,arc
     */
    private static void convertFile(String file) throws IOException {
        final Path cdx = Paths.get(file);
        final Path csv = Paths.get(file + ".csv");
        if (!Files.exists(cdx)) {
            error("The CDX file '" + cdx + "' does not exist");
            return;
        }
        log.debug("Starting conversion of '" + cdx + "' -> '" + csv + "'");
        final long startTime = System.nanoTime();
        try (FileInputStream fis = new FileInputStream(cdx.toFile());
             BufferedReader cdxReader = new BufferedReader(new InputStreamReader(
                     file.endsWith(".gz") ? new GZIPInputStream(fis) : fis));
             FileWriter fw = new FileWriter(csv.toFile(), false);
             BufferedWriter csvWriter = new BufferedWriter(fw)) {

            csvWriter.write(CSV_HEADER);
            csvWriter.write("\n");
            long entries = -1;
            String line;
            while ((line = cdxReader.readLine()) != null) {
                entries++;
                // TODO: Create a flexible reader with minimum requirements and maximum support
                if (entries == 0 && !CDX_HEADER.equals(line)) {
                    error(String.format(
                            "The current implementation only works with CDX files with the exact format '%s', " +
                            "but the format for the file '%s' was '%s'", CDX_HEADER, cdx, line));
                    return;
                }
                String csvLine = convertLine(line);
                if (!"".equals(csvLine)) {
                    csvWriter.write(csvLine);
                    csvWriter.write("\n"); // Don't use newline() as we always want \n
                }
            }
            csvWriter.flush();
            fw.flush();
            info(String.format("Converted %d entries from '%s' in %d seconds",
                               entries, cdx, (System.nanoTime() - startTime) / 1000000 / 60));
        }
    }

    private final static Pattern SSPLIT = Pattern.compile(" ");
    private final static Pattern PSPLIT = Pattern.compile("\\)?/");
    public static String convertLine(String cdxLine) {
        // ar,com,adsclasificados,aimogasta)/publicacion/images/209408_1_small.jpg 20110225190307 http://aimogasta.adsclasificados.com.ar/Publicacion/Images/209408_1_small.jpg image/jpeg 200 YN4T25EJJQ4FKAHRXN7TDZF2AOQ43D2X - - 2505 271488344 testWARCfiles/WIDE-20110225183219005-04371-13730~crawl301.us.archive.org~9443.warc.gz
        String tokens[] = SSPLIT.split(cdxLine);
        if (tokens.length != CDX_COLUMN_COUNT) {
            log.debug("The CDX line '" + cdxLine + "' did not conform to the header '" + CDX_HEADER + "'");
            return "";
        }
        final String[] split = PSPLIT.split(tokens[0], 2);
        // id,url,date,ourl,mime,response,newdigest,redirect,offset,arc,sdomain,path
        // We choose the ID to be digest+timestamp as that is the shortest near-guaranteed unique ID we can construct
        return z(tokens[5]+ tokens[1]) + "," +       // id:       YN4T25EJJQ4FKAHRXN7TDZF2AOQ43D2X20110225190307
               z(tokens[0]) + "," +                  // url:      ar,com,adsclasificados,aimogasta)/publicacion/images/209408_1_small.jpg
               toDate(tokens[1]) + "," +             // date:     20110225190307
               z(tokens[2]) + "," +                  // ourl:     http://aimogasta.adsclasificados.com.ar/Publicacion/Images/209408_1_small.jpg
               z(tokens[3]) + "," +                  // mime:     image/jpeg
               z(tokens[4]) + "," +                  // response: 200
               z(tokens[5]) + "," +                  // newdigest: YN4T25EJJQ4FKAHRXN7TDZF2AOQ43D2X
               z(tokens[6]) + "," +                  // redirect: -
               // t7 t8: - 2505
               z(tokens[9]) + "," +                  // offset:   271488344
               z(tokens[10]) + "," +                 // arc:      testWARCfiles/WIDE-20110225183219005-04371-13730~crawl301.us.archive.org~9443.warc.gz
               z(split[0]) + "," +                   // sdomain:  ar,com,adsclasificados,aimogasta
               (split.length == 1 ? "" : z(split[1])); // path:   publicacion/images/209408_1_small.jpg
    }

    private static String toDate(String cdxDate) {
        return cdxDate.substring(0, 4) + "-" + cdxDate.substring(4, 6) + "-" + cdxDate.substring(6, 8) + "T" +
               cdxDate.substring(8, 10) + ":" + cdxDate.substring(10, 12) + ":" + cdxDate.substring(12, 14) + "Z";
    }

    private static String z(String s) {
        return "".equals(s) || "-".equals(s) ? "" : s.replace("\\", "\\\\").replace(",", "\\,");
    }

    private static void error(String message) {
        log.error(message);
        System.err.println(message);
    }

    private static void info(String message) {
        log.info(message);
        System.out.println(message);
    }

    private static void usage() {
        System.out.println("CDX->CSV Convert");
        System.out.println("Usage: java -jar solrcdx.jar cdx-file*");
    }
}
