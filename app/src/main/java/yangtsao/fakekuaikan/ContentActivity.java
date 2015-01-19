package yangtsao.fakekuaikan;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ContentActivity extends Activity {
    private String strCommic_Url;
    private String strBase_Path;
    private String strComment_Url;
    private int screen_width;
    private int screen_height;
    private Bitmap mPlaceHolderBitmap;
    private ListView mListView;
    private LruCache<String, Bitmap> mMemoryCache;
    private Float preX,preY;
   // private WeakReference<DownLoadCommentTask> wrfCommentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        ((Button) findViewById(R.id.butContent)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               goBack();
            }
        });

        Bundle b=getIntent().getExtras();
        strCommic_Url=getApiUrlFromUrl(b.getString(CommonLab.COMIC_URL));
        CommonLab.log("Oncreate","load comic url:" + strCommic_Url);
        strBase_Path=b.getString(CommonLab.BASE_PATH);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screen_width = size.x;
        screen_height = size.y;
        mPlaceHolderBitmap= BitmapFactory.decodeResource(getResources(), R.drawable.kuaikuai_waiting);
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        //  CommonLab.log(CNAME,"cachesize:"+ String.valueOf(cacheSize) + "KB");
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

        mListView=(ListView) findViewById(R.id.lvContent);
        InitListViewTask task=new InitListViewTask(getBaseContext(),getLayoutInflater(),mListView);
        task.execute(strCommic_Url);


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {


        return true;
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_content, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ComicAdapter extends ArrayAdapter<String>{
        private LayoutInflater myInflater;

        public ComicAdapter(Context context, List<String> comicList){
            super(context,R.layout.commic_detail,R.id.textInvisible,comicList);
            //        CommonLab.log(CNAME, "Init DailyTitleApdapter");
            //        CommonLab.log(CNAME,"The Adpapter has "+ String.valueOf(getCount()));


            myInflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }



        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //  CommonLab.log(CNAME,"Add new view position:"+ String.valueOf(position));
            ComicViewHolder vh;
            View rowView=convertView;
            if (rowView==null){

                rowView=myInflater.inflate(R.layout.commic_detail,null);

                vh=new ComicViewHolder();

                vh.imgView=(ImageView) rowView.findViewById(R.id.imgComic);

                rowView.setTag(vh);
            }else {
                vh=(ComicViewHolder)rowView.getTag();
            }

            CommonLab.log("loadbitmap","load:"+getItem(position));
            loadBitmap(getItem(position), vh.imgView);

            return rowView;
        }
    }

    public class ComicViewHolder {
        public ImageView imgView;
    }

    private class CommentAdapter extends ArrayAdapter<JSONObject>{
        private LayoutInflater myInflater;
        public CommentAdapter (Context context, List<JSONObject> comicList){
            super(context,R.layout.comment_list_detail,R.id.CommentName,comicList);
            myInflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CommentViewHolder vh;
            View rowView=convertView;
            if(rowView==null){
                rowView=myInflater.inflate(R.layout.comment_list_detail,null);
                vh=new CommentViewHolder();
                vh.tvName=(TextView)rowView.findViewById(R.id.CommentName);
                vh.tvTime=(TextView)rowView.findViewById(R.id.CommentTime);
                vh.tvDetail=(TextView)rowView.findViewById(R.id.CommentDetail);
                vh.commentLogo=(ImageView)rowView.findViewById(R.id.imgCommentLogo);
                rowView.setTag(vh);

            }else{
                vh=(CommentViewHolder)rowView.getTag();
            }
            try {
                JSONObject jsonComment=getItem(position);
                CommonLab.log("content",jsonComment.toString());
                vh.tvName.setText(jsonComment.getJSONObject("user").getString("nickname"));
                vh.tvTime.setText(CommonLab.getDate(jsonComment.getLong("created_at")));
                vh.tvDetail.setText(jsonComment.getString("content"));
                loadLogoBitmap(jsonComment.getJSONObject("user").getString("avatar_url"), vh.commentLogo, R.drawable.kuaikan_reader_default_logo);
            }catch (JSONException e){
                e.printStackTrace();
            }
            return rowView;
        }
    }

    public class CommentViewHolder {
        public ImageView commentLogo;
        public TextView tvName;
        public TextView tvTime;
        public TextView tvDetail;

    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    public void loadBitmap(String url, ImageView imageView) {
        if (url.equalsIgnoreCase("null")) return;
        final Bitmap bitmap = getBitmapFromMemCache(url);
        if (bitmap!=null){
            imageView.setImageBitmap(bitmap);
        }else{
            if (cancelPotentialImageWork(url, imageView)) {
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(getResources(), mPlaceHolderBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(url,String.valueOf(imageView.getWidth()>0?imageView.getWidth():screen_width),String.valueOf(imageView.getHeight()));
            }
        }

    }
    public void loadLogoBitmap(String url, ImageView imageView, int resDrawble) {
        if (url==null || url.equalsIgnoreCase("null")) {
            String cacheIndex=String.valueOf(resDrawble);
            final Bitmap bitmap = getBitmapFromMemCache(url);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                Bitmap bitmapNew=BitmapFactory.decodeResource(getResources(),resDrawble);
                addBitmapToMemoryCache(cacheIndex,bitmapNew);
                imageView.setImageBitmap(bitmapNew);
            }
        }else {
            final Bitmap bitmap = getBitmapFromMemCache(url);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                if (cancelPotentialImageWork(url, imageView)) {
                    final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                    final AsyncDrawable asyncDrawable =
                            new AsyncDrawable(getResources(), mPlaceHolderBitmap, task);
                    imageView.setImageDrawable(asyncDrawable);
                    task.execute(url, String.valueOf(imageView.getWidth() > 0 ? imageView.getWidth() : screen_width), String.valueOf(imageView.getHeight()));
                }
            }
        }

    }
    public static boolean cancelPotentialImageWork(String data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.imageUrl;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == "" || bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String imageUrl;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            imageUrl = params[0];
            String imagePath=getPathFromUrl(imageUrl);
            File imageFile=new File(imagePath);
            //      CommonLab.log(CNAME,"upload:"+imagePath);
            if (imageFile.exists() && imageFile.length()>0) {
                //          CommonLab.log(CNAME,"upload from file:"+ imagePath);

            }else{
                //          CommonLab.log(CNAME,"download from url:" + imageUrl);
                CommonLab.downloadFile(imageUrl,imagePath,true);

            }
            return CommonLab.decodeBitmapFromImageFile(imagePath, Integer.valueOf(params[1]), 0);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                    addBitmapToMemoryCache(imageUrl,bitmap);
                }
            }
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);

            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }
    public String getPathFromUrl(String urlStr){
        Pattern p=Pattern.compile("/[[a-z][0-9]]+/[[a-z][0-9]]+\\.jpg");
        Matcher m=p.matcher(urlStr);
        String result="";
        if (m.find()) {
            result=m.group();

        }

        return strBase_Path + result;
    }
    public String getJsonPathFromUrl(String urlStr){
        Pattern p=Pattern.compile("/[[a-z][0-9]]+/[[a-z][0-9]]+\\z");
        Matcher m=p.matcher(urlStr);
        String result="";
        if (m.find()) {
            result=m.group();

        }

        return strBase_Path + result;
    }

    public String getApiUrlFromUrl(String urlStr){
        Pattern p=Pattern.compile("/[[a-z][0-9]]+/[[a-z][0-9][.]]+\\z");
        Matcher m=p.matcher(urlStr);
        String result="";
        if (m.find()) {
            result=m.group();

        }

        return CommonLab.URL_API_PREFIX + result;
    }

    class InitListViewTask extends AsyncTask<String,Void,MergeAdapter>{
        private ListView mListView;
        private LayoutInflater mInflater;
        private Context mContext;
        public InitListViewTask(Context context, LayoutInflater myInflater, ListView lv){
            mListView=lv;
            mInflater=myInflater;
            mContext=context;
        }

        @Override
        protected MergeAdapter doInBackground(String... strings) {
            MergeAdapter mMergeAdapter=new MergeAdapter();
            String strComicUrl=strings[0];
            String strComicPath=getJsonPathFromUrl(strComicUrl);
            File comicJsonFile=new File(strComicPath);
            if (!comicJsonFile.exists()) {
                CommonLab.downloadFile(strComicUrl, strComicPath, false);
            }
            StringBuilder sbComic=new StringBuilder();
            int intCharCode;

            try {
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(comicJsonFile));
                while ((intCharCode=in.read())>-1){
                    sbComic.appendCodePoint(intCharCode);
                }
                in.close();
            }catch (IOException e){
                e.printStackTrace();
            }
            View comicTitleView=mInflater.inflate(R.layout.content_title,null);
            JSONObject jsData;
            JSONObject jsTopic,jsUser;
            JSONArray jsImages;
            List<String> imgUrls=new ArrayList<String>();
            CommonLab.log("content","sb:" + sbComic.toString());
            try{
                jsData=((JSONObject) ((new JSONTokener(sbComic.toString())).nextValue())).getJSONObject("data");
                jsTopic=jsData.getJSONObject("topic");
                jsUser=jsTopic.getJSONObject("user");
                ((TextView) comicTitleView.findViewById(R.id.txtContentTitle)).setText(jsTopic.getString("title"));
                ((TextView) comicTitleView.findViewById(R.id.txtContentTime)).setText(CommonLab.getDate(jsTopic.getLong("created_at")));
                ((TextView) comicTitleView.findViewById(R.id.txtContentAuthor)).setText(jsUser.getString("nickname"));
                loadLogoBitmap(jsUser.getString("avatar_url"), (ImageView) comicTitleView.findViewById(R.id.imgContentLogo), R.drawable.kuaikan_author_default_logo);
                jsImages=jsData.getJSONArray("images");
                for(int i=0;i<jsImages.length();i++){
                    imgUrls.add(jsImages.getString(i));
                }

            }catch (JSONException e){
                e.printStackTrace();
            }
            mMergeAdapter.addView(comicTitleView);
            ComicAdapter comicAdapter=new ComicAdapter(mContext,imgUrls);
            mMergeAdapter.addAdapter(comicAdapter);

            mMergeAdapter.addView(mInflater.inflate(R.layout.content_comment_title,null));
            /*List<JSONObject> mCommentList=new ArrayList<JSONObject>();
            try {
                mCommentList.add((JSONObject) (new JSONTokener(CommonLab.FAKE_COMMENT)).nextValue());
            }catch (JSONException e){
                e.printStackTrace();
            }
            CommentAdapter commentAdapter=new CommentAdapter(mContext,mCommentList);
            mMergeAdapter.addAdapter(commentAdapter);*/
            String strCommentUrl=strCommic_Url + CommonLab.COMMENT_PART_URL;
            String strComment=CommonLab.DownLoadUrltoString(strCommentUrl);
            List<JSONObject> listComments=new ArrayList<JSONObject>();
            try {
                JSONObject jsonData = ((JSONObject) (new JSONTokener(strComment)).nextValue()).getJSONObject("data");
                JSONArray jsonComments=jsonData.getJSONArray("comments");
                for(int i=0;i<jsonComments.length();i++){
                    listComments.add(jsonComments.getJSONObject(i));
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
            CommentAdapter commentAdapter=new CommentAdapter(mContext,listComments);
            mMergeAdapter.addAdapter(commentAdapter);

            return mMergeAdapter;
        }

        @Override
        protected void onPostExecute(MergeAdapter mMergeAdapter) {
            super.onPostExecute(mMergeAdapter);
            mListView.setDividerHeight(0);
            mListView.setFooterDividersEnabled(false);
            mListView.setHeaderDividersEnabled(false);
            mListView.setAdapter(mMergeAdapter);
            mListView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            preX=motionEvent.getX();
                            preY=motionEvent.getY();
                            CommonLab.log("TouchOn","touchbegin: X="+ String.valueOf(preX) + " y=" + String.valueOf(preY) );

                            break;
                        case MotionEvent.ACTION_UP:
                            break;

                        case MotionEvent.ACTION_MOVE:

                            float moveX = preX - motionEvent.getX();
                            CommonLab.log("TouchOn","touchmove: moveX="+ String.valueOf(moveX) + " moveY=" + String.valueOf(preY-motionEvent.getY()));
                            // 左滑
                            if (moveX > 150 && moveX < 5000) {
                                CommonLab.log("TouchOn","left slide.");

                            }
                            // 右滑
                            else if (moveX < -150 && moveX > -5000) {
                                goBack();
                                // mDesignClothesBackground
                                // .setBackgroundResource(idClothesBackground[1]);
                            }

                    }




                    return false;
                }
            });
           /* mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                    CommonLab.log("onScroll", "i:"+String.valueOf(i) + "i2:" + String.valueOf(i2) + "i3:" + String.valueOf(i3));
                    if ((i+i2)==(i3-2)){
                        if (wrfCommentTask==null || wrfCommentTask.get()==null) {
                            CommonLab.log("onScroll","onload comments");
                            DownLoadCommentTask task = new DownLoadCommentTask(getBaseContext(), mListView);
                            wrfCommentTask = new WeakReference<DownLoadCommentTask>(task);
                            task.execute(strCommic_Url + CommonLab.COMMENT_PART_URL);
                        }else{
                            CommonLab.log("onScroll","already onloading comments");
                        }
                    }
                }
            });*/
        }
    }
    /*class DownLoadCommentTask extends AsyncTask<String,Void, List<JSONObject>>{
        private Context mContext;
        private ListView mListView;
        public DownLoadCommentTask(Context context,ListView listView){
            mContext=context;
            mListView=listView;
        }

        @Override
        protected List<JSONObject> doInBackground(String... strings) {
            String strCommentUrl=strings[0];
            String strComment=CommonLab.DownLoadUrltoString(strCommentUrl);
            List<JSONObject> listComments=new ArrayList<JSONObject>();
            try {
                JSONObject jsonData = ((JSONObject) (new JSONTokener(strComment)).nextValue()).getJSONObject("data");
                JSONArray jsonComments=jsonData.getJSONArray("comments");
                for(int i=0;i<jsonComments.length();i++){
                    listComments.add(jsonComments.getJSONObject(i));
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
            return listComments;
        }

        @Override
        protected void onPostExecute(List<JSONObject> jsonObjects) {
            super.onPostExecute(jsonObjects);
            MergeAdapter mergeAdapter=(MergeAdapter)mListView.getAdapter();
            CommentAdapter commentAdapter=new CommentAdapter(mContext,jsonObjects);
            mergeAdapter.addAdapter(commentAdapter);
            mergeAdapter.notifyDataSetChanged();

        }
    }*/

    private void goBack(){
        finish();
        overridePendingTransition(R.anim.slide_in_main,R.anim.slide_out_detail);
    }
}
