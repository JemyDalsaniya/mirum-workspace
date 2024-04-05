This is a working app that overrides the update_password.jsp in Liferay 7.3 GA1.

I used the following resources while creating my app:
https://git.fortiss.org/civitas-digitalis/platform/-/tree/master/modules/UpdatePasswordOverride
https://www.softwaresavvyblog.com/post/liferay-overriding-core-jsps

The app will print an "OVERRIDDEN" message to the top of the page when you visit:
http://localhost:8080/c/portal/update_password

Here are my detailed steps I took:

1. Create a new Liferay workspace on your computer:
- use blade init command from the terminal
- select dxp-7.3-sp1

2. Create an MVC Portlet project and remove the unnecessary files
- Ensure that your build.gradle file contains this line:
compileOnly group: "com.liferay.portal", name: "release.portal.api"
otherwise your class will not detect the import: com.liferay.portal.deploy.hot.CustomJspBag;

3. Modify your class:
- add "implements CustomJspBag" after your class' name
- Implement unimplemented methods (this is the part where using Liferay Developer Studio may come handy)
I used the earlier mentioned blog resources for that.

4. Build the project and deploy it to a running 7.3 GA1 server

5. Visit this page, and the overridden message should appear:
http://localhost:8080/c/portal/update_password
