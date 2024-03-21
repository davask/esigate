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

package org.esigate.extension;

import java.util.Properties;

import org.esigate.Driver;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.FetchEvent;
import org.esigate.util.Parameter;
import org.esigate.util.ParameterString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This extension adds a X-Correlation-Id header.
 * 
 * @author Veekee
 * 
 */
public class XCorrelationId implements Extension, IEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(XCorrelationId.class);
    private static final String DEFAULT_CORRELATION_ID_HEADER = "X-CORRELATION-ID";

    public static final Parameter<String> CONFIG_CORRELATION_ID_HEADER = new ParameterString("correlationIdHeader",
            DEFAULT_CORRELATION_ID_HEADER);

    private String correlationIdHeader;

    @Override
    public void init(Driver driver, Properties properties) {
        driver.getEventManager().register(EventManager.EVENT_FETCH_PRE, this);
        correlationIdHeader = CONFIG_CORRELATION_ID_HEADER.getValue(properties);
    }

    @Override
    public boolean event(EventDefinition id, Event event) {

        FetchEvent e = (FetchEvent) event;
        if (e.getHttpRequest().getFirstHeader(correlationIdHeader) == null) {
            e.getHttpRequest().addHeader(correlationIdHeader, String.valueOf(java.util.UUID.randomUUID()));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Injecting correlation id '{}' into '{}' header",
                        e.getHttpRequest().getFirstHeader(correlationIdHeader).getValue(), correlationIdHeader);
            }
        }

        // Continue processing
        return true;
    }

}