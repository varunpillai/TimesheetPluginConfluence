/*
 * Copyright 2016 Adrian Schnedlitz
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

package org.catrobat.confluence.services.impl;

import com.atlassian.confluence.core.service.NotAuthorizedException;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.catrobat.confluence.activeobjects.*;
import org.catrobat.confluence.rest.json.JsonTimesheetEntry;
import org.catrobat.confluence.services.PermissionService;
import org.catrobat.confluence.services.TeamService;
import org.joda.time.DateTime;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class PermissionServiceImpl implements PermissionService {

    private final UserManager userManager;
    private final TeamService teamService;
    private final ConfigService configService;
    private final UserAccessor userAccessor;

    public PermissionServiceImpl(UserManager userManager, TeamService teamService,
                                 ConfigService configService, UserAccessor userAccessor) {
        this.userManager = userManager;
        this.teamService = teamService;
        this.configService = configService;
        this.userAccessor = userAccessor;
    }

    public UserProfile checkIfUserExists(HttpServletRequest request) {
        UserProfile userProfile = userManager.getRemoteUser(request);

        if (userProfile == null) {
            throw new NotAuthorizedException("User does not exist.");
        }
        return userProfile;
    }

    public boolean checkIfUserIsGroupMember(HttpServletRequest request, String groupName) {
        UserProfile userProfile = userManager.getRemoteUser(request);

        if (userProfile == null) {
            return false;
        }

        List<String> userGroups = userAccessor.getGroupNames(userAccessor.getUserByKey(userProfile.getUserKey()));
        if (userGroups.contains(groupName))
            return true;

        return false;
    }

    public UserProfile checkIfUsernameExists(String userName) {
        UserProfile userProfile = userManager.getUserProfile(userName);

        if (userProfile == null) {
            throw new NotAuthorizedException("User does not exist.");
        }
        return userProfile;
    }

    public boolean checkIfUserExists(String userName) {
        UserProfile userProfile = userManager.getUserProfile(userName);
        if (userProfile == null) {
            return false;
        }
        return true;
    }

    public Response checkPermission(HttpServletRequest request) {
        UserKey userKey = userManager.getRemoteUser(request).getUserKey();

        if (userKey == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        /*
        else if (!userManager.isSystemAdmin(userKey)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        */
        return null;
    }

    public boolean isApproved(UserProfile applicationUser) {
        Config config = configService.getConfiguration();
        if (config.getApprovedGroups().length == 0 && config.getApprovedUsers().length == 0) {
            return false;
        }

        if (configService.isUserApproved(applicationUser.getUserKey().getStringValue())) {
            return true;
        }

        Collection<String> groupNameCollection = userAccessor.getGroupNamesForUserName(applicationUser.getUsername());
        for (String groupName : groupNameCollection) {
            if (configService.isGroupApproved(groupName))
                return true;
        }

        return false;
    }

    public boolean isApproved(String userName) {
        return isApproved(userManager.getUserProfile(userName));
    }

    public boolean userIsAdmin(String userName) {
        return userManager.isAdmin(userName);
    }

    private boolean userOwnsSheet(UserProfile user, Timesheet sheet) {
        if (sheet == null || user == null) {
            return false;
        }

        String sheetKey = sheet.getUserKey();
        String userKey = user.getUserKey().getStringValue();
        return sheetKey.equals(userKey);
    }

    private boolean userIsAdmin(UserProfile user) {
        return userManager.isAdmin(user.getUserKey());
    }

    private boolean dateIsOlderThanAMonth(Date date) {
        DateTime aMonthAgo = new DateTime().minusDays(30);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(aMonthAgo) < 0);
    }

    private boolean dateIsOlderThanFiveYears(Date date) {
        DateTime fiveYearsAgo = new DateTime().minusYears(5);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(fiveYearsAgo) < 0);
    }

    private boolean userCoordinatesTeamsOfSheet(UserProfile user, Timesheet sheet) {
        UserProfile owner = userManager.getUserProfile(sheet.getUserKey());
        if (owner == null)
            return false;

        Set<Team> ownerTeams = teamService.getTeamsOfUser(owner.getUsername());
        Set<Team> userTeams = teamService.getCoordinatorTeamsOfUser(user.getUsername());

        ownerTeams.retainAll(userTeams);

        return ownerTeams.size() > 0;
    }

    @Override
    public boolean userCanViewTimesheet(UserProfile user, Timesheet sheet) {
        return user != null && sheet != null &&
                (userOwnsSheet(user, sheet)
                        || userIsAdmin(user)
                        || userCoordinatesTeamsOfSheet(user, sheet));
    }

    @Override
    public void userCanEditTimesheetEntry(UserProfile user, Timesheet sheet, JsonTimesheetEntry entry) {

        if (userOwnsSheet(user, sheet)) {
            if (!entry.getIsGoogleDocImport()) {
                if (dateIsOlderThanAMonth(entry.getBeginDate()) || dateIsOlderThanAMonth(entry.getEndDate())) {
                    throw new NotAuthorizedException("You can not edit an entry that is older than 30 days.");
                }
            } else {
                if (dateIsOlderThanFiveYears(entry.getBeginDate()) || dateIsOlderThanFiveYears(entry.getEndDate())) {
                    throw new NotAuthorizedException("You can not edit an imported entry that is older than 5 years.");
                }
            }
        } else if (!userIsAdmin(user)) {
            throw new NotAuthorizedException("You are not Admin.");
        }
    }

    @Override
    public void userCanDeleteTimesheetEntry(UserProfile user, TimesheetEntry entry) {

        if (userOwnsSheet(user, entry.getTimeSheet())) {
            if (!entry.getIsGoogleDocImport()) {
                if (dateIsOlderThanAMonth(entry.getBeginDate()) || dateIsOlderThanAMonth(entry.getEndDate())) {
                    throw new NotAuthorizedException("You can not delete an that is older than 30 days.");
                }
            } else {
                if (dateIsOlderThanFiveYears(entry.getBeginDate()) || dateIsOlderThanFiveYears(entry.getEndDate())) {
                    throw new NotAuthorizedException("You can not delete an imported entry that is older than 5 years.");
                }
            }
        } else if (!userIsAdmin(user)) {
            throw new NotAuthorizedException("You are not Admin");
        }
    }
}
