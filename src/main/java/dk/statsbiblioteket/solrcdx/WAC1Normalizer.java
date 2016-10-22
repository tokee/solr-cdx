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

import java.util.regex.Pattern;

/**
 * Web Archiving Community consensus 1 Normalizer.
 *
 * An attempt to normalize a given input URL according to the rules laid out by the web
 * archiving community af of fall 2016.
 *
 * Normalization is used internally by applications to provide slightly fuzzy lookups of
 * URLs by removing such things as session ID parameters and lowercasing everything.
 * For internal use, there is no special need for a formalization of the normalization.
 *
 * For external use, such as cross-institutional exchange of material with URL as key, it
 * works best if there is some sort of standard for normalization. As of fall 2016, this
 * is not really the case for the web archiving community. Nevertheless, the WAC1Normalizer
 * is a best-effort attempt of implementing the rules commonly used, erring on the side of
 * high recall at the cost of precision
 * (See <a href="https://en.wikipedia.org/wiki/Precision_and_recall">Precision and recall
 * on Wikipedia</a>).
 *
 * @see http://crawler.archive.org/articles/user_manual/glossary.html#surt
 * @see https://iipc.github.io/warc-specifications/specifications/cdx-format/cdx-2015/
 * @see https://github.com/iipc/warc-specifications/blob/gh-pages/specifications/cdx-format/openwayback-cdxj/index.md
 */
public class WAC1Normalizer {
    private static Log log = LogFactory.getLog(WAC1Normalizer.class);

    private final Pattern NOISE = Pattern.compile(";jsessionid=[0-9a-z]{10,}");

}
