package org.webrtc.awesome.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @Author: hsh
 * @Description: java类作用描述
 * @CreateDate: 2020-05-22
 */

public class DstUser implements Parcelable {
    public String userName;
    public int avatarRes;
    public String userId;

    public DstUser() {
    }

    protected DstUser(Parcel in) {
        userName = in.readString();
        avatarRes = in.readInt();
        userId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userName);
        dest.writeInt(avatarRes);
        dest.writeString(userId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DstUser> CREATOR = new Creator<DstUser>() {
        @Override
        public DstUser createFromParcel(Parcel in) {
            return new DstUser(in);
        }

        @Override
        public DstUser[] newArray(int size) {
            return new DstUser[size];
        }
    };
}
