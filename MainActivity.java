package com.desperate.pez_android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.androidadvance.topsnackbar.TSnackbar;
import com.desperate.pez_android.fragment.ChangePasswordFragment;
import com.desperate.pez_android.fragment.PayFragment;
import com.desperate.pez_android.fragment.SaldoFragment;
import com.desperate.pez_android.other.MyJsonPostRequest;
import com.desperate.pez_android.other.NetworkReceiver;
import com.desperate.pez_android.fragment.HomeFragment;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.desperate.pez_android.R;
import com.desperate.pez_android.fragment.MeterFragment;
import com.desperate.pez_android.fragment.NotificationsFragment;
import com.desperate.pez_android.fragment.PaymentsFragment;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;


public class MainActivity extends AppCompatActivity implements NotificationsFragment.OnFragmentInteractionListener {

    private NavigationView navigationView;
    private DrawerLayout drawer;
    private View navHeader;
    private ImageView imgNavHeaderBg;
    private TextView txtName, txtWebsite, toolbarText;
    private Toolbar toolbar;
    private AppBarLayout appbar;
    CoordinatorLayout coordinatorLayout;

    // urls to load navigation header background image
    // and profile image
    private static final String urlNavHeaderBg = "https://www.energo.pl.ua/wp-content/uploads/2019/02/logo-pez-e1549604715120.png";

    // index to identify current nav menu item
    public static int navItemIndex = 0;

    // tags used to attach the fragments
    private static final String TAG_HOME = "home";
    private static final String TAG_PAY = "pay";
    private static final String TAG_PAYMENTS = "payments";
    private static final String TAG_SALDO = "saldo";
    private static final String TAG_METER = "meters";
    private static final String TAG_NOTIFICATION = "notifications";
    private static final String TAG_CHANGE_PASSWORD = "change_password";
    public static String CURRENT_TAG = TAG_HOME;

    // toolbar titles respected to selected nav menu item
    private String[] activityTitles;

    // flag to load home fragment when user presses back key
    private boolean shouldLoadHomeFragOnBackPress = true;
    private Handler mHandler;

    private int userId;
    private String sessionId;

    BroadcastReceiver broadcastReceiver;
    TSnackbar snackbar;
    boolean showSnackbar = false;
    SharedPreferences userPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/fontfabric_bold.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );



        coordinatorLayout = findViewById(R.id.coordinator_layout);

        /*FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("tag", "getInstanceId failed", task.getException());
                        return;
                    }

                    // Get new Instance ID token
                    String token = task.getResult().getToken();

                    // Log and toast
                    String msg = getString(R.string.msg_token_fmt, token);
                   Log.e("tagGGG", msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });*/

        FirebaseMessaging.getInstance().subscribeToTopic("all");
        broadcastReceiver = new NetworkReceiver() {
            @Override
            public void onNetworkChange() {
                controlSnackbar();
            }
        };

        SharedPreferences pref = getSharedPreferences("USER" , MODE_PRIVATE);
        Boolean isLoggedIn = pref.getBoolean("ISLOGGEDIN", false);

        if (!isLoggedIn) {
            finish();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }

        else {

            appbar = findViewById(R.id.appbar);
            toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbarText = (TextView) toolbar.findViewById(R.id.toolbar_title);
            toolbar.setTitle("");
            setSupportActionBar(toolbar);

            mHandler = new Handler();

            drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            navigationView = (NavigationView) findViewById(R.id.nav_view);

            // Navigation view header
            navHeader = navigationView.getHeaderView(0);
            //txtWebsite = (TextView) navHeader.findViewById(R.id.website);

            imgNavHeaderBg = (ImageView) navHeader.findViewById(R.id.img_header_bg);

            // load toolbar titles from string resources
            activityTitles = getResources().getStringArray(R.array.nav_item_activity_titles);

            // load nav menu header data
            //loadNavHeader();

            // initializing navigation menu
            setUpNavigationView();

            onConfigurationChanged(getResources().getConfiguration());

            if (savedInstanceState == null) {
                navItemIndex = 0;
                CURRENT_TAG = TAG_HOME;
                loadHomeFragment();
            }
        }
    }

    public void controlSnackbar() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > 22) {
            //cm.getNetworkCapabilities(cm.getActiveNetwork()).hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            if (cm.getActiveNetwork() != null) {
                if ( showSnackbar == true) {
                    snackbar = TSnackbar.make(coordinatorLayout, "Підключено", TSnackbar.LENGTH_SHORT);
                    generateSnackbar(snackbar);
                    updateUI();
                }
            } else {
                showSnackbar = true;
                snackbar = TSnackbar.make(coordinatorLayout, "Відсутнє з'єднання з інтернетом", TSnackbar.LENGTH_INDEFINITE);
                generateSnackbar(snackbar);
            }
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null) {
                if ( showSnackbar == true) {
                    snackbar = TSnackbar.make(coordinatorLayout, "Підключено", TSnackbar.LENGTH_SHORT);
                    generateSnackbar(snackbar);
                    updateUI();
                }
            } else {
                showSnackbar = true;
                snackbar = TSnackbar.make(coordinatorLayout, "Відсутнє з'єднання з інтернетом", TSnackbar.LENGTH_INDEFINITE);
                generateSnackbar(snackbar);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        selectNavMenu();

        setToolbarTitle();

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(broadcastReceiver, filter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (broadcastReceiver != null) {
            showSnackbar = false;
            unregisterReceiver(broadcastReceiver);
        }

    }
    /***
     * Load navigation menu header information
     * like background image,
     * name, website, notifications action view (dot)
     */
    private void loadNavHeader() {
        // website
       // txtWebsite.setText("www.energo.pl.ua");

        // loading header background image
        /*Glide.with(this).load(urlNavHeaderBg).transition(new DrawableTransitionOptions().crossFade())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgNavHeaderBg);*/

        // showing dot next to notifications label
       // navigationView.getMenu().getItem(4).setActionView(R.layout.menu_dot);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            appbar.getLayoutParams().height = AppBarLayout.LayoutParams.WRAP_CONTENT;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            appbar.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
            //toolbarText.setGravity(View.TEXT_ALIGNMENT_CENTER);
        }
    }

    /***
     * Returns respected fragment that user
     * selected from navigation menu
     */
    private void loadHomeFragment() {
        // selecting appropriate nav menu item
        selectNavMenu();

        // set toolbar title
        setToolbarTitle();

        // if user select the current navigation menu again, don't do anything
        // just close the navigation drawer
        if (getSupportFragmentManager().findFragmentByTag(CURRENT_TAG) != null) {
            drawer.closeDrawers();

            return;
        }

        // Sometimes, when fragment has huge data, screen seems hanging
        // when switching between navigation menus
        // So using runnable, the fragment is loaded with cross fade effect
        // This effect can be seen in GMail app
        Runnable mPendingRunnable = () -> {
            // update the main content by replacing fragments
            Fragment fragment = getHomeFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
                    android.R.anim.fade_out);
            fragmentTransaction.replace(R.id.frame, fragment, CURRENT_TAG);
            fragmentTransaction.commitAllowingStateLoss();
        };

        // If mPendingRunnable is not null, then add to the message queue
        if (mPendingRunnable != null) {
            mHandler.post(mPendingRunnable);
        }

        //Closing drawer on item click
        drawer.closeDrawers();

        // refresh toolbar menu
        invalidateOptionsMenu();
    }

    private Fragment getHomeFragment() {
        switch (navItemIndex) {

            case 0:
                // home
              //  Bundle bundle = new Bundle();
               // bundle.putInt("userId", userId);
              //  bundle.putString("sessionId", sessionId);
                HomeFragment homeFragment = new HomeFragment();
               // homeFragment.setArguments(bundle);
                return homeFragment;
            case 1:
                // watch payments
                PaymentsFragment paymentsFragment = new PaymentsFragment();
                return paymentsFragment;
            case 2:
                // meter
                MeterFragment meterFragment = new MeterFragment();
                return meterFragment;
            case 3:
                // saldo
                SaldoFragment saldoFragment = new SaldoFragment();
                return saldoFragment;
            case 5:
                NotificationsFragment notificationsFragment = new NotificationsFragment();
                return notificationsFragment;
            case 7:
                ChangePasswordFragment changePasswordFragment = new ChangePasswordFragment();
                return changePasswordFragment;
            default:
                return new HomeFragment();
        }
    }

    private void setToolbarTitle() {
        if (activityTitles != null)
        toolbarText.setText(activityTitles[navItemIndex]);
    }

    private void selectNavMenu() {
       // if(navItemIndex!=4)
        navigationView.getMenu().getItem(navItemIndex).setChecked(true);
    }

    private void setUpNavigationView() {
        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        // This method will trigger on item Click of navigation menu
        navigationView.setNavigationItemSelectedListener(menuItem -> {

            //Check to see which item was being clicked and perform appropriate action
            switch (menuItem.getItemId()) {
                //Replacing the main content with ContentFragment Which is our Inbox View;
                case R.id.nav_home:
                    navItemIndex = 0;
                    CURRENT_TAG = TAG_HOME;
                    break;
                case R.id.nav_payments:
                    navItemIndex = 1;
                    CURRENT_TAG = TAG_PAYMENTS;
                    break;
                case R.id.nav_meter:
                    navItemIndex = 2;
                    CURRENT_TAG = TAG_METER;
                    break;
                case R.id.nav_saldo:
                    navItemIndex = 3;
                    CURRENT_TAG = TAG_SALDO;
                    break;
                    //startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://my-payments.privatbank.ua/mypayments/customauth/identification/fp/static?staticToken=46b89c52535b7586a82bd6890c4694e95hk49mqw")));
                    //return true;
                case R.id.nav_notifications:
                    navItemIndex = 5;
                    CURRENT_TAG = TAG_NOTIFICATION;
                    break;
                case R.id.nav_about_us:
                    // launch new intent instead of loading fragment
                    startActivity(new Intent(MainActivity.this, AboutUsActivity.class));
                    drawer.closeDrawers();
                    return true;
                case R.id.nav_online_help:
                    // launch new intent instead of loading fragment
                    startActivity(new Intent(MainActivity.this, OnlineHelpActivity.class));
                    drawer.closeDrawers();
                    return true;
                case R.id.nav_change_password:
                    navItemIndex = 7;
                    CURRENT_TAG = TAG_CHANGE_PASSWORD;
                    break;
                case R.id.exit:
                    getApplicationContext().getSharedPreferences("USER", MODE_PRIVATE).edit().clear().commit();
                    getApplicationContext().getSharedPreferences("SPINNER", MODE_PRIVATE).edit().clear().commit();
                    getApplicationContext().getSharedPreferences("ISLOGGEDIN", MODE_PRIVATE).edit().putBoolean("ISLOGGEDIN", false).commit();
                    getApplicationContext().getSharedPreferences("NOTIFICATION", MODE_PRIVATE).edit().clear().commit();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    return true;
                default:
                    navItemIndex = 0;
            }

            //Checking if the item is in checked state or not, if not make it in checked state
            if (menuItem.isChecked()) {
                menuItem.setChecked(false);
            }
            else {
                menuItem.setChecked(true);
            }
            menuItem.setChecked(true);

            loadHomeFragment();

            return true;
        });

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank
                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawer.addDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessary or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawers();
            return;
        }

        // This code loads home fragment when back key is pressed
        // when user is in other fragment than home
        if (shouldLoadHomeFragOnBackPress) {
            // checking if user is on other navigation menu
            // rather than home
            if (navItemIndex != 0) {
                navItemIndex = 0;
                CURRENT_TAG = TAG_HOME;
                loadHomeFragment();
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        // show menu only when home fragment is selected
//        if (navItemIndex == 0) {
//            getMenuInflater().inflate(R.menu.main, menu);
//        }

        // when fragment is notifications, load the menu created for notifications
        if (navItemIndex == 6) {
            getMenuInflater().inflate(R.menu.notifications, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            getApplicationContext().getSharedPreferences("USER", MODE_PRIVATE).edit().clear().commit();
            getApplicationContext().getSharedPreferences("SPINNER", MODE_PRIVATE).edit().clear().commit();
            getApplicationContext().getSharedPreferences("ISLOGGEDIN", MODE_PRIVATE).edit().putBoolean("ISLOGGEDIN", false).commit();
            getApplicationContext().getSharedPreferences("NOTIFICATION", MODE_PRIVATE).edit().clear().commit();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        // user is in notifications fragment
        // and selected 'Mark all as Read'
        /*if (id == R.id.action_mark_all_read) {
            Toast.makeText(getApplicationContext(), "All notifications marked as read!", Toast.LENGTH_LONG).show();
        }*/

        // user is in notifications fragment
        // and selected 'Clear All'
        if (id == R.id.action_clear_notifications) {
            onClearListButtonClick();
        }

        return super.onOptionsItemSelected(item);
    }

    public void generateSnackbar(TSnackbar snackbar) {
        View view = snackbar.getView();
        TextView snackbarText = view.findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snackbarText.setTextColor(ContextCompat.getColor(getApplicationContext(),
                R.color.colorAccent));
        snackbar.show();
    }

//дописать для каждого фрагмента?
    private void updateUI() {
        HomeFragment fragment = new HomeFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
                android.R.anim.fade_out);
        fragmentTransaction.replace(R.id.frame, fragment, CURRENT_TAG);
        fragmentTransaction.commitAllowingStateLoss();
       // if(fragment != null && fragment.isAdded()) {
       //     userPref = getSharedPreferences("USER" , Context.MODE_PRIVATE);
       //     userId = userPref.getInt("userId", 0);
       //     fragment.getUserInfo(userId);
       // }
    }

    @Override
    public void onClearListButtonClick() {
        NotificationsFragment fragment = (NotificationsFragment) getSupportFragmentManager()
                .findFragmentByTag(CURRENT_TAG);
        if(fragment != null && fragment.isAdded()) {
            SharedPreferences pref = getSharedPreferences("NOTIFICATION" , MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString("notifications", "");
            edit.commit();
            fragment.clearNotificationList();
        }

    }

}
