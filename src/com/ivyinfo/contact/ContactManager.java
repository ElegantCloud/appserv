package com.ivyinfo.contact;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.ivyinfo.contact.beans.Contact;
import com.ivyinfo.contact.beans.Group;

public class ContactManager {
	static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
			Contacts._ID, Contacts.DISPLAY_NAME, Contacts.TIMES_CONTACTED };
	static final String[] PHONE_PROJECTION = new String[] { Phone.DISPLAY_NAME,
			Phone.NUMBER };
	static final String[] CONTACT_INFO_PROJECTION = new String[] {
			Phone.CONTACT_ID, Phone.DISPLAY_NAME };
	static final String[] GROUP_SUMMARY_PROJECTION = new String[] { Groups._ID,
			Groups.TITLE, Groups.SUMMARY_COUNT };
	static final String[] GROUPMEMBERSHIP_PROJECTION = new String[] {
			GroupMembership.CONTACT_ID, GroupMembership.DISPLAY_NAME,
			GroupMembership.TIMES_CONTACTED };

	private final static String CHAR_NUMBER_REG = "[0-9A-Za-z_\\.]*";

	private boolean isModify = true;
	private boolean isModifyNameSort = true;
	private boolean isModifyCompoundSort = true;

	private List<Contact> allContacts;
	private List<Contact> allContactsNameSorted;
	private List<Contact> allContactsCompoundSorted;

	private List<Contact> currentContactList;

	private Map<Integer, Contact> contactMap;
	private Map<String, Contact> contactNumMap;
	@SuppressWarnings("rawtypes")
	private Comparator cmp = Collator.getInstance();
	private Comparator<Contact> nameComparator = new Comparator<Contact>() {
		@SuppressWarnings("unchecked")
		@Override
		public int compare(Contact c1, Contact c2) {
			return cmp.compare(c1.getDefaultFullPinyinName(),
					c2.getDefaultFullPinyinName());
		}
	};

	/**
	 * compound comparator mixed with name and times-contacted comparing
	 */
	private Comparator<Contact> compoundComparator = new Comparator<Contact>() {

		@SuppressWarnings("unchecked")
		@Override
		public int compare(Contact c1, Contact c2) {
			int ret = 0;
			int c1Times = c1.getTimesContacted();
			int c2Times = c2.getTimesContacted();

			if (c1Times == 0 && c2Times == 0) {
				ret = cmp.compare(c1.getDefaultFullPinyinName(),
						c2.getDefaultFullPinyinName());
			} else {
				ret = c2Times - c1Times;
				if (ret == 0) {
					ret = cmp.compare(c1.getDefaultFullPinyinName(),
							c2.getDefaultFullPinyinName());
				}
			}
			return ret;
		}
	};

	private ContentResolver cr;

	public ContactManager(Context context) {
		cr = context.getContentResolver();
		contactNumMap = new HashMap<String, Contact>();
	}

	/**
	 * set the flag if the contacts are modified
	 * 
	 * @param flag
	 *            - true: modified, false: not modified
	 */
	public void setIsModifyFlag(boolean flag) {
		this.isModify = flag;
		this.isModifyNameSort = flag;
		this.isModifyCompoundSort = flag;
	}

	/**
	 * get all contacts
	 * 
	 * @return
	 * @deprecated
	 */
	public List<Contact> getAllContacts() {
		List<Contact> contacts = new ArrayList<Contact>();

		Cursor bcCur = cr.query(Contacts.CONTENT_URI,
				CONTACTS_SUMMARY_PROJECTION, null, null, null);
		int bcIdColIndex = bcCur.getColumnIndex(Contacts._ID);
		int bcNameColIndex = bcCur.getColumnIndex(Contacts.DISPLAY_NAME);
		int timesIndex = bcCur.getColumnIndex(Contacts.TIMES_CONTACTED);
		if (bcCur.moveToFirst()) {
			while (!bcCur.isAfterLast()) {
				int contactID = bcCur.getInt(bcIdColIndex);
				String cID = bcCur.getString(bcIdColIndex);
				String name = bcCur.getString(bcNameColIndex);
				int times = bcCur.getInt(timesIndex);
				Log.d("appserv", "id: " + cID + " name: " + name + " times: "
						+ times);

				Contact contact = new Contact();
				contact.setId(contactID);
				contact.setDisplayName(name);
				contact.setTimesContacted(times);

				contacts.add(contact);
				bcCur.moveToNext();
			}
		}
		bcCur.close();
		currentContactList = contacts;
		return contacts;
	}

	/**
	 * trim the country calling code of phone number
	 * 
	 * @param number
	 * @return
	 */
	public String trimCountryCode(String number) {
		String trimedNumber = number.trim();
		if (trimedNumber.startsWith("+") && trimedNumber.length() > 3) {
			trimedNumber = trimedNumber.substring(3);
		}
		return trimedNumber;
	}

	static final String[] CONTACT_WITH_PHONE_PROJECTION = new String[] {
			Phone.RAW_CONTACT_ID, Phone.CONTACT_ID, Phone.DISPLAY_NAME,
			Phone.NUMBER, Phone.TIMES_CONTACTED };

	/**
	 * get all contacts with phone number
	 * 
	 * @return
	 */
	public List<Contact> getAllContactsWithPhone() {
		long start = System.currentTimeMillis();

		if (allContacts == null || isModify) {
			isModify = false;

			allContacts = new ArrayList<Contact>();
			contactMap = new HashMap<Integer, Contact>();
			contactNumMap = new HashMap<String, Contact>();

			Cursor cur = cr.query(Phone.CONTENT_URI,
					CONTACT_WITH_PHONE_PROJECTION, null, null, Phone.CONTACT_ID
							+ " ASC");
			int rawContactIDIndex = cur.getColumnIndex(Phone.RAW_CONTACT_ID);
			int contactIDIndex = cur.getColumnIndex(Phone.CONTACT_ID);
			int nameIndex = cur.getColumnIndex(Phone.DISPLAY_NAME);
			int numIndex = cur.getColumnIndex(Phone.NUMBER);
			int timesIndex = cur.getColumnIndex(Phone.TIMES_CONTACTED);
			if (cur.moveToFirst()) {
				Contact priorContact = new Contact();
				if (!cur.isAfterLast()) {
					int rawContactID = cur.getInt(rawContactIDIndex);
					int contactID = cur.getInt(contactIDIndex);
					String name = cur.getString(nameIndex);
					String number = cur.getString(numIndex);
					number = trimCountryCode(number);
					int times = cur.getInt(timesIndex);
					// Log.d("appserv", "id: " + contactID + " raw id: " +
					// rawContactID + " name: " + name
					// + " number: " + number + " times: " + times);

					priorContact.setRawContactID(rawContactID);
					priorContact.setId(contactID);
					priorContact.setDisplayName(name);
					priorContact.setTimesContacted(times);
					priorContact.addPhone(number);

					contactMap.put(new Integer(contactID), priorContact);
					contactNumMap.put(number, priorContact);

					allContacts.add(priorContact);
					cur.moveToNext();
				}
				while (!cur.isAfterLast()) {
					int rawContactID = cur.getInt(rawContactIDIndex);
					int contactID = cur.getInt(contactIDIndex);
					String name = cur.getString(nameIndex);
					String number = cur.getString(numIndex);
					number = trimCountryCode(number);
					int times = cur.getInt(timesIndex);
					// Log.d("appserv", "id: " + contactID + " raw id: " +
					// rawContactID + " name: " + name
					// + " number: " + number + " times: " + times);

					if (contactID != priorContact.getId()) {
						Contact contact = new Contact();
						contact.setRawContactID(rawContactID);
						contact.setId(contactID);
						contact.setDisplayName(name);
						contact.setTimesContacted(times);
						contact.addPhone(number);

						contactMap.put(new Integer(contactID), contact);

						allContacts.add(contact);
						priorContact = contact;
					} else {
						priorContact.addPhone(number);
					}
					contactNumMap.put(number, priorContact);

					cur.moveToNext();
				}
			}
			cur.close();
			currentContactList = allContacts;

		}

		long end = System.currentTimeMillis();
		Log.d("appserv", "getAllContactsWithPhone time: " + (end - start));

		return allContacts;
	}

	/**
	 * get all contacts within JSONArray format
	 * 
	 * @return
	 */
	public JSONArray getAllContactsJSON() {
		List<Contact> contacts = getAllContactsWithPhone();
		JSONArray contactArr = new JSONArray();
		for (int i = 0; i < contacts.size(); i++) {
			Contact c = contacts.get(i);
			JSONObject cj = c.toJSONObject();
			contactArr.put(cj);
			Log.d("appserv", cj.toString());
		}
		return contactArr;
	}

	/**
	 * get all contacts sorted by name in ascendent
	 * 
	 * @return
	 */
	public List<Contact> getAllContactsByNameSort() {
		long start = System.currentTimeMillis();

		if (allContactsNameSorted == null || isModifyNameSort) {
			isModifyNameSort = false;

			allContactsNameSorted = getAllContactsWithPhone();
			Collections.sort(allContactsNameSorted, nameComparator);
			currentContactList = allContactsNameSorted;
		}
		long end = System.currentTimeMillis();
		Log.d("appserv", "getAllContactsByNameSort time: " + (end - start));
		return allContactsNameSorted;
	}

	/**
	 * get all contacts sorted by times and name
	 * 
	 * @return
	 */
	public List<Contact> getAllContactsByCompoundSort() {
		long start = System.currentTimeMillis();

		if (allContactsCompoundSorted == null || isModifyCompoundSort) {
			isModifyCompoundSort = false;

			allContactsCompoundSorted = getAllContactsWithPhone();
			Collections.sort(allContactsCompoundSorted, compoundComparator);
			currentContactList = allContactsCompoundSorted;
		}
		long end = System.currentTimeMillis();
		Log.d("appserv", "getAllContactsByCompoundSort time: " + (end - start));
		return allContactsCompoundSorted;
	}

	/**
	 * get all contacts sorted by name in ascendent within JSONArray format
	 * 
	 * @return
	 */
	public JSONArray getAllContactsByNameSortJSON() {
		List<Contact> contacts = getAllContactsByNameSort();
		JSONArray contactArr = new JSONArray();
		for (int i = 0; i < contacts.size(); i++) {
			Contact c = contacts.get(i);
			JSONObject cj = c.toJSONObject();
			contactArr.put(cj);
			// Log.d("appserv",
			// "$id: " + c.getId() + " name: " + c.getDisplayName()
			// + " times: " + c.getTimesContacted());
		}
		return contactArr;
	}

	/**
	 * get all contacts sorted by time_contacted and name in ascendent within
	 * JSONArray format
	 * 
	 * @return
	 */
	public JSONArray getAllContactsByCompoundSortJSON() {
		List<Contact> contacts = getAllContactsByCompoundSort();
		JSONArray contactArr = new JSONArray();
		for (int i = 0; i < contacts.size(); i++) {
			Contact c = contacts.get(i);
			JSONObject cj = c.toJSONObject();
			contactArr.put(cj);
			// Log.d("appserv",
			// "id: " + c.getId() + " name: " + c.getDisplayName()
			// + " times: " + c.getTimesContacted());
		}
		return contactArr;
	}

	/**
	 * get contact phone info
	 * 
	 * @param contactID
	 * @return
	 */
	public Contact getContactPhones(int contactID) {
		Contact contact = new Contact();
		contact.setId(contactID);
		// query all phone numbers
		Cursor phCur = cr.query(Phone.CONTENT_FILTER_URI, PHONE_PROJECTION,
				Phone.CONTACT_ID + "=?",
				new String[] { String.valueOf(contactID) }, null);
		List<String> phones = new ArrayList<String>();
		int nameIndex = phCur.getColumnIndex(Phone.DISPLAY_NAME);
		int phoneIndex = phCur.getColumnIndex(Phone.NUMBER);
		String name = "";
		if (phCur.moveToFirst()) {
			while (!phCur.isAfterLast()) {
				name = phCur.getString(nameIndex);
				String phoneNum = phCur.getString(phoneIndex);
				Log.d("appserv", "name: " + name + " phone: " + phoneNum);
				phones.add(phoneNum);
				phCur.moveToNext();
			}
		}
		phCur.close();
		contact.setDisplayName(name);
		contact.setPhones(phones);

		return contact;
	}

	/**
	 * get contact groups info
	 * 
	 * @param contactID
	 * @return
	 */
	public Contact getContactGroups(Contact contact) {
		if (contact == null) {
			return new Contact();
		}

		List<Group> groups = new ArrayList<Group>();
		Cursor gpMemshipCur = cr.query(Data.CONTENT_URI,
				new String[] { GroupMembership.GROUP_ROW_ID },
				GroupMembership.CONTACT_ID + "=? AND " + Data.MIMETYPE + "='"
						+ GroupMembership.CONTENT_ITEM_TYPE + "' ",
				new String[] { String.valueOf(contact.getId()) }, null);
		int gpIDColIndex = gpMemshipCur
				.getColumnIndex(GroupMembership.GROUP_ROW_ID);
		if (gpMemshipCur.moveToFirst()) {
			while (!gpMemshipCur.isAfterLast()) {
				Group group = new Group();

				int groupID = gpMemshipCur.getInt(gpIDColIndex);
				group.setId(groupID);
				// query group name by id
				Cursor groupCur = cr.query(Groups.CONTENT_URI,
						new String[] { Groups.TITLE }, Groups._ID + "=?",
						new String[] { String.valueOf(groupID) }, null);
				int gpNameIndex = groupCur.getColumnIndex(Groups.TITLE);
				groupCur.moveToFirst();
				String gpName = groupCur.getString(gpNameIndex);
				Log.d("appserv", "group name: " + gpName);
				group.setTitle(gpName);

				groups.add(group);
				gpMemshipCur.moveToNext();
			}
		}
		gpMemshipCur.close();
		contact.setGroups(groups);
		return contact;
	}

	/**
	 * get contact info by his/her phone number
	 * 
	 * @param phoneNum
	 * @return only including contact name & contact id
	 */
	public Contact getContactByPhone(String phoneNum) {
		Contact contact = contactNumMap.get(phoneNum);

		if (contact == null) {
			contact = new Contact();
			contact.setDisplayName(phoneNum);
		}

		return contact;
	}

	/**
	 * save contact group membership
	 * 
	 * @param contact
	 */
	public void saveContactGroups(Contact contact) {
		Log.d("appserv", "saveContactGroups");
		if (contact != null) {
			// first delete all group membership
			cr.delete(Data.CONTENT_URI, Data.MIMETYPE + "='"
					+ GroupMembership.CONTENT_ITEM_TYPE + "' AND "
					+ Data.CONTACT_ID + "=?",
					new String[] { String.valueOf(contact.getId()) });

			// then add group membership for the contact

			List<Group> groups = contact.getGroups();
			if (groups != null) {
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				for (Group g : groups) {
					Log.d("appserv",
							"saveContactGroups - group id: " + g.getId()
									+ " title: " + g.getTitle() + " rawid: "
									+ contact.getRawContactID() + " id: "
									+ contact.getId() + " name: "
									+ contact.getDisplayName());
					ops.add(ContentProviderOperation
							.newInsert(
									android.provider.Contacts.GroupMembership.CONTENT_URI)
							.withValue(
									android.provider.Contacts.GroupMembership.PERSON_ID,
									new Integer(contact.getRawContactID()))
							.withValue(
									android.provider.Contacts.GroupMembership.GROUP_ID,
									new Integer(g.getId())).build());
				}
				try {
					cr.applyBatch(ContactsContract.AUTHORITY, ops);
				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * add new group
	 * 
	 * @param title
	 *            - group title
	 */
	public void addGroup(String title) {
		ContentValues vals = new ContentValues();
		vals.put(Groups.TITLE, title);
		cr.insert(Groups.CONTENT_URI, vals);
	}

	/**
	 * delete group
	 * 
	 * @param groupID
	 */
	public void deleteGroup(int groupID) {
		cr.delete(
				Uri.parse(Groups.CONTENT_URI.toString() + "?"
						+ ContactsContract.CALLER_IS_SYNCADAPTER + "=true"),
				Groups._ID + "=?", new String[] { String.valueOf(groupID) });
	}

	/**
	 * delete contact by raw contact id
	 * 
	 * @param rawContactID
	 */
	public void deleteContact(int rawContactID) {
		cr.delete(
				Uri.parse(ContactsContract.RawContacts.CONTENT_URI.toString()
						+ "?" + ContactsContract.CALLER_IS_SYNCADAPTER
						+ "=true"), RawContacts._ID + "=?",
				new String[] { String.valueOf(rawContactID) });
	}

	/**
	 * get all groups
	 * 
	 * @return
	 */
	public List<Group> getAllGroups() {
		List<Group> groups = new ArrayList<Group>();
		Cursor groupCur = cr.query(Groups.CONTENT_SUMMARY_URI,
				GROUP_SUMMARY_PROJECTION, Groups.GROUP_VISIBLE + "=? AND "
						+ Groups.DELETED + "=?", new String[] { "1", "0" },
				null);
		int idIndex = groupCur.getColumnIndex(Groups._ID);
		int titleIndex = groupCur.getColumnIndex(Groups.TITLE);
		int countIndex = groupCur.getColumnIndex(Groups.SUMMARY_COUNT);
		if (groupCur.moveToFirst()) {
			while (!groupCur.isAfterLast()) {
				int id = groupCur.getInt(idIndex);
				String title = groupCur.getString(titleIndex);
				int summaryCount = groupCur.getInt(countIndex);
				Log.d("appserv", "group id: " + id + " title: " + title
						+ " summary count: " + summaryCount);

				Group group = new Group();
				group.setId(id);
				group.setTitle(title);
				group.setSummaryCount(summaryCount);
				groups.add(group);

				groupCur.moveToNext();
			}
		}
		groupCur.close();

		return groups;
	}

	/**
	 * get all groups within JSONArray format
	 * 
	 * @return
	 */
	public JSONArray getAllGroupsJSON() {
		List<Group> groups = getAllGroups();
		JSONArray groupArr = new JSONArray();
		for (int i = 0; i < groups.size(); i++) {
			Group c = groups.get(i);
			JSONObject cj = c.toJSONObject();
			groupArr.put(cj);
		}
		return groupArr;
	}

	/**
	 * get contacts by group
	 * 
	 * @param groupID
	 * @return
	 */
	public List<Contact> getContactsByGroup(int groupID) {
		List<Contact> contacts = new ArrayList<Contact>();
		Cursor gpMemshipCur = cr.query(Data.CONTENT_URI,
				GROUPMEMBERSHIP_PROJECTION, Data.MIMETYPE + "='"
						+ GroupMembership.CONTENT_ITEM_TYPE + "' AND "
						+ GroupMembership.GROUP_ROW_ID + "=?",
				new String[] { String.valueOf(groupID) }, null);

		int cIDIndex = gpMemshipCur.getColumnIndex(GroupMembership.CONTACT_ID);
		int cNameIndex = gpMemshipCur
				.getColumnIndex(GroupMembership.DISPLAY_NAME);
		int timesIndex = gpMemshipCur
				.getColumnIndex(GroupMembership.TIMES_CONTACTED);
		if (gpMemshipCur.moveToFirst()) {
			while (!gpMemshipCur.isAfterLast()) {
				int contactID = gpMemshipCur.getInt(cIDIndex);
				String name = gpMemshipCur.getString(cNameIndex);
				int times = gpMemshipCur.getInt(timesIndex);
				// Log.d("appserv", "id: " + contactID + " name: " + name +
				// " times: " + times);
				Contact contact = null;
				if (contactMap != null) {
					contact = contactMap.get(new Integer(contactID));
				}
				if (contact == null) {
					contact = new Contact();
					contact.setId(contactID);
					contact.setDisplayName(name);
					contact.setTimesContacted(times);
				}

				contacts.add(contact);
				gpMemshipCur.moveToNext();
			}
		}
		gpMemshipCur.close();

		currentContactList = contacts;
		return contacts;
	}

	/**
	 * get contacts by group sorted by name in ascendent
	 * 
	 * @param groupID
	 * @return
	 */
	public List<Contact> getContactsByGroupByNameSort(int groupID) {
		List<Contact> contacts = getContactsByGroup(groupID);
		Collections.sort(contacts, nameComparator);
		currentContactList = contacts;
		return contacts;
	}

	/**
	 * get contacts by group sorted by times and name
	 * 
	 * @param groupID
	 * @return
	 */
	public List<Contact> getContactsByGroupByCompoundSort(int groupID) {
		List<Contact> contacts = getContactsByGroup(groupID);
		Collections.sort(contacts, compoundComparator);
		currentContactList = contacts;
		return contacts;
	}

	/**
	 * get contacts by group sorted by name in ascendent within JSONArray format
	 * 
	 * @param groupID
	 * @return
	 */
	public JSONArray getContactsByGroupByNameSortJSON(int groupID) {
		List<Contact> contacts = getContactsByGroupByNameSort(groupID);
		JSONArray contactArr = new JSONArray();
		for (int i = 0; i < contacts.size(); i++) {
			Contact c = contacts.get(i);
			JSONObject cj = c.toJSONObject();
			contactArr.put(cj);
			Log.d("appserv", cj.toString());
		}
		return contactArr;
	}

	public JSONArray getContactsByGroupByCompoundSortJSON(int groupID) {
		List<Contact> contacts = getContactsByGroupByCompoundSort(groupID);
		JSONArray contactArr = new JSONArray();
		for (int i = 0; i < contacts.size(); i++) {
			Contact c = contacts.get(i);
			JSONObject cj = c.toJSONObject();
			contactArr.put(cj);
			Log.d("appserv",
					"id: " + c.getId() + " name: " + c.getDisplayName()
							+ " times: " + c.getTimesContacted());
		}
		return contactArr;
	}

	/**
	 * for learning only
	 * 
	 * @return
	 */
	@Deprecated
	public List<Contact> getAllFullContacts() {
		long start = System.currentTimeMillis();
		List<Contact> contacts = new ArrayList<Contact>();

		// first query all basic contacts info
		Cursor bcCur = cr.query(Contacts.CONTENT_URI, new String[] {
				Contacts._ID, Contacts.DISPLAY_NAME, Contacts.TIMES_CONTACTED,
				Contacts.IN_VISIBLE_GROUP }, null, null, null);
		int bcIdColIndex = bcCur.getColumnIndex(Contacts._ID);
		int bcNameColIndex = bcCur.getColumnIndex(Contacts.DISPLAY_NAME);
		int ivgIndex = bcCur.getColumnIndex(Contacts.IN_VISIBLE_GROUP);
		if (bcCur.moveToFirst()) {
			while (!bcCur.isAfterLast()) {
				int contactID = bcCur.getInt(bcIdColIndex);
				String name = bcCur.getString(bcNameColIndex);
				int ivg = bcCur.getInt(ivgIndex);
				Log.d("appserv", "id: " + contactID + " name: " + name
						+ " in visible group: " + ivg);

				Contact contact = new Contact();
				contact.setId(contactID);
				contact.setDisplayName(name);

				// query all phone numbers
				Cursor phCur = cr.query(Phone.CONTENT_URI, PHONE_PROJECTION,
						Phone.CONTACT_ID + "=?",
						new String[] { String.valueOf(contactID) }, null);
				List<String> phones = new ArrayList<String>();
				int phoneIndex = phCur.getColumnIndex(Phone.NUMBER);
				if (phCur.moveToFirst()) {
					while (!phCur.isAfterLast()) {
						String phoneNum = phCur.getString(phoneIndex);
						Log.d("appserv", "phone: " + phoneNum);
						phones.add(phoneNum);
						phCur.moveToNext();
					}
				}
				phCur.close();
				contact.setPhones(phones);

				// query groups
				List<Group> groups = new ArrayList<Group>();
				Cursor gpMemshipCur = cr.query(Data.CONTENT_URI, new String[] {
						GroupMembership._ID, GroupMembership.GROUP_ROW_ID },
						GroupMembership.CONTACT_ID + "=?" + " AND "
								+ Data.MIMETYPE + "=" + "'"
								+ GroupMembership.CONTENT_ITEM_TYPE + "' ",
						new String[] { String.valueOf(contactID) }, null);
				int gmidIndex = gpMemshipCur
						.getColumnIndex(GroupMembership._ID);
				int gpIDColIndex = gpMemshipCur
						.getColumnIndex(GroupMembership.GROUP_ROW_ID);
				if (gpMemshipCur.moveToFirst()) {
					while (!gpMemshipCur.isAfterLast()) {
						Group group = new Group();
						int gmid = gpMemshipCur.getInt(gmidIndex);
						Log.d("appserv", "group membership id: " + gmid);

						int groupID = gpMemshipCur.getInt(gpIDColIndex);
						group.setId(groupID);
						// query group name by id
						Cursor groupCur = cr.query(Groups.CONTENT_URI,
								new String[] { Groups.TITLE }, Groups._ID
										+ "=?",
								new String[] { String.valueOf(groupID) }, null);
						int gpNameIndex = groupCur.getColumnIndex(Groups.TITLE);
						groupCur.moveToFirst();
						String gpName = groupCur.getString(gpNameIndex);
						Log.d("appserv", "group name: " + gpName);
						group.setTitle(gpName);

						groups.add(group);
						gpMemshipCur.moveToNext();
					}
				}
				gpMemshipCur.close();
				contact.setGroups(groups);

				contacts.add(contact);
				bcCur.moveToNext();
			}
		}

		bcCur.close();
		long end = System.currentTimeMillis();
		Log.d("appserv", "get all time: " + (end - start));
		return contacts;
	}

	private boolean matchString(String queryStr, String sentence) {
		if (queryStr == null || sentence == null) {
			return false;
		}
		boolean match = false;
		int j = 0;
		for (int i = 0; i < sentence.length(); i++) {
			char s = sentence.charAt(i);
			char q = queryStr.charAt(j);

			if (s == q) {
				j++;
				if (j >= queryStr.length()) {
					match = true;
					break;
				}
			}

		}

		return match;
	}

	/**
	 * search contacts with compound searching method, including search by
	 * display name, search by pinyin name and search by phone number
	 * 
	 * @param queryStr
	 * @param contacts
	 * @return
	 */
	public List<Contact> search(String queryStr, List<Contact> contacts) {
		List<Contact> result = null;
		if (queryStr == null || queryStr.equals("") || contacts == null) {
			result = contacts;
		} else {
			// check if queryStr is number
			try {
				Long.parseLong(queryStr);
				// it's number, search by number
				result = searchByNumber(queryStr, contacts);
			} catch (NumberFormatException e) {
				// it's not number, search by name
				result = searchByName(queryStr, contacts);
			}
		}
		return result;
	}

	/**
	 * search contacts in the given contact list
	 * 
	 * @param queryStr
	 * @param contacts
	 * @return
	 */
	public List<Contact> searchByName(String queryStr, List<Contact> contacts) {
		List<Contact> result = null;
		long starttime = System.currentTimeMillis();
		if (queryStr == null || queryStr.equals("") || contacts == null) {
			result = contacts;
		} else {
			String firstChar = queryStr.substring(0, 1);
			if (firstChar.matches(CHAR_NUMBER_REG)) {
				// search by pinyin name
				result = searchByPinyinName(queryStr, contacts);
			} else {
				result = searchByDisplayName(queryStr, contacts);
			}
		}

		long endtime = System.currentTimeMillis();
		Log.d("appserv", "search time: " + (endtime - starttime));
		return result;
	}

	private List<Contact> searchByDisplayName(String queryStr,
			List<Contact> contacts) {
		List<Contact> result = new ArrayList<Contact>();
		if (queryStr == null || queryStr.equals("") || contacts == null) {
			result = contacts;
		} else {
			for (int i = 0; i < contacts.size(); i++) {
				Contact c = contacts.get(i);
				String displayName = c.getDisplayName();
				if (displayName.indexOf(queryStr) >= 0) {
					result.add(c);
				}
			}
		}
		return result;
	}

	private List<Contact> searchByPinyinName(String queryStr,
			List<Contact> contacts) {
		List<Contact> result = new ArrayList<Contact>();
		if (queryStr == null || queryStr.equals("") || contacts == null) {
			result = contacts;
		} else {
			queryStr = queryStr.toLowerCase();
			// Log.d("appserv", "search - query string: " + queryStr);
			if (queryStr.length() == 1) {
				// search in short names
				for (int i = 0; i < contacts.size(); i++) {
					Contact c = contacts.get(i);
					List<String> shortNames = c.getShortPinyinNames();
					boolean isMatch = false;
					for (String name : shortNames) {
						if (name.indexOf(queryStr) >= 0) {
							isMatch = true;
							break;
						}
					}

					if (isMatch) {
						result.add(c);
					}
				}
			} else {
				// search in full names
				for (int i = 0; i < contacts.size(); i++) {
					Contact c = contacts.get(i);
					List<String> fullNames = c.getFullPinyinNames();
					boolean isMatch = false;
					for (String name : fullNames) {
						if (matchString(queryStr, name)) {
							// Log.d("appserv", "search - match name: " + name);
							isMatch = true;
							break;
						}
					}
					if (isMatch) {
						result.add(c);
					}
				}
			}
		}
		return result;
	}

	/**
	 * search contacts from current contact list (which is got nearly)
	 * 
	 * @param queryStr
	 * @return
	 */
	public List<Contact> searchByNameFromCurrentContactList(String queryStr) {
		return searchByName(queryStr, currentContactList);
	}

	public JSONArray searchByNameFromCurrentContactListJSON(String queryStr) {
		List<Contact> contacts = searchByNameFromCurrentContactList(queryStr);
		JSONArray contactArr = new JSONArray();
		if (contacts != null) {
			for (int i = 0; i < contacts.size(); i++) {
				Contact c = contacts.get(i);
				JSONObject cj = c.toJSONObject();
				contactArr.put(cj);
				Log.d("appserv",
						"id: " + c.getId() + " name: " + c.getDisplayName()
								+ " times: " + c.getTimesContacted());
			}
		}
		return contactArr;
	}

	/**
	 * search contacts by phone number
	 * 
	 * @param number
	 *            - phone number
	 * @param contacts
	 *            - contact list to search
	 * @return
	 */
	public List<Contact> searchByNumber(String number, List<Contact> contacts) {
		long starttime = System.currentTimeMillis();
		List<Contact> result = new ArrayList<Contact>();

		if (number == null || number.equals("") || contacts == null) {
			result = contacts;
		} else {
			for (int i = 0; i < contacts.size(); i++) {
				Contact c = contacts.get(i);
				List<String> phones = c.getPhones();
				if (phones != null) {
					boolean isMatch = false;
					for (String phone : phones) {
						if (phone.indexOf(number) >= 0) {
							isMatch = true;
							break;
						}
					}
					if (isMatch) {
						result.add(c);
					}
				}
			}
		}
		long endtime = System.currentTimeMillis();
		Log.d("appserv", "search time: " + (endtime - starttime));
		return result;
	}

	/**
	 * search contacts by phone number from current contact list(which is got
	 * nearly)
	 * 
	 * @param number
	 * @return
	 */
	public List<Contact> searchByNumberFromCurrentContactList(String number) {
		return searchByNumber(number, currentContactList);
	}

	/**
	 * search contacts by phone number from current contact list(which is got
	 * nearly) within JSONArray format
	 * 
	 * @param number
	 * @return
	 */
	public JSONArray searchByNumberFromCurrentContactListJSON(String number) {
		List<Contact> contacts = searchByNumberFromCurrentContactList(number);
		JSONArray contactArr = new JSONArray();
		if (contacts != null) {
			for (int i = 0; i < contacts.size(); i++) {
				Contact c = contacts.get(i);
				JSONObject cj = c.toJSONObject();
				contactArr.put(cj);
				Log.d("appserv", cj.toString());
			}
		}
		return contactArr;
	}
}
