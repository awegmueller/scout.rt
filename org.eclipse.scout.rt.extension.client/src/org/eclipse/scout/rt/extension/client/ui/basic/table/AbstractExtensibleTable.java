/*******************************************************************************
 * Copyright (c) 2012 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.extension.client.ui.basic.table;

import java.util.List;

import org.eclipse.scout.commons.annotations.Replace;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.extension.client.ExtensionUtility;
import org.eclipse.scout.rt.extension.client.IExtensibleScoutObject;
import org.eclipse.scout.rt.extension.client.ui.action.menu.MenuExtensionUtility;

/**
 * Table supporting the following Scout extension features:
 * <ul>
 * <li>adding, removing and modifying statically configured menus</li>
 * <li>{@link Replace} annotation on columns and menus</li>
 * </ul>
 * 
 * @since 3.9.0
 */
public abstract class AbstractExtensibleTable extends AbstractTable implements IExtensibleScoutObject {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractExtensibleTable.class);

  public AbstractExtensibleTable() {
    super();
  }

  public AbstractExtensibleTable(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected void injectMenusInternal(List<IMenu> menuList) {
    super.injectMenusInternal(menuList);
    Object enclosingObject = ExtensionUtility.getEnclosingObject(this);
    if (enclosingObject != null) {
      MenuExtensionUtility.adaptMenus(enclosingObject, this, menuList);
    }
  }
}
