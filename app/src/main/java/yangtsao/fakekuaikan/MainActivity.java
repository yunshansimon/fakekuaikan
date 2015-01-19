package yangtsao.fakekuaikan;


import android.app.FragmentManager;


import android.content.Intent;
import android.os.Bundle;


import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends FragmentActivity implements DailyListFragment.dailyOnFragmentInteractionListener,ComicFragment.OnFragmentInteractionListener {

  //  private Js
    private Handler switchHandler;
    private TabHost myTabHost;
    private String base_path;
    private String daily_json_path;
    private String list1_json_path;
    private String list_json_path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        View decorView = getWindow().getDecorView();
// Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE ;
        decorView.setSystemUiVisibility(uiOptions);
// Remember that you should never show the action bar if the
// status bar is hidden, so hide that too if necessary.

        if(getActionBar()!=null) getActionBar().hide();


        base_path=CommonLab.check_build_path(getApplicationContext());
        daily_json_path=base_path+ File.separator+CommonLab.JSON_PATH + File.separator + CommonLab.DAILY_FILE;
        list1_json_path=base_path+File.separator+CommonLab.JSON_PATH+File.separator+CommonLab.LIST1_FILE;
        list_json_path=base_path+File.separator+CommonLab.JSON_PATH+File.separator+CommonLab.LIST_FILE;

        switchHandler=new Handler();



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       // getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }

    /**
     * Dispatch onStart() to all fragments.  Ensure any created loaders are
     * now started.
     */


    @Override
    protected void onResume() {
        super.onResume();
        if (myTabHost!=null) {
            switch_view();
        }else{
            switchHandler.postDelayed(runDown_load_first_jsons,1000);
        }
    }

    private void switch_view(){
        FragmentManager fm=getFragmentManager();
        switch (myTabHost.getCurrentTab()){
            case 0:
                fm.beginTransaction()
                        .replace(R.id.tab1, DailyListFragment.newInstance(daily_json_path, base_path))
                        .commit();

                break;
            case 1:
                fm.beginTransaction()
                        .replace(R.id.tab2, ComicFragment.newInstance(list1_json_path, base_path))
                        .commit();
                break;
            default:
        }
    }



    @Override
    public void onFragmentInteraction(String strUrl) {

    }

    @Override
    public void onDailyFragmentInteraction(String strUrl) {
        Intent intent=new Intent(this,ContentActivity.class);
        intent.putExtra(CommonLab.COMIC_URL,strUrl);
        intent.putExtra(CommonLab.BASE_PATH,base_path);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_detail,R.anim.slide_out_main);
    }

    protected Runnable runSwitch_To_MainView=new Runnable() {
        @Override
        public void run() {
            setContentView(R.layout.activity_main);
            myTabHost=(TabHost)findViewById(R.id.tabHost);
            myTabHost.setup();

            TabHost.TabSpec spec1=myTabHost.newTabSpec(getString(R.string.tab_daily));
            spec1.setContent(R.id.tab1);
            spec1.setIndicator(getLayoutInflater().inflate(R.layout.daily_tab,null));

            TabHost.TabSpec spec2=myTabHost.newTabSpec(getString(R.string.tab_List));
            spec2.setContent(R.id.tab2);
            spec2.setIndicator(getLayoutInflater().inflate(R.layout.list_tab,null));

            TabHost.TabSpec spec3=myTabHost.newTabSpec(getString(R.string.tab_account));
            spec3.setContent(R.id.tab3);
            spec3.setIndicator(getLayoutInflater().inflate(R.layout.account_tab,null));

            myTabHost.addTab(spec1);
            myTabHost.addTab(spec2);
            myTabHost.addTab(spec3);

            myTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
                @Override
                public void onTabChanged(String tabId) {
                    switch_view();
                }
            });
            switch_view();
        }
    };
    protected Runnable runDown_load_first_jsons=new Runnable() {
        @Override
        public void run() {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(CommonLab.downloadFile(CommonLab.URL_DAILY_JSON,daily_json_path,true) !=0){
                        switchHandler.post(show_web_err);
                        return;
                    }
                    if(CommonLab.downloadFile(CommonLab.URL_LIST1_JSON,list1_json_path,true)!=0){
                        switchHandler.post(show_web_err);
                        return;
                    }
                    if(CommonLab.downloadFile(CommonLab.URL_LIST_JSON,list_json_path,true)!=0){
                        switchHandler.post(show_web_err);
                        return;
                    }
                    switchHandler.post(runSwitch_To_MainView);
                }
            }).run();

        }
    };

    protected Runnable show_web_err=new Runnable() {
        @Override
        public void run() {
            Toast.makeText(getBaseContext(), getString(R.string.web_error), Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {

            }
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    };
}
