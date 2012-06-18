package com.ivyinfo.contact.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ivyinfo.util.Hanyu;

public class Contact implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2878858503688028889L;

	private int id = -1;
	private int rawContactID = -1;

	private String displayName = "";
	private String defaultFullPinyinName = "";

	private List<String> shortPinyinNames;
	private List<String> fullPinyinNames;

	private int timesContacted = 0;
	private List<String> phones;
	private List<Group> groups;

	private boolean selected = false;

	public int getTimesContacted() {
		return timesContacted;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void setTimesContacted(int timesContacted) {
		this.timesContacted = timesContacted;
	}

	public int getRawContactID() {
		return rawContactID;
	}

	public void setRawContactID(int rawContactID) {
		this.rawContactID = rawContactID;
	}

	public List<String> getShortPinyinNames() {
		return shortPinyinNames;
	}

	public void setShortPinyinNames(List<String> shortPinyinNames) {
		this.shortPinyinNames = shortPinyinNames;
	}

	public void setFullPinyinNames(List<String> fullPinyinNames) {
		this.fullPinyinNames = fullPinyinNames;
	}

	public List<String> getFullPinyinNames() {
		return fullPinyinNames;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
		// set full pingying name
		Hanyu hy = Hanyu.getInstance();
		hy.processString(displayName);
		setShortPinyinNames(hy.getShortPinyins());
		List<String> fullPinyins = hy.getFullPinyins();
		setFullPinyinNames(fullPinyins);
		setDefaultFullPinyinName(fullPinyins.get(0));
	}

	public String getDefaultFullPinyinName() {
		return defaultFullPinyinName;
	}

	public void setDefaultFullPinyinName(String fullPinyinName) {
		this.defaultFullPinyinName = fullPinyinName;
	}

	public List<String> getPhones() {
		return phones;
	}

	public void setPhones(List<String> phones) {
		this.phones = phones;
	}

	public void addPhone(String number) {
		if (phones == null) {
			phones = new ArrayList<String>();
		}
		phones.add(number);
	}

	public void addGroup(Group group) {
		if (groups == null) {
			groups = new ArrayList<Group>();
		}
		groups.add(group);
	}

	public List<Group> getGroups() {
		return groups;
	}

	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}

	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("id", id);
			obj.put("rawid", rawContactID);
			obj.put("name", displayName);
			obj.put("selected", selected);
			obj.put("times", timesContacted);
			JSONArray phoneArr = new JSONArray();
			if (phones != null) {
				for (String phone : phones) {
					phoneArr.put(phone);
				}
			}
			obj.put("phones", phoneArr);
			JSONArray groupArr = new JSONArray();
			if (groups != null) {
				for (Group g : groups) {
					groupArr.put(g.toJSONObject());
				}
			}
			obj.put("groups", groupArr);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return obj;
	}

}
