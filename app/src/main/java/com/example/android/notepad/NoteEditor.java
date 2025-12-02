package com.example.android.notepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

/**
 * Activity for editing and creating notes with color support.
 */
public class NoteEditor extends Activity {
    private static final String TAG = "NoteEditor";

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_COLOR,
            NotePad.Notes.COLUMN_NAME_CATEGORY
    };

    private static final String ORIGINAL_CONTENT = "origContent";

    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private EditText mTitleEdit;
    private String mOriginalContent;

    private int mCurrentColor = NotePad.Notes.COLOR_DEFAULT;
    private int mCurrentCategory = NotePad.Notes.CATEGORY_PERSONAL;
    private ImageButton mColorPickerButton;
    private Spinner mCategorySpinner;
    private AlertDialog mColorDialog;

    private static final int[] COLOR_VALUES = {
            NotePad.Notes.COLOR_DEFAULT,
            NotePad.Notes.COLOR_RED,
            NotePad.Notes.COLOR_ORANGE,
            NotePad.Notes.COLOR_YELLOW,
            NotePad.Notes.COLOR_GREEN,
            NotePad.Notes.COLOR_BLUE,
            NotePad.Notes.COLOR_PURPLE
    };

    private static final int[] COLOR_BACKGROUNDS = {
            0xFFFAFAFA, // Default
            0xFFFFEBEE, // Red
            0xFFFFF3E0, // Orange
            0xFFFFFDE7, // Yellow
            0xFFE8F5E9, // Green
            0xFFE3F2FD, // Blue
            0xFFF3E5F5  // Purple
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {
            mState = STATE_EDIT;
            mUri = intent.getData();
        } else if (Intent.ACTION_INSERT.equals(action) || Intent.ACTION_PASTE.equals(action)) {
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);
            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
                finish();
                return;
            }
            setResult(RESULT_OK, new Intent().setData(mUri));
        } else {
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        mCursor = managedQuery(mUri, PROJECTION, null, null, null);

        if (Intent.ACTION_PASTE.equals(action)) {
            performPaste();
            mState = STATE_EDIT;
        }

        setContentView(R.layout.note_editor);

        mText = (EditText) findViewById(R.id.note);
        mTitleEdit = (EditText) findViewById(R.id.title_edit);
        mColorPickerButton = (ImageButton) findViewById(R.id.color_picker_button);

        mCategorySpinner = (Spinner) findViewById(R.id.category_spinner);

        mColorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog();
            }
        });
        // 设置分类选择器
        setupCategorySpinner();
        // 初始化图标颜色
        updateColorButtonIcon();

        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }

    private void setupCategorySpinner() {
        // 创建分类数组
        String[] categories = {
                "个人",
                "工作",
                "学习",
                "想法",
                "待办事项",
                "其他"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCategorySpinner.setAdapter(adapter);

        mCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentCategory = position; // 位置对应分类常量
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCurrentCategory = NotePad.Notes.CATEGORY_PERSONAL;
            }
        });
    }

    private void showColorPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择笔记颜色");

        View dialogView = getLayoutInflater().inflate(R.layout.color_picker_dialogs, null);
        builder.setView(dialogView);

        setupColorButton(dialogView, R.id.color_default, NotePad.Notes.COLOR_DEFAULT);
        setupColorButton(dialogView, R.id.color_red, NotePad.Notes.COLOR_RED);
        setupColorButton(dialogView, R.id.color_orange, NotePad.Notes.COLOR_ORANGE);
        setupColorButton(dialogView, R.id.color_yellow, NotePad.Notes.COLOR_YELLOW);
        setupColorButton(dialogView, R.id.color_green, NotePad.Notes.COLOR_GREEN);
        setupColorButton(dialogView, R.id.color_blue, NotePad.Notes.COLOR_BLUE);
        setupColorButton(dialogView, R.id.color_purple, NotePad.Notes.COLOR_PURPLE);

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        mColorDialog = builder.show();
    }

    private void setupColorButton(View dialogView, int buttonId, final int color) {
        Button button = (Button) dialogView.findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentColor = color;
                updateBackgroundColor();
                updateColorButtonIcon();
                if (mColorDialog != null) {
                    mColorDialog.dismiss();
                }
            }
        });
    }

    private int getColorIconColor(int colorValue) {
        switch (colorValue) {
            case NotePad.Notes.COLOR_RED:
                return 0xFFFF5252; // 红色
            case NotePad.Notes.COLOR_ORANGE:
                return 0xFFFF9800; // 橙色
            case NotePad.Notes.COLOR_YELLOW:
                return 0xFFFFEB3B; // 黄色
            case NotePad.Notes.COLOR_GREEN:
                return 0xFF4CAF50; // 绿色
            case NotePad.Notes.COLOR_BLUE:
                return 0xFF2196F3; // 蓝色
            case NotePad.Notes.COLOR_PURPLE:
                return 0xFF9C27B0; // 紫色
            default:
                return 0xFF9E9E9E; // 默认灰色
        }
    }
    private void updateColorButtonIcon() {
        // API 21+ 使用 setColorFilter
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // 使用 Tint 方式
            mColorPickerButton.setImageTintList(
                    ColorStateList.valueOf(getColorIconColor(mCurrentColor))
            );
            mColorPickerButton.setImageTintMode(PorterDuff.Mode.SRC_IN);
        } else {
            // 旧版本使用 colorFilter
            mColorPickerButton.setColorFilter(getColorIconColor(mCurrentColor), PorterDuff.Mode.SRC_IN);
        }
    }

    private void updateBackgroundColor() {
        int backgroundColor = getBackgroundColor(mCurrentColor);
        View rootView = findViewById(android.R.id.content);
        rootView.setBackgroundColor(backgroundColor);
    }

    private int getBackgroundColor(int colorValue) {
        for (int i = 0; i < COLOR_VALUES.length; i++) {
            if (COLOR_VALUES[i] == colorValue) {
                return COLOR_BACKGROUNDS[i];
            }
        }
        return COLOR_BACKGROUNDS[0];
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (mCursor != null) {
            mCursor.requery();
            mCursor.moveToFirst();

            if (mState == STATE_EDIT) {
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            int colColorIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_COLOR);
            if (colColorIndex != -1) {
                mCurrentColor = mCursor.getInt(colColorIndex);
                updateBackgroundColor();
                updateColorButtonIcon();
            }

            // 加载分类
            int colCategoryIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CATEGORY);
            if (colCategoryIndex != -1) {
                mCurrentCategory = mCursor.getInt(colCategoryIndex);
                if (mCategorySpinner != null) {
                    mCategorySpinner.setSelection(mCurrentCategory);
                }
            }

            int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);

            String title = mCursor.getString(colTitleIndex);
            String note = mCursor.getString(colNoteIndex);

            mTitleEdit.setText(title);
            mText.setTextKeepState(note);

            if (mOriginalContent == null) {
                mOriginalContent = note;
            }
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null) {
            String text = mText.getText().toString();
            String title = mTitleEdit.getText().toString();
            int length = text.length();

            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();
            } else if (mState == STATE_EDIT) {
                updateNote(text, title);
            } else if (mState == STATE_INSERT) {
                updateNote(text, title);
                mState = STATE_EDIT;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        if (mState == STATE_EDIT) {
            Intent intent = new Intent(null, mUri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mCursor != null) {
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String savedNote = mCursor.getString(colNoteIndex);
            String currentNote = mText.getText().toString();
            if (savedNote != null && savedNote.equals(currentNote)) {
                menu.findItem(R.id.menu_revert).setVisible(false);
            } else {
                menu.findItem(R.id.menu_revert).setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_save) {
            String text = mText.getText().toString();
            String title = mTitleEdit.getText().toString();
            updateNote(text, title);
            finish();
            return true;
        } else if (id == R.id.menu_delete) {
            deleteNote();
            finish();
            return true;
        } else if (id == R.id.menu_revert) {
            cancelNote();
            return true;
        }
        return false;
    }

    private void performPaste() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ContentResolver cr = getContentResolver();

        if (clipboard != null) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null) {
                String text = null;
                String title = null;
                ClipData.Item item = clip.getItemAt(0);
                Uri uri = item.getUri();

                if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {
                    Cursor orig = cr.query(uri, PROJECTION, null, null, null);
                    if (orig != null) {
                        if (orig.moveToFirst()) {
                            int colNoteIndex = orig.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                            int colTitleIndex = orig.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                            text = orig.getString(colNoteIndex);
                            title = orig.getString(colTitleIndex);
                        }
                        orig.close();
                    }
                }

                if (text == null) {
                    text = item.coerceToText(this).toString();
                }

                updateNote(text, title);
            }
        }
    }

    private void updateNote(String text, String title) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        values.put(NotePad.Notes.COLUMN_NAME_COLOR, mCurrentColor);
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, mCurrentCategory);

        if (mState == STATE_INSERT) {
            if (TextUtils.isEmpty(title)) {
                int length = text.length();
                title = text.substring(0, Math.min(30, length));
                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        } else if (!TextUtils.isEmpty(title)) {
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        }

        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

        getContentResolver().update(mUri, values, null, null);
    }

    private void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }
}