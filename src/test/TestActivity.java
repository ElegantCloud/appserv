package test;

import com.ivyinfo.contact.ContactManager;
import com.ivyinfo.contact.R;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TestActivity extends Activity {
	private ContactManager cm;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("appserv", "TestActivity on create");
		setContentView(R.layout.main);
		
		Button insertContact = (Button) findViewById(R.id.insert_contact);
		insertContact.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_INSERT);
//				intent.setClassName("com.android.contacts.ui",
//						"com.android.contacts.ui.EditContactActivity");
				// intent.setType("vnd.android.cursor.dir/person");
				// intent.setType("vnd.android.cursor.dir/contact");
				intent.setType("vnd.android.cursor.dir/raw_contact");
				startActivity(intent);
			}
		});
		LinearLayout ll = (LinearLayout) findViewById(R.id.topparent);
//		View view1 = LayoutInflater.from(this).inflate(R.layout.test, null);
//		View view2 = LayoutInflater.from(this).inflate(R.layout.test, null);
//		ll.addView(view1);
//		ll.addView(view2);
		
		TextView t1 = new TextView(this);
		t1.setText("hello 1");
		TextView t2 = new TextView(this);
		t2.setText("hello 2");
		ll.addView(t1);
		ll.addView(t2);
		
		Button viewContact = (Button) findViewById(R.id.view_contact);
		viewContact.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(ContentUris.withAppendedId(Contacts.CONTENT_URI,
						515));
				startActivity(intent);
			}
		});
		
		cm = new ContactManager(this);
		
		cm.getAllContactsWithPhone();
		
//		cm.getAllFullContacts();
//		cm.getAllContacts();
//		cm.getAllContactsJSON();
//		cm.getAllContactsByCompoundSort();
//		cm.getAllContactsByCompoundSortJSON();
//		cm.getAllContactsByNameSortJSON();
//		cm.getAllGroups();
//		cm.getContactsByGroupByNameSortJSON(1);
//		cm.getContactsByGroupByCompoundSortJSON(2);
//		cm.searchByNameFromCurrentContactListJSON("zxy");
//		Log.d("appserv", "################");
//		cm.searchByNameFromCurrentContactListJSON("zshuddddddd");
//		Log.d("appserv", "################");
//		cm.searchByNameFromCurrentContactListJSON("hghui");
//		Log.d("appserv", "################");
//		cm.searchByNameFromCurrentContactListJSON("dishao");
		
//		Log.d("appserv", "################");
//		cm.searchByNumberFromCurrentContactListJSON("139");
//		Log.d("appserv", "################");
//		cm.searchByNumberFromCurrentContactListJSON("45");
//		Log.d("appserv", "################");
	}

}
