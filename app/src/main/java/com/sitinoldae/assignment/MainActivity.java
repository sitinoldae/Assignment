package com.sitinoldae.assignment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private EditText editText;
    private Button button;
    private String url;
    private ImageView imageView;
    private Toast toast;
    private boolean isvideo;
    private boolean isImage;
    private String fileName;
    private Vibrator vibrator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText= findViewById(R.id.editText);
        button= findViewById(R.id.button);
        imageView= findViewById(R.id.imageView);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        toast=Toast.makeText(getApplicationContext(),"null",Toast.LENGTH_SHORT);
        //adding listeners
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                editText.setText("https://file-examples.com/wp-content/uploads/2017/04/file_example_MP4_480_1_5MG.mp4");
                return true;
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                url = editText.getText().toString().trim();
                //checking if url contains http prefix
                if(!url.contains("htt")){
                  url="http://"+url;
                }
                if(isWriteStoragePermissionGranted()){
                    checkfiletype(url);
                }else{
                    toaster("permission denied");
                }

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toaster("storage permission" + "was " + " granted");
                //resume tasks needing this permission
                downloadFile();
            }
            if (grantResults[0] == -1) {
                toaster("storage permission" + "was " + " denied");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(editText.getText().toString().matches("")){
            editText.setText("");
            fileName="";
        }else{
            recreate();
        }
    }

    private void downloadFile() {
        if(isImage){
            //download image
            Picasso.get().load(url).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    saveImageToExternalStorage(bitmap);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    imageView.setImageBitmap(bitmap);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    toaster("downloading failed :"+url);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    Picasso.get().load(url).into(imageView);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    toaster("loading...");
                }
            });
        }
        if(isvideo){
            //download video
            Uri uri = Uri.parse(url);
            DownloadManager.Request r = new DownloadManager.Request(uri);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            r.allowScanningByMediaScanner();
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            try {
                Objects.requireNonNull(dm).enqueue(r);
                toaster("downloading "+fileName);
                vibrator.vibrate(400);
            } catch (Exception e) {
                e.printStackTrace();
                toaster("downloading failed :"+e.getMessage());
            }
        }
    }
    private boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                toaster("Permission is granted by user");
                return true;
            } else {
                toaster("Permission is denied");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            toaster("permission granted by os");
            return true;
        }
    }

    private void checkfiletype(String url) {
        toaster("checking file type");
        if(url.contains(".")) {
            String extension = url.substring(url.lastIndexOf(".")).toLowerCase();
             fileName = url.substring(url.lastIndexOf('/') + 1);
            switch (extension){
                case ".png":
                case ".jpg":
                case ".tif":
                case ".bmp":
                case ".jpeg":
                case ".webp":
                case ".gif":
                    isImage=true;
                    toaster("image file found");
                    downloadFile();
                    break;
                case ".mp4":
                case ".avi":
                case ".3gp":
                    isvideo=true;
                    toaster("video file found");
                    downloadFile();
                    break;
                default:
                    toaster("invalid link: "+url+extension);
                    break;
            }
        }else {
            toaster("invalid url : "+url);
        }
    }

    private void toaster(String message){
        Toast.makeText(getApplicationContext(),message.trim(),Toast.LENGTH_SHORT).show();
    }

    private void saveImageToExternalStorage(Bitmap finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        String fname = "Image-" + n + ".jpg";
        if(!(fileName ==null)){
            fname=fileName;
        }
        n = generator.nextInt(n);
        File file = new File(myDir, fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 94, out);
            out.flush();
            out.close();
        toaster("file saved in Pictures/saved_images : "+fname.trim());
        vibrator.vibrate(400);
        }
        catch (Exception e) {
            e.printStackTrace();
            toaster(Objects.requireNonNull(e.getMessage()));
        }


        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(this, new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });

    }
}