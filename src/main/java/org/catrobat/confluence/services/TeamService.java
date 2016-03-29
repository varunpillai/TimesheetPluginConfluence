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

package org.catrobat.confluence.services;

import com.atlassian.activeobjects.tx.Transactional;
import org.catrobat.confluence.activeobjects.Team;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Transactional
public interface TeamService {

    Team add(String name);

    boolean removeTeam(String teamName);

    List<Team> all();

    @Nullable
    Team getTeamByID(int id);

    @Nullable
    Team getTeamByName(String name);

    Set<Team> getTeamsOfUser(String userName);

    Set<Team> getCoordinatorTeamsOfUser(String userName);

}
