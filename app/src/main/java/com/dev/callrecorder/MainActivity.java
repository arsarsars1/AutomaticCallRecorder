package com.dev.callrecorder;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button recordButton;
    public static final String TAG = "Main Activity";
    public static final int REQUEST_PERMISSION = 3001;
    boolean record = true;
    Context context;


    private static final int REQUEST_CODE = 0;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName componentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recordButton = findViewById(R.id.button);
        context = this;

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, DeviceAdminDemo.class);

        if (!devicePolicyManager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Grant System Application Permission.");
            startActivityForResult(intent, REQUEST_CODE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_CODE == requestCode) {
            if(resultCode == RESULT_OK){
                runtimePermission();
            }
            else{
                finish();
                Toast.makeText(context, "System Level Permissions are Required", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //On RecordButton Clicked

    public void record(View view) {
        runtimePermission();
    }


    //Check for Permissions

    private void runtimePermission() {
        Dexter.withContext(context)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.PROCESS_OUTGOING_CALLS,
                        Manifest.permission.RECORD_AUDIO)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if(multiplePermissionsReport.isAnyPermissionPermanentlyDenied()){
                            List<PermissionDeniedResponse> res = multiplePermissionsReport.getDeniedPermissionResponses();
                            for (PermissionDeniedResponse response: res
                                 ) {
                                Toast.makeText(context, ""+response.getPermissionName(), Toast.LENGTH_SHORT).show();
                            }
                            showPermissionDialog();
                        }

                        if(multiplePermissionsReport.areAllPermissionsGranted()){
                            doAction();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                })
                .onSameThread().check();
    }


    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Permissions are required for app to Function!!")
                .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Leave", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    // Start and Stop Activity

    private void doAction() {

        Intent intent = new Intent(this, CallService.class);
        if(!record){
            startService(intent);
            recordButton.setText(R.string.stop);
            record = true;
        }
        else{
            recordButton.setText(R.string.start);
            record = false;
            stopService(intent);
        }
    }
}