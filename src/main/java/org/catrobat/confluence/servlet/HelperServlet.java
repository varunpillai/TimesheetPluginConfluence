/*
 * Copyright 2014 Stephan Fellhofer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.catrobat.confluence.servlet;

import com.atlassian.confluence.security.DefaultPermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.user.GroupManager;
import org.catrobat.confluence.activeobjects.ConfigService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class HelperServlet extends HttpServlet {
    private final UserManager userManager;
    private final LoginUriProvider loginUriProvider;
    private final WebSudoManager webSudoManager;
    private final GroupManager groupManager;
    private final ConfigService configurationService;

    public HelperServlet(final UserManager userManager, final LoginUriProvider loginUriProvider,
                         final WebSudoManager webSudoManager, final GroupManager groupManager,
                         final ConfigService configurationService) {
        this.userManager = checkNotNull(userManager, "userManager");
        this.loginUriProvider = checkNotNull(loginUriProvider, "loginProvider");
        this.webSudoManager = checkNotNull(webSudoManager, "webSudoManager");
        this.groupManager = checkNotNull(groupManager);
        this.configurationService = checkNotNull(configurationService);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        checkPermission(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        checkPermission(request, response);
    }

    private void checkPermission(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        //PermissionCondition permissionCondition = new PermissionCondition(null, configurationService, userManager, groupManager);

        ConfluenceUser confluenceUser = AuthenticatedUserThreadLocal.get();

        String username = confluenceUser.getName(); // @Arian: or getFullName() you have to try it out
        // UserKey userKey = confluenceUser.getKey(); // I am not sure if we need this anymore, UserKey is gone :)
        DefaultPermissionManager contentPermissionManager = new DefaultPermissionManager();
        boolean isSystemAdmin = contentPermissionManager.isSystemAdministrator(confluenceUser);

        if (username == null) {
            redirectToLogin(request, response);
            return;
        } else if (!isSystemAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        /*
        else if (!permissionCondition.isApproved(username)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        */
        if (!webSudoManager.canExecuteRequest(request)) {
            webSudoManager.enforceWebSudoProtection(request, response);
            return;
        }

        response.setContentType("text/html;charset=utf-8");
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }

    private URI getUri(HttpServletRequest request) {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null) {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }
}