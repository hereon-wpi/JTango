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


package fr.esrf.TangoDs;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.Device;
import fr.esrf.TangoApi.DbClass;
import fr.esrf.TangoApi.DbDatum;
import fr.esrf.TangoApi.DbDevExportInfo;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;

import java.util.Vector;

import static fr.esrf.TangoDs.TangoConst.*;

//TODO merge with DServerClass
public abstract class DeviceClass {
    private final Vector nodb_name_list = new Vector();
    /**
     * The TANGO device class name
     */
    protected String name;
    /**
     * The TANGO device class documentation URL
     */
    protected String doc_url;
    /**
     * The command(s) list
     */
    protected Vector command_list;
    /**
     * The device(s) list
     */
    protected Vector device_list;
    /**
     * The associated DbClass object
     */
    protected DbClass db_class;
    // +----------------------------------------------------------------------------
    //
    // method : DeviceClass(String s)
    // 
    // description : DeviceClass constructor. Protected method which will
    // be called automatically by the compiler.
    //
    // -----------------------------------------------------------------------------

    public void initClass() {
        device_list.clear();
        command_list.clear();
        command_list.addElement(new StatusCmd("Status", Tango_DEV_VOID, Tango_DEV_STRING,
                "Device status"));
        command_list.addElement(new StateCmd("State", Tango_DEV_VOID, Tango_DEV_STATE,
                "Device state"));
        command_list.addElement(new InitCmd("Init", Tango_DEV_VOID, Tango_DEV_VOID));
        Util.out4.println("DeviceClass  add cmd State/Status/Init " + get_name());
    }

    // +----------------------------------------------------------------------------
    //
    // method : get_class_system_resource(String s)
    // 
    // description : Method to retrieve some basic class resource(s)
    // The resource to be retrived are :
    // - The class doc URL
    //
    // -----------------------------------------------------------------------------

    private void get_class_system_resource() {

        //
        // Try to retrieve the class resource doc_url
        //

        if (Util._UseDb) {
            try {

                //
                // Call db server
                //

                final DbDatum res_value = db_class.get_property("doc_url");

                if (res_value.is_empty() == true) {
                    Util.out4.println("doc_url property for class " + name
                            + " is not defined in database");
                    doc_url = Tango_DefaultDocUrl;
                } else {
                    doc_url = res_value.extractString();
                }
            } catch (final DevFailed ex) {
                doc_url = Tango_DefaultDocUrl;
            } catch (final BAD_OPERATION ex) {
                doc_url = Tango_DefaultDocUrl;
            }
        } else {
            doc_url = Tango_DefaultDocUrl;
        }
    }

    // +-------------------------------------------------------------------------
    //
    // method : export_device()
    // 
    // description : This method exports a device to the outside world.
    // This is done by sending its CORBA network parameter
    // (mainly the IOR) to the Tango database
    //
    // --------------------------------------------------------------------------

    /**
     * Export a device.
     * <p>
     * Send device network parameter to TANGO database. The main parameter sent
     * to database is the CORBA stringified device IOR.
     *
     * @param dev The device to be exported
     * @throws DevFailed If the command sent to the database failed. Click <a
     *                   href="../../tango_basic/idl_html/Tango.html#DevFailed"
     *                   >here</a> to read <b>DevFailed</b> exception specification
     */

    protected void export_device(final DeviceImpl dev) throws DevFailed {
        Util.out4.println("DeviceClass::export_device() arrived");
        Device d;

        //
        // Activate the CORBA object incarnated by the Java object
        //

        final Util tg = Util.instance();
        final ORB orb = tg.get_orb();
        d = dev._this(orb);

        //
        // Get the object id and store it
        //

        byte[] oid = null;
        final POA r_poa = tg.get_poa();
        try {
            oid = r_poa.reference_to_id(d);
        } catch (final Exception ex) {
            final StringBuffer o = new StringBuffer("Can't get CORBA reference ID for device ");
            o.append(dev.get_name());

            Except.throw_exception("API_CantGetDevObjectID", o.toString(),
                    "DeviceClass.export_device()");
        }
        dev.set_obj_id(oid);

        //
        // Before exporting, check if database is used
        //
        if (Util._UseDb == false) {
            return;
            //
            // Prepare sent parameters
            //
        }

        final DbDevExportInfo exp_info = new DbDevExportInfo(dev.get_name(), orb
                .object_to_string(d), tg.get_host_name(), tg.get_version_str());

        //
        // Call db server
        //

        tg.get_database().export_device(exp_info);

        Util.out4.println("Leaving DeviceClass::export_device method()");

        //
        // Mark the device as exported
        //

        dev.set_exported_flag(true);
    }

    /**
     * Export a device.
     * <p>
     * This method activate the CORBA object associated with the servant dev. It
     * is used for device where a CORBALOC string is used to connect with. This
     * method is mainly used for the Tango database server.
     *
     * @param dev  The device to be exported
     * @param name The device CORBALOC name
     * @throws DevFailed If the command sent to the database failed. Click <a
     *                   href="../../tango_basic/idl_html/Tango.html#DevFailed"
     *                   >here</a> to read <b>DevFailed</b> exception specification
     */

    protected void export_device(final DeviceImpl dev, final String name) throws DevFailed {
        Util.out4.println("DeviceClass::export_device() arrived");
        Util.out4.println("name = " + name);

        //
        // For server started without db usage (Mostly the database server). In
        // this case,
        // it is necessary to create our own CORBA object id and to bind it into
        // the
        // OOC Boot Manager for access through a stringified object reference
        // constructed using the corbaloc style
        //

        final byte[] oid = name.getBytes();
        final Util tg = Util.instance();
        try {
            final org.omg.PortableServer.POA r_poa = tg.get_poa();
            r_poa.activate_object_with_id(oid, dev);
        } catch (final Exception ex) {
            final StringBuffer o = new StringBuffer("Can't activate device for device ");
            o.append(dev.get_name());

            Except.throw_exception("API_CantBindDevice", o.toString(),
                    "DeviceClass.export_device()");
        }

        //
        // Get the object id and store it
        //
        final ORB orb = tg.get_orb();
        dev._this(orb);
        dev.set_obj_id(oid);

        tg.registerDeviceForJacorb(name);

        //
        // Mark the device as exported
        //

        dev.set_exported_flag(true);

        Util.out4.println("Leaving DeviceClass::export_device method()");
    }

    // +----------------------------------------------------------------------------
    //
    // method : command_handler()
    // 
    // description : Command handler which is called by Device
    // when a command is received. It will check
    // to see if the command is implemented. If
    // so it will test to see if it is allowed
    // in this state. Finally it will execute the
    // command by calling its execute method.
    // If an error occurs it will throw a DevFailed
    // exception.
    //
    // -----------------------------------------------------------------------------

    /**
     * Execute a command.
     * <p>
     * It looks for the correct command object in the command object vector. If
     * the command is found, it invoke the <i>always_executed_hook</i> method.
     * Check if the command is allowed by invoking the <i>is_allowed</i> method
     * If the command is allowed, invokes the <i>execute</i> method.
     *
     * @param device  The device on which the command must be executed
     * @param command The command name
     * @param in_any  The command input data still packed in a CORBA Any object
     * @return A CORBA Any object with the output data packed in
     * @throws DevFailed If the command is not found, if the command is not allowed
     *                   in the actual device state and re-throws of all the
     *                   exception thrown by the <i>always_executed_hook</i>,
     *                   <i>is_alloed</i> and <i>execute</i> methods. Click <a
     *                   href=
     *                   "../../tango_basic/idl_html/Tango.html#DevFailed">here</a>
     *                   to read <b>DevFailed</b> exception specification
     */
    public Any command_handler(final DeviceImpl device, final String command, final Any in_any)
            throws DevFailed {
        Any ret = Util.instance().get_orb().create_any();

        Util.out4.println("Entering DeviceClass::command_handler() method");

        int i;
        final String cmd_name = command.toLowerCase();
        for (i = 0; i < command_list.size(); i++) {
            final Command cmd = (Command) command_list.elementAt(i);
            if (cmd.get_name().toLowerCase().equals(cmd_name) == true) {

                //
                // Call the always executed method
                //

                device.always_executed_hook();

                //
                // Check if the command is allowed
                //

                if (cmd.is_allowed(device, in_any) == false) {
                    final StringBuffer o = new StringBuffer("Command ");
                    o.append(command);
                    o.append(" not allowed when the device is in ");
                    o.append(Tango_DevStateName[device.get_state().value()]);
                    o.append(" state");

                    Except.throw_exception("API_CommandNotAllowed", o.toString(),
                            "DeviceClass.command_handler");
                }

                //
                // Execute the command
                //

                ret = cmd.execute(device, in_any);
                break;
            }
        }

        if (i == command_list.size()) {
            Util.out3.println("DeviceClass.command_handler(): command " + command + " not found");

            //
            // throw an exception to client
            //
            Except.throw_exception("API_CommandNotFound", "Command " + command + " not found",
                    "DeviceClass.command_handler");
        }

        Util.out4.println("Leaving DeviceClass.command_handler() method");
        return ret;
    }

    //
    // Two abstract methods
    //

    /**
     * Create command objects for all command supported by this class of device.
     * <p>
     * In the DeviceClass class, this method is pure abstract and must be
     * defined in sub-class. Its rule is to create the command object and to
     * store them in a vector of command objects
     */

    public abstract void command_factory();

    /**
     * Create all the attributes name supported by this class of device.
     * <p>
     * In the DeviceClass class, this method does nothing and must be re-defined
     * in sub-class if the sub-class supports attributes. Its rule is to store
     * the name of all the supported attributes in a vector.
     */

    public void attribute_factory(final Vector v) throws DevFailed {
    }

    /**
     * Create device(s) name list (for no database device server).
     * <p>
     * This method can be re-defined in DeviceClass sub-class for device server
     * started without database. Its rule is to initialise class device name.
     * The default method does nothing.
     *
     * @param list The device name list within a String vector
     */

    public void device_name_factory(final Vector list) {
    }

    /**
     * Create device(s).
     * <p>
     * In the DeviceClass class, this method is pure abstract and must be
     * defined in sub-class. Its rule is to create all the calss devices and to
     * store them in a vector of device
     *
     * @param dev_list The device name list
     * @throws DevFailed This method does not throw exception but a redefined
     *                   method can. Click <a
     *                   href="../../tango_basic/idl_html/Tango.html#DevFailed"
     *                   >here</a> to read <b>DevFailed</b> exception specification
     */

    public abstract void device_factory(String[] dev_list) throws DevFailed;

    //
    // Miscellaneous obvious methods
    //

    /**
     * Get the command object vector.
     *
     * @return A reference to the command vector
     */

    public Vector get_command_list() {
        return command_list;
    }

    /**
     * Get the device object vector.
     *
     * @return A reference to the device vector
     */

    public Vector get_device_list() {
        return device_list;
    }

    /**
     * Get the DServer object at the index in vector.
     *
     * @return the DServer object found at index.
     */

    public DeviceImpl get_device_at(final int idx) {
        return (DeviceImpl) device_list.elementAt(idx);
    }

    /**
     * Get the TANGO device class name.
     *
     * @return The TANGO device class name
     */

    public String get_name() {
        return name;
    }

    /**
     * Get the TANGO device class documentation URL.
     *
     * @return The TANGO device class documentation
     */

    public String get_doc_url() {
        return doc_url;
    }

    /**
     * Get a handle to the DbClass object
     *
     * @return A reference to the DbClass object
     */

    public DbClass get_db_class() {
        return db_class;
    }

    /**
     * Get a reference to the vector with device name list (for no database
     * device server).
     *
     * @return The device name list
     */
    public Vector get_nodb_name_list() {
        return nodb_name_list;
    }
}
