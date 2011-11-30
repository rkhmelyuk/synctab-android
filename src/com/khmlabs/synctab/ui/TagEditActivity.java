package com.khmlabs.synctab.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.khmlabs.synctab.R;
import com.khmlabs.synctab.RemoteOpStatus;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.db.DbMetadata;
import com.khmlabs.synctab.db.DbMetadata.TagsColumns;
import com.khmlabs.synctab.db.SyncTabDatabase;

/**
 * Activity to edit the list of tags: add new, rename and remove some.
 */
public class TagEditActivity extends BaseUserActivity implements AddSupport, RefreshSupport {

    private static final String TAG = "TagEditActivity";

    static final String[] ADAPTER_FROM = {TagsColumns.NAME};
    static final int[] ADAPTER_TO = {R.id.tag_name};

    private SimpleCursorAdapter.ViewBinder TAG_BINDER;

    ListView tags;
    SimpleCursorAdapter adapter;

    SyncTabDatabase database;
    boolean refreshing = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_tagedit);

        TAG_BINDER = new TagRowBinder(getSyncTabApplication());

        tags = (ListView) findViewById(R.id.tags);

        database = new SyncTabDatabase(this);
    }

    protected void onResume() {
        super.onResume();

        if (getSyncTabApplication().isAuthenticated()) {
            refresh();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        database.close();
    }

    public void refresh() {
        if (!refreshing) {
            refreshAdapter();
            new RefreshTagsListTask().execute();
        }
    }

    private void refreshAdapter() {
        final Cursor cursor = database.findTags();
        startManagingCursor(cursor);

        if (adapter == null) {
            adapter = new SimpleCursorAdapter(
                    this, R.layout.tag_row, cursor,
                    ADAPTER_FROM, ADAPTER_TO);

            adapter.setViewBinder(TAG_BINDER);
            tags.setAdapter(adapter);
        }
        else {
            adapter.getCursor().requery();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tagedit_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh: {
                refresh();
                return true;
            }
            case R.id.add: {
                add();
                return true;
            }
        }
        return false;
    }

    /**
     * Shows a dialog with request to enter new tag name.
     * User enters new tag name and presses Save button to add new or
     * Cancel if she doesn't want to create new tag.
     */
    public void add() {
        final EditText editText = new EditText(getApplicationContext());
        editText.setSingleLine(true);

        buildTagDialog(this, R.string.add_new_tag, R.string.enter_new_tag_name, editText,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name = editText.getText().toString().trim();
                        boolean correctName = validateTagName(TagEditActivity.this, name);

                        // if name is correct, than add a new tag and hide dialog
                        if (correctName) {
                            new AddTagTask().execute(name);
                            dialog.dismiss();
                        }
                    }
                }
        ).show();
    }

    /**
     * Shows a dialog to rename specified tag.
     * User may cancel edition, or change a name and save it.
     *
     * @param tagId       the local id of the tag.
     * @param currentName the current tag name, so user just edit existing.
     */
    private void renameTag(final int tagId, final String currentName) {
        final EditText editText = new EditText(getApplicationContext());
        editText.setSingleLine(true);
        editText.setText(currentName);

        buildTagDialog(this, R.string.rename, R.string.enter_new_tag_name, editText,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = editText.getText().toString().trim();
                        boolean correctName = validateTagName(TagEditActivity.this, newName);

                        // if name is correct, than rename a tag and hide dialog
                        if (correctName) {
                            new RenameTagTask().execute(tagId, newName);
                            dialog.dismiss();
                        }
                    }
                }
        ).show();
    }

    /**
     * Build a tag add/edit dialog. This dialog only shows a message and an input field for name.
     *
     * @param ctx          the request context.
     * @param title        the dialog title.
     * @param message      the dialog message.
     * @param input        the input element for tag name.
     * @param saveListener the action to call on save listeners.
     * @return the built tag dialog.
     */
    private static AlertDialog buildTagDialog(Context ctx, int title, int message, EditText input,
                                              DialogInterface.OnClickListener saveListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        // max length constraint
        int maxLength = ctx.getResources().getInteger(R.integer.tag_name_maxlength);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});

        builder.setView(getTagEditView(ctx, input));
        builder.setMessage(message);
        builder.setTitle(title);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.save, saveListener);

        return builder.create();
    }

    /**
     * Validates a tag name.
     *
     * @param ctx  the current context.
     * @param name the tag name.
     * @return true if name if valid, otherwise false.
     */
    private static boolean validateTagName(Context ctx, String name) {
        if (name.length() == 0) {
            Toast.makeText(ctx, R.string.tag_name_required, 3000).show();

            return false;
        }

        return true;
    }

    private static LinearLayout getTagEditView(Context ctx, EditText editText) {
        final LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);

        int margin = (int) ctx.getResources().getDimension(R.dimen.dialog_input_margin);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(margin, 0, margin, margin);

        layout.addView(editText, params);

        return layout;
    }

    private void removeTag(final int tagId, final String name) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String message = getResources().getString(R.string.confirm_remove_tag, name);

        builder.setMessage(message);
        builder.setTitle(R.string.remove);
        builder.setNegativeButton(android.R.string.no, null);
        builder.setPositiveButton(R.string.yes_remove, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                new RemoveTagTask().execute(tagId);
                dialog.dismiss();
            }
        });

        builder.show();
    }

    /**
     * The asynchronous task to download the list of tags from server.
     */
    private class RefreshTagsListTask extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... voids) {
            try {
                return getSyncTabApplication().getFacade().refreshTags();
            }
            catch (Exception e) {
                Log.e(TAG, "Error to refresh list of tags", e);
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            refreshing = true;
            titlebarHelper.setRefreshing(true);
        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                refreshAdapter();
            }

            refreshing = false;
            titlebarHelper.setRefreshing(false);
        }
    }

    /**
     * The async task to rename the tag.
     */
    private class RenameTagTask extends AsyncTask<Object, Void, RemoteOpStatus> {

        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            String message = getResources().getString(R.string.please_wait);
            progress = ProgressDialog.show(TagEditActivity.this, null, message, true, false);
        }

        @Override
        protected void onPostExecute(RemoteOpStatus status) {
            super.onPostExecute(status);

            if (status == RemoteOpStatus.Failed) {
                Toast.makeText(TagEditActivity.this, R.string.failed_rename_tag, 3000).show();
            }
            else {
                refreshAdapter();
            }

            progress.dismiss();
        }

        @Override
        protected RemoteOpStatus doInBackground(Object... params) {
            try {
                Integer tagId = (Integer) params[0];
                String newName = (String) params[1];

                return getSyncTabApplication().getFacade().renameTag(tagId, newName);
            }
            catch (Exception e) {
                Log.e(TAG, "Error to rename a tag", e);
            }
            return RemoteOpStatus.Failed;
        }
    }

    /**
     * The async task to add the tag.
     */
    private class AddTagTask extends AsyncTask<String, Void, RemoteOpStatus> {

        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            String message = getResources().getString(R.string.please_wait);
            progress = ProgressDialog.show(TagEditActivity.this, null, message, true, false);
        }

        @Override
        protected void onPostExecute(RemoteOpStatus status) {
            super.onPostExecute(status);

            if (status == RemoteOpStatus.Failed) {
                Toast.makeText(TagEditActivity.this, R.string.failed_rename_tag, 3000).show();
            }
            else {
                refreshAdapter();
            }

            progress.dismiss();
        }

        @Override
        protected RemoteOpStatus doInBackground(String... params) {
            try {
                String name = params[0];

                return getSyncTabApplication().getFacade().addTag(name);
            }
            catch (Exception e) {
                Log.e(TAG, "Error to add a new tag", e);
            }
            return RemoteOpStatus.Failed;
        }
    }

    /**
     * The async task to remove the tag.
     */
    private class RemoveTagTask extends AsyncTask<Integer, Void, RemoteOpStatus> {

        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            String message = getResources().getString(R.string.please_wait);
            progress = ProgressDialog.show(TagEditActivity.this, null, message, true, false);
        }

        @Override
        protected void onPostExecute(RemoteOpStatus status) {
            super.onPostExecute(status);

            if (status == RemoteOpStatus.Failed) {
                Toast.makeText(TagEditActivity.this, R.string.failed_remove_tag, 3000).show();
            }
            else {
                refreshAdapter();
            }

            progress.dismiss();
        }

        @Override
        protected RemoteOpStatus doInBackground(Integer... params) {
            try {
                final Integer tagId = params[0];
                if (tagId != null) {
                    return getSyncTabApplication().getFacade().removeTag(tagId);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error to remove tag");
            }

            return RemoteOpStatus.Failed;
        }
    }

    /**
     * The tag row binder. Responsible for binding actions to the rename/remove buttons,
     * hiding remove button for current tag etc.
     */
    private class TagRowBinder implements SimpleCursorAdapter.ViewBinder {

        // cache column indexes here
        int idColumnIndex = -1;
        int rowIdColumnIndex = -1;
        int nameColumnIndex = -1;

        // cache current tag.
        String currentTag = null;

        /** Single instance of the rename button click listener. */
        final View.OnClickListener renameOnClickListener = new View.OnClickListener() {
            public void onClick(View view) {
                final int position = (Integer) view.getTag();
                final Cursor cursor = (Cursor) tags.getAdapter().getItem(position);

                final int id = cursor.getInt(rowIdColumnIndex);
                final String name = cursor.getString(nameColumnIndex);

                renameTag(id, name);
            }
        };

        /** Single instance of the remove button click listener. */
        final View.OnClickListener removeOnClickListener = new View.OnClickListener() {
            public void onClick(View view) {
                final int position = (Integer) view.getTag();
                final Cursor cursor = (Cursor) tags.getAdapter().getItem(position);

                final int id = cursor.getInt(rowIdColumnIndex);
                final String name = cursor.getString(nameColumnIndex);

                removeTag(id, name);
            }
        };

        private TagRowBinder(SyncTabApplication app) {
            currentTag = app.getCurrentTag();
        }

        public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
            if (idColumnIndex == -1) {
                // init column indexes
                idColumnIndex = cursor.getColumnIndex(TagsColumns.ID);
                rowIdColumnIndex = cursor.getColumnIndex(DbMetadata.ID);
                nameColumnIndex = cursor.getColumnIndex(TagsColumns.NAME);
            }

            // get the root element of current row
            final ViewGroup root = (ViewGroup) view.getParent();

            // --- setup remove button

            View remove = (View) root.getTag(R.id.remove);
            if (remove == null) {
                remove = root.findViewById(R.id.remove);
                remove.setOnClickListener(removeOnClickListener);
                root.setTag(R.id.remove, remove);
            }

            // if current tag then hide remove icon
            final String tagId = cursor.getString(idColumnIndex);
            if (currentTag != null && currentTag.equals(tagId)) {
                remove.setVisibility(View.INVISIBLE);
            }
            else {
                remove.setVisibility(View.VISIBLE);
            }

            // --- setup rename button

            View rename = (View) root.getTag(R.id.rename);
            if (rename == null) {
                rename = root.findViewById(R.id.rename);
                rename.setOnClickListener(renameOnClickListener);
                root.setTag(R.id.rename, rename);
            }

            // --- setup position

            int position = cursor.getPosition();

            remove.setTag(position);
            rename.setTag(position);

            return false;
        }
    }
}
