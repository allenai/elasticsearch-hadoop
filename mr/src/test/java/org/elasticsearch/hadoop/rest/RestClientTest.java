package org.elasticsearch.hadoop.rest;

import org.elasticsearch.hadoop.cfg.ConfigurationOptions;
import org.elasticsearch.hadoop.util.ByteSequence;
import org.elasticsearch.hadoop.util.BytesArray;
import org.elasticsearch.hadoop.util.IOUtils;
import org.elasticsearch.hadoop.util.TestSettings;
import org.elasticsearch.hadoop.util.TrackingBytesArray;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.ws.Holder;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

public class RestClientTest {

    @Test
    public void testBatchWriteStatusIgnore() throws IOException {

        RestClient client = new RestClient(new TestSettings());

        // do an ok bulk request
        client.processBulkResponse(
            mockBulkResponse("bulk-status-ignore-rsp-200.json"),
            loadTestBytesArray("bulk-status-ignore-req.jsonl")
        );

        // simulate update on a missing document without the ignore setting
        try {
            client.processBulkResponse(
                mockBulkResponse("bulk-status-ignore-rsp-404.json"),
                loadTestBytesArray("bulk-status-ignore-req.jsonl")
            );
            Assert.fail("Normally 404 in a bulk requests should cause the client to abort.");
        } catch (EsHadoopInvalidRequest ignored) {
            // expected
        }

        // use the ignore setting to tolerate the previous situation
        client.close();
        TestSettings tolerantSettings = new TestSettings();
        // This test cares about 404 specifically,
        // but this works for other bulk item status codes too (comma-separated).
        tolerantSettings.setProperty(ConfigurationOptions.ES_BATCH_WRITE_STATUS_IGNORE, "404,503");
        client = new RestClient(tolerantSettings);
        client.processBulkResponse(
            mockBulkResponse("bulk-status-ignore-rsp-404.json"),
            loadTestBytesArray("bulk-status-ignore-req.jsonl")
        );
        // no exception
    }

    private Response mockBulkResponse(String path) throws IOException {
        InputStream data = getClass().getResource(path).openStream();
        // note bulk responses always return 200, the response contains results for each bulk item
        return new SimpleResponse(200, data, "127.0.0.1:9200");
    }

    private TrackingBytesArray loadTestBytesArray(String path) throws IOException {
        BytesArray bytes = IOUtils.asBytes(getClass().getResource(path).openStream());
        TrackingBytesArray tba = new TrackingBytesArray(new BytesArray(0));
        tba.copyFrom(bytes);
        return tba;
    }
}
