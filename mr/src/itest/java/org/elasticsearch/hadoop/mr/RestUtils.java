/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.hadoop.mr;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.hadoop.cfg.Settings;
import org.elasticsearch.hadoop.rest.Request;
import org.elasticsearch.hadoop.rest.Response;
import org.elasticsearch.hadoop.rest.RestClient;
import org.elasticsearch.hadoop.rest.RestClient.Health;
import org.elasticsearch.hadoop.serialization.dto.mapping.Field;
import org.elasticsearch.hadoop.util.ByteSequence;
import org.elasticsearch.hadoop.util.BytesArray;
import org.elasticsearch.hadoop.util.EsMajorVersion;
import org.elasticsearch.hadoop.util.IOUtils;
import org.elasticsearch.hadoop.util.StringUtils;
import org.elasticsearch.hadoop.util.TestSettings;
import org.elasticsearch.hadoop.util.TestUtils;
import org.elasticsearch.hadoop.util.unit.TimeValue;

import static org.elasticsearch.hadoop.rest.Request.Method.*;

public class RestUtils {

    public static class ExtendedRestClient extends RestClient {

        private static EsMajorVersion TEST_VERSION = null;

        private static Settings withVersion(Settings settings) {
            if (TEST_VERSION == null) {
                TEST_VERSION = TestUtils.getEsVersion();
            }
            settings.setInternalVersion(TEST_VERSION);
            return settings;
        }

        public ExtendedRestClient() {
            super(withVersion(new TestSettings()));
        }

        @Override
        public Response execute(Request.Method method, String path, ByteSequence buffer) {
            return super.execute(method, path, buffer);
        }

        public String get(String index) throws IOException {
            return IOUtils.asString(execute(Request.Method.GET, index));
        }

        public String post(String index, byte[] buffer) throws IOException {
            return IOUtils.asString(execute(Request.Method.POST, index, new BytesArray(buffer)).body());
        }

        public String put(String index, byte[] buffer) throws IOException {
            return IOUtils.asString(execute(PUT, index, new BytesArray(buffer)).body());
        }

        public String refresh(String index) throws IOException {
            return IOUtils.asString(execute(Request.Method.POST, index + "/_refresh", (ByteSequence) null).body());
        }
    }

    public static void putMapping(String index, String type, byte[] content) throws Exception {
        RestClient rc = new ExtendedRestClient();
        BytesArray bs = new BytesArray(content);
        rc.putMapping(index, index + "/" + type + "/_mapping", bs.bytes());
        rc.close();
    }

    /**
     * @deprecated use putMapping(index, type, content) instead
     */
    public static void putMapping(String indexAndType, byte[] content) throws Exception {
        List<String> parts = StringUtils.tokenize(indexAndType, "/");
        if (parts.size() != 2) {
            throw new IllegalArgumentException("Expected index/type, got [" + indexAndType + "] instead.");
        }
        putMapping(parts.get(0), parts.get(1), content);
    }

    public static Field getMapping(String index) throws Exception {
        ExtendedRestClient rc = new ExtendedRestClient();
        Field parseField = Field.parseField(rc.getMapping(index + "/_mapping"));
        rc.close();
        return parseField;
    }

    public static String get(String index) throws Exception {
        ExtendedRestClient rc = new ExtendedRestClient();
        String str = rc.get(index);
        rc.close();
        return str;
    }

    public static void putMapping(String index, String type, String location) throws Exception {
        putMapping(index, type, TestUtils.fromInputStream(RestUtils.class.getClassLoader().getResourceAsStream(location)));
    }

    /**
     * @deprecated use putMapping(index, type, location) instead
     */
    public static void putMapping(String index, String location) throws Exception {
        putMapping(index, TestUtils.fromInputStream(RestUtils.class.getClassLoader().getResourceAsStream(location)));
    }

    public static void postData(String index, String location) throws Exception {
        byte[] fromInputStream = TestUtils.fromInputStream(RestUtils.class.getClassLoader().getResourceAsStream(location));
        postData(index, fromInputStream);
    }

    public static void postData(String index, byte[] content) throws Exception {
        ExtendedRestClient rc = new ExtendedRestClient();
        rc.post(index, content);
        rc.close();
    }

    public static void put(String index, byte[] content) throws Exception {
        ExtendedRestClient rc = new ExtendedRestClient();
        rc.put(index, content);
        rc.close();
    }

    public static void delete(String index) throws Exception {
        ExtendedRestClient rc = new ExtendedRestClient();
        rc.delete(index);
        rc.close();
    }

    public static void bulkData(String index, String location) throws Exception {
        ExtendedRestClient rc = new ExtendedRestClient();
        rc.put(index + "/_bulk", TestUtils.fromInputStream(RestUtils.class.getClassLoader().getResourceAsStream(location)));
        rc.close();
    }

    public static void waitForYellow(String string) throws Exception {
        ExtendedRestClient rc = new ExtendedRestClient();
        rc.waitForHealth(string, Health.YELLOW, TimeValue.timeValueSeconds(5));
        rc.close();
    }

    public static void refresh(String string) throws Exception {
        ExtendedRestClient rc = new ExtendedRestClient();
        rc.refresh(string);
        rc.close();
    }

    public static boolean exists(String string) throws Exception {
        ExtendedRestClient rc = new ExtendedRestClient();
        boolean result;
        List<String> parts = StringUtils.tokenize(string, "/");
        if (parts.size() == 1) {
            result = rc.indexExists(string);
        } else if (parts.size() == 2) {
            result = rc.typeExists(parts.get(0), parts.get(1));
        } else if (parts.size() == 3) {
            result = rc.documentExists(parts.get(0), parts.get(1), parts.get(2));
        } else {
            throw new RuntimeException("Invalid exists path : " + string);
        }
        rc.close();
        return result;
    }

    public static boolean touch(String index) throws IOException {
        ExtendedRestClient rc = new ExtendedRestClient();
        boolean result = rc.touch(index);
        rc.close();
        return result;
    }
}
