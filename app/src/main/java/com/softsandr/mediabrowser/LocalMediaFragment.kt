package com.softsandr.mediabrowser

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_main.*

/**
 * A placeholder fragment containing a simple media.
 */
class LocalMediaFragment : Fragment() {

    private lateinit var localMedia: LocalMedia
    private var interceptEvents = false

    // a primitive zoomer
    private val scaleGestureDetector by lazy {
        ScaleGestureDetector(context,
            object : ScaleGestureDetector.OnScaleGestureListener {
                private var scaleFactor = 1.0f

                override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                    if (context is MainActivity) {
                        (context as MainActivity).stopAutoSliding()
                    }
                    scaleFactor = 1.0f
                    interceptEvents = true
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector?): Boolean {
                    scaleFactor *= detector?.scaleFactor ?: 1f
                    scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f))
                    media_image.scaleX = scaleFactor
                    media_image.scaleY = scaleFactor
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector?) {
                    media_image.scaleX = 1f
                    media_image.scaleY = 1f
                    if (context is MainActivity) {
                        (context as MainActivity).startAutoSliding()
                    }
                    interceptEvents = false
                }
            })}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_main, container, false)
        localMedia = arguments?.getSerializable(ARG_MEDIA_FILE_PATH) as LocalMedia
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let {
            Glide.with(it).load(localMedia.path).into(media_image)
        }

        media_image.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event?.action == MotionEvent.ACTION_DOWN) {
                if (context is MainActivity) {
                    (context as MainActivity).showSystemUi()
                }
            }
            true
        }
    }

    companion object {
        /**
         * The fragment argument representing the path to the media file for this fragment.
         */
        private const val ARG_MEDIA_FILE_PATH = "local_media_file"

        /**
         * Returns a new instance of this fragment for the given file path.
         */
        fun newInstance(localMedia: LocalMedia): LocalMediaFragment {
            val fragment = LocalMediaFragment()
            val args = Bundle()
            args.putSerializable(ARG_MEDIA_FILE_PATH, localMedia)
            fragment.arguments = args
            return fragment
        }
    }
}