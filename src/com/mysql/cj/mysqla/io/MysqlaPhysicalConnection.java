/*
  Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.

  The MySQL Connector/J is licensed under the terms of the GPLv2
  <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most MySQL Connectors.
  There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
  this software, see the FLOSS License Exception
  <http://www.mysql.com/about/legal/licensing/foss-exception.html>.

  This program is free software; you can redistribute it and/or modify it under the terms
  of the GNU General Public License as published by the Free Software Foundation; version 2
  of the License.

  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along with this
  program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
  Floor, Boston, MA 02110-1301  USA

 */

package com.mysql.cj.mysqla.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.Properties;

import com.mysql.cj.api.conf.PropertySet;
import com.mysql.cj.api.exception.ExceptionInterceptor;
import com.mysql.cj.api.io.PhysicalConnection;
import com.mysql.cj.api.log.Log;
import com.mysql.cj.core.conf.PropertyDefinitions;
import com.mysql.cj.core.exception.ExceptionFactory;
import com.mysql.cj.core.io.AbstractPhysicalConnection;
import com.mysql.cj.core.io.ReadAheadInputStream;

public class MysqlaPhysicalConnection extends AbstractPhysicalConnection implements PhysicalConnection {

    @Override
    public void connect(String host, int port, Properties props, PropertySet propertySet, ExceptionInterceptor exceptionInterceptor, Log log) {

        try {
            this.port = port;
            this.host = host;
            this.propertySet = propertySet;
            this.exceptionInterceptor = exceptionInterceptor;

            this.socketFactory = createSocketFactory(propertySet.getStringReadableProperty(PropertyDefinitions.PNAME_socketFactory).getStringValue());
            this.mysqlSocket = this.socketFactory.connect(this.host, this.port, props, DriverManager.getLoginTimeout() * 1000);

            int socketTimeout = propertySet.getIntegerReadableProperty(PropertyDefinitions.PNAME_socketTimeout).getValue();
            if (socketTimeout != 0) {
                try {
                    this.mysqlSocket.setSoTimeout(socketTimeout);
                } catch (Exception ex) {
                    /* Ignore if the platform does not support it */
                }
            }

            this.mysqlSocket = this.socketFactory.beforeHandshake();

            if (propertySet.getBooleanReadableProperty(PropertyDefinitions.PNAME_useReadAheadInput).getValue()) {
                this.mysqlInput = new ReadAheadInputStream(this.mysqlSocket.getInputStream(), 16384, propertySet.getBooleanReadableProperty(
                        PropertyDefinitions.PNAME_traceProtocol).getValue(), log);
            } else if (propertySet.getBooleanReadableProperty(PropertyDefinitions.PNAME_useUnbufferedInput).getValue()) {
                this.mysqlInput = this.mysqlSocket.getInputStream();
            } else {
                this.mysqlInput = new BufferedInputStream(this.mysqlSocket.getInputStream(), 16384);
            }

            this.mysqlOutput = new BufferedOutputStream(this.mysqlSocket.getOutputStream(), 16384);
        } catch (IOException ioEx) {
            throw ExceptionFactory.createCommunicationException(propertySet, null, 0, 0, ioEx, getExceptionInterceptor());
        }
    }

}
