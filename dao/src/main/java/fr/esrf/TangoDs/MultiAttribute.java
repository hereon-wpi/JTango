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

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrQuality;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.DbAttribute;

import java.util.Vector;

/**
 * There is one instance of this class for each device. This class is mainly
 * an aggregate of Attribute or WAttribute objects. It eases management of 
 * multiple attributes
 *
 * @author $Author: pascal_verdier $
 * @version $Revision: 25297 $
 */
 
public class MultiAttribute implements TangoConst
{
	private static final AttrProperty[] def_opt_prop = new AttrProperty[] {
	new AttrProperty("label",Tango_LabelNotSpec),
	new AttrProperty("description",Tango_DescNotSpec),
            new AttrProperty("unit", Tango_UnitNotSpec),
            new AttrProperty("standard_unit",Tango_StdUnitNotSpec),
	new AttrProperty("display_unit",Tango_DispUnitNotSpec),
	new AttrProperty("format",Tango_FormatNotSpec),
	new AttrProperty("min_value",Tango_AlrmValueNotSpec),
	new AttrProperty("max_value",Tango_AlrmValueNotSpec),
	new AttrProperty("min_alarm",Tango_AlrmValueNotSpec),
	new AttrProperty("max_alarm",Tango_AlrmValueNotSpec),
	new AttrProperty("writable_attr_name",Tango_AssocWritNotSpec)};
    /**
     * The Attribute objects vector.
     * <p>
     * This vector is often referred as the main attributes vector
     */
    protected Vector attr_list = new Vector();
    /**
     * The list of writable attribute.
     * <p>
     * It is a vector of index in the main attribute vector
     */
    protected Vector writable_attr_list = new Vector();
    /**
     * The list of attribute with an alarm level defined.
     * <p>
     * It is a vector of index in the main attribute vector
     */
    protected Vector alarm_attr_list = new Vector();

/**
 * Create a new MultiAttribute object.
 *
 * This constructor will in-turn call the constructor of the Attribute or
 * WAttribute class of all the device class attributes.
 *
 * @param dev_name The device name
 * @param dev_class Reference to the device DeviceClass object
 * @exception DevFailed If the command sent to the database failed.
 * Click <a href="../../tango_basic/idl_html/Tango.html#DevFailed">here</a> to read
 * <b>DevFailed</b> exception specification
 */
 
	public MultiAttribute(String dev_name, DeviceClass dev_class) throws DevFailed
	{
	}

//+-------------------------------------------------------------------------
//
// method : 		concat
// 
// description : 	Concatenate porperties defined at the class level and
//			at the device level. Prperties defined at the device
//			level have the highest priority
//
// in :			dev_prop : The device properties
//			class_prop : The class properties
//
// out :		result : The resulting vector
//
//--------------------------------------------------------------------------


	private void concat(Vector dev_prop,
			    Vector class_prop,
			    Vector result)
	{
	
//
// Copy all device properties
//

		int i;
		for (i = 0;i < dev_prop.size();i++)
			result.addElement(dev_prop.elementAt(i));
		
//
// Add class properties if they have not been redefined at the device level
//

		Vector tmp_result = (Vector)(result.clone());
		int nb_class_check = class_prop.size();
		for (i = 0;i < nb_class_check;i++)
		{
			int j;
			for (j = 0;j < tmp_result.size();j++)
			{
				if (((AttrProperty)(tmp_result.elementAt(j))).get_name().equals(((AttrProperty)(class_prop.elementAt(i))).get_name()) == true)
				{
					break;
				}
			}
			
			if (j == tmp_result.size())
				result.addElement(class_prop.elementAt(i));
		}
	}

//+-------------------------------------------------------------------------
//
// method : 		add_default
// 
// description : 	Add default value for optional property if they
//			are not defined
//
// in :			prop_list : The already defined property vector
//
//--------------------------------------------------------------------------

	private void add_default(Vector prop_list)
	{
		int nb_opt_prop = def_opt_prop.length;

//
// For all the optional attribute properties, search in the already built
// vector of attributes if they are defined. If yes, continue. Otherwise,
// add a new property with the default value
//
	
		for (int i = 0;i < nb_opt_prop;i++)
		{
			String opt_prop_name = def_opt_prop[i].get_name();

			int j;
			for (j = 0;j < prop_list.size();j++)
			{
				if (((AttrProperty)(prop_list.elementAt(j))).get_name().equals(opt_prop_name) == true)
					break;
			}
			if (j == prop_list.size())
			{
				prop_list.addElement(def_opt_prop[i]);
			}
		}
	}

//+-------------------------------------------------------------------------
//
// method : 		add_user_default
// 
// description : 	Add user default value for property if they
//			are not defined
//
// in :			prop_list : The already defined property vector
//			user_default : The user defined default property values
//
//--------------------------------------------------------------------------

	void add_user_default(Vector prop_list,
			      Vector user_default)
	{

//
// Add default user properties if they have not been defined in the database
//

		int nb_user = user_default.size();
		for (int i = 0;i < nb_user;i++)
		{
			String user_prop_name = ((AttrProperty)(user_default.elementAt(i))).get_name();

			int j;
			for (j = 0;j < prop_list.size();j++)
			{
				if (((AttrProperty)(prop_list.elementAt(j))).get_name().equals(user_prop_name) == true)
					break;
			}
		
			if (j == prop_list.size())
				prop_list.addElement(user_default.elementAt(i));
		}
	}

//+-------------------------------------------------------------------------
//
// method : 		MultiAttribute::check_associated
// 
// description :	Check if the writable_attr_name property is set and
//			in this case, check if the associated attribute exists
//			and is writable. This is necessary only for attribute
//			of the READ_WITH_WRITE or READ_WRITE types 
//
// argument : in : 	- index : The index of the attribute to checked in the
//				  attr vector
//			- dev_name : The device name
//
//--------------------------------------------------------------------------

	void check_associated(int index,String dev_name) throws DevFailed
	{

		Attribute att = ((Attribute)(attr_list.elementAt(index)));
		if ((att.get_writable() == AttrWriteType.READ_WITH_WRITE) ||
		    (att.get_writable() == AttrWriteType.READ_WRITE))
		{
		
			if (att.get_data_format().value() != AttrDataFormat._SCALAR)
			{
				StringBuffer o = new StringBuffer("Device --> ");
				
				o.append(dev_name);
				o.append("\nProperty writable_attr_name for attribute ");
				o.append(att.get_name());
				o.append(" is defined but this attribute data format is not SCALAR");
				Except.throw_exception("API_AttrOptProp",
						       o.toString(),
						       "MultiAttribute.check_associated");
			}
			
			int j;
			int tmp_ind = 0;
			String assoc_name = att.get_assoc_name();
			for (j = 0;j < writable_attr_list.size();j++)
			{
				tmp_ind = (Integer) (writable_attr_list.elementAt(j));
				if (((Attribute)(attr_list.elementAt(tmp_ind))).get_name().equals(assoc_name) == true)
					break;
			}
			if (j == writable_attr_list.size())
			{
				StringBuffer o = new StringBuffer("Device --> ");

				o.append(dev_name);		
				o.append("\nProperty writable_attr_name for attribute ");
				o.append(att.get_name()); 
				o.append(" is set to ");
				o.append(assoc_name);
				o.append(", but this attribute does not exists or is not writable");
				Except.throw_exception("API_AttrOptProp",
						       o.toString(),
						       "MultiAttribute.check_associated");
			}

//
// Also check if the associated write attribute is a scalar one
//

			Attribute tmp_att = ((Attribute)(attr_list.elementAt(tmp_ind)));			
			if (tmp_att.get_data_format().value() != AttrDataFormat._SCALAR)
			{
				StringBuffer o = new StringBuffer("Device --> ");

				o.append(dev_name);		
				o.append("\nProperty writable_attr_name for attribute ");
				o.append(att.get_name()); 
				o.append(" is set to ");
				o.append(assoc_name);
				o.append(", but this attribute is not of the SCALAR data format");
				Except.throw_exception("API_AttrOptProp",
						       o.toString(),
						       "MultiAttribute.check_assiocated");
			}

//
// Check that the two associated attributes have the same data type
//
			
			if (tmp_att.get_data_type() != att.get_data_type())
			{
				StringBuffer o = new StringBuffer("Device --> ");

				o.append(dev_name);		
				o.append("\nProperty writable_attr_name for attribute ");
				o.append(att.get_name()); 
				o.append(" is set to ");
				o.append(assoc_name);
				o.append(", but these two attributes do not support the same data type");
				Except.throw_exception("API_AttrOptProp",o.toString(),
						       "MultiAttribute.check_associated");
			}
			
			att.set_assoc_ind(tmp_ind);
		}

	}
	
//=================================================================
/**
 * Remove an attribute on the device attribute list.
 *
 * @param attname Attribute's name to be removed to the list
 * @throws DevFailed .
 */
//=================================================================
	public void remove_attribute(String attname) throws DevFailed
	{
		// Check if this attribute is already defined for this device.
		Attribute attr = get_attr_by_name(attname);
		
		//	And remove it
		attr_list.remove(attr);
	}

	
/**
 * Get Attribute object from its name.
 *
 * This method returns a reference to the Attribute object with a name passed
 * as parameter. The equality on attribute name is case independant.
 *
 * @param attr_name The attribute name
 * @return A reference to the Attribute object
 * @exception DevFailed If the attribute is not defined.
 * Click <a href="../../tango_basic/idl_html/Tango.html#DevFailed">here</a> to read
 * <b>DevFailed</b> exception specification
 */
 
	public Attribute get_attr_by_name(String attr_name) throws DevFailed
	{
		Attribute	attribute = null;
		for (int i=0 ; i<attr_list.size() ; i++)
		{
			Attribute	att = (Attribute)attr_list.elementAt(i);
			if (att.get_name().equalsIgnoreCase(attr_name))
				attribute = att;
		}

		if (attribute==null)
		{
			Util.out3.println("MultiAttribute.get_attr throwing exception");
			Except.throw_exception("API_AttrNotFound",
					    attr_name + " attribute not found" ,
					     "MultiAttribute.get_attr");
		}
		return attribute;
	}

/**
 * Get Writable Attribute object from its name.
 *
 * This method returns a reference to the WAttribute object with a name passed
 * as parameter. The equality on attribute name is case independant.
 *
 * @param attr_name The attribute name
 * @return A reference to the writable attribute object
 * @exception DevFailed If the attribute is not defined.
 * Click <a href="../../tango_basic/idl_html/Tango.html#DevFailed">here</a> to read
 * <b>DevFailed</b> exception specification
 */

	public WAttribute get_w_attr_by_name(String attr_name) throws DevFailed
	{
		Attribute att = get_attr_by_name(attr_name);
		
		return ((WAttribute)(att));
	} 

/**
 * Get Attribute index into the main attribute vector from its name.
 *
 * This method returns the index in the Attribute vector (stored in the
 * MultiAttribute object) of an attribute with a given name. The name equality
 * is case independant
 *
 * @param attr_name The attribute name
 * @return The index in the main attributes vector
 * @exception DevFailed If the attribute is not found in the vector.
 * Click <a href="../../tango_basic/idl_html/Tango.html#DevFailed">here</a> to read
 * <b>DevFailed</b> exception specification
 */
 
	public int get_attr_ind_by_name(String attr_name) throws DevFailed
	{
		int i;

		int nb_attr = attr_list.size();
		for (i = 0;i < nb_attr;i++)
		{
			if (((Attribute)(attr_list.elementAt(i))).get_name().equalsIgnoreCase(attr_name) == true)
				break;
		}
	
		if (i == nb_attr)
		{
			Util.out3.println("MultiAttribute.get_attr_ind_by_name throwing exception");
			Except.throw_exception("API_AttrNotFound",
					      attr_name + " attribute not found",
					      "MultiAttribute.get_attr_ind_by_name");
		}
	
		return i;
	}

/**
 * Check alarm on all attribute(s) with an alarm defined.
 *
 * This method returns a boolean set to true if one of the attribute with an 
 * alarm level defined is in alarm condition.
 *
 * @return A boolean set to true if one attribute is in alarm
 * @exception DevFailed If the alarm level are not defined for one of the
 * attribute in the list of alarmable one
 * Click <a href="../../tango_basic/idl_html/Tango.html#DevFailed">here</a> to read
 * <b>DevFailed</b> exception specification
 *
 */
 
	public boolean check_alarm() throws DevFailed
	{
		boolean ret = false;
		int nb_alarm = alarm_attr_list.size();

		for (int i = 0;i < nb_alarm;i++)
		{
			boolean tmp_ret = check_alarm((Integer) (alarm_attr_list.elementAt(i)));
			if (tmp_ret == true)
			{
				ret = true;
				break;
			}
		}
	
		return ret;
	}

/**
 * Check alarm for one attribute with a given name.
 *
 * This method returns a boolean set to true if the attribute with the given
 * name is in alarm condition
 *
 * @param  attr_name  The attribute name
 * @return A boolean set to true if the attribute is in alarm
 * @exception DevFailed If the attribute does not have any alarm level defined.
 * Click <a href="../../tango_basic/idl_html/Tango.html#DevFailed">here</a> to read
 * <b>DevFailed</b> exception specification
 *
 */
 	
	public boolean check_alarm(String attr_name) throws DevFailed
	{
		return get_attr_by_name(attr_name).check_alarm();
	}

/**
 * Check alarm for one attribute from its index in the main attributes vector.
 *
 * This method returns a boolean set to true if the attribute with the given
 * index in the attrobite object vector is in alarm condition
 *
 * @param  ind  The attribute index
 * @return A boolean set to true if the attribute is in alarm
 * @exception DevFailed If the attribute does not have any alarm level defined.
 * Click <a href="../../tango_basic/idl_html/Tango.html#DevFailed">here</a> to read
 * <b>DevFailed</b> exception specification
 *
 */
 	
	public boolean check_alarm(int ind) throws DevFailed
	{
		return get_attr_by_ind(ind).check_alarm();
	}

//+-------------------------------------------------------------------------
//
// method : 		add_write_value
// 
// description : 	For scalar attribute with an associated write 
//			attribute, the read_attributes CORBA operation also
//			returns the write value. This method gets the associated
//			write attribute value and adds it to the read
//			attribute
//
// in :			att : Reference to the attribute which must be read
//
//--------------------------------------------------------------------------

	void add_write_value(Attribute att)
	{
		WAttribute assoc_att = get_w_attr_by_ind(att.get_assoc_ind());
	
		switch (att.get_data_type())
		{
		case Tango_DEV_BOOLEAN :
			boolean	bool_write_val = assoc_att.getBooleanWriteValue();		
			att.add_write_value(bool_write_val);
			break;
		
		case Tango_DEV_SHORT :
			short sh_write_val = assoc_att.getShortWriteValue();		
			att.add_write_value(sh_write_val);
			break;
		
		case Tango_DEV_USHORT :
			short ush_write_val = assoc_att.getShortWriteValue();		
			att.add_write_value(ush_write_val);
			break;
		
		case Tango_DEV_LONG :
			int lg_write_val = assoc_att.getLongWriteValue();		
			att.add_write_value(lg_write_val);
			break;
		
		case Tango_DEV_ULONG :
			int ulg_write_val = assoc_att.getLongWriteValue();		
			att.add_write_value(ulg_write_val);
			break;
		
		case Tango_DEV_LONG64 :
			long lg64_write_val = assoc_att.getLong64WriteValue();		
			att.add_write_value(lg64_write_val);
			break;
		
		case Tango_DEV_ULONG64 :
			long ulg64_write_val = assoc_att.getULong64WriteValue();		
			att.add_write_value(ulg64_write_val);
			break;
		
		case Tango_DEV_DOUBLE :
			double db_write_val = assoc_att.getDoubleWriteValue();		
			att.add_write_value(db_write_val);
			break;
		
		case Tango_DEV_STRING :
			String str_write_val = assoc_att.getStringWriteValue();		
			att.add_write_value(str_write_val);
			break;
		}
	}
	
//+-------------------------------------------------------------------------
//
// method : 		read_alarm
// 
// description : 	Add a message in the device status string if one of
//			the device attribute is in the alarm state
//
// in :			status : The device status
//
//--------------------------------------------------------------------------

/**
 * Add alarm message to device status.
 *
 * This method add alarm mesage to the string passed as parameter. A message
 * is added for each attribute which is in alarm condition
 *
 * @param  status  The string (should be the device status)
 */
	public void read_alarm(StringBuffer status)
	{
		int i;
	
		for (i = 0;i < alarm_attr_list.size();i++)
		{
			Attribute att = get_attr_by_ind((Integer) (alarm_attr_list.elementAt(i)));
			if (att.get_quality().value() == AttrQuality._ATTR_ALARM)
			{
		
//
// Add a message for low level alarm
//

				if (att.is_min_alarm() == true)
				{
					status.append("\nAlarm : Value too low for attribute ");
					status.append(att.get_name());				
				}

//
// Add a message for high level alarm
//
			
				if (att.is_max_alarm() == true)
				{
					status.append("\nAlarm : Value too high for attribute ");
					status.append(att.get_name());
				}
			}			
		}
	}

					
//+-------------------------------------------------------------------------
//
// Methods to retrieve/set some data members from outside the class and all
// its inherited classes
//
//--------------------------------------------------------------------------

/**
 * Get list of attribute with an alarm level defined.
 *
 * @return  A vector of Integer object. Each object is the index in the main
 * attribute vector of attribute with alarm level defined
 */
 
	public Vector get_alarm_list()
	{
		return alarm_attr_list;
	}

/**
 * Get attribute number.
 *
 * @return  The attribute number
 */
 	
	public int get_attr_nb()
	{
		return attr_list.size();
	}
	
/**
 * Get Attribute object from its index.
 *
 * This method returns a reference to the Attribute object from the index in the
 * main attribute vector
 *
 * @param ind The attribute index
 * @return A reference to the Attribute object
 */
 	
	public Attribute get_attr_by_ind(int ind)
	{
		return ((Attribute)(attr_list.elementAt(ind)));
	}

/**
 * Get Writable Attribute object from its index.
 *
 * This method returns a reference to the Writable Attribute object from the
 * index in the main attribute vector
 *
 * @param ind The attribute index
 * @return A reference to the WAttribute object
 */
 	
	public WAttribute get_w_attr_by_ind(int ind)
	{
		return ((WAttribute)(attr_list.elementAt(ind)));
	}
}
