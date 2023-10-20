package org.alfresco.elasticsearch.reindexing.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestCategoryModel;
import org.alfresco.utility.data.DataUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Categories
{
    private static final String ROOT_CATEGORY_ID = "-root-";
    public static final RestCategoryModel ROOT_CATEGORY = RestCategoryModel.builder()
            .id(ROOT_CATEGORY_ID)
            .create();

    @Autowired
    private RestWrapper restClient;

    @Autowired
    private DataUser dataUser;

    private final Map<String, Category> categories = new HashMap<>();

    public List<Category> createNestedCategories(String... nestedCategoriesNames)
    {
        if (nestedCategoriesNames.length == 0)
        {
            return Collections.emptyList();
        }

        List<Category> categories = new ArrayList<>();
        categories.add(new Category(restClient, dataUser.getAdminUser(), ROOT_CATEGORY, nestedCategoriesNames[0]));
        this.categories.put(nestedCategoriesNames[0], categories.get(0));

        for (int i = 1; i < nestedCategoriesNames.length; i++)
        {
            categories.add(categories.get(i - 1).createSubcategory(nestedCategoriesNames[i]));
            this.categories.put(nestedCategoriesNames[i], categories.get(i));
        }

        return categories;
    }

    public Category get(String categoryName) {
        return categories.get(categoryName);
    }

    public void delete(String categoryName)
    {
        Category categoryToDelete = categories.remove(categoryName);
        delete(categoryToDelete);
    }

    private void delete(Category category)
    {
        restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .usingCategory(category)
                .deleteCategory();
    }
}
