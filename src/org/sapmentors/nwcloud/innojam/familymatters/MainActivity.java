package org.sapmentors.nwcloud.innojam.familymatters;


import java.util.ArrayList;
import java.util.List;

import org.sapmentors.nwcloud.innojam.familymatters.R;
import org.sapmentors.nwcloud.innojam.familymatters.backend.MobileDevice;
import org.sapmentors.nwcloud.innojam.familymatters.backend.NWCloudBackend;
import org.sapmentors.nwcloud.innojam.familymatters.backend.PushMessageExternal;
import org.sapmentors.nwcloud.innojam.familymatters.model.PushMessageResponse;
import org.sapmentors.nwcloud.innojam.familymatters.util.AndroidUtils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Main activity for the android app
 * 
 * On startup it registers the device for receiving GCM push messages
 * It also fetches all the devices from the NW cloud backend and provides
 * and interface for sending the users a message.
 * 
 * The message sent is sent to NW Cloud through a REST interface, which again
 * uses the GCM server library to send it via Google to the android device.
 * 
 * See NWCloudBackend class for how to swap between local and nwcloud backend
 * 
 * @author dagfinn.parnas
 *
 */
public class MainActivity extends Activity {
	protected static final String LOG_PREFIX = "NWCLOUD-GCM";

	Spinner spinnerEmailTo; 
	Button buttonSend;
	EditText editMessage; 
	ArrayAdapter<String> spinnerAdapter;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Register this device to Google Cloud Messaging for our API
        //Will also subsequently trigger storage of GCM registration key
        //for this devices in the NWCloud backend
        GCMIntentService.register(this);
        
        setContentView(R.layout.activity_main);
        
        //components in the view
        spinnerEmailTo = (Spinner)findViewById(R.id.spinner_emailto);
        buttonSend = (Button)findViewById(R.id.button_send);
        editMessage=(EditText)findViewById(R.id.edit_message);

        
    	spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
    	spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmailTo.setAdapter(spinnerAdapter);
        
        actionRefreshRecipientEmails();
    }
    
    public void actionRefreshRecipientEmails(){
    	//disable the send button untill the recipient devices have been fetched
        buttonSend.setEnabled(false);
        
        spinnerAdapter.clear();
        spinnerAdapter.add("Please wait... fetching recipients");
        spinnerAdapter.notifyDataSetChanged();
        
        NWCloudGetDevicesTask taskGetDevices = new NWCloudGetDevicesTask();
        taskGetDevices.execute();
    }
    
    public void actionRemoveMyDevice(){
        NWCloudRemoveMyDeviceTask taskGetDevices = new NWCloudRemoveMyDeviceTask(this);
        taskGetDevices.execute();
    }
    
    private void postDeviceRemove(boolean successful){
    	if(successful){
    		informUserAboutMessage("Will refetch recipients from sapnwcloud backend");
    		actionRefreshRecipientEmails();
    	}
    }
    
    public void actionCreateDummyNotification(View view){
    	
    	informUserAboutMessage("Dummy notification");
    	int notificationCounter=0;

		CharSequence contentTitle = "Request for babysitting";
		CharSequence tickerText = "Request for babysitting"; // message title
		String contentText = "Are you able to babysit for Phil's kid today?";
		
		Intent notificationIntent = new Intent(this, NotificationResponseActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		
		 Bitmap notificationPhoto = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.kid);
		
		Notification.Builder builder;
		builder = new Notification.Builder(getApplicationContext());
	    builder.setContentTitle(contentTitle);
	    builder.setContentText(contentText);
	    builder.setTicker(tickerText);
	    
	    builder.setSmallIcon(android.R.drawable.ic_menu_help);
	    //builder.setLargeIcon(android.R.drawable.ic_input_get);
	    builder.addAction(android.R.drawable.ic_menu_add, "Yes", contentIntent);
	    builder.addAction(android.R.drawable.ic_menu_delete, "No", contentIntent);
	    //builder.addAction(R.drawable.ic_launcher, "Launcher", notificationIntent);
	    builder.build();

	    Notification notification = new Notification.BigPictureStyle(builder).bigPicture(notificationPhoto).build();
		
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationCounter++, notification);
    	
    	
    }
    
    
    /**
     * Populate the spinner with emails from registered
     * android devices (from NWCloud backend)
     * 
     * @param devices
     */
    private void populateSpinner(MobileDevice[] devices){
    	spinnerAdapter.clear();
    	if(devices!=null){
        	spinnerAdapter.clear();
        	for (int i = 0; i < devices.length; i++) {
				MobileDevice device =devices[i];
				spinnerAdapter.add(device.getEmail());		
			}
        	buttonSend.setEnabled(true);
        }else {
        	spinnerAdapter.add("No devices registered");
        	informUserAboutMessage("No devices registered in backend yet. Try again later");
        	buttonSend.setEnabled(false);
        }
    	spinnerAdapter.notifyDataSetChanged();
    }


    
    public void actionSendMessage(View view){
    	informUserAboutMessage("Attempting to send message");
    	
    	PushMessageExternal pushMessageExternal = new PushMessageExternal();
    	
    	pushMessageExternal.setMessage(editMessage.getText().toString());
    	pushMessageExternal.setEmailFrom(AndroidUtils.getPrimaryAccountEmail(this));
    	
    	ArrayList<String> arEmailTo = new ArrayList<String>(10);
    	String strSelectedEmailTo = (String)spinnerEmailTo.getSelectedItem();
    	arEmailTo.add(strSelectedEmailTo);
    	
    	pushMessageExternal.setEmailTo(arEmailTo.toArray(new String[]{}));	
    	
    	NWCloudSendMessageTask taskSendMessage = new NWCloudSendMessageTask();
    	taskSendMessage.execute(pushMessageExternal);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh_recipient:
			informUserAboutMessage("Will refetch recipients from sapnwcloud backend");
			actionRefreshRecipientEmails();
			return true;
		case R.id.menu_delete_device:
			informUserAboutMessage("Will delete this device's information from sapnwcloud backend");
			actionRemoveMyDevice();
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}

	}
    
    
	public void informUserAboutMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		
	}
	
	public class NWCloudGetDevicesTask extends AsyncTask<Object, Object, MobileDevice[]> {
		
		public NWCloudGetDevicesTask (){
		}
		
		@Override
		protected MobileDevice[] doInBackground(Object... arg0){
			Log.d(LOG_PREFIX, "Before getting android devices from backend");
			MobileDevice[] devices = NWCloudBackend.getRegisteredDevices();
			return devices;
		}

		@Override
		protected void onPostExecute(MobileDevice[] result) {
			super.onPostExecute(result);
			populateSpinner(result);
		}
	}
	
	public class NWCloudSendMessageTask extends AsyncTask<PushMessageExternal, Object, PushMessageResponse> {
		
		public NWCloudSendMessageTask (){
		}
		
		@Override
		protected PushMessageResponse doInBackground(PushMessageExternal... arguments){
			PushMessageExternal pushMessage= arguments[0];
			Log.d(LOG_PREFIX, "About to send message to NWCloud backend. Msg;" + pushMessage.getMessage());
			PushMessageResponse pushmessageResponse = NWCloudBackend.sendMessage(pushMessage);
			return pushmessageResponse;
		}

		@Override
		protected void onPostExecute(PushMessageResponse pushmessageResponse) {
			informUserAboutMessage("Push message sent. Response code is " +pushmessageResponse.getResponseCode() +" ("+pushmessageResponse.getResponseMessage()+")");
		}
	}
	
	public class NWCloudRemoveMyDeviceTask extends AsyncTask<Void, Void, Boolean> {
		Context context; 
		public NWCloudRemoveMyDeviceTask (Context context){
			this.context=context;
		}
		
		@Override
		protected Boolean doInBackground(Void... arg0){
			Log.d(LOG_PREFIX, "Before getting android devices from backend");
			return Boolean.valueOf(NWCloudBackend.removeMyDevice(context));
		}

		@Override
		protected void onPostExecute(Boolean successful ) {
			if(successful.booleanValue()){
				informUserAboutMessage("This device was removed from NWCloud backend.\n(will be automatically re-added if you restart the app");
			}else {
				informUserAboutMessage("Failed to remove this device from NWCloud backend.\n(maybe it don't exist there)");
			}
			postDeviceRemove(successful.booleanValue());
			
		}
	}
}
