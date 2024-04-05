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
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="com.liferay.commerce.constants.CommerceOrderConstants" %>
<%@ page import="com.liferay.expando.kernel.model.ExpandoBridge" %>
<%@ taglib prefix = "fmt" uri = "http://java.sun.com/jsp/jstl/fmt" %>

<style>

.table-list-title[href], .table-list-title [href] {
	color: #37BAC6;
}

.dnd-th.content-renderer-actionLink {
	color: #37BAC6;
}

.pagination-bar {
	display: none;
}

.background-div {
	padding: 50px;
}

.background-div,
nav.management-bar.navbar.navbar-expand-md.justify-content-space-between.management-bar-light,
.navbar-form.navbar-form-autofit.navbar-overlay.navbar-overlay-sm-down {
	background-color: #F5F7FA !important;
}



/* datatable style */
.common-table{
position:relative;
}
.common-table table
{
   width:100%;

}
.common-table table tr th,
.common-table table tr td
{
    padding:0.75rem;
    font-size: .875rem;
}
.common-table table
{
    border:1px solid #e7e7ed;
    border-radius:4px;
    margin-top:20px;
}
.common-table table tr th{
 color:#272833;
}
.common-table table tr .clm-chg-cls
{
 color:#37BAC6;
}

.common-table table tr td:first-child a{
 color:#5c5e5e;
}
.common-table table thead tr th
{
    background:#fff;
}
.common-table table tbody .odd
{
   background:#f7f8f9;
}
.common-table table tbody .even
{
   background:#fff;
}
.common-table table tbody tr:hover{
   background:#f0f5ff;
}

.common-table table tbody tr td,
.common-table table tr th
{
 border-bottom:1px solid #e7e7ed;
 border-left:1px solid #e7e7ed;
}
.common-table table tbody tr td .view-btn{
width:60px;
margin:0 auto;
}
.common-table  .dataTables_filter{
  border: 1px solid #e7e7ed;
  background:#F5F7FA;
  padding:15px;
}
.common-table  .dataTables_filter input{

 border: 1px solid #e6e7e7;
 width: 100%;
 height: 2.5rem;
 font-size: 1rem;
 padding:0.4375rem 1rem;
 background:#f1f2f5;
 color:#272833;
 border-radius:4px;
}
.common-table  .dataTables_filter input:focus
{
       background-color: #f0f5ff;
       border-color: #80acff;
       box-shadow:none;
       outline:none;
}
.common-table  .dataTables_filter label{
    width:70%;
    margin:0 auto;
    display:block;
}
.common-table  .paginate_button
{
    height:48px;
    width:48px;
    border:1px solid var(--secondary-11-color);
    color:var(--secondary-11-color);
    font-size:16px;
    display:flex;
    align-items:center;
    justify-content:center;
    cursor:pointer;
}
.common-table  .dataTables_info {
 color:var(--secondary-11-color);
 font-size:13px;
 margin-top:15px;
}
.common-table  .paging_simple_numbers {
  display:flex;
  width:100%;
  margin-top:15px;
}
.common-table  .paging_simple_numbers span{
display:flex;
}
.common-table  .paging_simple_numbers span .paginate_button{
 color:var(--secondary-11-color);
 font-size:16px;
 line-height:24px;
 margin:0px 8px;
 border:none;
 width:auto;

}
.common-table  .paging_simple_numbers span .paginate_button.current
{
    background:var(--gradientBlueHorizontal);
    height:48px;
    width:48px;
    border-radius:50px;
    color:#fff;
    display:flex;
    align-items:center;
    justify-content:center;
}
.common-table .dataTables_length{
 position:absolute;
 bottom:0px;
}
.common-table .dataTables_length select{
        background: rgba(0, 0, 0, 0.001);
        border: 1px solid transparent;
        color: #6b6c7e;
}
.common-table .dataTables_length label
{
    color: #6b6c7e;
    font-weight: normal;
}


@media(max-width:1100px){
.common-table
{
  margin-bottom:130px;
}

}
</style>

<div class="background-div hide">
<liferay-ddm:template-renderer
	className="<%= CommerceOrderContentPortlet.class.getName() %>"
	contextObjects='<%=
		HashMapBuilder.<String, Object>put(
			"commerceOrderContentDisplayContext", commerceOrderContentDisplayContext
		).build()
		%>'
	displayStyle="<%= commerceOrderContentDisplayContext.getDisplayStyle(CommercePortletKeys.COMMERCE_ORDER_CONTENT) %>"
	displayStyleGroupId="<%= commerceOrderContentDisplayContext.getDisplayStyleGroupId(CommercePortletKeys.COMMERCE_ORDER_CONTENT) %>"
	entries="<%= commerceOrderSearchContainer.getResults() %>"
>
<frontend-data-set:classic-display
	dataProviderKey="<%= CommerceOrderFDSNames.PLACED_ORDERS %>"
	id="<%= CommerceOrderFDSNames.PLACED_ORDERS %>"
	itemsPerPage="<%= 10 %>"
	style="stacked"
/>
	</liferay-ddm:template-renderer>
</div>

<script src="https://code.jquery.com/jquery-3.7.0.min.js" crossorigin="anonymous"></script>
<script src="https://cdn.datatables.net/1.13.5/js/jquery.dataTables.min.js"></script>


<%
DecimalFormat df = new DecimalFormat("###.##");
%>
<div class="common-table background-div">
<table id="my-order-list" class="display" style="width:100%">
    <thead>
        <tr>
            <th><liferay-ui:message key="order-id" /></th>
            <th><liferay-ui:message key="order-name" /></th>
            <th><liferay-ui:message key="order-date" /></th>
            <th><liferay-ui:message key="status" /></th>
            <th class="clm-chg-cls"><liferay-ui:message key="amount" /></th>
             <th></th>
        </tr>
    </thead>
    <tbody>
        <% for(CommerceOrder commerceOrder : commerceOrderSearchContainer.getResults()){ %>
        <tr>
            <td><a href="?p_p_id=com_liferay_commerce_order_content_web_internal_portlet_CommerceOrderContentPortlet&_com_liferay_commerce_order_content_web_internal_portlet_CommerceOrderContentPortlet_mvcRenderCommandName=%2Fcommerce_order_content%2Fview_commerce_order_details&_com_liferay_commerce_order_content_web_internal_portlet_CommerceOrderContentPortlet_commerceOrderId=<%= commerceOrder.getCommerceOrderId()%>"> <%= commerceOrder.getCommerceOrderId()%> </a></td>
            <td>
            <%
            ExpandoBridge expandoBridge = commerceOrder.getExpandoBridge();
            if(expandoBridge.hasAttribute("orderName")){ %>
               <%= expandoBridge.getAttribute("orderName") %>
            <%}%>
            </td>
            <td><fmt:formatDate type="both" value = "<%= commerceOrder.getOrderDate()%>" /></td>
            <td><liferay-ui:message key="<%= CommerceOrderConstants.getOrderStatusLabel(commerceOrder.getOrderStatus()) %>" /></td>
            <td class="clm-chg-cls"><%= df.format(commerceOrder.getTotal()) %></td>
             <td><a href="?p_p_id=com_liferay_commerce_order_content_web_internal_portlet_CommerceOrderContentPortlet&_com_liferay_commerce_order_content_web_internal_portlet_CommerceOrderContentPortlet_mvcRenderCommandName=%2Fcommerce_order_content%2Fview_commerce_order_details&_com_liferay_commerce_order_content_web_internal_portlet_CommerceOrderContentPortlet_commerceOrderId=<%= commerceOrder.getCommerceOrderId()%>" class="btn btn-secondary btn-sm view-btn"><liferay-ui:message key="view" /></a></td>
        </tr>
        <%}%>
    </tbody>
</table>
</div>

<script type="text/javascript">
    $(document).ready(function(){
        $('#my-order-list').DataTable({
        language: {
                search: "",
                searchPlaceholder: "<liferay-ui:message key='search' />",
                paginate: {
                  next: '>',
                  previous: '<'
                },
                lengthMenu: "_MENU_ <liferay-ui:message key='items' />",
                emptyTable: "<liferay-ui:message key='no-results-found' />",
                info:"Showing _START_ to _END_ of _TOTAL_",
                infoEmpty:"Showing 0 to 0 of 0",
            }
        });
    });
</script>
