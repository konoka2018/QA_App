package jp.techacademy.konoka.fujiwara.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar mToolbar; //ドロワーの為のメンバ
    private int mGenre = 0;


    // firebase関連の変数たち。
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private DatabaseReference mFavorite;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionsListAdapter mAdapter;




    /*　実際のLISTの中身
   ------------------------------------------------------------------------------------- */
    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();
            String title = (String) map.get("title");
            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");
            String imageString = (String) map.get("image");
            byte[] bytes;
            if (imageString != null) {
                bytes = Base64.decode(imageString, Base64.DEFAULT);
            } else {
                bytes = new byte[0];
            }

            ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
            HashMap answerMap = (HashMap) map.get("answers");
            if (answerMap != null) {
                for (Object key : answerMap.keySet()) {
                    HashMap temp = (HashMap) answerMap.get((String) key);
                    String answerBody = (String) temp.get("body");
                    String answerName = (String) temp.get("name");
                    String answerUid = (String) temp.get("uid");
                    Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                    answerArrayList.add(answer);
                }
            }

            Question question = new Question(title, body, name, uid, dataSnapshot.getKey(),
                    mGenre, bytes, answerArrayList);
            mQuestionArrayList.add(question);
            mAdapter.notifyDataSetChanged();
        }


        /*　データに変化があった際に自動で呼び出されます

           firebaseに保存されているデータが変更された場合です
           例えばAさんがアプリを使ってるときに
           Bさんがデータを更新すると、Aさんの方でも画面が更新されるための仕組みです。
       ------------------------------------------------------------------------------------- */
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            // 変更があったQuestionを探す
            for (Question question : mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.getAnswers().clear();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String)
                                    key);
                            question.getAnswers().add(answer);
                        }
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }
        } //onChildChange 終わり。

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    private ChildEventListener mFavoriteListener = new ChildEventListener() {

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //ここがお気に入りを更新する部分です！

            //取得したデータの中身を取得しています
            HashMap<String, String> map = (HashMap<String, String>) dataSnapshot.getValue();

            //String favoriteQuestionUid = dataSnapshot.getKey();
            String favoriteQuestionUid = dataSnapshot.getKey();

            //取得したデータの中身に Genre というキーでジャンル番号が保存されているのでそれを取得します
            String genre = map.get("Genre");

            //contents->ジャンル番号->質問ID というツリーのデータを取得するためのパスを指定しています
            mDatabaseReference.child(Const.ContentsPATH)
                    .child(String.valueOf(genre))
                    .child(favoriteQuestionUid)

                    //上記ツリーのデータが取得できたときのイベントを設定しています
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // mEventListenerのonChildAddedと同じ処理
                            HashMap map = (HashMap) dataSnapshot.getValue();
                            String title = (String) map.get("title");
                            String body = (String) map.get("body");
                            String name = (String) map.get("name");
                            String uid = (String) map.get("uid");
                            String imageString = (String) map.get("image");
                            byte[] bytes;
                            if (imageString != null) {
                                bytes = Base64.decode(imageString, Base64.DEFAULT);
                            } else {
                                bytes = new byte[0];
                            }

                            ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                            HashMap answerMap = (HashMap) map.get("answers");

                            if (answerMap != null) {
                                for (Object key : answerMap.keySet()) {
                                    HashMap temp = (HashMap) answerMap.get((String) key);
                                    String answerBody = (String) temp.get("body");
                                    String answerName = (String) temp.get("name");
                                    String answerUid = (String) temp.get("uid");
                                    Answer answer = new Answer(answerBody, answerName, answerUid,
                                            (String) key);
                                    answerArrayList.add(answer);
                                }
                            }

                            Question question = new Question(title, body, name, uid, dataSnapshot
                                    .getKey(), mGenre, bytes, answerArrayList);
                            mQuestionArrayList.add(question);
                            mAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    /*オンクリエイトはここからスタート
    ------------------------------------------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
                if (mGenre == 0) {
                    Snackbar.make(view, "ジャンルを選択して下さい", Snackbar.LENGTH_LONG).show();
                    return;
                }

                // ログイン済みのユーザーを取得する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // ジャンルを渡して質問作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);
                    intent.putExtra("genre", mGenre);
                    startActivity(intent);
                }
            }
        });

        /*　ナビゲーションドロワーの設定
        ------------------------------------------------------------------------*/
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar,
                R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        /*　Firebaseを参照する
        ------------------------------------------------------------------------*/
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();


        /*　ListViewの準備
        ------------------------------------------------------------------------*/
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();


        /*　
        ------------------------------------------------------------------------*/
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });

        Bundle extras = getIntent().getExtras();

    }
    /*オンクリエイトはここまで
    ------------------------------------------------------------------------*/


    @Override
    protected void onResume() {
        super.onResume();

        // 1:趣味を既定の選択とする
        if (mGenre == 0) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            onNavigationItemSelected(navigationView.getMenu().getItem(0));
        }

        // 未ログイン状態の場合、お気に入りを非表示にする
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Menu menu = navigationView.getMenu();
            MenuItem favItem = menu.findItem(R.id.nav_fav);
            favItem.setVisible(false);

        } else {
            Menu menu = navigationView.getMenu();
            MenuItem favItem = menu.findItem(R.id.nav_fav);
            favItem.setVisible(true);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    /*右上のメニューから設定画面に進むようにします。
    ------------------------------------------------------------------------------------- */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*メニューを選択するとタイトルが変更される。
    ------------------------------------------------------------------------------------- */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_hobby) {
            mToolbar.setTitle("趣味");
            mGenre = 1;
        } else if (id == R.id.nav_life) {
            mToolbar.setTitle("生活");
            mGenre = 2;
        } else if (id == R.id.nav_health) {
            mToolbar.setTitle("健康");
            mGenre = 3;
        } else if (id == R.id.nav_compter) {
            mToolbar.setTitle("コンピューター");
            mGenre = 4;
        } else if (id == R.id.nav_fav) {
            mToolbar.setTitle("お気に入り");
            mGenre = 0;
        }


        // 選択したあとにドロワーを閉じるための処理
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);


        /* Firebaseに対してそのジャンルの質問のデータの変化を受け取るように
        　　先ほど作成したChildEventListenerを設定
        ------------------------------------------------------------------------------------- */
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear();
        mAdapter.setQuestionArrayList(mQuestionArrayList);
        mListView.setAdapter(mAdapter);


        //お気に入りを取得する処理を記入する
        if (mGenre == 0) {
                //お気に入りを取得する処理を記入する
                if (mFavorite != null) {
                    mFavorite.removeEventListener(mFavoriteListener);
                }

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                mFavorite = mDatabaseReference.child(Const.favoritePATH).child(user.getUid());
                mFavorite.addChildEventListener(mFavoriteListener);

            } else {
                // 選択したジャンルにリスナーを登録する
                if (mGenreRef != null) {
                    mGenreRef.removeEventListener(mEventListener);
                }

                mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf
                        (mGenre));
                mGenreRef.addChildEventListener(mEventListener);
            }
            return true;
        }


}//mainactivity ここで終わり。




 /* MEMO
 ------------------------------------------------------------------------------------- */

//addEventListenerをするとfirebaseにデータを取りにいって、
// データが取れるとonChildAddedが実行されるという仕組みです.