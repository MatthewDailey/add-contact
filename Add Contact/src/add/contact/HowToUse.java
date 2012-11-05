package add.contact;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

/**
 * HowToUse
 * @author mjdailey
 *
 * Very simple activity to display information about how to use the 
 * application.
 */
public class HowToUse extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_use);
    }

    /* Display no menu on this page */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        return false;
    }

    
}
