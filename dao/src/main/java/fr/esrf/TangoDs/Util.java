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
// $Revision: 25482 $
//
//-======================================================================


package fr.esrf.TangoDs;

import fr.esrf.Tango.*;
import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.Database;
import fr.esrf.TangoApi.DbDevImportInfo;
import org.apache.log4j.Level;
import org.omg.CORBA.*;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import java.lang.Object;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import static fr.esrf.TangoDs.TangoConst.*;

/**
 * This class is a used to store TANGO device server process data and to provide
 * the user with a set of utilities method. This class is implemented using the
 * singleton design pattern. Therefore a device server process can have only one
 * instance of this class and its constructor is not public.
 *
 * @author $Author: pascal_verdier $
 * @version $Revision: 25482 $
 */
@SuppressWarnings( { "NestedTryStatement" })
public class Util {
    /**
     * The UtilPrint object used for level 1 printing
     */

    static public UtilPrint out1;
    /**
     * The UtilPrint object used for level 2 printing
     */

    static public UtilPrint out2;
    /**
     * The UtilPrint object used for level 3 printing
     */

    static public UtilPrint out3;
    /**
     * The UtilPrint object used for level 4 printing
     */
    static public UtilPrint out4;
    /**
     * The UtilPrint object used for level 5 printing
     */

    static public UtilPrint out5;
    static public boolean _UseDb = true;
    static public boolean _daemon = false;
    static public int _sleep_between_connect = 10;
    static int _tracelevel = 0;
    /**
     * Get the serial model (TangoConst.BY_DEVICE, TangoConst.BY_Class or
     * TangoConst.NO_SYNC)
     */
    // ==========================================================
    static int access_counter = 0;
    private static Util _instance = null;
    static private int serial_model = BY_DEVICE;
    /**
     * returns the POA thread_pool_max value for ORB
     */
    // ==========================================================
    private static int thread_pool_max = 20; // Default value from
    // jacob.property (in jacrob
    // distrib.)
    private static boolean thread_pool_max_done = false;
    private final UtilExt ext;
    private final Vector<String> cmd_line_name_list = new Vector<String>();
    private final Vector class_name_list = new Vector();
    private String ds_exec_name;
    private String ds_instance_name;
    private StringBuffer ds_name;
    private String real_server_name;
    private String db_host;
    private Database db = null;
    private String hostname;
    private String version_str;
    private String pid_str;

    // +----------------------------------------------------------------------------
    //
    // method : instance()
    // 
    // description : static method to retrieve the Util object once it has
    // been initialised
    //
    // -----------------------------------------------------------------------------
    private Vector class_list;

    // +----------------------------------------------------------------------------
    //
    // method : Init()
    // 
    // description : static method to create/retrieve the Util object.
    // This method is the only one which enables a user to
    // create the object
    //
    // in : - argv : The command line argument
    // - class_name : The device server class name
    //
    // -----------------------------------------------------------------------------
    private ORB orb;

    // +----------------------------------------------------------------------------
    //
    // method : Util()
    // 
    // description : Constructor of the Util class.
    //
    // in : - argv : The command line argument
    // - class_name : The device server class name
    //
    // -----------------------------------------------------------------------------
    private POA _poa;

    // +----------------------------------------------------------------------------
    //
    // method : ckeck_args()
    // 
    // description : Check the command line arguments. The first one is
    // mandatory and is the server personal name. A -v option
    // is authorized with an optional argument. The other
    // option should be ORBacus option
    //
    // in : - argv : The command line argument
    //
    // -----------------------------------------------------------------------------

    /**
     * Constructs a newly allocated Util object.
     *
     * This constructor is protected following the singleton pattern
     *
     * @param argv
     *            The device server command line argument
     * @param class_name
     *            The TANGO device class name
     *
     */
    protected Util(final String[] argv, final String class_name) {

	//
	// Init ds executable name
	//

	ds_exec_name = class_name;

	//
	// Check server option
	//

	if (argv.length < 1) {
	    print_usage();
	    System.exit(-1);
	}

	//
	// Manage command line option (instance name and -v option)
	//

	check_args(argv);

	//
	// Build the four print objects
	//

	out1 = new UtilPrint(Level.INFO);
	out2 = new UtilPrint(Level.INFO);
	out3 = new UtilPrint(Level.DEBUG);
	out4 = new UtilPrint(Level.DEBUG);
	out5 = new UtilPrint(Level.DEBUG);

	//
	// Get Tango_host property
	//
	if (_UseDb == true) {
	    read_env();
	}

	org.omg.PortableServer.POA root_poa = null;
	try {
	    // Initialise CORBA
	    ApiUtil.set_in_server(true);
	    orb = ApiUtil.get_orb();

	    root_poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
	    // boot_manager =
	    // BootManagerHelper.narrow(orb.resolve_initial_references("BootManager"));
	} catch (final InvalidName ex) {
	    System.err.println("Can't intialise CORBA ORB !!!");
	    System.err.println("Failed when trying to get root POA reference");
	    System.exit(-1);
	}

	//
	// If the database is not used, create a POA with the USER_ID policy
	//

	org.omg.PortableServer.POA nodb_poa = null;
	try {
	    if (Util._UseDb == false) {
		final org.omg.PortableServer.IdAssignmentPolicy pol = root_poa
			.create_id_assignment_policy(org.omg.PortableServer.IdAssignmentPolicyValue.USER_ID);

		final org.omg.CORBA.Policy[] policy_list = new org.omg.CORBA.Policy[2];
		policy_list[0] = pol;
		policy_list[1] = root_poa
			.create_lifespan_policy(org.omg.PortableServer.LifespanPolicyValue.PERSISTENT);

		final org.omg.PortableServer.POAManager manager = root_poa.the_POAManager();
		nodb_poa = root_poa.create_POA("nodb_poa", manager, policy_list);
	    }
	} catch (final org.omg.PortableServer.POAPackage.AdapterAlreadyExists ex) {
	    System.err.println("Can't create CORBA POA !!!");
	    System.err.println("POA already exists");
	    System.exit(-1);
	} catch (final org.omg.PortableServer.POAPackage.InvalidPolicy ex) {
	    System.err.println("Can't create CORBA POA !!!");
	    System.err.println("Invalid policy");
	    System.exit(-1);
	}

	//
	// Store POA
	//

	if (_UseDb == true) {
	    _poa = root_poa;
	} else {
	    _poa = nodb_poa;
	}

	if (_UseDb == true) {

	    //
	    // Connect to the database
	    //

	    connect_db();
	}

	//
	// Connect to the database
	//
	Logging.init(ds_name.toString(), _tracelevel, db);

	if (_UseDb == true) {
	    //
	    // check if the server is not already running somewhere else
	    //

	    server_already_running();

	}

	//
	// create UtilExt class
	//
	ext = new UtilExt();
	//
	// Create the polling thread and start it
	//

	ext.poll_th = new PollThread(ext.shared_data, ext.poll_mon);
	ext.poll_th.start();

	//
	// Get process PID, host name and Tango version
	//

	misc_init();

	Util.out4.println("Util object singleton constructed");
    }

    // +----------------------------------------------------------------------------
    //
    // method : Tango::print_usage()
    // 
    // description : Print device server command line syntax
    //
    // in : - serv_name : The server name
    //
    // -----------------------------------------------------------------------------

    /**
     * Get the singleton object reference.
     * <p>
     * This method returns a reference to the object of the Util class. If the
     * class has not been initialised with it's init method, this method print a
     * message and abort the device server process
     *
     * @return The Util object reference
     */

    public static Util instance() {
        if (_instance == null) {
            System.err.println("Util is not initialised !!!");
            System.err.println("Exiting");
            System.exit(-1);
        }
        return _instance;
    }

    /**
     * Create and get the singleton object reference.
     * <p>
     * This method returns a reference to the object of the Util class. If the
     * class singleton object has not been created, it will be instanciated
     *
     * @param argv      The process argument String array
     * @param exec_name The device server executable name
     * @return The Util object reference
     */

    public static Util init(final String[] argv, final String exec_name) {
        if (_instance == null) {
            _instance = new Util(argv, exec_name);
        }
        return _instance;
    }

    // +----------------------------------------------------------------------------
    //
    // method : read_env()
    // 
    // description : Get the TANGO_HOST system property and extract the
    // database server host and port for it.
    // This system property should be set when the java
    // interpreter is started with its -D option
    //
    // -----------------------------------------------------------------------------

    /**
     * Create and return an empty CORBA Any object.
     * <p>
     * Create an empty CORBA Any object. Could be used by command which does not
     * return anything to the client. This method also prints a message on
     * screen (level 4) before it returns
     *
     * @param cmd The cmd name which use this empty Any. Only used to create the
     *            thrown exception (in case of) and in the displayed message
     * @return The empty CORBA Any
     * @throws DevFailed If the Any object creation failed. Click <a
     *                   href="../../tango_basic/idl_html/Tango.html#DevFailed"
     *                   >here</a> to read <b>DevFailed</b> exception specification
     */
    public static Any return_empty_any(final String cmd) throws DevFailed {
        Any ret = null;
        // noinspection ErrorNotRethrown
        try {
            ret = Util.instance().get_orb().create_any();
        } catch (final OutOfMemoryError ex) {
            final StringBuffer o = new StringBuffer(cmd);
            o.append(".execute");

            Util.out3.println("Bad allocation while in " + cmd + ".execute()");
            Except.throw_exception("API_MemoryAllocation", "Can't allocate memory in server", o
                    .toString());
        }

        Util.out4.println("Leaving " + cmd + ".execute()");

        return ret;
    }

    // +----------------------------------------------------------------------------
    //
    // method : misc_init()
    // 
    // description : This method initialises miscellaneous variable which
    // are needed later in the device server startup
    // sequence. These variables are :
    // The process ID
    // The host name
    // The Tango version
    //
    // -----------------------------------------------------------------------------

    /**
     * idem fabs in c libraray.
     */
    // ===============================================================
    public static double fabs(final double d) {
        if (d >= 0.0) {
            return d;
        } else {
            return -1.0 * d;
        }
    }

    // +----------------------------------------------------------------------------
    //
    // method : connect_db()
    // 
    // description : This method builds a connection to the Tango database
    // servant. It uses the db_host and db_port object
    // variables. The Tango database server implements its
    // CORBA object as named servant.
    //
    // -----------------------------------------------------------------------------

    static int getPoaThreadPoolMax() {
        if (!thread_pool_max_done) {
            final String str = System.getProperty("jacorb.poa.thread_pool_max");
            int value;
            try {
                value = Integer.parseInt(str);
                if (value > 0) {
                    thread_pool_max = value;
                }
            } catch (final NumberFormatException e) {
            }
            thread_pool_max_done = true;
        }
        return thread_pool_max;
    }

    // +----------------------------------------------------------------------------
    //
    // method : server_already_running()
    // 
    // description : Check if the same device server is not already running
    // somewhere else and refuse to start in this case
    //
    // -----------------------------------------------------------------------------

    /**
     * Get the serial model (TangoConst.BY_DEVICE, TangoConst.BY_Class or
     * TangoConst.NO_SYNC)
     */
    // ==========================================================
    static public int get_serial_model() {
        return serial_model;
    }

    // +----------------------------------------------------------------------------
    //
    // method : server_init()
    // 
    // description : To initialise all classes in the device server process
    //
    // -----------------------------------------------------------------------------

    /**
     * Set the serial model. The default is TangoConst.BY_DEVICE
     *
     * @param model the specified model (TangoConst.BY_DEVICE, TangoConst.BY_Class
     *              or TangoConst.NO_SYNC)
     */
    // ==========================================================
    static public void set_serial_model(final int model) {
        serial_model = model;
    }

    // +----------------------------------------------------------------------------
    //
    // method : server_run()
    // 
    // description : To start a device server
    //
    // -----------------------------------------------------------------------------

    static synchronized void increaseAccessConter() {
        access_counter++;
    }

    // +----------------------------------------------------------------------------
    //
    // method : get_device_list()
    // 
    // description : To return a list of device reference which name match the
    // specified pattern
    //
    // in : - pattern : The device name pattern
    //
    // -----------------------------------------------------------------------------

    static synchronized void decreaseAccessConter() {
        access_counter--;
    }

    // +----------------------------------------------------------------------------
    //
    // method : get_device_list_by_class()
    // 
    // description : To return a reference to the vector of device for a
    // specific class
    //
    // in : - class_name : The class name
    //
    // -----------------------------------------------------------------------------

    static synchronized int getAccessConter() {
        return access_counter;
    }

    // +----------------------------------------------------------------------------
    //
    // method : get_device_by_name()
    // 
    // description : To return a reference to the device object from its
    // name
    //
    // in : - dev_name : The device name
    //
    // -----------------------------------------------------------------------------

    private void check_args(final String argv[]) {

	if (argv[0].charAt(0) == '-') {
	    if (argv[0].equals("-?")) {
		print_usage();
		System.exit(0);
	    } else {
		print_usage();
		System.exit(-1);
	    }
	}
	ds_instance_name = argv[0];

	if (argv.length > 1) {
	    int ind = 1;
	    String dlist;
	    while (ind < argv.length) {
		if (argv[ind].charAt(0) == '-') {
		    switch (argv[ind].charAt(1)) {

		    //
		    // The verbose option
		    //

		    case 'v':
			if (argv[ind].length() == 2) {
			    if (argv.length - 1 > ind) {
				if (argv[ind + 1].charAt(0) == '-') {
				    set_trace_level(4);
				} else {
				    print_usage();
				    System.exit(-1);
				}
			    } else {
				set_trace_level(4);
			    }
			    ind++;
			} else {
			    final String level_str = argv[ind].substring(2);
			    int level = 0;
			    try {
				level = Integer.parseInt(level_str);
			    } catch (final NumberFormatException ex) {
				print_usage();
				System.exit(-1);
			    }

			    set_trace_level(level);
			    ind++;
			}
			break;

		    //
		    // Device server without database
		    //

		    case 'n':
			if (argv[ind].equals("-nodb") == false) {
			    print_usage();
			    System.exit(-1);
			} else {
			    _UseDb = false;
			    ind++;
			}
			break;

		    //
		    // Device list (for device server without database)
		    //

		    case 'd':
			if (argv[ind].equals("-dlist") == false) {
			    print_usage();
			    System.exit(-1);
			} else {
			    if (_UseDb == true) {
				print_usage();
				System.exit(-1);
			    }

			    ind++;
			    if (ind == argv.length) {
				print_usage();
				System.exit(-1);
			    } else {
				dlist = argv[ind].toLowerCase();

				//
				// Extract each device name
                    //
				String str;
				int start = 0;
				int pos;

				while ((pos = dlist.indexOf(',', start)) != -1) {
				    str = dlist.substring(start, pos);
				    start = pos + 1;
				    cmd_line_name_list.addElement(str);
				}
				if (start != dlist.length()) {
				    str = dlist.substring(start);
				    cmd_line_name_list.addElement(str);
				}

				//
				// Check that the same device name is not used
				// twice
				//

				int i, j;
				for (i = 0; i < cmd_line_name_list.size(); i++) {
				    for (j = 0; j < cmd_line_name_list.size(); j++) {
					if (i != j) {
					    if (cmd_line_name_list.elementAt(i).equals(
						    cmd_line_name_list.elementAt(j))) {
						System.err
							.println("Each device name must have different name");
						System.exit(-1);
					    }
					}
				    }
				}

			    }
			}
			break;

		    default:
			ind++;
			break;
		    }
		} else {
		    if (argv[ind - 1].substring(0, 2).equals("-v")) {
			print_usage();
			System.exit(-1);
		    }
		    ind++;
		}
	    }
	}

	//
	// Build server name in lower-case letters
	//

	real_server_name = ds_exec_name + "/" + ds_instance_name;
	ds_exec_name = ds_exec_name.toLowerCase();
	ds_instance_name = ds_instance_name.toLowerCase();

	ds_name = new StringBuffer(ds_exec_name);
	ds_name.append("/");
	ds_name.append(ds_instance_name);

	//
	// Check that the server name is not too long
	//

	if (ds_name.length() > Tango_MaxServerNameLength) {
	    System.err.println("The device server name is too long! Max length is "
		    + Tango_MaxServerNameLength + " characters.");
	    System.exit(-1);
	}
    }

    // +----------------------------------------------------------------------------
    //
    // method : get_dserver_device()
    // 
    // description : To return a reference to the dserver device automatically
    // attached to each device server process
    //
    // -----------------------------------------------------------------------------

    private void print_usage() {
	System.err.print("usage : java -DTANGO_HOST=$TANGO_HOST " + ds_exec_name
		+ " instance_name [-v[trace level]]");
	System.err.println(" [-nodb [-dlist <device name list>]]");
    }

    // +----------------------------------------------------------------------------
    //
    // method : unregister_server()
    // 
    // description : Unregister the server from the database
    //
    // -----------------------------------------------------------------------------

   // +-------------------------------------------------------------------------
    //
    // method : return_empty_any
    // 
    // description : Build and return one emty any for all commands which
    // returns an empty any (obvious no!)
    //
    // arguments : in : - cmd : The command name
    //
    // --------------------------------------------------------------------------

    private void read_env() {
        String env;
        try {
            if ((env = System.getProperty("TANGO_HOST")) == null) {
		if ((env = System.getenv("TANGO_HOST")) == null) {
		    Except.throw_connection_failed("TangoApi_TANGO_HOST_NOT_SET",
			    "Property \"TANGO_HOST\" not exported", "TangoDs.Util.read_env()");
		}
	    }

	    assert env != null;
	    if (env.indexOf(":") < 0) {
		Except.throw_connection_failed("TangoApi_TANGO_HOST_NOT_SET",
			"Unknown \"TANGO_HOST\" property " + env, "TangoDs.Util.read_env()");
	    }
	    final String[] array = ApiUtil.parseTangoHost(env);
	    db_host = array[0];
	} catch (final DevFailed e) {
	    Except.print_exception(e);
	    System.exit(-1);
	}
    }

    //
    // Miscellaneous basic methods
    //

    private void misc_init() {

        //
        // Get PID
        //

        pid_str = String.valueOf(0);

	//
	// Get hostname
	//

	try {
	    final String tmp_host1 = InetAddress.getLocalHost().toString();
	    int pos = tmp_host1.indexOf('/');
	    final String tmp_host2 = tmp_host1.substring(0, pos);
	    pos = tmp_host2.indexOf('.');
	    if (pos == -1) {
		hostname = tmp_host2;
	    } else {
		hostname = tmp_host2.substring(0, pos);
	    }
	} catch (final UnknownHostException ex) {
	    System.err.println("Cant retrieve server host name");
	    System.exit(1);
	}
	Util.out4.println("Hostname = " + hostname);

	//
	// Convert Tango version number to string (for device export)
	//

	version_str = String.valueOf(Tango_DevVersion);
    }

    /**
     * Connect the process to the TANGO database.
     *
     * If the connection to the database failed, a message is displayed on the
     * screen and the process is aborted
     */

    public synchronized void connect_db() {
		//Everything is fine
		//TODO remove later on
    }

    private synchronized void server_already_running() {

	Util.out4.println("Entering Tango::server_already_running method");
	//
	// First, sleep a while in order to solve race condition for two
	// servers started at the "same time" and a schedulling happens between
	// the database check and the export device. This system is inherited
	// from what has been implemented for the TACO control system
	//

	final Random rand = new Random();
	final int sl = rand.nextInt(1000);
	Util.out4.println("Waiting " + sl + " ms....");
	try {
	    wait(sl);
	} catch (final InterruptedException ex) {
	}

	//
	// Build device name and try to import it from database
	//

	final StringBuffer dev_name = new StringBuffer(Tango_DSDeviceDomain);
	dev_name.append('/');
	dev_name.append(ds_name);
	final String dev_name_str = new String(dev_name);

	Device dev = null;
	try {
	    final DbDevImportInfo db_dev = db.import_device(dev_name_str);
	    Util.out4.println("db.import_device(" + dev_name_str + ");    DONE ");
	    Util.out4.println("db_dev.exported ==" + db_dev.exported);

	    //
	    // If the device is not imported, leave function
	    //

	    if (db_dev.exported == false) {
		Util.out4.println("Leaving Tango::server_already_running method");
		return;
	    }

	    final org.omg.CORBA.Object obj = orb.string_to_object(db_dev.ior);
	    Util.out4.println("orb.string_to_object(db_dev.ior);    DONE ");

	    //
	    // Try to narrow the reference to a Tango_Device object
	    //

	    dev = DeviceHelper.narrow(obj);
	    Util.out4.println("dev = DeviceHelper.narrow(obj);    DONE ");
	} catch (final DevFailed e) {
	    if (e.errors[0].reason.equals("DB_DeviceNotDefined") == true) {
		System.err.println("This device server is not defined in database. Exiting!");
		System.exit(-1);
	    }
	} catch (final org.omg.CORBA.TIMEOUT e) {
	    // Receive a Timeout exception ---> It is not running !!!!
	    // System.err.println("org.omg.CORBA.TIMEOUT");
	} catch (final BAD_OPERATION e) {
	    System.err.println("Can't pack/unpack data sent to/from database in/to Any object");
	    System.exit(-1);
	} catch (final TRANSIENT e) {
	    Util.out4.println("Leaving Tango::server_already_running method");
	    return;
	} catch (final OBJECT_NOT_EXIST e) {
	    Util.out4.println("Leaving Tango::server_already_running method");
	    return;
	} catch (final COMM_FAILURE e) {
	    Util.out4.println("Leaving Tango::server_already_running method");
	    return;
	} catch (final Exception e) {
	    System.err.println(e);
	    e.printStackTrace();
	    System.exit(-1);
	}

	if (dev == null) {
	    Util.out4.println("Leaving Tango::server_already_running method");
	    return;
	}

	// Now, get the device name from the server
	try {
	    final String n = dev.name();
	    if (n.equals(dev_name_str) == true) {
		System.err.println("This server is already running, exiting!");
		System.exit(-1);
	    }
	} catch (final NO_RESPONSE e) {
	    System.err.println("This server is already running but is blocked!");
	    System.exit(-1);
	} catch (final SystemException e) {
	}
	Util.out4.println("Leaving Tango::server_already_running method");
    }

    /**
     * Ask a device server to listen for incoming request.
     *
     * This method does not return.
     */

    public void server_run() {
	orb.run();
    }

    /**
     * Get the list of device references which name name match the specified
     * pattern Returns a null vector in case there is no device matching the
     * pattern
     *
     * @param pattern
     *            The device name pattern
     */
    public Vector get_device_list(String pattern) {
	// - The returned list
	final Vector dl = new Vector();

	// ------------------------------------------------------------------
	// CASE I: pattern does not contain any '*' char - it's a device name
	if (pattern.indexOf('*') == -1) {
	    DeviceImpl dev = null;
	    try {
		dev = get_device_by_name(pattern);
	    } catch (final DevFailed df) {
		// - Ignore exception
	    }
	    if (dev != null) {
		dl.add(dev);
		return dl;
	    }
	}

	// - For the two remaining cases, we need the list of all DeviceClasses.
	final Vector dcl = get_class_list();
	// - A temp vector to store a given class' devices
	Vector temp_dl;

	// ------------------------------------------------------------------
	// CASE II: pattern == "*" - return a list containing all devices
	if (pattern.equals("*")) {
	    for (int i = 0; i < dcl.size(); i++) {
		temp_dl = ((DeviceClass) dcl.elementAt(i)).get_device_list();
		for (final Object aTemp_dl : temp_dl) {
		    final DeviceImpl dev = (DeviceImpl) aTemp_dl;
		    dl.add(dev);
		}
	    }
	    return dl;
	}

	// ------------------------------------------------------------------
	// CASE III: pattern contains at least one '*' char
	Iterator dl_it;
	String dev_name;
	DeviceImpl dev;
	final Iterator dcl_it = dcl.iterator();
	// - Compile the pattern
	pattern = pattern.replace('*', '.');
	final Pattern p = Pattern.compile(pattern);
	// - For each DeviceClass...
	while (dcl_it.hasNext()) {
	    // ...get device list
	    temp_dl = ((DeviceClass) dcl_it.next()).get_device_list();
	    // for each device in in list...
	    dl_it = temp_dl.iterator();
	    while (dl_it.hasNext()) {
		// ...get device ...
		dev = (DeviceImpl) dl_it.next();
		// ...and device name
		dev_name = dev.get_name().toLowerCase();
		// then look for token(s) in device name
		if (p.matcher(dev_name).matches()) {
		    dl.add(dev);
		}
	    }
	}
	return dl;
    }

    /**
     * Get the list of device references for a given TANGO class.
     *
     * Return the list of references for all devices served by one
     * implementation of the TANGO device pattern implemented in the process
     *
     * @param class_name
     *            The TANGO device class name
     * @return The device reference list
     * @exception DevFailed
     *                If in the device server process there is no TANGO device
     *                pattern implemented the TANGO device class given as
     *                parameter Click <a
     *                href="../../tango_basic/idl_html/Tango.html#DevFailed"
     *                >here</a> to read <b>DevFailed</b> exception specification
     */

    public Vector get_device_list_by_class(final String class_name) throws DevFailed {

	//
	// Retrieve class list. Don't use the get_dserver_device() method
	// followed by
	// the get_class_list(). In case of several classes embedded within
	// the same server and the use of this method in the object creation, it
	// will fail because the end of the dserver object creation is after the
	// end of the last server device creation.
	//

	final Vector cl_list = class_list;

	//
	// Check if the wanted class really exists
	//

	final int nb_class = cl_list.size();
	int i;
	for (i = 0; i < nb_class; i++) {
	    if (((DeviceClass) cl_list.elementAt(i)).get_name().equals(class_name) == true) {
		break;
	    }
	}

	//
	// Throw exception if the class is not found
	//

	if (i == nb_class) {
	    final StringBuffer o = new StringBuffer("Class ");
	    o.append(class_name);
	    o.append(" not found");

	    Except.throw_exception("API_ClassNotFound", o.toString(),
		    "Util::get_device_list_by_class()");
	}

	return ((DeviceClass) cl_list.elementAt(i)).get_device_list();
    }

    /**
     * Get a device reference from its name
     *
     * @param dev_name
     *            The TANGO device name
     * @return The device reference
     * @exception DevFailed
     *                If in the device is not served by one device pattern
     *                implemented in this process. Click <a
     *                href="../../tango_basic/idl_html/Tango.html#DevFailed"
     *                >here</a> to read <b>DevFailed</b> exception specification
     */

    public DeviceImpl get_device_by_name(String dev_name) throws DevFailed {
	//
	// Retrieve class list. Don't use the get_dserver_device() method
	// followed by
	// the get_class_list(). In case of several classes embedded within
	// the same server and the use of this method in the object creation, it
	// will fail because the end of the dserver object creation is after the
	// end of the last server device creation.
	//

	final Vector cl_list = class_list;

	//
	// Check if the wanted device exists in each class
	//
	dev_name = dev_name.toLowerCase();
	Vector dev_list = get_device_list_by_class(((DeviceClass) cl_list.elementAt(0)).get_name());
	final int nb_class = cl_list.size();
	int i, j, nb_dev;
	j = nb_dev = 0;
	boolean found = false;

	for (i = 0; i < nb_class; i++) {
	    dev_list = get_device_list_by_class(((DeviceClass) cl_list.elementAt(i)).get_name());
	    nb_dev = dev_list.size();
	    for (j = 0; j < nb_dev; j++) {
		if (((DeviceImpl) dev_list.elementAt(j)).get_name().toLowerCase().equals(dev_name) == true) {
		    found = true;
		    break;
		}
	    }
	    if (found == true) {
		break;
	    }
	}

	//
	// Check also the dserver device
	//

	if (found == false) {
	    final DServerClass ds_class = DServerClass.instance();
	    dev_list = ds_class.get_device_list();

	    final String name = ((DeviceImpl) dev_list.elementAt(0)).get_name();
	    if (name.compareToIgnoreCase(dev_name) == 0) {
		j = 0;
	    }
	}

	//
	// Throw exception if the class is not found
	//

	if (i == nb_class && j == nb_dev) {
	    final StringBuffer o = new StringBuffer("Device ");
	    o.append(dev_name);
	    o.append(" not found");

	    Except
		    .throw_exception("API_DeviceNotFound", o.toString(),
			    "Util::get_device_by_name()");
	}

	return (DeviceImpl) dev_list.elementAt(j);
    }

    /**
     * Get a reference to the dserver device attached to the device server
     * process
     *
     * @return A reference to the dserver device
     */

    public DServer get_dserver_device() {
	return (DServer) DServerClass.instance().get_device_list().elementAt(0);
    }

    /**
     * Unregister a device server process from the TANGO database.
     *
     * If the database call fails, a message is displayed on the screen and the
     * process is aborted
     */

    public void unregister_server() {

	Util.out4.println("Entering Tango::unregister_server method");

	if (Util._UseDb == true) {

	    //
	    // Mark all the devices belonging to this server as unexported
	    //

            try {
                db.unexport_server(ds_name.toString());
            } catch (SystemException | UserException e) {
                Except.print_exception(e);
                System.exit(-1);
            }
        }
        Util.out4.println("Leaving Tango::unregister_server method");
    }

    /**
     * Get the process trace level.
     *
     * @return The process trace level
     */

    public int get_trace_level() {
        return _tracelevel;
    }

    /**
     * Get a reference to the CORBA Boot Manager
     *
     * @return The CORBA boot manager public BootManager get_boot_manager() {
     *         return boot_manager; }
     */

    /**
     * Set the process trace level.
     *
     * @param level
     *            The new process level
     */
    public void set_trace_level(final int level) {
	_tracelevel = level;
    }

    /**
     * Get the device server instance name.
     *
     * @return The device server instance name
     */

    public String get_ds_inst_name() {
	return ds_instance_name;
    }

    /**
     * Get the device server executable name.
     *
     * @return The device server executable name
     */

    public String get_ds_exec_name() {
	return ds_exec_name;
    }

    /**
     * Get the device server name.
     *
     * The device server name is the device server executable name/the device
     * server instance name
     *
     * @return The device server name
     */

    public String get_ds_name() {
	return new String(ds_name);
    }

    /**
     * Get the real (not only lowercase) server name.
     *
     * The device server name is the device server executable name/the device
     * server instance name
     *
     * @return The real server name
     */

    public String get_ds_real_name() {
	return real_server_name;
    }

    /**
     * Get the host name where the device server process is running.
     *
     * @return The host name
     */

    public String get_host_name() {
	return hostname;
    }

    /**
     * Get the device server TANGO version.
     *
     * @return The device server version
     */

    public String get_version_str() {
	return version_str;
    }

    // ===============================================================

    /**
     * Get the TANGO database object reference
     *
     * @return The database reference
     */

    public Database get_database() {
        return db;
    }

    // ==========================================================

    /**
     * Get a reference to the CORBA ORB
     *
     * @return The CORBA ORB
     */
    public ORB get_orb() {
        return orb;
    }

    /**
     * Get a reference to the CORBA Portable Object Adapter (POA).
     *
     * For classical device server, this is the root POA. For no database device
     * server, this is a specific POA with the USER_ID policy.
     *
     * @return The CORBA POA
     */
    public POA get_poa() {
        return _poa;
    }

    /**
     * Get the device server process identifier as a String
     *
     * Return 0 as process identifer. Is ther any way to know process identifier
     * in a PURE java class ?
     */

    public String get_pid_str() {
	return pid_str;
    }

    /**
     * Get the DeviceClass list vector
     *
     * @return The DeviceClass vector reference
     */
    Vector get_class_list() {
        return class_list;
    }

    /**
     * Set the DeviceClass list vector
     *
     * @param list
     *            The DeviceClass vector reference
     */
    public void set_class_list(final Vector list) {
        class_list = list;
    }

    // ==========================================================

    /**
     * Get the commnd line device name list (for device server without database)
     *
     * @return The command line device name list
     */
    Vector get_cmd_line_name_list() {
	return cmd_line_name_list;
    }

    // ==========================================================

    /**
     * Add a new class in this device server (For no database device server)
     *
     * @param name
     *            The class name
     */
    public void add_class(final String name) {
	class_name_list.addElement(name);
    }

    // ==========================================================

    /**
     * Get the class name list (For no database device server)
     *
     * @return A string vector. Each element is a class name
     */

    public Vector get_class_name_list() {
	return class_name_list;
    }

    // ==========================================================
    // ==========================================================

    // ==========================================================

    /**
     * WARNING: The following code is JacORB specific. The jacorb.jar must have
     * been modified by adding org.jacorb.orb.ORB.putObjectKeyMap method. public
     * void putObjectKeyMap(String s, String t) { objectKeyMap.put(s, t); }
     *
     * Add device name in HashTable used for JacORB objectKeyMap if
     * _UseDb==false.
     *
     * @param name
     *            The device's name.
     */
    void registerDeviceForJacorb(final String name) {
        // Get the 3 fields of device name
        final StringTokenizer st = new StringTokenizer(name, "/");
        final String[] field = new String[3];
        for (int i = 0; i < 3 && st.countTokens() > 0; i++) {
            field[i] = st.nextToken();
        }

        // After a header used by JacORB, in the device name
        // the '/' char must be replaced by another separator
        final String separator = "&%25";
        final String targetname = "StandardImplName/nodb_poa/" + field[0] + separator + field[1]
                + separator + field[2];

        // And set the JacORB objectKeyMap HashMap
        final org.jacorb.orb.ORB jacorb = (org.jacorb.orb.ORB) orb;
        // Method added by PV for server without database.
        // in org/jacorb/orb/ORB.java
        jacorb.addObjectKey(name, targetname);
    }

    /**
     * This method sends command to the polling thread for all cmd/attr with
     * polling configuration stored in db.
     */
    // ==========================================================
    void polling_configure() {
	// Send a stop polling command to thread in order not to poll devices
	final DServer adm_dev = get_dserver_device();

	try {
	    adm_dev.stop_polling();
	} catch (final DevFailed e) {
	    Except.print_exception(e);
	}

	final Vector tmp_cl_list = adm_dev.get_class_list();// DeviceClass
	int upd;

	// Create the structure used to send data to the polling thread
	final DevVarLongStringArray send = new DevVarLongStringArray();
	send.lvalue = new int[1];
	send.svalue = new String[3];

	// A loop on each class and each device in class
	for (int i = 0; i < tmp_cl_list.size(); i++) {
	    final DeviceClass dc = (DeviceClass) tmp_cl_list.elementAt(i);
	    final Vector dev_list = dc.get_device_list(); // <DeviceImpl *
	    for (int j = 0; j < dev_list.size(); j++) {
		final DeviceImpl dev = (DeviceImpl) dev_list.elementAt(j);
		final Vector poll_cmd_list = dev.get_polled_cmd();
		final Vector poll_attr_list = dev.get_polled_attr();

		// Send a Add Object command to the polling thread only
		// if the polling period is different than zero
		for (int k = 0; k < poll_cmd_list.size(); k++) {
		    if (k == 0) {
			send.svalue[0] = dev.get_name();
			send.svalue[1] = "command";
		    }

		    // Convert polling period to a number
		    final String strval = (String) poll_cmd_list.elementAt(k + 1);
		    upd = Integer.parseInt(strval);

		    // Send command to the polling thread
		    if (upd != 0) {
			send.lvalue[0] = upd;
			send.svalue[2] = (String) poll_cmd_list.elementAt(k);

			try {
			    adm_dev.add_obj_polling(send, false);
			} catch (final DevFailed e) {
			}
		    }
		    k++;
		}

		// Send a Add Object attribute to the polling thread only
		// if the polling period is different than zero
		for (int k = 0; k < poll_attr_list.size(); k++) {

		    if (k == 0) {
			send.svalue[0] = dev.get_name();
			send.svalue[1] = "attribute";
		    }
		    // Convert polling period to a number
		    final String strval = (String) poll_attr_list.elementAt(k + 1);
		    upd = Integer.parseInt(strval);

		    // Send command to the polling thread
		    if (upd != 0) {
			send.lvalue[0] = upd;
			send.svalue[2] = (String) poll_attr_list.elementAt(k);

			try {
			    adm_dev.add_obj_polling(send, false);
			} catch (final DevFailed e) {
			}
		    }
		    k++;
		}
	    }
	}

	// Now, start the real polling
	try {
	    adm_dev.start_polling();
	} catch (final DevFailed e) {
	    Except.print_exception(e);
	}
    }

    // ==========================================================
    // ==========================================================
    PollThCmd get_poll_shared_cmd() {
	return ext.shared_data;
    }

    // ==========================================================

    // ==========================================================
    // ==========================================================
    TangoMonitor get_poll_monitor() {
	return ext.poll_mon;
    }

    // ==========================================================

    // ==========================================================
    // ==========================================================
    void poll_status(final boolean state) {
	ext.poll_on = state;
    }

    // ==========================================================

    // ==========================================================
    // ==========================================================
    boolean poll_status() {
	return ext.poll_on;
    }

    /**
     * Trigger the polling thread for polled attributes registered with a
     * polling update period set as "externally triggered" (0 mS)
     */
    // ==========================================================
    void trigger_attr_polling(final DeviceImpl dev, final String name) throws DevFailed {
	out4.println("Sending trigger to polling thread");

	// Check that the device is polled
	if (dev.is_polled() == false) {
	    Except.throw_exception("API_DeviceNotPolled", "Device " + dev.get_name()
		    + " is not polled", "Util.trigger_attr_polling");
	}

	// Find the wanted object in the list of device polled object
	final String obj_name = name.toLowerCase();
	final PollObj item = dev.get_polled_obj_by_type_name(Tango_POLL_ATTR, obj_name);

	// Check that it is an externally triggered polling object.
	// If it is not the case, throw exception
	final long tmp_upd = item.get_upd();
	if (tmp_upd != 0) {
	    Except.throw_exception("API_NotSupported", "Polling for attribute " + name
		    + " (device " + dev.get_name() + ")  is not externally triggered.",
		    "Util.trigger_attr_polling");
	}

	// Send command to the polling thread but wait
	// in case of previous cmd still not executed
	boolean interupted;
	final TangoMonitor mon = get_poll_monitor();
	final PollThCmd shared_cmd = get_poll_shared_cmd();

	mon.get_monitor();
	if (shared_cmd.trigger == true) {
	    try {
		mon.wait();
	    } catch (final InterruptedException e) {
	    }
	}
	shared_cmd.trigger = true;
	shared_cmd.dev = dev;
	shared_cmd.name = obj_name;
	shared_cmd.type = Tango_POLL_ATTR;

	mon.signal();

	out4.println("Trigger sent to polling thread");

	// Wait for thread to execute command
	dev.get_dev_monitor();

	while (shared_cmd.trigger == true) {

	    //
	    // Warning: It's possible to have a deadlock here (experienced under
	    // Windows) in case of this method being called from a command (or
	    // attribute
	    // methods) which are rapidly sent by the client.
	    // Client request cmd1 which send trigger to the polling thread
	    // The polling thread wake up clear shared_cmd.trigger and try to
	    // execute the command. But cmd 1 thread still owns the device
	    // monitor and
	    // polling thread wait. cmd 1 finished and client immediately send
	    // the
	    // command a new time. On Windows, it may happens that the polling
	    // thread is not activated just after the cmd thread has released
	    // the
	    // device monitor. As the client sent a new command, the device
	    // monitor
	    // is immediately re-taken by the thread executing the new command
	    // sent by
	    // the client. An order is sent to the polling thread and the cmd
	    // thread reach this code. It will wait for polling thread to clear
	    // shared_cmd.trigger. But, the polling thread is already waiting
	    // for
	    // the device monitor and ..... deadlock....
        //
	    /*
	     * bool deadlock = false; if (th->id() ==
	     * dev_mon.get_locking_thread_id()) { cout4 <<
	     * "Possible deadlock detected!" << endl; deadlock = true;
	     * dev_mon.rel_monitor(); dev_mon.rel_monitor(); }
	     */
	    interupted = mon.wait_it(Tango_DEFAULT_TIMEOUT);
	    /*
	     * if (deadlock == true) { dev_mon.get_monitor();
	     * dev_mon.get_monitor(); }
	     */
	    if (shared_cmd.trigger == true && interupted == false) {
		out4.println("TIME OUT");
		Except.throw_exception("API_CommandTimedOut", "Polling thread blocked !!!",
			"Util.trigger_attr_polling");
	    }
	}
	mon.rel_monitor();
	out4.println("Thread cmd normally executed");
    }

    /**
     * This method fills the polling buffer for one polled attribute registered
     * with an update period defined as "externally triggerred" (polling period
     * set to 0)
     *
     * @param dev
     *            The TANGO device
     * @param att_name
     *            The attribute name which must be polled
     * @param data
     *            The data stack with one element for each history element
     *
     * @throws DevFailed
     *             If the call failed
     */
    // ==========================================================
    public void fill_attr_polling_buffer(final DeviceImpl dev, final String att_name,
	    final TimedAttrData[] data) throws DevFailed {
	// Check that the device is polled
	if (dev.is_polled() == false) {
	    Except.throw_exception("API_DeviceNotPolled", "Device " + dev.get_name()
		    + " is not polled", "Util.fill_attr_polling_buffer()");
	}

	// Attribute name in lower case letters and check that it is marked as
	// polled
	final String obj_name = att_name.toLowerCase();
	dev.get_polled_obj_by_type_name(Tango_POLL_ATTR, obj_name);

	// Get a reference on the Attribute object
	final Attribute att = dev.get_device_attr().get_attr_by_name(att_name);

	// Check that it is a READ only attribute
	final AttrWriteType w_type = att.get_writable();
	if (w_type != AttrWriteType.READ) {
	    final String desc = "Attribute " + att_name + " of device " + dev.get_name()
		    + " is not READ only";
	    Except.throw_exception("API_DeviceNotPolled", desc, "Util.fill_attr_polling_buffer()");
	}

	// Check that history is not larger than polling buffer
	final int nb_elt = data.length;
	final int nb_poll = dev.get_poll_ring_depth();
	if (nb_elt > nb_poll) {
	    final String desc = "The polling buffer depth for attribute " + att_name
		    + " for device " + dev.get_name() + " is only " + nb_poll
		    + " which is less than " + nb_elt + " !";

	    Except.throw_exception("API_DeviceNotPolled", desc, "Util.fill_attr_polling_buffer()");
	}

	// A loop on each record
	DevFailed save_except = null;
	AttributeValue back = null;
	final TimeVal zero = new TimeVal(0, 0, 0);
	for (int i = 0; i < nb_elt; i++) {
	    // Check if read attr has failed
	    boolean attr_failed = false;
	    if (data[i].err != null && data[i].err.length > 0) {
		attr_failed = true;
		save_except = new DevFailed(data[i].err);
	    } else {
		// Allocate memory for the AttributeValueList sequence
		back = new AttributeValue();

		// Init name,date and quality factor
		back.time = new TimeVal(0, 0, 0);
		back.time.tv_sec = data[i].t_val.tv_sec;
		back.time.tv_usec = data[i].t_val.tv_usec;
		back.time.tv_nsec = 0;

		back.quality = data[i].qual;
		back.name = att_name;

		back.dim_x = 0;
		back.dim_y = 0;

		if (data[i].qual == AttrQuality.ATTR_VALID
			|| data[i].qual == AttrQuality.ATTR_ALARM
			|| data[i].qual == AttrQuality.ATTR_CHANGING) {
		    // Set Attribute object value
		    att.wanted_date(false);
		    att.set_date(data[i].t_val);
		    att.set_quality(data[i].qual);

		    // Init remaining fields in AttributeValueList
		    final Any any = ApiUtil.get_orb().create_any();
		    switch (att.get_data_type()) {
		    case Tango_DEV_BOOLEAN:
			att.set_value(data[i].bool_ptr, data[i].x, data[i].y);
			DevVarBooleanArrayHelper.insert(any, att.get_boolean_value());
			break;

		    case Tango_DEV_SHORT:
			att.set_value(data[i].sh_ptr, data[i].x, data[i].y);
			DevVarShortArrayHelper.insert(any, att.get_short_value());
			break;

		    case Tango_DEV_LONG:
			att.set_value(data[i].lg_ptr, data[i].x, data[i].y);
			DevVarLongArrayHelper.insert(any, att.get_long_value());
			break;

		    case Tango_DEV_LONG64:
			att.set_value(data[i].lg64_ptr, data[i].x, data[i].y);
			DevVarLong64ArrayHelper.insert(any, att.get_long64_value());
			break;

		    case Tango_DEV_FLOAT:
			att.set_value(data[i].fl_ptr, data[i].x, data[i].y);
			DevVarFloatArrayHelper.insert(any, att.get_float_value());
			break;

		    case Tango_DEV_DOUBLE:
			att.set_value(data[i].db_ptr, data[i].x, data[i].y);
			DevVarDoubleArrayHelper.insert(any, att.get_double_value());
			break;

		    case Tango_DEV_STRING:
			att.set_value(data[i].str_ptr, data[i].x, data[i].y);
			DevVarStringArrayHelper.insert(any, att.get_string_value());
			break;

		    /*
             * case Tango_DEV_USHORT :
		     * DevVarUShortArrayHelper.insert(any,
		     * att.get_ushort_value()); break;
		     *
		     * case Tango_DEV_UCHAR : DevVarUCharArrayHelper.insert(any,
		     * att.get_uchar_value()); break;
		     */
		    default:
			Except
				.throw_exception("Api_DataTypeNotSupported",
					"att.get_data_type() returns " + att.get_data_type()
						+ "\nType NOT supported",
					"Util.fill_attr_polling_buffer()");
		    }
		    back.value = any;
		    back.dim_x = data[i].x;
		    back.dim_y = data[i].y;
		}
	    }
	    // Fill one slot of polling buffer
	    try {
		get_poll_monitor().get_monitor();
		final PollObj item = dev.get_polled_obj_by_type_name(Tango_POLL_ATTR, obj_name);
		final TimeVal when = new TimeVal(0, 0, 0);
		if (attr_failed == false) {
		    when.tv_sec = back.time.tv_sec - Tango_DELTA_T;
		    when.tv_usec = back.time.tv_usec;
		    item.insert_data(back, when, zero);
		} else {
		    when.tv_sec = data[i].t_val.tv_sec - Tango_DELTA_T;
		    when.tv_usec = data[i].t_val.tv_usec;
		    item.insert_except(save_except, when, zero);
		}
		get_poll_monitor().rel_monitor();
	    } catch (final DevFailed e) {
		get_poll_monitor().rel_monitor();
	    }
	}
    }

    /**
     * This class is used for polling
     */
    // ==========================================================
    class UtilExt {
	Vector cmd_line_name_list;
	/**
	 * The polling thread object
	 */
	PollThread poll_th;
	/**
	 * The shared buffer
	 */
	PollThCmd shared_data;
	/**
	 * The monitor
	 */
	TangoMonitor poll_mon;
	/**
	 * Polling on flag
	 */
	boolean poll_on;

	UtilExt() {
	    shared_data = new PollThCmd();
	    shared_data.cmd_pending = false;
	    poll_mon = new TangoMonitor();
	}
    }
}
