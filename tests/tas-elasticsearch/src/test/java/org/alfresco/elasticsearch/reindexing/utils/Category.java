package org.alfresco.elasticsearch.reindexing.utils;

import java.util.UUID;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestCategoryModel;
import org.alfresco.utility.model.UserModel;

public class Category extends RestCategoryModel
{
    private final RestWrapper restClient;

    private final UserModel user;

    public Category(RestWrapper restClient, UserModel user, RestCategoryModel parent, String newCategoryName)
    {
        this.restClient = restClient;
        this.user = user;
        newCategoryName = generateRandomName(newCategoryName);

        RestCategoryModel category = restClient.authenticateUser(user)
                .withCoreAPI()
                .usingCategory(parent)
                .createSingleCategory(RestCategoryModel.builder().name(newCategoryName).create());

        setName(category.getName());
        setCount(category.getCount());
        setId(category.getId());
        setPath(category.getPath());
        setParentId(category.getParentId());
        setHasChildren(category.getHasChildren());
    }

    public Category createSubcategory(String subcategoryName)
    {
        return new Category(restClient, user, this, subcategoryName);
    }

    private static String generateRandomName(String categorySuffixx)
    {
        return "category" + categorySuffixx + "_" + UUID.randomUUID();
    }
}
