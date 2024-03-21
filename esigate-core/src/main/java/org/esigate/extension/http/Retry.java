package org.esigate.extension.http;

import java.util.Properties;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.esigate.Driver;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.FetchEvent;
import org.esigate.events.impl.HttpClientBuilderEvent;
import org.esigate.extension.Extension;
import org.esigate.util.Parameter;
import org.esigate.util.ParameterInteger;

/**
 * Enable Retry handler in http client.
 * 
 * @author Nicolas Richeton
 * 
 */
public class Retry implements Extension, IEventListener {
    int maxRetries;
    public static final Parameter<Integer> NB_RETRY = new ParameterInteger("http.retryCount", 3);

    public boolean event(EventDefinition id, Event event) {
        if (EventManager.EVENT_HTTP_BUILDER_INITIALIZATION.equals(id)) {
            HttpClientBuilderEvent e = (HttpClientBuilderEvent) event;
            // Register retry handler
            e.getHttpClientBuilder().setRetryHandler(new DefaultHttpRequestRetryHandler(maxRetries, true));
        } else if (EventManager.EVENT_FETCH_PRE.equals(id)) {
            FetchEvent e = (FetchEvent) event;
            // Count attempts
            int attemptNumber = getAttemptNumber(e);
            setAttemptNumber(attemptNumber + 1, e);
            e.getHttpRequest().addHeader("org.esigate.http.attempt", String.valueOf(getAttemptNumber(e)));
        } else if (EventManager.EVENT_FETCH_POST.equals(id)) {
            FetchEvent e = (FetchEvent) event;
            int attemptNumber = getAttemptNumber(e);
            if (attemptNumber <= maxRetries)
                e.setExit(false); // let's retry in case it failed
        }

        return true;
    }

    public void init(Driver driver, Properties properties) {
        driver.getEventManager().register(EventManager.EVENT_HTTP_BUILDER_INITIALIZATION, this);
        driver.getEventManager().register(EventManager.EVENT_FETCH_PRE, this);
        driver.getEventManager().register(EventManager.EVENT_FETCH_POST, this);

        // load configuration
        maxRetries = NB_RETRY.getValue(properties);
    }

    private final static String RETRY_KEY = "RetryExtension.attemptNumber";

    private int getAttemptNumber(FetchEvent e) {
        Object number = e.getHttpContext().getAttribute(RETRY_KEY);
        return number == null ? 0 : (Integer) number;
    }

    private void setAttemptNumber(int number, FetchEvent e) {
        e.getHttpContext().setAttribute(RETRY_KEY, number);
    }
}
