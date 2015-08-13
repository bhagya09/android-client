package com.bsb.hike.voip;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.CountDownTimer;

import com.bsb.hike.utils.Logger;


// http://stackoverflow.com/questions/14991158/using-the-android-recognizerintent-with-a-bluetooth-headset
public abstract class BluetoothHeadsetUtils
{
    private  Context mContext;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mConnectedHeadset;

    private AudioManager mAudioManager;

    private boolean mIsCountDownOn;
    private boolean mIsStarting;
    private boolean mIsOnHeadsetSco;
    private boolean mIsStarted;

    private static final String TAG = VoIPConstants.TAG +  " BluetoothHeadsetUtils"; //$NON-NLS-1$

    /**
     * Constructor
     * @param context
     */
    public BluetoothHeadsetUtils(Context context)
    {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Call this to start BluetoothHeadsetUtils functionalities.
     * @return  The return value of startBluetooth() or startBluetooth11()
     */
    public boolean start()
    {
        if (!mIsStarted)
        {
        	mIsStarted = true;
        	mIsStarted = startBluetooth();
        }

        return mIsStarted;
    }

    /**
     * Should call this on onResume or onDestroy.
     * Unregister broadcast receivers and stop Sco audio connection
     * and cancel count down.
     */
    public void stop()
    {
        if (mIsStarted)
        {
        	mIsStarted = false;
        	stopBluetooth();
        }
    }

    /**
     * 
     * @return  true if audio is connected through headset.
     */
    public boolean isOnHeadsetSco()
    {
        return mIsOnHeadsetSco;
    }

    public abstract void onHeadsetDisconnected();

    public abstract void onHeadsetConnected();

    public abstract void onScoAudioDisconnected();

    public abstract void onScoAudioConnected();

    /**
     * Register for bluetooth headset connection states and Sco audio states.
     * Try to connect to bluetooth headset audio by calling startBluetoothSco(). 
     * This is a work around for API < 11 to detect if a headset is connected before 
     * the application starts.
     * 
     * The official documentation for startBluetoothSco() states
     * 
     * "This method can be used by applications wanting to send and received audio to/from 
     *  a bluetooth SCO headset while the phone is not in call."
     *  
     * Does this mean that startBluetoothSco() would fail if the connected bluetooth device
     * is not a headset?
     * 
     * Thus if a call to startBluetoothSco() is successful, i.e mBroadcastReceiver will receive
     * an ACTION_SCO_AUDIO_STATE_CHANGED with intent extra SCO_AUDIO_STATE_CONNECTED, then
     * we assume that a headset is connected.
     * 
     * @return  false if device does not support bluetooth or current platform does not supports
     *                use of SCO for off call.
     */
    @SuppressWarnings("deprecation")
    private boolean startBluetooth()
    {
        Logger.d(TAG, "startBluetooth"); //$NON-NLS-1$

        // Device support bluetooth
        if (mBluetoothAdapter != null)
        {       
            if (mAudioManager.isBluetoothScoAvailableOffCall())
            {
                mContext.registerReceiver(mBroadcastReceiver, 
                                        new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
                mContext.registerReceiver(mBroadcastReceiver, 
                                        new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
                mContext.registerReceiver(mBroadcastReceiver, 
                                        new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED));

                // Need to set audio mode to MODE_IN_CALL for call to startBluetoothSco() to succeed.
                mAudioManager.setMode(AudioManager.MODE_IN_CALL);

                mIsCountDownOn = true;
                // mCountDown repeatedly tries to start bluetooth Sco audio connection.
                mCountDown.start();

                // need for audio sco, see mBroadcastReceiver
                mIsStarting = true;

                return true;
            }
        }

        return false;
    }

    /**
     * API < 11
     * Unregister broadcast receivers and stop Sco audio connection
     * and cancel count down.
     */
    private void stopBluetooth()
    {
        Logger.d(TAG, "stopBluetooth"); //$NON-NLS-1$

        if (mIsCountDownOn)
        {
            mIsCountDownOn = false;
            mCountDown.cancel();
        }

        // Need to stop Sco audio connection here when the app
        // change orientation or close with headset still turns on.
        mContext.unregisterReceiver(mBroadcastReceiver);
        mAudioManager.stopBluetoothSco();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
    }

    /**
     * Broadcast receiver for API < 11
     * Handle headset and Sco audio connection states.
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
    {
        @SuppressWarnings({"deprecation", "synthetic-access"})
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED))
            {       
                mConnectedHeadset = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothClass bluetoothClass = mConnectedHeadset.getBluetoothClass();
                if (bluetoothClass != null)
                {
                    // Check if device is a headset. Besides the 2 below, are there other 
                    // device classes also qualified as headset?
                    int deviceClass = bluetoothClass.getDeviceClass();
                    if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE 
                        || deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET)
                    {
                        // start bluetooth Sco audio connection.
                        // Calling startBluetoothSco() always returns faIL here, 
                        // that why a count down timer is implemented to call
                        // startBluetoothSco() in the onTick.
                        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                        mIsCountDownOn = true;
                        mCountDown.start();

                        // override this if you want to do other thing when the device is connected.
                        onHeadsetConnected();
                    }
                }

                Logger.d(TAG, mConnectedHeadset.getName() + " connected"); //$NON-NLS-1$
            }
            else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
            {
                Logger.d(TAG, "Headset disconnected"); //$NON-NLS-1$

                if (mIsCountDownOn)
                {
                    mIsCountDownOn = false;
                    mCountDown.cancel();
                }

                mAudioManager.setMode(AudioManager.MODE_NORMAL);

                // override this if you want to do other thing when the device is disconnected.
                onHeadsetDisconnected();
            }
            else if (action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED))
            {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, 
                                                AudioManager.SCO_AUDIO_STATE_ERROR);

                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED)
                {
                    mIsOnHeadsetSco = true;

                    if (mIsStarting)
                    {
                        // When the device is connected before the application starts,
                        // ACTION_ACL_CONNECTED will not be received, so call onHeadsetConnected here
                        mIsStarting = false;
                        onHeadsetConnected();
                    }

                    if (mIsCountDownOn)
                    {
                        mIsCountDownOn = false;
                        mCountDown.cancel();
                    }

                    // override this if you want to do other thing when Sco audio is connected.
                    onScoAudioConnected();

                    Logger.d(TAG, "Sco connected"); //$NON-NLS-1$
                }
                else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED)
                {
                    Logger.d(TAG, "Sco disconnected"); //$NON-NLS-1$

                    // Always receive SCO_AUDIO_STATE_DISCONNECTED on call to startBluetooth()
                    // which at that stage we do not want to do anything. Thus the if condition.
                    if (!mIsStarting)
                    {
                        mIsOnHeadsetSco = false;

                        // Need to call stopBluetoothSco(), otherwise startBluetoothSco()
                        // will not be successful.
                        mAudioManager.stopBluetoothSco();

                        // override this if you want to do other thing when Sco audio is disconnected.
                        onScoAudioDisconnected();
                    } 
                }
            }
        }
    };

    /**
     * API < 11
     * Try to connect to audio headset in onTick.
     */
    private CountDownTimer mCountDown = new CountDownTimer(10000, 1000)
    {

        @SuppressWarnings("synthetic-access")
        @Override
        public void onTick(long millisUntilFinished)
        {
            // When this call is successful, this count down timer will be canceled.
            mAudioManager.startBluetoothSco();

//            Logger.d(TAG, "\nonTick start bluetooth Sco"); //$NON-NLS-1$
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public void onFinish()
        {
            // Calls to startBluetoothSco() in onStick are not successful.
            // Should implement something to inform user of this failure
            mIsCountDownOn = false;
            mAudioManager.setMode(AudioManager.MODE_NORMAL);

//            Logger.d(TAG, "\nonFinish fail to connect to headset audio"); //$NON-NLS-1$
        }
    };

}  