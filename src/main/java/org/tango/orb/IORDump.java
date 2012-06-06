/**
 * Copyright (C) :     2012
 *
 * 	Synchrotron Soleil
 * 	L'Orme des merisiers
 * 	Saint Aubin
 * 	BP48
 * 	91192 GIF-SUR-YVETTE CEDEX
 *
 * This file is part of Tango.
 *
 * Tango is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tango is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Tango.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tango.orb;

import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.jacorb.orb.ParsedIOR;
import org.jacorb.orb.iiop.IIOPAddress;
import org.jacorb.orb.iiop.IIOPProfile;
import org.omg.CORBA.ORB;
import org.tango.utils.DevFailedUtils;

import fr.esrf.Tango.DevFailed;

/**
 * Tool to dump an IOR String
 * 
 * @author ABEILLE
 * 
 */
public final class IORDump {
    private final String iorString;
    private final String deviceName;
    private String typeId = "";
    private String iiopVersion = "";
    private String hostName = "";
    private int port = -1;

    public IORDump(final String devname, final String iorString) throws DevFailed {
	deviceName = devname;
	this.iorString = iorString;
	iorAnalysis();
    }

    @Override
    public String toString() {
	final ReflectionToStringBuilder reflectionToStringBuilder = new ReflectionToStringBuilder(this,
		ToStringStyle.SHORT_PREFIX_STYLE);
	return reflectionToStringBuilder.toString();
    }

    /**
     * Make the IOR analyse
     */
    @SuppressWarnings("unchecked")
    private void iorAnalysis() throws DevFailed {
	if (!iorString.startsWith("IOR:")) {
	    DevFailedUtils.throwDevFailed("CORBA_ERROR", "not an IOR");
	}
	final ORB orb = ORBManager.getOrb();
	final ParsedIOR pior = new ParsedIOR((org.jacorb.orb.ORB) orb, iorString);
	final org.omg.IOP.IOR ior = pior.getIOR();
	typeId = ior.type_id;
	final List<IIOPProfile> profiles = pior.getProfiles();
	for (int i = 0; i < profiles.size(); i++) {
	    final IIOPProfile p = profiles.get(i).to_GIOP_1_0();
	    iiopVersion = (int) p.version().major + "." + (int) p.version().minor;
	    final String name = ((IIOPAddress) p.getAddress()).getOriginalHost();
	    java.net.InetAddress iadd = null;
	    try {
		iadd = java.net.InetAddress.getByName(name);
	    } catch (final UnknownHostException e) {
		DevFailedUtils.throwDevFailed(e);
	    }
	    hostName = iadd.getHostName();

	    port = ((IIOPAddress) p.getAddress()).getPort();
	    if (port < 0) {
		port += 65536;
	    }
	}
    }

    public String getTypeId() {
	return typeId;
    }

    public String getHostName() {
	return hostName;
    }

    public int getPort() {
	return port;
    }

    public String getIIOPVersion() {
	return iiopVersion;
    }

    public String getDeviceName() {
	return deviceName;
    }
}
