package com.liferay.commerce.payment.method.amazon.internal.portlet.action;

import com.liferay.commerce.payment.method.amazon.internal.constants.AmazonCommercePaymentMethodConstants;
import com.liferay.commerce.product.constants.CPPortletKeys;
import com.liferay.commerce.product.model.CommerceChannel;
import com.liferay.commerce.product.service.CommerceChannelService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.settings.GroupServiceSettingsLocator;
import com.liferay.portal.kernel.settings.ModifiableSettings;
import com.liferay.portal.kernel.settings.Settings;
import com.liferay.portal.kernel.settings.SettingsFactory;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.ValidatorException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Vy Bui
 */
@Component(
	property = {
		"javax.portlet.name=" + CPPortletKeys.COMMERCE_PAYMENT_METHODS,
		"mvc.command.name=/commerce_payment_methods/edit_amazon_commerce_payment_method_configuration"
	},
	service = MVCActionCommand.class
)
public class EditAmazonCommercePaymentMethodConfigurationMVCActionCommand
	extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		if (cmd.equals(Constants.UPDATE)) {
			_updateCommercePaymentMethod(actionRequest);
		}
	}

	private void _updateCommercePaymentMethod(ActionRequest actionRequest)
		throws IOException, PortalException, ValidatorException {

		long commerceChannelId = ParamUtil.getLong(
			actionRequest, "commerceChannelId");

		CommerceChannel commerceChannel =
			_commerceChannelService.getCommerceChannel(commerceChannelId);

		Settings settings = _settingsFactory.getSettings(
			new GroupServiceSettingsLocator(
				commerceChannel.getGroupId(),
				AmazonCommercePaymentMethodConstants.SERVICE_NAME));

		ModifiableSettings modifiableSettings =
			settings.getModifiableSettings();

		String apiLoginId = ParamUtil.getString(
			actionRequest, "settings--merchantId--");

		modifiableSettings.setValue("merchantId", apiLoginId);

		String environment = ParamUtil.getString(
			actionRequest, "settings--environment--");

		modifiableSettings.setValue("environment", environment);

		String transactionKey = ParamUtil.getString(
			actionRequest, "settings--secretKey--");

		modifiableSettings.setValue("secretKey", transactionKey);

		String keyVersion = ParamUtil.getString(
			actionRequest, "settings--keyVersion--");

		modifiableSettings.setValue("keyVersion", keyVersion);

		modifiableSettings.store();
	}

	@Reference
	private CommerceChannelService _commerceChannelService;

	@Reference
	private SettingsFactory _settingsFactory;

}