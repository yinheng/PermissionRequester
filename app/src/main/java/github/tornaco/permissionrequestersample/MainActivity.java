
package github.tornaco.permissionrequestersample;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import github.tornaco.permission.requester.RequiresPermission;
import github.tornaco.permission.requester.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    MainActivityPermissionRequester.doSomeThing2Checked(getApplicationContext(), MainActivity.this);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    MainActivityPermissionRequester
                            .doSomeThingChecked("Hello2", mTextMessage, 2018, 12.3f, 2222d,
                                    getApplicationContext(),
                                    MainActivity.this);
                    return true;
            }
            return false;


        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionRequester.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @RequiresPermission.Before("onDoSomethingBefore")
    @RequiresPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CONTACTS})
    @RequiresPermission.OnDenied("onDoSomethingDenied")
    public void doSomeThing(String param1, View param2, int param3, float param4, double p5, Context context) {
        Toast.makeText(context, param1 + param2 + param3 + param4 + p5, Toast.LENGTH_LONG).show();
    }

    public void onDoSomethingDenied() {
        Toast.makeText(this, "onDoSomethingDenied", Toast.LENGTH_LONG).show();
    }

    public void onDoSomethingBefore() {
        Toast.makeText(this, "onDoSomethingBeforeXXX", Toast.LENGTH_LONG).show();
    }

    // Case2.
    @RequiresPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION})
    public void doSomeThing2(Context context) {
        Toast.makeText(context, "doSomeThing2", Toast.LENGTH_LONG).show();
    }
}
