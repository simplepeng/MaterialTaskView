
package com.simple.materialtaskview;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {

    RecentsView v;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        RecentsConfiguration.reinitialize(this);
        setContentView(R.layout.activity_main);
        v = (RecentsView)this.findViewById(R.id.recents_view);
        v.setTaskStacks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
