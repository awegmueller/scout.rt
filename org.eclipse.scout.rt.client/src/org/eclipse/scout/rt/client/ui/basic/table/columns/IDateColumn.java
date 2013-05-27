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
package org.eclipse.scout.rt.client.ui.basic.table.columns;

import java.util.Date;

public interface IDateColumn extends IColumn<Date> {
  double MILLIS_PER_DAY = 1000.0 * 3600.0 * 24.0;

  void setFormat(String s);

  String getFormat();

  boolean isHasDate();

  void setHasDate(boolean b);

  boolean isHasTime();

  void setHasTime(boolean b);

  void setAutoTimeMillis(long l);

  void setAutoTimeMillis(int hour, int minute, int second);

  long getAutoTimeMillis();

}
