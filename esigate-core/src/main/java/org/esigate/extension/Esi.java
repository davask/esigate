package org.esigate.extension;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import java.util.Properties;

import org.esigate.Driver;
import org.esigate.esi.EsiRenderer;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.RenderEvent;
import org.esigate.extension.surrogate.CapabilitiesEvent;
import org.esigate.extension.surrogate.Surrogate;

/**
 * This extension processes ESI directives. Ex:
 * <p>
 * &lt;esi:include src="$(PROVIDER{cms})/news" fragment="news_1"/&gt;
 * <p>
 * Surrogate-Control is supported, with the following behavior :
 * <ul>
 * <li>Exposed capabilities : "ESI/1.0", "ESI-Inline/1.0", "X-ESI-Fragment/1.0", "X-ESI-Replace/1.0", "X-ESI-XSLT/1.0",
 * "ESIGATE/4.0"</li>
 * <li>When one of these capabilities is requested, all are applied at once.</li>
 * </ul>
 * 
 * @author Nicolas Richeton
 */
public class Esi implements Extension, IEventListener {
    private static final String[] CAPABILITIES = new String[] {"ESI/1.0", "ESI-Inline/1.0", "X-ESI-Fragment/1.0",
            "X-ESI-Replace/1.0", "X-ESI-XSLT/1.0", "ESIGATE/4.0"};

    @Override
    public boolean event(EventDefinition id, Event event) {
        RenderEvent renderEvent = (RenderEvent) event;
        boolean doEsi = true;

        // ensure we should process esi
        if (renderEvent.getHttpResponse() != null
                && renderEvent.getHttpResponse().containsHeader(Surrogate.H_X_ENABLED_CAPABILITIES)) {
            String enabledCapabilities =
                    renderEvent.getHttpResponse().getFirstHeader(Surrogate.H_X_ENABLED_CAPABILITIES).getValue();

            doEsi = false;
            for (String capability : CAPABILITIES) {
                if (containsIgnoreCase(enabledCapabilities, capability)) {
                    doEsi = true;
                    break;
                }
            }
        }

        if (doEsi) {
            renderEvent.getRenderers().add(new EsiRenderer());
        }

        // Continue processing
        return true;
    }

    @Override
    public void init(Driver driver, Properties properties) {
        driver.getEventManager().register(EventManager.EVENT_RENDER_PRE, this);

        driver.getEventManager().register(Surrogate.EVENT_SURROGATE_CAPABILITIES, new IEventListener() {
            @Override
            public boolean event(EventDefinition id, Event event) {
                CapabilitiesEvent capEvent = (CapabilitiesEvent) event;
                for (String capability : CAPABILITIES) {
                    capEvent.getCapabilities().add(capability);
                }
                return true;
            }
        });
    }

}
