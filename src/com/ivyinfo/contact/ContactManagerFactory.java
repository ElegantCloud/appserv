package com.ivyinfo.contact;

import android.content.Context;
import android.util.Log;

public class ContactManagerFactory {
	
	private static ContactManager cm;
	
	public static void initContactManager(Context context) {
		Log.d("appserv", "initContactManager for " + context.toString());
		cm = new ContactManager(context);
	}
	
	public static ContactManager getContactManager() {
		return cm;
	}
	
}
