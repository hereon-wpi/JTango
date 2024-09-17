//+======================================================================
// $Source$
//
// Project:   Tango
//
// Description:  java source code for the TANGO client/server API.
//
// $Author: pascal_verdier $
//
// Copyright (C) :      2004,2005,2006,2007,2008,2009,2010,2011,2012,2013,2014,
//						European Synchrotron Radiation Facility
//                      BP 220, Grenoble 38043
//                      FRANCE
//
// This file is part of Tango.
//
// Tango is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// Tango is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License
// along with Tango.  If not, see <http://www.gnu.org/licenses/>.
//
// $Revision: 25297 $
//
//-======================================================================


package fr.esrf.TangoApi.factory;

import fr.esrf.Tango.factory.ITangoFactory;
import fr.esrf.TangoApi.*;
import org.tango.transport.HttpTransport;
import org.tango.transport.Transport;
import org.tango.transport.ZmqTransport;

public class DefaultTangoFactoryImpl implements ITangoFactory {

	public DefaultTangoFactoryImpl()
	{
	}

	/* (non-Javadoc)
		 * @see fr.esrf.TangoApi.factory.ITangoFactory#getConnectionDAO()
		 */
	public IConnectionDAO getConnectionDAO()
	{
		return new ConnectionDAODefaultImpl();
	}

	/* (non-Javadoc)
		 * @see fr.esrf.TangoApi.factory.ITangoFactory#getDeviceProxyDAO()
		 */
	public IDeviceProxyDAO getDeviceProxyDAO()
	{
		return new DeviceProxyDAODefaultImpl();
	}

	/* (non-Javadoc)
		 * @see fr.esrf.TangoApi.factory.ITangoFactory#getDatabaseDAO()
		 */
	public IDatabaseDAO getDatabaseDAO()
	{
		try
		{
		return new DatabaseDAODefaultImpl();
		}catch(Exception e)
		{
			// we may not have exception we build  DatabaseDAODefaultImpl but ...
			e.printStackTrace();
			return null;
		}
	}

	/* (non-Javadoc)
		 * @see fr.esrf.TangoApi.factory.ITangoFactory#getDeviceAttributeDAO()
		 */
	public IDeviceAttributeDAO getDeviceAttributeDAO()
	{
		return new DeviceAttributeDAODefaultImpl();
	}

	/* (non-Javadoc)
		 * @see fr.esrf.TangoApi.factory.ITangoFactory#getDeviceAttribute_3DAO()
		 */
	public IDeviceAttribute_3DAO getDeviceAttribute_3DAO()
	{
		return new DeviceAttribute_3DAODefaultImpl();
	}

	/* (non-Javadoc)
		 * @see fr.esrf.TangoApi.factory.ITangoFactory#getDeviceDataDAO()
		 */
	public IDeviceDataDAO getDeviceDataDAO()
	{
		return new DeviceDataDAODefaultImpl();
	}

	/* (non-Javadoc)
		 * @see fr.esrf.TangoApi.factory.ITangoFactory#getDeviceDataHistoryDAO()
		 */
	public IDeviceDataHistoryDAO getDeviceDataHistoryDAO()
	{
		return new DeviceDataHistoryDAODefaultImpl();
	}

	/* (non-Javadoc)
		 * @see fr.esrf.TangoApi.factory.ITangoFactory#getApiUtilDAO()
		 */
	public IApiUtilDAO getApiUtilDAO()
	{
		return new ApiUtilDAODefaultImpl();
	}

	/* (non-Javadoc)
		 * @see fr.esrf.TangoApi.factory.ITangoFactory#getIORDumpDAO()
		 */
	public IIORDumpDAO getIORDumpDAO()
	{
		return new IORDumpDAODefaultImpl();
	}

	public String getFactoryName()
	{
		return "TANGORB Default";
	}

    public Transport newTransport(String targetProtocol) {
        switch (targetProtocol) {
            case "zmq":
                return new ZmqTransport();
            case "http":
                return new HttpTransport();
            default:
                throw new IllegalArgumentException("Unsupported target transport: " + targetProtocol);
        }
    }

}
