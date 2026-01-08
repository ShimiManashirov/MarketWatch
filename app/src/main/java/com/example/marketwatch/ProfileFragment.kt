package com.example.marketwatch

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val myPostsButton: Button = view.findViewById(R.id.myPostsButton)

        myPostsButton.setOnClickListener {
            val intent = Intent(activity, UserPostsActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
