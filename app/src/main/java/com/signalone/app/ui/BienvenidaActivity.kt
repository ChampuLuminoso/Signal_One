package com.signalone.app.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.databinding.ActivityBienvenidaBinding

class BienvenidaActivity : AppCompatActivity() {
    private lateinit var b: ActivityBienvenidaBinding
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityBienvenidaBinding.inflate(layoutInflater)
        setContentView(b.root)
        val logo = SpannableString("SignalOne")
        logo.setSpan(ForegroundColorSpan(Color.parseColor("#B91C1C")), 6, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        b.tvLogo.text = logo
        b.btnComenzar.setOnClickListener { startActivity(Intent(this, RegistroActivity::class.java)) }
        b.tvLoginLink.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
    }
}
