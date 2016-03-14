package com.example.falling.downloadwithasynctask;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int REQUEST_CODE = 1;
    private Button mDownloadButton;
    private EditText mUrlText;
    private ProgressBar mProgressBar;
    private String mDownloadFolderName;
    private String mDownloadFileName;
    boolean isDownloading = false;
    private MyTask myTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downlaod);
        mDownloadButton = (Button) findViewById(R.id.download_button);
        mUrlText = (EditText) findViewById(R.id.Download_Url);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mDownloadButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.download_button:
                String url = mUrlText.getText().toString();
                if(url != null && url.length() > 0 ) {
                    if (InternetUtil.isNetworkConnected(v.getContext())) {
                        //查看权限
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            //申请WRITE_EXTERNAL_STORAGE权限
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_CODE);
                        } else {
                            if (!isDownloading) {
                                myTask = new MyTask();
                                isDownloading = true;
                                myTask.execute(url);
                            } else {
                                isDownloading = false;
                                myTask.cancel(true);
                            }
                        }
                    } else {
                        Toast.makeText(v.getContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(v.getContext(), getString(R.string.error_URL), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "下载开始", Toast.LENGTH_SHORT).show();
                    MyTask myTask = new MyTask();
                    myTask.execute(mUrlText.getText().toString());
                } else {
                    Toast.makeText(this, "没有权限无法下载", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private class MyTask extends AsyncTask<String, Integer, Boolean> {
        private int TotalSize; //总文件大小
        private InputStream inputStream; //下载的流
        private File fileName;
        private File foldName;
        private OutputStream outputStream;

        @Override
        protected Boolean doInBackground(String... params) {
            Log.i("tag", "doInBack");
            String strUrl = params[0];
            try {
                URL url = new URL(strUrl);
                URLConnection urlConnection = url.openConnection();
                inputStream = urlConnection.getInputStream();
                TotalSize = urlConnection.getContentLength();
                //设置文件夹路径和文件路径
                setFoldFileName(strUrl);
                outputStream = new FileOutputStream(fileName);

                if (checkFoldAndFile(foldName, fileName)) {
                    inputStream.close();
                    outputStream.close();
                    return false;
                }

                //下载
                startDownload();

                inputStream.close();
                outputStream.close();
                return true;

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDownloadButton.setText("取消");
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(0);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            mDownloadButton.setText("下载");
            if (aBoolean) {
                Toast.makeText(MainActivity.this, "下载完成，目录在 " + mDownloadFileName, Toast.LENGTH_LONG).show();
            } else {
                mProgressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, "下载失败，URL是否正确？", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mProgressBar.setVisibility(View.INVISIBLE);
            mDownloadButton.setText("下载");
            Toast.makeText(MainActivity.this, "下载取消", Toast.LENGTH_LONG).show();
            fileName.delete();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mProgressBar.setProgress(values[0]);
        }

        /**
         * 设置文件路径和文件夹路径
         *
         * @param strUrl
         */
        private void setFoldFileName(String strUrl) {
            //文件路径
            mDownloadFolderName = Environment.getExternalStorageDirectory() + "/MyDownloadDemo/";
            //文件名
            String strings[] = strUrl.split("/");
            mDownloadFileName = mDownloadFolderName + strings[strings.length - 1];

            fileName = new File(mDownloadFileName);
            foldName = new File(mDownloadFolderName);
        }

        /**
         * 下载
         *
         * @throws IOException
         */
        private void startDownload() throws IOException {
            int downloadSize = 0;
            byte[] bytes = new byte[2048];
            int length;
            int progress;
            while (!isCancelled() && (length = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, length);
                outputStream.flush();
                downloadSize += length;
                progress = downloadSize * 100 / TotalSize;
                publishProgress(progress);
            }
        }


        /**
         * 检查文件夹是否创建，未创建则创建。 检查要下载的文件是否已经存在，存在则删除。
         *
         * @param foldName
         * @param fileName
         * @return
         */
        private boolean checkFoldAndFile(File foldName, File fileName) {

            if (!foldName.exists()) {
                if (!foldName.mkdir()) {
                    return true;
                }
            }

            if (fileName.exists()) {
                if (!fileName.delete()) {
                    return true;
                }
            }
            return false;
        }
    }
}
