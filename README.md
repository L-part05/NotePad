NotePad-Android应用的介绍文档

一.初始应用的功能

1.新建笔记和编辑笔记

(1)在主界面点击右下方蓝色加号所示按钮，新建笔记并进入编辑界面

<img width="544" height="1014" alt="cc7ed48d7e53b500ab1f3d1eb7b40796" src="https://github.com/user-attachments/assets/73e323b4-962d-4162-b2b7-be70f9bbc0da" />



(2)进入笔记编辑界面后，可进行笔记编辑

<img width="569" height="1222" alt="7afe6c1ba27291886c7b5354dcaef10e" src="https://github.com/user-attachments/assets/8ad1c155-09f7-40bf-a2d4-a9bc934a340b" />


2.笔记列表

在进行笔记的新建和编辑后，在主界面中呈现笔记列表。

二.拓展基本功能

（一）.笔记条目增加时间戳显示

1.功能要求

每个新建笔记都会保存新建时间并显示；在修改笔记后更新为修改时间。

2.实现思路和技术实现

(1).数据库层实现:

I.修改数据库表结构设计，创建表时包含时间戳字段，即创建时间和修改时间，即将NotePadProvider中oncreate代码修改如下:

         public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "Creating database table...");
            String sql = "CREATE TABLE " + TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_COLOR + " INTEGER DEFAULT 0,"
                    + NotePad.Notes.COLUMN_NAME_CATEGORY + " INTEGER DEFAULT 0"
                    + ");";
            Log.d(TAG, "SQL: " + sql);
            db.execSQL(sql);
            Log.d(TAG, "Table created successfully with color column");
        }

II.插入笔记时设置时间戳,即在NotePadProvider中insert方法中添加如下代码:

        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
        }

        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);

(2).业务层实现:

I.更新笔记时设置修改时间,即在NoteEditor类中updatenote方法中添加下述代码:

 ContentValues values = new ContentValues();
 values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

(3).表示层实现:

I.查询时包含时间戳字段,即在NotesList类中在projection字符串数组变量中添加时间戳字段；

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,//添加时间戳字段
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_COLOR,//添加UI美化颜色
            NotePad.Notes.COLUMN_NAME_CATEGORY //添加笔记类型
    };

II.时间戳格式化显示，即在NoteList类中创建格式化显示时间戳对象；

  private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

III.在列表项中显示时间戳，先在NoteList类中添加如下代码:

 // 设置时间戳
 
    TextView timestampView =(TextView) view.findViewById(R.id.timestamp);
    String formattedDate = mDateFormat.format(new Date(timestamp));
    timestampView.setText(formattedDate);

再继续在notelist_item.xml布局文件中添加如下代码:

    <TextView
            android:id="@+id/timestamp"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?android:attr/textAppearanceSmall"
            android:textSize="12sp"
            android:textColor="#757575"
            android:paddingTop="2dp"
            android:singleLine="true" />

3.实现效果界面截图

在标题和内容当中显示最新修改时间:

<img width="544" height="1014" alt="658a36b15e596f8b78df623ea0765d95" src="https://github.com/user-attachments/assets/fffa88ec-1b45-4fdb-a111-535b6b496285" />


（二）.笔记查询功能（按标题、内容、标题和内容查询）

1.功能要求

点击搜索按钮，进行搜索界面。可以选择按标题、内容、标题和内容三种方式进行笔记模糊查询，并在下方显示出查询出笔记的数量，当选择清空查询内容时，便会在笔记列表中显示所有笔记。

2.实现思路和技术实现

(1).查询功能技术实现:

I.搜索对话框的构建与显示,即在NoteList类中新建showSearchDialog方法:

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

II.搜索执行逻辑,即在NoteList中新建performSearch方法:

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

III.清除搜索功能,即在NoteList类中新增clearSearch方法:

    private void clearSearch() {
        mCurrentSearchQuery = "";
        mCurrentSearchType = SEARCH_TYPE_TITLE;
        mCurrentCategoryFilter = -1;
        refreshNotesList();
        setTitle(getString(R.string.title_notes_list));
        Toast.makeText(this, "已清除搜索条件", Toast.LENGTH_SHORT).show();
    }

(2).查询页面展示，即新建选择查询条件页面search_dialog.xml:

   <?xml version="1.0" encoding="utf-8"?> 
   <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
       android:layout_width="match_parent"
       android:layout_height="wrap_content"
       android:orientation="vertical"
       android:padding="16dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="搜索笔记"
        android:textSize="18sp"
        android:textStyle="bold"
        android:paddingBottom="10dp" />

    <!-- 分类选择 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingBottom="10dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="分类:"
            android:textSize="14sp"
            android:layout_marginEnd="10dp" />

        <Spinner
            android:id="@+id/category_filter_spinner"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1" />

    </LinearLayout>

    <!-- Search Type Selection -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingBottom="10dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="搜索类型:"
            android:textSize="14sp"
            android:layout_marginEnd="10dp" />

        <Spinner
            android:id="@+id/search_type_spinner"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1" />

    </LinearLayout>

    <!-- Search Keyword Input -->
    <EditText
        android:id="@+id/search_edit_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="输入要搜索的关键词"
        android:maxLines="1"
        android:inputType="text" />

</LinearLayout>

3.实现效果界面截图

（1）.点击搜索按钮后可以选择按照类型或标题和内容搜索:

<img width="606" height="1379" alt="195305979bf1f1edb8f3015aa506c721" src="https://github.com/user-attachments/assets/f52cc1c6-a158-4e23-94c1-b41ecdb18d2d" />

(2).点击分类搜索后可以选择要搜索的分类:

<img width="621" height="1356" alt="4d9af0ac4561138b164b90ce01c15a8c" src="https://github.com/user-attachments/assets/a283cb2f-bce3-4e69-a75f-31561e4c7908" />

(3).点击搜索类型后可以选择标题搜索、内容搜索、标题和内容搜索

<img width="618" height="1332" alt="1b7218f09318cd2e470aadee5ec9afac" src="https://github.com/user-attachments/assets/92a87236-a394-491c-b384-89299e2d1505" />

(4).搜索成功后在最下方显示搜索出的笔记个数，同时将符合条件的笔记显示在主列表中

<img width="559" height="1131" alt="fbdb7999d9f954633b1eee2c75f0c5f5" src="https://github.com/user-attachments/assets/22b5f6bd-c44d-47c9-aaf1-b1dfe0767adb" />

(5).点击清除搜索内容后，会显示所有笔记在主列表中:

<img width="570" height="1163" alt="c38c1e5933cbb601d2968566538cef43" src="https://github.com/user-attachments/assets/aab7b8e9-24cb-4caa-8dbf-9f9ca3c53c2d" />


三.拓展附加功能

（一）.UI美化

1.功能要求

在新增笔记时，可以选择笔记的背景颜色，然后在笔记列表中显示笔记时会根据所选择的颜色为每个笔记添加背景颜色，同时美化原先暗淡的主题，还可以在每个笔记标题下预显示笔记内容。

2.实现思路和技术实现

(1). 自定义控件实现（LinedEditText）

I.自定义EditText控件定义,即创建LinedEditText，继承自 EditText，添加额外的绘制功能，并且使用静态内部类方式，仅在 NoteEditor 中使用，代码如下:


     package com.example.android.notepad;

     import android.content.Context;
     import android.graphics.Canvas;
     import android.graphics.Paint;
     import android.graphics.Rect;
     import android.util.AttributeSet;
     import android.widget.EditText;
     public class LinedEditText extends EditText {
      private Rect mRect;
      private Paint mPaint;

      public LinedEditText(Context context) {
        super(context);
        init();
      }

      public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
      }

      public LinedEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
      }

      private void init() {
        mRect = new Rect();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(0xFFE0E0E0); // Light gray lines
        mPaint.setStrokeWidth(1);
      }

      @Override
      protected void onDraw(Canvas canvas) {
        int lineCount = getLineCount();
        int lineHeight = getLineHeight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int viewHeight = getHeight();

        // Get the baseline for the first line
        getLineBounds(0, mRect);
        int baseline = mRect.top;

        // Draw horizontal lines for each line of text
        for (int i = 0; i < lineCount; i++) {
            int lineY = paddingTop + baseline + (i * lineHeight);
            // Stop drawing if we're beyond the visible area
            if (lineY > viewHeight - paddingBottom) {
                break;
            }
            canvas.drawLine(
                    getPaddingLeft(),
                    lineY + lineHeight - 4,
                    getWidth() - getPaddingRight(),
                    lineY + lineHeight - 4,
                    mPaint
            );
        }

        super.onDraw(canvas);
    }
}
                
(2).布局优化实现

I.笔记编辑界面布局优化,即修改note_editor.xml文件代码如下:

<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/root_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#FAFAFA">

    <!-- Custom Toolbar -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:background="#2196F3"
        android:gravity="center_vertical"
        android:paddingStart="16dp"
        android:paddingEnd="16dp">

        <TextView
            android:id="@+id/toolbar_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="编辑笔记"
            android:textColor="#FFFFFF"
            android:textSize="20sp"
            android:textStyle="bold" />
        <View
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_weight="1" />
        <TextView

            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="选择颜色"
            android:textColor="#FF5252"
            android:textSize="20sp"
            android:textStyle="bold" />

        <ImageButton
            android:id="@+id/color_picker_button"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="@android:color/transparent"
            android:src="@drawable/ic_menu_color"
            android:contentDescription="选择颜色"
          />

    </LinearLayout>

    <!-- 分类选择区域 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:background="#FFFFFF"
        android:layout_margin="16dp"
        android:padding="16dp"
        android:layout_marginTop="8dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="分类"
            android:textColor="#666666"
            android:textSize="14sp"
            android:paddingBottom="8dp"
            android:layout_marginEnd="16dp" />

        <Spinner
            android:id="@+id/category_spinner"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1" />

    </LinearLayout>

    <!-- Title Card -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="#FFFFFF"
        android:layout_margin="16dp"
        android:padding="16dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="标题"
            android:textColor="#666666"
            android:textSize="14sp"
            android:paddingBottom="8dp" />

        <EditText
            android:id="@+id/title_edit"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="输入笔记标题..."
            android:textSize="18sp"
            android:textStyle="bold"
            android:background="@null"
            android:padding="8dp"
            android:maxLines="1"
            android:singleLine="true"
            android:textColor="#212121" />

    </LinearLayout>

    <!-- Content Card -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:orientation="vertical"
        android:background="#FFFFFF"
        android:layout_marginStart="16dp"
        android:layout_marginEnd="16dp"
        android:layout_marginBottom="16dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="内容"
            android:textColor="#666666"
            android:textSize="14sp"
            android:padding="16dp"
            android:paddingBottom="8dp" />

        <ScrollView
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:padding="8dp"
            android:fillViewport="true">

            <com.example.android.notepad.LinedEditText
                android:id="@+id/note"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="@null"
                android:padding="16dp"
                android:scrollbars="vertical"
                android:fadingEdge="vertical"
                android:gravity="top"
                android:textSize="16sp"
                android:lineSpacingExtra="6dp"
                android:minHeight="400dp"
                android:textColor="#212121"
                android:hint="开始输入您的笔记内容..."
                android:capitalize="sentences" />

        </ScrollView>

    </LinearLayout>

</LinearLayout>

II.笔记列表项布局优化，即修改notes_list.xml文件代码如下；

<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- 顶部工具栏 -->
    <LinearLayout
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:background="#2196F3"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="16dp"
        android:paddingEnd="16dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="笔记列表"
            android:textColor="#FFFFFF"
            android:textSize="20sp"
            android:textStyle="bold" />

        <ImageButton
            android:id="@+id/btn_search"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="@android:color/transparent"
            android:src="@android:drawable/ic_menu_search"
            android:contentDescription="搜索"
            android:layout_marginStart="8dp" />

    </LinearLayout>

    <!-- 列表内容 -->
    <ListView
        android:id="@android:id/list"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@id/toolbar"
        android:layout_above="@+id/fab_add"
        android:drawSelectorOnTop="false" />

    <TextView
        android:id="@android:id/empty"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@id/toolbar"
        android:gravity="center"
        android:text="暂无笔记\n点击右下角按钮创建新笔记"
        android:textSize="18sp"
        android:visibility="gone" />

    <!-- 添加按钮 -->
    <ImageButton
        android:id="@+id/fab_add"
        android:layout_width="56dp"
        android:layout_height="56dp"
        android:layout_alignParentBottom="true"
        android:layout_alignParentEnd="true"
        android:layout_margin="16dp"
        android:background="@drawable/fab_background"
        android:src="@android:drawable/ic_input_add"
        android:contentDescription="添加笔记"
        android:elevation="8dp" />

</RelativeLayout>

(3). 主题实现

I.主题设置,使用 Holo Light 主题和对话框主题，使得背景颜色更加明亮，同时活动显示为对话框形式
  
   <activity 
    android:name="NoteEditor"
    android:theme="@android:style/Theme.Holo.Light"
    android:screenOrientation="sensor"
    android:configChanges="keyboardHidden|orientation">

   <activity 
    android:name="TitleEditor"
    android:label="@string/title_edit_title"
    android:icon="@drawable/ic_menu_edit"
    android:theme="@android:style/Theme.Holo.Dialog"
    android:windowSoftInputMode="stateVisible">

3.实现效果界面截图

(1).笔记列表中会根据笔记选择的颜色修改对应的背景颜色，同时笔记项不仅能显示标题，还能显示类型和内容:

<img width="544" height="1014" alt="cc7ed48d7e53b500ab1f3d1eb7b40796" src="https://github.com/user-attachments/assets/b4c15a6a-270e-48f3-b2d3-fa7c08c7c754" />


(2).在新建笔记时，点击选择颜色可以选择笔记背景颜色

<img width="569" height="1222" alt="7afe6c1ba27291886c7b5354dcaef10e" src="https://github.com/user-attachments/assets/b69af5e8-80c0-4894-832c-9f0f2702a73f" />

<img width="595" height="1377" alt="245954bce1e496831af0f013d0441f94" src="https://github.com/user-attachments/assets/9290b824-ca14-408f-b621-98d4175282e3" />

（二）.笔记类型

1.功能要求

在新增笔记时可以选择笔记类型，同时在查询笔记时也可以通过笔记类型查询，更好地进行笔记管理

2.实现思路和技术实现

(1).数据库层实现:

I.数据库和契约类（NotePad）中新增类型字段定义和分类常量:

  // 新增分类字段
  
          public static final String COLUMN_NAME_CATEGORY = "category";
          
  // 分类常量
  
          public static final int CATEGORY_PERSONAL = 0;   // 个人
          public static final int CATEGORY_WORK = 1;       // 工作
          public static final int CATEGORY_STUDY = 2;      // 学习
          public static final int CATEGORY_IDEA = 3;       // 想法
          public static final int CATEGORY_TODO = 4;       // 待办事项
          public static final int CATEGORY_OTHER = 5;      // 其他

II.数据库提供者（NotePadProvider）中的oncreate方法中添加类型字段处理:

       public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "Creating database table...");
            String sql = "CREATE TABLE " + TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_COLOR + " INTEGER DEFAULT 0,"
                    + NotePad.Notes.COLUMN_NAME_CATEGORY + " INTEGER DEFAULT 0" //类型字段处理
                    + ");";
            Log.d(TAG, "SQL: " + sql);
            db.execSQL(sql);
            Log.d(TAG, "Table created successfully with color column");
        }
        
(2).编辑界面类型选择实现:

I.布局中添加类型选择控件:
<!-- note_editor_with_type.xml -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingBottom="10dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="类型:"
        android:textSize="16sp"
        android:textStyle="bold"
        android:layout_marginEnd="10dp" />

    <Spinner
        android:id="@+id/note_type"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1" />

</LinearLayout>

II.类型下拉列表初始化:

// NoteEditor.java - 设置类型下拉列表

    private void setupTypeSpinner() {
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.note_types, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mTypeSpinner.setAdapter(adapter);
   }

III.类型数据的保存和加载:

// NoteEditor.java - 加载已有笔记的类型

@Override
protected void onResume() {
    super.onResume();
    
    if (mCursor != null) {
        mCursor.requery();
        mCursor.moveToFirst();
        
        if (mState == STATE_EDIT) {
            // 设置类型
            int colTypeIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TYPE);
            String type = mCursor.getString(colTypeIndex);
            setSpinnerSelection(mTypeSpinner, type);
        } else if (mState == STATE_INSERT) {
            // 新建笔记时默认选择第一个类型
            mTypeSpinner.setSelection(0);
        }
    }
}

// NoteEditor.java - 保存笔记时保存类型

    private final void updateNote(String text, String title, String type) {
    ContentValues values = new ContentValues();
    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
    values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
    values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);
    values.put(NotePad.Notes.COLUMN_NAME_TYPE, type);  
    
    // 更新数据库
    getContentResolver().update(mUri, values, null, null);
}

(3).类型数据资源定义

I.类型数组资源:

  <!-- res/values/arrays.xml -->
  
    <string-array name="note_types">
        <item>默认</item>
        <item>工作</item>
        <item>学习</item>
        <item>生活</item>
        <item>个人</item>
        <item>重要</item>
        <item>临时</item>
    </string-array>
    
3.实现效果界面截图

在笔记列表中可以显示出笔记类型，同时在新建笔记时可以选择笔记所属类型:

<img width="544" height="1014" alt="5750acc686ddb1b4ff88e4f98c5e42ea" src="https://github.com/user-attachments/assets/f299104a-7c5e-4d39-84e5-71f6448444e6" />

<img width="576" height="1337" alt="74e15ea71ebffbb418fe042546b9099f" src="https://github.com/user-attachments/assets/d040d2bf-6263-4513-9d73-742a4faa8090" />




