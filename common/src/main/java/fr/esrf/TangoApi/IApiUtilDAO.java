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
// $Revision: 26454 $
//
//-======================================================================


package fr.esrf.TangoApi;

import fr.esrf.Tango.AttrQuality;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import org.omg.CORBA.Request;

public interface IApiUtilDAO {
    // ===================================================================
    /**
     * Return the orb object
     */
    // ===================================================================
    public void set_in_server(boolean val);

    // ===================================================================
    /**
     * Return true if in server code or false if in client code.
     */
    // ===================================================================
    public boolean in_server();

   // ==========================================================================
    /*
     * Asynchronous request management
     */
    // ==========================================================================
    // ==========================================================================
    /**
     * Add request in hash table and return id
     */
    // ==========================================================================
    public int put_async_request(AsyncCallObject aco);

    // ==========================================================================
    /**
     * Return the request in hash table for the id
     * 
     * @throws DevFailed
     */
    // ==========================================================================
    public Request get_async_request(int id) throws DevFailed;

    // ==========================================================================
    /**
     * Return the Asynch Object in hash table for the id
     */
    // ==========================================================================
    public AsyncCallObject get_async_object(int id);

    // ==========================================================================
    /**
     * Remove asynchronous call request and id from hashtable.
     */
    // ==========================================================================
    public void remove_async_request(int id);

    // ==========================================================================
    /**
     * Set the reply_model in AsyncCallObject for the id key.
     */
    // ==========================================================================
    public void set_async_reply_model(int id, int reply_model);

    // ==========================================================================
    /**
     * Set the Callback object in AsyncCallObject for the id key.
     */
    // ==========================================================================
    public void set_async_reply_cb(int id, CallBack cb);

    // ==========================================================================
    /**
     * return the still pending asynchronous call for a reply model.
     * 
     * @param dev DeviceProxy object.
     * @param reply_model ApiDefs.ALL_ASYNCH, POLLING or CALLBACK.
     */
    // ==========================================================================
    public int pending_asynch_call(DeviceProxy dev, int reply_model);

    // ==========================================================================
    /**
     * return the still pending asynchronous call for a reply model.
     * 
     * @param reply_model  ApiDefs.ALL_ASYNCH, POLLING or CALLBACK.
     */
    // ==========================================================================
    public int pending_asynch_call(int reply_model);

    // ==========================================================================
    /**
     * Return the callback sub model used.
     * 
     * @param model ApiDefs.PUSH_CALLBACK or ApiDefs.PULL_CALLBACK.
     */
    // ==========================================================================
    public void set_asynch_cb_sub_model(int model);

    // ==========================================================================
    /**
     * Set the callback sub model used (ApiDefs.PUSH_CALLBACK or
     * ApiDefs.PULL_CALLBACK).
     */
    // ==========================================================================
    public int get_asynch_cb_sub_model();

    // ==========================================================================
    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public void get_asynch_replies();

    // ==========================================================================
    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public void get_asynch_replies(int timeout);

    // ==========================================================================
    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public void get_asynch_replies(DeviceProxy dev);

    // ==========================================================================
    /**
     * Fire callback methods for all (any device) asynchronous requests(cmd and
     * attr) with already arrived replies.
     */
    // ==========================================================================
    public void get_asynch_replies(DeviceProxy dev, int timeout);

    // ==========================================================================
    /*
     * Methods to convert data.
     */
    // ==========================================================================

    // ==========================================================================
    // ==========================================================================
    public String stateName(DevState state);

    // ==========================================================================
    // ==========================================================================
    public String stateName(short state_val);

    // ==========================================================================
    // ==========================================================================
    public String qualityName(AttrQuality att_q);

    // ==========================================================================
    // ==========================================================================
    public String qualityName(short att_q_val);

    // ===================================================================
    // ===================================================================
    public double getZmqVersion();
    // ===================================================================
    // ===================================================================
}
