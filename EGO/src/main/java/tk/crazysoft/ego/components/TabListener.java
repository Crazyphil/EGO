package tk.crazysoft.ego.components;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

public class TabListener<T extends Fragment> implements ActionBar.TabListener {

    private Fragment mFragment;
    private final AppCompatActivity mActivity;
    private final int mLayoutId;
    private final String mTag;
    private final Class<T> mClass;

    /** Constructor used each time a new tab is created.
     * @param activity  The host Activity, used to instantiate the fragment
     * @param tag  The identifier tag for the fragment
     * @param clz  The fragment's Class, used to instantiate the fragment
     */
    public TabListener(AppCompatActivity activity, String tag, Class<T> clz) {
        this(activity, android.R.id.content, tag, clz);
    }

    /** Constructor used each time a new tab is created.
     * @param activity  The host Activity, used to instantiate the fragment
     * @param layoutId The layout to add the fragment to
     * @param tag  The identifier tag for the fragment
     * @param clz  The fragment's Class, used to instantiate the fragment
     */
    public TabListener(AppCompatActivity activity, int layoutId, String tag, Class<T> clz) {
        mActivity = activity;
        mLayoutId = layoutId;
        mTag = tag;
        mClass = clz;

        // Check to see if we already have a fragment for this tab, probably
        // from a previously saved state.  If so, deactivate it, because our
        // initial state is that a tab isn't shown.
        mFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
        if (mFragment != null && !mFragment.isDetached()) {
            FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
            transactInCorrectContainer(ft, false);
            ft.commit();
        }
    }

    /* The following are each of the ActionBar.TabListener callbacks */

    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // Check if the fragment is already initialized
        if (mFragment == null) {
            // If not, instantiate and add it to the activity
            mFragment = Fragment.instantiate(mActivity, mClass.getName());
            ft.add(mLayoutId, mFragment, mTag);
        } else {
            // If it exists, simply attach it in order to show it if it was previously detached
            // or readd it to it's new parent if it was removed from another one previously
            transactInCorrectContainer(ft, true);
        }
    }

    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        if (mFragment != null) {
            // Detach the fragment, because another one is being attached
            ft.detach(mFragment);
        }
    }

    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // User selected the already selected tab. Usually do nothing.
    }

    private void transactInCorrectContainer(FragmentTransaction ft, boolean shouldAttach) {
        if (mFragment.getId() == 0) {
            ft.add(mLayoutId, mFragment, mTag);
        } else if (mFragment.getId() > 0 && mFragment.getId() != mLayoutId) {
            Fragment.SavedState savedState = null;
            if (mActivity.getSupportFragmentManager().findFragmentByTag(mFragment.getTag()) != null) {
                savedState = mActivity.getSupportFragmentManager().saveFragmentInstanceState(mFragment);
            }

            if (!shouldAttach) {
                ft.remove(mFragment);
            }

            try {
                mFragment = Fragment.instantiate(mActivity, mFragment.getClass().getName());
                mFragment.setInitialSavedState(savedState);
            } catch (Exception e) { // InstantiationException, IllegalAccessException
                throw new RuntimeException("Cannot reinstantiate fragment " + mFragment.getClass().getName(), e);
            }

            if (shouldAttach) {
                ft.add(mLayoutId, mFragment, mTag);
            }
        } else if (shouldAttach) {
            ft.attach(mFragment);
        } else {
            ft.detach(mFragment);
        }
    }

}