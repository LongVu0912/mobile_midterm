package com.example.mobile_midterm;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.File;
import java.util.List;
import java.util.Objects;

import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_midterm.adapters.DownloadAdapter;
import com.example.mobile_midterm.databases.DatabaseHelper;
import com.example.mobile_midterm.models.DownloadModel;
import com.example.mobile_midterm.utils.DownloadUtil;
import com.example.mobile_midterm.utils.PathUtil;

import java.util.ArrayList;

public class DownloadFragment extends Fragment implements ItemClickListener {
    private static final int PERMISSION_REQUEST_CODE = 101;
    DownloadAdapter downloadAdapter;
    List<DownloadModel> downloadModels = new ArrayList<>();
    private DatabaseHelper dbHelper;
    RecyclerView data_list;
    View view;
    Button add_download_list, clear_download_list;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_download, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        //Init props
        dbHelper = new DatabaseHelper(view.getContext());
        add_download_list = view.findViewById(R.id.add_download_list);
        clear_download_list = view.findViewById(R.id.clear_download_list);
        data_list = view.findViewById(R.id.data_list);

        //Init action
        add_download_list.setOnClickListener(v -> handleShowInputDialog());
        clear_download_list.setOnClickListener(v -> handleClearAllDownload());

        //Init adapter view
        List<DownloadModel> downloadModelsLocal = dbHelper.getAllDownloads();
        if (downloadModelsLocal != null && !downloadModelsLocal.isEmpty()) {
            downloadModels.addAll(downloadModelsLocal);

            for (int i = 0; i < downloadModels.size(); i++) {
                if (downloadModels.get(i).getStatus().equalsIgnoreCase("Pending") || downloadModels.get(i).getStatus().equalsIgnoreCase("Running") || downloadModels.get(i).getStatus().equalsIgnoreCase("Downloading")) {
                    //Continue download the file which still in download process
                    DownloadStatusTask downloadStatusTask = new DownloadStatusTask(downloadModels.get(i));
                    runTask(downloadStatusTask, "" + downloadModels.get(i).getDownloadId());
                }
            }
        }
        downloadAdapter = new DownloadAdapter(view.getContext(), downloadModels, this);
        data_list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        data_list.setAdapter(downloadAdapter);
    }
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void handleClearAllDownload() {
        dbHelper.deleteAllDownloads();
        downloadModels.clear();

        downloadAdapter.notifyDataSetChanged();
        // renew the correct position
        downloadAdapter.notifyItemRangeChanged(0, downloadModels.size());
    }

    @Override
    public void handleClickDownloadItem(String file_path, String status) {
        if (!Objects.equals(status, "Completed")) {
            Toast.makeText(view.getContext(), "File is not Downloaded Yet", Toast.LENGTH_SHORT).show();
            return;
        }
        openFile(file_path);
    }

    @Override
    public void handleClickShare(DownloadModel downloadModel) {
        File file = new File(downloadModel.getFile_path().replaceAll("file:///", ""));

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, "Sharing File from File Downloader");

            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri path = FileProvider.getUriForFile(view.getContext(), "com.example.download_manager", file);
            intent.putExtra(Intent.EXTRA_STREAM, path);
            intent.setType("*/*");
            startActivity(intent);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Toast.makeText(view.getContext(), "No Activity Availabe to Handle File", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void handleClickRemove(int index) {
        //remove a clicked download row from db
        DownloadModel downloadModel = downloadModels.get(index);

        dbHelper.deleteDownloadById(Long.valueOf(downloadModel.getDownloadId()).intValue());
        System.out.println("index: " + index);
        downloadModels.remove(index);
        downloadAdapter.notifyItemRemoved(index);

        // renew the correct position
        downloadAdapter.notifyItemRangeChanged(0, downloadModels.size());
    }

    @Override
    public void handleShowInputDialog() {
        AlertDialog.Builder al = new AlertDialog.Builder(view.getContext());
        View view = getLayoutInflater().inflate(R.layout.download_input_dialog, null);
        al.setView(view);

        final EditText editText = view.findViewById(R.id.input);
        Button paste = view.findViewById(R.id.paste);

        paste.setOnClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            try {
                CharSequence charSequence = Objects.requireNonNull(clipboardManager.getPrimaryClip()).getItemAt(0).getText();
                editText.setText(charSequence);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });

        al.setPositiveButton("Download", (dialog, which) -> downloadFile(editText.getText().toString()));

        al.setNegativeButton("Cancel", (dialog, which) -> {

        });
        AlertDialog AL = al.create();
        AL.setOnShowListener(dialog -> {
            Button positiveBtn = AL.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeBtn = AL.getButton(DialogInterface.BUTTON_NEGATIVE);

            positiveBtn.setTextColor(getResources().getColor(R.color.green700, null));
            negativeBtn.setTextColor(getResources().getColor(R.color.red700, null));

        });
        AL.show();
    }

    private void downloadFile(String url) {

        //Check permission of access storage in android device
        if (!checkPermission()) {
            requestPermission();
            Toast.makeText(view.getContext(), "Please Allow Permission to Download File", Toast.LENGTH_SHORT).show();
            return;
        }

        //Preprocessing
        String filename = URLUtil.guessFileName(url, null, null);
        String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String type = filename.split("\\.")[1];
        File file = new File(downloadPath, filename);

        //create basic request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setMimeType(type)
                .setTitle(filename)
                .setDescription("Downloading")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);

        //add request to download queue
        DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(DOWNLOAD_SERVICE);
        long downloadId = downloadManager.enqueue(request);

        // init model, add to view and db
        final DownloadModel downloadModel = DownloadModel.builder()
                .status("Downloading")
                .title(filename)
                .file_size("0")
                .progress("0")
                .isPaused(false)
                .downloadId(downloadId)
                .file_path("")
                .build();

        downloadModels.add(downloadModel);
        downloadAdapter.notifyItemInserted(downloadModels.size() - 1);

        dbHelper.addDownload(downloadModel);

        DownloadStatusTask downloadStatusTask = new DownloadStatusTask(downloadModel);
        runTask(downloadStatusTask, "" + downloadId);
    }

    @SuppressLint("StaticFieldLeak")
    public class DownloadStatusTask extends AsyncTask<String, String, String> {
        DownloadModel downloadModel;

        public DownloadStatusTask(DownloadModel downloadModel) {
            this.downloadModel = downloadModel;
        }

        @Override
        protected String doInBackground(String... strings) {
            downloadFileProcess(strings[0]);
            return null;
        }

        @SuppressLint("Range")
        private void downloadFileProcess(String downloadId) {
            DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(DOWNLOAD_SERVICE);
            boolean downloading = true;
            while (downloading) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(Long.parseLong(downloadId));
                Cursor cursor = downloadManager.query(query);
                cursor.moveToFirst();

                @SuppressLint("Range") int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                @SuppressLint("Range") int total_size = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false;
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("file_size", DownloadUtil.bytesIntoHumanReadable(total_size));
                    contentValues.put("progress", "100");

                    db.update("DownloadModel", contentValues, "downloadId=?", new String[]{String.valueOf(downloadModel.getDownloadId())});
                }

                int progress = (int) ((bytes_downloaded * 100L) / total_size);
                String status = DownloadUtil.getStatusMessage(cursor);

                publishProgress(new String[]{String.valueOf(progress), String.valueOf(bytes_downloaded), status});
                cursor.close();
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void onProgressUpdate(final String... values) {
            super.onProgressUpdate(values);
            downloadModel.setFile_size(DownloadUtil.bytesIntoHumanReadable(Long.parseLong(values[1])));
            downloadModel.setProgress(values[0]);
            if (!downloadModel.getStatus().equalsIgnoreCase("PAUSE") && !downloadModel.getStatus().equalsIgnoreCase("RESUME")) {
                downloadModel.setStatus(values[2]);
            }
            downloadAdapter.changeItem(downloadModel.getDownloadId());
        }
    }

    BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            boolean comp = downloadAdapter.ChangeItemWithStatus("Completed", id);

            if (comp) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(DOWNLOAD_SERVICE);
                Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
                cursor.moveToFirst();

                @SuppressLint("Range") String downloaded_path = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                downloadAdapter.setChangeItemFilePath(downloaded_path, id);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        view.getContext().unregisterReceiver(onComplete);
    }

    public void runTask(DownloadStatusTask downloadStatusTask, String id) {
        try {
            downloadStatusTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(getContext(), "Please Give Permission to Upload File", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions((Activity) requireContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(view.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(view.getContext(), "Permission Successfull", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(view.getContext(), "Permission Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openFile(String fileurl) {
        if (!checkPermission()) {
            requestPermission();
            Toast.makeText(view.getContext(), "Please Allow Permission to Open File", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            fileurl = PathUtil.getPath(view.getContext(), Uri.parse(fileurl));

            assert fileurl != null;
            File file = new File(fileurl);
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
            String type = mimeTypeMap.getMimeTypeFromExtension(ext);

            if (type == null) {
                type = "*/*";
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contne = FileProvider.getUriForFile(view.getContext(), "com.example.download_manager", file);
            intent.setDataAndType(contne, type);
            startActivity(intent);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Toast.makeText(view.getContext(), "Unable to Open File", Toast.LENGTH_SHORT).show();
        }
    }
}
