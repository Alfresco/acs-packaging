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
package org.alfresco.web.bean.spaces;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;

/**
 * Dialog bean to edit an existing space.
 *
 * @author gavinc
 */
public class EditSpaceDialog extends CreateSpaceDialog
{
   private static final long serialVersionUID = 6090397957979372269L;
   
   protected Node editableNode;

   @Override
   public void init(Map<String, String> parameters)
   {
      super.init(parameters);

      // setup the space being edited
      this.editableNode = initEditableNode();
      this.spaceType = this.editableNode.getType().toString();
   }

   @Override
   public boolean getFinishButtonDisabled()
   {
      return false;
   }

   /**
    * Init the editable Node
    */
   protected Node initEditableNode()
   {
      return new Node(this.browseBean.getActionSpace().getNodeRef());
   }

   @Override
   public String getFinishButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), "ok");
   }

   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      // update the existing node in the repository
      NodeRef nodeRef = this.editableNode.getNodeRef();
      Map<String, Object> editedProps = this.editableNode.getProperties();

      // handle the name property separately, perform a rename in case it changed
      String name = (String)editedProps.get(ContentModel.PROP_NAME);
      if (name != null)
      {
         this.getFileFolderService().rename(nodeRef, name);
      }

      // build the properties to add to the repository
      Map<QName, Serializable> repoProps = new HashMap<QName, Serializable>(7);

      // overwrite the current properties with the edited ones
      Iterator<String> iterProps = editedProps.keySet().iterator();
      while (iterProps.hasNext())
      {
         String propName = iterProps.next();
         QName qname = QName.createQName(propName);

         // make sure the property is represented correctly
         Serializable propValue = (Serializable)editedProps.get(propName);

         // check for empty strings when using number types, set to null in this case
         if ((propValue != null) && (propValue instanceof String) &&
             (propValue.toString().length() == 0))
         {
            PropertyDefinition propDef = this.getDictionaryService().getProperty(qname);
            if (propDef != null)
            {
               if (propDef.getDataType().getName().equals(DataTypeDefinition.DOUBLE) ||
                   propDef.getDataType().getName().equals(DataTypeDefinition.FLOAT) ||
                   propDef.getDataType().getName().equals(DataTypeDefinition.INT) ||
                   propDef.getDataType().getName().equals(DataTypeDefinition.LONG))
               {
                  propValue = null;
               }
            }
         }

         repoProps.put(qname, propValue);
      }

      // add the new properties back to the repository
      this.getNodeService().addProperties(nodeRef, repoProps);

      // we also need to persist any association changes that may have been made

      // add any associations added in the UI
      Map<String, Map<String, AssociationRef>> addedAssocs = this.editableNode.getAddedAssociations();
      for (Map<String, AssociationRef> typedAssoc : addedAssocs.values())
      {
         for (AssociationRef assoc : typedAssoc.values())
         {
            this.getNodeService().createAssociation(assoc.getSourceRef(), assoc.getTargetRef(), assoc.getTypeQName());
         }
      }

      // remove any association removed in the UI
      Map<String, Map<String, AssociationRef>> removedAssocs = this.editableNode.getRemovedAssociations();
      for (Map<String, AssociationRef> typedAssoc : removedAssocs.values())
      {
         for (AssociationRef assoc : typedAssoc.values())
         {
            this.getNodeService().removeAssociation(assoc.getSourceRef(), assoc.getTargetRef(), assoc.getTypeQName());
         }
      }

      // add any child associations added in the UI
      Map<String, Map<String, ChildAssociationRef>> addedChildAssocs = this.editableNode.getAddedChildAssociations();
      for (Map<String, ChildAssociationRef> typedAssoc : addedChildAssocs.values())
      {
         for (ChildAssociationRef assoc : typedAssoc.values())
         {
            this.getNodeService().addChild(assoc.getParentRef(), assoc.getChildRef(), assoc.getTypeQName(), assoc.getTypeQName());
         }
      }

      // remove any child association removed in the UI
      Map<String, Map<String, ChildAssociationRef>> removedChildAssocs = this.editableNode.getRemovedChildAssociations();
      for (Map<String, ChildAssociationRef> typedAssoc : removedChildAssocs.values())
      {
         for (ChildAssociationRef assoc : typedAssoc.values())
         {
            this.getNodeService().removeChild(assoc.getParentRef(), assoc.getChildRef());
         }
      }

      // do nothing by default, subclasses can override if necessary
      return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
   }

   @Override
   protected String doPostCommitProcessing(FacesContext context, String outcome)
   {
      this.browseBean.getActionSpace().reset();

      return outcome;
   }


   // ------------------------------------------------------------------------------
   // Bean getters and setters

   /**
    * Returns the node being edited
    *
    * @return The node being edited
    */
   public Node getEditableNode()
   {
      return this.editableNode;
   }

}
