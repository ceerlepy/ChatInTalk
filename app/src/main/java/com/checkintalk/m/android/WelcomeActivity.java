package com.checkintalk.m.android;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.MailTo;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.telephony.SmsManager;
import android.text.Html;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.checkintalk.m.android.listener.OnSwipeTouchListener;
import com.checkintalk.m.android.mail.GMailSender;
import com.checkintalk.m.android.model.User;
import com.checkintalk.m.android.util.Util;

public class WelcomeActivity extends FragmentActivity {

	private User user;
	private EditText email;
	private EditText pass;
	private EditText name;
	private EditText surname;
    private ViewFlipper viewFlipper;
    private  Button loginButton;
    private  Button registerButton;
    private FrameLayout containerLayout;
    private Dialog registerDialog;
    private Dialog loginDialog;
    private Dialog sifremiUnuttumDialog;
    private float lastX;
    private Context context;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
        containerLayout = (FrameLayout) findViewById(R.id.container);
        viewFlipper = (ViewFlipper) findViewById(R.id.viewflipper);
        registerButton = (Button) findViewById(R.id.b_register);
        loginButton = (Button) findViewById(R.id.b_login);
        context = getApplicationContext();
		/*if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}*/

        //DEneme

        try {
            user = Util.getInstance().readUserFromLocal(getFilesDir());
            createLoginDialog();
            createRegisterDialog();
            createSifremiUnuttumDialog();

            registerButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    registerDialog.show();
                }
            });
            loginButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    loginDialog.show();
                }
            });
        }catch(Exception ex){
            Toast.makeText(getApplicationContext(), "Genel bir hata oluştu!", Toast.LENGTH_LONG).show();
        }

    }

    // Using the following method, we will handle all screen swaps.
    public boolean onTouchEvent(MotionEvent touchevent) {
        switch (touchevent.getAction()) {

            case MotionEvent.ACTION_DOWN:
                lastX = touchevent.getX();
                break;
            case MotionEvent.ACTION_UP:
                float currentX = touchevent.getX();

                // Handling left to right screen swap.
                if (lastX < currentX) {

                    // If there aren't any other children, just break.
                    if (viewFlipper.getDisplayedChild() == 0)
                        break;

                    // Next screen comes in from left.
                    viewFlipper.setInAnimation(this, R.anim.slide_in_from_left);
                    // Current screen goes out from right.
                    viewFlipper.setOutAnimation(this, R.anim.slide_out_to_right);

                    // Display next screen.
                    viewFlipper.showNext();
                }

                // Handling right to left screen swap.
                if (lastX > currentX) {

                    // If there is a child (to the left), kust break.
                    if (viewFlipper.getDisplayedChild() == 1)
                        break;

                    // Next screen comes in from right.
                    viewFlipper.setInAnimation(this, R.anim.slide_in_from_right);
                    // Current screen goes out from left.
                    viewFlipper.setOutAnimation(this, R.anim.slide_out_to_left);

                    // Display previous screen.
                    viewFlipper.showPrevious();
                }
                break;
        }
        return false;
    }

	private void createLoginDialog() throws  Exception{
		loginDialog = new Dialog(WelcomeActivity.this);
		loginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		Window window = loginDialog.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();
		wlp.gravity = Gravity.BOTTOM;
		wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		window.setAttributes(wlp);

		loginDialog.setContentView(R.layout.login_dialog);
		loginDialog.setCancelable(true);
		loginDialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));

        loginDialog.findViewById(R.id.login_dialog_layout).setOnTouchListener(new OnSwipeTouchListener(context) {
            @Override
            public void onSwipeRight() {
                if (loginDialog.isShowing())
                    loginDialog.dismiss();
            }

            @Override
            public void onSwipeLeft() {
                if (loginDialog.isShowing())
                    loginDialog.dismiss();
            }
        });

        Display display = getWindowManager().getDefaultDisplay();

		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		loginDialog.getWindow().setLayout((85 * width) / 100,(85 * height) / 100);

        final TextView sifremiUnuttum =  (TextView)loginDialog.findViewById(R.id.tw_sifremi_unuttum_log);
        sifremiUnuttum.setPaintFlags(sifremiUnuttum.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        sifremiUnuttum.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(loginDialog!= null && loginDialog.isShowing())
                    loginDialog.dismiss();
                if(sifremiUnuttumDialog!= null && !sifremiUnuttumDialog.isShowing()) {
                    try {
                        createSifremiUnuttumDialog();
                        sifremiUnuttumDialog.show();
                    }catch(Exception ex){
                        Toast.makeText(getApplicationContext(),
                                "Sayfa açılırken bir hata oluştu.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        TextView hemenKatil =  (TextView)loginDialog.findViewById(R.id.tw_redirect_register_log);
        hemenKatil.setPaintFlags(hemenKatil.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        hemenKatil.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(loginDialog!= null && loginDialog.isShowing())
                    loginDialog.dismiss();
                if(registerDialog!= null && !registerDialog.isShowing())
                    registerDialog.show();
            }
        });

        Button b_login = (Button) loginDialog.findViewById(R.id.b_login);
		final EditText email = (EditText) loginDialog
				.findViewById(R.id.et_email);
		final EditText pass = (EditText) loginDialog
				.findViewById(R.id.et_password);

        if(user != null){
            email.setText(user.getEmail());
            pass.setText(user.getPassword());
        }


        b_login.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				if (email.getText().toString().trim().isEmpty()) {
					Toast.makeText(getApplicationContext(),
							"Email hatalı ya da boş olamaz!",
							Toast.LENGTH_SHORT).show();
				} else if (pass.getText().toString().trim().length() < 6) {
					Toast.makeText(getApplicationContext(),
							"Şifre 6 karakterden az ya da boş olamaz!",
							Toast.LENGTH_SHORT).show();
				} else if (user != null && user.getEmail()!=null && user.getEmail().toString().equalsIgnoreCase(email.getText().toString())
						&& user.getPassword()!=null && user.getPassword().toString().equals(pass.getText().toString())) {
					loginDialog.hide();
					startActivity(new Intent(context,MainActivity.class));
					finish();
				} else
					Toast.makeText(getApplicationContext(),
							"Email ya da şifre hatalı!", Toast.LENGTH_SHORT)
							.show();
			}
		});
	}

	private void createRegisterDialog() throws  Exception{
		user = new User();
		registerDialog = new Dialog(WelcomeActivity.this);
		registerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		Window window = registerDialog.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();
		wlp.gravity = Gravity.BOTTOM;
		wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		window.setAttributes(wlp);

		registerDialog.setContentView(R.layout.register_dialog);
		registerDialog.setCancelable(true);
		registerDialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));

        registerDialog.findViewById(R.id.register_dialog_layout).setOnTouchListener(new OnSwipeTouchListener(context) {
            @Override
            public void onSwipeRight() {
                if(registerDialog.isShowing())
                    registerDialog.dismiss();
            }
            @Override
            public void onSwipeLeft() {
                if(registerDialog.isShowing())
                    registerDialog.dismiss();
            }
        });

        TextView info =  (TextView)registerDialog.findViewById(R.id.tw_info_reg);
        String sourceString = getResources().getString(R.string.register_info);
        info.setText(Html.fromHtml(sourceString));

        TextView oturumAc =  (TextView)registerDialog.findViewById(R.id.tw_redirect_login_reg);
        oturumAc.setPaintFlags(oturumAc.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        oturumAc.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(registerDialog!= null && registerDialog.isShowing())
                    registerDialog.dismiss();
                if(loginDialog!= null && !loginDialog.isShowing())
                    loginDialog.show();
            }
        });

		Display display = getWindowManager().getDefaultDisplay();

		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		registerDialog.getWindow().setLayout((85 * width) / 100,(85 * height) / 100);

		Button b_register = (Button) registerDialog.findViewById(R.id.b_register);
		email = (EditText) registerDialog.findViewById(R.id.et_email_reg);
		pass = (EditText) registerDialog.findViewById(R.id.et_password_reg);

        b_register.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				boolean registrable = false;

				if (email.getText() != null && !email.getText().toString().trim().isEmpty()) {
					user.setEmail(email.getText().toString());
					registrable = true;
				} else {
					registrable = false;
					Toast.makeText(getApplicationContext(),
							"Email hatalı ya da boş olamaz!",
							Toast.LENGTH_SHORT).show();
				}
				if (registrable && pass.getText() != null
						&& !pass.getText().toString().trim().isEmpty()
						&& pass.getText().toString().length() > 5) {
					registrable = true;
					user.setPassword(pass.getText().toString());
				} else {
					registrable = false;
					Toast.makeText(getApplicationContext(),
							"Şifre 6 karakterden az ya da boş olamaz!",
							Toast.LENGTH_SHORT).show();
				}

				if (registrable) {
					registerDialog.hide();
                    if (Util.getInstance().writeUserOnLocal(user, getFilesDir())) {
                        loginDialog.show();;
                        Toast.makeText(getApplicationContext(), "Kayıt başarılı!", Toast.LENGTH_LONG).show();
                    } else {
                        registerDialog.show();
                        Toast.makeText(getApplicationContext(), "Kayıt sırasında genel bir hata oluştu!", Toast.LENGTH_LONG).show();
                    }
				}
			}
		});
	}

    private void createSifremiUnuttumDialog() throws  Exception{
        sifremiUnuttumDialog = new Dialog(WelcomeActivity.this);
        sifremiUnuttumDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = sifremiUnuttumDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);

        sifremiUnuttumDialog.setContentView(R.layout.sifremi_unuttum_dialog);
        sifremiUnuttumDialog.setCancelable(true);
        sifremiUnuttumDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));

        sifremiUnuttumDialog.findViewById(R.id.sifremi_unuttum_layout).setOnTouchListener(new OnSwipeTouchListener(context) {
            @Override
            public void onSwipeRight() {
                if (sifremiUnuttumDialog.isShowing())
                    sifremiUnuttumDialog.dismiss();
            }

            @Override
            public void onSwipeLeft() {
                if (sifremiUnuttumDialog.isShowing())
                    sifremiUnuttumDialog.dismiss();
            }
        });

        Display display = getWindowManager().getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        sifremiUnuttumDialog.getWindow().setLayout((85 * width) / 100,(85 * height) / 100);

        final Button send_password = (Button) sifremiUnuttumDialog.findViewById(R.id.password_send);
        final Button send_otp = (Button) sifremiUnuttumDialog.findViewById(R.id.otp_send);
        final EditText email = (EditText) sifremiUnuttumDialog.findViewById(R.id.et_pass_email);
        final EditText phone = (EditText) sifremiUnuttumDialog.findViewById(R.id.et_pass_phone);
        final EditText otp = (EditText) sifremiUnuttumDialog.findViewById(R.id.et_otp);
        final TextView otpInfo = (TextView) sifremiUnuttumDialog.findViewById(R.id.tw_otp_info);

        send_otp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                boolean sending_otp = false;
                if (phone.getText() != null
                        && !phone.getText().toString().trim().isEmpty()
                        && phone.getText().toString().length() == 11) {
                    sending_otp = true;
                } else {
                    sending_otp = false;
                    Toast.makeText(getApplicationContext(),
                            "Telefon numarası 11 haneli olmaldır.",
                            Toast.LENGTH_SHORT).show();
                }

                if (sending_otp) {
                   //sendSMS(phone.getText().toString(),"Sedat deneme otp gedi mi sana :))  223344");
                    otp.setVisibility(View.VISIBLE);
                    otpInfo.setVisibility(View.VISIBLE);
                    email.setVisibility(View.VISIBLE);
                    send_password.setVisibility(View.VISIBLE);
                    phone.setVisibility(View.GONE);
                    send_otp.setVisibility(View.GONE);
                }
            }
        });

        send_password.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                boolean sending_password = false;
                if (email.getText() != null && !email.getText().toString().trim().isEmpty()) {
                    sending_password = true;
                } else {
                    sending_password = false;
                    Toast.makeText(getApplicationContext(),
                            "Email hatalı ya da boş olamaz!",
                            Toast.LENGTH_SHORT).show();
                }

                if (sending_password) {
                    /*Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setType("message/rfc822");
                    //emailIntent.putExtra(Intent.EXTRA_EMAIL  , new String[]{""+email.getText()});
                    emailIntent.setData(Uri.parse("mailto:"+email.getText()));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "CTalk Şifremi Unuttum");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Merhaba Veysel Tosun, yeni şifreniz :223344");
                    emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(emailIntent);
                        Toast.makeText(getApplicationContext(),
                                email.getText().toString()+" mail adresinize şifreniz gönderilmiştir.",
                                Toast.LENGTH_SHORT).show();
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(WelcomeActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }*/
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                GMailSender sender = new GMailSender(
                                        "veyseltosun.vt@gmail.com",
                                        "BiCo2288!");
                                //sender.addAttachment(Environment.getExternalStorageDirectory().getPath()+"/image.jpg");
                                sender.sendMail("Test mail", "This mail has been sent from android app along with attachment",
                                        "veyseltosun.vt@gmail.com",
                                        email.getText().toString());
                                Toast.makeText(getApplicationContext(),
                                        email.getText().toString()+" mail adresinize şifreniz gönderilmiştir.",
                                        Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(),"Mail gönderimi sırasında hata oluştu.",Toast.LENGTH_LONG).show();
                            }
                        }
                    }).start();
                }
            }
        });
    }


    //---sends an SMS message to another device---
    private void sendSMS(String phoneNumber, String message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_welcome,
					container, false);
			return rootView;
		}
	}
}
