/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.esigate.http;

import java.io.IOException;
import java.util.Properties;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.execchain.ClientExecChain;
import org.esigate.HttpErrorPage;
import org.esigate.cache.CacheAdapter;
import org.esigate.events.EventManager;
import org.esigate.events.impl.FetchEvent;

public class ProxyingHttpClientBuilder extends CachingHttpClientBuilder {
    private Properties properties;
    private EventManager eventManager;
    private boolean useCache = true;

    @Override
    protected ClientExecChain decorateMainExec(ClientExecChain mainExec) {
        ClientExecChain result = mainExec;
        result = addFetchEvent(result);
        if (useCache) {
            CacheAdapter cacheAdapter = new CacheAdapter();
            cacheAdapter.init(properties);
            result = cacheAdapter.wrapBackendHttpClient(result);
            result = super.decorateMainExec(result);
            result = cacheAdapter.wrapCachingHttpClient(result);
        }
        return result;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public boolean isUseCache() {
        return useCache;
    }

    /**
     * Decorate with fetch event managements
     * 
     * @param wrapped
     * @return the decorated ClientExecChain
     */
    private ClientExecChain addFetchEvent(final ClientExecChain wrapped) {
        return new ClientExecChain() {

            @Override
            public CloseableHttpResponse execute(HttpRoute route, HttpRequestWrapper request,
                    HttpClientContext httpClientContext, HttpExecutionAware execAware) throws IOException,
                    HttpException {
                OutgoingRequestContext context = OutgoingRequestContext.adapt(httpClientContext);
                // Create request event
                FetchEvent fetchEvent = new FetchEvent(context, request);

                eventManager.fire(EventManager.EVENT_FETCH_PRE, fetchEvent);

                if (fetchEvent.isExit()) {
                    if (fetchEvent.getHttpResponse() == null) {
                        // Provide an error page in order to avoid a NullPointerException
                        fetchEvent.setHttpResponse(HttpErrorPage.generateHttpResponse(
                                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                "An extension stopped the processing of the request without providing a response"));
                    }
                } else {
                    try {
                        fetchEvent.setHttpResponse(wrapped.execute(route, request, context, execAware));
                        eventManager.fire(EventManager.EVENT_FETCH_POST, fetchEvent);
                    } catch (IOException | HttpException e) {
                        fetchEvent.setHttpResponse(HttpErrorPage.generateHttpResponse(e));
                        // Usually we want to render and cache the exception but we let an extension decide
                        fetchEvent.setExit(true);
                        eventManager.fire(EventManager.EVENT_FETCH_POST, fetchEvent);
                        if (!fetchEvent.isExit())
                            throw e; // Throw the exception and let http client process it (may retry)
                    }
                }

                return fetchEvent.getHttpResponse();
            }
        };

    }
}
