package org.esigate.http;

import java.net.URI;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedirectStrategy extends DefaultRedirectStrategy {
	private final static Logger LOG = LoggerFactory
			.getLogger(RedirectStrategy.class);

	private final static String LAST_REQUEST = "LAST_REQUEST";

	@Override
	public URI getLocationURI(HttpRequest request, HttpResponse response,
			HttpContext context) throws ProtocolException {
		URI redirectLocation = super.getLocationURI(request, response, context);
		if (LOG.isInfoEnabled()) {
			LOG.info(request.getRequestLine() + " -> "
					+ response.getStatusLine()
					+ " -> automaticaly following redirect to "
					+ redirectLocation.toString());
		}
		return redirectLocation;
	}

	@Override
	public HttpUriRequest getRedirect(HttpRequest request,
			HttpResponse response, HttpContext context)
			throws ProtocolException {
		HttpUriRequest newRequest = super.getRedirect(request, response,
				context);
		context.setAttribute(LAST_REQUEST, newRequest);
		return newRequest;
	}

	public final static HttpRequest getLastRequest(HttpContext context) {
		return (HttpRequest) context.getAttribute(LAST_REQUEST);
	}

}