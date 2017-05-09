package com.example.wei.girlpicture;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "MainActivity";

    @BindView(R.id.list_view)
    ListView mlistView;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mySwipeRefreshLayout;

    private Gson mGson = new Gson();

    private List<ResultBean.ResultsBean> mData = new ArrayList<ResultBean.ResultsBean>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mySwipeRefreshLayout.setOnRefreshListener(this);
        mySwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);
        // sendSyncRequest();
        sendAsyncRequest(true,page);

        mlistView.setAdapter(mBaseAdapter);
        /*设置条目滚动监听*/
        mlistView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //当ListView停稳之后判断是否滑到底部
                if (scrollState == SCROLL_STATE_IDLE) {
                    if (mlistView.getLastVisiblePosition() == mData.size() - 1) {
                        //要加载更多数据
                        loadMoreData();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
    }

    private void loadMoreData() {
        sendAsyncRequest(true,++page);
    }


    private BaseAdapter mBaseAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                convertView = View.inflate(MainActivity.this, R.layout.list_item, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Log.d(TAG, "getView: " + mData.get(position).getPublishedAt());
            //刷新发布时间
            viewHolder.mPublish.setText(mData.get(position).getPublishedAt());
            //刷新网络图片
            String url = mData.get(position).getUrl();
            Glide.with(MainActivity.this).load(url).bitmapTransform(new CropCircleTransformation(MainActivity.this)).into(viewHolder.mImage);
            return convertView;
        }
    };

    public class ViewHolder {
        private ImageView mImage;
        private TextView mPublish;

        public ViewHolder(View root) {
            mImage = (ImageView) root.findViewById(R.id.image);
            mPublish = (TextView) root.findViewById(R.id.publish);
        }
    }

    /*
    * 下拉刷新
    * */
    private int page = 1;

    @Override
    public void onRefresh() {
        //mData.clear();
        ++page;
        sendAsyncRequest(false,page);
        mySwipeRefreshLayout.setRefreshing(false);
    }

    /*
    * 异步请求
    * */
    private void sendAsyncRequest(final boolean up, int i) {
        OkHttpClient okHttpClient = new OkHttpClient();
        //创建一个请求
        String url = "http://gank.io/api/data/%E7%A6%8F%E5%88%A9/10/" + i;
        Request request = new Request.Builder().get().url(url).build();
        //发送异步请求,不需要等待网络结果就执行后续代码,内部是在子线程中执行网络请求
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: ");
            }

            //在子线程
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resultString = response.body().string();
                ResultBean resultBean = mGson.fromJson(resultString, ResultBean.class);
                Log.d(TAG, "onResponse: " + resultString);
                if (up){
                    mData.addAll(resultBean.getResults());
                }else{
                    mData.addAll(0,resultBean.getResults());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBaseAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
        Log.d(TAG, "sendAsyncRequest: ");
    }


    private void sendSyncRequest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient okHttpClient = new OkHttpClient();
                //创建一个请求
                String url = "http://gank.io/api/data/%E7%A6%8F%E5%88%A9/10/1";
                Request request = new Request.Builder().get().url(url).build();
                //发送同步请求
                try {
                    Response response = okHttpClient.newCall(request).execute();//会等待网络返回结果,然后执行后续代码
                    //打印结果
                    String resultString = response.body().string();
                    Log.d(TAG, "sendSyncRequest:" + resultString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
