package com.example.xyzreader.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;
    private long mSelectedItemId;
    private int mTopInset;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private boolean mIsHiding;

    @Bind(R.id.pager) ViewPager mPager;
    @Bind(R.id.up_container) FrameLayout mUpButtonContainer;
    @Bind(R.id.action_up) ImageButton mUpButton;
    @Bind(R.id.share_fab) FloatingActionButton mFab;
    MyPagerAdapter mPagerAdapter;

    ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (positionOffset == 0) {
                showFab();
            } else {
                hideFab();
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (mCursor != null) {
                mCursor.moveToPosition(position);
            }
            mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
            updateUpButtonPosition();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mUpButton.animate()
                    .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                    .setDuration(300);
        }
    };

    @OnClick(R.id.action_up)
    public void onUpClick() {
        onSupportNavigateUp();
    }

    @OnClick(R.id.share_fab)
    public void onShareClick() {
        startActivity(Intent.createChooser(
                ShareCompat.IntentBuilder.from(this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(),
                getString(R.string.action_share)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.onApplyWindowInsets(windowInsets);
                    mTopInset = windowInsets.getSystemWindowInsetTop();
                    mUpButtonContainer.setTranslationY(mTopInset);
                    updateUpButtonPosition();
                    return windowInsets;
                }
            });
        }

        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int)TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPager.removeOnPageChangeListener(mOnPageChangeListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPager.addOnPageChangeListener(mOnPageChangeListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    void hideFab() {
        if (!mIsHiding) {
            if (ViewCompat.isLaidOut(mFab) && !mFab.isInEditMode()) {
                mFab.animate().scaleX(0.0F)
                        .scaleY(0.0F)
                        .alpha(0.0F)
                        .setDuration(200L)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            public void onAnimationCancel(Animator animation) {
                            }

                            public void onAnimationEnd(Animator animation) {
                                mFab.setVisibility(View.GONE);
                            }

                            public void onAnimationStart(Animator animation) {
                                mFab.setVisibility(View.VISIBLE);
                            }
                        });
            } else {
                mFab.setVisibility(View.GONE);
            }
            mIsHiding = true;
        }
    }

    void showFab() {
        if (mIsHiding) {
            if (ViewCompat.isLaidOut(mFab) && !mFab.isInEditMode()) {
                mFab.setAlpha(0.0F);
                mFab.setScaleY(0.0F);
                mFab.setScaleX(0.0F);
                mFab.animate()
                        .scaleX(1.0F)
                        .scaleY(1.0F)
                        .alpha(1.0F)
                        .setDuration(200L)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            public void onAnimationStart(Animator animation) {
                                mFab.setVisibility(View.VISIBLE);
                            }
                        });
            } else {
                mFab.setVisibility(View.VISIBLE);
                mFab.setAlpha(1.0F);
                mFab.setScaleY(1.0F);
                mFab.setScaleX(1.0F);
            }
        }
        mIsHiding = false;
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment)object;
            if (fragment != null) {
                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
                updateUpButtonPosition();
            }
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
