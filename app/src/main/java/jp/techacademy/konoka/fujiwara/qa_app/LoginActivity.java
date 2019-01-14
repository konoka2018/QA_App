package jp.techacademy.konoka.fujiwara.qa_app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    EditText mEmailEditText;    //EditTextを保持する変数を定義
    EditText mPasswordEditText; //EditTextを保持する変数を定義
    EditText mNameEditText;     //EditTextを保持する変数を定義
    ProgressDialog mProgress;   //アカウント作成中はプログレスダイアログを表示するため、ProgressDialogも定義


    FirebaseAuth mAuth;
    OnCompleteListener<AuthResult> mCreateAccountListener; //処理の完了を受け取るリスナー
    OnCompleteListener<AuthResult> mLoginListener; //処理の完了を受け取るリスナー
    DatabaseReference mDataBaseReference; //データベースへの読み書きに必要


    // アカウント作成時にフラグを立て、ログイン処理後に名前をFirebaseに保存する
    boolean mIsCreateAccount = false;


    /*オンクリエイトはここからスタート
    ------------------------------------------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /*Firebase関連の流れ

        データベースへのリファレンスを取得
        ①FirebaseAuthクラスのインスタンスを取得
        ②アカウント作成処理のリスナーを作成
        ③ログイン処理のリスナーを作成
        ④イトルバーのタイトルを変更
        ⑤UIをメンバ変数に保持
        ⑥アカウント作成ボタンとログインボタンのOnClickListenerを設定
    　　------------------------------------------------------------------------*/
        mDataBaseReference = FirebaseDatabase.getInstance().getReference();

        // FirebaseAuthのオブジェクトを取得する
        mAuth = FirebaseAuth.getInstance();

        // Firebaseのアカウント作成処理はOnCompleteListenerクラスで受け取り
        mCreateAccountListener = new OnCompleteListener<AuthResult>() {
            @Override

            //onCompleteメソッドをオーバーライドする必要があります。
            public void onComplete(Task<AuthResult> task) {

                //引数で渡ってきたTaskクラスのisSuccessfulメソッドで成功したかどうかを確認
                if (task.isSuccessful()) {
                    // 成功した場合
                    // ログインを行う
                    String email = mEmailEditText.getText().toString();
                    String password = mPasswordEditText.getText().toString();
                    login(email, password);
                } else {

                    //アカウント作成に失敗した場合は、Snackbarでエラーの旨を表示し、
                    // 処理中に表示していたダイアログを dismiss メソッドで非表示
                    View view = findViewById(android.R.id.content);
                    Snackbar.make(view, "アカウント作成に失敗しました", Snackbar.LENGTH_LONG).show
                            ();

                    // プログレスダイアログを非表示にする
                    mProgress.dismiss();
                }
            }
        };

        // ログイン処理のリスナー
        mLoginListener = new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {

                if (task.isSuccessful()) {
                    // 成功した場合
                    FirebaseUser user = mAuth.getCurrentUser();
                    DatabaseReference userRef = mDataBaseReference.child(Const.UsersPATH).child
                            (user.getUid());

                    if (mIsCreateAccount) {
                        // アカウント作成の時は表示名をFirebaseに保存する
                        String name = mNameEditText.getText().toString();


                        Map<String, String> data = new HashMap<String, String>();
                        data.put("name", name);
                        userRef.setValue(data);

                        // 表示名をPrefarenceに保存する
                        saveName(name);
                    } else {
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                Map data = (Map) snapshot.getValue();
                                saveName((String)data.get("name"));
                            }
                            @Override
                            public void onCancelled(DatabaseError firebaseError) {
                            }
                        });
                    }

                    // プログレスダイアログを非表示にする
                    mProgress.dismiss();

                    // Activityを閉じる
                    finish();

                } else {
                    // 失敗した場合
                    // エラーを表示する
                    View view = findViewById(android.R.id.content);
                    Snackbar.make(view, "ログインに失敗しました", Snackbar.LENGTH_LONG).show();

                    // プログレスダイアログを非表示にする
                    mProgress.dismiss();
                }
            }
        };

        // UIの準備
        setTitle("ログイン");

        mEmailEditText = (EditText) findViewById(R.id.emailText);
        mPasswordEditText = (EditText) findViewById(R.id.passwordText);
        mNameEditText = (EditText) findViewById(R.id.nameText);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("処理中...");

        Button createButton = (Button) findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // キーボードが出てたら閉じる
                InputMethodManager im = (InputMethodManager)getSystemService(Context
                        .INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                String email = mEmailEditText.getText().toString();
                String password = mPasswordEditText.getText().toString();
                String name = mNameEditText.getText().toString();

                if (email.length() != 0 && password.length() >= 6 && name.length() != 0) {
                    // ログイン時に表示名を保存するようにフラグを立てる
                    mIsCreateAccount = true;

                    createAccount(email, password);
                } else {
                    // エラーを表示する
                    Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // キーボードが出てたら閉じる
                InputMethodManager im = (InputMethodManager)getSystemService(Context
                        .INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                String email = mEmailEditText.getText().toString();
                String password = mPasswordEditText.getText().toString();

                if (email.length() != 0 && password.length() >= 6) {
                    // フラグを落としておく
                    mIsCreateAccount = false;

                    login(email, password);
                } else {
                    // エラーを表示する
                    Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }
     /*オンクリエイトはここで終わり
    ------------------------------------------------------------------------*/

    private void createAccount(String email, String password) {
        // プログレスダイアログを表示する
        mProgress.show();

        // アカウントを作成する
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener
                (mCreateAccountListener);
    }

    private void login(String email, String password) {
        // プログレスダイアログを表示する
        mProgress.show();

        // ログインする
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener);
    }

    private void saveName(String name) {
        // Preferenceに保存する
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(Const.NameKEY, name);
        editor.commit();
    }
}