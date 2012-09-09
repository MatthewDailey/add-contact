package add.contact;

import java.util.ArrayList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * AddFromText
 *
 * This activity allows the user to add a new contact by selecting a recent
 * text message from a list with the new contacts name. It sets the contact
 * number as the address of the incoming text and the name as the body of 
 * the message. 
 * 
 * The application will prevent a user from adding a multiple contacts and 
 * popup a message displaying the name of the contact with the nubmer they
 * tried to add again. It will also provide a second check when a user 
 * selects a message to make sure erronious contacts are not added if the 
 * wrong text is selected. Once a contact is successfully added, the activity
 * will finish and leave the user with a message saying teh contact was added.
 * 
 * @author Matt
 */
public class AddFromText extends Activity {

	/* Async task to fetch recent text messages */
	private LoadMessages retreiver;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        /* display loading screen and launch async loading task */
        setContentView(R.layout.progress);
        retreiver = new LoadMessages(getContentResolver());
        retreiver.execute();
    }

    /*
     * Override to make sure to cancel the async task if the loading is pause.
     * 
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause()
    {
    	super.onPause();
    	retreiver.cancel(true);
    }
	
    /**
     * LoadMessages
     * @author Matt
     *
     * Asynchronous task to load contacts so that the loading process is not
     * tied to the UI thread. 
     */
    class LoadMessages extends AsyncTask<Void,Integer,ArrayList<TextInfo>>
    {
    	/* content resolver used for querys, passed by the calling activity */
    	ContentResolver cr;
    	/* uri location of the sms message on the phone.
    	 * note that this is not standardized and there is no standard API
    	 * for handling SMS message so this may not be correct. 	 */
    	private final Uri SMS_LOCATION;
    	
    	public LoadMessages(ContentResolver c)
    	{
    		this.cr = c;
    		SMS_LOCATION = Uri.parse("content://sms");
    	}
    	
    	/**
    	 * Task done in background to query the text messages and load a list
    	 * of the most recent text messages.
    	 * 
    	 * The return value of this method is passed to onPostExecute.
    	 */
		@Override
		protected ArrayList<TextInfo> doInBackground(Void... arg0) 
		{
			/* order the messages in decending order by date */
			String sortOrder = "date COLLATE LOCALIZED DESC";
			/* get only incoming messages */
			String mask = "type='1'";
			/* project over relevant columns of hte table */
			String[] projection = {"body", "address", "date" };
			/* query to get cursor over the resulting rows */
			Cursor c = cr.query(SMS_LOCATION, projection, mask, null, sortOrder);
			
			ArrayList<TextInfo> recentTexts = new ArrayList<TextInfo>(); 

			/* add at most 50 most recent text messages and create TextInfo
			 * objects out of them to return.  */
			int cnt = 0;
			while( c.moveToNext() && cnt < 50)
			{
				TextInfo ti = new TextInfo();
				/* get the address which is the phone number then getName() to
				 * try to load the actual name of that contact if it exists 
				 * already */
				ti.setName(getName(c.getString(c.getColumnIndex("address"))));
				/* simply load the body of the text */
				ti.setMsg(c.getString(c.getColumnIndex("body")));
				recentTexts.add(ti);
				cnt++;
			}
			
			return recentTexts;
		}
    	
		/**
		 * get the name of a contact from phone number.
		 * 
		 * @param number - number of contact. 
		 * @return - name of contact if there exists a contact with that
		 * name, otherwise returns input number.
		 */
		private String getName(String number)
		{
			/* get uri of phone numbers */
			Uri phone_num_uri = Uri.withAppendedPath(ContactsContract.
					PhoneLookup.CONTENT_FILTER_URI,Uri.encode(number));
			/* get name column and query*/
			String[] projection = {ContactsContract.
					PhoneLookup.DISPLAY_NAME };
			Cursor c = cr.query(phone_num_uri, projection,	
					null, null, null);
			
			try
			{
				/* check the first contact in the cursor, if none, return input. */ 
				if(c.moveToFirst())
				{
					return c.getString(c.getColumnIndex(
							ContactsContract.PhoneLookup.DISPLAY_NAME));
				}
				else
				{
					return number;
				}
			}
			finally
			{
				c.close();
			}
		}
		
		/**
		 * Called when doInBackgroud returns with argument the return value 
		 * of doInBackground.
		 * 
		 * Displays the list of recent texts and builds/assigns the onclick
		 * listeners to call add contact methods.
		 */
		protected void onPostExecute(ArrayList<TextInfo> texts)
		{
	        setContentView(R.layout.activity_add_from_text);
	        
	        
	        /* get the contact list view */
	        final ListView lv1 = (ListView) findViewById(R.id.ListView01);
	        lv1.setAdapter(new CustomTextBaseAdapter(AddFromText.this, texts));
	        
	        /* set onclick adapter for list items */
	        lv1.setOnItemClickListener(new OnItemClickListener() {

	        	/**
	        	 * method to double check that the user selected the desired 
	        	 * text.
	        	 */
	            private void queryCorrectContact(final String name, 
	            		final String msg)
	            {
	            	/* create an alert dialog */
	            	AlertDialog.Builder builder = new AlertDialog.
	            			Builder(AddFromText.this);
	            	
	            	/* build the alert */
	            	builder.setMessage("Add contact "+ msg +"?")
	            	       .setCancelable(false)
	            	       /* yes-button code */
	            	       .setPositiveButton("Yes", new DialogInterface.
	            	    		   OnClickListener() 
	            	       {
	            	    	   /* user selected the correct contact so we
	            	    	    * add the contact and pop a toast message.
	            	    	    * then close the activity.
	            	    	    */
	            	           public void onClick(DialogInterface dialog, 
	            	        		   int id) 
	            	           {
	            	        	   /* try to add contact and alert user if 
	            	        	    * we fail with runtime. */
	            	        	   try
	            	        	   {
	            	        		   Util.addContact(AddFromText.this, 
	           	        					name, msg );
	            	        		   /* try to send text and alert user
	            	        		    * if we fail.    */
	            	        		   try
	            	        		   {
	            	        			   Util.toast_msg(AddFromText.this, 
	           	        					"Added Contact: "+msg);
	            	        		   }
	            	        		   catch(Exception e)
	            	        		   {
	            	        			   Util.toast_msg(AddFromText.this,
	            	        					 "Failed to send name to" +
	            	        					   "new contact.");
	            	        		   }
	            	        		  
	            	        	   }
	            	        	   catch(Exception e)
	            	        	   {
	            	        		   Util.toast_msg(AddFromText.this,
	            	        				   "Failed to add contact.");
	            	        	   }
	            	        		  AddFromText.this.finish();
	            	        		dialog.cancel();
	            	           }
	            	       })
	            	       /* no-button code */
	            	       .setNegativeButton("No", new DialogInterface.
	            	    		   OnClickListener() 
	            	       {
	            	           public void onClick(DialogInterface dialog, 
	            	        		   int id) 
	            	           {
	            	        	   /* change the button back to off and leave 
	            	        	    * dialog */
	            	                dialog.cancel();
	            	           }
	            	       });
	            	
	            	/* Build the alert and show it */
	            	AlertDialog alert = builder.create();
	            	alert.show();
	            }
	        	
	            /**
	             * Build the on click listener to check if the name of the 
	             * text is a contact name or a phone number so we only add
	             * new contacts. 
	             */
	        	@Override
		        public void onItemClick(AdapterView<?> a, View v, int position,
		        		long id) 
	        	{
	        		/* get TextInfo object represented by the list item */
	        		TextInfo selected_text = (TextInfo) 
	        				lv1.getItemAtPosition(position);
	        		/* check if the name is actually a number of the name of
	        		 * a preexisting contact */
	        		if( Util.isInteger(selected_text.getName()) )
	        		{
	        			queryCorrectContact(selected_text.getName(), selected_text.getMsg());
	        		}
	        		else
	        		{
	        			Util.toast_msg(AddFromText.this,"Contact already exists: "+selected_text.getName());
	        		}
	        		
	        	}  
	        });			
		}
    }
    
    /**
     * Set up options menu to either set name or how-to
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
    	/* create a set name option in the menu */
    	MenuItem setName = menu.add("Set Name");
    	setName.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item) 
			{
				Intent i = new Intent();
				i.setClass(AddFromText.this, SetName.class);
				AddFromText.this.startActivity(i);
				return true;
			}
    	});
    	
    	/* create an info option in the menu */
    	MenuItem info = menu.add("How to use Add Contact");
    	info.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item) 
			{
				Intent i = new Intent();
				i.setClass(AddFromText.this, HowToUse.class);
				AddFromText.this.startActivity(i);
				return true;
			}
    	});
    	
        getMenuInflater().inflate(R.menu.activity_add_from_text, menu);
        return true;
    }
    
    /**
     * CustomTextBaseAdapter
     *
     * Create a listview from a arraylist of TextInfo objects. The listview
     * shows the name of a preexisting contact or the number of a new contact
     * followed by the content of hte text message.
     * 
     * @author Matt Dailey
     */
    public class CustomTextBaseAdapter extends BaseAdapter {
    	
    	private ArrayList<TextInfo> searchArrayList;
    	 
    	private LayoutInflater mInflater;

    	public CustomTextBaseAdapter(Context context, ArrayList<TextInfo> results) {
    		searchArrayList = results;
    		mInflater = LayoutInflater.from(context);
    	}

    	public int getCount() {
    		return searchArrayList.size();
    	}

    	public Object getItem(int position) {
    		return searchArrayList.get(position);
    	}

    	public long getItemId(int position) {
    		return position;
    	}

    	public View getView(int position, View convertView, ViewGroup parent) {
    		ViewHolder holder;
	    	if (convertView == null) {
	    		convertView = mInflater.inflate(R.layout.custom_text_layout, null);
	    	    holder = new ViewHolder();
	    	    holder.txtName = (TextView) convertView.findViewById(R.id.name_or_number);
	    	    holder.txtPhone = (TextView) convertView.findViewById(R.id.msg);
	
	    	    convertView.setTag(holder);
	    	} 
	    	else 
	    	{
	    		holder = (ViewHolder) convertView.getTag();
	    	}
	    	  
	    	holder.txtName.setText(searchArrayList.get(position).getName());
	    	holder.txtPhone.setText(searchArrayList.get(position).getMsg());
	
	    	return convertView;
    	}
    	
    	class ViewHolder 
    	{
    		TextView txtName;
	    	TextView txtPhone;
    	}
   	}
    
    /**
     * TextInfo
     *
     * Object representing a text message storing the name of a prexisting
     * contact or a number if no name is assigned to it as well as the 
     * body of the message.
     * 
     * @author Matt Dailey
     */
    private class TextInfo
    {
    	private String name;
    	private String msg;
    	
    	public void setMsg(String m)
    	{
    		this.msg = m;
    	}
    	
    	public void setName(String n)
    	{
    		this.name = n;
    	}
    	
    	public String getName()
    	{
    		return this.name;	
    	}
 
    	public String getMsg()
    	{
    		return this.msg;
    	}
    	
    }
}
