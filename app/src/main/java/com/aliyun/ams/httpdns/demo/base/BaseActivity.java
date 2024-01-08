package com.aliyun.ams.httpdns.demo.base;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aliyun.ams.httpdns.demo.MyApp;
import com.aliyun.ams.httpdns.demo.R;

public class BaseActivity extends Activity {

    public static final int MSG_WHAT_LOG = 10000;

    private ScrollView logScrollView;
    private TextView logView;
    private LinearLayout llContainer;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_WHAT_LOG:
                        logView.setText(logView.getText() + "\n" + (String) msg.obj);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                logScrollView.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                        break;
                }
            }
        };

        logScrollView = findViewById(R.id.logScrollView);
        logView = findViewById(R.id.tvConsoleText);
        llContainer = findViewById(R.id.llContainer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        handler = null;
    }

    /**
     * 发送日志到界面
     *
     * @param log
     */
    protected void sendLog(String log) {
        Log.d(MyApp.TAG, log);
        if (handler != null) {
            Message msg = handler.obtainMessage(MSG_WHAT_LOG, log);
            handler.sendMessage(msg);
        }
    }

    protected void cleanLog() {
        logView.setText("");
    }

    protected void addView(int layoutId, OnViewCreated created) {
        FrameLayout container = new FrameLayout(this);
        View.inflate(this, layoutId, container);
        llContainer.addView(container, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        created.onViewCreated(container);
    }

    protected void addOneButton(
            final String labelOne, final View.OnClickListener clickListenerOne
    ) {
        addView(R.layout.item_one_button, new OnViewCreated() {
            @Override
            public void onViewCreated(View view) {

                Button btnOne = view.findViewById(R.id.btnOne);
                btnOne.setText(labelOne);
                btnOne.setOnClickListener(clickListenerOne);
            }
        });
    }

    protected void addTwoButton(
            final String labelOne, final View.OnClickListener clickListenerOne,
            final String labelTwo, final View.OnClickListener clickListenerTwo
    ) {
        addView(R.layout.item_two_button, new OnViewCreated() {
            @Override
            public void onViewCreated(View view) {

                Button btnOne = view.findViewById(R.id.btnOne);
                btnOne.setText(labelOne);
                btnOne.setOnClickListener(clickListenerOne);

                Button btnTwo = view.findViewById(R.id.btnTwo);
                btnTwo.setText(labelTwo);
                btnTwo.setOnClickListener(clickListenerTwo);
            }
        });
    }

    protected void addThreeButton(
            final String labelOne, final View.OnClickListener clickListenerOne,
            final String labelTwo, final View.OnClickListener clickListenerTwo,
            final String labelThree, final View.OnClickListener clickListenerThree
    ) {
        addView(R.layout.item_three_button, new OnViewCreated() {
            @Override
            public void onViewCreated(View view) {

                Button btnOne = view.findViewById(R.id.btnOne);
                btnOne.setText(labelOne);
                btnOne.setOnClickListener(clickListenerOne);

                Button btnTwo = view.findViewById(R.id.btnTwo);
                btnTwo.setText(labelTwo);
                btnTwo.setOnClickListener(clickListenerTwo);

                Button btnThree = view.findViewById(R.id.btnThree);
                btnThree.setText(labelThree);
                btnThree.setOnClickListener(clickListenerThree);
            }
        });
    }


    protected void addFourButton(
            final String labelOne, final View.OnClickListener clickListenerOne,
            final String labelTwo, final View.OnClickListener clickListenerTwo,
            final String labelThree, final View.OnClickListener clickListenerThree,
            final String labelFour, final View.OnClickListener clickListenerFour
    ) {
        addView(R.layout.item_four_button, new OnViewCreated() {
            @Override
            public void onViewCreated(View view) {

                Button btnOne = view.findViewById(R.id.btnOne);
                btnOne.setText(labelOne);
                btnOne.setOnClickListener(clickListenerOne);

                Button btnTwo = view.findViewById(R.id.btnTwo);
                btnTwo.setText(labelTwo);
                btnTwo.setOnClickListener(clickListenerTwo);

                Button btnThree = view.findViewById(R.id.btnThree);
                btnThree.setText(labelThree);
                btnThree.setOnClickListener(clickListenerThree);

                Button btnFour = view.findViewById(R.id.btnFour);
                btnFour.setText(labelFour);
                btnFour.setOnClickListener(clickListenerFour);
            }
        });
    }

    protected void addEditTextButton(
            final String hint,
            final String labelOne, final OnButtonClick clickListenerOne
    ) {
        addView(R.layout.item_edit_button, new OnViewCreated() {
            @Override
            public void onViewCreated(View view) {

                Button btnOne = view.findViewById(R.id.btnOne);
                btnOne.setText(labelOne);
                final EditText editText = view.findViewById(R.id.etOne);
                editText.setHint(hint);
                btnOne.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickListenerOne.onBtnClick(editText);
                    }
                });
            }
        });
    }


    protected void addEditTextEditTextButton(
            final String hintOne, final String hintTwo,
            final String labelOne, final OnButtonClickMoreView clickListenerOne
    ) {
        addView(R.layout.item_edit_edit_button, new OnViewCreated() {
            @Override
            public void onViewCreated(View view) {

                Button btnOne = view.findViewById(R.id.btnOne);
                btnOne.setText(labelOne);
                final EditText editTextOne = view.findViewById(R.id.etOne);
                editTextOne.setHint(hintOne);
                final EditText editTextTwo = view.findViewById(R.id.etTwo);
                editTextTwo.setHint(hintTwo);
                btnOne.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickListenerOne.onBtnClick(new View[]{editTextOne, editTextTwo});
                    }
                });
            }
        });
    }

    protected void addAutoCompleteTextViewButton(
            final String[] strings, final String hint, final String labelOne, final OnButtonClick clickListenerOne
    ) {
        addView(R.layout.item_autocomplete_button, new OnViewCreated() {
            @Override
            public void onViewCreated(View view) {

                Button btnOne = view.findViewById(R.id.btnOne);
                btnOne.setText(labelOne);

                final AutoCompleteTextView actvOne = view.findViewById(R.id.actvOne);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_dropdown_item_1line, strings);
                actvOne.setAdapter(adapter);
                actvOne.setHint(hint);

                btnOne.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickListenerOne.onBtnClick(actvOne);
                    }
                });
            }
        });
    }

    public interface OnViewCreated {
        void onViewCreated(View view);
    }

    public interface OnButtonClick {
        void onBtnClick(View view);
    }

    public interface OnButtonClickMoreView {
        void onBtnClick(View[] views);
    }
}
