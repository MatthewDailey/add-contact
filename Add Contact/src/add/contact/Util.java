package add.contact;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.widget.Toast;

/**
 * Util
 * @author Matt
 *
 * This class provides utility methods used throughout hte application.
 * 	i) isInteger - to check if a string is an int for phone number validation.
 *  ii) toast_msg - to pop up toast messages.
 *  iii) addContact - add contacts to the phone book.
 */
public class Util 
{

	/* check if a sting is an int. used to validate phone numbers 
     * taken from:
     * http://stackoverflow.com/questions/237159/whats-the-best-way-to-
     * check-to-see-if-a-string-represents-an-integer-in-java*/
    public static boolean isInteger(String str) {
        if (str == null) {
                return false;
        }
        int length = str.length();
        if (length == 0) {
                return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
                if (length == 1) {
                        return false;
                }
                i = 1;
        }
        for (; i < length; i++) {
                char c = str.charAt(i);
                if (c <= '/' || c >= ':') {
                        return false;
                }
        }
        return true;
    }
	
    /**
     * Pop up a toast message from an activity.
     * @param a - the calling activity.
     * @param txt - the text of the toast message.
     */
    public static void toast_msg(Activity a, String txt)
    {
	    Context ctx = a.getApplicationContext();
	    int duration = Toast.LENGTH_SHORT;
	    Toast toast = Toast.makeText(ctx, txt, duration);
	    toast.show();
    }
    
    /**
     * Method to guess at the users name so that they may not have to
     * manually enter the name.
     * 
     * @param cr content resolver to search the phone
     * @return a best effort attempt at the phone's user's name
     */
    @TargetApi(14)
	public static String getUserName(ContentResolver cr)
    {
    	/* return string */
    	String result = "";
    	
    	/* break based on os version since user name retreival
    	 * was not supported until verion 14    	 */
    	if (android.os.Build.VERSION.SDK_INT >= 14) {
        	System.out.println("Entered version specific name look up");
        	/* get content uri */
    		Uri uri = ContactsContract.Profile.CONTENT_URI;
        	/* project only display name */
        	String[] projection = new String []{
        			ContactsContract.Contacts.DISPLAY_NAME
        	};
        	/* get the cursor for query */
        	Cursor c = cr.query(uri, projection, null, null, null);
        	
        	while( c.moveToNext() ){
        	for( String s : c.getColumnNames())
        	{
        		System.out.println(s + " : " + c.getString(c.getColumnIndex(s)));
        	}}
        	
        	/* retreive the name */
        	try
        	{
        		if( c.moveToFirst() )
        		{
        			result = c.getString(
        					c.getColumnIndex(
        					ContactsContract.Contacts.DISPLAY_NAME));
        		}
        	}
        	finally
        	{
        		c.close();
        	}
    	} 
    	
    	return result;
    }
    
    
    /**
     * Determine which account has visible contacts and get that one.
     * This is done by looking at contacts in the visible group and 
     * checking their account 
     * 
     * @return an array of 3 values: group id, account name and account type
     * 		of a visible group in the users contact list which we should add
     * 		new contacts to. 
     */
    private static String[] getVisibleAccount(ContentResolver cr)
    {
        /* return variables and visibilty of contact check */
        String acct_name = null;
        String acct_type = null;
        String group_id = null;
        String visible = "0";
        
        /* set fields to get */
        String[] fields = new String[] {
                GroupMembership.GROUP_SOURCE_ID,
                Contacts.IN_VISIBLE_GROUP, 
                ContactsContract.Settings.ACCOUNT_NAME, 
                ContactsContract.Settings.ACCOUNT_TYPE};

        /* pull rows of db, make sure mimetype is a groupmembership 
         * to get group id*/
        Cursor c = cr.query(Data.CONTENT_URI, fields,
                 Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE + "'",
                 null, null);
        
        /* Check rows to find an visible acct/group combination */
        while(c.moveToNext())
        {
        	acct_name = c.getString(c.getColumnIndex(
        			ContactsContract.Settings.ACCOUNT_NAME));
        	acct_type = c.getString(c.getColumnIndex(
        			ContactsContract.Settings.ACCOUNT_TYPE));
        	group_id = c.getString(c.getColumnIndex(
        			GroupMembership.GROUP_SOURCE_ID));
        	visible = c.getString(c.getColumnIndex(
        			Contacts.IN_VISIBLE_GROUP));
        	
        	/* if we got a valid visible group, break otherwise keep looking */
        	if(acct_name != null && acct_type != null && group_id != null 
        			&& Integer.parseInt(visible) > 0)
        	{
        		break;
        	}
        	else
        	{
        		acct_name = null;
        		acct_type = null;
        		group_id = null;
        		visible = "0";
        	}
        }

        /* set return values */
        String[] out = new String[3];
        out[0] = group_id;
        out[1] = acct_name;
        out[2] = acct_type;
        return out;
    } 

    /** 
     * Method which does the dirty work of inserting a new contact into the
     * users phone database.
     * 
     * @param a - calling activity, to get content resolver.
     * @param phone - phone number of new contact as a string.
     * @param name - name of the new contact.
     */
    public static void addContact(Activity a, String phone, String name)
    {
    	/* correct caps */
    	name = capitalizeName(name);
    	
    	/* get the account and group info */
    	String[] acct_info = getVisibleAccount(a.getContentResolver());
    	String group_id = acct_info[0];
    	String acct_name = acct_info[1]; 
    	String acct_type = acct_info[2];
    	
    	/* check to make sure such a group exists, if not we are S.O.L. 
    	 * so alert the user.*/
    	if(group_id == null || acct_name == null || acct_type == null)
    	{
    		toast_msg(a, "Unable to find valid contact group. " +
    				"This function may not work for you.");
    	}
    	else 
    	{
    		/* create a batch operation to perform on the database */
	        ArrayList<ContentProviderOperation> ops = 
	        		new ArrayList<ContentProviderOperation>();
	        /* create contact with target account info */
	        ops.add(ContentProviderOperation.newInsert(
	        		ContactsContract.RawContacts.CONTENT_URI)
	                .withValue(ContactsContract.RawContacts.
	                		ACCOUNT_TYPE, acct_type)
	                .withValue(ContactsContract.RawContacts.
	                		ACCOUNT_NAME, acct_name)
	                .build());
	        /* add the contact name */
	        ops.add(ContentProviderOperation.newInsert(ContactsContract.
	        		Data.CONTENT_URI)
	                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
	                .withValue(ContactsContract.Data.MIMETYPE,
	                        ContactsContract.CommonDataKinds.StructuredName.
	                        CONTENT_ITEM_TYPE)
	                .withValue(ContactsContract.CommonDataKinds.StructuredName.
	                		DISPLAY_NAME, name)
	                .build());
	        /* add the contact number */
	        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.
	        		CONTENT_URI)
	                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
	                .withValue(ContactsContract.Data.MIMETYPE,
	                	ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
	                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, 
	                		phone)
	                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, 
	                		ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
	                .build());
	        /* add teh contact group */
	        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.
	        		CONTENT_URI)
	                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
	                .withValue(GroupMembership.GROUP_SOURCE_ID, group_id)
	                .withValue(GroupMembership.MIMETYPE, 
	                		GroupMembership.CONTENT_ITEM_TYPE)
	                .build());
	
	        /* apply the the set of operations defined above */
	        try 
	        {
	        	a.getContentResolver().applyBatch(
	        			ContactsContract.AUTHORITY, ops);
	        	toast_msg(a, "Added contact: "+name);
	        } 
	        catch (Exception e) 
	        {
	            // Display warning
	        	toast_msg(a, "Failed to create new contact.");
	        }
    	}
    }
    
    /**
     * capitalize the first letter in each word in a persons name.
     * 
     * @param name the name the be capitalized
     * @return a correctly capitalized version of the the name. 
     */
    private static String capitalizeName( String name ) 
    {
    	String[] words = name.split("\\s+");
    	
    	StringBuilder sb = new StringBuilder();
    	
    	for( String word : words)
    	{
    		sb.append(Character.toUpperCase(word.charAt(0)))
    	        .append( word.substring(1).toLowerCase() );

    		sb.append(" ");
    	}
    	
    	return sb.toString();
    }

}