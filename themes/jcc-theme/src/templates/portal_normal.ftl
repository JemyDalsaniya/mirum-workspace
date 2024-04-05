<!DOCTYPE html>

<#include init />

<html class="${root_css_class}" dir="<@liferay.language key="lang.dir" />" lang="${w3c_language_id}">

<head>
	<title>${html_title}</title>

	<meta name="viewport" content="width=device-width, initial-scale=1.0, interactive-widget=resizes-content">
	<@liferay_util["include"] page=top_head_include />
	<link href="https://unpkg.com/aos@2.3.1/dist/aos.css" rel="stylesheet">
	<script src="https://unpkg.com/aos@2.3.1/dist/aos.js"></script>
	<script src="https://code.jquery.com/jquery-3.6.0.min.js" integrity="sha256-/xUj+3OJU5yExlq6GSYGSHk7tPXikynS7ogEvDej/m4=" crossorigin="anonymous"></script>
	<#--  <script src="https://unpkg.com/swiper/swiper-bundle.min.js"></script>   -->
	<script src="https://cdnjs.cloudflare.com/ajax/libs/Swiper/8.4.4/swiper-bundle.min.js" integrity="sha512-k2o1KZdvUi59PUXirfThShW9Gdwtk+jVYum6t7RmyCNAVyF9ozijFpvLEWmpgqkHuqSWpflsLf5+PEW6Lxy/wA==" crossorigin="anonymous" referrerpolicy="no-referrer"></script>
	<link href="https://fonts.googleapis.com/css2?family=Cairo:wght@200;300;400;500;600;700;800;900&display=swap" rel="stylesheet">
	<#--  <link rel="stylesheet" href="https://unpkg.com/swiper/swiper-bundle.min.css" />    -->
	<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/Swiper/8.4.4/swiper-bundle.css" integrity="sha512-wbWvHguVvzF+YVRdi8jOHFkXFpg7Pabs9NxwNJjEEOjiaEgjoLn6j5+rPzEqIwIroYUMxQTQahy+te87XQStuA==" crossorigin="anonymous" referrerpolicy="no-referrer" />
	<script type="text/javascript" src="https://maps.googleapis.com/maps/api/js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.4/moment-with-locales.min.js" integrity="sha512-42PE0rd+wZ2hNXftlM78BSehIGzezNeQuzihiBCvUEB3CVxHvsShF86wBWwQORNxNINlBPuq7rG4WWhNiTVHFg==" crossorigin="anonymous" referrerpolicy="no-referrer"></script>
 	<script type="text/javascript">
		// handle the noconflict designation, use namespace $jcc for jQuery.
	$jcc = jQuery.noConflict(true);
	let containAPaginationClicked = false;

	const urlParams = new URLSearchParams(location.search);
	for (const [key, value] of urlParams) {
	if (key?.indexOf("page_number") != -1) {
		containAPaginationClicked = true;
	}
	}

	   function handleSearchChange() {
       		 // Check if an element with class "taglib-empty-result-message" exists
			if ($jcc('.taglib-empty-result-message').length > 0) {
				// If it exists, add "display: none" to elements with class "portlet-header"
				$jcc('.portlet-search-results .portlet-header').css('display', 'none');
				// remove border-top color of portlet-content-container
				$jcc('.portlet-search-results .portlet-content-container').css('border-top','2px solid transparent')
			}

			 // Check if the element with class pagination-results exists
            if ($jcc(".pagination-results").length > 0) {
                // Get the text from the pagination-results div
                var originalText = $jcc(".pagination-results").text();
                // Replace Arabic digits with English digits
                var convertedText = originalText.replace(/١/g, "1")
                                               .replace(/٢/g, "2")
                                               .replace(/٣/g, "3")
                                               .replace(/٤/g, "4")
                                               .replace(/٥/g, "5")
                                               .replace(/٦/g, "6")
                                               .replace(/٧/g, "7")
                                               .replace(/٨/g, "8")
                                               .replace(/٩/g, "9")
                                               .replace(/٠/g, "0");

                // Update the content of the pagination-results div
                $jcc(".pagination-results").text(convertedText);
            }
			
    		if ($jcc(".portlet-tag-facet").length === 0) {
        			// The element with class "portlet-tag-facet" does not exist, so remove margins and paddings.
        			$jcc(".portlet-search-results").addClass('no-padding-top');
			}
		}
	// override window.history callback
	(function(history){
    const pushState = history.pushState;
    history.pushState = function(state) {
        if (typeof history.onpushstate == "function") {
            history.onpushstate({state: state});
        }
		// call function to execute styling on history params change
        setTimeout(()=>handleSearchChange(),1500);
        return pushState.apply(history, arguments);
    }
	})(window.history);


	$jcc(document).ready(function () {
		// call function to execute styling on history params change
        setTimeout(()=>handleSearchChange(),1500);
		// Select all elements with the class "list-group-item" and bind a click event handler
   		 $jcc('.list-group-item').click(function () {
        // Find the anchor link within the clicked list item
        var anchorLink = $jcc(this).find('a');
        if (anchorLink.length) {
            // Programmatically trigger a click event on the anchor link
            anchorLink[0].click();
       		}
   		});      
	})
	

	if (containAPaginationClicked) {
	$jcc(document).ready(function () {
		let pagination = $jcc(".pagination.pagination-root")
		.eq(0)
		.parents(".d-lg-block");
		if (pagination) {
		$jcc("html, body").animate(
			{
			scrollTop: pagination.offset().top - 200,
			},
			"slow"
		);
		};
	});

	$jcc(window).on('load',function () {
		let pagination = $jcc(".pagination.pagination-root")
		.eq(0)
		.parents(".d-lg-block");
		if (pagination) {
		$jcc("html, body").animate(
			{
			scrollTop: pagination.offset().top - 200,
			},
			"slow"
		);
		}
		//check if we have a form
		let frm = $jcc('.lfr-ddm-form-container');
		if(frm.length > 0){
           	$jcc("html, body").animate(
			{
			scrollTop: frm.offset().top - 200,
			},
			"slow"
		);
		}
	});
	}

	setTimeout(function(){
		if($jcc('.jcc-share-link').length > 0){
			$jcc('.jcc-share-link').on('click',function(){
				$jcc(this).toggleClass('show-share');
			});
		}
	} , 1000);
	</script>
    <script type='text/javascript' src='https://platform-api.sharethis.com/js/sharethis.js#property=63187c5cfd193a0013760368&product=inline-share-buttons' async='async'></script>	<style>
		.has-edit-mode-menu [data-aos]{
			opacity: 1 !important;
			transform: none !important;
		}
	</style>
	<!-- Google Tag Manager -->
	<script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push(

	{'gtm.start': new Date().getTime(),event:'gtm.js'}
	);var f=d.getElementsByTagName(s)[0],
	j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
	'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
	})(window,document,'script','dataLayer','GTM-MRJ4ZD');</script>
	<!-- End Google Tag Manager -->
</head>
<body class="${css_class}">
<!-- Google Tag Manager (noscript) -->
<noscript><iframe src="https://www.googletagmanager.com/ns.html?id=GTM-MRJ4ZD"
height="0" width="0" style="display:none;visibility:hidden"></iframe></noscript>
<!-- End Google Tag Manager (noscript) -->

<@liferay_ui["quick-access"] contentId="#main-content" />

<@liferay_util["include"] page=body_top_include />

<div class="d-flex flex-column min-vh-100">
	<@liferay.control_menu />

	<div class="d-flex flex-column flex-fill position-relative" id="wrapper">
		<#if show_header>
			<header id="banner">
				<div class="navbar navbar-classic navbar-top py-3">
					<div class="container-fluid container-fluid-max-xl user-personal-bar">
						<div class="align-items-center autofit-row">
							<a class="${logo_css_class} align-items-center d-md-inline-flex d-sm-none d-none logo-md" href="${site_default_url}" title="<@liferay.language_format arguments="" key="go-to-x" />">
								<img alt="${logo_description}" class="mr-2" height="56" src="${site_logo}" />

								<#if show_site_name>
									<h2 class="font-weight-bold h2 mb-0 text-dark" role="heading" aria-level="1">${site_name}</h2>
								</#if>
							</a>

							<#assign preferences = freeMarkerPortletPreferences.getPreferences({"portletSetupPortletDecoratorId": "barebone", "destination": "/search"}) />

							<div class="autofit-col autofit-col-expand">
								<#if show_header_search>
									<div class="justify-content-md-end mr-4 navbar-form" role="search">
										<@liferay.search_bar default_preferences="${preferences}" />
									</div>
								</#if>
							</div>

							<div class="autofit-col">
								<@liferay.user_personal_bar />
							</div>
						</div>
					</div>
				</div>

				<div class="navbar navbar-classic navbar-expand-md navbar-light pb-3">
					<div class="container-fluid container-fluid-max-xl">
						<a class="${logo_css_class} align-items-center d-inline-flex d-md-none logo-xs" href="${site_default_url}" rel="nofollow">
							<img alt="${logo_description}" class="mr-2" height="56" src="${site_logo}" />

							<#if show_site_name>
								<h2 class="font-weight-bold h2 mb-0 text-dark">${site_name}</h2>
								<h3 class="font-weight-normal h5 mb-0 ml-2 text-dark">Custom</h3>
							</#if>
						</a>

						<#include "${full_templates_path}/navigation.ftl" />
					</div>
				</div>
			</header>
		</#if>

		<section class="${portal_content_css_class} flex-fill" id="content">
			<h2 class="sr-only" role="heading" aria-level="1">${the_title}</h2>

			<#if selectable>
				<@liferay_util["include"] page=content_include />
			<#else>
				${portletDisplay.recycle()}

				${portletDisplay.setTitle(the_title)}

				<@liferay_theme["wrap-portlet"] page="portlet.ftl">
					<@liferay_util["include"] page=content_include />
				</@>
			</#if>
		</section>

		<#if show_footer>
			<footer id="footer" role="contentinfo">
				<div class="container">
					<div class="row">
						<div class="col-md-12 text-center text-md-left">
							<@liferay.language_format
								arguments='<a class="text-white" href="http://www.liferay.com" rel="external">Liferay</a>'
								key="powered-by-x"
							/>
						</div>
					</div>
				</div>
			</footer>
		</#if>
	</div>
</div>

<@liferay_util["include"] page=body_bottom_include />

<@liferay_util["include"] page=bottom_include />


<script>
 if (!document.body.classList.contains("has-edit-mode-menu")) {
  AOS.init({
	once: true,
	duration: 1500,
  });
 }
</script>


</body>

</html>