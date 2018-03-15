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
package org.alfresco.web.bean.ajax;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.alfresco.web.app.servlet.ajax.InvokeCommand;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.util.XMLUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Bean managing the ajax servlet upload of a multi-part form containing file content
 * to replace the content of a node within the repository. 
 * 
 * @author Mike Hatfield
 */
public class ContentUpdateBean implements Serializable
{
   private static final long serialVersionUID = -7209326198823950952L;
   
   private static Log logger = LogFactory.getLog(ContentUpdateBean.class);
   
   /**
    * Ajax method to update file content. A multi-part form is required as the input.
    * 
    * "return-page" = javascript to execute on return from the upload request
    * "nodeRef" = the nodeRef of the item to update the content of
    * 
    * @throws Exception
    */
   @InvokeCommand.ResponseMimetype(value=MimetypeMap.MIMETYPE_HTML)
   public void updateFile() throws Exception
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      ExternalContext externalContext = fc.getExternalContext();
      HttpServletRequest request = (HttpServletRequest)externalContext.getRequest();
      
      ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
      upload.setHeaderEncoding("UTF-8");
      
      List<FileItem> fileItems = upload.parseRequest(request);
      String strNodeRef = null;
      String strFilename = null;
      String strReturnPage = null;
      File file = null;
      
      for (FileItem item : fileItems)
      {
         if (item.isFormField() && item.getFieldName().equals("return-page"))
         {
            strReturnPage = item.getString();
         }
         else if (item.isFormField() && item.getFieldName().equals("nodeRef"))
         {
            strNodeRef = item.getString();
         }
         else
         {
            strFilename = FilenameUtils.getName(item.getName());
            file = TempFileProvider.createTempFile("alfresco", ".upload");
            item.write(file);
         }
      }
      
      if (logger.isDebugEnabled())
         logger.debug("Ajax content update request: " + strFilename + " to nodeRef: " + strNodeRef + " return page: " + strReturnPage);
      
      try
      {
         if (file != null && strNodeRef != null && strNodeRef.length() != 0)
         {
            NodeRef nodeRef = new NodeRef(strNodeRef);
            if (nodeRef != null)
            {
               ServiceRegistry services = Repository.getServiceRegistry(fc);
               
               // get a writer for the content and put the file
               ContentWriter writer = services.getContentService().getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
               writer.putContent(file);
            }
         }
      }
      catch (Exception e)
      {
         strReturnPage = strReturnPage.replace("${UPLOAD_ERROR}", e.getMessage());
      }

      Document result = XMLUtil.newDocument();
      Element htmlEl = result.createElement("html");
      result.appendChild(htmlEl);
      Element bodyEl = result.createElement("body");
      htmlEl.appendChild(bodyEl);

      Element scriptEl = result.createElement("script");
      bodyEl.appendChild(scriptEl);
      scriptEl.setAttribute("type", "text/javascript");
      Node scriptText = result.createTextNode(strReturnPage);
      scriptEl.appendChild(scriptText);

      if (logger.isDebugEnabled())
         logger.debug("Content update request complete.");
      
      ResponseWriter out = fc.getResponseWriter();
      XMLUtil.print(result, out);
   }
}
