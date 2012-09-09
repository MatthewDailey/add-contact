package add.contact;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;


/***
 * AddContactMenu
 * @author Matt Dailey
 *
 * This activity is the app launch menu with options to either:
 * 		i) add contact from a recent text message.
 * 		ii) add contact from a dialpad and send that person a text with
 * 			your name.
 * 		iii) view your contacts list in most recently added order.
 * 		iv) set the name the app send in action (ii)                  
 */
public class AddContactMenu extends Activity 
{
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        /* set up layout */
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_menu);    
        
        /* bind listeners to menu buttons */
        prepareScreen();
    }

    /**
     * prepareButtons
     * 
     * bind onClick listeners to menu buttons. needs to be called on create
     * and on restart. 
     **/
    private void prepareScreen()
    {
    	/* bind activity to add from text button */
        Button b1 = (Button) this.findViewById(R.id.button1);
        b1.setOnClickListener(new OnClickListener()
        {
        	@Override
        	public void onClick(View arg0)
        	{
        		addFromText();
        	}
        });        

        /* bind activity to addfrom dialpad button */
        Button b2 = (Button) this.findViewById(R.id.button2);
        b2.setOnClickListener(new OnClickListener()
        {
        	@Override
        	public void onClick(View arg0){
        		addFromDialpad();
        	}
        });
    	
        /* bind recent contacts button */
        Button b3 = (Button) this.findViewById(R.id.button3);
        b3.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View arg0) 
			{
				viewRecentContacts();
			}
        });    
        
    }
    
    public boolean openOptionsMenu(View v)
    {
    	this.openOptionsMenu();
    	return true;
    }
    
    /**
     * Open options list which allows the user to change the name which
     * will be texted to new contacts as well as an information page.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {	
    	
    	/* create a set name option in the menu */
    	MenuItem setName = menu.add("Set Name");
    	setName.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item) 
			{
				Intent i = new Intent();
				i.setClass(AddContactMenu.this, SetName.class);
				AddContactMenu.this.startActivity(i);
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
				i.setClass(AddContactMenu.this, HowToUse.class);
				AddContactMenu.this.startActivity(i);
				return true;
			}
    	});
    	
    	
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    /**
     * Add a contact from a text message. Start new activity on top of the
     * menu displaying list of most recent text messages.
     */
    private void addFromText()
    {
    	Intent i = new Intent();
    	i.setClass(this, AddFromText.class);
    	startActivity(i);
    }
    
    /**
     * Add a contact from dial pad. Start a new activity on top of the menu.
     * Displays first and last name inputs with a number input (dialpad)
     */
    private void addFromDialpad()
    {
    	Intent i = new Intent();
    	i.setClass(this, AddFromDialpad.class);
    	startActivity(i);
    }
    
    /**
     * Display contacts in most recent order. Click on a contact will open
     * contact page on top of recent list
     */
    private void viewRecentContacts()
    {
        Intent i = new Intent();
        i.setClass(this, ViewRecentContacts.class);
        startActivity(i);
    }
    
    @Override
    public void onRestart()
    {
    	super.onRestart();
    	
    	
        setContentView(R.layout.activity_menu);
        
        prepareScreen();
    }
}
