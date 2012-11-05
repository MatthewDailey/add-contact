package add.contact;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * SetName
 * @author Matt
 *
 * This activity allows the user to set the name which will be sent to
 * new contacts added through the dialpad.
 * 
 * The layout is a simple text input with a button to submit. A pop up 
 * will alert the user that the name has been successfully set and 
 * remind them of the set value.
 * 
 * This is called by the toggle button if there is no set name and the 
 * set name button on the home page.
 */
public class SetName extends Activity
    {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	/* establish the UI */
            super.onCreate(savedInstanceState);
            setContentView(R.layout.set_name);
            
            /* attempt to find and set the users name */
            EditText name_input = (EditText) this.findViewById(R.id.set_name_input);
            String name_attempt = Util.getUserName(getContentResolver());
            if( !name_attempt.equals("") )
            {
            	TextView found_name_msg = (TextView) this.findViewById(R.id.found_name_msg);
            	found_name_msg.setText(getString(R.string.found_name));
            	name_input.setText(name_attempt);
            }
            

            /* set the onclick handler */
            Button set_name = (Button) this.findViewById(R.id.set_name_button);
            set_name.setOnClickListener(new OnClickListener()
            {
				@Override
				public void onClick(View arg0) {
					String new_name = ((EditText) SetName.this.findViewById(
							R.id.set_name_input)).getText().toString();
					
					/* if they have selected to have no name, reset to const */
					if(new_name.equals(""))
					{
						new_name = AddFromDialpad.NO_NAME;
						Util.toast_msg(SetName.this, "Enter a name.");
						return;
					}
					else
					{
						/* access the preference, edit and commit new pref */
				    	SharedPreferences settings = getSharedPreferences(
				    			AddFromDialpad.PREFS_NAME, 0);
				    	Editor pref_editor = settings.edit();
				    	pref_editor.putString("name", new_name);
				    	pref_editor.apply();
				    	
				    	/* tell the user the name that was set */
				    	Util.toast_msg(SetName.this, "Name set to "+ 
				    	new_name +".");
				    	/* everything done, close the app */
				    	finish();
					}
				}
            });
            
        }    	
    }