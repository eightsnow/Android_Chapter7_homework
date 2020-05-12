package com.bytedance.videoplayer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import com.airbnb.lottie.LottieAnimationView;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MyAdapter.ListItemClickListener{

    private String[] mPermissionsArrays = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE };
    private final static int REQUEST_PERMISSION = 123;

    public static List<VideoInfo> allVideoList = null;// 视频信息集合

    private MyAdapter mAdapter;
    private RecyclerView myRecycleView;
    private Button buttonSearch;
    private LottieAnimationView animationView;
    private boolean isSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        animationView = findViewById(R.id.animation_view);
        buttonSearch = findViewById(R.id.searchButton);
        myRecycleView = findViewById(R.id.list_video);

        // 检查权限
        if (!checkPermissionAllGranted(mPermissionsArrays)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(mPermissionsArrays, REQUEST_PERMISSION);
            }
        }
        else{
            searchFiles();
        }

        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPermissionAllGranted(mPermissionsArrays)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(mPermissionsArrays, REQUEST_PERMISSION);
                    }
                }
                else{
                    isSearch = true;
                    animationView.animate().alpha(1);
                    searchFiles();
                }
            }
        });

    }

    private void updateRecycleView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        myRecycleView.setLayoutManager(layoutManager);
        try
        {
            mAdapter = new MyAdapter(allVideoList,this);
            myRecycleView.setAdapter(mAdapter);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void searchFiles() {
        buttonSearch.setEnabled(false);
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    allVideoList = new ArrayList<>();
                    getVideoFile(allVideoList, Environment.getExternalStorageDirectory());// 扫描手机内部存储
                    String path = getAppRootOfSdCardRemovable();
                    if(path != null)
                        getVideoFile(allVideoList, new File(getAppRootOfSdCardRemovable()));// 扫描手机外置SD卡
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            animationView.animate().alpha(0).setDuration(500);
                            updateRecycleView();
                            buttonSearch.setEnabled(true);
                            isSearch = false;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onListItemClick(int clickedItemIndex, String path) {
        if(!isSearch) {
            Intent intent = new Intent(this, VideoActivity.class);
            intent.putExtra("Path", path);
            startActivity(intent);
        }
    }

    private boolean checkPermissionAllGranted(String[] permissions) {
        // 6.0以下不需要
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;// 只要有一个权限没有被授予, 则直接返回 false
            }
        }
        return true;
    }

    private void getVideoFile(final List<VideoInfo> list, File file) {// 获得视频文件

        if(file == null)
            return;

        File[] filelist = file.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                // sdCard找到视频名称
                String name = file.getName();
                int i = name.indexOf('.');
                if (i != -1) {
                    name = name.substring(i);
                    if (name.equalsIgnoreCase(".mp4")
                            || name.equalsIgnoreCase(".wmv")
                            || name.equalsIgnoreCase(".rmvb")
                            || name.equalsIgnoreCase(".mov")
                            || name.equalsIgnoreCase(".m4v")
                            || name.equalsIgnoreCase(".avi")
                            || name.equalsIgnoreCase(".mkv")
                            || name.equalsIgnoreCase(".flv")
                            || name.equalsIgnoreCase(".f4v")) {
                        VideoInfo vi = new VideoInfo();
                        vi.setDisplayName(file.getName());
                        vi.setPath(file.getAbsolutePath());
                        list.add(vi);
                        return true;
                    }
                } else if (file.isDirectory()) {
                    getVideoFile(list, file);
                }
                return false;
            }
        });
    }

    /**
     * 获取外置卡（可拆卸的）的目录。
     * Environment.getExternalStorageDirectory()获取的目录，有可能是内置卡的。
     * 在高版本上，能访问的外置卡目录只能是/Android/data/{package}。
     */
    private String getAppRootOfSdCardRemovable() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        /**
         * 这一句取的还是内置卡的目录。
         * /storage/emulated/0/Android/data/com.newayte.nvideo.phone/cache
         * 神奇的是，加上这一句，这个可移动卡就能访问了。
         * 猜测是相当于执行了某种初始化动作。
         */
        StorageManager mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                if ((Boolean) isRemovable.invoke(storageVolumeElement)) {
                    return path;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
