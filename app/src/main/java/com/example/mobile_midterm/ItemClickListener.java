package com.example.mobile_midterm;

import com.example.mobile_midterm.models.DownloadModel;

public interface ItemClickListener {
    void handleClickDownloadItem(String file_path, String status);

    void handleClickShare(DownloadModel downloadModel);

    void handleClickRemove(int position);

    void handleClearAllDownload();

    void handleShowInputDialog();
}
