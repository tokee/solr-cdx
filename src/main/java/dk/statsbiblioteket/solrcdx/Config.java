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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class Config {
    private static Log log = LogFactory.getLog(Config.class);
    public static final String HOME_KEY = "solrcdx.home";

    private static final String DEFAULT_PROPS = "solrcdx.default.properties";
    private static final String PROPS = "solrcdx.properties";
    private static final String home;
    private static final Properties conf;

    static { // Default values
        String buildHome = "";
        if (System.getenv(HOME_KEY) != null) {
            buildHome = System.getenv(HOME_KEY);
        } else if (System.getProperty(HOME_KEY) != null) {
            buildHome = System.getProperty(HOME_KEY);
        }
        if (!buildHome.isEmpty()) {
            if (!buildHome.endsWith("/")) {
                buildHome += "/";
            }
            log.info("Using '" + buildHome + "' as root for property resolving");
        }
        home = buildHome;

        URL urlD = Util.resolveURL(DEFAULT_PROPS);
        Properties confL = null;
        if (urlD == null) {
            log.info("No default properties " + DEFAULT_PROPS + " found. Attempting override properties");
        } else {
            try {
                try (InputStream is = urlD.openStream()) {
                    confL = new Properties();
                    confL.load(is);
                }
                log.info("Loaded default properties from " + urlD);
            } catch (Exception e) {
                String message = "Unable to load default properties from '" + urlD + "'";
                log.fatal(message + ". solrcdx will not be able to start", e);
                throw new RuntimeException(message, e);
            }
        }

        log.debug("Attempting load of override properties from " + buildHome + PROPS);
        URL urlO = Util.resolveURL(buildHome + PROPS);
        if (urlO == null) {
            if (confL == null) {
                String message = "Neither " + DEFAULT_PROPS + " nor " + buildHome + PROPS + " could be located";
                log.fatal(message + ". solrcdx will not be able to start");
                throw new IllegalStateException(message);
            }
            log.info("Only " + DEFAULT_PROPS + " available. No overrides loaded");
        } else {
            try {
                try (InputStream is = urlO.openStream()) {
                    if (confL == null) {
                        confL = new Properties();
                        confL.load(is);
                        log.info("Loaded default override properties only from " + urlO);
                    } else {
                        confL = new Properties(confL);
                        confL.load(is);
                        log.info("Loaded and layered overrides from " + urlO);
                    }
                }
            } catch (Exception e) {
                String message = "Unable to load override properties from '" + urlO + "'";
                log.fatal(message + ". solrcdx will not be able to start", e);
                throw new RuntimeException(message, e);
            }
        }
        conf = confL;
    }
    public static Integer getInt(String key) {
        return Integer.parseInt(conf.getProperty(key));
    }
    public static Long getLong(String key) {
        return Long.parseLong(conf.getProperty(key));
    }
    public static String getString(String key) {
        return conf.getProperty(key);
    }
    public static Boolean getBool(String key) {
        return Boolean.parseBoolean(conf.getProperty(key));
    }
    public static Double getDouble(String key) {
        return Double.parseDouble(conf.getProperty(key));
    }

}
