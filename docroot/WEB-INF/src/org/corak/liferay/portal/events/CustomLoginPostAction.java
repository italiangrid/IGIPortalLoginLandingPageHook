/**
 * Copyright (c) 2010 Tarkan Corak. All rights reserved.
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

package org.corak.liferay.portal.events;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.struts.LastPath;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

public class CustomLoginPostAction extends Action {

	/**
	 * Will be fired for each Login Event and redirects the user to her or his landing page.
	 */
	public void run(HttpServletRequest request, HttpServletResponse response)
			throws ActionException {

		try {
			debug("CustomLoginPostAction for User: " + request.getRemoteUser());

			HttpSession session = request.getSession();
			User user = UserLocalServiceUtil.getUser(PortalUtil
					.getUserId(request));
			LastPath lastPath = null;
			Group firstUserGroup = null;
			
			Long companyId = PortalUtil.getCompanyId(request);
			
			//Role roleUser = RoleLocalServiceUtil.getRole(companyId, "User");
			
			Role rolePowerUser = RoleLocalServiceUtil.getRole(companyId, "Power User");
			
			Role roleAdministrator = RoleLocalServiceUtil.getRole(companyId, "Administrator");

			// First look for User's Private Pages
			if(!(UserLocalServiceUtil.hasRoleUser(rolePowerUser.getRoleId(), user.getUserId())||UserLocalServiceUtil.hasRoleUser(roleAdministrator.getRoleId(), user.getUserId())) ) {
				// Redirect to User's Private Pages
				lastPath = new LastPath(request.getContextPath(), "/web/guest/registration");
			}else{
				lastPath = null;
				if(lastPath == null) {
					// Look for Organizations
					debug("Looking for associated Organizations");
					List<Organization> organizations = user.getOrganizations();
					for(Organization organization : organizations) {
						Group group = organization.getGroup();
						debug("Organization: " + group.getName()
								+ ", friendlyUrl: " + group.getFriendlyURL());
						if(firstUserGroup == null)
							firstUserGroup = group;
						if(group.hasPrivateLayouts())
							lastPath = new LastPath(request.getContextPath(), PropsUtil.get(PropsKeys.LAYOUT_FRIENDLY_URL_PRIVATE_GROUP_SERVLET_MAPPING)+group.getFriendlyURL());
						if(lastPath != null)
							break;
					}
				}
	
				if(lastPath == null) {
					// Look for Communities
					debug("Looking for associated Communities");
					List<Group> groups = user.getGroups();
					for(Group group : groups) {
						debug("Community: " + group.getName() + ", friendlyUrl: "
								+ group.getFriendlyURL());
						//if(group.getName().equals("Guest")){
							//lastPath = new LastPath(request.getContextPath(), "/web/guest/workflow");
						//	break;
						//}
						//if(firstUserGroup == null)
							firstUserGroup = group;
							if(group.hasPrivateLayouts()&&(!group.getName().equals("Guest")))
								lastPath = new LastPath(request.getContextPath(), PropsUtil.get(PropsKeys.LAYOUT_FRIENDLY_URL_PRIVATE_GROUP_SERVLET_MAPPING)+group.getFriendlyURL());
							if(lastPath != null)
								break;
					}
					if(firstUserGroup.getName().equals("Guest")){
						lastPath = new LastPath(request.getContextPath(), "/web/guest/workflow");
					}
				}
	
				if(lastPath == null && firstUserGroup != null) {
					// Redirect to the Public Pages of the first found
					// Community/Organization
					lastPath = new LastPath(request.getContextPath(), PropsUtil.get(PropsKeys.LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING)+firstUserGroup.getFriendlyURL());
				}
				
				/*if((lastPath == null)&&(UserLocalServiceUtil.hasRoleUser(rolePowerUser.getRoleId(), user.getUserId()) || UserLocalServiceUtil.hasRoleUser(roleAdministrator.getRoleId(), user.getUserId()))) {
					// Redirect to User's Private Pages
					lastPath = new LastPath(request.getContextPath(), "/web/guest/workflow");
				}*/
				
			}

			if(lastPath != null) {
				debug("lastPath = " + lastPath.toString());
				session.setAttribute(WebKeys.LAST_PATH, lastPath);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Helper Method to create debug log messages
	 * @param msg Log message as String
	 */
	private void debug(String msg) {
		if (_log.isDebugEnabled()) {
			_log.debug(msg);
		}
	}

	private static Log _log = LogFactoryUtil
			.getLog(CustomLoginPostAction.class);
}
