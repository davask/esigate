package net.webassembletool.parser;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import net.webassembletool.HttpErrorPage;

public interface BodyTagElement extends Element {

	public void setRequest(HttpServletRequest request);

	public void doAfterBody(String body, Appendable out, ElementStack stack)
			throws IOException, HttpErrorPage;

}
