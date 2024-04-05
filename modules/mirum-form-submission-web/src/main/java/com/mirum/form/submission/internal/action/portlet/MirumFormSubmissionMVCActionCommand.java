package com.mirum.form.submission.internal.action.portlet;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.service.AssetEntryLocalServiceUtil;
import com.liferay.captcha.util.CaptchaUtil;
import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.account.service.CommerceAccountLocalServiceUtil;
import com.liferay.commerce.context.CommerceContext;
import com.liferay.commerce.context.CommerceContextFactory;
import com.liferay.commerce.currency.model.CommerceCurrency;
import com.liferay.commerce.currency.service.CommerceCurrencyLocalServiceUtil;
import com.liferay.commerce.exception.CommerceOrderAccountLimitException;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.product.model.*;
import com.liferay.commerce.product.service.CPDefinitionLocalServiceUtil;
import com.liferay.commerce.product.service.CPInstanceLocalServiceUtil;
import com.liferay.commerce.product.service.CommerceChannelLocalService;
import com.liferay.commerce.product.service.CommerceChannelLocalServiceUtil;
import com.liferay.commerce.service.CommerceOrderItemLocalServiceUtil;
import com.liferay.commerce.service.CommerceOrderService;
import com.liferay.commerce.service.CommerceOrderTypeService;
import com.liferay.dynamic.data.mapping.constants.DDMPortletKeys;
import com.liferay.dynamic.data.mapping.exception.FormInstanceNotPublishedException;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldTypeServicesTracker;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldValueRenderer;
import com.liferay.dynamic.data.mapping.form.values.factory.DDMFormValuesFactory;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMFormInstance;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecord;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecordVersion;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceSettings;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceVersion;
import com.liferay.dynamic.data.mapping.model.DDMFormSuccessPageSettings;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMStructureVersion;
import com.liferay.dynamic.data.mapping.model.LocalizedValue;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceRecordService;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceRecordVersionLocalService;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceService;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceVersionLocalService;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.util.comparator.FormInstanceVersionVersionComparator;
import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.UserEmailAddressException;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Html;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;


import com.mirum.form.submission.internal.action.constants.DDMFormWebKeys;
import com.mirum.form.submission.internal.action.exceptions.MembershipCheckException;
import com.mirum.form.submission.internal.action.exceptions.OTPCheckException;
import com.mirum.form.submission.internal.action.lifecycle.AddDefaultSharedFormLayoutPortalInstanceLifecycleListener;
import com.mirum.form.submission.internal.action.util.MirumHTTPInvoke;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashMap;

import java.io.Serializable;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


/**
 * @author Vy
 */
@Component(
        immediate = true,
        property = {
                "javax.portlet.name=" + DDMPortletKeys.DYNAMIC_DATA_MAPPING_FORM,
                "javax.portlet.name=" + DDMPortletKeys.DYNAMIC_DATA_MAPPING_FORM_ADMIN,
                "mvc.command.name=/dynamic_data_mapping_form/add_form_instance_record",
                "service.ranking:Integer=100",
                "javax.portlet.security-role-ref=guest,administrator"
        },
        service = MVCActionCommand.class
)
public class MirumFormSubmissionMVCActionCommand extends BaseMVCActionCommand {

    @Override
    protected void doProcessAction(
            ActionRequest actionRequest, ActionResponse actionResponse)
            throws Exception {

        PortletSession portletSession = actionRequest.getPortletSession();

        long groupId = ParamUtil.getLong(actionRequest, "groupId");

        if (groupId == 0) {
            groupId = GetterUtil.getLong(
                    portletSession.getAttribute(DDMFormWebKeys.GROUP_ID));
        }

        ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(
                WebKeys.THEME_DISPLAY);

        long userId = themeDisplay.getUserId();

        long formInstanceId = ParamUtil.getLong(
                actionRequest, "formInstanceId");

        if (formInstanceId == 0) {
            formInstanceId = GetterUtil.getLong(
                    portletSession.getAttribute(
                            DDMFormWebKeys.DYNAMIC_DATA_MAPPING_FORM_INSTANCE_ID));
        }

        DDMFormInstance ddmFormInstance =
                _ddmFormInstanceService.getFormInstance(formInstanceId);

        _addFormInstanceMVCCommandHelper.validateExpirationStatus(
                ddmFormInstance, actionRequest);
        _addFormInstanceMVCCommandHelper.validateSubmissionLimitStatus(
                ddmFormInstance, _ddmFormInstanceRecordVersionLocalService,
                actionRequest);

        _validatePublishStatus(actionRequest, ddmFormInstance);

        _validateCaptcha(actionRequest, ddmFormInstance);

        DDMForm ddmForm = getDDMForm(ddmFormInstance);

        DDMFormValues ddmFormValues = _ddmFormValuesFactory.create(
                actionRequest, ddmForm);



        User _user = UserLocalServiceUtil.fetchUser(themeDisplay.getUserId());

        ServiceContext serviceContext = ServiceContextFactory.getInstance(
                DDMFormInstanceRecord.class.getName(), actionRequest);


        //custom code start
        String finalQuantity = null;
        String product = _getByFieldReference(ddmFormValues, formInstanceId, "product");
        _log.info("Product = " + product);
        String productQuantity = _getByFieldReference(ddmFormValues, formInstanceId, "quantity");
        _log.info("Quantity = " + productQuantity);
        String primaryProduct = _getByFieldReference(ddmFormValues, formInstanceId, "primaryProduct");
        _log.info("Primary Product = " + primaryProduct);
        String primaryQuantity = _getByFieldReference(ddmFormValues, formInstanceId, "primaryQuantity");
        _log.info("Primary Quantity = " + primaryQuantity);
        String specialQuantity = _getByFieldReference(ddmFormValues, formInstanceId, "secondaryServiceQuantity");
        _log.info("Secondary Quantity = " + specialQuantity);
        String isOfflinePaymentValue = _getByFieldReference(ddmFormValues, formInstanceId, "isOfflinePayment");
        _log.info("isOfflinePayment is : " + isOfflinePaymentValue);
        if(Validator.isNotNull(productQuantity)){
            finalQuantity = productQuantity;
        }else if(Validator.isNotNull(primaryQuantity)){
            finalQuantity = primaryQuantity;
        } else {
            finalQuantity = "1";
        }

        String currentLanguage = ParamUtil.getString(actionRequest, "languageId");
        if(Validator.isNull(currentLanguage)){
            String[] values = ddmFormInstance.getAvailableLanguageIds();
            for (String value : values){
                String fieldValue = ddmFormValues.getDDMFormFieldValues().get(0).getValue().getValues().get(LocaleUtil.fromLanguageId(value));
                if(Validator.isNotNull(fieldValue) && !fieldValue.equals("[]")){
                    currentLanguage = value;
                }
            }
        }

        String[] productType = new String[]{primaryProduct};
        List<String>    productList = null;

        if (Validator.isNull(primaryProduct)) {
            productType = new String[]{product};
        }

        String externalReferenceCode = null;
        String sku = null;
        if (Validator.isNull(productType[0])) {
            externalReferenceCode = _extractByField(
                    actionRequest, ddmFormValues, formInstanceId, "hiddenProduct");
            sku = _extractByField(
                    actionRequest, ddmFormValues, formInstanceId, "sku");
            if (_log.isDebugEnabled()) {
                _log.debug("externalReferenceCode: " + externalReferenceCode);
                _log.debug("getting sku: " + sku);
            }
        }

        List<Object[]> cpDefinitions = null;
        CPInstance cpInstanceDynamic = null;
        if ((Validator.isNotNull(productQuantity) || Validator.isNotNull(primaryQuantity) || Validator.isNotNull(finalQuantity)) && Validator.isNotNull(productType[0]) && Validator.isNotNull(currentLanguage)) {
            cpDefinitions = getCPDefinitionFromDSL(currentLanguage, productType, cpDefinitions);
            String myExternalReferenceCode = null;

            if (Validator.isNull(externalReferenceCode) && Validator.isNotNull(cpDefinitions) && !cpDefinitions.isEmpty()) {
                for (Object[] cpDefinition : cpDefinitions) {
                    myExternalReferenceCode = String.valueOf(cpDefinition[0]);
                    externalReferenceCode = myExternalReferenceCode;
                    _log.info("myExternalReferenceCode : " + myExternalReferenceCode);
                }
            }
        }
        //custom code end

        //expando logic starts
        if (Validator.isNotNull(cpDefinitions)) {
            for (Object[] cpDefinition : cpDefinitions) {
                ExpandoBridge expandoBridgeForProduct = CPDefinitionLocalServiceUtil.getCPDefinition((long) cpDefinition[0]).getExpandoBridge();
                if (Validator.isNotNull(expandoBridgeForProduct) && expandoBridgeForProduct.hasAttribute(EXPANDO_SECONDARY_SERVICE) && expandoBridgeForProduct.getAttribute(EXPANDO_SECONDARY_SERVICE).equals(true)) {
                    String multipleProduct = _getByFieldReference(ddmFormValues, formInstanceId, "secondaryProduct");
                    String[] multipleProductArray = multipleProduct.split(", ");
                    productList = new ArrayList<>(Arrays.asList(productType));
                    if (Validator.isNotNull(productList)) {
                        productList.addAll(Arrays.asList(multipleProductArray));
//                        productType = productList.toArray(new String[0]);
                        productType = productList.stream()
                                .map(String::trim)
                                .toArray(String[]::new);
                    }

                    _log.info("productType : " + Arrays.toString(productType));
                }
            }
        }
        if (Validator.isNotNull(primaryQuantity) && Validator.isNotNull(productType[0]) && Validator.isNotNull(currentLanguage)) {
            cpDefinitions = getCPDefinitionFromDSL(currentLanguage, productType, cpDefinitions);
        }
        //expando logic ends


        if (Validator.isNotNull(externalReferenceCode)) {
            CommerceChannel commerceChannel =
                    CommerceChannelLocalServiceUtil.fetchCommerceChannelBySiteGroupId(
                            groupId);

            if (commerceChannel == null) {
                if (_log.isErrorEnabled()) {
                    _log.error("Could not find channel for groupId: " + groupId);
                }
            }

            CommerceCurrency commerceCurrency =
                    CommerceCurrencyLocalServiceUtil.fetchPrimaryCommerceCurrency(
                            themeDisplay.getCompanyId());

            if (commerceCurrency == null) {
                if (_log.isErrorEnabled()) {
                    _log.error("Could not find currency for groupId: " + groupId);
                }
            }

            CommerceOrder mirumOrder = null;

            CommerceAccount account = null;
            try {
                account =
                        CommerceAccountLocalServiceUtil.getPersonalCommerceAccount(
                                _user.getUserId());

                _log.info("account: " + account.getName());

            } catch (PortalException pe) {
                if (_log.isErrorEnabled()) {
                    _log.error("Could not find commerce account for username: " + _user.getFullName() + ". Error due to: " + pe.getMessage());
                }
            }

            String membershipId = _getByFieldReference(ddmFormValues, formInstanceId, "membershipId");
            _log.info("form Instance Id : " + formInstanceId);
            _log.info("membership id :" + membershipId);
            if (!membershipId.isEmpty()) {
                if (_log.isInfoEnabled()) {
                    _log.error("validate membership");
                }
                if (!_validateMembership(
                        membershipId)) {
                    _log.error("Membership is not valid!");
                    SessionErrors.add(actionRequest, MembershipCheckException.class);
                    return;
                }
            }


            try {
                mirumOrder = _commerceOrderService.addCommerceOrder(
                        commerceChannel.getGroupId(), account.getCommerceAccountId(),
                        commerceCurrency.getCommerceCurrencyId(), 0);

                mirumOrder.setShippingAddressId(
                        account.getDefaultShippingAddressId());
                mirumOrder.setBillingAddressId(
                        account.getDefaultShippingAddressId());

                //mirumOrder = _commerceOrderService.updateCommerceOrder(mirumOrder);

                String formName = ddmFormInstance.getName(Locale.getDefault());
                _log.info("my form name: " + ddmFormInstance.getName(Locale.getDefault()));
                ExpandoBridge expandoBridge = mirumOrder.getExpandoBridge();
                if (Validator.isNotNull(expandoBridge) && expandoBridge.hasAttribute("orderName") && expandoBridge.hasAttribute("isOfflinePayment") && expandoBridge.hasAttribute("offlinePaymentDetails")) {
                    expandoBridge.setAttribute("orderName", formName);
                    switch (isOfflinePaymentValue) {
                        case "true":
                            expandoBridge.setAttribute("isOfflinePayment", "true");
                            String offlinePaymentDetailsEnglish = _getByFieldReference(ddmFormValues, formInstanceId, "offlinePaymentDetails", LocaleUtil.fromLanguageId("en_US"));
                            String offlinePaymentDetailsArabic = _getByFieldReference(ddmFormValues, formInstanceId, "offlinePaymentDetails", LocaleUtil.fromLanguageId("ar_SA"));
                            Map<Locale, String> localeMap = new HashMap<Locale, String>();
                            localeMap.put(com.liferay.portal.kernel.util.LocaleUtil.fromLanguageId("en_US"), offlinePaymentDetailsEnglish);
                            localeMap.put(com.liferay.portal.kernel.util.LocaleUtil.fromLanguageId("ar_SA"), offlinePaymentDetailsArabic);
                            expandoBridge.setAttribute("offlinePaymentDetails", (Serializable) localeMap);
                            break;
                        case "hide":
                            expandoBridge.setAttribute("isOfflinePayment", "hide");
                            break;
                        default:
                            expandoBridge.setAttribute("isOfflinePayment", "false");
                    }
                    _log.debug("mirum order exapndo bridge value :" + mirumOrder.getExpandoBridge().getAttributes());
                }

                mirumOrder = _commerceOrderService.updateCommerceOrder(mirumOrder);

                _log.info("order: " + mirumOrder);
            } catch (Exception exception) {
                if (exception instanceof CommerceOrderAccountLimitException) {
                    hideDefaultErrorMessage(actionRequest);

                    SessionErrors.add(actionRequest, exception.getClass());
                }

                if (_log.isErrorEnabled()) {
                    _log.error(
                            "Could not create order for accountId:  " +
                                    account.getCommerceAccountId());
                }

                throw exception;
            }

            CommerceContext commerceContext = _commerceContextFactory.create(
                    themeDisplay.getCompanyId(),
                    _commerceChannelLocalService.getCommerceChannelGroupIdBySiteGroupId(
                            groupId),
                    themeDisplay.getUserId(), mirumOrder.getCommerceOrderId(),
                    account.getCommerceAccountId());

            _log.info("commerce context");
            serviceContext.setCompanyId(themeDisplay.getCompanyId());
            serviceContext.setScopeGroupId(groupId);
            serviceContext.setUserId(themeDisplay.getUserId());
            try {
                CPInstance cpInstance = null;
                int quantity = 0;
                if (Validator.isNotNull(sku)) {
                    cpInstance = CPInstanceLocalServiceUtil.getCPInstance(Long.parseLong(externalReferenceCode), sku);
                    quantity = 1;
                    if (Validator.isNotNull(quantity) && Validator.isNotNull(cpInstance))
                        CommerceOrderItemLocalServiceUtil.addCommerceOrderItem(
                                mirumOrder.getCommerceOrderId(), cpInstance.getCPInstanceId(),
                                "[]", quantity, 1, commerceContext, serviceContext);

                } else {
                    if (Validator.isNotNull(cpDefinitions)) {
                        long cpDefinitionId = 0;
                        for (Object[] cpDefinition : cpDefinitions) {
                            cpDefinitionId = (long) cpDefinition[0];
                            _log.debug("cpDefinitionId: " + cpDefinitionId);
                            _log.debug("cpDefinition: " + Arrays.toString(cpDefinition));
                            cpInstanceDynamic = CPInstanceLocalServiceUtil.getCPDefinitionApprovedCPInstances(cpDefinitionId).get(0);
                            _log.debug("cpInstanceDynamic in loop: " + cpInstanceDynamic);
                            if (Validator.isNotNull(cpInstanceDynamic)) {
                                cpInstance = CPInstanceLocalServiceUtil.getCPInstance(cpDefinitionId, cpInstanceDynamic.getSku());
                                _log.debug("cpInstance" + cpInstance);
                                if (Validator.isNumber(finalQuantity))
                                    quantity = new Integer(finalQuantity);
                            }
                            ExpandoBridge expandoBridge = CPDefinitionLocalServiceUtil.getCPDefinition(cpDefinitionId).getExpandoBridge();
                            if (Validator.isNotNull(expandoBridge) && expandoBridge.hasAttribute(EXPANDO_HAS_POINT) && expandoBridge.getAttribute(EXPANDO_HAS_POINT).equals(true)) {
                                quantity = new Integer(specialQuantity);
                                quantity = quantity * new Integer(finalQuantity);
                                _log.info("new updated quantity :" + quantity);
                            }
                            if (Validator.isNotNull(quantity) && Validator.isNotNull(cpInstance)) {

                                _log.info("Quantity = " + quantity);
                                _log.info("Cp Instance = " + cpInstance.getCPInstanceId());
                                try {
                                    CommerceOrderItemLocalServiceUtil.addCommerceOrderItem(
                                            mirumOrder.getCommerceOrderId(), cpInstance.getCPInstanceId(),
                                            "[]", quantity, 1, commerceContext, serviceContext);
                                } catch (Exception e) {

                                    _log.error("Exception Occure while adding commerce order  = " + e);
                                }
                            }
                        }
                    }
                }
            } catch (PortalException pe) {
                if (_log.isErrorEnabled()) {
                    _log.error(
                            String.format(
                                    "Cannot add commerce order item with orderId: %s, and commerce account Id: %s " + "exception : " + pe,
                                    mirumOrder.getCommerceOrderId(),
                                    commerceContext.getCommerceAccount(
                                    ).getCommerceAccountId()));
                }
            }

            List<DDMFormFieldValue> ddmFormFieldValuesOri =
                    ddmFormValues.getDDMFormFieldValues();

            for (int index = 0;
                 index < ddmFormValues.getDDMFormFieldValues(
                 ).size(); index++) {

                DDMFormFieldValue orderId = ddmFormFieldValuesOri.get(index);

                if (orderId.getFieldReference(
                ).equals(
                        "orderId"
                )) {

//                        LocalizedValue localizedValue = new LocalizedValue(themeDisplay.getLocale());
//                    LocalizedValue localizedValue = new LocalizedValue(LocaleUtil.getDefault());
                    LocalizedValue localizedValue = new LocalizedValue(LocaleUtil.fromLanguageId(currentLanguage));

                    _log.info("locale default: " + LocaleUtil.getDefault());
                    _log.info("localized value for erc: " + localizedValue);

                    for (Locale availableLocale : ddmForm.getAvailableLocales()) {
                        _log.info("availableLocale for erc: " + availableLocale);
                        localizedValue.addString(
                                availableLocale, mirumOrder.getExternalReferenceCode());
                    }

                    orderId.setValue(localizedValue);

                    ddmFormFieldValuesOri.set(index, orderId);

                    _log.info("order value for erc: " + orderId);
                }
            }

            ddmFormValues.setDDMFormFieldValues(ddmFormFieldValuesOri);

            _addFormInstanceMVCCommandHelper.updateNonevaluableDDMFormFields(
                    actionRequest, ddmForm, ddmFormValues,
                    LocaleUtil.fromLanguageId(_language.getLanguageId(actionRequest)));

            serviceContext.setRequest(_portal.getHttpServletRequest(actionRequest));
            _log.info("pass the order");
        }
//		//				START CUSTOMIZATION
        String mobileCode = _getByFieldReference(ddmFormValues, formInstanceId, "otpCode");
        String phoneNumber = _getByFieldReference(ddmFormValues, formInstanceId, "phoneNumber");
        _log.error("mobileCode : " + mobileCode);


        _log.error("phoneNumber : " + mobileCode);

        if (!mobileCode.isEmpty()) {

            if (_log.isInfoEnabled()) {
                _log.error("validate otp");
            }
            if (!_validateMobileOTP(phoneNumber, mobileCode)) {


                SessionErrors.add(actionRequest, OTPCheckException.class);

                return;
            }
        }
        // CUSTOM

        if (!themeDisplay.isSignedIn()) {
            Role adminRole = RoleLocalServiceUtil.getRole(
                    themeDisplay.getCompanyId(), RoleConstants.ADMINISTRATOR);

            long[] userIds = UserLocalServiceUtil.getRoleUserIds(
                    adminRole.getRoleId());

            if (!ArrayUtil.isEmpty(userIds)) {
                userId = userIds[0];
                serviceContext.setUserId(userId);
            }
        }

        try {
            if (_log.isInfoEnabled()) {
                _log.info("update instance");
            }
            _updateFormInstanceRecord(
                    actionRequest, ddmFormInstance, ddmFormValues, groupId,
                    serviceContext, userId);

        } catch (Exception ex) {
            if (_log.isErrorEnabled()) {
                _log.error("error: " + ex.getMessage());
            }
            //throw an exception
            SessionErrors.add(actionRequest, UserEmailAddressException.MustNotBeDuplicate.class);
        }


        if (!SessionErrors.isEmpty(actionRequest)) {
            return;
        }

        if (SessionMessages.contains(
                actionRequest,
                _portal.getPortletId(actionRequest) +
                        SessionMessages.KEY_SUFFIX_HIDE_DEFAULT_SUCCESS_MESSAGE)) {

            SessionMessages.clear(actionRequest);
        }

        SessionMessages.add(actionRequest, "formInstanceRecordAdded");

        DDMFormInstanceSettings ddmFormInstanceSettings =
                ddmFormInstance.getSettingsModel();

        String redirectURL = ParamUtil.getString(
                actionRequest, "redirect", ddmFormInstanceSettings.redirectURL());

        if (_log.isInfoEnabled()) {
            _log.info("all done");
        }
        if (Validator.isNotNull(redirectURL)) {
            portletSession.setAttribute(
                    DDMFormWebKeys.DYNAMIC_DATA_MAPPING_FORM_INSTANCE_ID,
                    formInstanceId);
            portletSession.setAttribute(DDMFormWebKeys.GROUP_ID, groupId);

            sendRedirect(actionRequest, actionResponse, redirectURL);
        } else {
            DDMFormSuccessPageSettings ddmFormSuccessPageSettings =
                    ddmForm.getDDMFormSuccessPageSettings();

            if (ddmFormSuccessPageSettings.isEnabled()) {
                hideDefaultSuccessMessage(actionRequest);
            }
        }
    }

    private List<Object[]> getCPDefinitionFromDSL(String currentLanguage, String[] productType, List<Object[]> cpDefinitions) {
        try {
            DSLQuery query = DSLQueryFactoryUtil
                    .select(CPDefinitionTable.INSTANCE.CPDefinitionId, CPDefinitionLocalizationTable.INSTANCE.name, CPInstanceTable.INSTANCE.price)
                    .from(CPDefinitionTable.INSTANCE)
                    .innerJoinON(CPDefinitionLocalizationTable.INSTANCE, CPDefinitionLocalizationTable.INSTANCE.CPDefinitionId.eq(CPDefinitionTable.INSTANCE.CPDefinitionId))
                    .innerJoinON(CPInstanceTable.INSTANCE, CPInstanceTable.INSTANCE.CPDefinitionId.eq(CPDefinitionTable.INSTANCE.CPDefinitionId))
                    .where(CPDefinitionLocalizationTable.INSTANCE.name.in(productType).and(CPDefinitionLocalizationTable.INSTANCE.languageId.eq(currentLanguage)).and(CPInstanceTable.INSTANCE.status.eq(0)));


            cpDefinitions = CPDefinitionLocalServiceUtil.dslQuery(query);
            _log.debug("product size : " + cpDefinitions.size());

        } catch (Exception e) {
            _log.error("Error Occurred while getting Product Details based on the form " + e);
        }
        return cpDefinitions;
    }

    private String _getByFieldReference(DDMFormValues ddmFormValues, long ddmFormInstanceId, String fieldReference, Locale locale) throws Exception {

        String fieldValue = StringPool.BLANK;
        try {
            Map<String, DDMFormField> ddmFormFields = getDistinctFields(
                    ddmFormInstanceId);

            Map<String, List<DDMFormFieldValue>> ddmFormFieldValuesMap =
                    ddmFormValues.getDDMFormFieldValuesReferencesMap(false);

            for (Map.Entry<String, DDMFormField> entry : ddmFormFields.entrySet()) {
                if (entry.getKey(
                ).equalsIgnoreCase(
                        fieldReference
                )) {

                    fieldValue = getDDMFormFieldValue(
                            entry.getValue(), ddmFormFieldValuesMap,
                            locale);
                    break;
                }

            }
        }catch (Exception e){
            _log.info("getDDMFormFieldValue exception: "+ e);
        }
        return fieldValue;
    }

    private String _getByFieldReference(DDMFormValues ddmFormValues, long ddmFormInstanceId, String fieldReference) throws Exception {

        String fieldValue = StringPool.BLANK;
        try {
            Map<String, DDMFormField> ddmFormFields = getDistinctFields(
                    ddmFormInstanceId);

            Map<String, List<DDMFormFieldValue>> ddmFormFieldValuesMap =
                    ddmFormValues.getDDMFormFieldValuesReferencesMap(false);

            for (Map.Entry<String, DDMFormField> entry : ddmFormFields.entrySet()) {
                if (entry.getKey(
                ).equalsIgnoreCase(
                        fieldReference
                )) {

                    fieldValue = getDDMFormFieldValue(
                            entry.getValue(), ddmFormFieldValuesMap,
                            ddmFormValues.getDefaultLocale());
                    break;
                }

            }
        }catch (Exception e){
            _log.info("getDDMFormFieldValue exception: "+ e);
        }
        return fieldValue;
    }

    protected DDMForm getDDMForm(DDMFormInstance ddmFormInstance)
            throws PortalException {

        DDMStructure ddmStructure = ddmFormInstance.getStructure();

        return ddmStructure.getDDMForm();
    }

    protected String getDDMFormFieldValue(
            DDMFormField ddmFormField,
            Map<String, List<DDMFormFieldValue>> ddmFormFieldValueMap,
            Locale locale) {

        List<DDMFormFieldValue> ddmFormFieldValues = ddmFormFieldValueMap.get(
                ddmFormField.getFieldReference());

        DDMFormFieldValueRenderer ddmFormFieldValueRenderer =
                ddmFormFieldTypeServicesTracker.getDDMFormFieldValueRenderer(
                        ddmFormField.getType());

        Stream<DDMFormFieldValue> stream = ddmFormFieldValues.stream();

        return _html.unescape(
                StringUtil.merge(
                        stream.map(
                                ddmForFieldValue -> ddmFormFieldValueRenderer.render(
                                        ddmForFieldValue, locale)
                        ).filter(
                                Validator::isNotNull
                        ).collect(
                                Collectors.toList()
                        ),
                        StringPool.COMMA_AND_SPACE));
    }

    protected Map<String, DDMFormField> getDistinctFields(
            long ddmFormInstanceId)
            throws Exception {

        List<DDMStructureVersion> ddmStructureVersions = getStructureVersions(
                ddmFormInstanceId);

        Map<String, DDMFormField> ddmFormFields = new LinkedHashMap<>();

        Stream<DDMStructureVersion> stream = ddmStructureVersions.stream();

        stream.map(
                this::getNontransientDDMFormFieldsReferencesMap
        ).forEach(
                map -> map.forEach(
                        (key, ddmFormField) -> ddmFormFields.putIfAbsent(
                                key, ddmFormField))
        );

        return ddmFormFields;
    }

    protected Map<String, DDMFormField>
    getNontransientDDMFormFieldsReferencesMap(
            DDMStructureVersion ddmStructureVersion) {

        DDMForm ddmForm = ddmStructureVersion.getDDMForm();

        return ddmForm.getNontransientDDMFormFieldsReferencesMap(true);
    }

    protected List<DDMStructureVersion> getStructureVersions(
            long ddmFormInstanceId)
            throws Exception {

        List<DDMFormInstanceVersion> ddmFormInstanceVersions =
                ddmFormInstanceVersionLocalService.getFormInstanceVersions(
                        ddmFormInstanceId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);

        ddmFormInstanceVersions = ListUtil.sort(
                ddmFormInstanceVersions,
                new FormInstanceVersionVersionComparator());

        List<DDMStructureVersion> ddmStructureVersions = new ArrayList<>();

        for (DDMFormInstanceVersion ddmFormInstanceVersion :
                ddmFormInstanceVersions) {

            ddmStructureVersions.add(
                    ddmFormInstanceVersion.getStructureVersion());
        }

        return ddmStructureVersions;
    }

    @Reference(unbind = "-")
    protected void setDDMFormInstanceRecordService(
            DDMFormInstanceRecordService ddmFormInstanceRecordService) {

        _ddmFormInstanceRecordService = ddmFormInstanceRecordService;
    }

    @Reference(unbind = "-")
    protected void setDDMFormInstanceService(
            DDMFormInstanceService ddmFormInstanceService) {

        _ddmFormInstanceService = ddmFormInstanceService;
    }

    @Reference(unbind = "-")
    protected void setDDMFormValuesFactory(
            DDMFormValuesFactory ddmFormValuesFactory) {

        _ddmFormValuesFactory = ddmFormValuesFactory;
    }

    @Reference
    protected DDMFormFieldTypeServicesTracker ddmFormFieldTypeServicesTracker;

    @Reference
    protected DDMFormInstanceVersionLocalService
            ddmFormInstanceVersionLocalService;

    private String _extractByField(
            ActionRequest actionRequest, DDMFormValues ddmFormValues,
            long ddmFormInstanceId, String fieldName)
            throws Exception {

        Map<String, DDMFormField> ddmFormFields = getDistinctFields(
                ddmFormInstanceId);

        Map<String, List<DDMFormFieldValue>> ddmFormFieldValuesMap =
                ddmFormValues.getDDMFormFieldValuesReferencesMap(false);
        String productId = "";

        for (Map.Entry<String, DDMFormField> entry : ddmFormFields.entrySet()) {
            if (entry.getKey(
            ).equalsIgnoreCase(
                    fieldName
            )) {

                productId = String.valueOf(
                        getDDMFormFieldValue(
                                entry.getValue(), ddmFormFieldValuesMap,
                                ddmFormValues.getDefaultLocale()));

                break;
            }
        }

        return productId;
    }

    private void _updateFormInstanceRecord(
            ActionRequest actionRequest, DDMFormInstance ddmFormInstance,
            DDMFormValues ddmFormValues, long groupId,
            ServiceContext serviceContext, long userId)
            throws Exception {

        // CUSTOM
        DDMFormInstanceRecord ddmFormInstanceRecord = null;

        long ddmFormInstanceRecordId = ParamUtil.getLong(
                actionRequest, "formInstanceRecordId");

        if (_log.isInfoEnabled()) {
            _log.info("ddmFormInstanceRecordId = " + ddmFormInstanceRecordId);
        }

        if (ddmFormInstanceRecordId != 0) {
            ddmFormInstanceRecord = _ddmFormInstanceRecordService.updateFormInstanceRecord(
                    ddmFormInstanceRecordId, false, ddmFormValues, serviceContext);
        } else {
            DDMFormInstanceRecordVersion ddmFormInstanceRecordVersion =
                    _ddmFormInstanceRecordVersionLocalService.
                            fetchLatestFormInstanceRecordVersion(
                                    userId, ddmFormInstance.getFormInstanceId(),
                                    ddmFormInstance.getVersion(),
                                    WorkflowConstants.STATUS_DRAFT);

            if (_log.isInfoEnabled()) {
                _log.info("form instance record version = " + ddmFormInstanceRecordVersion);
            }

            if (ddmFormInstanceRecordVersion == null) {
                if (_log.isInfoEnabled()) {
                    _log.info("add form = " + ddmFormInstance.getFormInstanceId());
                }
                ddmFormInstanceRecord = _ddmFormInstanceRecordService.addFormInstanceRecord(
                        groupId, ddmFormInstance.getFormInstanceId(),
                        ddmFormValues, serviceContext);
                if (_log.isInfoEnabled()) {
                    _log.info("added form ");
                }

            } else {
                ddmFormInstanceRecord = _ddmFormInstanceRecordService.updateFormInstanceRecord(
                        ddmFormInstanceRecordVersion.getFormInstanceRecordId(),
                        false, ddmFormValues, serviceContext);
            }
        }
        AssetEntry assetEntry = AssetEntryLocalServiceUtil.getEntry(DDMFormInstanceRecord.class.getName(), ddmFormInstanceRecord.getFormInstanceRecordId());
        assetEntry.setTitle(assetEntry.getTitle() + " " + ddmFormInstanceRecord.getFormInstanceRecordId());
        AssetEntryLocalServiceUtil.updateAssetEntry(assetEntry);
    }

    private void _validateCaptcha(
            ActionRequest actionRequest, DDMFormInstance ddmFormInstance)
            throws Exception {

        DDMFormInstanceSettings formInstanceSettings =
                ddmFormInstance.getSettingsModel();

        if (formInstanceSettings.requireCaptcha()) {
            CaptchaUtil.check(actionRequest);
        }
    }

    private boolean _validateMembership(
            String membershipId)
            throws Exception {

        return MirumHTTPInvoke.validateMembership(membershipId);
    }

    private boolean _validateMobileOTP(
            String phoneNumber, String otpCode)
            throws Exception {

        if (Validator.isNull(otpCode)) {
            return true;
        }

        return MirumHTTPInvoke.validateOTPCode(phoneNumber, otpCode);
    }

    private void _validatePublishStatus(
            ActionRequest actionRequest, DDMFormInstance ddmFormInstance)
            throws Exception {

        ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(
                WebKeys.THEME_DISPLAY);

        String currentURL = ParamUtil.getString(actionRequest, "currentURL");

        DDMFormInstanceSettings ddmFormInstanceSettings =
                ddmFormInstance.getSettingsModel();

        if (StringUtil.startsWith(
                currentURL,
                _addDefaultSharedFormLayoutPortalInstanceLifecycleListener.
                        getFormLayoutURL(themeDisplay)) &&
                !ddmFormInstanceSettings.published()) {

            throw new FormInstanceNotPublishedException(
                    "Form instance " + ddmFormInstance.getFormInstanceId() +
                            " is not published");
        }
    }

    private static final Log _log = LogFactoryUtil.getLog(
            MirumFormSubmissionMVCActionCommand.class);

    private static final String EXPANDO_SECONDARY_SERVICE = "hasSecondaryService";
    private static final String EXPANDO_HAS_POINT = "hasPoint";

    @Reference
    private AddDefaultSharedFormLayoutPortalInstanceLifecycleListener
            _addDefaultSharedFormLayoutPortalInstanceLifecycleListener;

    @Reference
    private AddFormInstanceRecordMVCCommandHelper
            _addFormInstanceMVCCommandHelper;

    @Reference
    private CommerceChannelLocalService _commerceChannelLocalService;

    @Reference
    private CommerceContextFactory _commerceContextFactory;

    @Reference
    private CommerceOrderService _commerceOrderService;

    @Reference
    private CommerceOrderTypeService _commerceOrderTypeService;

    private DDMFormInstanceRecordService _ddmFormInstanceRecordService;

    @Reference
    private DDMFormInstanceRecordVersionLocalService
            _ddmFormInstanceRecordVersionLocalService;

    private DDMFormInstanceService _ddmFormInstanceService;
    private DDMFormValuesFactory _ddmFormValuesFactory;

    @Reference
    private Html _html;

    @Reference
    private Language _language;

    @Reference
    private Portal _portal;

}
