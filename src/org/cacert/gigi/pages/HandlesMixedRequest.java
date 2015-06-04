package org.cacert.gigi.pages;

import javax.servlet.http.HttpServletRequest;

/**
 * Marks a {@link Page} as beeing able to handle
 * {@link HttpServletRequest#getQueryString()} in
 * {@link HttpServletRequest#getMethod()}<code>== "POST"</code>
 */
public interface HandlesMixedRequest {

}
