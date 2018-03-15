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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.web.scripts.FileTypeImageUtils;
import org.alfresco.service.cmr.ml.ContentFilterLanguagesService;
import org.alfresco.service.cmr.ml.EditionService;
import org.alfresco.service.cmr.ml.MultilingualContentService;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.extensions.surf.util.ParameterCheck;

/**
 * Bean with generic function helping the rendering of the versioned properties
 *
 * @author Yanick Pignot
 */
public class VersionedDocumentDetailsDialog implements Serializable
{
    private static final long serialVersionUID = 1575175076564595262L;
   
    /** Dependencies */
	 transient private VersionService versionService;
	 transient private EditionService editionService;
	 transient private NodeService nodeService;
	 transient private MultilingualContentService multilingualContentService;
	 transient private ContentFilterLanguagesService contentFilterLanguagesService;

    /** Determine if the version is a translation of a old edition */
    private boolean fromPreviousEditon;

    /** The version selected by the user */
    private Version documentVersion;
    private VersionHistory versionHistory;

    /** The multilingual information of the selected version selected by the user */
    private Version documentEdition;
    private VersionHistory editionHistory;
    
    /** Common property resolvers accessed from the BrowseBean */
    private BrowseBean browseBean;


    public void init()
    {
        fromPreviousEditon = false;
        documentVersion = null;
        versionHistory = null;
        documentEdition = null;
        editionHistory = null;
    }
    
    /**
     * @param browseBean The BrowseBean to set.
     */
    public void setBrowseBean(BrowseBean browseBean)
    {
        this.browseBean = browseBean;
    }

    /**
     * Set which version of the current node that the user want to display the properties
     */
    public void setBrowsingVersion(ActionEvent event)
    {
       init();

       // Get the properties of the action event
       UIActionLink link = (UIActionLink)event.getComponent();
       Map<String, String> params = link.getParameterMap();

       String versionLabel = params.get("versionLabel");
       String id = params.get("id");
       String lang = params.get("lang");

       setBrowsingVersion(id, versionLabel, lang);
    }


    /**
     * Implementation of setBrowsingVersion action event to be use with the needed parameters.
     */
    private void setBrowsingVersion(String id, String versionLabel, String lang)
    {
	   // test if the mandatories parameter are valid
	   ParameterCheck.mandatoryString("The id of the node", id);
	   ParameterCheck.mandatoryString("The version of the node", versionLabel);

       try
       {
    	   // try to get the nodeRef with the given ID. This node is not a versioned node.
    	   NodeRef currentNodeRef = new NodeRef(Repository.getStoreRef(), id);

    	   // the threatment is different if the node is a translation or a mlContainer
    	   if(getNodeService().getType(currentNodeRef).equals(ContentModel.TYPE_MULTILINGUAL_CONTAINER))
    	   {
               //  test if the lang parameter is valid
               ParameterCheck.mandatoryString("The lang of the node", lang);

               fromPreviousEditon = true;

               versionLabel = cleanVersionLabel(versionLabel);

               // set the edition information of the mlContainer of the selected translation version
               this.editionHistory  = getEditionService().getEditions(currentNodeRef);
               this.documentEdition = editionHistory.getVersion(versionLabel);

               // set the version to display
               this.documentVersion = getBrowsingVersionForMLContainer(currentNodeRef, versionLabel, lang);
    	   }
    	   else
    	   {
               fromPreviousEditon = false;

               // set the version history
               this.versionHistory = getVersionService().getVersionHistory(currentNodeRef);
               // set the version to display
               this.documentVersion = getBrowsingVersionForDocument(currentNodeRef, versionLabel);
    	   }
       }
       catch (InvalidNodeRefException refErr)
       {
          Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
        		  FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {id}) );
       }
    }

   /**
    * Navigates to next item in the list of versioned content for the current VersionHistory
    */
   public void nextItem(ActionEvent event)
   {
       // Get the properties of the action event
       UIActionLink link = (UIActionLink)event.getComponent();
       Map<String, String> params = link.getParameterMap();

       String versionLabel = params.get("versionLabel");

       // if the version is not specified, get the next version
       if(versionLabel == null)
       {
           List<Version> nextVersions = new ArrayList<Version>(this.versionHistory.getSuccessors(this.documentVersion));

           // if the version history doesn't contains successor, get the root version (the first one)
           if(nextVersions == null || nextVersions.size() < 1)
           {
               this.documentVersion = versionHistory.getRootVersion();
           }
           else
           {
               this.documentVersion = nextVersions.get(0);
           }
       }
       else
       {
           this.documentVersion = this.versionHistory.getVersion(versionLabel);
       }
   }

   /**
    * Navigates to previous item in the list of versioned content for the current VersionHistory
    */
   public void previousItem(ActionEvent event)
   {
       // Get the properties of the action event
       UIActionLink link = (UIActionLink)event.getComponent();
       Map<String, String> params = link.getParameterMap();

       String versionLabel = params.get("versionLabel");

       // if the version is not specified, get the next version
       if(versionLabel == null)
       {
           Version prevVersion = this.versionHistory.getPredecessor(this.documentVersion);

           // if the version history doesn't contains predecessor, get the last version (ie. most recent)
           if(prevVersion == null)
           {
               List<Version> allVersions = new ArrayList<Version>(this.versionHistory.getAllVersions());
               this.documentVersion = allVersions.get(0);
           }
           else
           {
               this.documentVersion = prevVersion;
           }
       }
       else
       {
           this.documentVersion = this.versionHistory.getVersion(versionLabel);
       }
   }

   /**
    * Returns a list of objects representing the translations of the given version of the mlContainer
    *
    * @return List of translations
    */
    @SuppressWarnings("unchecked")
    public List getTranslations()
    {
       // get the version of the mlContainer and its translations
       List<VersionHistory> translationsList = getEditionService().getVersionedTranslations(this.documentEdition);

       Map<Locale, NodeRef> translationNodeRef;

       // if translation size == 0, the edition is the current edition and the translations are not yet attached.
       if(translationsList.size() == 0)
       {
           // the selection edition is the current: use the multilingual content service
           translationNodeRef = getMultilingualContentService().getTranslations(this.documentEdition.getVersionedNodeRef());
       }
       else
       {
           translationNodeRef = new HashMap<Locale, NodeRef>(translationsList.size());

           // get the last (most recent) version of the translation in the given lang of the edition
           for (VersionHistory history : translationsList)
           {
               //   get the list of versions (in descending order - ie. most recent first)
               List<Version> orderedVersions = new ArrayList<Version>(history.getAllVersions());

               // the last (most recent) version is the first version of the list
               Version lastVersion = orderedVersions.get(0);

               // fill the list of translation
               if(lastVersion != null)
               {
                   NodeRef versionNodeRef = lastVersion.getFrozenStateNodeRef();
                   Locale locale = (Locale) getNodeService().getProperty(versionNodeRef, ContentModel.PROP_LOCALE);
                   translationNodeRef.put(locale, versionNodeRef);
               }
           }
       }

       // the list of client-side translation to return
       List<MapNode> translations = new ArrayList<MapNode>(translationNodeRef.size());

       for (Map.Entry<Locale, NodeRef> entry : translationNodeRef.entrySet())
       {
           Locale locale  = entry.getKey();
           NodeRef nodeRef = entry.getValue();

           //  create a map node representation of the translation
           MapNode mapNode = new MapNode(nodeRef);

           String lgge = (locale != null) ?
                 // convert the locale into new ISO codes
                 getContentFilterLanguagesService().convertToNewISOCode(locale.getLanguage()).toUpperCase()
                 : null;

           mapNode.put("name", getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME));
           mapNode.put("language", lgge);
           mapNode.put("url", DownloadContentServlet.generateBrowserURL(nodeRef, mapNode.getName()));

           mapNode.put("notEmpty", new Boolean(!getNodeService().hasAspect(nodeRef, ContentModel.ASPECT_MULTILINGUAL_EMPTY_TRANSLATION)));

           // add the client side version to the list
           translations.add(mapNode);

       }

       return translations;
    }


    /**
     * Returns a list of objects representing the versions of the
     * current document
     *
     * @return List of previous versions
     */
    public List getVersionHistory()
    {
       List<MapNode> versions = new ArrayList<MapNode>();

       for (Version version : this.versionHistory.getAllVersions())
       {
          // create a map node representation of the version
          MapNode clientVersion = new MapNode(version.getFrozenStateNodeRef());
          clientVersion.put("versionLabel", version.getVersionLabel());
          clientVersion.put("notes", version.getDescription());
          clientVersion.put("author", version.getCreator());
          clientVersion.put("versionDate", version.getCreatedDate());

          if(getFrozenStateDocument().hasAspect(ContentModel.ASPECT_MULTILINGUAL_EMPTY_TRANSLATION))
          {
              clientVersion.put("url", null);
          }
          else
          {
              clientVersion.put("url", DownloadContentServlet.generateBrowserURL(version.getFrozenStateNodeRef(),
                      clientVersion.getName()));
          }

          // add the client side version to the list
          versions.add(clientVersion);
       }

       return versions;
    }

   /**
    * @return true if the version is a translation of a previous edition
    */
   public boolean isFromPreviousEditon()
   {
       return fromPreviousEditon;
   }

   /**
    * Returns the URL to download content for the current document
    *
    * @return Content url to download the current document
    */
   public String getUrl()
   {
       return DownloadContentServlet.generateBrowserURL(getFrozenStateNodeRef(), getName());
   }

   /**
    * @return the versioned node selected by the user
    */
   public Node getFrozenStateDocument()
   {
       Node node = new Node(getFrozenStateNodeRef());
       node.addPropertyResolver("mimetype", this.browseBean.resolverMimetype);
       node.addPropertyResolver("encoding", this.browseBean.resolverEncoding);
       node.addPropertyResolver("size", this.browseBean.resolverSize);
       return node;
   }

   /**
    * @return the versioned node ref selected by the user
    */
   public NodeRef getFrozenStateNodeRef()
   {
       return documentVersion.getFrozenStateNodeRef();
   }

   /**
    * @return the edition of the mlContainer of the selected verion of the translation
    */
   public Node getMultilingualContainerDocument()
   {
       return new Node(documentEdition.getFrozenStateNodeRef());
   }

  /**
   * @return the name of selected version
   */
   public String getName()
   {
      String name  = (String)getNodeService().getProperty(getFrozenStateNodeRef(), ContentModel.PROP_NAME);
      return name;
   }

   /**
    * @return the file type image URL of the version
    */
   public String getFileType32()
   {
       String fileType = FileTypeImageUtils.getFileTypeImage(getName(), false);
       return fileType;
   }

   public boolean isEmptyTranslation()
   {
       return getNodeService().hasAspect(getFrozenStateNodeRef(), ContentModel.ASPECT_MULTILINGUAL_EMPTY_TRANSLATION);
   }

   /**
    * @return the version to display of the document selected by the user
    */
   public Version getVersion()
   {
       return this.documentVersion;
   }

   /**
    * @return the next page to display according to which page the dialog is coming from
    */
   public String getOutcome()
   {
       return (this.fromPreviousEditon) ? "dialog:showMLContainerDetails" :  "dialog:showDocDetails";
   }

   /**
    * Util method which remove the eventual '(actual)'  label from the version label.
    */
   private String cleanVersionLabel(String versionLabel)
   {
       // remove the (actual vesrion) label if needed
       int idx = versionLabel.indexOf(' ');
       if(idx > -1)
       {
           versionLabel = versionLabel.substring(0, idx);
       }
       return versionLabel;
   }

   /**
    * Util method which returns the given version of a node
    */
   private Version getBrowsingVersionForDocument(NodeRef document, String versionLabel)
   {
	   return this.getVersionService().getVersionHistory(document).getVersion(versionLabel);
   }

   /**
    * Util method which returns the current version of a node
    */
   private Version getBrowsingCurrentVersionForMLContainer(NodeRef document, String lang)
   {
	   NodeRef translation = getMultilingualContentService().getTranslationForLocale(document, I18NUtil.parseLocale(lang));
	   this.versionHistory = getVersionService().getVersionHistory(translation);

	   return getVersionService().getCurrentVersion(translation);
   }

   /**
    * Util method which return the last (most recent) version of a translation of a given edition of a mlContainer according its language
    */
   @SuppressWarnings("unchecked")
   private Version getBrowsingVersionForMLContainer(NodeRef document, String editionLabel, String lang)
   {
       // get the list of translations of the given edition of the mlContainer
	   List<VersionHistory> translations = getEditionService().getVersionedTranslations(this.documentEdition);

	   // if translation size == 0, the edition is the current edition and the translations are not yet attached.
	   if(translations.size() == 0)
	   {
		   // the selection edition is the current.
		   return getBrowsingCurrentVersionForMLContainer(document, lang);
	   }
	   else
	   {
		   Version versionToReturn = null;

		   // get the last (most recent) version of the translation in the given lang of the edition
		   for (VersionHistory history : translations)
		   {
			   //	get the list of versions (in descending order - ie. most recent first)
	           List<Version> orderedVersions = new ArrayList<Version>(history.getAllVersions());

	           // the last version (ie. most recent) is the first version of the list
	           Version lastVersion = orderedVersions.get(0);

			   if(lastVersion != null)
			   {
				   Map<QName, Serializable> properties = getEditionService().getVersionedMetadatas(lastVersion);
				   Locale locale = (Locale) properties.get(ContentModel.PROP_LOCALE);

				   if(locale.getLanguage().equalsIgnoreCase(lang))
				   {
					   versionToReturn = lastVersion;
                       this.versionHistory = history;
					   break;
				   }
			   }
		   }
		   return versionToReturn;
	   }

   }


  /**
   * @param versionService the Version Service to set
   */
   public void setVersionService(VersionService versionService)
   {
      this.versionService = versionService;
   }
   
   protected VersionService getVersionService()
   {
      if (versionService == null)
      {
         versionService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getVersionService();
      }
      return versionService;
   }

   /**
    * @param editionService the Edition Service to set
	*/
   public void setEditionService(EditionService editionService)
   {
       this.editionService = editionService;
   }
   
   protected EditionService getEditionService()
   {
      if (editionService == null)
      {
         editionService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getEditionService();
      }
      return editionService;
   }

   /**
    * @param nodeService the Node Service to set
    */
   public void setNodeService(NodeService nodeService)
   {
       this.nodeService = nodeService;
   }
   
   protected NodeService getNodeService()
   {
      if (nodeService == null)
      {
         nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
      }
      return nodeService;
   }

   /**
    * @param contentFilterLanguagesService the Content Filter Languages Service to set
    */
   public void setContentFilterLanguagesService(ContentFilterLanguagesService contentFilterLanguagesService)
   {
      this.contentFilterLanguagesService = contentFilterLanguagesService;
   }
   
   protected ContentFilterLanguagesService getContentFilterLanguagesService()
   {
      if (contentFilterLanguagesService == null)
      {
         contentFilterLanguagesService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getContentFilterLanguagesService();
      }
      return contentFilterLanguagesService;
   }

   /**
    * @param multilingualContentService Content Service the Multilingual Content Service to set
    */
   public void setMultilingualContentService(MultilingualContentService multilingualContentService)
   {
      this.multilingualContentService = multilingualContentService;
   }
   
   protected MultilingualContentService getMultilingualContentService()
   {
      if (multilingualContentService == null)
      {
         multilingualContentService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getMultilingualContentService();
      }
      return multilingualContentService;
   }
}