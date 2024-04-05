package ddm.custom.notification.sender;

import com.liferay.dynamic.data.mapping.constants.DDMPortletKeys;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldTypeServicesTracker;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldValueRenderer;
import com.liferay.dynamic.data.mapping.internal.notification.DDMFormEmailNotificationSender;
import com.liferay.dynamic.data.mapping.model.*;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceVersionLocalService;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.util.comparator.FormInstanceVersionVersionComparator;
import com.liferay.mail.kernel.model.MailMessage;
import com.liferay.mail.kernel.service.MailService;
import com.liferay.petra.io.unsync.UnsyncStringWriter;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.template.*;
import com.liferay.portal.kernel.util.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import java.io.Writer;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.liferay.dynamic.data.mapping.service.DDMStructureVersionLocalServiceUtil.getStructureVersions;

/**
 * @author root318
 */
@Component(
		immediate = true, property = "service.ranking:Integer=" + Integer.MAX_VALUE,
		service = DDMFormEmailNotificationSender.class
)
public class DdmCustomNotificationSender extends DDMFormEmailNotificationSender {


	public void sendEmailNotification(
			DDMFormInstanceRecord ddmFormInstanceRecord,
			ServiceContext serviceContext) {


		try {
			MailMessage mailMessage = _createMailMessage(
					ddmFormInstanceRecord, serviceContext);


			_mailService.sendEmail(mailMessage);
		}
		catch (Exception exception) {
			_log.error("Unable to send form email", exception);
		}
	}

	protected Map<String, List<DDMFormFieldValue>> getDDMFormFieldValuesMap(
			DDMFormInstanceRecord ddmFormInstanceRecord)
			throws PortalException {

		DDMFormValues ddmFormValues = ddmFormInstanceRecord.getDDMFormValues();

		return ddmFormValues.getDDMFormFieldValuesMap(true);
	}

	protected Map<String, Object> getFieldProperties(
			List<DDMFormFieldValue> ddmFormFieldValues, Locale locale) {

		DDMFormField ddmFormField = _getDDMFormField(ddmFormFieldValues);

		if (Objects.equals(ddmFormField.getType(), "fieldset")) {
			return null;
		}



		if (Objects.equals(ddmFormField.getType(), "paragraph")) {
			return HashMapBuilder.<String, Object>put(
					"label", _getLabel(ddmFormField, locale)
			).put(
					"value", _getParagraphText(ddmFormField, locale)
			).build();
		}

		List<String> renderedDDMFormFieldValues = ListUtil.toList(
				ddmFormFieldValues,
				new Function<DDMFormFieldValue, String>() {

					@Override
					public String apply(DDMFormFieldValue ddmFormFieldValue) {
						return _renderDDMFormFieldValue(ddmFormFieldValue, locale);
					}

				});

		return HashMapBuilder.<String, Object>put(
				"label", _getLabel(ddmFormField, locale)
		).put(
				"value",
				StringUtil.merge(
						renderedDDMFormFieldValues, StringPool.COMMA_AND_SPACE)
		).put(
				"hideField",_isHiddenField(ddmFormField)
		).build();
	}

	protected List<Object> getFields(
			List<String> fieldNames,
			Map<String, List<DDMFormFieldValue>> ddmFormFieldValuesMap,
			Locale locale,
			DDMFormInstanceRecord ddmFormInstanceRecord,
			DDMFormInstance ddmFormInstance) throws Exception {

		List<Object> fields = new ArrayList<>();

		for (String fieldName : fieldNames) {
			List<DDMFormFieldValue> ddmFormFieldValues =
					ddmFormFieldValuesMap.get(fieldName);

			if (ddmFormFieldValues == null) {
				continue;
			}

			String isEmailFieldReference = _getByFieldReference(ddmFormInstanceRecord.getDDMFormValues(), ddmFormInstance.getFormInstanceId(), EMAIl_FIELD_REFERENCE);
			if(Validator.isNotNull(isEmailFieldReference)){
				if (getFieldProperties(ddmFormFieldValues, locale).get("hideField").toString().equals("false")) {
					fields.add(getFieldProperties(ddmFormFieldValues, locale));
				}
			} else {
				fields.add(getFieldProperties(ddmFormFieldValues, locale));
			}

			fields.addAll(
					_getNestedFields(
							ddmFormFieldValues, ddmFormFieldValuesMap, locale));
		}

		return fields;
	}

	private MailMessage _createMailMessage(
			DDMFormInstanceRecord ddmFormInstanceRecord,
			ServiceContext serviceContext)
			throws Exception {

		DDMFormInstance ddmFormInstance =
				ddmFormInstanceRecord.getFormInstance();

		InternetAddress fromInternetAddress = new InternetAddress(
				_getEmailFromAddress(ddmFormInstance),
				_getEmailFromName(ddmFormInstance));

		String subject = _getEmailSubject(ddmFormInstance);

		String body = _getEmailBody(
				serviceContext, ddmFormInstance, ddmFormInstanceRecord);

		MailMessage mailMessage = new MailMessage(
				fromInternetAddress, subject, body, true);

		InternetAddress[] toAddresses = InternetAddress.parse(
				_getEmailToAddress(ddmFormInstance, ddmFormInstanceRecord));

		mailMessage.setTo(toAddresses);

		return mailMessage;
	}

	private Template _createTemplate(
			ServiceContext serviceContext, DDMFormInstance ddmFormInstance,
				DDMFormInstanceRecord ddmFormInstanceRecord)
			throws Exception {


		Template template = null;
		String vacancyEmailAddress = _getByFieldReference(ddmFormInstanceRecord.getDDMFormValues(), ddmFormInstance.getFormInstanceId(), VACANCY_EMAIl_FIELD_REFERENCE);
		String emailAddress = _getByFieldReference(ddmFormInstanceRecord.getDDMFormValues(), ddmFormInstance.getFormInstanceId(), EMAIl_FIELD_REFERENCE);
		if(Validator.isNotNull(vacancyEmailAddress)){
			template = TemplateManagerUtil.getTemplate(
					TemplateConstants.LANG_TYPE_FTL,
					_getTemplateResource(_TEMPLATE_VACANCY_PATH), false);
		} else if (Validator.isNotNull(emailAddress)) {
			template = TemplateManagerUtil.getTemplate(
					TemplateConstants.LANG_TYPE_FTL,
					_getTemplateResource(_TEMPLATE_ORDER_WORKSHOP_PATH), false);
		} else {
			template = TemplateManagerUtil.getTemplate(
					TemplateConstants.LANG_TYPE_FTL,
					_getTemplateResource(_TEMPLATE_DEFAULT_PATH), false);
		}


		_populateParameters(
				template, serviceContext, ddmFormInstance, ddmFormInstanceRecord);

		return template;
	}

	private DDMFormField _getDDMFormField(
			List<DDMFormFieldValue> ddmFormFieldValues) {

		DDMFormFieldValue ddmFormFieldValue = ddmFormFieldValues.get(0);

		return ddmFormFieldValue.getDDMFormField();
	}

	private DDMFormLayout _getDDMFormLayout(DDMFormInstance ddmFormInstance)
			throws Exception {

		DDMStructure ddmStructure = ddmFormInstance.getStructure();

		return ddmStructure.getDDMFormLayout();
	}

	private String _getEmailBody(
			ServiceContext serviceContext, DDMFormInstance ddmFormInstance,
			DDMFormInstanceRecord ddmFormInstanceRecord)
			throws Exception {

		Template template = _createTemplate(
				serviceContext, ddmFormInstance, ddmFormInstanceRecord);

		return _render(template);
	}

	private String _getEmailFromAddress(DDMFormInstance ddmFormInstance)
			throws Exception {

		DDMFormInstanceSettings formInstancetings =
				ddmFormInstance.getSettingsModel();

		String defaultEmailFromAddress = _prefsProps.getString(
				ddmFormInstance.getCompanyId(), PropsKeys.ADMIN_EMAIL_FROM_ADDRESS);

		return GetterUtil.getString(
				formInstancetings.emailFromAddress(), defaultEmailFromAddress);
	}

	private String _getEmailFromName(DDMFormInstance ddmFormInstance)
			throws Exception {

		DDMFormInstanceSettings formInstancetings =
				ddmFormInstance.getSettingsModel();

		String defaultEmailFromName = _prefsProps.getString(
				ddmFormInstance.getCompanyId(), PropsKeys.ADMIN_EMAIL_FROM_NAME);

		return GetterUtil.getString(
				formInstancetings.emailFromName(), defaultEmailFromName);
	}

	private String _getEmailSubject(DDMFormInstance ddmFormInstance)
			throws Exception {

		DDMFormInstanceSettings formInstancetings =
				ddmFormInstance.getSettingsModel();

		DDMStructure ddmStructure = ddmFormInstance.getStructure();

		DDMForm ddmForm = ddmStructure.getDDMForm();

		Locale locale = ddmForm.getDefaultLocale();

		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
				"content.Language", locale, DdmCustomNotificationSender.class);

		String defaultEmailSubject = _language.format(
				resourceBundle, "new-x-form-submitted",
				ddmFormInstance.getName(locale), false);

		return GetterUtil.getString(
				formInstancetings.emailSubject(), defaultEmailSubject);
	}

	private String _getEmailToAddress(DDMFormInstance ddmFormInstance, DDMFormInstanceRecord ddmFormInstanceRecord)
			throws Exception {


		String defaultEmailToAddress = StringPool.BLANK;
		String vacancyEmailAddress = _getByFieldReference(ddmFormInstanceRecord.getDDMFormValues(), ddmFormInstance.getFormInstanceId(), VACANCY_EMAIl_FIELD_REFERENCE);
		String emailAddress = _getByFieldReference(ddmFormInstanceRecord.getDDMFormValues(), ddmFormInstance.getFormInstanceId(), EMAIl_FIELD_REFERENCE);


		DDMFormInstanceSettings formInstancetings =
				ddmFormInstance.getSettingsModel();

		User user = _userLocalService.fetchUser(ddmFormInstance.getUserId());

		if (user != null) {
			defaultEmailToAddress = user.getEmailAddress();
		}

		if(Validator.isNotNull(vacancyEmailAddress)){
			return GetterUtil.getString(
					vacancyEmailAddress, defaultEmailToAddress);
		} else if (Validator.isNotNull(emailAddress)) {
			return GetterUtil.getString(
					emailAddress, defaultEmailToAddress);
		} else {
			return GetterUtil.getString(
					formInstancetings.emailToAddress(), defaultEmailToAddress);
		}
	}

	private List<String> _getFieldNames(DDMFormLayoutPage ddmFormLayoutPage) {
		List<String> fieldNames = new ArrayList<>();

		for (DDMFormLayoutRow ddmFormLayoutRow :
				ddmFormLayoutPage.getDDMFormLayoutRows()) {

			for (DDMFormLayoutColumn ddmFormLayoutColumn :
					ddmFormLayoutRow.getDDMFormLayoutColumns()) {

				fieldNames.addAll(ddmFormLayoutColumn.getDDMFormFieldNames());
			}
		}

		return fieldNames;
	}

	private String _getLabel(DDMFormField ddmFormField, Locale locale) {
		LocalizedValue label = ddmFormField.getLabel();

		if (ddmFormField.isRequired()) {
			return label.getString(locale) + StringPool.STAR;
		}

		return label.getString(locale);
	}

	private Locale _getLocale(
			DDMFormInstance ddmFormInstance, ServiceContext serviceContext)
			throws Exception {

		HttpServletRequest httpServletRequest = serviceContext.getRequest();

		String languageId = GetterUtil.getString(
				httpServletRequest.getParameter("languageId"),
				ddmFormInstance.getDefaultLanguageId());

		return LocaleUtil.fromLanguageId(languageId);
	}


	private boolean _isHiddenField(DDMFormField ddmFormField){
		boolean hidden = false;
		if(ddmFormField.hasProperty("hideField")){
			 hidden = (boolean) ddmFormField.getProperty("hideField");
		}
		return hidden;
	}
	private List<Map<String, Object>> _getNestedFields(
			List<DDMFormFieldValue> ddmFormFieldValues,
			Map<String, List<DDMFormFieldValue>> ddmFormFieldValuesMap,
			Locale locale) {

		List<Map<String, Object>> nestedFields = new ArrayList<>();

		DDMFormField ddmFormField = _getDDMFormField(ddmFormFieldValues);

		Map<String, DDMFormField> nestedDDMFormFieldsMap =
				ddmFormField.getNestedDDMFormFieldsMap();

		for (String key : nestedDDMFormFieldsMap.keySet()) {
			nestedFields.add(
					getFieldProperties(ddmFormFieldValuesMap.get(key), locale));
		}

		return nestedFields;
	}

	private Map<String, Object> _getPage(
			DDMFormLayoutPage ddmFormLayoutPage,
			Map<String, List<DDMFormFieldValue>> ddmFormFieldValuesMap,
			Locale locale,
			DDMFormInstanceRecord ddmFormInstanceRecord,
			DDMFormInstance ddmFormInstance) throws Exception {

		return HashMapBuilder.<String, Object>put(
				"fields",
				getFields(
						_getFieldNames(ddmFormLayoutPage), ddmFormFieldValuesMap,
						locale, ddmFormInstanceRecord, ddmFormInstance)
		).put(
				"title",
				() -> {
					LocalizedValue title = ddmFormLayoutPage.getTitle();

					return title.getString(locale);
				}
		).build();
	}

	private List<Object> _getPages(
			DDMFormInstance ddmFormInstance,
			DDMFormInstanceRecord ddmFormInstanceRecord, Locale locale)
			throws Exception {

		List<Object> pages = new ArrayList<>();

		DDMFormLayout ddmFormLayout = _getDDMFormLayout(ddmFormInstance);

		for (DDMFormLayoutPage ddmFormLayoutPage :
				ddmFormLayout.getDDMFormLayoutPages()) {

			pages.add(
					_getPage(
							ddmFormLayoutPage,
							getDDMFormFieldValuesMap(ddmFormInstanceRecord), locale, ddmFormInstanceRecord, ddmFormInstance));
		}

		return pages;
	}

	private String _getParagraphText(DDMFormField ddmFormField, Locale locale) {
		LocalizedValue text = (LocalizedValue)ddmFormField.getProperty("text");

		if (text == null) {
			return StringPool.BLANK;
		}

		return _htmlParser.extractText(text.getString(locale));
	}

	private ResourceBundle _getResourceBundle(Locale locale) {
		return ResourceBundleUtil.getBundle(
				"content.Language", locale, DdmCustomNotificationSender.class);
	}

	private String _getSiteName(long groupId, Locale locale) throws Exception {
		Group siteGroup = _groupLocalService.fetchGroup(groupId);

		if (siteGroup != null) {
			return siteGroup.getDescriptiveName(locale);
		}

		return StringPool.BLANK;
	}

	private TemplateResource _getTemplateResource(String templatePath) {
		Class<?> clazz = DdmCustomNotificationSender.class;

		ClassLoader classLoader = clazz.getClassLoader();

		URL templateURL = classLoader.getResource(templatePath);

		return new URLTemplateResource(templateURL.getPath(), templateURL);
	}

	private String _getUserName(
			DDMFormInstanceRecord ddmFormInstanceRecord, Locale locale) {

		String userName = ddmFormInstanceRecord.getUserName();

		if (Validator.isNotNull(userName)) {
			return userName;
		}

		return _language.get(_getResourceBundle(locale), "someone");
	}

	private String _getViewFormEntriesURL(
			ServiceContext serviceContext, DDMFormInstance ddmFormInstance)
			throws Exception {

		String portletNamespace = _portal.getPortletNamespace(
				DDMPortletKeys.DYNAMIC_DATA_MAPPING_FORM_ADMIN);

		return _portal.getSiteAdminURL(
				serviceContext.getPortalURL(),
				_groupLocalService.getGroup(ddmFormInstance.getGroupId()),
				DDMPortletKeys.DYNAMIC_DATA_MAPPING_FORM_ADMIN,
				HashMapBuilder.put(
						portletNamespace.concat("mvcPath"),
						new String[] {"/admin/view_form_instance_records.jsp"}
				).put(
						portletNamespace.concat("formInstanceId"),
						new String[] {
								String.valueOf(ddmFormInstance.getFormInstanceId())
						}
				).build());
	}

	private String _getViewFormURL(
			ServiceContext serviceContext, DDMFormInstance ddmFormInstance,
			DDMFormInstanceRecord ddmFormInstanceRecord)
			throws Exception {

		String portletNamespace = _portal.getPortletNamespace(
				DDMPortletKeys.DYNAMIC_DATA_MAPPING_FORM_ADMIN);

		return _portal.getSiteAdminURL(
				serviceContext.getPortalURL(),
				_groupLocalService.getGroup(ddmFormInstance.getGroupId()),
				DDMPortletKeys.DYNAMIC_DATA_MAPPING_FORM_ADMIN,
				HashMapBuilder.put(
						portletNamespace.concat("mvcPath"),
						new String[] {"/admin/view_form_instance_record.jsp"}
				).put(
						portletNamespace.concat("formInstanceRecordId"),
						new String[] {
								String.valueOf(
										ddmFormInstanceRecord.getFormInstanceRecordId())
						}
				).put(
						portletNamespace.concat("formInstanceId"),
						new String[] {
								String.valueOf(ddmFormInstance.getFormInstanceId())
						}
				).build());
	}

	private void _populateParameters(
			Template template, ServiceContext serviceContext,
			DDMFormInstance ddmFormInstance,
			DDMFormInstanceRecord ddmFormInstanceRecord)
			throws Exception {

		Locale locale = _getLocale(ddmFormInstance, serviceContext);
		template.put("locale", locale);
		template.put("formName", ddmFormInstance.getName(locale));
		template.put("formInstanceRecordId" ,ddmFormInstanceRecord.getFormInstanceRecordId());
		template.put(
				"pages", _getPages(ddmFormInstance, ddmFormInstanceRecord, locale));
		template.put(
				"siteName", _getSiteName(ddmFormInstance.getGroupId(), locale));
		template.put("userName", _getUserName(ddmFormInstanceRecord, locale));

		template.put(
				"viewFormEntriesURL",
				_getViewFormEntriesURL(serviceContext, ddmFormInstance));
		template.put(
				"viewFormURL",
				_getViewFormURL(
						serviceContext, ddmFormInstance, ddmFormInstanceRecord));
	}

	private String _render(Template template) throws Exception {
		Writer writer = new UnsyncStringWriter();

		template.processTemplate(writer);

		return writer.toString();
	}

	private String _renderDDMFormFieldValue(
			DDMFormFieldValue ddmFormFieldValue, Locale locale) {

		if (ddmFormFieldValue.getValue() == null) {
			return StringPool.BLANK;
		}

		DDMFormFieldValueRenderer ddmFormFieldValueRenderer =
				_ddmFormFieldTypeServicesTracker.getDDMFormFieldValueRenderer(ddmFormFieldValue.getType());

		return ddmFormFieldValueRenderer.render(ddmFormFieldValue, locale);
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

	private static final String _TEMPLATE_DEFAULT_PATH =
			"/META-INF/resources/notification/default_notification_template.ftl";

	private static final String _TEMPLATE_VACANCY_PATH =
			"/META-INF/resources/notification/vacancy_form_notification.ftl";

	private static final String _TEMPLATE_ORDER_WORKSHOP_PATH =
			"/META-INF/resources/notification/order_and_workshop_notification.ftl";



	private static final Log _log = LogFactoryUtil.getLog(
			DdmCustomNotificationSender.class);

//	@Reference
//	private DDMFormFieldTypeServicesRegistry _ddmFormFieldTypeServicesRegistry;

	private static final String EMAIl_FIELD_REFERENCE = "Email";
	private static final String VACANCY_EMAIl_FIELD_REFERENCE = "VacancyEmail";


	@Reference
	protected DDMFormFieldTypeServicesTracker ddmFormFieldTypeServicesTracker;

	@Reference
	private Html _html;

	@Reference
	private DDMFormFieldTypeServicesTracker _ddmFormFieldTypeServicesTracker;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private HtmlParser _htmlParser;

	@Reference
	private Language _language;

	@Reference
	private MailService _mailService;

	@Reference
	private Portal _portal;

	@Reference
	private PrefsProps _prefsProps;

	@Reference
	private UserLocalService _userLocalService;

	@Reference
	protected DDMFormInstanceVersionLocalService
			ddmFormInstanceVersionLocalService;

}
