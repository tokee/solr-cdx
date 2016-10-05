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

import org.junit.Test;

import static org.junit.Assert.*;

public class ConvertTest {

    public static final String SAMPLE_SURT =
            "ar,com,adsclasificados,aimogasta)/publicacion/images/209408_1_small.jpg " +
            "20110225190307 " +
            "http://aimogasta.adsclasificados.com.ar/Publicacion/Images/209408_1_small.jpg " +
            "image/jpeg " +
            "200 " +
            "YN4T25EJJQ4FKAHRXN7TDZF2AOQ43D2X - " +
            "- 2505 " +
            "271488344 " +
            "testWARCfiles/WIDE-20110225183219005-04371-13730~crawl301.us.archive.org~9443.warc.gz";
    public static final String EXPECTED_SURT =
            "YN4T25EJJQ4FKAHRXN7TDZF2AOQ43D2X20110225190307," +
            "ar\\,com\\,adsclasificados\\,aimogasta)/publicacion/images/209408_1_small.jpg," +
            "2011-02-25T19:03:07Z," +
            "http://aimogasta.adsclasificados.com.ar/Publicacion/Images/209408_1_small.jpg," +
            "image/jpeg," +
            "200," +
            "YN4T25EJJQ4FKAHRXN7TDZF2AOQ43D2X," +
            "," +
            "271488344," +
            "testWARCfiles/WIDE-20110225183219005-04371-13730~crawl301.us.archive.org~9443.warc.gz," +
            "ar\\,com\\,adsclasificados\\,aimogasta," +
            "publicacion/images/209408_1_small.jpg";
    @Test
    public void testSurt() {
        assertEquals("Conversion of sample line should work as expected",
                     EXPECTED_SURT, Convert.convertLine(SAMPLE_SURT));
    }

    // Not properly SURTed, contains comma and backslash in the URL.
    public static final String SAMPLE_MESSY =
            "roar.com/redirps.htm?enc=1&p=3&vars=337;;~gngevtqpkeu~jvvr<11uvqtghtqpv0nkpmu{pgti{0eqo1hu/dkp1uvqtgagkf" +
            "?ixv,\\,tyeh[(qhhgtkf?36587(uvkf?9(uwdkf?~553~2~4~~~449~~~2~pm:6yi3cc4:q29fde:qlm2mq~36468;7759~324;7368" +
            "=32224633~64;~5~3520447048035;~~~yyy0dguvftkxgtu0qti~~~~~~~&ws=103 " +
            "20150223131313 " +
            "http://www.roar.com/redirps.htm?p=3&ws=103&enc=1&vars=337%3B%3B%7EGngevtqpkeu%7Ejvvr%3C11uvqtghtqpv0nkpm" +
            "u%7Bpgti%7B0eqo1hu%2Fdkp1uvqtgAgkf%3FIXv%2C%5C%2CtYeh%5B%28qhhgtkf%3F36587%28uvkf%3F9%28uwdkf%3F%7E553" +
            "%7E2%7E4%7E%7E%7E449%7E%7E%7E2%7Epm%3A6yi3cc4%3Aq29fde%3Aqlm2mq%7E36468%3B7759%7E324%3B7368%3D32224633" +
            "%7E64%3B%7E5%7E3520447048035%3B%7E%7E%7Eyyy0dguvftkxgtu0qti%7E%7E%7E%7E%7E%7E%7E " +
            "text/html " +
            "200 " +
            "WOHPFAZTJGHWFA6GUS5DTGXJBGXSS62Q " +
            "- - - " +
            "206245741 " +
            "224161-223-20150223121926-00000-sb-prod-har-006.statsbiblioteket.dk.warc";
    public static final String EXPECTED_MESSY =
            "WOHPFAZTJGHWFA6GUS5DTGXJBGXSS62Q20150223131313," +
            "roar.com/redirps.htm?enc=1&p=3&vars=337;;~gngevtqpkeu~jvvr<11uvqtghtqpv0nkpmu{pgti{0eqo1hu/dkp1uvqtgagkf" +
            "?ixv\\,\\\\\\,tyeh[(qhhgtkf?36587(uvkf?9(uwdkf?~553~2~4~~~449~~~2~pm:6yi3cc4:q29fde:qlm2mq~36468;7759~324;" +
            "7368=32224633~64;~5~3520447048035;~~~yyy0dguvftkxgtu0qti~~~~~~~&ws=103," +
            "2015-02-23T13:13:13Z," +
            "http://www.roar.com/redirps.htm?p=3&ws=103&enc=1&vars=337%3B%3B%7EGngevtqpkeu%7Ejvvr%3C11uvqtghtqpv0nkpm" +
            "u%7Bpgti%7B0eqo1hu%2Fdkp1uvqtgAgkf%3FIXv%2C%5C%2CtYeh%5B%28qhhgtkf%3F36587%28uvkf%3F9%28uwdkf%3F%7E553" +
            "%7E2%7E4%7E%7E%7E449%7E%7E%7E2%7Epm%3A6yi3cc4%3Aq29fde%3Aqlm2mq%7E36468%3B7759%7E324%3B7368%3D32224633" +
            "%7E64%3B%7E5%7E3520447048035%3B%7E%7E%7Eyyy0dguvftkxgtu0qti%7E%7E%7E%7E%7E%7E%7E," +
            "text/html," +
            "200," +
            "WOHPFAZTJGHWFA6GUS5DTGXJBGXSS62Q," +
            "," +
            "206245741," +
            "224161-223-20150223121926-00000-sb-prod-har-006.statsbiblioteket.dk.warc," +
            "roar.com," +
            "redirps.htm?enc=1&p=3&vars=337;;~gngevtqpkeu~jvvr<11uvqtghtqpv0nkpmu{pgti{0eqo1hu/dkp1uvqtgagkf?" +
            "ixv\\,\\\\\\,tyeh[(qhhgtkf?36587(uvkf?9(uwdkf?~553~2~4~~~449~~~2~pm:6yi3cc4:q29fde:qlm2mq~36468;7759~324;" +
            "7368=32224633~64;~5~3520447048035;~~~yyy0dguvftkxgtu0qti~~~~~~~&ws=103";
    @Test
    public void testMessy() {
        assertEquals("Conversion of sample line should work as expected",
                     EXPECTED_MESSY.replaceAll("[^\\\\],", "\n"),
                     Convert.convertLine(SAMPLE_MESSY).replaceAll("[^\\\\],", "\n"));
    }

    @Test
    public void debug() {
        System.out.println("?ixv,\\,tyeh[(qhhgtkf?36587(uvkf?9(u".replace("\\", "***\\\\***").replace(",", "\\,"));
    }
}