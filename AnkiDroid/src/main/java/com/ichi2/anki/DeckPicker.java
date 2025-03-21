/****************************************************************************************
 * Copyright (c) 2009 Andrew Dubya <andrewdubya@gmail.com>                              *
 * Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>                           *
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>                                   *
 * Copyright (c) 2009 Daniel Svard <daniel.svard@gmail.com>                             *
 * Copyright (c) 2010 Norbert Nagold <norbert.nagold@gmail.com>                         *
 * Copyright (c) 2014 Timothy Rae <perceptualchaos2@gmail.com>
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.SQLException;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import com.afollestad.materialdialogs.GravityEnum;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.anki.CollectionHelper.CollectionIntegrityStorageCheck;
import com.ichi2.anki.InitialActivity.StartupFailure;
import com.ichi2.anki.StudyOptionsFragment.StudyOptionsListener;
import com.ichi2.anki.analytics.UsageAnalytics;
import com.ichi2.anki.dialogs.AsyncDialogFragment;
import com.ichi2.anki.dialogs.ConfirmationDialog;
import com.ichi2.anki.dialogs.CreateDeckDialog;
import com.ichi2.anki.dialogs.DeckPickerNoSpaceToDowngradeDialog;
import com.ichi2.anki.dialogs.DeckPickerNoSpaceToDowngradeDialog.FileSizeFormatter;
import com.ichi2.anki.dialogs.ImportFileSelectionFragment;
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog;
import com.ichi2.anki.dialogs.DatabaseErrorDialog;
import com.ichi2.anki.dialogs.DeckPickerAnalyticsOptInDialog;
import com.ichi2.anki.dialogs.DeckPickerBackupNoSpaceLeftDialog;
import com.ichi2.anki.dialogs.DeckPickerConfirmDeleteDeckDialog;
import com.ichi2.anki.dialogs.DeckPickerContextMenu;
import com.ichi2.anki.dialogs.DeckPickerNoSpaceLeftDialog;
import com.ichi2.anki.dialogs.DialogHandler;
import com.ichi2.anki.dialogs.ImportDialog;
import com.ichi2.anki.dialogs.MediaCheckDialog;
import com.ichi2.anki.dialogs.SyncErrorDialog;
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialogFactory;
import com.ichi2.anki.exception.ConfirmModSchemaException;
import com.ichi2.anki.export.ActivityExportingDelegate;
import com.ichi2.anki.receiver.SdCardReceiver;
import com.ichi2.anki.servicelayer.DeckService;
import com.ichi2.anki.servicelayer.SchedulerService;
import com.ichi2.anki.servicelayer.UndoService;
import com.ichi2.anki.stats.AnkiStatsTaskHandler;
import com.ichi2.anki.web.HostNumFactory;
import com.ichi2.anki.widgets.DeckAdapter;
import com.ichi2.async.Cancellable;
import com.ichi2.async.Connection;
import com.ichi2.async.Connection.Payload;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskListener;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.async.TaskManager;
import com.ichi2.compat.CompatHelper;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Model;
import com.ichi2.libanki.ModelManager;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.backend.exception.DeckRenameException;
import com.ichi2.libanki.importer.AnkiPackageImporter;
import com.ichi2.libanki.sched.AbstractDeckTreeNode;
import com.ichi2.libanki.sched.TreeNode;
import com.ichi2.libanki.sync.CustomSyncServerUrlException;
import com.ichi2.libanki.sync.Syncer;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.ui.BadgeDrawableBuilder;
import com.ichi2.utils.AdaptionUtil;
import com.ichi2.utils.ImportUtils;
import com.ichi2.utils.Computation;
import com.ichi2.utils.KotlinCleanup;
import com.ichi2.utils.Permissions;
import com.ichi2.utils.SyncStatus;
import com.ichi2.utils.Triple;
import com.ichi2.utils.VersionUtils;
import com.ichi2.widget.WidgetStatus;

import com.ichi2.utils.JSONException;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import kotlin.Unit;
import timber.log.Timber;

import static com.ichi2.async.Connection.ConflictResolution.FULL_DOWNLOAD;

import static com.ichi2.anim.ActivityTransitionAnimation.Direction.*;


public class DeckPicker extends NavigationDrawerActivity implements
        StudyOptionsListener, SyncErrorDialog.SyncErrorDialogListener, ImportDialog.ImportDialogListener,
        MediaCheckDialog.MediaCheckDialogListener, ActivityCompat.OnRequestPermissionsResultCallback,
        CustomStudyDialog.CustomStudyListener{


    /**
     * Result codes from other activities
     */
    public static final int RESULT_MEDIA_EJECTED = 202;
    public static final int RESULT_DB_ERROR = 203;

    protected static final String UPGRADE_VERSION_KEY = "lastUpgradeVersion";

    /**
     * Available options performed by other activities (request codes for onActivityResult())
     */
    @VisibleForTesting
    static final int REQUEST_STORAGE_PERMISSION = 0;
    private static final int REQUEST_PATH_UPDATE = 1;
    public static final int REPORT_FEEDBACK = 4;
    private static final int LOG_IN_FOR_SYNC = 6;
    private static final int SHOW_INFO_NEW_VERSION = 9;
    public static final int SHOW_STUDYOPTIONS = 11;
    private static final int ADD_NOTE = 12;
    public static final int PICK_APKG_FILE = 13;
    private static final int PICK_EXPORT_FILE = 14;

    // For automatic syncing
    // 10 minutes in milliseconds.
    public static final long AUTOMATIC_SYNC_MIN_INTERVAL = 600000;

    private static final int SWIPE_TO_SYNC_TRIGGER_DISTANCE = 400;

    // Short animation duration from system
    private int mShortAnimDuration;

    private boolean mBackButtonPressedToExit = false;

    private RelativeLayout mDeckPickerContent;

    private MaterialDialog mProgressDialog;
    private View mStudyoptionsFrame;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mRecyclerViewLayoutManager;
    private DeckAdapter mDeckListAdapter;

    private final Snackbar.Callback mSnackbarShowHideCallback = new Snackbar.Callback();

    private ActivityExportingDelegate mExportingDelegate;

    private LinearLayout mNoDecksPlaceholder;

    private SwipeRefreshLayout mPullToSyncWrapper;

    private TextView mReviewSummaryTextView;

    private BroadcastReceiver mUnmountReceiver = null;

    private EditText mDialogEditText;

    private DeckPickerFloatingActionMenu mFloatingActionMenu;

    // flag asking user to do a full sync which is used in upgrade path
    private boolean mRecommendFullSync = false;

    // flag keeping track of when the app has been paused
    private boolean mActivityPaused = false;

    // Flag to keep track of startup error
    private boolean mStartupError = false;

    private String mExportFileName;

    @Nullable private Cancellable mEmptyCardTask = null;

    @VisibleForTesting
    public List<TreeNode<AbstractDeckTreeNode>> mDueTree;

    /**
     * Flag to indicate whether the activity will perform a sync in its onResume.
     * Since syncing closes the database, this flag allows us to avoid doing any
     * work in onResume that might use the database and go straight to syncing.
     */
    private boolean mSyncOnResume = false;

    /**
     * Keep track of which deck was last given focus in the deck list. If we find that this value
     * has changed between deck list refreshes, we need to recenter the deck list to the new current
     * deck.
     */
    private long mFocusedDeck;

    /** If we have accepted the "We will show you permissions" dialog, don't show it again on activity rebirth */
    private boolean mClosedWelcomeMessage;

    private SearchView mToolbarSearchView;

    private CustomStudyDialogFactory mCustomStudyDialogFactory;
    private DeckPickerContextMenu.Factory mContextMenuFactory;

    // ----------------------------------------------------------------------------
    // LISTENERS
    // ----------------------------------------------------------------------------

    private final OnClickListener mDeckExpanderClickListener = view -> {
        Long did = (Long) view.getTag();
        if (!getCol().getDecks().children(did).isEmpty()) {
            getCol().getDecks().collapse(did);
            __renderPage();
            dismissAllDialogFragments();
        }
    };

    private final OnClickListener mDeckClickListener = v -> onDeckClick(v, DeckSelectionType.DEFAULT);

    private final OnClickListener mCountsClickListener = v -> onDeckClick(v, DeckSelectionType.SHOW_STUDY_OPTIONS);


    private void onDeckClick(View v, DeckSelectionType selectionType) {
        long deckId = (long) v.getTag();
        Timber.i("DeckPicker:: Selected deck with id %d", deckId);

        boolean collectionIsOpen = false;
        try {
            collectionIsOpen = colIsOpen();
            handleDeckSelection(deckId, selectionType);
            if (mFragmented) {
                // Calling notifyDataSetChanged() will update the color of the selected deck.
                // This interferes with the ripple effect, so we don't do it if lollipop and not tablet view
                mDeckListAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            // Maybe later don't report if collectionIsOpen is false?
            Timber.w(e);
            String info = deckId + " colOpen:" + collectionIsOpen;
            AnkiDroidApp.sendExceptionReport(e, "deckPicker::onDeckClick", info);
            displayFailedToOpenDeck(deckId);
        }
    }


    private void displayFailedToOpenDeck(long deckId) {
        // #6208 - if the click is accepted before the sync completes, we get a failure.
        // We use the Deck ID as the deck likely doesn't exist any more.
        String message = getString(R.string.deck_picker_failed_deck_load, Long.toString(deckId));
        UIUtils.showThemedToast(this, message, false);
        Timber.w(message);
    }


    private final View.OnLongClickListener mDeckLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            long deckId = (long) v.getTag();
            Timber.i("DeckPicker:: Long tapped on deck with id %d", deckId);
            showDialogFragment(mContextMenuFactory.newDeckPickerContextMenu(deckId));
            return true;
        }
    };

    public BackupManager getBackupManager() {
        return new BackupManager();
    }

    private final ImportAddListener mImportAddListener = new ImportAddListener(this);
    private static class ImportAddListener extends TaskListenerWithContext<DeckPicker, String, Triple<AnkiPackageImporter, Boolean, String>> {
        public ImportAddListener(DeckPicker deckPicker) {
            super(deckPicker);
        }

        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, Triple<AnkiPackageImporter, Boolean, String> result) {
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            // If boolean and string are both set, we are signalling an error message
            // instead of a successful result.
            if (result.second && result.third != null) {
                Timber.w("Import: Add Failed: %s", result.third);
                deckPicker.showSimpleMessageDialog(result.third);
            } else {
                Timber.i("Import: Add succeeded");
                AnkiPackageImporter imp = result.first;
                deckPicker.showSimpleMessageDialog(TextUtils.join("\n", imp.getLog()));
                deckPicker.updateDeckList();
            }
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            if (deckPicker.mProgressDialog == null || !deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker,
                        deckPicker.getResources().getString(R.string.import_title), null, false);
            }
        }


        @Override
        public void actualOnProgressUpdate(@NonNull DeckPicker deckPicker, String content) {
            deckPicker.mProgressDialog.setContent(content);
        }
    }

    private ImportReplaceListener importReplaceListener() {
        return new ImportReplaceListener(this);
    }
    private static class ImportReplaceListener extends TaskListenerWithContext<DeckPicker, String, Computation<?>>{
        public ImportReplaceListener(DeckPicker deckPicker) {
            super(deckPicker);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, Computation<?> result) {
            Timber.i("Import: Replace Task Completed");
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            Resources res = deckPicker.getResources();
            if (result.succeeded()) {
                deckPicker.updateDeckList();
            } else {
                deckPicker.showSimpleMessageDialog(res.getString(R.string.import_log_no_apkg), true);
            }
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            if (deckPicker.mProgressDialog == null || !deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker,
                        deckPicker.getResources().getString(R.string.import_title),
                        deckPicker.getResources().getString(R.string.import_replacing), false);
            }
        }


        @Override
        public void actualOnProgressUpdate(@NonNull DeckPicker deckPicker, String message) {
            deckPicker.mProgressDialog.setContent(message);
        }
    }


    // ----------------------------------------------------------------------------
    // ANDROID ACTIVITY METHODS
    // ----------------------------------------------------------------------------

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) throws SQLException {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return;
        }
        Timber.d("onCreate()");

        mExportingDelegate = new ActivityExportingDelegate(this, this::getCol);

        mCustomStudyDialogFactory = new CustomStudyDialogFactory(this::getCol, this).attachToActivity(this);
        mContextMenuFactory = new DeckPickerContextMenu.Factory(this::getCol).attachToActivity(this);

        //we need to restore here, as we need it before super.onCreate() is called.
        restoreWelcomeMessage(savedInstanceState);

        // Then set theme and content view
        super.onCreate(savedInstanceState);
        handleStartup();
        setContentView(R.layout.homescreen);
        View mainView = findViewById(android.R.id.content);

        // check, if tablet layout
        mStudyoptionsFrame = findViewById(R.id.studyoptions_fragment);
        // set protected variable from NavigationDrawerActivity
        mFragmented = mStudyoptionsFrame != null && mStudyoptionsFrame.getVisibility() == View.VISIBLE;

        // Open StudyOptionsFragment if in fragmented mode
        if (mFragmented && !mStartupError) {
            loadStudyOptionsFragment(false);
        }

        registerExternalStorageListener();

        // create inherited navigation drawer layout here so that it can be used by parent class
        initNavigationDrawer(mainView);
        setTitle(getResources().getString(R.string.app_name));

        mDeckPickerContent = findViewById(R.id.deck_picker_content);
        mRecyclerView = findViewById(R.id.files);
        mNoDecksPlaceholder = findViewById(R.id.no_decks_placeholder);

        mDeckPickerContent.setVisibility(View.GONE);
        mNoDecksPlaceholder.setVisibility(View.GONE);

        // specify a LinearLayoutManager and set up item dividers for the RecyclerView
        mRecyclerViewLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mRecyclerViewLayoutManager);
        TypedArray ta = this.obtainStyledAttributes(new int[] { R.attr.deckDivider });
        Drawable divider = ta.getDrawable(0);
        ta.recycle();
        DividerItemDecoration dividerDecorator = new DividerItemDecoration(this, mRecyclerViewLayoutManager.getOrientation());
        dividerDecorator.setDrawable(divider);
        mRecyclerView.addItemDecoration(dividerDecorator);

        // Add background to Deckpicker activity
        View view = mFragmented ? findViewById(R.id.deckpicker_xl_view) : findViewById(R.id.root_layout);

        boolean hasDeckPickerBackground = false;
        try {
            hasDeckPickerBackground = applyDeckPickerBackground(view);
        } catch (OutOfMemoryError e) { //6608 - OOM should be catchable here.
            Timber.w(e, "Failed to apply background - OOM");
            UIUtils.showThemedToast(this, getString(R.string.background_image_too_large), false);
        } catch (Exception e) {
            Timber.w(e, "Failed to apply background");
            UIUtils.showThemedToast(this, getString(R.string.failed_to_apply_background_image, e.getLocalizedMessage()), false);
        }

        // create and set an adapter for the RecyclerView
        mDeckListAdapter = new DeckAdapter(getLayoutInflater(), this);
        mDeckListAdapter.setDeckClickListener(mDeckClickListener);
        mDeckListAdapter.setCountsClickListener(mCountsClickListener);
        mDeckListAdapter.setDeckExpanderClickListener(mDeckExpanderClickListener);
        mDeckListAdapter.setDeckLongClickListener(mDeckLongClickListener);
        mDeckListAdapter.enablePartialTransparencyForBackground(hasDeckPickerBackground);
        mRecyclerView.setAdapter(mDeckListAdapter);

        mPullToSyncWrapper = findViewById(R.id.pull_to_sync_wrapper);
        mPullToSyncWrapper.setDistanceToTriggerSync(SWIPE_TO_SYNC_TRIGGER_DISTANCE);
        mPullToSyncWrapper.setOnRefreshListener(() -> {
            Timber.i("Pull to Sync: Syncing");
            mPullToSyncWrapper.setRefreshing(false);
            sync();
        });
        mPullToSyncWrapper.getViewTreeObserver().addOnScrollChangedListener(() ->
                mPullToSyncWrapper.setEnabled(mRecyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition() == 0));

        // Setup the FloatingActionButtons, should work everywhere with min API >= 15
        mFloatingActionMenu = new DeckPickerFloatingActionMenu(view, this);

        mReviewSummaryTextView = findViewById(R.id.today_stats_text_view);

        mShortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        new Onboarding.DeckPicker(this, mRecyclerViewLayoutManager).onCreate();
    }

    /**
     * The first call in showing dialogs for startup - error or success.
     * Attempts startup if storage permission has been acquired, else, it requests the permission
     * */
    public void handleStartup() {
        if (Permissions.hasStorageAccessPermission(this)) {
            StartupFailure failure = InitialActivity.getStartupFailureType(this);
            if (failure == null) {
                // Show any necessary dialogs (e.g. changelog, special messages, etc)
                SharedPreferences sharedPrefs = AnkiDroidApp.getSharedPrefs(this);
                showStartupScreensAndDialogs(sharedPrefs, 0);
                mStartupError = false;
            } else {
                // Show error dialogs
                handleStartupFailure(failure);
                mStartupError = true;
            }
        } else {
            requestStoragePermission();
        }
    }


    @VisibleForTesting
    void handleStartupFailure(StartupFailure failure) {
        switch (failure) {
            case SD_CARD_NOT_MOUNTED:
                Timber.i("SD card not mounted");
                onSdCardNotMounted();
                break;
            case DIRECTORY_NOT_ACCESSIBLE:
                Timber.i("AnkiDroid directory inaccessible");
                Intent i = Preferences.AdvancedSettingsFragment.getSubscreenIntent(this);
                startActivityForResultWithoutAnimation(i, REQUEST_PATH_UPDATE);
                UIUtils.showThemedToast(this, R.string.directory_inaccessible, false);
                break;
            case FUTURE_ANKIDROID_VERSION:
                Timber.i("Displaying database versioning");
                showDatabaseErrorDialog(DatabaseErrorDialog.INCOMPATIBLE_DB_VERSION);
                break;
            case DATABASE_LOCKED:
                Timber.i("Displaying database locked error");
                showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_DB_LOCKED);
                break;
            case DATABASE_DOWNGRADE_REQUIRED:
                // This has a callback to continue with handleStartup
                InitialActivity.downgradeBackend(this);
                break;
            case WEBVIEW_FAILED:
                new MaterialDialog.Builder(this)
                        .title(R.string.ankidroid_init_failed_webview_title)
                        .content(getString(R.string.ankidroid_init_failed_webview, AnkiDroidApp.getWebViewErrorMessage()))
                        .positiveText(R.string.close)
                        .onPositive((d, w) -> exit())
                        .cancelable(false)
                        .show();
                break;
            case DB_ERROR:
            default:
                displayDatabaseFailure();
        }
    }


    public void displayDatabaseFailure() {
        Timber.i("Displaying database error");
        showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_LOAD_FAILED);
    }


    // throws doesn't seem to be checked by the compiler - consider it to be documentation
    private boolean applyDeckPickerBackground(View view) throws OutOfMemoryError {
        //Allow the user to clear data and get back to a good state if they provide an invalid background.
        if (!AnkiDroidApp.getSharedPrefs(this).getBoolean("deckPickerBackground", false)) {
            Timber.d("No DeckPicker background preference");
            view.setBackgroundResource(0);
            return false;
        }
        String currentAnkiDroidDirectory = CollectionHelper.getCurrentAnkiDroidDirectory(this);
        File imgFile = new File(currentAnkiDroidDirectory, "DeckPickerBackground.png" );
        if (!imgFile.exists()) {
            Timber.d("No DeckPicker background image");
            view.setBackgroundResource(0);
            return false;
        } else {
            Timber.i("Applying background");
            Drawable drawable = Drawable.createFromPath(imgFile.getAbsolutePath());
            view.setBackground(drawable);
            return true;
        }
    }

    public void requestStoragePermission() {
        if (mClosedWelcomeMessage) {
            // DEFECT #5847: This fails if the activity is killed.
            //Even if the dialog is showing, we want to show it again.
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            Timber.i("Displaying initial permission request dialog");
            // Request storage permission if we don't have it (e.g. on Android 6.0+)
            new MaterialDialog.Builder(this)
                    .title(R.string.collection_load_welcome_request_permissions_title)
                    .titleGravity(GravityEnum.CENTER)
                    .content(R.string.collection_load_welcome_request_permissions_details)
                    .positiveText(R.string.dialog_ok)
                    .onPositive((innerDialog, innerWhich) -> {
                        this.mClosedWelcomeMessage = true;
                        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_STORAGE_PERMISSION);
                    })
                    .cancelable(false)
                    .canceledOnTouchOutside(false)
                    .show();
        }
    }

    /**
     * It can fail if an ancestor is a filtered deck.
     * @param deckName Create a deck with this name.
     * @return Whether creation succeeded.
     */
    protected boolean createNewDeck(String deckName) {
        Timber.i("DeckPicker:: Creating new deck...");
        try {
            getCol().getDecks().id(deckName);
        } catch (DeckRenameException ex) {
            UIUtils.showThemedToast(this, ex.getLocalizedMessage(getResources()), false);
            return false;
        }
        updateDeckList();
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Null check to prevent crash when col inaccessible
        // #9081: sync leaves the collection closed, thus colIsOpen() is insufficient, carefully open the collection if possible
        if (CollectionHelper.getInstance().getColSafe(this) == null) {
            return false;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.d("onCreateOptionsMenu()");
        mFloatingActionMenu.closeFloatingActionMenu();
        getMenuInflater().inflate(R.menu.deck_picker, menu);
        boolean sdCardAvailable = AnkiDroidApp.isSdCardMounted();
        menu.findItem(R.id.action_sync).setEnabled(sdCardAvailable);
        menu.findItem(R.id.action_new_filtered_deck).setEnabled(sdCardAvailable);
        menu.findItem(R.id.action_check_database).setEnabled(sdCardAvailable);
        menu.findItem(R.id.action_check_media).setEnabled(sdCardAvailable);
        menu.findItem(R.id.action_empty_cards).setEnabled(sdCardAvailable);

        MenuItem toolbarSearchItem = menu.findItem(R.id.deck_picker_action_filter);
        toolbarSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            // When SearchItem is expanded
            public boolean onMenuItemActionExpand(MenuItem item) {
                Timber.i("DeckPicker:: SearchItem opened");
                // Hide the floating action button if it is visible
                mFloatingActionMenu.hideFloatingActionButton();
                return true;
            }

            @Override
            // When SearchItem is collapsed
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Timber.i("DeckPicker:: SearchItem closed");
                // Show the floating action button if it is hidden
                mFloatingActionMenu.showFloatingActionButton();
                return true;
            }
        });

        mToolbarSearchView = (SearchView) toolbarSearchItem.getActionView();

        mToolbarSearchView.setQueryHint(getString(R.string.search_decks));
        mToolbarSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                Filterable adapter = (Filterable) mRecyclerView.getAdapter();
                adapter.getFilter().filter(newText);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                mToolbarSearchView.clearFocus();
                return true;
            }
        });

        if (colIsOpen()) {
            displaySyncBadge(menu);

            // Show / hide undo
            if (mFragmented || !getCol().undoAvailable()) {
                menu.findItem(R.id.action_undo).setVisible(false);
            } else {
                Resources res = getResources();
                menu.findItem(R.id.action_undo).setVisible(true);
                String undo = res.getString(R.string.studyoptions_congrats_undo, getCol().undoName(res));
                menu.findItem(R.id.action_undo).setTitle(undo);
            }

            // Remove the filter - not necessary and search has other implications for new users.
            menu.findItem(R.id.deck_picker_action_filter).setVisible(getCol().getDecks().count() >= 10);
        }


        return super.onCreateOptionsMenu(menu);
    }


    @VisibleForTesting
    protected void displaySyncBadge(Menu menu) {
        MenuItem syncMenu = menu.findItem(R.id.action_sync);
        SyncStatus syncStatus = SyncStatus.getSyncStatus(this::getCol);
        switch (syncStatus) {
            case BADGE_DISABLED:
            case NO_CHANGES:
            case INCONCLUSIVE:
                BadgeDrawableBuilder.removeBadge(syncMenu);
                syncMenu.setTitle(R.string.button_sync);
                break;
            case HAS_CHANGES:
                // Light orange icon
                new BadgeDrawableBuilder(getResources())
                        .withColor(ContextCompat.getColor(this, R.color.badge_warning))
                        .replaceBadge(syncMenu);
                syncMenu.setTitle(R.string.button_sync);
                break;
            case NO_ACCOUNT:
            case FULL_SYNC:
                if (syncStatus == SyncStatus.NO_ACCOUNT) {
                    syncMenu.setTitle(R.string.sync_menu_title_no_account);
                } else if (syncStatus == SyncStatus.FULL_SYNC) {
                    syncMenu.setTitle(R.string.sync_menu_title_full_sync);
                }
                // Orange-red icon with exclamation mark
                new BadgeDrawableBuilder(getResources())
                        .withText('!')
                        .withColor(ContextCompat.getColor(this, R.color.badge_error))
                        .replaceBadge(syncMenu);
                break;
            default:
                Timber.w("Unhandled sync status: %s", syncStatus);
                syncMenu.setTitle(R.string.sync_title);
                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mFloatingActionMenu.closeFloatingActionMenu();

        Resources res = getResources();
        if (getDrawerToggle().onOptionsItemSelected(item)) {
            return true;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.action_undo) {
            Timber.i("DeckPicker:: Undo button pressed");
            undo();
            return true;
        } else if (itemId == R.id.deck_picker_action_filter) {
            Timber.i("DeckPicker:: Search button pressed");
            return true;
        } else if (itemId == R.id.action_sync) {
            Timber.i("DeckPicker:: Sync button pressed");
            sync();
            return true;
        } else if (itemId == R.id.action_import) {
            Timber.i("DeckPicker:: Import button pressed");
            showDialogFragment(ImportFileSelectionFragment.createInstance(this));
            return true;
        } else if (itemId == R.id.action_new_filtered_deck) {
            CreateDeckDialog createFilteredDeckDialog = new CreateDeckDialog(DeckPicker.this, R.string.new_deck, CreateDeckDialog.DeckDialogType.FILTERED_DECK, null);
            createFilteredDeckDialog.setOnNewDeckCreated((id) -> {
                // a filtered deck was created
                openStudyOptions(true);
            });
            createFilteredDeckDialog.showFilteredDeckDialog();
            return true;
        } else if (itemId == R.id.action_check_database) {
            Timber.i("DeckPicker:: Check database button pressed");
            showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_CONFIRM_DATABASE_CHECK);
            return true;
        } else if (itemId == R.id.action_check_media) {
            Timber.i("DeckPicker:: Check media button pressed");
            showMediaCheckDialog(MediaCheckDialog.DIALOG_CONFIRM_MEDIA_CHECK);
            return true;
        } else if (itemId == R.id.action_empty_cards) {
            Timber.i("DeckPicker:: Empty cards button pressed");
            handleEmptyCards();
            return true;
        } else if (itemId == R.id.action_model_browser_open) {
            Timber.i("DeckPicker:: Model browser button pressed");
            Intent noteTypeBrowser = new Intent(this, ModelBrowser.class);
            startActivityForResultWithAnimation(noteTypeBrowser, 0, START);
            return true;
        } else if (itemId == R.id.action_restore_backup) {
            Timber.i("DeckPicker:: Restore from backup button pressed");
            showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_CONFIRM_RESTORE_BACKUP);
            return true;
        } else if (itemId == R.id.action_export) {
            Timber.i("DeckPicker:: Export collection button pressed");
            String msg = getResources().getString(R.string.confirm_apkg_export);
            mExportingDelegate.showExportDialog(msg);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_MEDIA_EJECTED) {
            onSdCardNotMounted();
            return;
        } else if (resultCode == RESULT_DB_ERROR) {
            handleDbError();
            return;
        }

        if (requestCode == SHOW_INFO_NEW_VERSION) {
            showStartupScreensAndDialogs(AnkiDroidApp.getSharedPrefs(getBaseContext()), 3);
        } else if (requestCode == LOG_IN_FOR_SYNC && resultCode == RESULT_OK) {
            mSyncOnResume = true;
        } else if (requestCode == REQUEST_REVIEW || requestCode == SHOW_STUDYOPTIONS) {
            if (resultCode == Reviewer.RESULT_NO_MORE_CARDS) {
                // Show a message when reviewing has finished
                if (getCol().getSched().count() == 0) {
                    UIUtils.showSimpleSnackbar(this, R.string.studyoptions_congrats_finished, false);
                } else {
                    UIUtils.showSimpleSnackbar(this, R.string.studyoptions_no_cards_due, false);
                }
            } else if (resultCode == Reviewer.RESULT_ABORT_AND_SYNC) {
                Timber.i("Obtained Abort and Sync result");
                TaskManager.waitForAllToFinish(4);
                sync();
            }
        } else if (requestCode == REQUEST_PATH_UPDATE) {
            // The collection path was inaccessible on startup so just close the activity and let user restart
            finishWithoutAnimation();
        } else if ((requestCode == PICK_APKG_FILE) && (resultCode == RESULT_OK)) {
            ImportUtils.ImportResult importResult = ImportUtils.handleFileImport(this, intent);
            if (!importResult.isSuccess()) {
                ImportUtils.showImportUnsuccessfulDialog(this, importResult.getHumanReadableMessage(), false);
            }
        }
    }


    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION && permissions.length == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                invalidateOptionsMenu();
                handleStartup();
            } else {
                // User denied access to file storage  so show error toast and display "App Info"
                UIUtils.showThemedToast(this, R.string.startup_no_storage_permission, false);
                finishWithoutAnimation();
                // Open the Android settings page for our app so that the user can grant the missing permission
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityWithoutAnimation(intent);
            }
        }
    }


    @Override
    protected void onResume() {
        Timber.d("onResume()");
        super.onResume();
        refreshState();
    }


    public void refreshState() {
        mActivityPaused = false;
        if (mSyncOnResume) {
            Timber.i("Performing Sync on Resume");
            sync();
            mSyncOnResume = false;
        } else if (colIsOpen()) {
            selectNavigationItem(R.id.nav_decks);
            if (mDueTree == null) {
                updateDeckList(true);
            }
            updateDeckList();
            setTitle(getResources().getString(R.string.app_name));
        }
        /* Complete task and enqueue fetching nonessential data for
          startup. */
        if (colIsOpen()) {
            TaskManager.launchCollectionTask(new CollectionTask.LoadCollectionComplete());
        }
        // Update sync status (if we've come back from a screen)
        supportInvalidateOptionsMenu();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("mClosedWelcomeMessage", mClosedWelcomeMessage);
        savedInstanceState.putBoolean("mIsFABOpen", mFloatingActionMenu.isFABOpen());
    }


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mFloatingActionMenu.setFABOpen(savedInstanceState.getBoolean("mIsFABOpen"));
    }


    @Override
    protected void onPause() {
        Timber.d("onPause()");
        mActivityPaused = true;
        // The deck count will be computed on resume. No need to compute it now
        TaskManager.cancelAllTasks(CollectionTask.LoadDeckCounts.class);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Timber.d("onStop()");
        super.onStop();
        if (colIsOpen()) {
            WidgetStatus.update(this);
            // Ignore the modification - a change in deck shouldn't trigger the icon for "pending changes".
            UIUtils.saveCollectionInBackground(true);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        Timber.d("onDestroy()");
    }

    private void automaticSync() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());

        // Check whether the option is selected, the user is signed in and last sync was AUTOMATIC_SYNC_TIME ago
        // (currently 10 minutes)
        String hkey = preferences.getString("hkey", "");
        long lastSyncTime = preferences.getLong("lastSyncTime", 0);
        if (hkey.length() != 0 && preferences.getBoolean("automaticSyncMode", false) &&
                Connection.isOnline() && getCol().getTime().intTimeMS() - lastSyncTime > AUTOMATIC_SYNC_MIN_INTERVAL) {
            Timber.i("Triggering Automatic Sync");
            sync();
        }
    }

    @Override
    @KotlinCleanup("once in Kotlin: use HandlerUtils.executeFunctionWithDelay")
    public void onBackPressed() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        if (isDrawerOpen()) {
            super.onBackPressed();
        } else {
            Timber.i("Back key pressed");
            if (mFloatingActionMenu.isFABOpen()) {
                mFloatingActionMenu.closeFloatingActionMenu();
            } else {
                if (!preferences.getBoolean("exitViaDoubleTapBack", false) || mBackButtonPressedToExit) {
                    automaticSync();
                    finishWithAnimation();
                } else {
                    UIUtils.showThemedToast(this, getString(R.string.back_pressed_once), true);
                }
                mBackButtonPressedToExit = true;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBackButtonPressedToExit = false;
                    }
                }, Consts.SHORT_TOAST_DURATION);
            }
        }
    }

    private void finishWithAnimation() {
        super.finishWithAnimation(DOWN);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mToolbarSearchView != null && mToolbarSearchView.hasFocus()) {
            Timber.d("Skipping keypress: search action bar is focused");
            return true;
        }

        switch(keyCode) {

            case KeyEvent.KEYCODE_A:
                Timber.i("Adding Note from keypress");
                addNote();
                break;

            case KeyEvent.KEYCODE_B:
                Timber.i("Open Browser from keypress");
                openCardBrowser();
                break;

            case KeyEvent.KEYCODE_Y:
                Timber.i("Sync from keypress");
                sync();
                break;

            case KeyEvent.KEYCODE_SLASH:
            case KeyEvent.KEYCODE_S:
                Timber.i("Study from keypress");
                handleDeckSelection(getCol().getDecks().selected(), DeckSelectionType.SKIP_STUDY_OPTIONS);
                break;
            default:
                break;
        }

        return super.onKeyUp(keyCode, event);
    }

    // ----------------------------------------------------------------------------
    // CUSTOM METHODS
    // ----------------------------------------------------------------------------

    private void restoreWelcomeMessage(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mClosedWelcomeMessage = savedInstanceState.getBoolean("mClosedWelcomeMessage");
    }

    /**
     * Perform the following tasks:
     * Automatic backup
     * loadStudyOptionsFragment() if tablet
     * Automatic sync
     */
    private void onFinishedStartup() {
        // create backup in background if needed
        BackupManager.performBackupInBackground(getCol().getPath(), getCol().getTime());

        // Force a full sync if flag was set in upgrade path, asking the user to confirm if necessary
        if (mRecommendFullSync) {
            mRecommendFullSync = false;
            try {
                getCol().modSchema();
            } catch (ConfirmModSchemaException e) {
                Timber.w("Forcing full sync");
                e.log();
                // If libanki determines it's necessary to confirm the full sync then show a confirmation dialog
                // We have to show the dialog via the DialogHandler since this method is called via an async task
                Resources res = getResources();
                Message handlerMessage = Message.obtain();
                handlerMessage.what = DialogHandler.MSG_SHOW_FORCE_FULL_SYNC_DIALOG;
                Bundle handlerMessageData = new Bundle();
                handlerMessageData.putString("message", res.getString(R.string.full_sync_confirmation_upgrade) +
                        "\n\n" + res.getString(R.string.full_sync_confirmation));
                handlerMessage.setData(handlerMessageData);
                getDialogHandler().sendMessage(handlerMessage);
            }
        }

        automaticSync();
    }

    private void showCollectionErrorDialog() {
        getDialogHandler().sendEmptyMessage(DialogHandler.MSG_SHOW_COLLECTION_LOADING_ERROR_DIALOG);
    }

    public void addNote() {
        Intent intent = new Intent(DeckPicker.this, NoteEditor.class);
        intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_DECKPICKER);
        startActivityForResultWithAnimation(intent, ADD_NOTE, START);
    }


    private void showStartupScreensAndDialogs(SharedPreferences preferences, int skip) {

        // For Android 8/8.1 we want to use software rendering by default or the Reviewer UI is broken #7369
        if (CompatHelper.getSdkVersion() == Build.VERSION_CODES.O ||
                CompatHelper.getSdkVersion() == Build.VERSION_CODES.O_MR1) {
            if (!preferences.contains("softwareRender")) {
                Timber.i("Android 8/8.1 detected with no render preference. Turning on software render.");
                preferences.edit().putBoolean("softwareRender", true).apply();
            } else {
                Timber.i("Android 8/8.1 detected, software render preference already exists.");
            }
        }

        if (!BackupManager.enoughDiscSpace(CollectionHelper.getCurrentAnkiDroidDirectory(this))) {
            Timber.i("Not enough space to do backup");
            showDialogFragment(DeckPickerNoSpaceLeftDialog.newInstance());
        } else if (preferences.getBoolean("noSpaceLeft", false)) {
            Timber.i("No space left");
            showDialogFragment(DeckPickerBackupNoSpaceLeftDialog.newInstance());
            preferences.edit().remove("noSpaceLeft").apply();
        } else if (InitialActivity.performSetupFromFreshInstallOrClearedPreferences(preferences)) {
            onFinishedStartup();
        } else if (skip < 2 && !InitialActivity.isLatestVersion(preferences)) {
            Timber.i("AnkiDroid is being updated and a collection already exists.");
            // The user might appreciate us now, see if they will help us get better?
            if (!preferences.contains(UsageAnalytics.ANALYTICS_OPTIN_KEY)) {
                displayAnalyticsOptInDialog();
            }

            // For upgrades, we check if we are upgrading
            // to a version that contains additions to the database integrity check routine that we would
            // like to run on all collections. A missing version number is assumed to be a fresh
            // installation of AnkiDroid and we don't run the check.
            long current = VersionUtils.getPkgVersionCode();
            Timber.i("Current AnkiDroid version: %s", current);
            long previous;
            if (preferences.contains(UPGRADE_VERSION_KEY)) {
                // Upgrading currently installed app
                previous = getPreviousVersion(preferences, current);
            } else {
                // Fresh install
                previous = current;
            }
            preferences.edit().putLong(UPGRADE_VERSION_KEY, current).apply();

            // Delete the media database made by any version before 2.3 beta due to upgrade errors.
            // It is rebuilt on the next sync or media check
            if (previous < 20300200) {
                Timber.i("Deleting media database");
                File mediaDb = new File(CollectionHelper.getCurrentAnkiDroidDirectory(this), "collection.media.ad.db2");
                if (mediaDb.exists()) {
                    mediaDb.delete();
                }
            }
            // Recommend the user to do a full-sync if they're upgrading from before 2.3.1beta8
            if (previous < 20301208) {
                Timber.i("Recommend the user to do a full-sync");
                mRecommendFullSync = true;
            }

            // Fix "font-family" definition in templates created by AnkiDroid before 2.6alpha23
            if (previous < 20600123) {
                Timber.i("Fixing font-family definition in templates");
                try {
                    ModelManager models = getCol().getModels();
                    for (Model m : models.all()) {
                        String css = m.getString("css");
                        if (css.contains("font-familiy")) {
                            m.put("css", css.replace("font-familiy", "font-family"));
                            models.save(m);
                        }
                    }
                    models.flush();
                } catch (JSONException e) {
                    Timber.e(e, "Failed to upgrade css definitions.");
                }
            }

            // Check if preference upgrade or database check required, otherwise go to new feature screen
            int upgradeDbVersion = AnkiDroidApp.CHECK_DB_AT_VERSION;

            // Specifying a checkpoint in the future is not supported, please don't do it!
            if (current < upgradeDbVersion) {
                Timber.e("Invalid value for CHECK_DB_AT_VERSION");
                UIUtils.showSimpleSnackbar(this, "Invalid value for CHECK_DB_AT_VERSION", false);
                onFinishedStartup();
                return;
            }

            // Skip full DB check if the basic check is OK
            //TODO: remove this variable if we really want to do the full db check on every user
            boolean skipDbCheck = false;
            //if (previous < upgradeDbVersion && getCol().basicCheck()) {
            //    skipDbCheck = true;
            //}

            boolean upgradedPreferences = InitialActivity.upgradePreferences(this, previous);
            // Integrity check loads asynchronously and then restart deck picker when finished
            //noinspection ConstantConditions
            if (!skipDbCheck && previous < upgradeDbVersion) {
                Timber.i("showStartupScreensAndDialogs() running integrityCheck()");
                //#5852 - since we may have a warning about disk space, we don't want to force a check database
                //and show a warning before the user knows what is happening.
                new MaterialDialog.Builder(this)
                        .title(R.string.integrity_check_startup_title)
                        .content(R.string.integrity_check_startup_content)
                        .positiveText(R.string.check_db)
                        .negativeText(R.string.close)
                        .onPositive((materialDialog, dialogAction) -> integrityCheck())
                        .onNeutral((materialDialog, dialogAction) -> restartActivity())
                        .onNegative((materialDialog, dialogAction) ->  restartActivity())
                        .canceledOnTouchOutside(false)
                        .cancelable(false)
                        .build()
                        .show();
                return;
            }
            if (upgradedPreferences) {
                Timber.i("Updated preferences with no integrity check - restarting activity");
                // If integrityCheck() doesn't occur, but we did update preferences we should restart DeckPicker to
                // proceed
                restartActivity();
                return;
            }

            // If no changes are required we go to the new features activity
            // There the "lastVersion" is set, so that this code is not reached again
            if (VersionUtils.isReleaseVersion()) {
                Timber.i("Displaying new features");
                Intent infoIntent = new Intent(this, Info.class);
                infoIntent.putExtra(Info.TYPE_EXTRA, Info.TYPE_NEW_VERSION);

                if (skip != 0) {
                    startActivityForResultWithAnimation(infoIntent, SHOW_INFO_NEW_VERSION,
                            START);
                } else {
                    startActivityForResultWithoutAnimation(infoIntent, SHOW_INFO_NEW_VERSION);
                }
            } else {
                Timber.i("Dev Build - not showing 'new features'");
                // Don't show new features dialog for development builds
                InitialActivity.setUpgradedToLatestVersion(preferences);
                String ver = getResources().getString(R.string.updated_version, VersionUtils.getPkgVersionName());
                UIUtils.showSnackbar(this, ver, true, -1, null, findViewById(R.id.root_layout), null);
                showStartupScreensAndDialogs(preferences, 2);
            }
        } else {
            // This is the main call when there is nothing special required
            Timber.i("No startup screens required");
            onFinishedStartup();
        }
    }

    public void displayDowngradeFailedNoSpace() {
        Timber.w("Not enough space to downgrade");
        FileSizeFormatter formatter = new FileSizeFormatter(this);
        String collectionPath = CollectionHelper.getCollectionPath(this);
        File collectionFile = new File(collectionPath);
        showDialogFragment(DeckPickerNoSpaceToDowngradeDialog.newInstance(formatter, collectionFile));
    }


    @VisibleForTesting
    protected void displayAnalyticsOptInDialog() {
        showDialogFragment(DeckPickerAnalyticsOptInDialog.newInstance());
    }


    protected long getPreviousVersion(SharedPreferences preferences, long current) {
        long previous;
        try {
            previous = preferences.getLong(UPGRADE_VERSION_KEY, current);
        } catch (ClassCastException e) {
            Timber.w(e);
            try {
                // set 20900203 to default value, as it's the latest version that stores integer in shared prefs
                previous = preferences.getInt(UPGRADE_VERSION_KEY, 20900203);
            } catch (ClassCastException cce) {
                Timber.w(cce);
                // Previous versions stored this as a string.
                String s = preferences.getString(UPGRADE_VERSION_KEY, "");
                // The last version of AnkiDroid that stored this as a string was 2.0.2.
                // We manually set the version here, but anything older will force a DB check.
                if ("2.0.2".equals(s)) {
                    previous = 40;
                } else {
                    previous = 0;
                }
            }
            Timber.d("Updating shared preferences stored key %s type to long", UPGRADE_VERSION_KEY);
            // Expected Editor.putLong to be called later to update the value in shared prefs
            preferences.edit().remove(UPGRADE_VERSION_KEY).apply();
        }
        Timber.i("Previous AnkiDroid version: %s", previous);
        return previous;
    }


    private UndoTaskListener undoTaskListener(boolean isReview) {
        return new UndoTaskListener(isReview, this);
    }
    private static class UndoTaskListener extends TaskListenerWithContext<DeckPicker, Unit, Computation<? extends SchedulerService.NextCard<?>>> {
        private final boolean mIsReview;

        public UndoTaskListener(boolean isReview, DeckPicker deckPicker) {
            super(deckPicker);
            this.mIsReview = isReview;
        }


        @Override
        public void actualOnCancelled(@NonNull DeckPicker deckPicker) {
            deckPicker.hideProgressBar();
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.showProgressBar();
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, Computation<? extends SchedulerService.NextCard<?>> voi) {
            deckPicker.hideProgressBar();
            Timber.i("Undo completed");
            if (mIsReview) {
                Timber.i("Review undone - opening reviewer.");
                deckPicker.openReviewer();
            }
        }
    }
    private void undo() {
        Timber.i("undo()");
        String undoReviewString = getResources().getString(R.string.undo_action_review);
        final boolean isReview = undoReviewString.equals(getCol().undoName(getResources()));
        new UndoService.Undo().runWithHandler(undoTaskListener(isReview));
    }


    // Show dialogs to deal with database loading issues etc
    public void showDatabaseErrorDialog(int id) {
        AsyncDialogFragment newFragment = DatabaseErrorDialog.newInstance(id);
        showAsyncDialogFragment(newFragment);
    }


    @Override
    public void showMediaCheckDialog(int id) {
        showAsyncDialogFragment(MediaCheckDialog.newInstance(id));
    }


    @Override
    public void showMediaCheckDialog(int id, List<? extends List<String>> checkList) {
        showAsyncDialogFragment(MediaCheckDialog.newInstance(id, checkList));
    }


    /**
     * Show a specific sync error dialog
     * @param id id of dialog to show
     */
    @Override
    public void showSyncErrorDialog(int id) {
        showSyncErrorDialog(id, "");
    }

    /**
     * Show a specific sync error dialog
     * @param id id of dialog to show
     * @param message text to show
     */
    @Override
    public void showSyncErrorDialog(int id, String message) {
        AsyncDialogFragment newFragment = SyncErrorDialog.newInstance(id, message);
        showAsyncDialogFragment(newFragment, NotificationChannels.Channel.SYNC);
    }

    /**
     *  Show simple error dialog with just the message and OK button. Reload the activity when dialog closed.
     */
    private void showSyncErrorMessage(@Nullable String message) {
        String title = getResources().getString(R.string.sync_error);
        showSimpleMessageDialog(title, message, true);
    }

    /**
     *  Show a simple snackbar message or notification if the activity is not in foreground
     * @param messageResource String resource for message
     */
    private void showSyncLogMessage(@StringRes int messageResource, String syncMessage) {
        if (mActivityPaused) {
            Resources res = AnkiDroidApp.getAppResources();
            showSimpleNotification(res.getString(R.string.app_name),
                    res.getString(messageResource),
                    NotificationChannels.Channel.SYNC);
        } else {
            if (syncMessage == null || syncMessage.length() == 0) {
                if (messageResource == R.string.youre_offline && !Connection.getAllowLoginSyncOnNoConnection()) {
                    //#6396 - Add a temporary "Try Anyway" button until we sort out `isOnline`
                    View root = this.findViewById(R.id.root_layout);
                    UIUtils.showSnackbar(this, messageResource, false, R.string.sync_even_if_offline, (v) -> {
                        Connection.setAllowLoginSyncOnNoConnection(true);
                        sync();
                    }, null);
                } else {
                    UIUtils.showSimpleSnackbar(this, messageResource, false);
                }
            } else {
                Resources res = AnkiDroidApp.getAppResources();
                showSimpleMessageDialog(res.getString(messageResource), syncMessage, false);
            }
        }
    }

    public void showImportDialog(int id, String message) {
        Timber.d("showImportDialog() delegating to ImportDialog");
        if (message == null) {
            message = "";
        }
        AsyncDialogFragment newFragment = ImportDialog.newInstance(id, message);
        showAsyncDialogFragment(newFragment);
    }


    public void onSdCardNotMounted() {
        UIUtils.showThemedToast(this, getResources().getString(R.string.sd_card_not_mounted), false);
        finishWithoutAnimation();
    }

    // Callback method to submit error report
    public void sendErrorReport() {
        AnkiDroidApp.sendExceptionReport(new RuntimeException(), "DeckPicker.sendErrorReport");
    }


    private RepairCollectionTask repairCollectionTask() {
        return new RepairCollectionTask(this);
    }
    private static class RepairCollectionTask extends TaskListenerWithContext<DeckPicker, Void, Boolean>{
        public RepairCollectionTask(DeckPicker deckPicker) {
            super(deckPicker);
        }

        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, null,
                    deckPicker.getResources().getString(R.string.backup_repair_deck_progress), false);
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, Boolean result) {
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            if (!result) {
                UIUtils.showThemedToast(deckPicker, deckPicker.getResources().getString(R.string.deck_repair_error), true);
                deckPicker.showCollectionErrorDialog();
            }
        }
    }
    // Callback method to handle repairing deck
    public void repairCollection() {
        Timber.i("Repairing the Collection");
        TaskManager.launchCollectionTask(new CollectionTask.RepairCollection(), repairCollectionTask());
    }


    // Callback method to handle database integrity check
    public void integrityCheck() {
        //#5852 - We were having issues with integrity checks where the users had run out of space.
        //display a dialog box if we don't have the space
        CollectionIntegrityStorageCheck status = CollectionIntegrityStorageCheck.createInstance(this);
        if (status.shouldWarnOnIntegrityCheck()) {
            Timber.d("Displaying File Size confirmation");
            new MaterialDialog.Builder(this)
                    .title(R.string.check_db_title)
                    .content(status.getWarningDetails(this))
                    .positiveText(R.string.integrity_check_continue_anyway)
                    .onPositive((dialog, which) -> performIntegrityCheck())
                    .negativeText(R.string.dialog_cancel)
                    .show();
        } else {
            performIntegrityCheck();
        }
    }


    private void performIntegrityCheck() {
        Timber.i("performIntegrityCheck()");
        TaskManager.launchCollectionTask(new CollectionTask.CheckDatabase(), new CheckDatabaseListener());
    }


    private MediaCheckListener mediaCheckListener() {
        return new MediaCheckListener(this);
    }
    private static class MediaCheckListener extends TaskListenerWithContext<DeckPicker, Void, Computation<List<List<String>>>>{
        public MediaCheckListener (DeckPicker deckPicker) {
            super(deckPicker);
        }

        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, null,
                    deckPicker.getResources().getString(R.string.check_media_message), false);
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, Computation<List<List<String>>> result) {
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            if (result.succeeded()) {
                @SuppressWarnings("unchecked")
                List<List<String>> checkList = result.getValue();
                deckPicker.showMediaCheckDialog(MediaCheckDialog.DIALOG_MEDIA_CHECK_RESULTS, checkList);
            } else {
                deckPicker.showSimpleMessageDialog(deckPicker.getResources().getString(R.string.check_media_failed));
            }
        }
    }
    @Override
    public void mediaCheck() {
        TaskManager.launchCollectionTask(new CollectionTask.CheckMedia(), mediaCheckListener());
    }

    private MediaDeleteListener mediaDeleteListener() {
        return new MediaDeleteListener(this);
    }
    private static class MediaDeleteListener extends TaskListenerWithContext<DeckPicker, Void, Integer> {
        public MediaDeleteListener (DeckPicker deckPicker) {
            super(deckPicker);
        }

        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, null,
                    deckPicker.getResources().getString(R.string.delete_media_message), false);
        }

        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, Integer deletedFiles) {
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            deckPicker.showSimpleMessageDialog(deckPicker.getResources().getString(R.string.delete_media_result_title),
                    deckPicker.getResources().getQuantityString(R.plurals.delete_media_result_message, deletedFiles, deletedFiles));
        }
    }
    @Override
    public void deleteUnused(List<String> unused) {
        TaskManager.launchCollectionTask(new CollectionTask.DeleteMedia(unused), mediaDeleteListener());
    }


    public void exit() {
        CollectionHelper.getInstance().closeCollection(false, "DeckPicker:exit()");
        finishWithoutAnimation();
    }


    public void handleDbError() {
        Timber.i("Displaying Database Error");
        showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_LOAD_FAILED);
    }

    public void handleDbLocked() {
        Timber.i("Displaying Database Locked");
        showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_DB_LOCKED);
    }


    public void restoreFromBackup(String path) {
        importReplace(path);
    }


    // Helper function to check if there are any saved stacktraces
    public boolean hasErrorFiles() {
        for (String file : this.fileList()) {
            if (file.endsWith(".stacktrace")) {
                return true;
            }
        }
        return false;
    }


    // Sync with Anki Web
    @Override
    public void sync() {
        sync(null);
    }


    /**
     * The mother of all syncing attempts. This might be called from sync() as first attempt to sync a collection OR
     * from the mSyncConflictResolutionListener if the first attempt determines that a full-sync is required.
     */
    @Override
    public void sync(Connection.ConflictResolution syncConflictResolution) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        String hkey = preferences.getString("hkey", "");
        if (hkey.length() == 0) {
            Timber.w("User not logged in");
            mPullToSyncWrapper.setRefreshing(false);
            showSyncErrorDialog(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC);
        } else {
            Connection.sync(mSyncListener,
                    new Connection.Payload(new Object[] { hkey,
                            preferences.getBoolean("syncFetchesMedia", true),
                            syncConflictResolution,
                            HostNumFactory.getInstance(getBaseContext()) }));
        }
    }


    private final Connection.TaskListener mSyncListener = new Connection.CancellableTaskListener() {
        private String mCurrentMessage;
        private long mCountUp;
        private long mCountDown;
        private boolean mDialogDisplayFailure = false;

        @Override
        public void onDisconnected() {
            showSyncLogMessage(R.string.youre_offline, "");
        }

        @Override
        public void onCancelled() {
            showSyncLogMessage(R.string.sync_cancelled, "");
            if (!mDialogDisplayFailure) {
                mProgressDialog.dismiss();
                // update deck list in case sync was cancelled during media sync and main sync was actually successful
                updateDeckList();
            }
            // reset our display failure fate, just in case it is re-used
            mDialogDisplayFailure = false;
        }

        @Override
        public void onPreExecute() {
            mCountUp = 0;
            mCountDown = 0;
            final long syncStartTime = getCol().getTime().intTimeMS();

            if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                try {
                    mProgressDialog = StyledProgressDialog
                            .show(DeckPicker.this, getResources().getString(R.string.sync_title),
                                    getResources().getString(R.string.sync_title) + "\n"
                                            + getResources().getString(R.string.sync_up_down_size, mCountUp, mCountDown),
                                    false);
                } catch (WindowManager.BadTokenException e) {
                    // If we could not show the progress dialog to start even, bail out - user will get a message
                    Timber.w(e, "Unable to display Sync progress dialog, Activity not valid?");
                    mDialogDisplayFailure = true;
                    Connection.cancel();
                    return;
                }

                // Override the back key so that the user can cancel a sync which is in progress
                mProgressDialog.setOnKeyListener((dialog, keyCode, event) -> {
                    // Make sure our method doesn't get called twice
                    if (event.getAction()!=KeyEvent.ACTION_DOWN) {
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_BACK && Connection.isCancellable() &&
                            !Connection.getIsCancelled()) {
                        // If less than 2s has elapsed since sync started then don't ask for confirmation
                        if (getCol().getTime().intTimeMS() - syncStartTime < 2000) {
                            Connection.cancel();
                            mProgressDialog.setContent(R.string.sync_cancel_message);
                            return true;
                        }
                        // Show confirmation dialog to check if the user wants to cancel the sync
                        MaterialDialog.Builder builder = new MaterialDialog.Builder(mProgressDialog.getContext());
                        builder.content(R.string.cancel_sync_confirm)
                                .cancelable(false)
                                .positiveText(R.string.dialog_ok)
                                .negativeText(R.string.continue_sync)
                                .onPositive((inner_dialog, which) -> {
                                    mProgressDialog.setContent(R.string.sync_cancel_message);
                                    Connection.cancel();
                                });
                        builder.show();
                        return true;
                    } else {
                        return false;
                    }
                });
            }

            // Store the current time so that we don't bother the user with a sync prompt for another 10 minutes
            // Note: getLs() in Libanki doesn't take into account the case when no changes were found, or sync cancelled
            SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
            preferences.edit().putLong("lastSyncTime", syncStartTime).apply();
        }


        @Override
        public void onProgressUpdate(Object... values) {
            Resources res = getResources();
            if (values[0] instanceof Integer) {
                int id = (Integer) values[0];
                if (id != 0) {
                    mCurrentMessage = res.getString(id);
                }
                if (values.length >= 3) {
                    mCountUp = (Long) values[1];
                    mCountDown = (Long) values[2];
                }
            } else if (values[0] instanceof String) {
                mCurrentMessage = (String) values[0];
                if (values.length >= 3) {
                    mCountUp = (Long) values[1];
                    mCountDown = (Long) values[2];
                }
            }
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.setContent(mCurrentMessage + "\n"
                        + res
                        .getString(R.string.sync_up_down_size, mCountUp / 1024, mCountDown / 1024));
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        public void onPostExecute(Payload data) {
            mPullToSyncWrapper.setRefreshing(false);
            String dialogMessage = "";
            Timber.d("Sync Listener onPostExecute()");
            Resources res = getResources();
            try {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            } catch (IllegalArgumentException e) {
                Timber.e(e, "Could not dismiss mProgressDialog. The Activity must have been destroyed while the AsyncTask was running");
                AnkiDroidApp.sendExceptionReport(e, "DeckPicker.onPostExecute", "Could not dismiss mProgressDialog");
            }
            String syncMessage = data.message;
            Timber.i("Sync Listener: onPostExecute: Data: %s", data.toString());
            if (!data.success) {
                Object[] result = data.result;
                Syncer.ConnectionResultType resultType = data.resultType;
                if (resultType != null) {
                    switch (resultType) {
                        case BAD_AUTH:
                            // delete old auth information
                            SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
                            Editor editor = preferences.edit();
                            editor.putString("username", "");
                            editor.putString("hkey", "");
                            editor.apply();
                            // then show not logged in dialog
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC);
                            break;
                        case NO_CHANGES:
                            SyncStatus.markSyncCompleted();
                            // show no changes message, use false flag so we don't show "sync error" as the Dialog title
                            showSyncLogMessage(R.string.sync_no_changes_message, "");
                            break;
                        case CLOCK_OFF:
                            long diff = (Long) result[0];
                            if (diff >= 86100) {
                                // The difference if more than a day minus 5 minutes acceptable by ankiweb error
                                dialogMessage = res.getString(R.string.sync_log_clocks_unsynchronized, diff,
                                        res.getString(R.string.sync_log_clocks_unsynchronized_date));
                            } else if (Math.abs((diff % 3600.0) - 1800.0) >= 1500.0) {
                                // The difference would be within limit if we adjusted the time by few hours
                                // It doesn't work for all timezones, but it covers most and it's a guess anyway
                                dialogMessage = res.getString(R.string.sync_log_clocks_unsynchronized, diff,
                                        res.getString(R.string.sync_log_clocks_unsynchronized_tz));
                            } else {
                                dialogMessage = res.getString(R.string.sync_log_clocks_unsynchronized, diff, "");
                            }
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case FULL_SYNC:
                            if (getCol().isEmpty()) {
                                // don't prompt user to resolve sync conflict if local collection empty
                                sync(FULL_DOWNLOAD);
                                // TODO: Also do reverse check to see if AnkiWeb collection is empty if Anki Desktop
                                // implements it
                            } else {
                                // If can't be resolved then automatically then show conflict resolution dialog
                                showSyncErrorDialog(SyncErrorDialog.DIALOG_SYNC_CONFLICT_RESOLUTION);
                            }
                            break;
                        case BASIC_CHECK_FAILED:
                            dialogMessage = res.getString(R.string.sync_basic_check_failed, res.getString(R.string.check_db));
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_SYNC_BASIC_CHECK_ERROR, joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case DB_ERROR:
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_SYNC_CORRUPT_COLLECTION, syncMessage);
                            break;
                        case OVERWRITE_ERROR:
                            dialogMessage = res.getString(R.string.sync_overwrite_error);
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case REMOTE_DB_ERROR:
                            dialogMessage = res.getString(R.string.sync_remote_db_error);
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case SD_ACCESS_ERROR:
                            dialogMessage = res.getString(R.string.sync_write_access_error);
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case FINISH_ERROR:
                            dialogMessage = res.getString(R.string.sync_log_finish_error);
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case CONNECTION_ERROR:
                            dialogMessage = res.getString(R.string.sync_connection_error);
                            if (result.length >= 0 && result[0] instanceof Exception) {
                                dialogMessage += "\n\n" + ((Exception) result[0]).getLocalizedMessage();
                            }
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case IO_EXCEPTION:
                            handleDbError();
                            break;
                        case GENERIC_ERROR:
                            dialogMessage = res.getString(R.string.sync_generic_error);
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case OUT_OF_MEMORY_ERROR:
                            dialogMessage = res.getString(R.string.error_insufficient_memory);
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case SANITY_CHECK_ERROR:
                            dialogMessage = res.getString(R.string.sync_sanity_failed);
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_SYNC_SANITY_ERROR,
                                    joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case SERVER_ABORT:
                            // syncMsg has already been set above, no need to fetch it here.
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case MEDIA_SYNC_SERVER_ERROR:
                            dialogMessage = res.getString(R.string.sync_media_error_check);
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_MEDIA_SYNC_ERROR,
                                    joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        case CUSTOM_SYNC_SERVER_URL:
                            String url = result.length > 0 && result[0] instanceof CustomSyncServerUrlException
                                    ? ((CustomSyncServerUrlException) result[0]).getUrl() : "unknown";
                            dialogMessage = res.getString(R.string.sync_error_invalid_sync_server, url);
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                        default:
                            if (result.length > 0 && result[0] instanceof Integer) {
                                int code = (Integer) result[0];
                                dialogMessage = rewriteError(code);
                                if (dialogMessage == null) {
                                    dialogMessage = res.getString(R.string.sync_log_error_specific,
                                            Integer.toString(code), result[1]);
                                }
                            } else {
                                dialogMessage = res.getString(R.string.sync_log_error_specific, Integer.toString(-1), resultType);
                            }
                            showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                            break;
                    }
                } else {
                    dialogMessage = res.getString(R.string.sync_generic_error);
                    showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                }
            } else {
                Timber.i("Sync was successful");
                if (data.data[2] != null && !"".equals(data.data[2])) {
                    Timber.i("Syncing had additional information");
                    // There was a media error, so show it
                    // Note: Do not log this data. May contain user email.
                    String message = res.getString(R.string.sync_database_acknowledge) + "\n\n" + data.data[2];
                    showSimpleMessageDialog(message);
                } else if (data.data.length > 0 && data.data[0] instanceof Connection.ConflictResolution) {
                    // A full sync occurred
                    Connection.ConflictResolution dataString = (Connection.ConflictResolution) data.data[0];
                    switch (dataString) {
                        case FULL_UPLOAD:
                            Timber.i("Full Upload Completed");
                            showSyncLogMessage(R.string.sync_log_uploading_message, syncMessage);
                            break;
                        case FULL_DOWNLOAD:
                            Timber.i("Full Download Completed");
                            showSyncLogMessage(R.string.backup_full_sync_from_server, syncMessage);
                            break;
                        default: // should not be possible
                            Timber.i("Full Sync Completed (Unknown Direction)");
                            showSyncLogMessage(R.string.sync_database_acknowledge, syncMessage);
                            break;
                    }
                } else {
                    Timber.i("Regular sync completed successfully");
                    showSyncLogMessage(R.string.sync_database_acknowledge, syncMessage);
                }
                // Mark sync as completed - then refresh the sync icon
                SyncStatus.markSyncCompleted();
                supportInvalidateOptionsMenu();

                updateDeckList();
                WidgetStatus.update(DeckPicker.this);
                if (mFragmented) {
                    try {
                        loadStudyOptionsFragment(false);
                    } catch (IllegalStateException e) {
                        // Activity was stopped or destroyed when the sync finished. Losing the
                        // fragment here is fine since we build a fresh fragment on resume anyway.
                        Timber.w(e, "Failed to load StudyOptionsFragment after sync.");
                    }
                }
            }
        }
    };

    @VisibleForTesting()
    @Nullable
    public String rewriteError(int code) {
        String msg;
        Resources res = getResources();
        switch (code) {
            case 407:
                msg = res.getString(R.string.sync_error_407_proxy_required);
                break;
            case 409:
                msg = res.getString(R.string.sync_error_409);
                break;
            case 413:
                msg = res.getString(R.string.sync_error_413_collection_size);
                break;
            case 500:
                msg = res.getString(R.string.sync_error_500_unknown);
                break;
            case 501:
                msg = res.getString(R.string.sync_error_501_upgrade_required);
                break;
            case 502:
                msg = res.getString(R.string.sync_error_502_maintenance);
                break;
            case 503:
                msg = res.getString(R.string.sync_too_busy);
                break;
            case 504:
                msg = res.getString(R.string.sync_error_504_gateway_timeout);
                break;
            default:
                msg = null;
                break;
        }
        return msg;
    }

    @Nullable
    public static String joinSyncMessages(@Nullable String dialogMessage, @Nullable  String syncMessage) {
        // If both strings have text, separate them by a new line, otherwise return whichever has text
        if (!TextUtils.isEmpty(dialogMessage) && !TextUtils.isEmpty(syncMessage)) {
            return dialogMessage + "\n\n" + syncMessage;
        } else if (!TextUtils.isEmpty(dialogMessage)) {
            return dialogMessage;
        } else {
            return syncMessage;
        }
    }


    @Override
    public void loginToSyncServer() {
        Intent myAccount = new Intent(this, MyAccount.class);
        myAccount.putExtra("notLoggedIn", true);
        startActivityForResultWithAnimation(myAccount, LOG_IN_FOR_SYNC, FADE);
    }


    // Callback to import a file -- adding it to existing collection
    @Override
    public void importAdd(String importPath) {
        Timber.d("importAdd() for file %s", importPath);
        TaskManager.launchCollectionTask(new CollectionTask.ImportAdd(importPath), mImportAddListener);
    }


    // Callback to import a file -- replacing the existing collection
    @Override
    public void importReplace(String importPath) {
        TaskManager.launchCollectionTask(new CollectionTask.ImportReplace(importPath), importReplaceListener());
    }


    /**
     * Load a new studyOptionsFragment. If withDeckOptions is true, the deck options activity will
     * be loaded on top of it. Use this flag when creating a new filtered deck to allow the user to
     * modify the filter settings before being shown the fragment. The fragment itself will handle
     * rebuilding the deck if the settings change.
     */
    private void loadStudyOptionsFragment(boolean withDeckOptions) {
        StudyOptionsFragment details = StudyOptionsFragment.newInstance(withDeckOptions);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.studyoptions_fragment, details);
        ft.commit();
    }


    public StudyOptionsFragment getFragment() {
        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.studyoptions_fragment);
        if ((frag instanceof StudyOptionsFragment)) {
            return (StudyOptionsFragment) frag;
        }
        return null;
    }


    /**
     * Show a message when the SD card is ejected
     */
    private void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(SdCardReceiver.MEDIA_EJECT)) {
                        onSdCardNotMounted();
                    } else if (intent.getAction().equals(SdCardReceiver.MEDIA_MOUNT)) {
                        restartActivity();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(SdCardReceiver.MEDIA_EJECT);
            iFilter.addAction(SdCardReceiver.MEDIA_MOUNT);
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }


    public void openAnkiWebSharedDecks() {
        Intent intent = new Intent(this, SharedDecksActivity.class);
        startActivityWithoutAnimation(intent);
    }


    private void openStudyOptions(boolean withDeckOptions) {
        if (mFragmented) {
            // The fragment will show the study options screen instead of launching a new activity.
            loadStudyOptionsFragment(withDeckOptions);
        } else {
            Intent intent = new Intent();
            intent.putExtra("withDeckOptions", withDeckOptions);
            intent.setClass(this, StudyOptionsActivity.class);
            startActivityForResultWithAnimation(intent, SHOW_STUDYOPTIONS, START);
        }
    }

    private void openReviewerOrStudyOptions(DeckSelectionType selectionType) {
        switch (selectionType) {
            case DEFAULT:
                if (mFragmented) {
                    openStudyOptions(false);
                } else {
                    openReviewer();
                }
                return;
            case SHOW_STUDY_OPTIONS:
                openStudyOptions(false);
                return;
            case SKIP_STUDY_OPTIONS:
                openReviewer();
                return;
            default:
                Timber.w("openReviewerOrStudyOptions: Unknown selection: %s", selectionType);
        }
    }

    private void handleDeckSelection(long did, DeckSelectionType selectionType) {
        // Clear the undo history when selecting a new deck
        if (getCol().getDecks().selected() != did) {
            getCol().clearUndo();
        }
        // Select the deck
        getCol().getDecks().select(did);
        // Also forget the last deck used by the Browser
        CardBrowser.clearLastDeckId();
        // Reset the schedule so that we get the counts for the currently selected deck
        mFocusedDeck = did;
        // Get some info about the deck to handle special cases
        TreeNode<AbstractDeckTreeNode> deckDueTreeNode = mDeckListAdapter.getNodeByDid(did);
        if (!deckDueTreeNode.getValue().shouldDisplayCounts() || deckDueTreeNode.getValue().knownToHaveRep()) {
            // If we don't yet have numbers, we trust the user that they knows what they opens, tries to open it.
            // If there is nothing to review, it'll come back to deck picker.
            openReviewerOrStudyOptions(selectionType);
            return;
        }
        // There are numbers
        // Figure out what action to take
        if (getCol().getSched().hasCardsTodayAfterStudyAheadLimit()) {
            // If there are cards due that can't be studied yet (due to the learn ahead limit) then go to study options
            openStudyOptions(false);
        } else if (getCol().getSched().newDue() || getCol().getSched().revDue()) {
            // If there are no cards to review because of the daily study limit then give "Study more" option
            UIUtils.showSnackbar(this, R.string.studyoptions_limit_reached, false, R.string.study_more, v -> {
                CustomStudyDialog d = mCustomStudyDialogFactory.newCustomStudyDialog().withArguments(
                        CustomStudyDialog.ContextMenuConfiguration.LIMITS,
                        getCol().getDecks().selected(), true);
                showDialogFragment(d);
            }, findViewById(R.id.root_layout), mSnackbarShowHideCallback);
            // Check if we need to update the fragment or update the deck list. The same checks
            // are required for all snackbars below.
            if (mFragmented) {
                // Tablets must always show the study options that corresponds to the current deck,
                // regardless of whether the deck is currently reviewable or not.
                openStudyOptions(false);
            } else {
                // On phones, we update the deck list to ensure the currently selected deck is
                // highlighted correctly.
                updateDeckList();
            }
        } else if (getCol().getDecks().isDyn(did)) {
            // Go to the study options screen if filtered deck with no cards to study
            openStudyOptions(false);
        } else if (!deckDueTreeNode.hasChildren() && getCol().isEmptyDeck(did)) {
            // If the deck is empty and has no children then show a message saying it's empty
            UIUtils.showSnackbar(this, R.string.empty_deck, false, R.string.empty_deck_add_note,
                    v -> addNote(), findViewById(R.id.root_layout), mSnackbarShowHideCallback);
            if (mFragmented) {
                openStudyOptions(false);
            } else {
                updateDeckList();
            }
        } else {
            // Otherwise say there are no cards scheduled to study, and give option to do custom study
            UIUtils.showSnackbar(this, R.string.studyoptions_empty_schedule, false, R.string.custom_study, v -> {
                CustomStudyDialog d = mCustomStudyDialogFactory.newCustomStudyDialog().withArguments(
                        CustomStudyDialog.ContextMenuConfiguration.EMPTY_SCHEDULE,
                        getCol().getDecks().selected(), true);
                showDialogFragment(d);
            }, findViewById(R.id.root_layout), mSnackbarShowHideCallback);
            if (mFragmented) {
                openStudyOptions(false);
            } else {
                updateDeckList();
            }
        }
    }


    private void openHelpUrl(Uri helpUrl) {
        openUrl(helpUrl);
    }


    /**
     * Scroll the deck list so that it is centered on the current deck.
     *
     * @param did The deck ID of the deck to select.
     */
    private void scrollDecklistToDeck(long did) {
        int position = mDeckListAdapter.findDeckPosition(did);
        mRecyclerViewLayoutManager.scrollToPositionWithOffset(position, (mRecyclerView.getHeight() / 2));
    }


    private <T extends AbstractDeckTreeNode> UpdateDeckListListener<T> updateDeckListListener() {
        return new UpdateDeckListListener<T>(this);
    }
    private static class UpdateDeckListListener<T extends AbstractDeckTreeNode> extends TaskListenerWithContext<DeckPicker, Void, List<TreeNode<T>>>{
        public UpdateDeckListListener(DeckPicker deckPicker) {
            super(deckPicker);
        }

        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            if (!deckPicker.colIsOpen()) {
                deckPicker.showProgressBar();
            }
            Timber.d("Refreshing deck list");
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, List<TreeNode<T>> dueTree) {
            Timber.i("Updating deck list UI");
            deckPicker.hideProgressBar();
            // Make sure the fragment is visible
            if (deckPicker.mFragmented) {
                deckPicker.mStudyoptionsFrame.setVisibility(View.VISIBLE);
            }
            if (dueTree == null) {
                Timber.e("null result loading deck counts");
                deckPicker.showCollectionErrorDialog();
                return;
            }
            deckPicker.mDueTree = dueTree.stream().map(x -> x.unsafeCastToType(AbstractDeckTreeNode.class) ).collect(Collectors.toList());

            deckPicker.__renderPage();
            // Update the mini statistics bar as well
            AnkiStatsTaskHandler.createReviewSummaryStatistics(deckPicker.getCol(), deckPicker.mReviewSummaryTextView);
            Timber.d("Startup - Deck List UI Completed");
        }
    }
    /**
     * Launch an asynchronous task to rebuild the deck list and recalculate the deck counts. Use this
     * after any change to a deck (e.g., rename, importing, add/delete) that needs to be reflected
     * in the deck list.
     *
     * This method also triggers an update for the widget to reflect the newly calculated counts.
     */
    protected void updateDeckList() {
        updateDeckList(false);
    }

    private void updateDeckList(boolean quick) {
        if (quick) {
            TaskManager.launchCollectionTask(new CollectionTask.LoadDeck(), updateDeckListListener());
        } else {
            TaskManager.launchCollectionTask(new CollectionTask.LoadDeckCounts(), updateDeckListListener());
        }
    }

    public void __renderPage() {
        if (mDueTree == null) {
            // mDueTree may be set back to null when the activity restart.
            // We may need to recompute it.
            updateDeckList();
            return;
        }

        // Check if default deck is the only available and there are no cards
        boolean isEmpty = mDueTree.size() == 1 && mDueTree.get(0).getValue().getDid() == 1 && getCol().isEmpty();

        if (animationDisabled()) {
            mDeckPickerContent.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            mNoDecksPlaceholder.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        } else {
            float translation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                    getResources().getDisplayMetrics());

            boolean decksListShown = mDeckPickerContent.getVisibility() == View.VISIBLE;
            boolean placeholderShown = mNoDecksPlaceholder.getVisibility() == View.VISIBLE;

            if (isEmpty) {
                if (decksListShown) {
                    fadeOut(mDeckPickerContent, mShortAnimDuration, translation);
                }

                if (!placeholderShown) {
                    fadeIn(mNoDecksPlaceholder, mShortAnimDuration, translation)
                            // This is some bad choreographing here
                            .setStartDelay(decksListShown ? mShortAnimDuration * 2 : 0);
                }
            } else {
                if (!decksListShown) {
                    fadeIn(mDeckPickerContent, mShortAnimDuration, translation)
                            .setStartDelay(placeholderShown ? mShortAnimDuration * 2 : 0);
                }

                if (placeholderShown) {
                    fadeOut(mNoDecksPlaceholder, mShortAnimDuration, translation);
                }
            }
        }

        CharSequence currentFilter = mToolbarSearchView != null ? mToolbarSearchView.getQuery() : null;

        if (isEmpty) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(null);
            }
            if (mToolbarSearchView != null) {
                mDeckListAdapter.getFilter().filter(currentFilter);
            }
            // We're done here
            return;
        }


        mDeckListAdapter.buildDeckList(mDueTree, getCol(), currentFilter);

        // Set the "x due in y minutes" subtitle
        try {
            Integer eta = mDeckListAdapter.getEta();
            Integer due = mDeckListAdapter.getDue();
            Resources res = getResources();
            if (getCol().cardCount() != -1) {
                String time = "-";
                if (eta != -1 && eta != null) {
                    time = Utils.timeQuantityTopDeckPicker(this, eta*60);
                }
                if (due != null && getSupportActionBar() != null) {
                    String subTitle;
                    if (due == 0) {
                        subTitle = res.getQuantityString(R.plurals.deckpicker_title_zero_due, getCol().cardCount(), getCol().cardCount());
                    } else {
                        subTitle = res.getQuantityString(R.plurals.deckpicker_title, due, due, time);
                    }
                    getSupportActionBar().setSubtitle(subTitle);
                }
            }
        } catch (RuntimeException e) {
            Timber.e(e, "RuntimeException setting time remaining");
        }

        long current = getCol().getDecks().current().optLong("id");
        if (mFocusedDeck != current) {
            scrollDecklistToDeck(current);
            mFocusedDeck = current;
        }
    }

    // Animation utility methods used by __renderPage() method

    public static ViewPropertyAnimator fadeIn(View view, int duration) {
        return fadeIn(view, duration, 0);
    }

    public static ViewPropertyAnimator fadeIn(View view, int duration, float translation) {
        return fadeIn(view, duration, translation, () -> view.setVisibility(View.VISIBLE));
    }

    public static ViewPropertyAnimator fadeIn(View view, int duration, float translation, Runnable startAction) {
        view.setAlpha(0);
        view.setTranslationY(translation);
        return view.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(duration)
                .withStartAction(startAction);
    }

    public static ViewPropertyAnimator fadeOut(View view, int duration) {
        return fadeOut(view, duration, 0);
    }

    public static ViewPropertyAnimator fadeOut(View view, int duration, float translation) {
        return fadeOut(view, duration, translation, () -> view.setVisibility(View.GONE));
    }

    public static ViewPropertyAnimator fadeOut(View view, int duration, float translation, Runnable endAction) {
        view.setAlpha(1);
        view.setTranslationY(0);
        return view.animate()
                .alpha(0)
                .translationY(translation)
                .setDuration(duration)
                .withEndAction(endAction);
    }


    // Callback to show study options for currently selected deck
    public void showContextMenuDeckOptions(long did) {
        // open deck options
        if (getCol().getDecks().isDyn(did)) {
            // open cram options if filtered deck
            Intent i = new Intent(DeckPicker.this, FilteredDeckOptions.class);
            i.putExtra("did", did);
            startActivityWithAnimation(i, FADE);
        } else {
            // otherwise open regular options
            Intent i = new Intent(DeckPicker.this, DeckOptions.class);
            i.putExtra("did", did);
            startActivityWithAnimation(i, FADE);
        }
    }


    public void exportDeck(long did) {
        String msg = getResources().getString(R.string.confirm_apkg_export_deck, getCol().getDecks().get(did).getString("name"));
        mExportingDelegate.showExportDialog(msg, did);
    }

    public void createIcon(Context context, long did) {
        // This code should not be reachable with lower versions
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(this, Long.toString(did))
                .setIntent(new Intent(context, Reviewer.class)
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra("deckId", did)
                )
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setShortLabel(Decks.basename(getCol().getDecks().name(did)))
                .setLongLabel(getCol().getDecks().name(did))
                .build();

        try {
            boolean success = ShortcutManagerCompat.requestPinShortcut(this, shortcut, null);

            // User report: "success" is true even if Vivo does not have permission
            if (AdaptionUtil.isVivo()) {
                UIUtils.showThemedToast(this, getString(R.string.create_shortcut_error_vivo), false);
            }

            if (!success) {
                UIUtils.showThemedToast(this, getString(R.string.create_shortcut_failed), false);
            }
        } catch (Exception e) {
            Timber.w(e);
            UIUtils.showThemedToast(this, getString(R.string.create_shortcut_error, e.getLocalizedMessage()), false);
        }
    }

    public void renameDeckDialog(final long did) {
        final String currentName = getCol().getDecks().name(did);
        CreateDeckDialog createDeckDialog = new CreateDeckDialog(DeckPicker.this, R.string.rename_deck, CreateDeckDialog.DeckDialogType.RENAME_DECK, null);
        createDeckDialog.setDeckName(currentName);
        createDeckDialog.setOnNewDeckCreated((id) -> {
            dismissAllDialogFragments();
            mDeckListAdapter.notifyDataSetChanged();
            updateDeckList();
            if (mFragmented) {
                loadStudyOptionsFragment(false);
            }
        });
        createDeckDialog.showDialog();
    }

    public void confirmDeckDeletion(long did) {
        Resources res = getResources();
        if (!colIsOpen()) {
            return;
        }
        if (did == 1) {
            UIUtils.showSimpleSnackbar(this, R.string.delete_deck_default_deck, true);
            dismissAllDialogFragments();
            return;
        }
        // Get the number of cards contained in this deck and its subdecks
        int cnt = DeckService.countCardsInDeckTree(getCol(), did);
        boolean isDyn = getCol().getDecks().isDyn(did);
        // Delete empty decks without warning. Filtered decks save filters in the deck data, so require confirmation.
        if (cnt == 0 && !isDyn) {
            deleteDeck(did);
            dismissAllDialogFragments();
            return;
        }
        // Otherwise we show a warning and require confirmation
        String msg;
        String deckName = "'" + getCol().getDecks().name(did) + "'";
        if (isDyn) {
            msg = res.getString(R.string.delete_cram_deck_message, deckName);
        } else {
            msg = res.getQuantityString(R.plurals.delete_deck_message, cnt, deckName, cnt);
        }
        showDialogFragment(DeckPickerConfirmDeleteDeckDialog.newInstance(msg, did));
    }


    /**
     * Deletes the provided deck, child decks. and all cards inside.
     * Use {@link #confirmDeckDeletion(long)} for a confirmation dialog
     * @param did the deck to delete
     */
    public void deleteDeck(final long did) {
        TaskManager.launchCollectionTask(new CollectionTask.DeleteDeck(did), deleteDeckListener(did));
    }
    private DeleteDeckListener deleteDeckListener(long did) {
        return new DeleteDeckListener(did, this);
    }
    private static class DeleteDeckListener extends TaskListenerWithContext<DeckPicker, Void, int[]>{
        private final long mDid;
        // Flag to indicate if the deck being deleted is the current deck.
        private boolean mRemovingCurrent;

        public DeleteDeckListener(long did, DeckPicker deckPicker) {
            super(deckPicker);
            this.mDid = did;
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, null,
                    deckPicker.getResources().getString(R.string.delete_deck), false);
            if (mDid == deckPicker.getCol().getDecks().current().optLong("id")) {
                mRemovingCurrent = true;
            }
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, @Nullable int[] v) {
            // After deleting a deck there is no more undo stack
            // Rebuild options menu with side effect of resetting undo button state
            deckPicker.invalidateOptionsMenu();

            // In fragmented mode, if the deleted deck was the current deck, we need to reload
            // the study options fragment with a valid deck and re-center the deck list to the
            // new current deck. Otherwise we just update the list normally.
            if (deckPicker.mFragmented && mRemovingCurrent) {
                deckPicker.updateDeckList();
                deckPicker.openStudyOptions(false);
            } else {
                deckPicker.updateDeckList();
            }

            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                try {
                    deckPicker.mProgressDialog.dismiss();
                } catch (Exception e) {
                    Timber.e(e, "onPostExecute - Exception dismissing dialog");
                }
            }
        }
    }

    /**
     * Show progress bars and rebuild deck list on completion
     */
    private SimpleProgressListener simpleProgressListener() {
        return new SimpleProgressListener(this);
    }
    private static class SimpleProgressListener extends TaskListenerWithContext<DeckPicker, Void, StudyOptionsFragment.DeckStudyData>{
        public SimpleProgressListener (DeckPicker deckPicker) {
            super(deckPicker);
        }

        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.showProgressBar();
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, StudyOptionsFragment.DeckStudyData stats) {
            deckPicker.updateDeckList();
            if (deckPicker.mFragmented) {
                deckPicker.loadStudyOptionsFragment(false);
            }
        }
    }


    public void rebuildFiltered(long did) {
        getCol().getDecks().select(did);
        TaskManager.launchCollectionTask(new CollectionTask.RebuildCram(), simpleProgressListener());
    }

    public void emptyFiltered(long did) {
        getCol().getDecks().select(did);
        TaskManager.launchCollectionTask(new CollectionTask.EmptyCram(), simpleProgressListener());
    }

    @Override
    public void onAttachedToWindow() {

        if (!mFragmented) {
            Window window = getWindow();
            window.setFormat(PixelFormat.RGBA_8888);
        }
    }


    @Override
    public void onRequireDeckListUpdate() {
        updateDeckList();
    }


    private void openReviewer() {
        Intent reviewer = new Intent(this, Reviewer.class);
        startActivityForResultWithAnimation(reviewer, REQUEST_REVIEW, START);
    }

    @Override
    public void onCreateCustomStudySession() {
        updateDeckList();
        openStudyOptions(false);
    }

    @Override
    public void onExtendStudyLimits() {
        if (mFragmented) {
            getFragment().refreshInterface(true);
        }
        updateDeckList();
    }

    public void handleEmptyCards() {
        mEmptyCardTask = TaskManager.launchCollectionTask(new CollectionTask.FindEmptyCards(), handlerEmptyCardListener());
    }
    private HandleEmptyCardListener handlerEmptyCardListener() {
        return new HandleEmptyCardListener(this);
    }
    private static class HandleEmptyCardListener extends TaskListenerWithContext<DeckPicker, Integer, List<Long>> {
        private final int mNumberOfCards;
        private final int mOnePercent;
        private int mIncreaseSinceLastUpdate = 0;

        public HandleEmptyCardListener(DeckPicker deckPicker) {
            super(deckPicker);
            mNumberOfCards = deckPicker.getCol().cardCount();
            mOnePercent = mNumberOfCards / 100;
        }

        private void confirmCancel(@NonNull DeckPicker deckPicker, @NonNull Cancellable  task) {
            new MaterialDialog.Builder(deckPicker)
                    .content(R.string.confirm_cancel)
                    .positiveText(deckPicker.getResources().getString(R.string.yes))
                    .negativeText(deckPicker.getResources().getString(R.string.dialog_no))
                    .onNegative((x, y) -> actualOnPreExecute(deckPicker))
                    .onPositive((x, y) -> task.safeCancel()).show()
            ;
        }

        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            DialogInterface.OnCancelListener onCancel = (dialogInterface) -> {
                Cancellable  emptyCardTask = deckPicker.mEmptyCardTask;
                if (emptyCardTask != null) {
                    confirmCancel(deckPicker, emptyCardTask);
                }};

            deckPicker.mProgressDialog = new MaterialDialog.Builder(deckPicker)
                    .progress(false, mNumberOfCards)
                    .title(R.string.emtpy_cards_finding)
                    .cancelable(true)
                    .show();
            deckPicker.mProgressDialog.setOnCancelListener(onCancel);
            deckPicker.mProgressDialog.setCanceledOnTouchOutside(false);
        }

        @Override
        public void actualOnProgressUpdate(@NonNull DeckPicker deckPicker, @NonNull Integer progress) {
            mIncreaseSinceLastUpdate += progress;
            // Increase each time at least a percent of card has been processed since last update
            if (mIncreaseSinceLastUpdate > mOnePercent) {
                deckPicker.mProgressDialog.incrementProgress(mIncreaseSinceLastUpdate);
                mIncreaseSinceLastUpdate = 0;
            }
        }

        @Override
        public void actualOnCancelled(@NonNull DeckPicker deckPicker) {
            deckPicker.mEmptyCardTask = null;
        }

        /**
         * @param deckPicker
         * @param cids Null if it is cancelled (in this case we should not have called this method) or a list of cids
         */
        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, @Nullable List<Long> cids) {
            deckPicker.mEmptyCardTask = null;
            if (cids == null) {
                return;
            }
            if (cids.isEmpty()) {
                deckPicker.showSimpleMessageDialog(deckPicker.getResources().getString(R.string.empty_cards_none));
            } else {
                String msg = String.format(deckPicker.getResources().getString(R.string.empty_cards_count), cids.size());
                ConfirmationDialog dialog = new ConfirmationDialog();
                dialog.setArgs(msg);
                Runnable confirm = () -> {
                    deckPicker.getCol().remCards(cids);
                    UIUtils.showSimpleSnackbar(deckPicker, String.format(
                            deckPicker.getResources().getString(R.string.empty_cards_deleted), cids.size()), false);
                };
                dialog.setConfirm(confirm);
                deckPicker.showDialogFragment(dialog);
            }

            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
        }
    }

    public void createSubDeckDialog(long did) {
        CreateDeckDialog createDeckDialog = new CreateDeckDialog(DeckPicker.this, R.string.create_subdeck, CreateDeckDialog.DeckDialogType.SUB_DECK, did);
        createDeckDialog.setOnNewDeckCreated((i) -> {
            // a deck was created
            dismissAllDialogFragments();
            mDeckListAdapter.notifyDataSetChanged();
            updateDeckList();
            if (mFragmented) {
                loadStudyOptionsFragment(false);
            }
        });
        createDeckDialog.showDialog();
    }

    @VisibleForTesting
    public int getDeckCount() {
        return mDeckListAdapter.getItemCount();
    }

    @VisibleForTesting
    class CheckDatabaseListener extends TaskListener<String, Pair<Boolean, Collection.CheckDatabaseResult>> {
        @Override
        public void onPreExecute() {
            mProgressDialog = StyledProgressDialog.show(DeckPicker.this, AnkiDroidApp.getAppResources().getString(R.string.app_name),
                    getResources().getString(R.string.check_db_message), false);
        }


        @Override
        public void onPostExecute(Pair<Boolean, Collection.CheckDatabaseResult> result) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (result == null) {
                handleDbError();
                return;
            }

            Collection.CheckDatabaseResult databaseResult = result.second;

            if (databaseResult == null) {
                if (result.first) {
                    Timber.w("Expected result data, got nothing");
                } else {
                    handleDbError();
                }
                return;
            }

            if (!result.first || databaseResult.getFailed()) {
                if (databaseResult.getDatabaseLocked()) {
                    handleDbLocked();
                } else {
                    handleDbError();
                }
                return;
            }


            int count = databaseResult.getCardsWithFixedHomeDeckCount();
            if (count != 0) {
                String message = getResources().getString(R.string.integrity_check_fixed_no_home_deck, count);
                UIUtils.showThemedToast(DeckPicker.this,  message, false);
            }

            String msg;
            long shrunkInMb = Math.round(databaseResult.getSizeChangeInKb() / 1024.0);
            if (shrunkInMb > 0.0) {
                msg = getResources().getString(R.string.check_db_acknowledge_shrunk, (int) shrunkInMb);
            } else {
                msg = getResources().getString(R.string.check_db_acknowledge);
            }
            // Show result of database check and restart the app
            showSimpleMessageDialog(msg, true);
        }


        @Override
        public void onProgressUpdate(String message) {
            mProgressDialog.setContent(message);
        }
    }


    /**
     * Check if at least one deck is being displayed.
     */
    public boolean hasAtLeastOneDeckBeingDisplayed() {
        return mDeckListAdapter.getItemCount() > 0 && mRecyclerViewLayoutManager.getChildAt(0) != null;
    }

    private enum DeckSelectionType {
        /** Show study options if fragmented, otherwise, review */
        DEFAULT,
        /** Always show study options (if the deck counts are clicked) */
        SHOW_STUDY_OPTIONS,
        /** Always open reviewer (keyboard shortcut) */
        SKIP_STUDY_OPTIONS
    }
}
