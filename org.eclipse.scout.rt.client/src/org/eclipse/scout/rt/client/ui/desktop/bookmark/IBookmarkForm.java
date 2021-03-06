/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.ui.desktop.bookmark;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.rt.shared.services.common.bookmark.BookmarkFolder;

public interface IBookmarkForm extends IForm {

  /**
   * @param bookmark
   *          the bookmark to be modified
   */
  void setBookmark(Bookmark bookmark);

  /**
   * @return the bookmark for further processing
   */
  Bookmark getBookmark();

  /**
   * Set the top-level folder where the bookmark is located
   * 
   * @param rootFolder
   *          top-level folder
   */
  void setBookmarkRootFolder(BookmarkFolder rootFolder);

  /**
   * @return the top-level folder where the bookmark is located
   */
  BookmarkFolder getBookmarkRootFolder();

  /**
   * @return the folder where the bookmark is located
   * @throws ProcessingException
   */
  BookmarkFolder getFolder() throws ProcessingException;

  /**
   * @param folder
   *          the folder where the bookmark is located
   * @throws ProcessingException
   */
  void setFolder(BookmarkFolder folder) throws ProcessingException;

  /**
   * Opens the form for modification of an existing bookmark
   * 
   * @throws ProcessingException
   */
  void startModify() throws ProcessingException;

  /**
   * Opens the form for creation of a new bookmark
   * 
   * @throws ProcessingException
   */
  void startNew() throws ProcessingException;
}
