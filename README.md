# PermissionRequester

## Why I create this project?
> Learing gradle plugin development, and I love the feeling of code auto generation.

## Status and version

[ ![Download](https://api.bintray.com/packages/potestadetornaco/android/permission-requester-compiler/images/download.svg?version=1.3) ](https://bintray.com/potestadetornaco/android/permission-requester-compiler/1.3/link)


## How to use in your projects?
> Please setup your gradle project as below:

1. Add dependency in gradle build script
```gradle
dependencies {
...

provided 'github.tornaco:permission-requester-annotation:$version'
annotationProcessor 'github.tornaco:permission-requester-compiler:$version'
}
```
1.1 Change ```provide``` to ```compile``` since 1.4

2. Annotate your Activity or Fragment class
```java
@RuntimePermissions
public class MainActivity extends AppCompatActivity {}
```

3. Annotate your method that need to check permissions
```java
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
```


4. Add below line in your activity or fragment

Activity

```java
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionRequester.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
```

Fragment

```java
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Call you helper class here.
        SomeFragmentV4PermissionRequester.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
```

5. Now you can call your original method with subfix instead.
```java
... // Some code.
MainActivityPermissionRequester
                            .doSomeThingChecked("Hello2", mTextMessage, 2018, 12.3f, 2222d,
                                    getApplicationContext(),
                                    MainActivity.this);
```




### Enjoy yourself
