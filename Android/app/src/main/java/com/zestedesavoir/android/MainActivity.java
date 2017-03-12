package com.zestedesavoir.android;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.zestedesavoir.android.login.LoginFragment;
import com.zestedesavoir.android.login.managers.Session;
import com.zestedesavoir.android.notification.NotificationsFragment;
import com.zestedesavoir.android.notification.managers.NotificationsManager;
import com.zestedesavoir.android.notification.services.NotificationService;
import com.zestedesavoir.android.notification.services.StarterReceiver;
import com.zestedesavoir.android.settings.SettingsFragment;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity implements OnNavigationListener {
    private static final String STACK_MAIN = "Main";

    @Inject
    Session session;

    @Inject
    NotificationsManager manager;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @NonNull
    private CompositeSubscription subscription = new CompositeSubscription();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);
        ((ZdSApplication) getApplicationContext()).getAppComponent().inject(this);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (subscription.isUnsubscribed()) {
            subscription = new CompositeSubscription();
        }
        subscription.add(session.isAuthenticated()
                .flatMap(aBoolean -> manager.clear().map(aVoid -> aBoolean))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    if (!aBoolean) {
                        goToLoginScreen();
                    } else {
                        goToNotificationScreen();
                    }
                })
        );
        final NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NotificationService.NOTIFICATION_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();
        subscription.clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendBroadcast(StarterReceiver.getStartIntent(getApplicationContext()));
    }

    @Override
    public void goToLoginScreen() {
        subscription.add(session.disconnect()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
                    toolbar.setVisibility(View.GONE);
                    goTo(LoginFragment.newInstance(session), LoginFragment.TAG, null);
                })
        );
    }

    @Override
    public void goToNotificationScreen() {
        toolbar.setVisibility(View.VISIBLE);
        goTo(NotificationsFragment.newInstance(manager), NotificationsFragment.TAG, null);
    }

    @Override
    public void goToSettingsScreen() {
        toolbar.setVisibility(View.VISIBLE);
        goTo(SettingsFragment.newInstance(session), SettingsFragment.TAG);
    }

    @Override
    public void back() {
        getSupportFragmentManager().popBackStack();
    }

    private void goTo(Fragment fragment, String tag) {
        goTo(fragment, tag, tag);
    }

    private void goTo(Fragment fragment, String tag, String nameStack) {
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content, fragment, tag);
        if (nameStack != null) {
            ft.addToBackStack(nameStack);
        }
        ft.commit();
    }
}
