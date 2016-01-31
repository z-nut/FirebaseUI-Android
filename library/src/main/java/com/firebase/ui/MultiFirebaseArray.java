package com.firebase.ui;


import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class implements an array-like collection on top of multiple Firebase locations.
 *
 * Created by Brad J Johnson on 1/24/2016.
 */
public class MultiFirebaseArray implements ChildEventListener{

    public interface OnChangedListener {
        enum EventType { Added, Changed, Removed, Moved }
        void onChanged(EventType type, int index, int oldIndex);
    }

    protected List<Query> mQueryList;
    protected OnChangedListener mListener;
    protected ArrayList<DataSnapshot> mSnapshots;

    //TODO How can I not duplicate code between the constructors?
    protected MultiFirebaseArray (Query ref){
        mQueryList = new ArrayList<>(1);
        mQueryList.add(ref);
        mSnapshots = new ArrayList<DataSnapshot>();
        for(int i = 0; i < mQueryList.size(); i++)
            mQueryList.get(i).addChildEventListener(this);
    }

    public MultiFirebaseArray(List<Query> refList) {
        mQueryList = refList;
        mSnapshots = new ArrayList<DataSnapshot>();
        for(int i = 0; i < mQueryList.size(); i++)
            mQueryList.get(i).addChildEventListener(this);
    }

    public void cleanup() {
        for(int i = 0; i < mSnapshots.size(); i++) {
            mQueryList.get(i).removeEventListener(this);
        }
    }

    public int getCount() {
        return mSnapshots.size();

    }
    public DataSnapshot getItem(int index) {
        return mSnapshots.get(index);
    }

    private int getIndexForKey(String key) {
        int index = 0;
        for (DataSnapshot snapshot : mSnapshots) {
            if (snapshot.getKey().equals(key)) {
                return index;
            } else {
                index++;
            }
        }
        throw new IllegalArgumentException("Key not found");
    }

    // Start of ChildEventListener methods
    public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
        int index = 0;
        if (previousChildKey != null) {
            index = getIndexForKey(previousChildKey) + 1;
        }
        mSnapshots.add(index, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Added, index);
    }

    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.set(index, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Changed, index);
    }

    public void onChildRemoved(DataSnapshot snapshot) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(index);
        notifyChangedListeners(OnChangedListener.EventType.Removed, index);
    }

    public void onChildMoved(DataSnapshot snapshot, String previousChildKey) {
        int oldIndex = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(oldIndex);
        int newIndex = previousChildKey == null ? 0 : (getIndexForKey(previousChildKey) + 1);
        mSnapshots.add(newIndex, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Moved, newIndex, oldIndex);
    }

    public void onCancelled(FirebaseError firebaseError) {
        // TODO: what do we do with this?
    }
    // End of ChildEventListener methods

    public void setOnChangedListener(OnChangedListener listener) {
        mListener = listener;
    }
    protected void notifyChangedListeners(OnChangedListener.EventType type, int index) {
        notifyChangedListeners(type, index, -1);
    }
    protected void notifyChangedListeners(OnChangedListener.EventType type, int index, int oldIndex) {
        if(mListener != null)
            mListener.onChanged(type, index, oldIndex);
    }
}
