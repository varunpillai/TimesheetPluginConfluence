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

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.core.service.NotAuthorizedException;
import net.java.ao.Query;
import net.java.ao.schema.Table;
import org.catrobat.confluence.activeobjects.Category;
import org.catrobat.confluence.services.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

@Table("CatServiceI")
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private final ActiveObjects ao;

    public CategoryServiceImpl(ActiveObjects ao) {
        this.ao = checkNotNull(ao);
    }

    public Category getCategoryByID(int id) {
        Category[] found = ao.find(Category.class, "ID = ?", id);
        assert (found.length <= 1);
        return (found.length > 0) ? found[0] : null;
    }

    public List<Category> all() {
        return newArrayList(ao.find(Category.class, Query.select().order("NAME ASC")));
    }

    public Category add(String name) {
        Category category = ao.create(Category.class);
        category.setName(name);
        category.save();
        return category;
    }

    public boolean removeCategory(String name) {
        Category[] found = ao.find(Category.class, "NAME = ?", name);

        if (found.length > 1) {
            throw new NotAuthorizedException("Multiple Categories with the same Name");
        } else if (found.length == 0) {
            throw new NotAuthorizedException("No Category with this Name");
        }

        ao.delete(found);
        return true;
    }

}
