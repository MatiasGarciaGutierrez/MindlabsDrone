package com.example.CombineProject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;

import static java.lang.Thread.sleep;


public class MainActivity extends AppCompatActivity {
    private static int SPLASH_TIME_OUT = 1000;
    ProgressBar progressBar;
    Button button;
    Button upload_button;
    TextView p_text;

    private MediaManager mMediaManager;
    private Bitmap bm;
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/MediaManagerDemo/");
    private int currentProgress = -1;

    public static final int PICK_IMAGE = 1;

    String folder = "new_temp_photos";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,  Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET}, 1);

        /*
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
                startActivity(homeIntent);
                finish();
            }
        }, SPLASH_TIME_OUT);
        */


        setContentView(R.layout.activity_main);




        button = (Button) findViewById(R.id.btn_upload);
        upload_button = (Button) findViewById(R.id.btn_download);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        p_text = (TextView) findViewById(R.id.n_text);


        //createTestFile();

        //String[] file_list = getFileList(folder);
        //createImageList(file_list);

        //LoadingDialog ld = new LoadingDialog(this);

        //ld.startLoadingDialog();


        upload_button.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                initMediaManager();
                Toast.makeText(getApplicationContext(),"Boton onClick",Toast. LENGTH_SHORT).show();
            }
        }));



        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Starting Upload", Toast.LENGTH_SHORT).show();

                AsyncUploader au = new AsyncUploader();
                au.execute(folder);
            }
        });



    }

    private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
        @Override
        public void onFileListStateChange(MediaManager.FileListState state) {
            currentFileListState = state;
        }
    };

    private void initMediaManager() {
        Toast. makeText(getApplicationContext(),"Funcionando int media manager",Toast. LENGTH_SHORT).show();
        //Log.d("DEBUG", DemoApplication.getProductInstance().toString());
        if (DemoApplication.getProductInstance() == null) {
            // Caso de que el dispositivo no se encuentra conectado.
            Toast. makeText(getApplicationContext(),"Disconnected",Toast. LENGTH_SHORT).show();
            // Toast disconnected
            return;
        } else {
            // En caso de estar conectado
            if (null != DemoApplication.getCameraInstance() && DemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                String auxName;
                auxName = "--.--";
                auxName = DemoApplication.getProductInstance().getModel().getDisplayName(); // Nombre del dron conectado ej. Mavic Pro 2
                Toast.makeText(getApplicationContext(),"Connected?..",Toast. LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(),auxName,Toast. LENGTH_SHORT).show();
                mMediaManager = DemoApplication.getCameraInstance().getMediaManager();
                getFileListDrone(); //
            } else if (null != DemoApplication.getCameraInstance()
                    && !DemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                Toast. makeText(getApplicationContext(),"Not sopported",Toast. LENGTH_SHORT).show();

            }
        }
        return;
    }

    @Override
    protected void onDestroy() {
        if (mMediaManager != null) {
            mMediaManager.stop(null);
            mMediaManager.exitMediaDownloading();

        }

        DemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError mError) {
                if (mError != null){
                }
            }
        });
        super.onDestroy();
    }

    private void downloadFileByIndex(final int index){
        if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
                || (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
            return;
        }

        mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<String>() {
            @Override
            public void onFailure(DJIError error) {
                //Toast.makeText(getApplicationContext(),"Descarga fallida..",Toast. LENGTH_SHORT).show();
                currentProgress = -1;
            }

            @Override
            public void onProgress(long total, long current) {
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {
                int tmpProgress = (int) (1.0 * current / total * 100);
                if (tmpProgress != currentProgress) {
                    Toast.makeText(getApplicationContext(),"Progreso: " + tmpProgress,Toast. LENGTH_SHORT).show();
                    currentProgress = tmpProgress;
                }
            }

            @Override
            public void onStart() {
                currentProgress = -1;
                Toast.makeText(getApplicationContext(),"Iniciando descarga..",Toast. LENGTH_SHORT).show();

            }

            @Override
            public void onSuccess(String filePath) {
                Toast.makeText(getApplicationContext(),"Descarga ok ok.." + filePath,Toast. LENGTH_SHORT).show();
                currentProgress = -1;
            }
        });
    }





    private void getFileListDrone() {
        mMediaManager = DemoApplication.getCameraInstance().getMediaManager();
        if (mMediaManager != null) {

            if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)){
                Toast.makeText(getApplicationContext(),"Bussy..",Toast. LENGTH_SHORT).show();
            }else{

                mMediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, new CommonCallbacks.CompletionCallback() {

                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            //Reset data
                            if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
                                mediaFileList.clear();
                            }
                            mediaFileList = mMediaManager.getSDCardFileListSnapshot();

                            if (mediaFileList.size() <= 0) {
                                return;
                            }
                            for (int i = 0; i < mediaFileList.size(); i++) {
                                downloadFileByIndex(i);
                            }
                        }
                    }
                });
            }
        }
    }


    private String[] getFileList(String folder_path){
        String base_folder_path = Environment.getExternalStorageDirectory().toString();
        File image_list_file = new File(base_folder_path+"/new_temp_photos/test_file.txt");

        if (image_list_file.exists()){
            image_list_file.delete();
        }

        File temp_folder = new File(base_folder_path+"/"+folder_path);
        File[] fList = temp_folder.listFiles();
        String[] file_list = new String[fList.length];

        for (int i = 0; i<fList.length; i++){
            file_list[i] = fList[i].getName();
        }

        return file_list;
    }


    @TargetApi(23)
    public void createImageList(String[] fileList){
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            String base_folder_path = Environment.getExternalStorageDirectory().toString();
            Log.d("DEBUG", "Estan los permisos papitos");
            try{
                File newFile = new File(base_folder_path+"/new_temp_photos/state_file.txt");
                FileWriter writer = new FileWriter(newFile, false);
                for (int i = 0; i<fileList.length; i++){
                    if (fileList[i].equals("state_file.txt")){
                        int a = 1;
                        Log.d("DEBUG", "state fileee ");
                    }
                    else{
                        writer.append(fileList[i]+";NOK\n");
                    }

                }
                writer.flush();
                writer.close();
                Toast.makeText(getApplicationContext(), "Data has been written to Report File", Toast.LENGTH_SHORT).show();

            }
            catch (Exception e) {
                Log.d("DEBUG", "EXCEPTION ACAAAAAAAAA: ");
            }
        } else {

            if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                Toast.makeText(getApplicationContext(), "No write external storage permission", Toast.LENGTH_SHORT).show();
            }

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 110);
        }
    }


    private class AsyncUploader extends  AsyncTask<String, Integer, String>{

        private String base_folder_path;
        private String user;
        private  String host;
        private String privateKey;
        private int port;
        private ChannelSftp sftpConnection;
        private Session session;

        public AsyncUploader() {
            this.base_folder_path = Environment.getExternalStorageDirectory().toString();
            this.user = "ubuntu";
            this.host = "18.217.252.160";
            this.privateKey = this.base_folder_path + "/gateway-node.pem";
            this.port = 22;

            try {
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");

                JSch jsch = new JSch();
                jsch.addIdentity(this.privateKey);
                this.session = jsch.getSession(this.user, this.host, this.port);
                session.setConfig(config);
                session.setTimeout(10000);

            } catch (Exception e) {
                Log.d("DEBUG", "Conection failed in constructor: " + e);
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            try{
                this.session.connect();
                this.sftpConnection = (ChannelSftp) this.session.openChannel("sftp");
                this.sftpConnection.connect();

            } catch (Exception e1) {
                Log.d("DEBUG", "Conection failed in doInBackground: " + e1);
            }

            String relative_folder = strings[0];
            String folder_path = this.base_folder_path+"/"+relative_folder;

            String[] file_list = getFileList(folder);
            createImageList(file_list);

            Log.d("DEBUG", "folder_path: "+folder_path);

            safe_create_server_folder(relative_folder);

            try{
                String filename_android_state = this.base_folder_path+"/new_temp_photos/state_file.txt";
                this.sftpConnection.put(filename_android_state, "state_file.txt");
            }catch (Exception e){
                Log.d("DEBUG", "Failed sending state_file");
            }

            while (true) {
                List missing_list = get_missing_images_from_state();
                Log.d("DEBUG", "missing_files: " + missing_list.size());

                if (missing_list.size() == 0){
                    break;
                }

                int count = 1;
                for(int i = 0; i < missing_list.size(); i++){
                    String filename = missing_list.get(i).toString();
                    String filename_android = this.base_folder_path+"/new_temp_photos/"+filename;
                    try{
                        this.sftpConnection.put(filename_android, filename);
                        Log.d("DEBUG", "filename: "+filename);
                        publishProgress(count*100/(missing_list.size()));
                        count = count + 1;
                    } catch (Exception e) {
                        Log.d("DEBUG", "failed in file: " + filename);
                        Log.d("DEBUG", "Exception: " + e);
                    }
                }


                try {
                    sleep(15000);
                    this.sftpConnection.get("state_file.txt", this.base_folder_path + "/new_temp_photos/state_file.txt");
                    Log.d("DEBUG", "i got the state_file");
                } catch (Exception e) {
                    Log.d("DEBUG", "Failed getting state_file");
                }

            }


            this.sftpConnection.exit();
            this.sftpConnection.disconnect();

            return "Everything all right";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            p_text.setVisibility(View.VISIBLE);

            //Create the index files in a .txt
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.INVISIBLE);
            p_text.setVisibility(View.INVISIBLE);
            Toast.makeText(getApplicationContext(), "All photos sended!", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
            p_text.setText(Integer.toString(values[0])+"%");
        }

        private void safe_create_server_folder(String folder_name){
            try{
                this.sftpConnection.cd(folder_name);

            } catch (Exception e) {
                try{
                    this.sftpConnection.mkdir(folder_name);
                    this.sftpConnection.cd(folder_name);
                } catch (Exception e2) {
                    Log.d("DEBUG", "failed creating folder: " + folder_name);
                }

            }

        }

    }


    public List get_missing_images_from_state(){
        List missing_list = new ArrayList();

        String base_folder_path = Environment.getExternalStorageDirectory().toString();
        File newFile = new File(base_folder_path+"/new_temp_photos/state_file.txt");
        try{
            FileInputStream fis = new FileInputStream(newFile);
            DataInputStream in = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            while ((strLine = br.readLine()) != null) {
                String[] split_str = strLine.split(";");

                if (split_str[1].equals("NOK")){
                    missing_list.add(split_str[0]);
                }

            }
            br.close();
            in.close();
            fis.close();
        }catch (Exception e){
            Log.d("DEBUG", "Failed in reading state file");
        }

        return missing_list;
    }


    // Checks if a volume containing external storage is available
    // for read and write.
    private boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED;
    }

    // Checks if a volume containing external storage is available to at least read.
    private boolean isExternalStorageReadable() {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED ||
                Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED_READ_ONLY;
    }
}



