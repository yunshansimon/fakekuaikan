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
import android.app.Fragment;

import android.support.v4.util.LruCache;

import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link dailyOnFragmentInteractionListener}
 * interface.
 */
public class DailyListFragment extends Fragment implements ListView.OnItemClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TITLE_PATH = "title";
    private static final String CNAME="DailyFm";
    // TODO: Rename and change types of parameters
    private String daily_json_file;
    private String base_path;
    private int screen_width;
    private int screen_height;
    private Bitmap mPlaceHolderBitmap;
    private LruCache<String, Bitmap> mMemoryCache;
    private dailyOnFragmentInteractionListener mListener;
    private Context context;
    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;
    private List<JSONObject> mJsonList;

    // TODO: Rename and change types of parameters
    public static DailyListFragment newInstance(String param1 , String param2) {
        DailyListFragment fragment = new DailyListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2,param2);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DailyListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size= new Point();
        display.getSize(size);
        screen_width = size.x;
        screen_height = size.y;
        if (getArguments() != null) {
            daily_json_file = getArguments().getString(ARG_PARAM1);
            base_path=getArguments().getString(ARG_PARAM2);
        }
        mPlaceHolderBitmap= BitmapFactory.decodeResource(getResources(),R.drawable.kuaikuai_waiting);
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
        // TODO: Change Adapter to display your content
        mJsonList=new ArrayList<JSONObject>();

        try {
            File dailyFile=new File(daily_json_file);

         //   CommonLab.log(CNAME,"daily file path:" + daily_json_file + " " + (dailyFile.exists()?"exists":"no exists"));
            StringBuilder sb=new StringBuilder();
            BufferedReader bfR=new BufferedReader(new InputStreamReader(new FileInputStream(dailyFile)));
            String line;
           while ((line=bfR.readLine())!=null){
               sb.append(line+ '\n');
           }
            bfR.close();
        //    CommonLab.log(CNAME,"cb string:" + sb.toString());
            JSONObject mJson=(JSONObject) (new JSONTokener(sb.toString())).nextValue();
        //    CommonLab.log(CNAME,"mJson length:"+ String.valueOf(mJson.length()));
           // JSONObject mJson= (new JSONObject(cb.toString())).getJSONObject("data");
            JSONArray mJarry=mJson.getJSONObject("data").getJSONArray("comics");
            for (int i=0;i<mJarry.length();i++){
                mJsonList.add(mJarry.getJSONObject(i));
         //       CommonLab.log(CNAME,"mJarry add:" + mJarry.getJSONObject(i).toString());
            }

        }catch (IOException e){
            CommonLab.log(CNAME,"DailyJsonFile is not exists.");
        }catch (JSONException e){
            CommonLab.log(CNAME,"Error json format of daily_json_file.");
        }
        mAdapter = new DailyTitleApdapter(getActivity().getBaseContext(),mJsonList);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dailylist, container, false);

        // Set the adapter
        mListView = (ListView) view.findViewById(R.id.listview);

        mListView.setAdapter(mAdapter);
     //   CommonLab.log(CNAME,"setAdapter mAdapter");
      //  CommonLab.log(CNAME,"mlist:" + String.valueOf(mListView.getAdapter().getCount()));
        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
     //   CommonLab.log(CNAME,view.toString());
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context=activity.getBaseContext();
        try {
            mListener = (dailyOnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            try {
                mListener.onDailyFragmentInteraction(mJsonList.get(position).getString(CommonLab.COMIC_URL));
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
   /* public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }*/

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface dailyOnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onDailyFragmentInteraction(String strUrl);
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
        final Bitmap bitmap = getBitmapFromMemCache(url);
        if (bitmap!=null){
            imageView.setImageBitmap(bitmap);
        }else{
            if (cancelPotentialImageWork(url, imageView)) {
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(getResources(), mPlaceHolderBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(url,String.valueOf(screen_width),String.valueOf(0));
            }
        }
//        if (cancelPotentialImageWork(url, imageView)) {
//            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
//            final AsyncDrawable asyncDrawable =
//                    new AsyncDrawable(getResources(), mPlaceHolderBitmap, task);
//            imageView.setImageDrawable(asyncDrawable);
//            task.execute(url,String.valueOf(imageView.getWidth()),String.valueOf(imageView.getHeight()));
//        }
    }
    public static boolean cancelPotentialImageWork(String data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.imageUrl;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == "" || bitmapData != data) {
                // Cancel previous task
                CommonLab.log(CNAME,"Cancel ImageWork:"+data);
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
                return CommonLab.decodeBitmapFromImageFile(imagePath, Integer.valueOf(params[1]), Integer.valueOf(params[2]));
            }else{
      //          CommonLab.log(CNAME,"download from url:" + imageUrl);
                CommonLab.downloadFile(imageUrl,imagePath,true);
                return CommonLab.decodeBitmapFromImageFile(imagePath, Integer.valueOf(params[1]), Integer.valueOf(params[2]));
            }

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

        return base_path + result;
    }

    private class DailyTitleApdapter extends ArrayAdapter<JSONObject>  {

        private LayoutInflater myInflater;

        public DailyTitleApdapter(Context context, List<JSONObject> titleList){
            super(context,R.layout.daily_list_detail,R.id.textTitleUp,titleList);
    //        CommonLab.log(CNAME, "Init DailyTitleApdapter");
    //        CommonLab.log(CNAME,"The Adpapter has "+ String.valueOf(getCount()));


            myInflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
          //  CommonLab.log(CNAME,"Add new view position:"+ String.valueOf(position));
            ViewHolder vh;
            View rowView=convertView;
            if (rowView==null){

                rowView=myInflater.inflate(R.layout.daily_list_detail,null);

                vh=new ViewHolder();
                vh.txtTitleUp=(TextView) rowView.findViewById(R.id.textTitleUp);
                vh.txtTitleDown=(TextView) rowView.findViewById(R.id.textTitleDown);
                vh.txtNumber=(TextView) rowView.findViewById(R.id.textNumber);
                vh.imgTitle=(ImageView) rowView.findViewById(R.id.imageTitle);
                rowView.setTag(vh);
            }else {
                vh=(ViewHolder)rowView.getTag();
            }
            try {
                vh.txtTitleUp.setText(getItem(position).getJSONObject(CommonLab.TOPIC).getString(CommonLab.TITLE_UP));
         //       CommonLab.log(CNAME,vh.txtTitleUp.getText().toString());
                vh.txtTitleDown.setText(this.getItem(position).getString(CommonLab.TITLE_DOWN));
                vh.txtNumber.setText(this.getItem(position).getString(CommonLab.COMMENT_NUM));
                loadBitmap(getItem(position).getString(CommonLab.COVER_IMAGE_URL),vh.imgTitle);
            }catch (JSONException e){
                e.printStackTrace();
            }
            return rowView;
        }


    }
    public class ViewHolder{
        public TextView txtTitleUp;
        public TextView txtTitleDown;
        public TextView txtNumber;
        public ImageView imgTitle;
    }
}
