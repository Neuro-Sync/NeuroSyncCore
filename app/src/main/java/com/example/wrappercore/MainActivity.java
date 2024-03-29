package com.example.wrappercore;

import ai.Model;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.wrappercore.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

  private static final String ACTION_USB_PERMISSION = "com.example.wrappercore.USB_PERMISSION";
  private static final String TAG = "Test_Android_USB";
  private Boolean switchState = false;
  private UsbSerialPort port;
  private AppBarConfiguration appBarConfiguration;
  private float[] result = new float[]{4};

  private void sendSdkBtPacket() throws IOException {

    int TIMEOUT = 1000;
    byte[] request = switchState ? "1".getBytes() : "0".getBytes();
    byte[] response = new byte[1024];

    port.write(request, TIMEOUT);
    int len = port.read(response, TIMEOUT);
    Log.i(TAG, "Received data: " + new String(response, 0, len));
  }

  private void modelTest() {
    Model modelHandler = new Model("https://learny-v1.onrender.com/api/v1/downloadModel");

    float[][] inputData = new float[][]{{1, 2}};

    result = modelHandler.runInference(inputData);
//    result = new float[]{3};

  }

  private void asyncSendBtPackets(UsbDevice mDevice, UsbManager manager) {
    Log.i(TAG, "Device: " + mDevice.getVendorId());
    UsbInterface usbInterface = mDevice.getInterface(0);
    UsbEndpoint endpoint = usbInterface.getEndpoint(0);
    UsbDeviceConnection connection = manager.openDevice(mDevice);
    byte[] DATA = "1".getBytes();
    boolean forceClaim = true;
//
    connection.claimInterface(mDevice.getInterface(0), true);
//    connection.bulkTransfer(endpoint, DATA, DATA.length, 0);

// Initialize UsbRequest
    UsbRequest usbRequest = new UsbRequest();
    usbRequest.initialize(connection, endpoint);

// Prepare the data to send asynchronously

// Wrap the data in a ByteBuffer
    ByteBuffer buffer = ByteBuffer.allocate(DATA.length);
    buffer.put(DATA);
    buffer.flip(); // Prepare the buffer for reading

// Queue the asynchronous request
    new Thread(() -> {
      if (usbRequest.queue(buffer, buffer.limit())) {
        // Wait for the result
        UsbRequest response = connection.requestWait();
        if (response == usbRequest) {
          // Request completed successfully
          // You can handle the response if needed
          Log.i("Test_Android_USB", "Request completed successfully");
          Log.i("Test_Android_USB", "Response: " + buffer);
        } else {
          Log.e("Test_Android_USB", "Error sending request");
        }
      } else {
        Log.e("Test_Android_USB", "Error queueing request");
      }

// Release the USB interface when done
      connection.releaseInterface(usbInterface);
    }).start();

  }

  private void sendBtPackets(UsbDevice mDevice, UsbManager manager) {
    Log.i(TAG, "Device: " + mDevice.getVendorId());
    UsbInterface usbInterface = mDevice.getInterface(0);
    UsbDeviceConnection connection = manager.openDevice(mDevice);
    byte[] DATA = "1".getBytes();
    boolean forceClaim = true;
    int TIMEOUT = 1000;

    UsbEndpoint endpointOut = usbInterface.getEndpoint(0); // Output endpoint
    UsbEndpoint endpointIn = usbInterface.getEndpoint(1); // Input endpoint

    if (connection != null) {
      connection.claimInterface(usbInterface, true);

      // Send data to the device
      int bytesSent = connection.bulkTransfer(endpointOut, DATA, DATA.length, TIMEOUT);

      if (bytesSent >= 0) {
        // Receive response from the device
        byte[] buffer = new byte[1];
        int bytesRead = connection.bulkTransfer(endpointIn, buffer, buffer.length, TIMEOUT);

        if (bytesRead >= 0) {
          // Process the received data (buffer)
          Log.i(TAG, "Received data: " + new String(buffer));
        } else {
          // Error: Failed to receive data from the device
          Log.e(TAG, "Failed to receive data from the device");
        }
      } else {
        // Error: Failed to send data to the device
        Log.e(TAG, "Failed to send data to the device");
      }

      // Release the USB interface when done
      connection.releaseInterface(usbInterface);
      connection.close();
    } else {
      // Error: Failed to open a connection to the USB device
      Log.e(TAG, "Failed to open a connection to the USB device");
    }
  }

  private void getBtAccess(UsbDevice device, UsbManager manager) {
    // Create a PendingIntent for USB permission request
    PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

    // Create a BroadcastReceiver to handle USB permission
    BroadcastReceiver usbReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
          synchronized (this) {
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
              if (usbDevice != null && usbDevice.equals(device)) {
                // Permission granted, proceed with opening the USB device
                UsbDeviceConnection connection = manager.openDevice(device);
                if (connection != null) {
                  // Device opened successfully, perform further operations
                  Log.i(TAG, "Device opened successfully");
                  // Here you can perform USB communication or other tasks
//                  asyncSendBtPackets(device, manager);
//                  sendBtPackets(device, manager);
                }
              }
            } else {
              // Permission denied for the USB device
              Log.e(TAG, "Permission denied for USB device: " + device.getDeviceName());
              // Handle the permission denial if needed
            }
          }
        }
      }
    };

    // Register the BroadcastReceiver to handle USB permission
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY); // Set priority to high
    registerReceiver(usbReceiver, filter); // Add permission flag
//    if (VERSION.SDK_INT >= VERSION_CODES.O) {
//    }
    // Request USB permission for the device
    manager.requestPermission(device, permissionIntent);
  }

  public void initBtManager() {

    UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
    UsbDevice mDevice = null;
    for (UsbDevice device : deviceList.values()) {
      if (device.getDeviceName().equals("/dev/bus/usb/001/003")) {
        mDevice = device;
      }
      Log.i(TAG,
          "Device Name: " + device.getDeviceName() + " Device ID: " + device.getDeviceId() + " Vendor: "
              + device.getVendorId() + " Product: " + device.getProductId());

    }
    if (mDevice != null) {
      Log.i(TAG, "sdk dev: " + VERSION.SDK_INT);
      getBtAccess(mDevice, manager);
//      if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
//      }
    }
  }

  public void initBtManagerSerialSdk() throws IOException {
    // Find all available drivers from attached devices.
    UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
    List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
    if (availableDrivers.isEmpty()) {
      return;
    }

    Map<String, UsbDevice> diverList = manager.getDeviceList();
    UsbDevice mDevice;
    for (UsbDevice device : diverList.values()) {
      mDevice = device;
      getBtAccess(mDevice, manager);
      Log.i(TAG,
          "Device Name: " + device.getDeviceName() + " Device ID: " + device.getDeviceId() + " Vendor: "
              + device.getVendorId() + " Product: " + device.getProductId());
    }

    // Open a connection to the first available driver.
    UsbSerialDriver driver = availableDrivers.get(0);
    UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
    if (connection == null) {
      // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
      return;
    }

    Log.i(TAG, "Device: " + driver.getDevice().getVendorId() + " ports: " + driver.getPorts().size());
    port = driver.getPorts().get(0);
    port.open(connection);
    port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

//    modelTest();
    try {
      initBtManagerSerialSdk();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    com.example.wrappercore.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    setSupportActionBar(binding.toolbar);

    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
    appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    binding.fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
//        initBtManager();
        try {
          sendSdkBtPacket();
          switchState = !switchState;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        Snackbar.make(view, "Replace with your own action " + result[0], Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onSupportNavigateUp() {
    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
    return NavigationUI.navigateUp(navController, appBarConfiguration)
        || super.onSupportNavigateUp();
  }
}