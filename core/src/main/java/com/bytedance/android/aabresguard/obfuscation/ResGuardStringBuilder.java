package com.bytedance.android.aabresguard.obfuscation;

import com.bytedance.android.aabresguard.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 混淆字典.
 * <p>
 * Copied from: https://github.com/shwenzhang/AndResGuard
 */
public class ResGuardStringBuilder {

    private final List<String> mReplaceStringBuffer;
    private final Set<Integer> mIsReplaced;
    private final Set<Integer> mIsWhiteList;
    private HashSet<Pattern> mBlacklistPatterns = new HashSet<>();
    private Set<String> mRemovedStrings = new HashSet<>();
    private String[] mAToZ = {
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
            "w", "x", "y", "z"
    };
    private String[] mAToAll = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "_", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",
            "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };
    /**
     * 在window上面有些关键字是不能作为文件名的
     * CON, PRN, AUX, CLOCK$, NUL
     * COM1, COM2, COM3, COM4, COM5, COM6, COM7, COM8, COM9
     * LPT1, LPT2, LPT3, LPT4, LPT5, LPT6, LPT7, LPT8, and LPT9.
     */
    private HashSet<String> mFileNameBlackList;
    private boolean mExtended = false;

    public ResGuardStringBuilder() {
        mFileNameBlackList = new HashSet<>();
        mFileNameBlackList.add("con");
        mFileNameBlackList.add("prn");
        mFileNameBlackList.add("aux");
        mFileNameBlackList.add("nul");
        for (int i = 1; i <= 9; i++) {
            mFileNameBlackList.add("com" + i);
            mFileNameBlackList.add("lpt" + i);
        }
        mReplaceStringBuffer = new ArrayList<>();
        mIsReplaced = new HashSet<>();
        mIsWhiteList = new HashSet<>();
    }

    public void reset(HashSet<Pattern> blacklistPatterns) {
        reset(blacklistPatterns, false);
    }

    public void reset(HashSet<Pattern> blacklistPatterns, boolean useRandomName) {
        mReplaceStringBuffer.clear();
        mIsReplaced.clear();
        mIsWhiteList.clear();
        mExtended = false;
        mBlacklistPatterns = blacklistPatterns != null ? blacklistPatterns : new HashSet<>();
        mRemovedStrings.clear();

        for (String str : mAToZ) {
            if (!Utils.match(str, blacklistPatterns)) {
                mReplaceStringBuffer.add(str);
            }
        }

        for (String first : mAToZ) {
            for (String aMAToAll : mAToAll) {
                String str = first + aMAToAll;
                if (!Utils.match(str, blacklistPatterns)) {
                    mReplaceStringBuffer.add(str);
                }
            }
        }

        for (String first : mAToZ) {
            for (String second : mAToAll) {
                for (String third : mAToAll) {
                    String str = first + second + third;
                    if (!mFileNameBlackList.contains(str) && !Utils.match(str, blacklistPatterns)) {
                        mReplaceStringBuffer.add(str);
                    }
                }
            }
        }

        if (useRandomName) {
            Collections.shuffle(mReplaceStringBuffer);
        }
    }

    // 对于某种类型用过的mapping，全部不能再用了
    public void removeStrings(Collection<String> collection) {
        if (collection == null) return;
        mRemovedStrings.addAll(collection);
        mReplaceStringBuffer.removeAll(collection);
    }

    public boolean isReplaced(int id) {
        return mIsReplaced.contains(id);
    }

    public boolean isInWhiteList(int id) {
        return mIsWhiteList.contains(id);
    }

    public void setInWhiteList(int id) {
        mIsWhiteList.add(id);
    }

    public void setInReplaceList(int id) {
        mIsReplaced.add(id);
    }

    public String getReplaceString(Collection<String> names) throws IllegalArgumentException {
        if (mReplaceStringBuffer.isEmpty()) {
            if (!mExtended) {
                extendDictionary();
            }
            if (mReplaceStringBuffer.isEmpty()) {
                throw new IllegalArgumentException("Resource name dictionary exhausted. " +
                        "Consider reducing whitelist rules or using shorter names.");
            }
        }
        if (names != null) {
            for (int i = 0; i < mReplaceStringBuffer.size(); i++) {
                String name = mReplaceStringBuffer.get(i);
                if (names.contains(name)) {
                    continue;
                }
                return mReplaceStringBuffer.remove(i);
            }
            // 所有候选都被排除，尝试扩展字典
            if (!mExtended) {
                extendDictionary();
                return getReplaceString(names);
            }
        }
        return mReplaceStringBuffer.remove(0);
    }

    public String getReplaceString() {
        return getReplaceString(null);
    }

    private void extendDictionary() {
        mExtended = true;
        for (String first : mAToZ) {
            for (String second : mAToAll) {
                for (String third : mAToAll) {
                    for (String fourth : mAToAll) {
                        String str = first + second + third + fourth;
                        if (!mFileNameBlackList.contains(str)
                                && !Utils.match(str, mBlacklistPatterns)
                                && !mRemovedStrings.contains(str)) {
                            mReplaceStringBuffer.add(str);
                        }
                    }
                }
            }
        }
    }
}
