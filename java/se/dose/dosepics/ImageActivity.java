package se.dose.dosepics;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Activity that lets the user browse images previously shared to the server
 *
 * The Activity is just basically a ViewPager that lets you browse images.
 * First a list of images is being retrieved from the server, then a
 * PagerAdapter is being configured to use this info to retrieve individual
 * images.
 */
public class ImageActivity extends AppCompatActivity {

    private ImageCollectionPagerAdapter adapter;
    private ViewPager viewPager;

    // Information about where to read images. Note that all images are public
    // and as such, no password is needed
    private String resource;
    private String username;

    // Full screen mode vs "show action bar and image info mode"
    private boolean fullscreen = false;

    // We need to keep track of the runners that wait for the action bar to hide,
    // in order to be able to cancel them when a new one is started
    private ImageFragment.ActionBarHider actionBarhider;

    /**
     * Called when the activity is starting. Entry point for the Activity
     * Sets up the some field variables and start by getting a list of image
     * resources that belong to the user
     *
     * @param savedInstanceState - not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        resource = sp.getString("resource", "");
        username = sp.getString("username", "");

        getListOfImages();
    }

    /**
     * FIXME: Major problem! This is an ugly solution to a complex problem!
     *
     * When the Activity is restarted a new list of images is retrieved, which
     * basically restarts the activity
     */
    @Override
    public void onResume()
    {
        super.onResume();
        getListOfImages();
    }

    /**
     * Start a background thread to get a list of image resources owned by user
     */
    private void getListOfImages()
    {
        GetListOfImages glof = new GetListOfImages();
        glof.execute();
    }

    /**
     * AsyncTask that handles the request to get list of image resources
     */
    public class GetListOfImages extends AsyncTask<Void, Void, Void> {

        List<Integer> imageList = null;
        @Override
        public Void doInBackground(Void... dummy) {
            String finalResource = resource + "/users/" + username + "/pics";

            URL url;
            HttpURLConnection conn;
            imageList = new LinkedList<>();
            try {
                url = new URL(finalResource);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                String responseBody;
                int responseCode = conn.getResponseCode();
                if (responseCode >= 400)
                    responseBody = readStream(conn.getErrorStream());
                else
                    responseBody = readStream(conn.getInputStream());

                JSONArray responseArray = new JSONArray(responseBody);
                for(int i = 0; i < responseArray.length(); i++)
                    imageList.add(responseArray.getInt(i));
            } catch (Exception e) {
                // This will be enough to report an error in onPostExecute()
                imageList = null;
            }
            return null;
        }

        private String readStream(InputStream is)
        {
            BufferedReader r;
            StringBuilder res = new StringBuilder();
            try {
                r = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = r.readLine()) != null)
                {
                    res.append(line);
                }
            } catch (Exception e)
            {
            }
            return res.toString();
        }

        @Override
        public void onPostExecute(Void dummy)
        {
            if(imageList != null)
                listOfImagesReceived(imageList);
            else
            {
                Log.d("DOSESE", "Failed to get number of images owned by " + username);
                /*
                Toast.makeText(getApplicationContext(),
                        "Failed to connect to server, please check the configuration!",
                        Toast.LENGTH_LONG).show();
                */
                Intent i = new Intent(getApplicationContext(), ConfigurationActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
            }
        }
    }

    /**
     * Called when the list of image resources has been received.
     * Sets up the PagerAdapter for the ViewPager that starts loading the first
     * image resource
     */
    public void listOfImagesReceived(List<Integer> imageList)
    {
        adapter = new ImageCollectionPagerAdapter(getSupportFragmentManager());
        adapter.imageResources = imageList;
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(adapter);

        // FIXME: This is debug, start at the end of the list
        viewPager.setCurrentItem(imageList.size());

        // Keep two fragments alive on each side
        viewPager.setOffscreenPageLimit(2);
    }

    /**
     * Adapter for ViewPager
     */
    public class ImageCollectionPagerAdapter extends FragmentStatePagerAdapter {

        // The list of image resources
        List<Integer> imageResources;

        // Constructor needed for some reason
        public ImageCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int index) {
            Fragment fragment = new ImageFragment();
            Bundle args = new Bundle();
            args.putInt(ImageFragment.IMAGE_INDEX, imageResources.get(index));
            args.putString(ImageFragment.RESOURCE, resource);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return imageResources.size();
        }

        // Use this to return the current image resource being displayed
        @Override
        public CharSequence getPageTitle(int position) {
            return "" + imageResources.get(position);
        }
    }

    /**
     * User has clicked the share icon and wishes to share the link to the
     * image resource on the server
     */
    private void shareLink()
    {
        int item = viewPager.getCurrentItem();
        int picResource = Integer.parseInt(adapter.getPageTitle(item).toString());
        String link = resource + "/pics/" + picResource + "/pic";
        Intent i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.putExtra(Intent.EXTRA_TEXT, link);
        i.setType("text/plain");
        startActivity(i);
    }

    /**
     * Manual Quote:
     * "Initialize the contents of the Activity's standard options menu.
     * We just supply the item(s) from the menu_image XML file
     *
     * @param menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_image, menu);
        return true;
    }

    /**
     * Manual quote:
     * "This hook is called whenever an item in your options menu is selected."
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_configure_dosepics) {
            Intent i = new Intent(getApplicationContext(), ConfigurationActivity.class);
            startActivity(i);
        } else if(id == R.id.action_share_link) {
            shareLink();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fragment class for each image. This is where the fun stuff starts.
     *
     * Three things are loaded in turn from the server when the Fragment is
     * created:
     *
     *  * Thumbnail
     *  * Information about image
     *  * Actual image
     *
     * The thumbnail is loaded immediately and shown to the user. Then the
     * information is loaded directly when the thumbnail has been received.
     *
     * If the Fragment is still alive after 3000 ms, or if the user presses the
     * thumbnail before that timeout, the actual image is loaded. This is done
     * to avoid unnecessary network traffic if the user is just quickly swiping
     * by the images.
     *
     * Also, each time the Fragment is pressed a full-screen vs "show action
     * bar and image info" is toggled.
     */
    public static class ImageFragment extends Fragment {

        public static final String IMAGE_INDEX = "se.dose.dosepics.IMAGE_FRAGMENT_INDEX";
        public static final String RESOURCE = "se.dose.dosepics.IMAGE_FRAGMENT_RESOURCE";

        // The order in which data is being read from the server
        private enum Mode { NONE, THUMB, INFO, IMAGE };
        private Mode mode;

        // Where to find the REST API
        private String resource = "";

        // What image resource to load
        private int id = 0;

        // The Fragment view containing the image
        ViewGroup rootView = null;

        // Keep track of whether we have already started a download of the
        // actual image. This flag is used by the delayed auto-loader
        private boolean isLoadingRealImage = false;

        // Information about the image
        private String imageInfo = "Information is still being loaded...";

        // The currently shown image, be it thumbnail or real image
        Bitmap image;

        // FIXME: I don't want these.
        GetImage getImage;
        GetImage getThumb;

        /**
         * Main entry of the Fragment, called when it is created.
         *
         * Set up the mode to show that nothing has been loaded yet.
         *
         * @param savedInstanceState - not used
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            id = getArguments().getInt(IMAGE_INDEX);
            resource = getArguments().getString(RESOURCE);
            mode = Mode.NONE;
        }

        /**
         * Called when the Fragment is being seen by the user. Used only for
         * a single purpuse: to keep track of whether we are in fullscreen mode
         * and hence should or should not view the information about the image
         * at the bottom
         *
         * @param isVisibleToUser - not used
         */
        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            updateFragment();
        }

        /**
         * Called when the Fragment's View is crated.
         *
         * Here's where the magic starts! Start by loading the thumbnail and
         * tell the user what's going on by showing a progress bar that fills
         * the entire Fragment (and screen)
         *
         * @param inflater      - not used
         * @param container     - not used
         * @param savedInstance - not used
         * @return
         */
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container,
                                 Bundle savedInstance) {
            rootView = (ViewGroup) inflater.inflate(R.layout.fragment_image_object, container, false);

            // Enable fullscreen mode
            WindowManager.LayoutParams attr = getActivity().getWindow().getAttributes();
            attr.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getActivity().getWindow().setAttributes(attr);

            // Start of network chain, begin by loading the thumbnail
            getThumb = new GetImage();
            getThumb.execute();

            updateFragment();
            return rootView;
        }

        /**
         * Called when a thumbnail has been received. Start next step by
         * getting information about the image
         */
        private void thumbReceived()
        {
            mode = Mode.THUMB;

            // Get information about image
            Runnable getInfoRunnable = new Runnable() {
                @Override
                public void run() {
                    // We might have been destroyed already, make sure this is
                    // not the case before we continue
                    if (getContext() == null) {
                        return;
                    }
                    // Get the real image in the background
                    GetImageInfo getImageInfo = new GetImageInfo();
                    getImageInfo.execute(id);
                }
            };
            getInfoRunnable.run();

            updateFragment();
        }

        /**
         * Called when image info has been received. If the Fragment is still
         * alive after 3000 ms, automatically start the download of the real
         * image
         */

        private void infoReceived()
        {
            mode = Mode.INFO;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    // We might have been destroyed already, make sure this is
                    // not the case before we continue
                    if (getContext() == null) {
                        return;
                    }
                    doLoadRealImage();
                }
            };
            new Handler().postDelayed(r, 3000);
            updateFragment();
        }

        /**
         * The final image has been received and we are done!
         */
        private void imageReceived()
        {
            mode = Mode.IMAGE;
            updateFragment();
        }

        /**
         * Help function to do the actual loading of the image
         */
        private void doLoadRealImage()
        {
            // Are we already loading the real image?
            // This happens if the automatic load is triggered after the user
            // has already clicked the image to start a download
            if(isLoadingRealImage)
                return;
            isLoadingRealImage = true;

            // Get the real image in the background
            getImage = new GetImage();
            getImage.execute();
        }

        /**
         * Update the GUI
         */
        private void updateFragment() {

            // Sometimes we are destroyed before we have time to update
            if (getActivity() == null) {
                return;
            }

            final android.support.v7.app.ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();

            rootView.removeAllViews();

            // If we haven't even received the thumbnail yet, just add a
            // progress bar and disable action bar
            if (mode == Mode.NONE) {
                ab.hide();

                // Add a text telling user what's going on
                TextView tv = new TextView(getContext());
                tv.setText("Loading thumbnail...");
                rootView.addView(tv);

                // Add a progress bar
                ProgressBar progressBar = new ProgressBar(getContext());
                progressBar.setIndeterminate(true);
                rootView.addView(progressBar);

                return;
            }

            // FrameLayout to put image into
            FrameLayout frameLayout = new FrameLayout(getContext());
            LinearLayout.LayoutParams frameparams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0);
            frameLayout.setLayoutParams(frameparams);

            // Image view with our image to be put into frameLayout
            ImageView iv = new ImageView(getContext());
            iv.setImageBitmap(image);
            // If the user clicks the thumbnail, toggle the fullscreen mode
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Toggle full screen flag
                    ((ImageActivity) getActivity()).fullscreen =
                            !((ImageActivity) getActivity()).fullscreen;
                    updateFragment();

                    // Also, if we haven't loaded the real image yet, start loading it immediately
                    if (mode == Mode.THUMB || mode == Mode.INFO)
                        doLoadRealImage();
                }
            });
            FrameLayout.LayoutParams imageLayoutParams = new
                    FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    0);
            imageLayoutParams.gravity = Gravity.TOP;
            iv.setLayoutParams(imageLayoutParams);
            frameLayout.addView(iv);
            rootView.addView(frameLayout);

            // If we are not in fullscreen mode, add information about the image
            // at the bottom by putting it into a Toolbar and add it to the
            // FrameLayout with the image
            if (!((ImageActivity) getActivity()).fullscreen) {
                // Add a TextView to an action bar, and put the
                // action bar in the frame layout
                TextView tv = new TextView(getContext());
                tv.setText(imageInfo);
                Toolbar bottomBar = new Toolbar(getContext());
                bottomBar.addView(tv);

                FrameLayout.LayoutParams barLayoutParams = new
                        FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        0);
                barLayoutParams.gravity = Gravity.BOTTOM;
                bottomBar.setLayoutParams(barLayoutParams);
                frameLayout.addView(bottomBar);
            }

            // Handle the visibility of the action bar
            if (((ImageActivity) getActivity()).fullscreen) {
                // Hide the action bar
                try {
                    ab.hide();
                } catch (Exception e) {
                    // If we're destroyed already
                    return;
                }
            } else {
                // Enable action bar
                try {
                    ab.show();

                    // But hide it again in 5000 ms
                    ActionBarHider runner = ((ImageActivity) getActivity()).actionBarhider;

                    // If there was a previous hider started already, cancel it
                    if(runner != null)
                        runner.isCancelled = true;

                    ((ImageActivity) getActivity()).actionBarhider = new ActionBarHider();
                    new Handler().postDelayed(
                            ((ImageActivity) getActivity()).actionBarhider, 5000);

                } catch (Exception e) {
                    // If we're destroyed already
                    return;
                }
            }
        }

        /**
         * Background thread that will just wait for 5000 ms and (if not
         * cancelled) hide the action bar
         */
        class ActionBarHider implements Runnable {
            boolean isCancelled = false;

            @Override
            public void run() {

                // We might have been destroyed already, make sure this is not the case
                // before we continue
                if (getContext() == null) {
                    return;
                }

                // If we are already in fullscreen mode, do nothing
                if (!isCancelled) {
                    ((ImageActivity) getActivity()).fullscreen = true;
                    updateFragment();
                }

            }
        }

        /**********************************************************
         * AsyncTask that gets an image from the REST API
         **********************************************************/
        class GetImage extends AsyncTask<Void, Void, Void> {

            @Override
            public Void doInBackground(Void... dummy) {

                String finalResource = resource + "/pics/" + id;

                // What are we currently looking for?
                if(mode == Mode.NONE)
                    finalResource += "/thumb";
                else if(mode == Mode.INFO)
                    finalResource += "/swipe";

                URL url;
                HttpURLConnection conn;
                try {
                    url = new URL(finalResource);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    int responseCode = conn.getResponseCode();
                    if (responseCode >= 400)
                        // Failed
                        return null;
                    else {
                        // Success! Now, if width is larger than the height, rotate image
                        Bitmap unrotatedImage = BitmapFactory.decodeStream(conn.getInputStream());
                        if(unrotatedImage.getWidth() > unrotatedImage.getHeight()) {
                            Matrix matrix = new Matrix();
                            matrix.postRotate(90);

                            image = Bitmap.createBitmap(
                                    unrotatedImage,
                                    0,
                                    0,
                                    unrotatedImage.getWidth(),
                                    unrotatedImage.getHeight(),
                                    matrix,
                                    true);
                        } else
                            image = unrotatedImage;
                    }
                } catch (Exception e) {
                    return null;
                }

                return null;
            }

            @Override
            public void onPostExecute(Void dummy) {

                // What were we getting? Thumbnail or actual image?
                if(mode == Mode.NONE)
                    thumbReceived();
                else if(mode == Mode.INFO)
                    imageReceived();
            }
        }

        /******************************************************************
         * AsyncTask that gets information about an image from the REST API
         *****************************************************************/
        class GetImageInfo extends AsyncTask<Integer, Void, String> {

            @Override
            public String doInBackground(Integer... ints) {

                String finalResource = resource + "/pics/" + id;

                URL url;
                HttpURLConnection conn;
                try {
                    url = new URL(finalResource);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    String responseBody;
                    int responseCode = conn.getResponseCode();
                    if (responseCode >= 400)
                        responseBody = readStream(conn.getErrorStream());
                    else
                        responseBody = readStream(conn.getInputStream());

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    return jsonResponse.getString("info");
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public void onPostExecute(String info) {
                if(info == null)
                    imageInfo = "Failed to retrieve information about image!";
                else {
                    imageInfo = info;
                    infoReceived();
                }
            }

            private String readStream(InputStream is)
            {
                BufferedReader r;
                StringBuilder res = new StringBuilder();
                try {
                    r = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = r.readLine()) != null)
                    {
                        res.append(line);
                    }
                } catch (Exception e)
                {
                }
                return res.toString();
            }
        }
    }

}