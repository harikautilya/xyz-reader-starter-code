package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;
    private long mSelectedItemId;
    private MyPagerAdapter mPagerAdapter;
    private ViewPager viewPager;
    private RecyclerView detailOfArticles;

    private List<String> data;
    private ImageAdapter imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapse_toolbar);
        detailOfArticles = findViewById(R.id.image_iv);
        viewPager = findViewById(R.id.pager);
        data = new ArrayList<>();

        getSupportLoaderManager().initLoader(0, null, this);
        setSupportActionBar(toolbar);
        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mPagerAdapter);
        viewPager.setPageMargin((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        viewPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        imageAdapter = new ImageAdapter(this);

        Intent intent = getIntent();
        if (intent == null) {
            closeOnError();
        }

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                detailOfArticles.smoothScrollToPosition(position);
                collapsingToolbarLayout.setTitle("");
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(detailOfArticles);


        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();
        imageAdapter.notifyDataSetChanged();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Select the start ID
                int position = -1;
                if (mStartId > 0) {
                    mCursor.moveToFirst();

                    while (!mCursor.isAfterLast()) {

                        if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                            position = mCursor.getPosition();
                            break;
                        }
                        mCursor.moveToNext();
                    }
                    mStartId = 0;
                }

                final int finalPosition = position;
                ArticleDetailActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewPager.setCurrentItem(finalPosition, false);
                        populateToolbar(detailOfArticles, finalPosition);
                        imageAdapter.notifyDataSetChanged();
                    }
                });


            }
        }).start();


    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
        imageAdapter.notifyDataSetChanged();
    }


    @SuppressLint("ClickableViewAccessibility")
    private void populateToolbar(RecyclerView ingredientsIv, int currentPosition) {
        final LinearLayoutManager imageManger = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        ingredientsIv.setLayoutManager(imageManger);
        ingredientsIv.setAdapter(imageAdapter);
        ingredientsIv.scrollToPosition(currentPosition);
        ingredientsIv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Used to remove the touch of user related activity and still allow scrolling
                return true;
            }
        });
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
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }

    private void closeOnError() {
        finish();
        Toast.makeText(this, R.string.detail_error_message, Toast.LENGTH_SHORT).show();
    }

    public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageHolder> {

        private Context context;


        public ImageAdapter(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ImageHolder(LayoutInflater.from(context).inflate(R.layout.item_image, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ImageHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.bind(mCursor.getString(ArticleLoader.Query.PHOTO_URL));
        }

        @Override
        public int getItemCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }

        class ImageHolder extends RecyclerView.ViewHolder {

            ImageView sandwichImage;


            public ImageHolder(View itemView) {
                super(itemView);
                sandwichImage = itemView.findViewById(R.id.article_image);
            }

            public void bind(String image) {

                ImageLoaderHelper.getInstance(ArticleDetailActivity.this).getImageLoader()
                        .get(image, new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                                Bitmap bitmap = imageContainer.getBitmap();
                                if (bitmap != null) {
                                    sandwichImage.setImageBitmap(imageContainer.getBitmap());

                                }
                            }

                            @Override
                            public void onErrorResponse(VolleyError volleyError) {

                            }
                        });
            }
        }
    }
}
