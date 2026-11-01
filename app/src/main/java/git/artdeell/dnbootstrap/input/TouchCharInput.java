package git.artdeell.dnbootstrap.input;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.KeyCodes;

@SuppressLint("AppCompatCustomView")
public class TouchCharInput extends EditText {
    private static final String DEFAULT_CHARS = " ";
    private boolean keyboardRequestPending = false;
    public TouchCharInput(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setText(DEFAULT_CHARS);
        super.addTextChangedListener(new Watcher());
    }

    public TouchCharInput(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public TouchCharInput(@NonNull Context context) {
        this(context, null);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        int textLength = getText().length();
        // Only allow selections that end at the end of the text that was put in
        if(selEnd == textLength) return;
        setSelection(textLength);
    }

    private void compareSegments(String oldSegment, String newSegment) {
        int oldSegLength = oldSegment.length();
        int newSegLength = newSegment.length();
        int lastChange;
        for(lastChange = 0; lastChange < Math.min(oldSegLength, newSegLength); lastChange++) {
            if(oldSegment.charAt(lastChange) != newSegment.charAt(lastChange)) break;
        }
        int deleteChars = oldSegLength - lastChange;
        String addChars = newSegment.substring(lastChange);
        sendChanges(deleteChars, addChars);
    }

    private void sendChanges(int charsRemoved, String charsAdded) {
        for(;charsRemoved > 0;charsRemoved--) {
            GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_BACKSPACE, 1, 0);
            GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_BACKSPACE, 0, 0);
        }
        GLFW.sendBulkUnicodeEvent(charsAdded, 0);
    }

    public void requestKeyboard() {
        if(isFocused()) requestShowIme();
        else {
            keyboardRequestPending = true;
            requestFocus();
        }
    }

    private void requestShowIme() {
        keyboardRequestPending = false;
        if(Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindowInsetsController();
            if(controller == null) return;
            controller.show(WindowInsets.Type.ime());
        }else {
            InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if(keyboardRequestPending && focused) requestShowIme();
    }

    @Override
    public void addTextChangedListener(TextWatcher watcher) {
        throw new RuntimeException("Not allowed");
    }

    class Watcher implements TextWatcher {
        private CharSequence lastSegment;
        private boolean internalChanges = false;

        @Override
        public void afterTextChanged(Editable editable) {
            if(editable.length() < DEFAULT_CHARS.length()) {
                internalChanges = true;
                editable.insert(0, DEFAULT_CHARS);
                internalChanges = false;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            if(internalChanges) return;
            lastSegment = charSequence.subSequence(start, start + count);
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int lengthBefore, int lengthAfter) {
            if(internalChanges) return;
            assert lastSegment.length() == lengthBefore;
            assert start + lengthAfter == charSequence.length();
            String segment = charSequence.toString().substring(start, start + lengthAfter);
            if(lengthBefore == 0) {
                sendChanges(0, segment);
                return;
            }
            if(lengthAfter == 0) {
                sendChanges(lengthBefore, "");
                return;
            }
            String lastSegment = this.lastSegment.toString();
            compareSegments(lastSegment, segment);
            this.lastSegment = null;
        }
    }
}
