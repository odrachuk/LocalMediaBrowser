package com.softsandr.mediabrowser

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer


class MainActivity : AppCompatActivity() {

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var sectionsPagerAdapter: SectionsPagerAdapter? = null
    private var timer: Timer? = null
    private var isToolbarShown = true

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val fullScreenHandler by lazy { FullscreenHandler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        container.setPageTransformer(false, CrossFadeTransformer())

        if (isPermissionGranted()) {
            findLocalMediaFiles()
        }
    }

    private fun isPermissionGranted(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted. Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSIONS
                )
            }
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                for (i in grantResults.indices) {
                    if (grantResults.isNotEmpty() && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        findLocalMediaFiles()
                    } else {
                        Toast.makeText(
                            this, getString(R.string.permission_request_explanation),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            stopAutoSliding()

            // the application should have only one scree, that is why I use a dialog for that
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.setAppearance(android.R.style.TextAppearance_Medium)
            input.setText(sharedPreferences.getInt(SHARED_PREF_SLIDING_PERIOD, SLIDING_DEFAULT_PERIOD_SEC).toString())
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.setting_dialog))
                .setMessage(getString(R.string.set_auto_sliding_period))
                .setView(input)
                .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                    val newPeriod: Int = try {
                        input.text.toString().toInt()
                    } catch (ignored: Exception) {
                        SLIDING_DEFAULT_PERIOD_SEC
                    }
                    sharedPreferences.edit().putInt(SHARED_PREF_SLIDING_PERIOD, newPeriod).apply()
                    startAutoSliding()
                }
                .setOnCancelListener { startAutoSliding() }
                .show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val localMedia = mutableListOf<LocalMedia>()

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            return LocalMediaFragment.newInstance(localMedia[position])
        }

        override fun getCount(): Int = localMedia.size

        fun addItems(localFolders: Collection<LocalMediaFolder>) {
            localFolders.forEach { folder ->
                folder.folderFilePaths.forEach { file ->
                    localMedia.add(LocalMedia(file, folder.isImages))
                }
            }
            notifyDataSetChanged()
        }

        fun clear() {
            localMedia.clear()
        }
    }

    private fun findLocalMediaFiles() {
        stopAutoSliding()
        progress.visibility = View.VISIBLE
        container.visibility = View.GONE

        // Create the adapter that will return a fragment for each media file.
        if (sectionsPagerAdapter == null) {
            sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        }
        sectionsPagerAdapter?.clear()

        findLocal(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )
        findLocal(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN
        )

        startAutoSliding()
    }

    fun startAutoSliding() {
        if (sectionsPagerAdapter != null && sectionsPagerAdapter!!.count > 0) {
            val sliderPeriod = sharedPreferences.getInt(SHARED_PREF_SLIDING_PERIOD, SLIDING_DEFAULT_PERIOD_SEC)
            timer = timer(
                "Slider Timer", false, 0L,
                TimeUnit.SECONDS.toMillis(sliderPeriod.toLong())
            ) {
                runOnUiThread {
                    if (container.currentItem == sectionsPagerAdapter!!.count - 1) {
                        container.currentItem = 0
                    } else {
                        container.currentItem = container.currentItem + 1
                    }
                }
            }
        }
    }

    fun stopAutoSliding() {
        timer?.cancel()
    }

    private fun findLocal(uri: Uri, projectionFolder: String, orderBy: String) {
        val localMediaFolders = mutableListOf<LocalMediaFolder>()
        val projection = arrayOf(MediaStore.MediaColumns.DATA, projectionFolder)
        applicationContext.contentResolver.query(
            uri, projection, null, null, "$orderBy DESC"
        )?.use {
            val columnFolder = it.getColumnIndexOrThrow(projectionFolder)
            val columnFilePath = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

            var pos = 0
            var absolutePathOfImage: String?
            while (it.moveToNext()) {
                absolutePathOfImage = it.getString(columnFilePath)

                var isFolder = false
                for (i in 0 until localMediaFolders.size) {
                    if (localMediaFolders[i].folderName == it.getString(columnFolder)) {
                        isFolder = true
                        pos = i
                        break
                    }
                }

                val filesInFolder = mutableListOf<String>()
                if (isFolder) {
                    filesInFolder.addAll(localMediaFolders[pos].folderFilePaths)
                    filesInFolder.add(absolutePathOfImage)
                    localMediaFolders[pos].folderFilePaths = filesInFolder
                } else {
                    filesInFolder.add(absolutePathOfImage)
                    localMediaFolders.add(
                        LocalMediaFolder(
                            it.getString(columnFolder), filesInFolder,
                            uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                    )
                }
            }
        }

        progress.visibility = View.GONE
        container.visibility = View.VISIBLE

        // Set up the ViewPager with the sections adapter.
        container.adapter = sectionsPagerAdapter
        sectionsPagerAdapter?.addItems(localMediaFolders)
    }

    fun showSystemUi() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        toolbar.visibility = View.VISIBLE
        isToolbarShown = true

        fullScreenHandler.removeMessages(1)
        fullScreenHandler.sendEmptyMessageDelayed(1, TimeUnit.SECONDS.toMillis(4))
    }

    fun hideSystemUi() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        toolbar.visibility = View.GONE
        isToolbarShown = false
    }

    private fun EditText.setAppearance(resId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setTextAppearance(resId)
        } else {
            setTextAppearance(context, resId)
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1111
        private const val SHARED_PREF_SLIDING_PERIOD = "sliding_period"
        private const val SLIDING_DEFAULT_PERIOD_SEC = 3
    }
}

private class FullscreenHandler(private val mainActivity: MainActivity?) : Handler() {
    override fun handleMessage(msg: Message?) {
        if (msg?.what == 1) {
            mainActivity?.hideSystemUi()
        }
    }
}
