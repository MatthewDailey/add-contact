package add.contact;

import java.util.ArrayList;
import java.util.HashMap;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;

/**
 * ViewRecentContacts
 * @author Matt
 *
 * This activity allows users to view their contacts list in the most
 * recently added order. The layout is a simple list of contacts, when 
 * clicked they display the contact page for that contact.
 */
public class ViewRecentContacts extends Activity 
{
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		/* display loading ui while the contacs load */
		super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.progress);

        /* Execute task to get contact list. */
        new LoadContacts(getContentResolver()).execute();
    }
	
	@Override
	public void onRestart()
	{
		super.onRestart();
		/* display loading page */
        setContentView(R.layout.progress);
        
        /* asynchronously load contacts list */
        new LoadContacts(getContentResolver()).execute();
	}
	
	/**
	 * LoadContacts
	 * @author Matt
	 *
	 * Asynchronous task to load contacts without blocking the ui thread.
	 * On completely loading contacts, the task then displays them in a list.
	 */
	class LoadContacts extends AsyncTask<Void, Integer, Integer>
    {
    	/* content resolver to query the contacts */
    	private ContentResolver cr;
    	/* array to hold names of contacts */
    	ArrayList<String> names;
    	/* array to hold contact ids */
    	ArrayList<String> keys;
    	
    	public LoadContacts( ContentResolver cr )
    	{
    		this.cr = cr;
    		this.names = new ArrayList<String>();
			this.keys = new ArrayList<String>();
    	}
    	
    	/**
    	 * Method to do in backgroud. Gets a cursor over contacts and 
    	 * get their lookup key and display name which are saved in class
    	 * arrays which onPostExecute will use to display the list.
    	 * 
    	 * THis returns integer because it must return a non-Void value 
    	 * to trigger the onPostExecute callback.
    	 */
    	@Override
		protected Integer doInBackground(Void... params) {
    		Cursor c = getContactsCursor();
			
			try
			{
				while(c.moveToNext())
				{
					String name = c.getString(c.getColumnIndex(
							ContactsContract.Contacts.DISPLAY_NAME));
					String key = c.getString(c.getColumnIndex(
							ContactsContract.Contacts.LOOKUP_KEY));
					this.keys.add(key);
					this.names.add(name);
				}
			}
			finally
			{
				c.close();
			}
			return null;
		}

    	/**
    	 * Called on completion of doInBackground. This takes the list of
    	 * names and lookup keys and displays a list of contact names which
    	 * can be clicked to access the contact's page. 
    	 */
		protected void onPostExecute(Integer result)
		{
			/* set the layout to the new contact page */
			setContentView(R.layout.contact_manager);
			/* number the names to make it clear the names are in order */
			ArrayList<String> numbered_names = new ArrayList<String>();
			/* map from name to key so we can get look up key from a name */
			final HashMap<String,String> name_to_key = 
					new HashMap<String,String>();
			
			/* update the names to include their number */
			for( int i = 1; i < this.names.size()+1; i++)
			{
				numbered_names.add( i + ". " + this.names.get(i-1) );
			}
			
			/* fill in map from names to keys */
			for( int i = 0; i < this.keys.size(); i++)
			{
				name_to_key.put(numbered_names.get(i), this.keys.get(i));
			}
			
			/* get the list view to put contacts in */
			ListView lv = (ListView) findViewById(R.id.contactList);
			
			/* set the list items to be the numbered names */
			lv.setAdapter((ListAdapter) new ArrayAdapter<String>(
					ViewRecentContacts.this,R.layout.list_item, numbered_names));

			/* set the onclick listener for the contact list */
			lv.setOnItemClickListener(
					new OnItemClickListener()
					{
						/* Launch the contact info on click */
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1,
								int arg2, long arg3) {
							/* get the lookup key of the click contact */
							String key = name_to_key.get(((TextView) arg1).
									getText());
							
							/* use the key to launch contact lookup activity */
							Intent intent = new Intent(Intent.ACTION_VIEW);
							Uri uri = Uri.withAppendedPath(
									ContactsContract.Contacts.
									CONTENT_LOOKUP_URI, key);
							intent.setData(uri);
							startActivity(intent);
						}
						
					});

		}

		/**
		 * Get a list of all visible contacts in reverse order of being added.
		 * 
		 * The table does not actually store the date a contact was added 
		 * but it is reasonable to make the assumption that table rows are 
		 * added at the end of the db so row ids are used to get an ordering
		 * of contact addition times.
		 *
		 * @return - cursor over all visible contacts in order of addition,
		 * newest first.
		 */
		private Cursor getContactsCursor()
		{
			/* create uri to look up contacts */
	        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
	        /* get important columns */
	        String[] projection = new String[] {
	                ContactsContract.Contacts._ID,
	                ContactsContract.Contacts.DISPLAY_NAME,
	                ContactsContract.Contacts.LOOKUP_KEY
	        };
	        
	        /* make sure we only get visible contacts */
	        String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP 
	        		+ " = '1'";
	        String[] selectionArgs = null;
	        /* sort by id in descending order */
	        String sortOrder = ContactsContract.Contacts._ID +
	        		" COLLATE LOCALIZED DESC";
	        
	        /* return cursor */
	        return cr.query(uri, projection, selection, selectionArgs, sortOrder);
		}

		
    }
}
