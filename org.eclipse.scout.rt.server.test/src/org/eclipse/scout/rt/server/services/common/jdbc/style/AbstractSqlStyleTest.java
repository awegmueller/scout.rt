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
package org.eclipse.scout.rt.server.services.common.jdbc.style;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.easymock.EasyMock;
import org.eclipse.scout.rt.server.services.common.jdbc.SqlBind;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author awe, msc
 */
public class AbstractSqlStyleTest {

  AbstractSqlStyle sql = new AbstractSqlStyle() {
    private static final long serialVersionUID = 1L;

    @Override
    public void testConnection(Connection conn) throws SQLException {
    }

    @Override
    public boolean isBlobEnabled() {
      return false;
    }

    @Override
    public boolean isClobEnabled() {
      return false;
    }

    @Override
    public boolean isLargeString(String s) {
      return false;
    }

    @Override
    protected int getMaxListSize() {
      return 0;
    }
  };

  @Test
  public void testWriteBind() throws SQLException {
    BigDecimal bd = new BigDecimal("9.123");
    SqlBind bind = new SqlBind(Types.DECIMAL, bd);
    PreparedStatement ps = EasyMock.createMock(PreparedStatement.class);
    ps.setObject(1, bd, Types.DECIMAL, 3);
    EasyMock.expectLastCall();
    EasyMock.replay(ps);
    sql.writeBind(ps, 1, bind);
  }

  @Test
  public void testBuildBindFor() throws Exception {
    Character c = Character.valueOf('x');
    SqlBind bin = sql.buildBindFor(c, Character.class);
    Assert.assertEquals(Types.VARCHAR, bin.getSqlType());
    Assert.assertTrue(bin.getValue() instanceof String);
  }

}
