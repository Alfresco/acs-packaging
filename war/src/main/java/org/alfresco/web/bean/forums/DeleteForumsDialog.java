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
package org.alfresco.web.bean.forums;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.spaces.DeleteSpaceDialog;

/**
 * Bean implementation for the "Delete Forum Space" dialog
 * 
 * @author gavinc
 */
public class DeleteForumsDialog extends DeleteSpaceDialog
{
   private static final long serialVersionUID = -1673691210425371041L;
   
   protected boolean reDisplayForums;

   // ------------------------------------------------------------------------------
   // Dialog implementation
   
   @Override
   public void init(Map<String, String> parameters)
   {
      super.init(parameters);
      
      // reset the reDisplayForums flag
      this.reDisplayForums = false;
   }

   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      // find out what the parent type of the node being deleted 
      Node node = this.browseBean.getActionSpace();
      ChildAssociationRef assoc = this.getNodeService().getPrimaryParent(node.getNodeRef());
      if (assoc != null)
      {
         NodeRef parent = assoc.getParentRef();
         QName parentType = this.getNodeService().getType(parent);
         if (parentType.equals(ForumModel.TYPE_FORUMS))
         {
            this.reDisplayForums = true;
         }
      }

      return super.finishImpl(context, outcome);
   }

   @Override
   protected String doPostCommitProcessing(FacesContext context, String outcome)
   {
      outcome = super.doPostCommitProcessing(context, outcome);
      
      if (this.reDisplayForums)
      {
         return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME +
                AlfrescoNavigationHandler.OUTCOME_SEPARATOR + "forumsDeleted";
      }
      else
      {
         return outcome;
      }
   }
}
