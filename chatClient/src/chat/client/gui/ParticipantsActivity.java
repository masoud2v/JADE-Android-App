/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package chat.client.gui;

import java.util.HashMap;
import java.util.logging.Level;

import chat.client.agent.ChatClientInterface;
import android.database.Cursor;
import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.provider.ContactsContract.CommonDataKinds.*;

/**
 * This activity implement the participants interface.
 * 
 * @author Michele Izzo - Telecomitalia
 * edited by Satish Inampudi
 */

public class ParticipantsActivity extends ListActivity {
	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	private MyReceiver myReceiver;
	private String nickname;
	private ChatClientInterface chatClientInterface;

	private HashMap<String, String> myContacts = new HashMap<String, String>(); //myContacts gets All contacts using Contacts Content Provider 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			nickname = extras.getString("nickname");
		}

		try {
			chatClientInterface = MicroRuntime.getAgent(nickname).getO2AInterface(ChatClientInterface.class);
		} catch (StaleProxyException e) {
			e.printStackTrace();
		} catch (ControllerException e) {
			e.printStackTrace();
		}

		myReceiver = new MyReceiver();

		IntentFilter refreshParticipantsFilter = new IntentFilter();
		refreshParticipantsFilter.addAction("jade.demo.chat.REFRESH_PARTICIPANTS");
		registerReceiver(myReceiver, refreshParticipantsFilter);

		setContentView(R.layout.participants);
		setListAdapter(new ArrayAdapter<String>(this, R.layout.participant, checkContacts()));

		ListView listView = getListView();
		listView.setTextFilterEnabled(true);
		listView.setOnItemClickListener(listViewtListener);
	}

	/*
	 * TASK 2: This method is called when participant's name is clicked in participants list.
	 * This will create a new contact if participant is not in user's contacts.
	 */
	private OnItemClickListener listViewtListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			String clickedParticipant = (String) ((ListView) parent).getItemAtPosition(position);
			if (clickedParticipant.contains("[N]")) {
				clickedParticipant = clickedParticipant.substring(0, clickedParticipant.indexOf("[N]")-1); //Assuming participant's name does not contain [N] substring.
				ContentValues values = new ContentValues();
				Uri rawContactUri = getContentResolver().insert(RawContacts.CONTENT_URI, values);
				long rawContactId = ContentUris.parseId(rawContactUri);
				values.clear();
				values.put(Data.RAW_CONTACT_ID, rawContactId);
				values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
				values.put(StructuredName.DISPLAY_NAME, clickedParticipant);
				values.put(StructuredName.GIVEN_NAME, clickedParticipant);
				values.put(StructuredName.FAMILY_NAME, clickedParticipant);
				Uri dataUri = getContentResolver().insert(Data.CONTENT_URI, values);
				System.out.println(dataUri.toString());
			}
			finish();
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(myReceiver);

		logger.log(Level.INFO, "Destroy activity!");
	}

	/*
	 * TASK2: This Method concatenated [Y] or [N] based on presence of participant in contacts
	 */
	private String[] checkContacts() {
		String[] participants = chatClientInterface.getParticipantNames();
		Cursor c = null;
		try {
			c = getContentResolver().query(Data.CONTENT_URI, new String[] { StructuredName.GIVEN_NAME, StructuredName.FAMILY_NAME }, null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (c != null) {
			int givenNameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
			int familyNameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
			while (c.moveToNext()) {
				myContacts.put(c.getString(givenNameIndex), c.getString(familyNameIndex));
			}
		}

		for (int i = 0; i < participants.length; i++) {
			if (myContacts.containsKey(participants[i])) {
				participants[i] = participants[i].concat(" [Y]");
			} else
				participants[i] = participants[i].concat(" [N]");
		}
		return participants;
	}

	private class MyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			logger.log(Level.INFO, "Received intent " + action);
			if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_PARTICIPANTS")) {
				setListAdapter(new ArrayAdapter<String>(ParticipantsActivity.this, R.layout.participant, checkContacts()));
			}
		}
	}

}
