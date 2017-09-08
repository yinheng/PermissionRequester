package github.tornaco.permissionrequestersample.fragment;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import github.tornaco.permission.requester.RequiresPermission;
import github.tornaco.permission.requester.RuntimePermissions;

/**
 * Created by guohao4 on 2017/9/8.
 * Email: Tornaco@163.com
 */
@RuntimePermissions
public class SomeFragment extends Fragment {

    @RequiresPermission.Before("onDoSomethingBefore")
    @RequiresPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CONTACTS})
    @RequiresPermission.OnDenied("onDoSomethingDenied")
    public void doSomeThing(String param1, View param2, int param3, float param4, double p5, Context context) {
        Toast.makeText(context, param1 + param2 + param3 + param4 + p5, Toast.LENGTH_LONG).show();
    }

    public void onDoSomethingDenied() {
        Toast.makeText(getActivity(), "onDoSomethingDenied", Toast.LENGTH_LONG).show();
    }

    public void onDoSomethingBefore() {
        Toast.makeText(getActivity(), "onDoSomethingBeforeXXX", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
