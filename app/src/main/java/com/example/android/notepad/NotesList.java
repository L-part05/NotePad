package com.example.android.notepad;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Displays a list of notes with search and color support.
 */
public class NotesList extends ListActivity {
    private static final String TAG = "NotesList";
    private String mCurrentSearchQuery = "";
    private AlertDialog mSearchDialog;
    private int mCurrentCategoryFilter = -1;

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_COLOR,
            NotePad.Notes.COLUMN_NAME_CATEGORY
    };

    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 2;
    private static final int COLUMN_INDEX_NOTE = 3;
    private static final int COLUMN_INDEX_COLOR = 4;
    private static final int COLUMN_INDEX_CATEGORY = 5;

    private static final int SEARCH_TYPE_TITLE = 0;
    private static final int SEARCH_TYPE_CONTENT = 1;
    private static final int SEARCH_TYPE_BOTH = 2;
    private int mCurrentSearchType = SEARCH_TYPE_TITLE;
    private int mCurrentCategory = NotePad.Notes.CATEGORY_PERSONAL;
    private Spinner mCategorySpinner;

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        // 设置添加按钮点击事件
        ImageButton fabAdd =(ImageButton) findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesList.this, NoteEditor.class);
                intent.setAction(Intent.ACTION_INSERT);
                intent.setData(getIntent().getData());
                startActivity(intent);
            }
        });

        // 设置搜索按钮点击事件
        ImageButton btnSearch =(ImageButton) findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchDialog();
            }
        });

        getListView().setOnCreateContextMenuListener(this);
        refreshNotesList();


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

    private void refreshNotesList() {
        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // 使用自定义适配器，而不是 SimpleCursorAdapter
        NotesAdapter adapter = new NotesAdapter(this, cursor);
        setListAdapter(adapter);
    }

    // 自定义适配器类
    private class NotesAdapter extends CursorAdapter {

        public NotesAdapter(Context context, Cursor c) {
            super(context, c, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.noteslist_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // 获取数据
            String title = cursor.getString(COLUMN_INDEX_TITLE);
            long timestamp = cursor.getLong(COLUMN_INDEX_MODIFICATION_DATE);
            String content = cursor.getString(COLUMN_INDEX_NOTE);
            int colorValue = cursor.getInt(COLUMN_INDEX_COLOR);
            int categoryValue = cursor.getInt(COLUMN_INDEX_CATEGORY);

            // 设置分类标签（最显眼）
            TextView categoryLabel = (TextView) view.findViewById(R.id.category_label);
            if (categoryLabel != null) {
                categoryLabel.setText(getCategoryText(categoryValue));
                categoryLabel.setBackgroundColor(getCategoryColor(categoryValue));
            }

            // 设置标题
            TextView titleView =(TextView) view.findViewById(android.R.id.text1);
            titleView.setText(title);

            // 设置时间戳
            TextView timestampView =(TextView) view.findViewById(R.id.timestamp);
            String formattedDate = mDateFormat.format(new Date(timestamp));
            timestampView.setText(formattedDate);

            // 设置内容预览
            TextView contentPreview =(TextView) view.findViewById(R.id.content_preview);
            if (content != null && content.length() > 100) {
                content = content.substring(0, 100) + "...";
            } else if (content == null) {
                content = "";
            }
            contentPreview.setText(content);

            // 设置颜色指示器
            View colorIndicator = view.findViewById(R.id.color_indicator);
            colorIndicator.setBackgroundColor(getColorForValue(colorValue));

            // 设置整个列表项的背景颜色
            int backgroundColor = getBackgroundColorForValue(colorValue);
            view.setBackgroundColor(backgroundColor);

            // 根据需要调整文字颜色，确保在深色背景上可见
            if (isDarkColor(backgroundColor)) {
                titleView.setTextColor(Color.WHITE);
                timestampView.setTextColor(Color.LTGRAY);
                contentPreview.setTextColor(Color.LTGRAY);
                // 分类标签使用白色文字
                if (categoryLabel != null) {
                    categoryLabel.setTextColor(Color.WHITE);
                }
            } else {
                titleView.setTextColor(Color.BLACK);
                timestampView.setTextColor(Color.DKGRAY);
                contentPreview.setTextColor(Color.DKGRAY);
                // 分类标签使用白色文字
                if (categoryLabel != null) {
                    categoryLabel.setTextColor(Color.WHITE);
                }
            }
        }
    }

    // 新增方法：根据颜色值返回背景颜色
    private int getBackgroundColorForValue(int colorValue) {
        switch (colorValue) {
            case NotePad.Notes.COLOR_RED:
                return 0xFFFFEBEE; // 浅红色
            case NotePad.Notes.COLOR_ORANGE:
                return 0xFFFFF3E0; // 浅橙色
            case NotePad.Notes.COLOR_YELLOW:
                return 0xFFFFFDE7; // 浅黄色
            case NotePad.Notes.COLOR_GREEN:
                return 0xFFE8F5E9; // 浅绿色
            case NotePad.Notes.COLOR_BLUE:
                return 0xFFE3F2FD; // 浅蓝色
            case NotePad.Notes.COLOR_PURPLE:
                return 0xFFF3E5F5; // 浅紫色
            default:
                return Color.WHITE; // 默认白色
        }
    }
    // 判断是否为深色
    private boolean isDarkColor(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private int getColorForValue(int colorValue) {
        switch (colorValue) {
            case NotePad.Notes.COLOR_RED:
                return 0xFFFF5252;
            case NotePad.Notes.COLOR_ORANGE:
                return 0xFFFF9800;
            case NotePad.Notes.COLOR_YELLOW:
                return 0xFFFFEB3B;
            case NotePad.Notes.COLOR_GREEN:
                return 0xFF4CAF50;
            case NotePad.Notes.COLOR_BLUE:
                return 0xFF2196F3;
            case NotePad.Notes.COLOR_PURPLE:
                return 0xFF9C27B0;
            default:
                return 0xFFF5F5F5;
        }
    }

    // 获取分类颜色
    private int getCategoryColor(int categoryValue) {
        switch (categoryValue) {
            case NotePad.Notes.CATEGORY_PERSONAL:
                return 0xFF2196F3; // 蓝色
            case NotePad.Notes.CATEGORY_WORK:
                return 0xFF4CAF50; // 绿色
            case NotePad.Notes.CATEGORY_STUDY:
                return 0xFFFF9800; // 橙色
            case NotePad.Notes.CATEGORY_IDEA:
                return 0xFF9C27B0; // 紫色
            case NotePad.Notes.CATEGORY_TODO:
                return 0xFFF44336; // 红色
            case NotePad.Notes.CATEGORY_OTHER:
                return 0xFF607D8B; // 蓝灰色
            default:
                return 0xFF2196F3; // 默认蓝色
        }
    }

    // 获取分类文本
    private String getCategoryText(int categoryValue) {
        switch (categoryValue) {
            case NotePad.Notes.CATEGORY_PERSONAL:
                return "个人";
            case NotePad.Notes.CATEGORY_WORK:
                return "工作";
            case NotePad.Notes.CATEGORY_STUDY:
                return "学习";
            case NotePad.Notes.CATEGORY_IDEA:
                return "想法";
            case NotePad.Notes.CATEGORY_TODO:
                return "待办事项";
            case NotePad.Notes.CATEGORY_OTHER:
                return "其他";
            default:
                return "个人";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        MenuItem pasteItem = menu.findItem(R.id.menu_paste);

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            pasteItem.setEnabled(true);
        } else {
            pasteItem.setEnabled(false);
        }

        final boolean haveItems = getListAdapter().getCount() > 0;
        if (haveItems) {
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
            MenuItem[] items = new MenuItem[1];
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NotesList.class), null, intent, 0, items);
            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_add) {
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_INSERT);
            intent.setData(getIntent().getData());
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_paste) {
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_PASTE);
            intent.setData(getIntent().getData());
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_search) {
            showSearchDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("搜索笔记");

        View dialogView = getLayoutInflater().inflate(R.layout.search_dialog, null);
        builder.setView(dialogView);

        final EditText searchEditText = (EditText) dialogView.findViewById(R.id.search_edit_text);
        final Spinner searchTypeSpinner = (Spinner) dialogView.findViewById(R.id.search_type_spinner);
        final Spinner categoryFilterSpinner = (Spinner) dialogView.findViewById(R.id.category_filter_spinner);

        // 设置搜索类型适配器
        ArrayAdapter<CharSequence> searchTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.search_types, android.R.layout.simple_spinner_item);
        searchTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchTypeSpinner.setAdapter(searchTypeAdapter);
        searchTypeSpinner.setSelection(mCurrentSearchType);

        // 设置分类过滤器适配器
        String[] categoryFilters = {
                "所有分类",
                "个人",
                "工作",
                "学习",
                "想法",
                "待办事项",
                "其他"
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                categoryFilters
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(categoryAdapter);
        categoryFilterSpinner.setSelection(mCurrentCategoryFilter + 1); // +1 因为0是"所有分类"

        if (!mCurrentSearchQuery.isEmpty()) {
            searchEditText.setText(mCurrentSearchQuery);
        }

        builder.setPositiveButton("搜索", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String query = searchEditText.getText().toString().trim();
                int searchType = searchTypeSpinner.getSelectedItemPosition();
                int categoryFilter = categoryFilterSpinner.getSelectedItemPosition() - 1; // -1 转换为分类常量

                // 调用新的三参数版本
                performSearch(query, searchType, categoryFilter);
            }
        });

        builder.setNegativeButton("取消", null);

        if (!mCurrentSearchQuery.isEmpty() || mCurrentCategoryFilter != -1) {
            builder.setNeutralButton("清除搜索", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    clearSearch();
                }
            });
        }

        mSearchDialog = builder.show();
    }

    // 更新为三参数的 performSearch 方法
    private void performSearch(String query, int searchType, int categoryFilter) {
        mCurrentSearchQuery = query;
        mCurrentSearchType = searchType;
        mCurrentCategoryFilter = categoryFilter;

        String selection = null;
        String[] selectionArgs = null;

        // 构建查询条件
        if (query.isEmpty() && categoryFilter == -1) {
            // 没有搜索条件，显示所有
            selection = null;
            selectionArgs = null;
            setTitle("笔记列表");
        } else {
            List<String> selectionParts = new ArrayList<>();
            List<String> args = new ArrayList<>();

            // 添加分类筛选条件
            if (categoryFilter != -1) {
                selectionParts.add(NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?");
                args.add(String.valueOf(categoryFilter));
            }

            // 添加文本搜索条件
            if (!query.isEmpty()) {
                String queryCondition = null;
                switch (searchType) {
                    case SEARCH_TYPE_TITLE:
                        queryCondition = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
                        args.add("%" + query + "%");
                        break;
                    case SEARCH_TYPE_CONTENT:
                        queryCondition = NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
                        args.add("%" + query + "%");
                        break;
                    case SEARCH_TYPE_BOTH:
                        queryCondition = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                                NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
                        args.add("%" + query + "%");
                        args.add("%" + query + "%");
                        break;
                    default:
                        queryCondition = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
                        args.add("%" + query + "%");
                }

                if (queryCondition != null) {
                    selectionParts.add("(" + queryCondition + ")");
                }
            }

            // 组合所有条件
            if (!selectionParts.isEmpty()) {
                selection = TextUtils.join(" AND ", selectionParts);
                selectionArgs = args.toArray(new String[0]);
            }

            // 设置标题
            StringBuilder titleBuilder = new StringBuilder("搜索");
            if (categoryFilter != -1) {
                titleBuilder.append(" - ");
                titleBuilder.append(getCategoryText(categoryFilter));
            }
            if (!query.isEmpty()) {
                titleBuilder.append(" - ");
                titleBuilder.append(getSearchTypeText(searchType));
                titleBuilder.append(": ");
                titleBuilder.append(query);
            }
            setTitle(titleBuilder.toString());
        }

        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        updateAdapter(cursor);

        if (!query.isEmpty() || categoryFilter != -1) {
            int count = cursor != null ? cursor.getCount() : 0;
            StringBuilder messageBuilder = new StringBuilder();
            if (categoryFilter != -1) {
                messageBuilder.append(getCategoryText(categoryFilter));
                messageBuilder.append(" ");
            }
            messageBuilder.append(count > 0 ? "找到 " + count + " 个笔记" : "没有找到匹配的笔记");
            Toast.makeText(this, messageBuilder.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getSearchTypeText(int searchType) {
        switch (searchType) {
            case SEARCH_TYPE_TITLE:
                return "标题";
            case SEARCH_TYPE_CONTENT:
                return "内容";
            case SEARCH_TYPE_BOTH:
                return "标题和内容";
            default:
                return "标题";
        }
    }

    private void clearSearch() {
        mCurrentSearchQuery = "";
        mCurrentSearchType = SEARCH_TYPE_TITLE;
        mCurrentCategoryFilter = -1;
        refreshNotesList();
        setTitle(getString(R.string.title_notes_list));
        Toast.makeText(this, "已清除搜索条件", Toast.LENGTH_SHORT).show();
    }

    private void updateAdapter(Cursor cursor) {
        // 不要强制转换为 SimpleCursorAdapter，因为我们现在使用的是 NotesAdapter
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        if (adapter != null) {
            adapter.changeCursor(cursor);
        } else {
            // 如果没有适配器，创建一个新的
            NotesAdapter newAdapter = new NotesAdapter(this, cursor);
            setListAdapter(newAdapter);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
        int id = item.getItemId();
        if (id == R.id.context_open) {
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_EDIT);
            intent.setData(noteUri);
            startActivity(intent);
            return true;
        } else if (id == R.id.context_copy) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), "Note", noteUri));
            }
            return true;
        } else if (id == R.id.context_delete) {
            getContentResolver().delete(noteUri, null, null);
            Toast.makeText(this, "笔记已删除", Toast.LENGTH_SHORT).show();
            refreshNotesList();
            return true;
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // 注意：position 包括了 header 和 footer 的位置
        // 我们需要减去 header 的数量
        int headerCount = l.getHeaderViewsCount();
        int actualPosition = position - headerCount;

        if (actualPosition >= 0) {
            Cursor cursor = (Cursor) getListAdapter().getItem(actualPosition);
            if (cursor != null) {
                long noteId = cursor.getLong(COLUMN_INDEX_ID);
                Uri uri = ContentUris.withAppendedId(getIntent().getData(), noteId);

                String action = getIntent().getAction();
                if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
                    setResult(RESULT_OK, new Intent().setData(uri));
                } else {
                    Intent intent = new Intent(this, NoteEditor.class);
                    intent.setAction(Intent.ACTION_EDIT);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 如果有搜索条件（包括关键词或分类筛选），使用搜索条件刷新列表
        boolean hasSearchQuery = mCurrentSearchQuery != null && !mCurrentSearchQuery.isEmpty();
        boolean hasCategoryFilter = mCurrentCategoryFilter != -1; // -1 表示没有分类筛选

        if (hasSearchQuery || hasCategoryFilter) {
            // 调用带分类筛选的搜索方法
            performSearch(mCurrentSearchQuery, mCurrentSearchType, mCurrentCategoryFilter);
        } else {
            // 没有搜索条件，刷新所有笔记
            refreshNotesList();
        }
    }
}