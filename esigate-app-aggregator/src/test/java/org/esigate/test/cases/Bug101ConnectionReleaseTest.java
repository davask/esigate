package org.esigate.test.cases;

import java.io.IOException;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.esigate.Driver;
import org.esigate.HttpErrorPage;
import org.esigate.Parameters;
import org.esigate.http.HttpResponseUtils;
import org.esigate.http.IncomingRequest;
import org.esigate.tags.BlockRenderer;
import org.esigate.test.TestUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bug101ConnectionReleaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(Bug101ConnectionReleaseTest.class);

    private void render(Driver driver, String page) throws IOException {
        StringBuilderWriter writer = new StringBuilderWriter();
        IncomingRequest httpRequest = TestUtils.createIncomingRequest().build();
        try {
            CloseableHttpResponse response =
                    driver.render("/esigate-app-aggregated1/" + page, httpRequest, new BlockRenderer(null,
                            "/esigate-app-aggregated1/" + page));
            writer.append(HttpResponseUtils.toString(response));
            writer.close();
        } catch (HttpErrorPage e) {
            LOG.info(page + " -> " + e.getHttpResponse().getStatusLine().getStatusCode());
        }
    }

    /**
     * This method while return immediately if no connections are leaked of will return after 20 or 30 seconds, (pool
     * timeout)
     * 
     * @throws IOException
     */
    @Test
    public void testConnectionLeak() throws IOException {
        Properties properties = new Properties();
        properties.put(Parameters.MAX_CONNECTIONS_PER_HOST.getName(), "1");
        properties.put(Parameters.REMOTE_URL_BASE.getName(), "http://localhost:8080/");
        properties.put(Parameters.SOCKET_TIMEOUT.getName(), "4000");
        properties.put(Parameters.USE_CACHE.getName(), "false");

        Driver driver = Driver.builder().setName("test").setProperties(properties).build();

        // 2 first calls / do not measure first requests which can be slow
        render(driver, "utf8.jsp");
        render(driver, "utf8.jsp");

        long start = System.currentTimeMillis();
        // Should take less than 500ms each
        render(driver, "error500.jsp");
        render(driver, "error500.jsp");
        render(driver, "error404");
        render(driver, "error404");
        // Should take 4000ms each
        render(driver, "slow.jsp");
        render(driver, "slow.jsp");
        // Should take less than 500ms each
        render(driver, "");
        render(driver, "");
        Assert.assertTrue("Connection pool timeout : ressource leaked", System.currentTimeMillis() - start < 11000);
    }

}
