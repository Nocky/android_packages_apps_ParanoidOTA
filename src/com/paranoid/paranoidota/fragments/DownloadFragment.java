/*
 * Copyright 2013 ParanoidAndroid Project
 *
 * This file is part of Paranoid OTA.
 *
 * Paranoid OTA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Paranoid OTA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Paranoid OTA.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.paranoid.paranoidota.fragments;

import java.io.File;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.paranoid.paranoidota.IOUtils;
import com.paranoid.paranoidota.MainActivity;
import com.paranoid.paranoidota.R;
import com.paranoid.paranoidota.Utils;
import com.paranoid.paranoidota.helpers.DownloadHelper;
import com.paranoid.paranoidota.helpers.SettingsHelper;
import com.paranoid.paranoidota.updater.GappsUpdater;
import com.paranoid.paranoidota.updater.RomUpdater;
import com.paranoid.paranoidota.updater.Updater.PackageInfo;
import com.paranoid.paranoidota.updater.Updater.UpdaterListener;

public class DownloadFragment extends android.preference.PreferenceFragment implements UpdaterListener {

    private Context mContext;
    private PreferenceCategory mRomRoot;
    private PreferenceCategory mGappsRoot;
    private RomUpdater mRomUpdater;
    private GappsUpdater mGappsUpdater;
    private PackageInfo[] mRomPackages;
    private PackageInfo[] mGappsPackages;

    Preference.OnPreferenceClickListener mDownloadListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            boolean isRom = preference.getExtras().getBoolean("isRom");
            if (DownloadHelper.isDownloading(isRom)) {
                int resId = isRom ? R.string.already_downloading_rom : R.string.already_downloading_gapps;
                Toast.makeText(mContext, resId, Toast.LENGTH_LONG)
                        .show();
                return false;
            }
            int index = Integer.parseInt(preference.getKey());
            PackageInfo info = isRom ? mRomPackages[index] : mGappsPackages[index];
            DownloadHelper.downloadFile(info.getPath(), info.getFilename(), info.getMd5(), isRom);
            Toast.makeText(
                    mContext,
                    mContext.getResources().getString(R.string.downloading_title,
                            new Object[] { info.getFilename() }), Toast.LENGTH_LONG).show();
            return false;
        }
    };

    Preference.OnPreferenceClickListener mDownloadedListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            boolean isRom = preference.getExtras().getBoolean("isRom");
            String fileName = preference.getExtras().getString("fileName");
            if (DownloadHelper.isDownloading(isRom, fileName)) {
                int resId = isRom ? R.string.wait_downloading_rom : R.string.wait_downloading_gapps;
                Toast.makeText(mContext, resId, Toast.LENGTH_LONG)
                        .show();
                return false;
            }
            int index = Integer.parseInt(preference.getKey());
            PackageInfo info = isRom ? mRomPackages[index] : mGappsPackages[index];
            File file = new File(new SettingsHelper(mContext).getDownloadPath(), info.getFilename());
            ((MainActivity) getActivity()).addFile(file);
            return false;
        }
    };

    public void setUpdaters(RomUpdater romUpdater, GappsUpdater gappsUpdater) {
        mRomUpdater = romUpdater;
        mRomUpdater.addUpdaterListener(this);
        mGappsUpdater = gappsUpdater;
        mGappsUpdater.addUpdaterListener(this);
        updateRom(mRomUpdater.getLastUpdates());
        updateGapps(mGappsUpdater.getLastUpdates());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(mContext);

        mRomRoot = new PreferenceCategory(mContext);
        mRomRoot.setTitle(R.string.rom);
        root.addPreference(mRomRoot);

        mGappsRoot = new PreferenceCategory(mContext);
        mGappsRoot.setTitle(R.string.gapps);
        root.addPreference(mGappsRoot);

        setPreferenceScreen(root);

        if (mRomUpdater != null) {
            updateRom(mRomUpdater.getLastUpdates());
        }

        if (mGappsUpdater != null) {
            updateGapps(mGappsUpdater.getLastUpdates());
        }
    }

    @Override
    public void versionFound(PackageInfo[] packages, boolean isRom) {
        if (isRom) {
            updateRom(packages);
        } else {
            updateGapps(packages);
        }
    }

    @Override
    public void startChecking(boolean isRom) {
        if (isRom) {
            updateRom(null);
        } else {
            updateGapps(null);
        }
    }

    @Override
    public void checkError(boolean isRom) {
    }

    private void updateRom(PackageInfo[] packages) {
        if (mRomRoot == null) {
            return;
        }

        mRomPackages = packages;

        update(mRomPackages, mRomRoot, true);
    }

    private void updateGapps(PackageInfo[] packages) {
        if (mGappsRoot == null) {
            return;
        }

        mGappsPackages = packages;

        update(mGappsPackages, mGappsRoot, false);
    }

    private void update(PackageInfo[] packages, PreferenceCategory root, boolean isRom) {
        root.removeAll();

        Preference info = new Preference(mContext);
        info.setSummary(getSummary(isRom));
        info.setIcon(R.drawable.ic_info);
        info.setSelectable(false);
        root.addPreference(info);

        if(packages != null && packages.length > 0) {
            for(int i = 0; i<packages.length && i<10; i++) {
                final Preference pref = new Preference(mContext);
                pref.setTitle(getPackageTitle(packages[i], isRom));
                String size = packages[i].getSize();
                pref.setKey(String.valueOf(i));
                pref.getExtras().putBoolean("isRom", isRom);
                pref.getExtras().putString("fileName", packages[i].getFilename());
                if(IOUtils.isOnDownloadList(mContext, packages[i].getFilename())) {
                    pref.setIcon(R.drawable.ic_offline);
                    pref.setOnPreferenceClickListener(mDownloadedListener);
                    if ("0".equals(size)) {
                        size = IOUtils.getDownloadSize(mContext, packages[i].getFilename());
                    }
                } else {
                    pref.setIcon(R.drawable.ic_download);
                    pref.setOnPreferenceClickListener(mDownloadListener);
                }
                if ("0".equals(size)) {
                    size = mContext.getResources().getString(R.string.unknown_size);
                }
                pref.setSummary(size);
                root.addPreference(pref);
            }
            info.setTitle(getOutdatedIconResourceId(isRom));
        } else {
            info.setTitle(getNoUpdatesIconResourceId(isRom));
        }
    }

    private String getSummary(boolean isRom) {
        if (isRom) {
            return Utils.getReadableVersion(Utils.getProp(Utils.MOD_VERSION));
        } else {
            if (mGappsUpdater.getVersion() == -1) {
                return mContext.getResources().getString(R.string.no_gapps_installed);
            }
            return Utils.getReadableVersion("pa_gapps-" + mGappsUpdater.getPlatform() + "-" + mGappsUpdater.getVersion());
        }
    }

    private String getPackageTitle(PackageInfo packageInfo, boolean isRom) {
        if (isRom) {
            return Utils.getReadableVersion(packageInfo.getFilename());
        } else {
            return packageInfo.getFilename();
        }
    }

    private int getOutdatedIconResourceId(boolean isRom) {
        if (!Utils.weAreInAospa()) {
            return isRom ? R.string.update_rom_to_aospa : R.string.update_gapps_to_aospa;
        }
        return isRom ? R.string.rom_outdated : R.string.gapps_outdated;
    }

    private int getNoUpdatesIconResourceId(boolean isRom) {
        return isRom ? R.string.no_rom_updates : R.string.no_gapps_updates;
    }
}
