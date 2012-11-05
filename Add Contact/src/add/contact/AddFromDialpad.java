package add.contact;

import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v4.app.FragmentActivity;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ToggleButton;

/**
 * AddFromDialpad Activity
 * @author Matt Dailey
 * 
 * In this activity the user adds a name and number as a contact. The number
 * is automatically set as the contacts mobile number. The activity also allows
 * the user the option to send a text with his/her name to the new contact.
 * 
 * The layout is 2 text inputs (name and phone) with a toggle button to 
 * set whether to send the name text and a button to submit the contact. THe
 * toggle button checks if a name has been set in the application and if none
 * exists, it will prompt the user for a name, which they can choose not to set.
 * This will then cause the app to send a text with a short message saying no
 * name has been set. If the contact is successfully added, the activity will
 * exit leaving a popup message saying the contact has been added.
 */
public class AddFromDialpad extends FragmentActivity {
	
	/* name of preference file to look up chosen name to send */
    public static final String PREFS_NAME = "NameFile";
    /* name of prefence file to look up saved state of inputs */
    public static final String SAVED_INPUTS = "InputsFile";
    /* message to send if no name is set */
    public static final String NO_NAME = "No name is set in Add Contact but " +
    		"here is my number";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_from_dialpad);

        prepareScreen();
    }

    @Override 
    public void onRestart()
    {
    	super.onRestart();
    	
        setContentView(R.layout.add_from_dialpad);
        
        prepareScreen();
    }
    
    /**
     * Open options list which allows the user to change the name which
     * will be texted to new contacts as well as an information page.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {	
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.activity_menu, menu);
    	
    	/* create a set name option in the menu */
    	MenuItem setName = menu.getItem(1);
    	setName.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item) 
			{
				Intent i = new Intent();
				i.setClass(AddFromDialpad.this, SetName.class);
				AddFromDialpad.this.startActivity(i);
				return true;
			}
    	});
    	
    	/* create an info option in the menu */
    	MenuItem info = menu.getItem(0);
    	info.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item) 
			{
				Intent i = new Intent();
				i.setClass(AddFromDialpad.this, HowToUse.class);
				AddFromDialpad.this.startActivity(i);
				return true;
			}
    	});
    	
    	
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    /**
     * Set onclick listeners for both the toggle button and the submit button.
     * 
     * The toggle button must check if a name has been set when a user attempts
     * to toggle on the message sending capabilities.
     */
    private void prepareScreen()
    {
    	/* set addcontact button to submit contact */
    	Button b = (Button) this.findViewById(R.id.add_dial_button);
    	b.setOnClickListener( new OnClickListener()
    	{
			@Override
			public void onClick(View arg0) 
			{
				/* get name input */
		    	EditText name_text = (EditText) AddFromDialpad.this.
		    			findViewById(R.id.name_input);
		    	String name = name_text.getText().toString();
		    	
		    	/* validate name input, otherwise pop up a reminder */
		    	if(name.length() < 1)
		    	{
		    		Util.toast_msg(AddFromDialpad.this, "Enter contact name.");
		    		return;
		    	}
		    	
		    	/* get phone input and validate */
		    	EditText phone_text = (EditText) AddFromDialpad.this.
		    			findViewById(R.id.phone_number_input);
		    	String phone = phone_text.getText().toString();
		    	
		    	/* check if the phone number is 10 digit int */
		    	if(phone.length() != 10 || !Util.isInteger(phone) )
		    	{
		    		Util.toast_msg(AddFromDialpad.this, 
		    				"Check that phone number is 10 digits.");
		    		return;
		    	}
				
		    	/* add the contact and send the message, then close the 
		    	 * activity. Catch any exception and alert gracefully */
		    	try
		    	{
		    		Util.addContact(AddFromDialpad.this, phone, name);
		    		
		    		/* attempt to send text, if no exception was thrown 
		    		 * adding */
		    		try
			    	{
			    		sendText(phone);
			    	}
			    	catch(Exception e)
			    	{
			    		Util.toast_msg(AddFromDialpad.this, "Failed to " +
			    				"send text to new contact." );
			    	}
		    	}
		    	catch(Exception e)
		    	{
		    		Util.toast_msg(AddFromDialpad.this, 
		    				"Failed to add contact.");
		    	}
		    	
		    	/* finally close the activity */
				finish();
			}
    	});
    	
    	/* set toggle to query for a name if necessary */
    	ToggleButton shouldSend = (ToggleButton) this.
    			findViewById(R.id.should_text_toggle);
    	shouldSend.setOnCheckedChangeListener(new OnCheckedChangeListener()
    	{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, 
					boolean isChecked) {
				/* if the user is trying to send message, and no name is set
				 * ask them to set one. */
				if(isChecked && getName().equals(NO_NAME))
				{
					queryName();
				}
			}
    	});
    }

    /**
     * Send a text to the input number with the content being the name
     * set in the application.
     * 
     * @param number - phone number to send SMS text message to.
     */
    private void sendText(String number)
    {
    	String name = getName();
    	
    	SmsManager.getDefault().sendTextMessage(number, null, name, 
    			null, null);
    }

    /**
     * Check to see if a name is set in the application preferences 
     * and retrieve it, otherwise return a default string.
     * 
     * @return - user name if it exist otherwise, default string NO_NAME
     */
    private String getName()
    {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        
        /* get the name, or a useful message */
        String name = settings.getString("name", NO_NAME);
        
        return name;
    }

    /**
     * Pop up a message asking the user if they would like to set a name
     * in the application preferences. If so, launch an activity to do so.
     */
    private void queryName()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	
    	/* create intent to start the name setting activity */
    	final Intent i = new Intent();
    	i.setClass(this, SetName.class);
    	
    	/* set up alert builder */
    	builder.setMessage("There is no name associated with this app. " +
    			"Would you like to add one?")
    	       .setCancelable(false)
    	       /* add a yes button */
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() 
    	       {
    	           public void onClick(DialogInterface dialog, int id) 
    	           {
    	                dialog.cancel();
    	                /* launch the set name activity */
    	                startActivity(i);
    	           }
    	       })
    	       /* add a no button */
    	       .setNegativeButton("No", new DialogInterface.OnClickListener() 
    	       {
    	           public void onClick(DialogInterface dialog, int id) 
    	           {
    	        	   /* change the button back to off and leave dialog */
    	        	   ((ToggleButton)AddFromDialpad.this.findViewById(
    	        			   R.id.should_text_toggle)).setChecked(false);
    	                dialog.cancel();
    	           }
    	       });
    	
    	/* build and show the alert */
    	AlertDialog alert = builder.create();
    	alert.show();
    }
        

    
    /*
     * Overriden to save input values for name and phone number.
     * 
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
    	super.onPause();
    	/* get the preference file to edit */
    	SharedPreferences settings = getSharedPreferences(SAVED_INPUTS, 0); 
    	
    	/* get the fields with values to save */
    	EditText phone_text = (EditText) AddFromDialpad.this.
    			findViewById(R.id.phone_number_input);
    	EditText name_text = (EditText) AddFromDialpad.this.
    			findViewById(R.id.name_input);
    	ToggleButton shouldSend = (ToggleButton) this.
    			findViewById(R.id.should_text_toggle);
    	
    	/* create editor and set current values */
    	Editor editSettings = settings.edit();
    	
    	editSettings.putBoolean("isChecked", shouldSend.isChecked() );
    	editSettings.putString("name", name_text.getText().toString() );
    	editSettings.putString("number", phone_text.getText().toString() );

    	editSettings.apply();
    }
    
    /*
     * Overriden to load saved values for name and phone number inputs.
     * 
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
    	super.onResume();
    	
    	/* read in the saved values */
    	SharedPreferences settings = getSharedPreferences(SAVED_INPUTS, 0);
    	String name = settings.getString("name", "");
    	String phone_number = settings.getString("number","");
    	boolean send = settings.getBoolean("isChecked", false);
    	
    	/* set the various fields */
    	EditText phone_text = (EditText) this.findViewById(
    			R.id.phone_number_input);
    	phone_text.setText(phone_number);
    	
    	EditText name_text = (EditText) AddFromDialpad.this.
    			findViewById(R.id.name_input);
    	name_text.setText(name);
    	
    	ToggleButton shouldSend = (ToggleButton) this.
    			findViewById(R.id.should_text_toggle);
    	shouldSend.setChecked(send && !getName().equals(NO_NAME) );
    }
    
    /*
     * Overriden to clear values when the activity is destroyed so names
     * and numbers to not persist between uses.
     * 
     * (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
    	/* get the preference file to edit */
    	SharedPreferences settings = getSharedPreferences(SAVED_INPUTS, 0);

    	/* create editor and clear values */
    	Editor editSettings = settings.edit();
    	
    	editSettings.clear();

    	editSettings.apply();
    	
     	super.onDestroy();
    }

}
