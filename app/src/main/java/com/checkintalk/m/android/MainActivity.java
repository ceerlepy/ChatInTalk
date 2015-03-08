package com.checkintalk.m.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.checkintalk.m.android.R;
import com.checkintalk.m.android.connection.HttpConnection;
import com.checkintalk.m.android.listener.OnSwipeTouchListener;
import com.checkintalk.m.android.parser.DirectionsJSONParser;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends FragmentActivity implements
                                GoogleApiClient.ConnectionCallbacks,
                                GoogleApiClient.OnConnectionFailedListener,
                                LocationListener {

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	GoogleMap googleMap;
	List<LatLng> wayPoints;
	SharedPreferences sharedPreferences;
	LatLng currentPoint;
	LatLng lastPoint;
	Marker lastMarker;
	Circle othersLastCircle;
	int locationCount = 0;
	boolean firstLocationChange = true;
	Socket socket = null;
	View infoView = null;
	Dialog chatDialog = null;
	StringBuilder chatContent = new StringBuilder();
	LinearLayout chatLayout;
	Button chatButton;
	private GoogleApiClient mGoogleApiClient;
	protected Location currentLocation;
	Context context;
	Button dialogButton;
	ScrollView chatScroll;
	private static SimpleDateFormat sdf;
	static {
	   sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.activity_main);
		context = getApplicationContext();
		
		dialogButton = (Button)findViewById(R.id.dialogButton);
		
		infoView = getLayoutInflater().inflate(R.layout.info_window, null);
		chatDialog = new Dialog(MainActivity.this);
		chatDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		Window window = chatDialog.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();
		wlp.gravity = Gravity.BOTTOM;
		wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		window.setAttributes(wlp);
		
		chatDialog.setContentView(R.layout.chat_window);
		chatDialog.setCancelable(true);
		chatDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
	    chatDialog.getWindow().setLayout((80*width)/100,(80*height)/100); 
		
		chatButton = (Button)chatDialog.findViewById(R.id.sharebutton);
		chatLayout = (LinearLayout)chatDialog.findViewById(R.id.chatLayout);
		chatScroll = (ScrollView)chatDialog.findViewById(R.id.chatScroll);
        //mLocationClient = new LocationClient(this, this, this);
		wayPoints = new ArrayList<LatLng>();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
	}
	
	 @Override
	 protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
     }
	    
	 /*
	  * Called when the Activity is no longer visible.
	  */
	 @Override
	 protected void onStop() {
        mGoogleApiClient.disconnect();
	    super.onStop();
	 }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
	
	@Override
	protected void onResume(){
		servicesConnected();
		super.onResume();
	}
	
	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;
		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
		    super();
		    mDialog = null;
		}
		// Set the dialog to display
		public void setDialog(Dialog dialog) {
		    mDialog = dialog;
		}
		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
		    return mDialog;
		}
	}
	
	/*
	* Handle results returned to the FragmentActivity
	* by Google Play services
	*/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Decide what to do based on the original request code
		switch (requestCode) {
		    case CONNECTION_FAILURE_RESOLUTION_REQUEST :
		        switch (resultCode) {
		            case Activity.RESULT_OK :
		            break;
		        }
		}
	}
	
	private boolean servicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
		    // In debug mode, log the status
		    Log.d("Location Updates", "Google Play services is available.");
		    // Continue
		    return true;
		    // Google Play services was not available for some reason.
		    // resultCode holds the error code.
		} else {
		    // Get the error dialog from Google Play services
		    showErrorDialog(resultCode);
		}
		return false;
	}

	private Marker drawMarker(LatLng point, BitmapDescriptor icon, String hedef){
        // Creating an instance of MarkerOptions
        MarkerOptions markerOptions = new MarkerOptions();
 
        // Setsting latitude and longitude for the marker
        markerOptions.position(point);
        markerOptions.draggable(true);
        markerOptions.icon(icon);
        markerOptions.title(hedef);
        markerOptions.flat(true);
        markerOptions.anchor(0.0f, 1.0f);
        markerOptions.rotation(245);
 
        // Adding marker on the Google Map
        Marker marker = googleMap.addMarker(markerOptions);
        
        return marker;
    }
	
	@Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        final Location  mLastLocation = (Location) LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

        try{
        //  IO.Options opts = new IO.Options();
        //  opts.forceNew = true;
			socket = IO.socket("http://169.254.108.187:3000");
			socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
	
			 @Override
			 public void call(Object... args) {
				  try {
					JSONObject obj = new JSONObject();
					obj.put("latitude", currentLocation.getLatitude());
					obj.put("longitude", currentLocation.getLongitude());
					socket.emit("position", obj, new Ack() {
						@Override
					  	public void call(Object... args) {
						  System.out.println("Servera Mesaj gitti");
					  	}
					});
				  } catch (Exception e) {
					e.printStackTrace();
				  }
			  }
		
			}).on("hasNewLocation", new Emitter.Listener() {
		
			  @Override
			  public void call(Object... args) {
				  try {
					JSONObject obj = (JSONObject)args[0];
					final double latitude = obj.getDouble("latitude");
					final double longitude = obj.getDouble("longitude");
					// runOnUiThread is needed if you want to change something in the UI thread
	                runOnUiThread(new Runnable() {
	                    public void run() {
	                    	if(othersLastCircle != null)
	                    		othersLastCircle.remove();
	                    	CircleOptions circleOptions = new CircleOptions();
	                    	circleOptions.center(new LatLng(latitude, longitude)).radius(5)// In meters
	                    	.strokeColor(Color.RED)
	                        .fillColor(Color.BLUE); 
		                    // Get back the mutable Circle
	                        othersLastCircle = googleMap.addCircle(circleOptions);
	                    }
	                });
				  } catch (JSONException e) {
					e.printStackTrace();
				  }
			  }
		
			}).on("newMessage", new Emitter.Listener() {
				
				  @Override
				  public void call(Object... args) {
					  try {
						JSONObject obj = (JSONObject)args[0];
						final String from = obj.getString("from");
						final String message = obj.getString("message");
						final String time = obj.getString("time");
						// runOnUiThread is needed if you want to change something in the UI thread
		                runOnUiThread(new Runnable() {
		                    public void run() {
		                    	View receiveView = getLayoutInflater().inflate(R.layout.receivemessage, null);
		                    	LinearLayout receiveLayout = (LinearLayout)receiveView.findViewById(R.id.receiveLayout);
		                    	TextView receiveName = (TextView) receiveLayout.findViewById(R.id.receiveName);
		                    	TextView receiveMessage = (TextView) receiveLayout.findViewById(R.id.receiveMessage);
		                    	TextView receiveTime = (TextView) receiveLayout.findViewById(R.id.receiveTime);
		                    	
		                    	receiveName.setText(from);
		                    	receiveMessage.setText(message);
		                    	receiveTime.setText(time);
		                    	
		                    	chatLayout.addView(receiveLayout);
		                    	if (!chatDialog.isShowing())
		                    		chatDialog.show();
		                    	
		                    	chatScroll.post(new Runnable() {
		                    	    @Override
		                    	    public void run() {
		                    	    	chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
		                    	    }
		                    	});
		                    }
		                });
					  } catch (JSONException e) {
						  e.printStackTrace();
					  }
				  }
			
			}).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
		
				@Override
				public void call(Object... args) {
				  socket.disconnect();
				}
		
			});
			socket.connect();
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
    	    
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    	googleMap = mapFragment.getMap();
		googleMap.setMyLocationEnabled(true);
		googleMap.getUiSettings().setMyLocationButtonEnabled(true);
//		googleMap.setOnMyLocationChangeListener(this);
		googleMap.setBuildingsEnabled(true);
		googleMap.setIndoorEnabled(true);
		googleMap.setTrafficEnabled(true);
		
		googleMap.setInfoWindowAdapter(new InfoWindowAdapter() {

	        // Use default InfoWindow frame
	        @Override
	        public View getInfoWindow(Marker marker) {
	            return null;
	        }

	        // Defines the contents of the InfoWindow
	        @Override
	        public View getInfoContents(Marker marker) {
	            // Getting view from the layout file info_window_layout
	            return infoView;
	        }
		});
		 
		currentPoint = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
		lastMarker = drawMarker(currentPoint,BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
				"Konumum");
        
        CameraPosition cameraPosition = CameraPosition.builder().target(currentPoint).zoom(13).bearing(90).build();
        // Animate the change in camera view over 2 seconds
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),2000, null);
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPoint,13));
//        googleMap.animateCamera(CameraUpdateFactory.zoomTo(Float.parseFloat("13")));
        
        googleMap.setOnMyLocationChangeListener(new OnMyLocationChangeListener() {
			@Override
			public void onMyLocationChange(Location location) {
				try {
					currentPoint =  new LatLng(location.getLatitude(), location.getLongitude());
					JSONObject obj = new JSONObject();
					obj.put("latitude", location.getLatitude());
					obj.put("longitude", location.getLongitude());
					socket.emit("position", obj, new Ack() {
					  @Override
					  	public void call(Object... args) {
						  System.out.println("Servera Mesaj gitti");
					  	}
					});
					lastMarker.remove();
					lastMarker = drawMarker(currentPoint,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),"KONUMUM");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
        
        googleMap.setOnMapClickListener(new OnMapClickListener() {
        	 
            @Override
            public void onMapClick(LatLng point) {
                
            	if(wayPoints.size() <= 6){
            		drawMarker(point,BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),"HEDEF");
            		wayPoints.add(point);
            	    lastPoint = point;
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putString("lat"+ Integer.toString((locationCount-1)), Double.toString(point.latitude));
//                editor.putString("lng"+ Integer.toString((locationCount-1)), Double.toString(point.longitude));
//                editor.putInt("locationCount", locationCount);
//                editor.putString("zoom", Float.toString(googleMap.getCameraPosition().zoom));
//                editor.commit();           
            	    
	                String url = getDirectionsUrl(currentPoint, wayPoints, lastPoint);
	                DownloadTask downloadTask = new DownloadTask();
	                downloadTask.execute(url);
	                Toast.makeText(getBaseContext(), "Marker is added to the Map", Toast.LENGTH_SHORT).show();
            	}
            }
        });
        
        googleMap.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
 
                // Removing the marker and circle from the Google Map
                googleMap.clear();
                wayPoints.clear();
                lastPoint = null;
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.clear();
//                editor.commit();
//                locationCount=0;
            }
        });
        
        googleMap.setOnMarkerClickListener(new OnMarkerClickListener() {
			
			@Override
			public boolean onMarkerClick(Marker marker) {
				marker.showInfoWindow();
				return true;
			}
		});
        
        chatDialog.findViewById(R.id.sharebutton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 EditText editText = (EditText)chatDialog.findViewById(R.id.sharetext);
				 if(editText.getText() != null && !editText.getText().toString().isEmpty()){
					 try {
						 
						String time = sdf.format(new Date());
						 
						JSONObject msj = new JSONObject();
						msj.put("from", "veysel");
						msj.put("message", editText.getText());
						msj.put("time", time);
						
						socket.emit("message", msj);
						
						View sendView = getLayoutInflater().inflate(R.layout.sendmessage, null);
						LinearLayout sendLayout = (LinearLayout)sendView.findViewById(R.id.sendLayout);
//                    	TextView sendName = (TextView) sendLayout.findViewById(R.id.sendName);
                    	TextView sendMessage = (TextView) sendLayout.findViewById(R.id.sendMessage);
                    	TextView sendTime = (TextView) sendLayout.findViewById(R.id.sendTime);
                    	
                    	sendMessage.setText(editText.getText());
                    	sendTime.setText(time);
                    	
                    	chatLayout.addView(sendLayout);
                    	if (!chatDialog.isShowing())
                    		chatDialog.show();
                    	TextView shareText = (TextView)chatDialog.findViewById(R.id.sharetext);
                    	shareText.setText("");
                    	
                    	chatScroll.post(new Runnable() {
                    	    @Override
                    	    public void run() {
                    	    	chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
                    	    }
                    	});
					} catch (JSONException e) {
						e.printStackTrace();
					}
				 }
			}
		});
        
        chatDialog.findViewById(R.id.chatScroll).setOnTouchListener(new OnSwipeTouchListener(context) {
        	@Override
            public void onSwipeRight() {
                if(chatDialog.isShowing())
                	chatDialog.dismiss();
            }
        	@Override
            public void onSwipeLeft() {
        		 if(chatDialog.isShowing())
                 	chatDialog.dismiss();
            }
        });
        
        dialogButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				  if(chatDialog.isShowing())
	                chatDialog.dismiss();
				  else
					chatDialog.show();
			}
		});
        
        googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
			
			@Override
			public void onInfoWindowClick(Marker marker) {
			    if(marker.getSnippet() == null){
		            googleMap.moveCamera(CameraUpdateFactory.zoomIn());
		        }
			    if (!chatDialog.isShowing())
				    chatDialog.show();
				marker.showInfoWindow();
			}
		});
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("ChatInTalk", "GoogleApiClient connection has been suspend");
        mGoogleApiClient.disconnect();
    }
    @Override
    public void onLocationChanged(Location location) {

    }
    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

	private void showErrorDialog(int errorCode) {
		
		 Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
				 	errorCode,
		            this,
		            CONNECTION_FAILURE_RESOLUTION_REQUEST);
		
	    // If Google Play services can provide an error dialog
	    if (errorDialog != null) {
	        // Create a new DialogFragment for the error dialog
	        ErrorDialogFragment errorFragment = new ErrorDialogFragment();
	        // Set the dialog in the DialogFragment
	        errorFragment.setDialog(errorDialog);
	        // Show the error dialog in the DialogFragment
	        errorFragment.show(getFragmentManager(),"Location Updates");
	    }
	}
	
	private String getDirectionsUrl(LatLng origin,List<LatLng> waypoints,LatLng lastStation){
		 
        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;
        // Destination of route
        String str_dest = "destination="+lastStation.latitude+","+lastStation.longitude;
        StringBuilder builder = null;
        if(waypoints != null && waypoints.size() > 0){
        	builder = new StringBuilder();
        	builder.append("optimize:true|");
        	for (int i = 0; i < waypoints.size(); i++) {
				LatLng waypoint = waypoints.get(i);
				builder.append("via:"+waypoint.latitude+","+waypoint.longitude);
				if(i < waypoints.size() -1)
					builder.append("|");
			}
        }
        // Sensor enabled
        String mode="mode=walking";
        // Building the parameters to the web service
        String sensor = "sensor=false";
        String str_waypoints = null;
        if(builder != null && builder.toString() != null && !builder.toString().isEmpty())
    		str_waypoints= "waypoints="+builder.toString();
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+mode+ (str_waypoints != null ? "&"+str_waypoints : "");
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;
 
        return url;
    }
	
	// Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String>{
 
        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {
 
            // For storing data from web service
            String data = "";
 
            try{
                // Fetching the data from web service
                data = HttpConnection.readUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }
 
        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
 
            ParserTask parserTask = new ParserTask();
            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }
 
    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{
 
        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
 
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
 
            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
 
                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }
 
        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            String distance = "";
            String duration = "";
 
            if(result.size()<1){
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }
 
            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();
 
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);
 
                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    
                    if(point.containsKey("distance")){    // Get distance from the list
                        distance = (String)point.get("distance");
                        continue;
                    }else if(point.containsKey("duration")){ // Get duration from the list
                        duration = (String)point.get("duration");
                        continue;
                    }
 
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
 
                    points.add(position);
                }
 
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.zIndex(1);
                lineOptions.color(Color.MAGENTA);
//                lineOptions.geodesic(true);
            }
 
            Toast.makeText(getBaseContext(),"Distance:"+distance + ", Duration:"+duration,Toast.LENGTH_SHORT).show();
 
            // Drawing polyline in the Google Map for the i-th route
            googleMap.addPolyline(lineOptions);
        }
    }
}
