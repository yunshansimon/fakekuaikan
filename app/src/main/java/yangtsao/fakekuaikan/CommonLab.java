package yangtsao.fakekuaikan;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;


public class CommonLab {
    private CommonLab(){
        throw new UnsupportedOperationException("This class is non-instantiable, so stop trying!");
    }

    public static final boolean IS_LOGGABLE                           = false;

    public final static String URL_DAILY_JSON="http://api.kuaikanmanhua.com/v1/comic_lists/1?limit=20&offset=0";
    public final static String URL_LIST_JSON="http://api.kuaikanmanhua.com/v1/topics?limit=20&offset=0";
    public final static String URL_LIST1_JSON="http://api.kuaikanmanhua.com/v1/topic_lists/1?limit=20&offset=0";
    public final static String BASE_PATH="base_path";
    public final static String JSON_PATH="json";
    public final static String COMIC_PATH="comic";
    public final static String DAILY_FILE="daily.json";
    public final static String LIST1_FILE="list1.json";
    public final static String LIST_FILE="list.json";
    public final static String URL_API_PREFIX="http://api.kuaikanmanhua.com/v1";
    public final static String COMMENT_PART_URL="/comments?limit=20&offset=0";

    public final static String ERROR="error";
    public final static String COMMENT_NUM="comments_count";          //每日推荐，漫画评论数量
    public final static String COVER_IMAGE_URL="cover_image_url";      //每日推荐，漫画封面网址
    public final static String TITLE_UP="title";                       //每日推荐，漫画上标题
    public final static String TITLE_DOWN="title";                    //每日推荐，漫画下标题
    public final static String TOPIC="topic";                           //包含配合上标题的json数组名
    public final static String COMIC_URL="url";                         //每日推荐，漫画网址




    public static void log(String tag, String message) {
        if (CommonLab.IS_LOGGABLE) {
            Log.d(tag, message);
        }
    }

    public static int downloadFile(String url, String dest_file_path ,Boolean isCover) {
        File dest_file = new File(dest_file_path);
        try {
          //  File dest_file = new File(dest_file_path);
            CommonLab.log("download", "Begin download from:" + url + "to path:" +dest_file_path);

            if (dest_file.exists() && !isCover) {
                return 0;
            }else if (dest_file.exists() && isCover) {
                dest_file.delete();
            }
            File parentDir=new File(getParentPathFormString(dest_file_path));
            if (!parentDir.exists()){
                parentDir.mkdirs();
                CommonLab.log("download","makdir:"+parentDir.getAbsolutePath());
            }
            dest_file.createNewFile();
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(3000);
            InputStream in = new BufferedInputStream(conn.getInputStream());
            OutputStream out=new BufferedOutputStream(new FileOutputStream(dest_file));
            int buffer=0;
            try {

                while((buffer=in.read())!=-1){
                    out.write(buffer);
                }
                out.flush();
            }
            catch (SocketTimeoutException e){
                dest_file.delete();
                log("download",url +"Connection is timeout, The work is stopped.");
                return -1;
            }finally {
                in.close();
                out.close();
            }
            // hideProgressIndicator();
            log("download",url + "is ok, file path:" + dest_file_path);
            return 0;
        } catch (IOException e) {
            dest_file.delete();
            e.printStackTrace();
            log("download", url + "is bad, The work is stopped.");
            return -1;
        }
    }

    public static String DownLoadUrltoString(String strSourceUrl){
        StringBuilder sb=new StringBuilder();
        try {
            URL u = new URL(strSourceUrl);
            URLConnection conn = u.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(3000);
            InputStream in = new BufferedInputStream(conn.getInputStream());
            int buff;
            try{
                while ((buff=in.read())>-1){
                    sb.appendCodePoint(buff);
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                in.close();
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }

    public static int calculateInImageSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeBitmapFromImageFile(String imageFile,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        if (reqHeight==0){
            Bitmap tempBitmap=BitmapFactory.decodeFile(imageFile);
            int w=tempBitmap.getWidth();
            int h=tempBitmap.getHeight();
            reqHeight=reqWidth*h/w;
            tempBitmap.recycle();
            CommonLab.log("decodeimage","reqWidth:"+String.valueOf(reqWidth)+" reqHeight:"+ String.valueOf(reqHeight)+ " w:" + String.valueOf(w) + " h:" + String.valueOf(h));
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInImageSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imageFile, options);
    }

    public static String check_build_path(Context context){
        String Base_Path;
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Base_Path=context.getFilesDir().getAbsolutePath();
        }else{
            Base_Path = Environment.getExternalStorageDirectory()+ File.separator+"frakekuaikan";

        }
        File path=new File(Base_Path+ File.separator +COMIC_PATH);
        if (!path.exists()) path.mkdirs();
        path=new File(Base_Path + File.separator + JSON_PATH);
        if (!path.exists()) path.mkdirs();
        return Base_Path;
    }

    public static void Online_Check_And_Notify(Context context){
        if (isOnline(context)){
            ConnectivityManager connMgr = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected()){
                Toast.makeText(context,"Your phone is connecting by WIFI", Toast.LENGTH_LONG).show();
            }
            networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (networkInfo.isConnected()){
                Toast.makeText(context,"Your phone is connecting by MOBILE", Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(context,"Your phone is not online!", Toast.LENGTH_LONG).show();
        }
    }

    private static boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
    private static String getParentPathFormString(String filePath){

        return filePath.replaceFirst("/[[a-z][0-9][.]]+\\z","");
    }

    public static String getDate(long time) {

        Time timeObj=new Time(time*1000);

        String date = DateFormat.format("MM月dd日 hh:mm", timeObj).toString();
        CommonLab.log("getDate","Input:"+ String.valueOf(time) + "  Output:" +date);
        return date;
    }

    public static long getPassedTime(long lngTime){
        return System.currentTimeMillis()-lngTime;
    }
}

