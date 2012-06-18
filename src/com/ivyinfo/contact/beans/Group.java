package com.ivyinfo.contact.beans;

import java.io.Serializable;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Group implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2354002797732702168L;

	private int id = -1;
	
	private String title = "";
	private int summaryCount = 0;
	

	private List<Contact> contacts;

	public int getSummaryCount() {
		return summaryCount;
	}
	
	public void setSummaryCount(int summaryCount) {
		this.summaryCount = summaryCount;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}
	
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("id", id);
			obj.put("title", title);
			JSONArray contactArr = new JSONArray();
			if (contacts != null) {
				for (Contact c : contacts) {
					contactArr.put(c.toJSONObject());
				}
			}
			obj.put("contacts", contactArr);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(title);
		return sb.toString();
	}
}
