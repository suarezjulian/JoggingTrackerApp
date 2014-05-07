package co.juliansuarez.joggingtracker.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import co.juliansuarez.joggingtracker.JoggingTrackerApp;
import co.juliansuarez.joggingtracker.MainActivity;
import co.juliansuarez.joggingtracker.R;
import co.juliansuarez.joggingtracker.constants.Prefs;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // UI references.
    @InjectView(R.id.autoCompleteTextViewEmail)
    AutoCompleteTextView mAutoCompleteTextViewEmail;

    @InjectView(R.id.editTextPassword)
    EditText mEditTextPassword;

    @InjectView(R.id.editTextConfirmPassword)
    EditText mEditTextConfirmPassword;

    @InjectView(R.id.progressBarLogin)
    View mProgressBarLogin;

    @InjectView(R.id.scrollViewLoginForm)
    View mScrollViewLoginForm;

    @InjectView(R.id.checkBoxNewUser)
    CheckBox mCheckBoxNewUser;

    private boolean mCreateAccount = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (JoggingTrackerApp.APP.getString(Prefs.USER_EMAIL, "").length() > 0) {
            startMainActivity();
        } else {
            setContentView(R.layout.activity_login);
            ButterKnife.inject(this);

            // Set up the login form.
            initViews();
            initListeners();
            updateView();
        }
    }

    private void initListeners() {
        mEditTextPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLoginOrSignup();
                    return true;
                }
                return false;
            }
        });

        mCheckBoxNewUser.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCreateAccount = isChecked;
                updateView();
            }
        });
    }

    private void updateView() {
        if (mCreateAccount) {
            mEditTextConfirmPassword.setVisibility(View.VISIBLE);
        } else {
            mEditTextConfirmPassword.setVisibility(View.GONE);
        }
    }

    private void initViews() {
        if (VERSION.SDK_INT >= 14) {
            // Use ContactsContract.Profile (API 14+)
            getSupportLoaderManager().initLoader(0, null, this);
        } else if (VERSION.SDK_INT >= 8) {
            // Use AccountManager (API 8+)
            new SetupEmailAutoCompleteTask().execute(null, null);
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    @OnClick(R.id.buttonSignIn)
    public void attemptLoginOrSignup() {

        // Reset errors.
        mAutoCompleteTextViewEmail.setError(null);
        mEditTextPassword.setError(null);

        // Store values at the time of the login attempt.
        String email = mAutoCompleteTextViewEmail.getText().toString();
        String password = mEditTextPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;
        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mEditTextPassword.setError(getString(R.string.error_invalid_password));
            focusView = mEditTextPassword;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mAutoCompleteTextViewEmail.setError(getString(R.string.error_field_required));
            focusView = mAutoCompleteTextViewEmail;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mAutoCompleteTextViewEmail.setError(getString(R.string.error_invalid_email));
            focusView = mAutoCompleteTextViewEmail;
            cancel = true;
        }

        if (mCreateAccount) {
            String passwordConfirm = mEditTextConfirmPassword.getText().toString();
            if (!passwordMatch(password, passwordConfirm)) {
                mEditTextConfirmPassword.setError(getString(R.string.error_passwords_do_not_match));
                focusView = mEditTextConfirmPassword;
            }
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            if (!mCreateAccount) {
                loginInBackground(email, password);
            } else {
                createAccount(email, password);
            }
        }


    }

    private boolean passwordMatch(String password, String passwordConfirm) {
        return !TextUtils.isEmpty(passwordConfirm) && passwordConfirm.equals(password);

    }

    private void loginInBackground(final String email, String password) {
        ParseUser.logInInBackground(email, password, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                showProgress(false);
                if (user != null) {
                    JoggingTrackerApp.APP.saveString(Prefs.USER_EMAIL, user.getEmail());
                    JoggingTrackerApp.APP.saveString(Prefs.USER_TOKEN, user.getSessionToken());
                    startMainActivity();
                } else {
                    // Signup failed. Look at the ParseException to see what happened.
                    mEditTextPassword.setError(getString(R.string.error_incorrect_password));
                    mEditTextPassword.requestFocus();
                }
            }
        });

    }

    private void startMainActivity() {
        finish();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
    }

    private void createAccount(String email, String password) {
        final ParseUser user = new ParseUser();
        user.setUsername(email);
        user.setPassword(password);
        user.setEmail(email);

        user.signUpInBackground(new SignUpCallback() {
            public void done(ParseException e) {
                showProgress(false);
                if (e == null) {
                    JoggingTrackerApp.APP.saveString(Prefs.USER_EMAIL, user.getEmail());
                    JoggingTrackerApp.APP.saveString(Prefs.USER_TOKEN, user.getSessionToken());
                    startMainActivity();
                } else {
                    // Sign up didn't succeed. Look at the ParseException
                    // to figure out what went wrong
                    mEditTextPassword.setError(e.getMessage());
                    mEditTextPassword.requestFocus();
                }
            }
        });
    }

    private boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mScrollViewLoginForm.setVisibility(show ? View.GONE : View.VISIBLE);
            mScrollViewLoginForm.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mScrollViewLoginForm.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressBarLogin.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressBarLogin.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressBarLogin.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressBarLogin.setVisibility(show ? View.VISIBLE : View.GONE);
            mScrollViewLoginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(this,
                                // Retrieve data rows for the device user's 'profile' contact.
                                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                                                     ContactsContract.Contacts.Data
                                                             .CONTENT_DIRECTORY
                                ),
                                ProfileQuery.PROJECTION,

                                // Select only email addresses.
                                ContactsContract.Contacts.Data.MIMETYPE +
                                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                                // Show primary email addresses first. Note that there won't be
                                // a primary email address if the user hasn't specified one.
                                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC"
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
    }

    /**
     * Use an AsyncTask to fetch the user's email addresses on a background thread, and update
     * the email text field with results on the main UI thread.
     */
    class SetupEmailAutoCompleteTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... voids) {
            ArrayList<String> emailAddressCollection = new ArrayList<>();

            // Get all emails from the user's contacts and copy them to a list.
            ContentResolver cr = getContentResolver();
            Cursor emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                                       null, null, null);
            while (emailCur.moveToNext()) {
                String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract
                                                                                  .CommonDataKinds.Email.DATA));
                emailAddressCollection.add(email);
            }
            emailCur.close();

            return emailAddressCollection;
        }

        @Override
        protected void onPostExecute(List<String> emailAddressCollection) {
            addEmailsToAutoComplete(emailAddressCollection);
        }
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                                   android.R.layout.simple_dropdown_item_1line,
                                   emailAddressCollection);

        mAutoCompleteTextViewEmail.setAdapter(adapter);
    }
}



