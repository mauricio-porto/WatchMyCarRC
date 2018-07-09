package com.braintech.watchmycar;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.braintech.watchmycar.base.ApplicationPreferences;
import com.braintech.watchmycar.base.BluetoothChatService;

import static com.braintech.watchmycar.Utils.getTimerText;

public class WatchMyCarRC extends AppCompatActivity {

    private static final String TAG = WatchMyCarRC.class.getSimpleName();
    private ApplicationPreferences preferences = null;

	// Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private boolean isConfigured = false;
    private CountDownTimer cTimer;
    private boolean mOnTimerTicking = false;
    private TextView txtAlarmStatus;
    private TextView txtTimer;
    private TextView txtTimerTitle;

    private boolean hasPermissions = false;
    private boolean receiverSvcConnected = false;
    private boolean isBound = false;
    private boolean armed = false;
    private Messenger messageReceiver = null;

    private static final int PERMISSION_RECORD_AUDIO = 0;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_START_SERVICE = 3;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 4;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 5;


    private TextView mTextMessage;

	/**
	 * Name of the connected device
	 */
	private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddress = null;

	/**
	 * Member object for the chat services
	 */
	private BluetoothChatService mChatService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new ApplicationPreferences(getApplicationContext());

        setContentView(R.layout.main_screen);

		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();   // TODO: localize!!!
            finish();
            return;
        }

        mTextMessage = (TextView) findViewById(R.id.message);
        txtAlarmStatus = (TextView) findViewById(R.id.alarm_status_text);
        txtTimer = (TextView) findViewById(R.id.timer_text);
        txtTimerTitle = (TextView) findViewById(R.id.timer_text_title);
        setDelayTimer();

        findViewById(R.id.btnStartStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (armed) {
                    doCancel();
                } else {
                    if (mOnTimerTicking) {
                        doCancel();
                    } else {
                        initTimer();
                    }
                }
            }
        });

    }

	@Override
	public void onStart() {
		super.onStart();
		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else if (mChatService == null) {
            // Initialize the BluetoothChatService to perform bluetooth connections
            mChatService = new BluetoothChatService(this, mHandler);

            if (!connectKnownDevice()) {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
		}
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        outState.putBoolean("armed", armed);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
        armed = savedInstanceState.getBoolean("armed");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
		if (mChatService != null) {
			mChatService.stop();
		}
    }

    private void setDelayTimer() {
        //int timeM = preferences.getTimerDelay() * 1000;
        int timeM = 30000;
        txtTimer.setText(getTimerText(timeM));
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        setDelayTimer();

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
    }

    private void initTimer() {
        //cTimer = new CountDownTimer((preferences.getTimerDelay()) * 1000, 1000) {
        cTimer = new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {
                mOnTimerTicking = true;
                txtTimer.setText(getTimerText(millisUntilFinished));
            }

            public void onFinish() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mOnTimerTicking = false;
                txtAlarmStatus.setText(R.string.alarm_status_on);
                txtTimer.setVisibility(View.INVISIBLE);
                txtTimerTitle.setVisibility(View.INVISIBLE);
                armed = true;
                //sendTextToService(Keeper.ARM, "armed");
            }
        };
        cTimer.start();
    }

    private void doCancel() {

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
            mOnTimerTicking = false;
        }

        txtAlarmStatus.setText(R.string.alarm_status_off);
        int timeM = 30000;  //preferences.getTimerDelay() * 1000;
        txtTimer.setText(getTimerText(timeM));
        txtTimer.setVisibility(View.VISIBLE);
        txtTimerTitle.setVisibility(View.VISIBLE);
        armed = false;
        //sendTextToService(Keeper.DISARM, "disarmed");
    }

	/**
	 * The Handler that gets information back from the BluetoothChatService
	 */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
				    mTextMessage.setText(getString(R.string.title_connected_to, mConnectedDeviceName));
					preferences.setBTDeviceAddress(mConnectedDeviceName);
					preferences.setBTDeviceAddress(mConnectedDeviceAddress);
					break;
				case BluetoothChatService.STATE_CONNECTING:
					mTextMessage.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					mTextMessage.setText(R.string.title_not_connected);
					break;
				}
				break;
			case Constants.MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				//mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case Constants.MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				//mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
				break;
			case Constants.MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                Toast.makeText(WatchMyCarRC.this, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;
			case Constants.MESSAGE_TOAST:
                Toast.makeText(WatchMyCarRC.this, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
                // Initialize the BluetoothChatService to perform bluetooth connections
                mChatService = new BluetoothChatService(this, mHandler);
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}


	/**
	 * Establish connection with other device
	 *
	 * @param data
	 *            An {@link Intent} with
	 *            {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
	 * @param secure
	 *            Socket Security type - Secure (true) , Insecure (false)
	 */
	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		mConnectedDeviceAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mConnectedDeviceAddress);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}

    private boolean connectKnownDevice() {
	    boolean result = false;
	    String deviceAddress = preferences.getBTDeviceAddress();
	    if (deviceAddress != null) {
	        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
	        // Attempt to connect to the device
		    mChatService.connect(device, true);
		    result = true;
        }
	    return result;
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }
}
