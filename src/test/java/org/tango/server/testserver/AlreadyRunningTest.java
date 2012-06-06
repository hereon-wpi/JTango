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
package org.tango.server.testserver;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Test;
import org.tango.orb.ORBManager;
import org.tango.server.ServerManager;

import fr.esrf.Tango.DevFailed;

public class AlreadyRunningTest {

    // XXX: server must be declared in tango db before running this test

    @Test(expected = DevFailed.class)
    public void testDB() throws DevFailed {
	assertThat(System.getProperty("TANGO_HOST"), notNullValue());
	JTangoTest.start();
	ORBManager.init(true, "dserver/" + JTangoTest.SERVER_NAME + "/" + JTangoTest.INSTANCE_NAME);
    }

    @Test(expected = DevFailed.class)
    public void test() throws DevFailed {
	JTangoTest.startNoDb();
	JTangoTest.startNoDb();
    }

    @After
    public void stopDevice() throws DevFailed {
	ServerManager.getInstance().stop();
    }

}
