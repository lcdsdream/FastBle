package com.clj.blesample;

import android.os.AsyncTask;
import android.widget.ProgressBar;

/**
 * Created by lcd on 9/7/16.
 */
public class ScanProgressBar extends AsyncTask<Integer,Integer,String>
{
        private ProgressBar pgbar;
        public ScanProgressBar(ProgressBar pgbar) {
            super();
            this.pgbar = pgbar;
        }

        class DelayOperator {
            public void delay()
            {
                try {
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }

        //该方法不运行在UI线程中,主要用于异步操作,通过调用publishProgress()方法
        //触发onProgressUpdate对UI进行操作
        @Override
        protected String doInBackground(Integer... params) {
            DelayOperator dop = new DelayOperator();
            int i;
            for (i = 0;i < 100; i += (100*100)/params[0].intValue()) {
                dop.delay();
                publishProgress(i);
            }
            return  i + params[0].intValue() + "";
        }

        @Override
        protected void onPreExecute() {
        }

        //在doBackground方法中,每次调用publishProgress方法都会触发该方法
        //运行在UI线程中,可对UI控件进行操作
        @Override
        protected void onProgressUpdate(Integer... values) {
            int value = values[0];
            pgbar.setProgress(value);
        }
}
