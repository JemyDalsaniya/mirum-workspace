<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ include file="/init.jsp" %>

<%@ page import="java.math.RoundingMode" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="com.liferay.portal.kernel.util.StringBundler" %>
<%@ page import="com.liferay.commerce.model.CommerceOrderItem" %>
<%@ page import="com.liferay.commerce.product.model.CPDefinition" %>
<%@ page import="com.fasterxml.jackson.databind.ObjectMapper" %>
<%@ page import="com.liferay.portal.kernel.exception.PortalException" %>
<%@ page import="com.fasterxml.jackson.databind.JsonNode" %>
<%@ page import="com.liferay.portal.kernel.util.LocaleUtil" %>
<%@ page import="com.liferay.commerce.product.service.CPDefinitionLocalServiceUtil" %>
<%@ page import="com.liferay.expando.kernel.model.ExpandoBridge" %>
<%@ page import="java.math.BigDecimal" %>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" integrity="sha512-iecdLmaskl7CVkqkXNQ/ZH/XLlvWZOJyj7Yy7tcenmpD1ypASozpmT/E0iPtmFIB46ZmdtAc9eNBvH0H/ZpiBw==" crossorigin="anonymous" referrerpolicy="no-referrer" />
<style>
    .pagination-bar,
    .navbar-form.navbar-form-autofit.navbar-overlay.navbar-overlay-sm-down,
    .management-bar-wrapper,
    .dnd-td.item-actions {
        display: none;
    }

    a:hover {
        text-decoration: none;
    }

    .data-set .dnd-table .dnd-thead {
        margin-bottom: 0;
    }

    .order-table {
        width: 100%;
    }

    .order-table th {
        padding: 0.75rem;
        font-size: 12px;
        color: #7f848d;
        letter-spacing: 1.2px;
        text-transform: uppercase;
        line-height: 1.1;
        background: #fff;
    }

    .order-table th.total-amount {
        color: rgba(55, 186, 198, 1);
    }

    .order-table tr td {
        font-size: 0.875rem;
        font-weight: 500;
        line-height: 1.45;
        margin-bottom: 0;
        color: #272833;
        border-bottom: 1px solid #e7e7ed;
        padding: 0.75rem;
    }

    .order-table tr td a {
        color: #272833;
        display: inline-block;
        max-width: 100%;
        text-decoration: none;
        outline: none;
        font-weight: 600;

    }

    .order-table td.total-amount {
        color: rgba(55, 186, 198, 1);
    }
    .align-right {
        float: right;
        bottom: 60px;
        margin-right: 120px;
    }
    .align-left {
        float: left;
        bottom: 60px;
        margin-left: 120px;
    }

    .mirum-order-detail{
        background-color: #F5F7FA;
        margin: 0px 0px;
        height: 600px;
        padding: 50px
    }
    .commerce-panel{
        padding: 0px 200px;
    }
    @media(max-width:500px){
        .mirum-order-detail{
            margin-left: 10px;
            margin-right: 10px;
            padding: 10px;
            height: auto;
            margin-bottom: 150px;
        }
        .commerce-panel{
            padding: 20px;
        }
        .horizontal-scroll{
            padding: 0;
            overflow-x: auto;
        }
        .align-right {
            float: right;
            top: 10px;
            margin-right: 10px;
        }
        .align-left{
            top: 10px;
            margin-left: 10px;
        }
        .btn.btn-rounded:not(.btn-colored-link).btn-regular{
            padding: 10px 20px 10px 20px;
        }
    }
    @media(min-width:501px) and (max-width:768px){
        .mirum-order-detail{
            margin-left: 20px;
            margin-right: 20px;
            padding: 20px;
            height: auto;
            margin-bottom: 150px;
        }
        .commerce-panel{
            padding: 20px;
        }
        .horizontal-scroll{
            padding: 0;
            overflow-x: auto;
        }
        .align-right {
            float: right;
            top: 10px;
            margin-right: 20px;
        }
        .align-left{
            margin-left: 20px;
            top: 10px;
        }
        .btn.btn-rounded:not(.btn-colored-link).btn-regular{
            padding: 15px 25px 15px 25px;
        }
    }
    @media(min-width:769px) and (max-width:1300px){
        .mirum-order-detail{
            margin-right: 80px;
            margin-left: 80px;
            height: auto;
            margin-bottom: 150px;
        }
        .commerce-panel{
            padding: 20px;
        }
        .horizontal-scroll{
            padding: 0;
            overflow-x: auto;
        }
        .align-right {
            float: right;
            top: 10px;
            margin-right: 120px;
        }
        .align-left{
            margin-left: 120px;
            top: 10px;
        }
        .btn.btn-rounded:not(.btn-colored-link).btn-regular{
            padding: 15px 25px 15px 25px;
        }
    }
    @media(min-width:1301px){
        .mirum-order-detail{
            margin: 0px 100px;
            /*height: 600px;*/
            height: auto;
            padding: 50px;
        }
        .commerce-panel{
            padding: 0px;
        }
        .horizontal-scroll{
            padding: 0;
            overflow-x: auto;
        }
    }
	@media (max-width: 1700px) {
		.portlet-dropzone {
			padding-left: 0px;
			padding-right: 0px;
		}
	}


</style>

<%
CommerceOrder commerceOrder = commerceOrderContentDisplayContext.getCommerceOrder();

CommerceAddress billingCommerceAddress = commerceOrder.getBillingAddress();
CommerceAddress shippingCommerceAddress = commerceOrder.getShippingAddress();

CommerceOrderPrice commerceOrderPrice = commerceOrderContentDisplayContext.getCommerceOrderPrice();

CommerceMoney shippingValueCommerceMoney = commerceOrderPrice.getShippingValue();
CommerceDiscountValue shippingCommerceDiscountValue = commerceOrderPrice.getShippingDiscountValue();
CommerceMoney subtotalCommerceMoney = commerceOrderPrice.getSubtotal();
CommerceDiscountValue subtotalCommerceDiscountValue = commerceOrderPrice.getSubtotalDiscountValue();
CommerceMoney taxValueCommerceMoney = commerceOrderPrice.getTaxValue();
CommerceDiscountValue totalCommerceDiscountValue = commerceOrderPrice.getTotalDiscountValue();
CommerceMoney totalOrderCommerceMoney = commerceOrderPrice.getTotal();

String priceDisplayType = commerceOrderContentDisplayContext.getCommercePriceDisplayType();

if (priceDisplayType.equals(CommercePricingConstants.TAX_INCLUDED_IN_PRICE)) {
	shippingValueCommerceMoney = commerceOrderPrice.getShippingValueWithTaxAmount();
	shippingCommerceDiscountValue = commerceOrderPrice.getShippingDiscountValueWithTaxAmount();
	subtotalCommerceMoney = commerceOrderPrice.getSubtotalWithTaxAmount();
	subtotalCommerceDiscountValue = commerceOrderPrice.getSubtotalDiscountValueWithTaxAmount();
	totalCommerceDiscountValue = commerceOrderPrice.getTotalDiscountValueWithTaxAmount();
	totalOrderCommerceMoney = commerceOrderPrice.getTotalWithTaxAmount();
}

CommerceAccount commerceAccount = commerceOrderContentDisplayContext.getCommerceAccount();

if (commerceOrder != null) {
	commerceAccount = commerceOrder.getCommerceAccount();
}
%>

<liferay-ui:error exception="<%= CommerceOrderValidatorException.class %>">

	<%
	CommerceOrderValidatorException commerceOrderValidatorException = (CommerceOrderValidatorException)errorException;
	%>

	<c:if test="<%= commerceOrderValidatorException != null %>">

		<%
		for (CommerceOrderValidatorResult commerceOrderValidatorResult : commerceOrderValidatorException.getCommerceOrderValidatorResults()) {
		%>

			<liferay-ui:message key="<%= HtmlUtil.escape(commerceOrderValidatorResult.getLocalizedMessage()) %>" />

		<%
		}
		%>

	</c:if>
</liferay-ui:error>

<c:choose>
    <c:when test="<%= themeDisplay.getLanguageId().equals("en_US") %>">
        <button class="btn btn-main btn-primary btn-regular btn-rounded align-right" id="" name=""
                style="display: none !important;" type="submit">
                <span class="lfr-btn-label" id="print">
            <liferay-ui:message key="print-the-bill"/>&nbsp;<i class="fa fa-print" aria-hidden="true"></i></span>
        </button>
    </c:when>
    <c:otherwise>
        <button class="btn btn-main btn-primary btn-regular btn-rounded align-left" id="" name=""
                style="display: none !important;" type="submit">
                <span class="lfr-btn-label" id="print">
            <liferay-ui:message key="print-the-bill"/>&nbsp;<i class="fa fa-print" aria-hidden="true"></i></span>
        </button>
    </c:otherwise>
</c:choose>
<%--
<button class="btn btn-main btn-primary btn-regular btn-rounded align-right" id="" name=""
        style="display: inline !important; bottom: 60px; margin-left: 120px;" type="submit">
    <span class="lfr-btn-label" id="print">
<liferay-ui:message key="print-the-bill"/>&nbsp;<i class="fa fa-print" aria-hidden="true"></i></span>
</button>--%>
<div class="mirum-order-detail">
    <div class="commerce-panel">
        <div class="commerce-panel__content" style="line-height: 30px;">
            <div class="align-items-center row">
                <div class="col-md-3">
                    <dl class="commerce-list">
                        <dt>
                            <strong><liferay-ui:message key="order-id"/></strong>
                        </dt>
                        <dd><%= commerceOrder.getCommerceOrderId() %>
                        </dd>
                    </dl>
                </div>

			<div class="col-md-3">
				<dl class="commerce-list">
					<dt>
						<strong><liferay-ui:message key="total" /></strong>
					</dt>
					<dd><%= HtmlUtil.escape(totalOrderCommerceMoney.format(Locale.ENGLISH)) %></dd>
				</dl>
			</div>
		</div>
	</div>

	<%--<div class="commerce-panel__content" style="line-height: 30px;">
		<div class="align-items-center row">
			<div class="col-md-3">
				<dl class="commerce-list">
					<dt>
						<strong><liferay-ui:message key="notes" /></strong>
					</dt>
					<dd>

						<%
						request.setAttribute("order_notes.jsp-showLabel", Boolean.TRUE);
						request.setAttribute("order_notes.jsp-taglibLinkCssClass", "link-outline link-outline-borderless link-outline-secondary lfr-icon-item-reverse");
						%>

						<liferay-util:include page="/placed_commerce_orders/order_notes.jsp" servletContext="<%= application %>" />
					</dd>
				</dl>
			</div>

			<div class="col-md-3">
				<dl class="commerce-list">
					<dt>
						<strong><liferay-ui:message key="account-id" /></strong>
					</dt>
					<dd><%= commerceAccount.getCommerceAccountId() %></dd>
				</dl>
			</div>
		</div>
	</div>--%>

	<div class="commerce-panel__content" style="line-height: 30px;">
		<div class="align-items-center row">
			<div class="col-md-3">
				<dl class="commerce-list">
					<dt>
						<strong><liferay-ui:message key="order-date" /></strong>
					</dt>
					<dd>
						<%= commerceOrderContentDisplayContext.getCommerceOrderDate(commerceOrder) %>

						<c:if test="<%= commerceOrderContentDisplayContext.isShowCommerceOrderCreateTime() %>">
							<%= commerceOrderContentDisplayContext.getCommerceOrderTime(commerceOrder) %>
						</c:if>
					</dd>
				</dl>
			</div>

			<div class="col-md-3">
				<dl class="commerce-list">
					<dt>
						<strong><liferay-ui:message key="order-name" /></strong>
					</dt>
<%--					<dd><%= HtmlUtil.escape(commerceOrder.getExternalReferenceCode()) %></dd>--%>
					<dd><%= commerceOrder.getExpandoBridge().getAttribute("orderName") %></dd>
				</dl>
			</div>
		</div>
	</div>

	<div class="commerce-panel__content" style="line-height: 30px;">
		<div class="align-items-center row">
			<div class="col-md-3">
				<dl class="commerce-list">
					<dt>
						<strong><liferay-ui:message key="order-status" /></strong>
					</dt>
					<dd><%= commerceOrderContentDisplayContext.getCommerceOrderStatus(commerceOrder) %></dd>
				</dl>
			</div>

			<!-- START CUSTOMIZE -->

			<div class="col-md-3">
				<dl class="commerce-list">
					<dt>
						<strong><liferay-ui:message key="order-transactionId" /></strong>
					</dt>
					<dd><%= HtmlUtil.escape(commerceOrder.getTransactionId()) %></dd>
				</dl>
			</div>

			<!-- END CUSTOMIZE -->
		</div>
	</div>
</div>

<c:if test="<%= commerceOrderContentDisplayContext.isShowPurchaseOrderNumber() %>">
	<div class="row">
		<div class="col-md-12">
			<div class="commerce-panel">
				<div class="commerce-panel__title"><liferay-ui:message key="purchase-order-number" /></div>
				<div class="commerce-panel__content">
					<div class="row">
						<div class="col-md-6">
							<dl class="commerce-list">
								<%= HtmlUtil.escape(commerceOrder.getPurchaseOrderNumber()) %>
							</dl>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</c:if>

<div class="commerce-cta is-visible">
	<portlet:actionURL name="/commerce_open_order_content/edit_commerce_order" var="editCommerceOrderActionURL">
		<portlet:param name="mvcRenderCommandName" value="/commerce_order_content/view_commerce_order_details" />
	</portlet:actionURL>

	<aui:form action="<%= editCommerceOrderActionURL %>" method="post" name="fm">
		<aui:input name="<%= Constants.CMD %>" type="hidden" />
		<aui:input name="commerceOrderId" type="hidden" value="<%= String.valueOf(commerceOrder.getCommerceOrderId()) %>" />
	</aui:form>

	<c:if test="<%= commerceOrderContentDisplayContext.isShowRetryPayment() %>">
		<aui:button cssClass="btn-lg" href="<%= commerceOrderContentDisplayContext.getRetryPaymentURL() %>" primary="<%= true %>" value="retry-payment" />
	</c:if>

	<liferay-util:dynamic-include key="com.liferay.commerce.order.content.web#/place_order_detail_cta#" />
</div>

<div class="row horizontal-scroll">
	<%-- <div class="col-md-1">
	</div> --%>

	<div class="col-md-9">
		<c:choose>
			<c:when test="<%= commerceOrder.getPaymentStatus() == 4 %>">
				<p style="color: #B11010;">
					<liferay-ui:message key="payment-failed" />
				</p>
			</c:when>
			<c:when test="<%= commerceOrder.getPaymentStatus() == 0 %>">
				<p style="color: #287D3C;">
					<liferay-ui:message key="payment-success" />
				</p>
			</c:when>
			<c:otherwise>
				<p>
					<liferay-ui:message key="payment-pending" />
				</p>
			</c:otherwise>
		</c:choose>

		<br />

		<%
		java.util.Map<String, String> contextParams = new java.util.HashMap<>();
		contextParams.put("commerceOrderId", String.valueOf(commerceOrder.getCommerceOrderId()));
		%>

            <%--	custom code	start	--%>

            <div class="addVat">
                <table class="order-table">
                    <tr>
                        <th><liferay-ui:message key="name"/></th>
                        <%--                        <th><liferay-ui:message key="options"/></th>--%>
<%--                        <th><liferay-ui:message key="sku"/></th>--%>
                        <th><liferay-ui:message key="list-price"/></th>
                        <%-- <th>Sale Price</th>--%>
                        <th><liferay-ui:message key="quantity"/></th>
                        <th><liferay-ui:message key="vat"/></th>
                        <th class="total-amount"><liferay-ui:message key="total"/></th>
                        <%--<th>Shipped Quantity</th>--%>

                    </tr>
                    <%

                        List<CommerceOrderItem> commerceOrderList = commerceOrder.getCommerceOrderItems();
                        CPDefinition cpDefinition = null;
						boolean isMultiProduct = false;
						int primaryProductQuantity = 0;
						int secondaryServiceQuantity = 0;
						boolean hasPoint = false;
						String finalQuantity = "";
//                        String url = "";


                        if (commerceOrderList != null && !commerceOrderList.isEmpty()) {
							isMultiProduct = commerceOrderList.size()>1?true:false;
							for (CommerceOrderItem orderItem : commerceOrderList) {
								hasPoint = false;
								try {
									cpDefinition = orderItem.getCPDefinition();
									ExpandoBridge expandoBridgeQuantity = CPDefinitionLocalServiceUtil.getCPDefinition(cpDefinition.getCPDefinitionId()).getExpandoBridge();
									if (expandoBridgeQuantity.hasAttribute("hasPoint") && expandoBridgeQuantity.getAttribute("hasPoint").equals(false)){
										primaryProductQuantity = orderItem.getQuantity();
									} else if (expandoBridgeQuantity.hasAttribute("hasPoint") && expandoBridgeQuantity.getAttribute("hasPoint").equals(true)) {
										secondaryServiceQuantity = orderItem.getQuantity();
										hasPoint = true;
									}
									if (primaryProductQuantity != 0) {
										finalQuantity = secondaryServiceQuantity / primaryProductQuantity + "x" + primaryProductQuantity;
									} else {
										finalQuantity = "N/A";
									}
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
//                            url = themeDisplay.getCDNBaseURL() + "/web/jcc/p/" + cpDefinition.getURL(themeDisplay.getLanguageId());

                    %>
                    <tr>
                        <td><a class="" data-senna-off="true"
<%--							   href=<%=url%>--%>
						>
                            <%=cpDefinition.getNameMap().get(LocaleUtil.fromLanguageId(themeDisplay.getLanguageId()))%>
                        </a></td>
                        <%-- <td>
                             <%=optionValue%>
                         </td>--%>
                       <%-- <td><a class="" data-senna-off="true"
                               href=<%=url%>><%=cpDefinition.getCPInstances().get(0).getSku()%>
                        </a>
                        </td>--%>
<%--                        <td><%= subtotalCommerceMoney.getPrice().setScale(2, RoundingMode.HALF_UP)  %> SAR</td>--%>
						<td><%= orderItem.getUnitPrice().setScale(2, RoundingMode.HALF_UP)  %> SAR</td>
						<td>
							<c:choose>
								<c:when test="<%=hasPoint%>">
									<%= finalQuantity %>
								</c:when>
								<c:otherwise>
									<span class="commerce-quantity"><%= orderItem.getQuantity() %></span>
								</c:otherwise>
							</c:choose>
						</td>
						<td>
						    <c:choose>
                                <c:when test="<%=hasPoint && primaryProductQuantity != 0%>">
                                        <%= ((orderItem.getUnitPriceWithTaxAmount().subtract(orderItem.getUnitPrice())).setScale(2, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf((secondaryServiceQuantity / primaryProductQuantity) * primaryProductQuantity)) %>
                                </c:when>
                                <c:otherwise>
                                    <%= ((orderItem.getUnitPriceWithTaxAmount().subtract(orderItem.getUnitPrice())).setScale(2, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(orderItem.getQuantity())) %>
                                </c:otherwise>
                            </c:choose>
						    SAR
						</td>
						<td class="total-amount"><%= orderItem.getFinalPriceWithTaxAmount().setScale(2, RoundingMode.HALF_UP)%>
							SAR
						</td>
						<tr>

						</tr>
					<%
							}
						}
					%>
                        <%--<td>0</td>--%>
					<tr class="<%= isMultiProduct ? "" : "hide" %>" >
						<td><liferay-ui:message key="order-total-value"/></td>
						<td></td>
						<td></td>
						<td></td>
						<td class="total-amount">
							<%--                                    <%= commerceOrder.getTotalWithTaxAmount().setScale(2, RoundingMode.HALF_UP)%> SAR--%>
							<%= totalOrderCommerceMoney.getPrice().setScale(2, RoundingMode.HALF_UP)%> SAR
						</td>
					</tr>
                </table>
            </div>
        </div>

        <div class="col-md-1">
        </div>
    </div>

    <div class="row" style="padding: 0 40px;">
        <div class="col-md-3" style="text-align: center;">
            <c:if test="<%= commerceOrder.getPaymentStatus() == 4 %>">

                <%
                    String basURL = themeDisplay.getCDNBaseURL();
                    String retryPayment = StringBundler.concat(basURL, "/web/jcc/checkout/-/checkout/order-summary/", commerceOrder.getUuid(), "?applicantId=" + commerceOrder.getPrintedNote());
                %>

                <aui:button-row>
                    <a href="<%= retryPayment %>">
                        <aui:button cssClass="btn btn-main btn-primary btn-regular btn-rounded" primary="<%= true %>"
                                    style="display: inline;" type="submit" value="retry-payment"/>
                    </a>
                </aui:button-row>
            </c:if>
        </div>
    </div>
</div>

<script>
    function downloadPdf() {
        const language = themeDisplay.getLanguageId();
        fetch('/o/mirum/download', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                "commerceOrderId": "<%=commerceOrder.getCommerceOrderId()%>",
                "languageId": language
            })
        })
            .then(response => response.blob())
            .then(blob => {
                const downloadLink = document.createElement('a');
                const url = window.URL.createObjectURL(blob);
                downloadLink.href = url;
                downloadLink.download = 'invoice.pdf';
                downloadLink.click();
                window.URL.revokeObjectURL(url);
            })
            .catch(error => {
                console.error('Error:', error);
            });
    }
    const button = document.getElementById("print");
    button.addEventListener("click", downloadPdf);
</script>

