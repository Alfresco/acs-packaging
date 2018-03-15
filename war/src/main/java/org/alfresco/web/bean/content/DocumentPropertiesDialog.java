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
package org.alfresco.web.bean.content;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.transaction.UserTransaction;

import org.springframework.extensions.config.Config;
import org.springframework.extensions.config.ConfigService;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.data.QuickSort;
import org.alfresco.web.ui.common.Utils;

/**
 * Backing bean for the edit document properties dialog
 * 
 * @author gavinc
 */
public class DocumentPropertiesDialog implements Serializable
{
   private static final long serialVersionUID = 3065164840163513872L;
   
   private static final String TEMP_PROP_MIMETYPE = "mimetype";
   private static final String TEMP_PROP_ENCODING = "encoding";
   
   transient private NodeService nodeService;
   transient private FileFolderService fileFolderService;
   transient private DictionaryService dictionaryService;
   
   protected BrowseBean browseBean;
   private List<SelectItem> contentTypes;
   private Node editableNode;
   private Boolean hasOtherProperties;
   
   /**
    * Returns the node being edited
    * 
    * @return The node being edited
    */
   public Node getEditableNode()
   {
      return this.editableNode;
   }
   
   /**
    * Event handler called to setup the document for property editing
    * 
    * @param event The event
    */
   public void setupDocumentForAction(ActionEvent event)
   {
      this.editableNode = new Node(this.browseBean.getDocument().getNodeRef());
      
      // special case for Mimetype - since this is a sub-property of the ContentData object
      // we must extract it so it can be edited in the client, then we check for it later
      // and create a new ContentData object to wrap it and it's associated URL
      ContentData content = (ContentData)this.editableNode.getProperties().get(ContentModel.PROP_CONTENT);
      if (content != null)
      {
         this.editableNode.getProperties().put(TEMP_PROP_MIMETYPE, content.getMimetype());
         this.editableNode.getProperties().put(TEMP_PROP_ENCODING, content.getEncoding());
      }
      
      this.hasOtherProperties = null;
   }
   
   /**
    * Event handler used to save the edited properties back to the repository
    * 
    * @return The outcome
    */
   public String save()
   {
      String outcome = "cancel";
      
      UserTransaction tx = null;
      
      try
      {
         tx = Repository.getUserTransaction(FacesContext.getCurrentInstance());
         tx.begin();
         
         NodeRef nodeRef = this.browseBean.getDocument().getNodeRef();
         Map<String, Object> props = this.editableNode.getProperties();
         
         // get the name and move the node as necessary
         String name = (String) props.get(ContentModel.PROP_NAME);
         if (name != null)
         {
            getFileFolderService().rename(nodeRef, name);
         }
         
         Map<QName, Serializable> properties = getNodeService().getProperties(nodeRef);
         // we need to put all the properties from the editable bag back into 
         // the format expected by the repository
         
         // Deal with the special mimetype property for ContentData
         String mimetype = (String)props.get(TEMP_PROP_MIMETYPE);
         if (mimetype != null)
         {
            // remove temporary prop from list so it isn't saved with the others
            props.remove(TEMP_PROP_MIMETYPE);
            ContentData contentData = (ContentData)props.get(ContentModel.PROP_CONTENT);
            if (contentData != null)
            {
               contentData = ContentData.setMimetype(contentData, mimetype);
               props.put(ContentModel.PROP_CONTENT.toString(), contentData);
            }
         }
         
         // Deal with the special encoding property for ContentData
         String encoding = (String) props.get(TEMP_PROP_ENCODING);
         if (encoding != null)
         {
            // remove temporary prop from list so it isn't saved with the others
            props.remove(TEMP_PROP_ENCODING);
            ContentData contentData = (ContentData)props.get(ContentModel.PROP_CONTENT);
            if (contentData != null)
            {
               contentData = ContentData.setEncoding(contentData, encoding);
               props.put(ContentModel.PROP_CONTENT.toString(), contentData);
            }
         }
         
         // extra and deal with the Author prop if the aspect has not been applied yet
         String author = (String)props.get(ContentModel.PROP_AUTHOR);
         if (author != null && author.length() != 0)
         {
            // add aspect if required
            if (getNodeService().hasAspect(nodeRef, ContentModel.ASPECT_AUTHOR) == false)
            {
               Map<QName, Serializable> authorProps = new HashMap<QName, Serializable>(1, 1.0f);
               authorProps.put(ContentModel.PROP_AUTHOR, author);
               getNodeService().addAspect(nodeRef, ContentModel.ASPECT_AUTHOR, authorProps);
            }
            // else it will get updated in the later setProperties() call
         }
         
         // deal with adding the "titled" aspect if required
         String title = (String)props.get(ContentModel.PROP_TITLE);
         String description = (String)props.get(ContentModel.PROP_DESCRIPTION);
         if (title != null || description != null)
         {
            // add the aspect to be sure it's present
            getNodeService().addAspect(nodeRef, ContentModel.ASPECT_TITLED, null);
            // props will get added later in setProperties()
         }
         
         // add the remaining properties
         Iterator<String> iterProps = props.keySet().iterator();
         while (iterProps.hasNext())
         {
            String propName = iterProps.next();
            QName qname = QName.createQName(propName);
            
            // make sure the property is represented correctly
            Serializable propValue = (Serializable)props.get(propName);
            
            // check for empty strings when using number types, set to null in this case
            if ((propValue != null) && (propValue instanceof String) && 
                (propValue.toString().length() == 0))
            {
               PropertyDefinition propDef = getDictionaryService().getProperty(qname);
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
            
            properties.put(qname, propValue);
         }
         
         // send the properties back to the repository
         getNodeService().setProperties(this.browseBean.getDocument().getNodeRef(), properties);
         
         // we also need to persist any association changes that may have been made
         
         // add any associations added in the UI
         Map<String, Map<String, AssociationRef>> addedAssocs = this.editableNode.getAddedAssociations();
         for (Map<String, AssociationRef> typedAssoc : addedAssocs.values())
         {
            for (AssociationRef assoc : typedAssoc.values())
            {
               getNodeService().createAssociation(assoc.getSourceRef(), assoc.getTargetRef(), assoc.getTypeQName());
            }
         }
         
         // remove any association removed in the UI
         Map<String, Map<String, AssociationRef>> removedAssocs = this.editableNode.getRemovedAssociations();
         for (Map<String, AssociationRef> typedAssoc : removedAssocs.values())
         {
            for (AssociationRef assoc : typedAssoc.values())
            {
               getNodeService().removeAssociation(assoc.getSourceRef(), assoc.getTargetRef(), assoc.getTypeQName());
            }
         }
         
         // add any child associations added in the UI
         Map<String, Map<String, ChildAssociationRef>> addedChildAssocs = this.editableNode.getAddedChildAssociations();
         for (Map<String, ChildAssociationRef> typedAssoc : addedChildAssocs.values())
         {
            for (ChildAssociationRef assoc : typedAssoc.values())
            {
               getNodeService().addChild(assoc.getParentRef(), assoc.getChildRef(), assoc.getTypeQName(), assoc.getTypeQName());
            }
         }
         
         // remove any child association removed in the UI
         Map<String, Map<String, ChildAssociationRef>> removedChildAssocs = this.editableNode.getRemovedChildAssociations();
         for (Map<String, ChildAssociationRef> typedAssoc : removedChildAssocs.values())
         {
            for (ChildAssociationRef assoc : typedAssoc.values())
            {
               getNodeService().removeChild(assoc.getParentRef(), assoc.getChildRef());
            }
         }
         
         // commit the transaction
         tx.commit();
         
         // set the outcome to refresh
         outcome = "finish";
         
         // reset the document held by the browse bean as it's just been updated
         this.browseBean.getDocument().reset();
      }
      catch (FileExistsException e)
      {
         // rollback the transaction
         try { if (tx != null) {tx.rollback();} } catch (Exception ex) {}
         // print status message  
         String statusMsg = MessageFormat.format(
               Application.getMessage(
                     FacesContext.getCurrentInstance(), "error_exists"), 
                     e.getName());
         Utils.addErrorMessage(statusMsg);
         // no outcome
         outcome = null;
      }
      catch (InvalidNodeRefException err)
      {
         // rollback the transaction
         try { if (tx != null) {tx.rollback();} } catch (Exception ex) {}
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {this.browseBean.getDocument().getId()}) );
         // this failure means the node no longer exists - we cannot show the doc properties screen
         outcome = "browse";
      }
      catch (Throwable e)
      {
         // rollback the transaction
         try { if (tx != null) {tx.rollback();} } catch (Exception ex) {}
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), e.getMessage()), e);
      }
      
      return outcome;
   }
   
   public Map<String, Object> getProperties()
   {
      return this.editableNode.getProperties();
   }
   
   /**
    * @return Returns a list of content types to allow the user to select from
    */
   public List<SelectItem> getContentTypes()
   {
      if (this.contentTypes == null)
      {
         this.contentTypes = new ArrayList<SelectItem>(80);
         ServiceRegistry registry = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
         MimetypeService mimetypeService = registry.getMimetypeService();
         
         // get the mime type display names
         Map<String, String> mimeTypes = mimetypeService.getDisplaysByMimetype();
         for (String mimeType : mimeTypes.keySet())
         {
            this.contentTypes.add(new SelectItem(mimeType, mimeTypes.get(mimeType)));
         }
         
         // make sure the list is sorted by the values
         QuickSort sorter = new QuickSort(this.contentTypes, "label", true, IDataContainer.SORT_CASEINSENSITIVE);
         sorter.sort();
      }
      
      return this.contentTypes;
   }
   
   /**
    * Determines whether this document has any other properties other than the 
    * default set to display to the user.
    * 
    * @return true of there are properties to show, false otherwise
    */
   public boolean getOtherPropertiesPresent()
   {
      if ((this.hasOtherProperties == null) || (Application.isDynamicConfig(FacesContext.getCurrentInstance())))
      {
         // we need to use the config service to see whether there are any
         // editable properties configured for this document.
         ConfigService configSvc = Application.getConfigService(FacesContext.getCurrentInstance());
         Config configProps = configSvc.getConfig(this.editableNode);
         PropertySheetConfigElement propsToDisplay = (PropertySheetConfigElement)configProps.
               getConfigElement("property-sheet");
         
         if (propsToDisplay != null && propsToDisplay.getEditableItemNamesToShow().size() > 0)
         {
            this.hasOtherProperties = Boolean.TRUE;
         }
         else
         {
            this.hasOtherProperties = Boolean.FALSE;
         }
      }
      
      return this.hasOtherProperties.booleanValue();
   }
   
   /**
    * @return Returns the nodeService.
    */
   public NodeService getNodeService()
   {
      if (nodeService == null)
      {
         nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
      }
      
      return this.nodeService;
   }

   /**
    * @param nodeService The nodeService to set.
    */
   public void setNodeService(NodeService nodeService)
   {
      this.nodeService = nodeService;
   }
   
   /**
    * @param fileFolderService the file and folder model-specific functions
    */
   public void setFileFolderService(FileFolderService fileFolderService)
   {
      this.fileFolderService = fileFolderService;
   }
   
   protected FileFolderService getFileFolderService()
   {
      if (fileFolderService == null)
      {
         fileFolderService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getFileFolderService();
      }
      return fileFolderService;
   }

   /**
    * Sets the DictionaryService to use when persisting metadata
    * 
    * @param dictionaryService The DictionaryService
    */
   public void setDictionaryService(DictionaryService dictionaryService)
   {
      this.dictionaryService = dictionaryService;
   }
   
   protected DictionaryService getDictionaryService()
   {
      if (dictionaryService == null)
      {
         dictionaryService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getDictionaryService();
      }
      return dictionaryService;
   }

   /**
    * @return The BrowseBean
    */
   public BrowseBean getBrowseBean()
   {
      return this.browseBean;
   }

   /**
    * @param browseBean The BrowseBean to set.
    */
   public void setBrowseBean(BrowseBean browseBean)
   {
      this.browseBean = browseBean;
   }
}
