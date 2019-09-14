package com.sitinoldae.assignment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private EditText editText;
    private AlertDialog progressDialog;
    private Button open,download,paste;
    private ImageView imagePreview;
    private VideoView videoPreview;
    private Toast toast;
    private boolean isvideo;
    private boolean isImage;
    private String fileName;
    //dec fetch
    private Vibrator vibrator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText= findViewById(R.id.edittext);
        open= findViewById(R.id.openBtn);
        download= findViewById(R.id.downloadBtn);
        paste= findViewById(R.id.pasteBtn);
        imagePreview= findViewById(R.id.imagePreview);
        videoPreview= findViewById(R.id.videoPreview);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        progressDialog = new AlertDialog.Builder(MainActivity.this).create();
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("The image is being downloaded !");
        toast=Toast.makeText(getApplicationContext(),"null",Toast.LENGTH_SHORT);
        //adding listeners
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                try {
                    String textToPaste = (String) clipboard.getPrimaryClip().getItemAt(0).getText();
                    editText.setText(textToPaste);
                } catch (Exception e) {
                    toaster(e.getMessage());
                    return;
                }
                //paste data from clipboard
            }
        });
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //opening the url to a viewer
                String CurrentUrl=editText.getText().toString().trim();
                if(!CurrentUrl.isEmpty()){
                    //do something
                    if(!CurrentUrl.contains("htt")){
                        CurrentUrl="http://"+CurrentUrl;
                    }
                    checkfiletype(CurrentUrl,true);
                }else{
                    toaster("insert url first");
                }
            }
        });
        paste.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                editText.setText("https://file-examples.com/wp-content/uploads/2017/04/file_example_MP4_480_1_5MG.mp4");
                return true;
            }
        });
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               String url = editText.getText().toString().trim();
                //checking if url contains http prefix
                if(!url.contains("htt")){
                  url="http://"+url;
                }
                if(isWriteStoragePermissionGranted()){
                    checkfiletype(url,false);
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
        }
        if(progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private void downloadFile( String url, final String extension) {
        final String currentURL=url;
        if(isImage){
            //download image
            Picasso.get()
                    .load(url)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .into(new Target() {
                @Override
                public void onBitmapLoaded( Bitmap bitmap, Picasso.LoadedFrom from) {
                    saveImageToExternalStorage(bitmap);
                    imagePreview.setImageBitmap(bitmap);
                    progressDialog.dismiss();
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    toaster("downloading failed with format :"+extension+currentURL);
                    imagePreview.setImageDrawable(errorDrawable);
                    Picasso.get().load(currentURL).into(imagePreview);
                    progressDialog.dismiss();
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    imagePreview.setImageDrawable(placeHolderDrawable);
                    toaster("loading...");
                    progressDialog.show();
                }
            });
        }
        if(isvideo){
            toaster("video determined");
            //download video
            Uri uri = Uri.parse(url);
            DownloadManager.Request r = new DownloadManager.Request(uri);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS+"/Assignment/videos", fileName);
            r.allowScanningByMediaScanner();
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            try {
                Objects.requireNonNull(dm).enqueue(r);
                toaster("downloading...");
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("downloading "+fileName);
                alertDialog.setMessage("do you want to play it as well ?");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                MediaController mediaController = new MediaController(getApplicationContext());
                                videoPreview.setMediaController(mediaController);
                                videoPreview.setVideoURI(Uri.parse(url));
                                videoPreview.start();
                                dialog.dismiss();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "no", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                alertDialog.show();
                vibrator.vibrate(400);
            } catch (Exception e) {
                e.printStackTrace();
                toaster("downloading failed :"+e.getMessage());
            }
        }
        isvideo=false;
        isImage=false;
        editText.setText("");
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

    private void checkfiletype(String url, Boolean open) {
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
                    if(open==true){
                        openFile(url);
                    }else
                    downloadFile(url,extension);
                    break;
                case ".mp4":
                case ".avi":
                case ".3gp":
                    isvideo=true;
                    toaster("video file found");
                    if(open==true){
                        openFile(url);
                    }else
                        downloadFile(url, extension);
                    break;
                default:
                    toaster("invalid link: "+url+extension);
                    break;
            }
        }else {
            toaster("invalid url : "+url);
        }
    }

    private void openFile(String url) {
        //open file
        if(isImage){
            Picasso.get().load(Uri.parse(url)).networkPolicy(NetworkPolicy.NO_CACHE).into(imagePreview);
        }
        if(isvideo){
            toaster("video file found");
            videoPreview.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    mediaPlayer.release();
                    toaster("error");
                    return true;
                }
            });
            videoPreview.setVideoPath(url);
            videoPreview.start();
            toaster("playing video");
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(url);
                intent.setData(uri);
                startActivity(intent);

            }
        };
        isvideo=false;
        isImage=false;
    }

    private void toaster(String message){
        Toast.makeText(getApplicationContext(),message.trim(),Toast.LENGTH_SHORT).show();
    }

    private void saveImageToExternalStorage(Bitmap finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File myDir = new File(root +"/Assignment/images");
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
        toaster("file saved in Downloads/Assignment: "+fname.trim());
        vibrator.vibrate(400);
        }
        catch (Exception e) {
            e.printStackTrace();
            toaster(Objects.requireNonNull(e.getMessage()));
        }


        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(this, new String[] { file.toString() }, null,
                (path, uri) -> {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                    Log.i("ExternalStorage", "-> uri=" + uri);
                });

    }
}