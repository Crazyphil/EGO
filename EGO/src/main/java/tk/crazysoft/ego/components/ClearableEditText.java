package tk.crazysoft.ego.components;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import tk.crazysoft.ego.R;

// Source: http://arunbadole1209.wordpress.com/2011/12/16/how-to-create-edittext-with-crossx-button-at-end-of-it/
public class ClearableEditText extends RelativeLayout {
    LayoutInflater inflater = null;
    EditText editText;
    Button buttonClear;

    public ClearableEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews();
    }

    public ClearableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public ClearableEditText(Context context) {
        super(context);
        initViews();
    }

    void initViews() {
        inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.clearable_edit_text, this, true);
        editText = (EditText)findViewById(R.id.editText);
        buttonClear = (Button)findViewById(R.id.buttonClear);
        buttonClear.setVisibility(RelativeLayout.INVISIBLE);

        buttonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearText();
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                toggleClearButton();
            }
        });
    }

    public void clearText() {
        editText.setText("");
    }

    public void toggleClearButton() {
        if (getText().length() > 0) {
            buttonClear.setVisibility(RelativeLayout.VISIBLE);
        } else {
            buttonClear.setVisibility(RelativeLayout.INVISIBLE);
        }
    }

    public EditText getText() {
        return editText;
    }

    public void setText(CharSequence text) {
        editText.setText(text);
    }
}