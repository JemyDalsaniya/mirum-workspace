package com.liferay.commerce.payment.method.amazon.internal;

import com.liferay.commerce.constants.CommerceOrderConstants;
import com.liferay.commerce.constants.CommerceOrderPaymentConstants;
import com.liferay.commerce.constants.CommercePaymentConstants;
import com.liferay.commerce.currency.model.CommerceCurrency;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.payment.method.CommercePaymentMethod;
import com.liferay.commerce.payment.method.amazon.internal.configuration.AmazonGroupServiceConfiguration;
import com.liferay.commerce.payment.method.amazon.internal.connector.Environment;
import com.liferay.commerce.payment.method.amazon.internal.connector.PaypageClient;
import com.liferay.commerce.payment.method.amazon.internal.constants.AmazonCommercePaymentMethodConstants;
import com.liferay.commerce.payment.request.CommercePaymentRequest;
import com.liferay.commerce.payment.result.CommercePaymentResult;
import com.liferay.commerce.service.CommerceOrderService;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.settings.GroupServiceSettingsLocator;
import com.liferay.portal.kernel.util.HttpComponentsUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.URLCodec;
import com.liferay.portal.kernel.uuid.PortalUUID;
import com.worldline.sips.model.CaptureMode;
import com.worldline.sips.model.Currency;
import com.worldline.sips.model.InitializationResponse;
import com.worldline.sips.model.OrderChannel;
import com.worldline.sips.model.PaymentRequest;
import com.worldline.sips.model.RedirectionStatusCode;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "commerce.payment.engine.method.key=" + AmazonCommercePaymentMethod.KEY,
	service = CommercePaymentMethod.class
)
public class AmazonCommercePaymentMethod implements CommercePaymentMethod {

	public static final String KEY = "amazon-system";

	@Override
	public CommercePaymentResult cancelPayment(
			CommercePaymentRequest commercePaymentRequest)
			throws Exception {

		AmazonCommercePaymentRequest amazonCommercePaymentRequest =
				(AmazonCommercePaymentRequest)commercePaymentRequest;

		return new CommercePaymentResult(
				amazonCommercePaymentRequest.getTransactionId(),
				amazonCommercePaymentRequest.getCommerceOrderId(),
				CommerceOrderPaymentConstants.STATUS_CANCELLED, false, null, null,
				Collections.emptyList(), true);
	}
	@Override
	public CommercePaymentResult completePayment(
			CommercePaymentRequest commercePaymentRequest)
		throws Exception {

		AmazonCommercePaymentRequest amazonCommercePaymentRequest =
				(AmazonCommercePaymentRequest)commercePaymentRequest;

		return new CommercePaymentResult(
				amazonCommercePaymentRequest.getTransactionId(),
				amazonCommercePaymentRequest.getCommerceOrderId(),
				CommerceOrderPaymentConstants.STATUS_COMPLETED, false, null, null,
				Collections.emptyList(), true);
	}

	@Override
	public String getDescription(Locale locale) {
		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", locale, getClass());

		return LanguageUtil.get(
			resourceBundle, "pay-via-amazon-commerce-payment-method");
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName(Locale locale) {
		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", locale, getClass());

		return LanguageUtil.get(
			resourceBundle, "amazon-commerce-payment-method");
	}

	@Override
	public int getPaymentType() {
		return CommercePaymentConstants.COMMERCE_PAYMENT_METHOD_TYPE_ONLINE_REDIRECT;
	}

	@Override
	public String getServletPath() {
		return AmazonCommercePaymentMethodConstants.SERVLET_PATH;
	}

	@Override
	public boolean isCancelEnabled() {
		return true;
	}

	@Override
	public boolean isCompleteEnabled() {
		return true;
	}

	@Override
	public boolean isProcessPaymentEnabled() {
		return true;
	}

	@Override
	public CommercePaymentResult processPayment(
			CommercePaymentRequest commercePaymentRequest)
		throws Exception {

		AmazonCommercePaymentRequest amazonCommercePaymentRequest =
				(AmazonCommercePaymentRequest)commercePaymentRequest;

		CommerceOrder commerceOrder = _commerceOrderService.getCommerceOrder(
				amazonCommercePaymentRequest.getCommerceOrderId());

		CommerceCurrency commerceCurrency = commerceOrder.getCommerceCurrency();

//		if (!Objects.equals(commerceCurrency.getCode(), "EUR")) {
//			throw new Exception("Amazon accepts only EUR currency");
//		}

		PaymentRequest paymentRequest = new PaymentRequest();

		int normalizedMultiplier = (int)Math.pow(
				10, commerceCurrency.getMaxFractionDigits());

		BigDecimal orderTotal = commerceOrder.getTotal();

		BigDecimal normalizedOrderTotal = orderTotal.multiply(
				new BigDecimal(normalizedMultiplier));

		paymentRequest.setAmount(normalizedOrderTotal.intValue());

		URL returnURL = new URL(amazonCommercePaymentRequest.getReturnUrl());

		Map<String, String[]> parameters = HttpComponentsUtil.getParameterMap(
				returnURL.getQuery());

		URL baseURL = new URL(
				returnURL.getProtocol(), returnURL.getHost(), returnURL.getPort(),
				returnURL.getPath());

		URL automaticURL = new URL(
				StringBundler.concat(
						baseURL.toString(), "?groupId=", parameters.get("groupId")[0],
						"&type=automatic&uuid=", parameters.get("uuid")[0]));

		paymentRequest.setAutomaticResponseUrl(automaticURL);

		paymentRequest.setCaptureMode(CaptureMode.IMMEDIATE);
		paymentRequest.setCurrencyCode(Currency.EUR);
		paymentRequest.setCustomerId(String.valueOf(commerceOrder.getUserId()));

		StringBundler normalURLSB = new StringBundler(4);

		normalURLSB.append(baseURL.toString());
		normalURLSB.append("?redirect=");

		String encodeURL = URLCodec.encodeURL(parameters.get("redirect")[0]);

		normalURLSB.append(encodeURL);

		normalURLSB.append("&type=normal");

		URL normalURL = new URL(normalURLSB.toString());

		paymentRequest.setNormalReturnUrl(normalURL);

		paymentRequest.setOrderChannel(OrderChannel.INTERNET);
		paymentRequest.setOrderId(
				String.valueOf(commerceOrder.getCommerceOrderId()));

		String transactionUuid = _portalUUID.generate();

		String transactionId = StringUtil.replace(
				transactionUuid, CharPool.DASH, StringPool.BLANK);

		paymentRequest.setTransactionReference(transactionId);

		AmazonGroupServiceConfiguration amazonGroupServiceConfiguration =
				_getConfiguration(commerceOrder.getGroupId());

		String environment = StringUtil.toUpperCase(
				amazonGroupServiceConfiguration.environment());

		String keyVersion = amazonGroupServiceConfiguration.keyVersion();

		PaypageClient paypageClient = new PaypageClient(
				Environment.valueOf(environment),
				amazonGroupServiceConfiguration.amazonId(),
				amazonGroupServiceConfiguration.secretKey());

		InitializationResponse initializationResponse =
				paypageClient.initialize(paymentRequest);

		List<String> resultMessage = Collections.singletonList(
				initializationResponse.getRedirectionStatusMessage());

		RedirectionStatusCode responseCode =
				initializationResponse.getRedirectionStatusCode();

		if (!Objects.equals(responseCode.getCode(), "00")) {
			return new CommercePaymentResult(
					transactionId, commerceOrder.getCommerceOrderId(),
					CommerceOrderPaymentConstants.STATUS_FAILED, true, null, null,
					resultMessage, false);
		}

		String url = StringBundler.concat(
				_getServletUrl(amazonCommercePaymentRequest), "?redirectURL=",
				URLCodec.encodeURL(
						String.valueOf(initializationResponse.getRedirectionUrl())),
				"&redirectionData=",
				URLEncoder.encode(
						initializationResponse.getRedirectionData(), StringPool.UTF8),
				"&seal=",
				URLEncoder.encode(
						initializationResponse.getSeal(), StringPool.UTF8));

		return new CommercePaymentResult(
				transactionId, commerceOrder.getCommerceOrderId(),
				CommerceOrderConstants.PAYMENT_STATUS_AUTHORIZED, true, url, null,
				resultMessage, true);
	}

	private AmazonGroupServiceConfiguration _getConfiguration(long groupId)
			throws Exception {

		return _configurationProvider.getConfiguration(
				AmazonGroupServiceConfiguration.class,
				new GroupServiceSettingsLocator(
						groupId, AmazonCommercePaymentMethodConstants.SERVICE_NAME));
	}

	private ResourceBundle _getResourceBundle(Locale locale) {
		return ResourceBundleUtil.getBundle(
				"content.Language", locale, getClass());
	}
	private String _getServletUrl(
			AmazonCommercePaymentRequest amazonCommercePaymentRequest) {

		return StringBundler.concat(
				_portal.getPortalURL(
						amazonCommercePaymentRequest.getHttpServletRequest()),
				_portal.getPathModule(), StringPool.SLASH,
				AmazonCommercePaymentMethodConstants.SERVLET_PATH);
	}
	
	@Reference
	private CommerceOrderService _commerceOrderService;

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private Language _language;

	@Reference
	private Portal _portal;

	@Reference
	private PortalUUID _portalUUID;
}