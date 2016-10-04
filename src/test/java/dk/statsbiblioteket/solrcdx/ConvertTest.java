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

    @Test
    public void testLine() {
        String SAMPLE =
                "ar,com,adsclasificados,aimogasta)/publicacion/images/209408_1_small.jpg " +
                "20110225190307 " +
                "http://aimogasta.adsclasificados.com.ar/Publicacion/Images/209408_1_small.jpg " +
                "image/jpeg " +
                "200 " +
                "YN4T25EJJQ4FKAHRXN7TDZF2AOQ43D2X - " +
                "- 2505 " +
                "271488344 " +
                "testWARCfiles/WIDE-20110225183219005-04371-13730~crawl301.us.archive.org~9443.warc.gz";
        String EXPECTED =
                "YN4T25EJJQ4FKAHRXN7TDZF2AOQ43D2X20110225190307," +
                "ar\\,com\\,adsclasificados\\,aimogasta)/publicacion/images/209408_1_small.jpg," +
                "2011-02-25T19:03:07Z," +
                "http://aimogasta.adsclasificados.com.ar/Publicacion/Images/209408_1_small.jpg," +
                "image/jpeg,200,YN4T25EJJQ4FKAHRXN7TDZF2AOQ43D2X," +
                "," +
                "271488344," +
                "testWARCfiles/WIDE-20110225183219005-04371-13730~crawl301.us.archive.org~9443.warc.gz," +
                "ar\\,com\\,adsclasificados\\,aimogasta," +
                "publicacion/images/209408_1_small.jpg";
        assertEquals("Conversion of sample line should work as expected", EXPECTED, Convert.convertLine(SAMPLE));
    }
}