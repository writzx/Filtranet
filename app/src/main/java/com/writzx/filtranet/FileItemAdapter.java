package com.writzx.filtranet;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FileItemAdapter extends ArrayAdapter<ListItem> {
    private class FileViewHolder {
        private ImageView fileicon;
        private TextView filename;
        private TextView filesize;
        private TextView filetype;
        private TextView filepath;
        private TextView date_added;
    }

    private class ProgressViewHolder {
        private TextView filename;
        private TextView filepath;
        private TextView fileProgress;
        private ProgressBar fileProgressBar;
    }

    public FileItemAdapter(@NonNull Context context, @NonNull List<ListItem> objects) {
        super(context, R.layout.file_list_item, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ListItem listitem = getItem(position);

        if (listitem != null) {
            switch (listitem.getType()) {
                case ListItem.TYPE_FILE:
                    FileItem fileitem = (FileItem) listitem;
                    FileViewHolder fileViewHolder;

                    if (convertView == null) {
                        fileViewHolder = new FileViewHolder();

                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        convertView = inflater.inflate(R.layout.file_list_item, parent, false);

                        fileViewHolder.fileicon = convertView.findViewById(R.id.fileIcon);
                        fileViewHolder.filename = convertView.findViewById(R.id.fileName);
                        fileViewHolder.filesize = convertView.findViewById(R.id.fileSize);
                        fileViewHolder.filepath = convertView.findViewById(R.id.filePath);
                        fileViewHolder.filetype = convertView.findViewById(R.id.fileType);
                        fileViewHolder.date_added = convertView.findViewById(R.id.dateAdded);

                        convertView.setTag(fileViewHolder);
                    } else {
                        fileViewHolder = (FileViewHolder) convertView.getTag();
                    }

                    if (fileViewHolder != null) {
                        fileViewHolder.fileicon.setImageDrawable(fileitem.getIcon());
                        fileViewHolder.filename.setText(fileitem.getFileName());
                        fileViewHolder.filepath.setText(fileitem.getPath());
                        fileViewHolder.filetype.setText(fileitem.getMimeType());
                        fileViewHolder.filesize.setText(FileUtils.getReadableFileSize(fileitem.getFileSize()));
                        fileViewHolder.date_added.setText(FileItem.dateformat.format(fileitem.getDateAdded()));
                    }
                    break;
                case ListItem.TYPE_PROGRESS:
                    ProgressItem progressitem = (ProgressItem) listitem;
                    ProgressViewHolder progressViewHolder;

                    if (convertView == null) {
                        progressViewHolder = new ProgressViewHolder();

                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        convertView = inflater.inflate(R.layout.progress_list_item, parent, false);

                        progressViewHolder.filename = convertView.findViewById(R.id.pFileName);
                        progressViewHolder.filepath = convertView.findViewById(R.id.pFilePath);
                        progressViewHolder.fileProgress = convertView.findViewById(R.id.pFileProgress);
                        progressViewHolder.fileProgressBar = convertView.findViewById(R.id.pFileProgressBar);

                        convertView.setTag(progressViewHolder);
                    } else {
                        progressViewHolder = (ProgressViewHolder) convertView.getTag();
                    }

                    if (progressViewHolder != null) {
                        progressViewHolder.filename.setText(progressitem.getFileName());
                        progressViewHolder.filepath.setText(progressitem.getFilePath());

                        int bytesRead = progressitem.getBytesRead();
                        int totalLen = progressitem.getTotalLength();

                        float percent = totalLen == 0 ? 0 : 100 * (((float) bytesRead) / totalLen);
                        progressViewHolder.fileProgress.setText(String.format(Locale.getDefault(), "%s of %s (%.2f %%)",
                                FileUtils.getReadableFileSize(bytesRead), FileUtils.getReadableFileSize(totalLen), percent));

                        progressViewHolder.fileProgressBar.setMax(totalLen);
                        progressViewHolder.fileProgressBar.setProgress(bytesRead);
                    }
                    break;
            }
        }

        // Return the completed view to render on screen
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return Objects.requireNonNull(getItem(position)).getType();
    }

    //to check if the adapter contains a specific file
    public boolean contains(String filepath) {
        for (int i = 0; i < getCount(); i++) {
            ListItem li = getItem(i);
            if ((li instanceof FileItem && ((FileItem) li).getPath().equals(filepath)) ||
                    (li instanceof ProgressItem && ((ProgressItem) li).getFilePath().equals(filepath))) {
                return true;
            }
        }
        return false;
    }
}
