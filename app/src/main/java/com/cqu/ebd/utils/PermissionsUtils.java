package com.cqu.ebd.utils;

import android.app.Activity;
import android.content.pm.PackageManager;


import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PermissionsUtils {
    public interface PermissionsListener {
        /**
         *同意
         */
        void onGranted();

        /**
         * 拒绝
         */
        void onDenied(List<String> permissions);

        /**
         * 不在提醒
         */
        void onNoAsk(List<String> permissions);
    }

    private Activity mActivity;
    private PermissionsListener mListener;
    private int requestCode;
    public PermissionsUtils(Activity activity, PermissionsListener listener) {
        this.mActivity = activity;
        this.mListener = listener;
    }

    public synchronized boolean checkRequestPermissionRationale(String[] permissions) {
        boolean rationale = false;
        for (String permission : permissions) {
            rationale = rationale || ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission);
        }
        return rationale;
    }

    public synchronized void requestPermissions(String[] permissions, int requestCode) {
        ArrayList<String> permissionsList = new ArrayList<>();
        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this.mActivity, perm)) {
                permissionsList.add(perm);
            }
            //Log.i("Per","show Rationale "+ActivityCompat.shouldShowRequestPermissionRationale(mActivity, perm));
        }
        if (!permissionsList.isEmpty()) {
            // 进入到这里代表没有权限.
            this.requestCode = requestCode;
            String[] strings = new String[permissionsList.size()];
            ActivityCompat.requestPermissions(this.mActivity, permissionsList.toArray(strings), requestCode);
        } else {
            if (mListener != null) {
                mListener.onGranted();
            }
        }
    }

    public synchronized void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != this.requestCode) {
            return;
        }
        LinkedList<String> grantedPermissions = new LinkedList<>();
        LinkedList<String> deniedPermissions = new LinkedList<>();
        LinkedList<String> noAskPermissions = new LinkedList<>();
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permission);
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this.mActivity, permission)) {
                noAskPermissions.add(permission);
            } else {
                deniedPermissions.add(permission);
            }
        }
        //全部允许才回调 onGranted 否则只要有一个拒绝都回调 onDenied
        if (!grantedPermissions.isEmpty() && deniedPermissions.isEmpty() && noAskPermissions.isEmpty()) {
            if (mListener != null) {
                mListener.onGranted();
            }
        } else {
            if (mListener != null) {
                if (!deniedPermissions.isEmpty()) {
                    mListener.onDenied(deniedPermissions);
                }
                if (!noAskPermissions.isEmpty()) {
                    mListener.onNoAsk(noAskPermissions);
                }
            }
        }
    }
}
