package com.signalone.app.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.R
import com.signalone.app.databinding.ActivityContactosBinding
import com.signalone.app.databinding.ItemContactoBinding

class ContactosActivity : AppCompatActivity() {
    private lateinit var b: ActivityContactosBinding
    private lateinit var adapter: ContactosAdapter

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityContactosBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = ContactosAdapter(this) { index, accion ->
            when (accion) {
                "editar"   -> mostrarDialogo(index)
                "eliminar" -> confirmarEliminar(index)
            }
        }
        b.lvContactos.adapter = adapter
        b.tvBack.setOnClickListener { finish() }
        b.btnAgregar.setOnClickListener { mostrarDialogo(null) }
        actualizarVista()
    }

    private fun guardarYActualizar() {
        UserPreferences.guardarContactos(this, AppState.contactos)
        actualizarVista()
    }

    private fun actualizarVista() {
        adapter.notifyDataSetChanged()
        b.lvContactos.visibility = if (AppState.contactos.isEmpty()) View.GONE  else View.VISIBLE
        b.tvEmpty.visibility     = if (AppState.contactos.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun mostrarDialogo(editIndex: Int?) {
        val editando = editIndex != null
        val c = if (editando) AppState.contactos[editIndex!!] else null

        val nombreEt = EditText(this).apply {
            hint = "Ej: Mamá"; if (editando) setText(c!!.nombre)
            setTextColor(Color.parseColor("#F1F5F9"))
            setHintTextColor(Color.parseColor("#94A3B8"))
        }
        val telEt = EditText(this).apply {
            hint = "+57 300 000 0000"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            if (editando) setText(c!!.telefono)
            setTextColor(Color.parseColor("#F1F5F9"))
            setHintTextColor(Color.parseColor("#94A3B8"))
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(48, 32, 48, 16)
            addView(TextView(context).apply { text = "Nombre"; setTextColor(Color.parseColor("#94A3B8")) })
            addView(nombreEt)
            addView(TextView(context).apply {
                text = "Teléfono"; setTextColor(Color.parseColor("#94A3B8")); setPadding(0, 16, 0, 4)
            })
            addView(telEt)
        }
        AlertDialog.Builder(this)
            .setTitle(if (editando) "Editar contacto" else "Agregar contacto")
            .setView(layout)
            .setPositiveButton(getString(R.string.guardar)) { _, _ ->
                val nombre = nombreEt.text.toString().trim()
                val tel    = telEt.text.toString().trim()
                if (nombre.isNotEmpty() && tel.isNotEmpty()) {
                    if (editando) {
                        AppState.contactos[editIndex!!] =
                            AppState.contactos[editIndex].copy(nombre = nombre, telefono = tel)
                    } else {
                        val colorIdx = AppState.contactos.size % AppState.avatarColors.size
                        AppState.contactos.add(Contacto(nombre, tel, AppState.avatarColors[colorIdx]))
                    }
                    guardarYActualizar()   // ← persistir inmediatamente
                }
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun confirmarEliminar(index: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar contacto")
            .setMessage("¿Eliminar a ${AppState.contactos[index].nombre}?")
            .setPositiveButton(getString(R.string.eliminar)) { _, _ ->
                AppState.contactos.removeAt(index)
                guardarYActualizar()   // ← persistir inmediatamente
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    inner class ContactosAdapter(
        ctx: Context,
        private val onAction: (Int, String) -> Unit
    ) : BaseAdapter() {
        override fun getCount()              = AppState.contactos.size
        override fun getItem(p: Int)         = AppState.contactos[p]
        override fun getItemId(p: Int)       = p.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = ItemContactoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            val c = AppState.contactos[position]
            item.tvAvatar.text           = c.inicial
            item.tvAvatar.setBackgroundColor(c.color)
            item.tvNombre.text           = c.nombre
            item.tvTelefono.text         = c.telefono
            item.ivMenu.setOnClickListener { v ->
                PopupMenu(v.context, v).apply {
                    menu.add(getString(R.string.editar))
                    menu.add(getString(R.string.eliminar))
                    setOnMenuItemClickListener { mi ->
                        when (mi.title.toString()) {
                            getString(R.string.editar)   -> onAction(position, "editar")
                            getString(R.string.eliminar) -> onAction(position, "eliminar")
                        }
                        true
                    }
                    show()
                }
            }
            return item.root
        }
    }
}
