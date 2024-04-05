package com.liferay.commerce.payment.method.amazon.internal.constants;

import com.liferay.commerce.payment.method.amazon.internal.connector.Environment;
import com.liferay.portal.kernel.util.StringUtil;

/**
 * @author Vy Bui
 */
public class AmazonCommercePaymentMethodConstants {

	public static final String[] ENVIRONMENTS = {
		StringUtil.toLowerCase(Environment.PROD.name()),
		StringUtil.toLowerCase(Environment.TEST.name())
	};

	public static final String SERVICE_NAME =
		"com.liferay.commerce.payment.method.amazon";

	public static final String SERVLET_PATH = "amazon-payment";

}