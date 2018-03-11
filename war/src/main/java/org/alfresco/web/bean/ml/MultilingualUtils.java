/*
 * #%L
 * Alfresco Repository WAR Community
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.web.bean.ml;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.ml.MultilingualContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

/**
 * Util class for the management of multilingual documents on the web client side
 *
 * @author yanipig
 */
public class MultilingualUtils implements Serializable
{

    private static final long serialVersionUID = 2218309432064312000L;

   /**
     * Returns true if the current user has enough right to add a content to the space
     * where the pivot translation is located in.
     *
     * @param multlingualDocument Node
     * @param fc FacesContext
     * @return boolean
     */
    public static boolean canAddChildrenToPivotSpace(Node multlingualDocument, FacesContext fc)
    {
        MultilingualContentService mlservice = getMultilingualContentService(fc);
        NodeService nodeService = getNodeService(fc);

        // get the pivot translation and get the space where it's located
        NodeRef pivot = mlservice.getPivotTranslation(multlingualDocument.getNodeRef());
        NodeRef space = nodeService.getPrimaryParent(pivot).getParentRef();

        // return if the current user can add a content to the space of the pivot
        return new Node(space).hasPermission(PermissionService.ADD_CHILDREN);
    }

    /**
     * Returns true if the current user can delete each translation of the mlContainer of the given node
     *
     * @param multlingualDocument Node
     * @param fc FacesContext
     * @return boolean
     */
    public static boolean canDeleteEachTranslation(Node multlingualDocument, FacesContext fc)
    {
        boolean can = true;

        MultilingualContentService mlservice = getMultilingualContentService(fc);

        Map<Locale, NodeRef> translations = mlservice.getTranslations(multlingualDocument.getNodeRef());
        for (Map.Entry<Locale, NodeRef> entry : translations.entrySet())
        {
            Node translation = new Node(entry.getValue());

            if(translation.hasPermission(PermissionService.DELETE_NODE) == false
                    || translation.isLocked() == true
                    || translation.hasAspect(ContentModel.ASPECT_WORKING_COPY) == true
                )
            {
                can = false;
                break;
            }
        }

        return can;
    }

    /**
     * Returns true if the current user can move each translation of the mlContainer of the given node
     *
     * @param multlingualDocument Node
     * @param fc FacesContext
     * @return boolean
     */
    public static boolean canMoveEachTranslation(Node multlingualDocument, FacesContext fc)
    {
        boolean can = true;

        MultilingualContentService mlservice = getMultilingualContentService(fc);

        Map<Locale, NodeRef> translations = mlservice.getTranslations(multlingualDocument.getNodeRef());
        for (Map.Entry<Locale, NodeRef> entry : translations.entrySet())
        {
            Node translation = new Node(entry.getValue());

            if(translation.hasPermission(PermissionService.DELETE_NODE) == false)
            {
                can = false;
                break;
            }
        }

        return can;
    }

    /**
     * Returns true if the current user can delete each translation and create
 *   * a new content in the space
     *
     * @param multlingualDocument Node
     * @param fc FacesContext
     * @return boolean
     */
    public static boolean canStartNewEditon(Node multlingualDocument, FacesContext fc)
    {
        boolean canDelete = MultilingualUtils.canMoveEachTranslation(multlingualDocument, fc);
        boolean canCreate = MultilingualUtils.canAddChildrenToPivotSpace(multlingualDocument, fc);

        return canDelete && canCreate;
    }

    private static MultilingualContentService getMultilingualContentService(FacesContext fc)
    {
        return (MultilingualContentService) FacesHelper.getManagedBean(fc, "MultilingualContentService");
    }

    private static NodeService getNodeService(FacesContext fc)
    {
        return (NodeService) FacesHelper.getManagedBean(fc, "NodeService");
    }

}
