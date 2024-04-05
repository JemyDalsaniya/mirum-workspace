package com.liferay.commerce.payment.method.amazon.internal.servlet.filter;

import com.liferay.commerce.payment.method.amazon.internal.constants.AmazonCommercePaymentMethodConstants;
import com.liferay.portal.servlet.filters.authverifier.AuthVerifierFilter;

import javax.servlet.Filter;

import org.osgi.service.component.annotations.Component;

/**
 * @author Vy Bui
 */
@Component(
	property = {
		"filter.init.auth.verifier.PortalSessionAuthVerifier.urls.includes=/" + AmazonCommercePaymentMethodConstants.SERVLET_PATH + "/*",
		"osgi.http.whiteboard.filter.name=com.liferay.commerce.payment.method.amazon.internal.servlet.filter.CommercePaymentMethodAmazonAuthVerifierFilter",
		"osgi.http.whiteboard.servlet.pattern=/" + AmazonCommercePaymentMethodConstants.SERVLET_PATH + "/*"
	},
	service = Filter.class
)
public class CommercePaymentMethodAmazonAuthVerifierFilter
	extends AuthVerifierFilter {
}