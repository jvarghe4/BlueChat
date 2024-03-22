package com.chatapp.bluechat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class    MainActivity extends AppCompatActivity {


    @SuppressLint("ResourceAsColor")
    private static final int REQUEST_CODE_BLUETOOTH = 1001;
    private TextView status;
    private Button btnConnect;

    private ListView listView;
    private Dialog dialog;
    private TextInputLayout inputLayout;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;
    private BluetoothAdapter bluetoothAdapter;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private ChatController chatController;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;









    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        findViewsByIds();








        //check device support bluetooth or not
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            finish();
        }


        //set chat adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
        listView.setAdapter(chatAdapter);
    }

    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(@NonNull Message msg) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                // Permission is granted. Start scanning for Bluetooth devices.
                // You can start scanning here.
            } else {
                // Permission is not granted. Request the permission.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermission(android.Manifest.permission.BLUETOOTH_CONNECT, REQUEST_CODE_BLUETOOTH);
                }
            }

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatController.STATE_CONNECTED:
                            setStatus("Connected to: " + connectingDevice.getName());
                            btnConnect.setEnabled(false);
                            setConnectButtonState(chatController.getState());
                            break;
                        case ChatController.STATE_CONNECTING:
                            setStatus("Connecting...");
                            btnConnect.setEnabled(false);
                            break;
                        case ChatController.STATE_LISTEN:

                        case ChatController.STATE_NONE:
                            btnConnect.setEnabled(true);
                            setStatus("Not connected");
                            break;



                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    chatMessages.add("Me: " + writeMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    String readMessage = new String(readBuf, 0, msg.arg1);
                    chatMessages.add(connectingDevice.getName() + ":  " + readMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;

            }
            return false;
        }
    });

    private void showPrinterPickDialog() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                checkPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE)) {
            // Permission is granted. Start scanning for Bluetooth devices.
            // You can start scanning here.
        } else {
            // Permission is not granted. Request the permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermission(Manifest.permission.BLUETOOTH_SCAN, REQUEST_CODE_BLUETOOTH);
                requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_CODE_BLUETOOTH);
                requestPermission(Manifest.permission.BLUETOOTH_ADVERTISE, REQUEST_CODE_BLUETOOTH);
            }
        }

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_bluetooth);
        dialog.setTitle("Bluetooth Devices");

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();




        //Initializing bluetooth adapters
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ArrayAdapter<String> discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);




        //locate listviews and attach the adapters
        ListView listView = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        ListView listView2 = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
        listView.setAdapter(pairedDevicesAdapter);
        listView2.setAdapter(discoveredDevicesAdapter);



        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);



        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter);

        //check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
            // Permission is granted. Start scanning for Bluetooth devices.
            // You can start scanning here.
        } else {
            // Permission is not granted. Request the permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermission(android.Manifest.permission.BLUETOOTH_CONNECT, REQUEST_CODE_BLUETOOTH);
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (!pairedDevices.isEmpty()) {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                // Permission is granted. Start scanning for Bluetooth devices.
                // You can start scanning here.
            } else {
                // Permission is not granted. Request the permission.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermission(android.Manifest.permission.BLUETOOTH_CONNECT, REQUEST_CODE_BLUETOOTH);
                }
            }

            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add(getString(R.string.none_paired));
        }


        //Handling listview item click event
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
                    // Permission is granted. Start scanning for Bluetooth devices.
                    // You can start scanning here.
                } else {
                    // Permission is not granted. Request the permission.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestPermission(android.Manifest.permission.BLUETOOTH_SCAN, REQUEST_CODE_BLUETOOTH);
                    }
                }

                bluetoothAdapter.cancelDiscovery();

                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);


                connectToDevice(address);
                dialog.dismiss();


            }

        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    // Permission is granted. Start scanning for Bluetooth devices.
                    // You can start scanning here.
                } else {
                    // Permission is not granted. Request the permission.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestPermission(Manifest.permission.BLUETOOTH_SCAN, REQUEST_CODE_BLUETOOTH);
                    }
                }

                bluetoothAdapter.cancelDiscovery();

                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);


                connectToDevice(address);
                dialog.dismiss();
            }
        });


        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void setStatus(String s) {

        // Change the button text to "Connecting..."
        setConnectButtonState(chatController.getState());
        status.setText(s);
    }

    private void connectToDevice(String deviceAddress) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
            // Permission is granted. Start scanning for Bluetooth devices.
            // You can start scanning here.
        } else {
            // Permission is not granted. Request the permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermission(android.Manifest.permission.BLUETOOTH_SCAN, REQUEST_CODE_BLUETOOTH);
            }
        }

        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        chatController.connect(device);
    }


    private void findViewsByIds() {
        status = (TextView) findViewById(R.id.status);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        listView = (ListView) findViewById(R.id.list);
        inputLayout = (TextInputLayout) findViewById(R.id.input_layout);
        View btnSend = findViewById(R.id.btn_send);



        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Objects.requireNonNull(inputLayout.getEditText()).getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please input some texts", Toast.LENGTH_SHORT).show();
                } else {
                    //TODO: here
                    sendMessage(inputLayout.getEditText().getText().toString());
                    inputLayout.getEditText().setText("");
                }
            }
        });

        //to disconnect the device
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chatController.getState() == ChatController.STATE_CONNECTED) {
                    disconnect();
                }else {
                    // If not connected, show printer pick dialog to connect
                    showPrinterPickDialog();
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    chatController = new ChatController(this, handler);
                } else {
                    Toast.makeText(this, "Bluetooth still disabled, turn off application!", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void sendMessage(String message) {
        if (chatController.getState() != ChatController.STATE_CONNECTED) {
            Toast.makeText(this, "Connection was lost!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatController.write(send);
        }
    }

    @Override
    public void onStart() {
        super.onStart();



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
            // Permission is granted. Start scanning for Bluetooth devices.
            // You can start scanning here.

        } else {
            // Permission is not granted. Request the permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermission(android.Manifest.permission.BLUETOOTH_CONNECT, REQUEST_CODE_BLUETOOTH);
            }
        }

//        if (!bluetoothAdapter.isEnabled()) {
//            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//                if (result.getResultCode() == Activity.RESULT_OK) {
//                    chatController = new ChatController(this, handler);
//                } else {
//                    Toast.makeText(this, "Bluetooth activation denied", Toast.LENGTH_SHORT).show();
//                }
//            });
//            enableBluetoothLauncher.launch(enableIntent);
//        } else {
//            chatController = new ChatController(this, handler);
//        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            chatController = new ChatController(this, handler);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (chatController != null) {
            if (chatController.getState() == ChatController.STATE_NONE) {
                chatController.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatController != null)
            chatController.stop();

    }

    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                // Permission is granted. Start scanning for Bluetooth devices.
                // You can start scanning here.
            } else {
                // Permission is not granted. Request the permission.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_CODE_BLUETOOTH);
                }
            }


            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                assert device != null;
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add(getString(R.string.none_found));
                }
            }
        }
    };




    @RequiresApi(api = Build.VERSION_CODES.S)
    private boolean checkPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int permissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), permission);
            return permissionResult == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestPermission(String permission, int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_BLUETOOTH) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // The user has granted the permission.
                // Start scanning for Bluetooth devices.
            } else {
                // The user has denied the permission.
                // Display an error message or take necessary actions.
            }
        }
    }



    @SuppressLint("SetTextI18n")
    public void setConnectButtonState(int state) {
        if (state == ChatController.STATE_CONNECTED) {
            btnConnect.setText("Disconnect");
            btnConnect.setEnabled(true);
        } else if (state == ChatController.STATE_NONE) {
            btnConnect.setText("Connect");
            btnConnect.setEnabled(true);
            clearChat();
        } else if (state == ChatController.STATE_CONNECTING) {
            btnConnect.setText("Connecting...");
            btnConnect.setEnabled(false);
        } else if (state == ChatController.STATE_DISCONNECTED) {
            btnConnect.setText("Connect");
            btnConnect.setEnabled(true);
            chatMessages.clear();
            clearChat();
        } else {
            // Default case: If none of the predefined states match
            btnConnect.setText("Connect to a device");
            btnConnect.setEnabled(true);
            clearChat();
        }
    }


    public void disconnect() {
        if (chatController.getState() == ChatController.STATE_CONNECTED) {
            chatController.disconnectDevice();
            setStatus("Disconnected");
            btnConnect.setText("Connect");

        }
    }

    //used to clear chat messages after disconnecting from a device
    private void clearChat() {
        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
    }


}