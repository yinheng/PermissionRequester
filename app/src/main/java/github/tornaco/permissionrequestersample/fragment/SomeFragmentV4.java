package github.tornaco.permissionrequestersample.fragment;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Toast;

import github.tornaco.permission.requester.RequiresPermission;
import github.tornaco.permission.requester.RuntimePermissions;

/**
 * Created by guohao4 on 2017/9/8.
 * Email: Tornaco@163.com
 */
@RuntimePermissions
public class SomeFragmentV4 extends Fragment {

    @RequiresPermission.Before("onDoSomethingBefore")
    @RequiresPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CONTACTS})
    @RequiresPermission.OnDenied("onDoSomethingDenied")
    public void doSomeThing(String param1, View param2, int param3, float param4, double p5, Context context) {
        Toast.makeText(context, param1 + param2 + param3 + param4 + p5, Toast.LENGTH_LONG).show();
    }

    public void onDoSomethingDenied() {
        Toast.makeText(getContext(), "onDoSomethingDenied@SomeFragmentV4", Toast.LENGTH_LONG).show();
    }

    public void onDoSomethingBefore() {
        Toast.makeText(getContext(), "onDoSomethingBeforeXXX@SomeFragmentV4", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SomeFragmentV4PermissionRequester.doSomeThingChecked("SomeFragmentV4", null, 1, 2, 3, getActivity(), this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Call you helper class here.
        SomeFragmentV4PermissionRequester.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
